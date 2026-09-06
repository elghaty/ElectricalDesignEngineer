package com.electricaldesignengineer.app

import kotlin.math.sqrt

/**
 * Professional electrical design calculation engine.
 *
 * Design sequence:
 * Load -> Demand -> Current -> Cable -> Correction Factors
 * -> Voltage Drop -> Breaker -> Short Circuit -> Checks
 *
 * The engine automatically increases cable and breaker sizes
 * until the design requirements are satisfied.
 */
object EngineeringDesignEngine {

    // ============================================================
    // DESIGN LIMITS
    // ============================================================

    const val DEFAULT_MAX_VOLTAGE_DROP_PERCENT = 5.0

    // ============================================================
    // CABLE
    // ============================================================

    enum class CableMaterial {
        COPPER,
        ALUMINIUM
    }

    enum class InsulationType {
        PVC,
        XLPE,
        EPR
    }

    enum class InstallationMethod {
        CONDUIT,
        TRUNKING,
        CABLE_TRAY,
        CABLE_LADDER,
        FREE_AIR,
        DIRECT_BURIED,
        DUCT
    }

    data class CableSpecification(
        val sizeMm2: Double,
        val material: CableMaterial,
        val insulation: InsulationType,
        val cores: Int,
        val installationMethod: InstallationMethod,
        val baseAmpacityA: Double,
        val resistanceOhmPerKm: Double,
        val reactanceOhmPerKm: Double
    )

    /**
     * Standard conductor sizes.
     *
     * Ampacity values are intentionally treated as a design database
     * and not as universal IEC tabulated values. Final project design
     * should allow the user to select the applicable standard/table.
     */
    private val copperXLPETrayAmpacity = listOf(
        1.5 to 21.0,
        2.5 to 29.0,
        4.0 to 38.0,
        6.0 to 49.0,
        10.0 to 68.0,
        16.0 to 91.0,
        25.0 to 119.0,
        35.0 to 147.0,
        50.0 to 179.0,
        70.0 to 225.0,
        95.0 to 271.0,
        120.0 to 314.0,
        150.0 to 359.0,
        185.0 to 408.0,
        240.0 to 476.0,
        300.0 to 541.0,
        400.0 to 621.0,
        500.0 to 700.0,
        630.0 to 795.0
    )

    // ============================================================
    // BREAKER
    // ============================================================

    data class Breaker(
        val ratingA: Int,
        val icuKA: Double,
        val poles: Int = 4
    )

    private val standardBreakerRatings = listOf(
        6, 10, 16, 20, 25, 32, 40, 50, 63,
        80, 100, 125, 160, 200, 250, 315,
        400, 500, 630, 800, 1000, 1250,
        1600, 2000, 2500, 3200, 4000
    )

    private val standardIcuKA = listOf(
        6.0,
        10.0,
        15.0,
        25.0,
        36.0,
        50.0,
        65.0,
        85.0,
        100.0
    )

    // ============================================================
    // CORRECTION FACTORS
    // ============================================================

    data class CorrectionFactors(
        val ambientTemperatureFactor: Double = 1.0,
        val groupingFactor: Double = 1.0,
        val thermalInsulationFactor: Double = 1.0,
        val soilFactor: Double = 1.0,
        val installationFactor: Double = 1.0
    ) {
        fun total(): Double {
            return ambientTemperatureFactor *
                    groupingFactor *
                    thermalInsulationFactor *
                    soilFactor *
                    installationFactor
        }
    }

    // ============================================================
    // LOAD
    // ============================================================

    data class LoadInput(
        val name: String,
        val quantity: Double,
        val unitPowerKW: Double,
        val demandFactor: Double,
        val powerFactor: Double,
        val phase: String = "3-PH"
    )

    data class LoadResult(
        val connectedKW: Double,
        val demandKW: Double,
        val demandKVA: Double,
        val currentA: Double
    )

    fun calculateLoad(
        load: LoadInput,
        voltageV: Double,
        threePhase: Boolean
    ): LoadResult {

        val quantity = load.quantity.coerceAtLeast(0.0)
        val unitPower = load.unitPowerKW.coerceAtLeast(0.0)

        val connectedKW =
            quantity * unitPower

        val df =
            load.demandFactor.coerceIn(0.0, 1.0)

        val pf =
            load.powerFactor.coerceIn(0.01, 1.0)

        val demandKW =
            connectedKW * df

        val demandKVA =
            demandKW / pf

        val currentA =
            if (threePhase) {
                threePhaseCurrent(
                    demandKVA,
                    voltageV
                )
            } else {
                singlePhaseCurrent(
                    demandKVA,
                    voltageV
                )
            }

        return LoadResult(
            connectedKW = connectedKW,
            demandKW = demandKW,
            demandKVA = demandKVA,
            currentA = currentA
        )
    }

    // ============================================================
    // CURRENT
    // ============================================================

    fun threePhaseCurrent(
        kva: Double,
        voltageV: Double
    ): Double {

        if (kva <= 0.0 || voltageV <= 0.0) {
            return 0.0
        }

        return kva * 1000.0 /
                (sqrt(3.0) * voltageV)
    }

    fun singlePhaseCurrent(
        kva: Double,
        voltageV: Double
    ): Double {

        if (kva <= 0.0 || voltageV <= 0.0) {
            return 0.0
        }

        return kva * 1000.0 /
                voltageV
    }

    // ============================================================
    // TOTAL LOAD
    // ============================================================

    data class TotalLoadResult(
        val connectedKW: Double,
        val demandKW: Double,
        val demandKVA: Double,
        val currentA: Double,
        val effectivePF: Double
    )

    fun calculateTotalLoad(
        loads: List<LoadInput>,
        voltageV: Double,
        threePhase: Boolean,
        diversityFactor: Double = 1.0,
        coincidenceFactor: Double = 1.0
    ): TotalLoadResult {

        var connectedKW = 0.0
        var demandKW = 0.0
        var demandKVA = 0.0

        loads.forEach { load ->

            val result =
                calculateLoad(
                    load,
                    voltageV,
                    threePhase
                )

            connectedKW += result.connectedKW
            demandKW += result.demandKW
            demandKVA += result.demandKVA
        }

        val safeDiversity =
            diversityFactor.coerceAtLeast(0.01)

        val safeCoincidence =
            coincidenceFactor.coerceIn(0.0, 1.0)

        val diversifiedKVA =
            demandKVA / safeDiversity

        val finalKVA =
            diversifiedKVA * safeCoincidence

        val finalDemandKW =
            if (demandKVA > 0.0) {
                demandKW *
                        (finalKVA / demandKVA)
            } else {
                0.0
            }

        val effectivePF =
            if (finalKVA > 0.0) {
                finalDemandKW / finalKVA
            } else {
                1.0
            }

        val currentA =
            if (threePhase) {
                threePhaseCurrent(
                    finalKVA,
                    voltageV
                )
            } else {
                singlePhaseCurrent(
                    finalKVA,
                    voltageV
                )
            }

        return TotalLoadResult(
            connectedKW = connectedKW,
            demandKW = finalDemandKW,
            demandKVA = finalKVA,
            currentA = currentA,
            effectivePF = effectivePF
        )
    }

    // ============================================================
    // CABLE SELECTION
    // ============================================================

    data class CableDesignResult(
        val success: Boolean,
        val cable: CableSpecification?,
        val correctionFactors: CorrectionFactors,
        val correctedAmpacityA: Double,
        val voltageDropV: Double,
        val voltageDropPercent: Double,
        val parallelRuns: Int,
        val status: String
    )

    /**
     * Automatically searches cable sizes and parallel runs.
     *
     * The selected cable must satisfy:
     *
     * Ib <= Iz(corrected)
     *
     * and
     *
     * Voltage Drop <= allowed limit
     */
    fun autoSelectCable(
        designCurrentA: Double,
        lengthM: Double,
        voltageV: Double,
        powerFactor: Double,
        threePhase: Boolean,
        material: CableMaterial = CableMaterial.COPPER,
        insulation: InsulationType = InsulationType.XLPE,
        cores: Int = 4,
        installationMethod: InstallationMethod =
            InstallationMethod.CABLE_TRAY,
        correctionFactors: CorrectionFactors =
            CorrectionFactors(),
        maxVoltageDropPercent: Double =
            DEFAULT_MAX_VOLTAGE_DROP_PERCENT,
        maxParallelRuns: Int = 8
    ): CableDesignResult {

        if (
            designCurrentA <= 0.0 ||
            lengthM < 0.0 ||
            voltageV <= 0.0
        ) {
            return CableDesignResult(
                false,
                null,
                correctionFactors,
                0.0,
                0.0,
                0.0,
                0,
                "INVALID INPUT"
            )
        }

        for (runs in 1..maxParallelRuns) {

            for (entry in copperXLPETrayAmpacity) {

                val size =
                    entry.first

                val baseAmpacity =
                    entry.second

                val adjustedBase =
                    when (material) {
                        CableMaterial.COPPER ->
                            baseAmpacity

                        CableMaterial.ALUMINIUM ->
                            baseAmpacity * 0.78
                    }

                val insulationFactor =
                    when (insulation) {
                        InsulationType.PVC -> 0.92
                        InsulationType.XLPE -> 1.00
                        InsulationType.EPR -> 1.00
                    }

                val methodFactor =
                    when (installationMethod) {
                        InstallationMethod.CABLE_TRAY -> 1.00
                        InstallationMethod.CABLE_LADDER -> 1.05
                        InstallationMethod.FREE_AIR -> 1.08
                        InstallationMethod.CONDUIT -> 0.90
                        InstallationMethod.TRUNKING -> 0.95
                        InstallationMethod.DIRECT_BURIED -> 0.88
                        InstallationMethod.DUCT -> 0.90
                    }

                val effectiveFactors =
                    correctionFactors.copy(
                        thermalInsulationFactor =
                            correctionFactors.thermalInsulationFactor *
                                    insulationFactor,
                        installationFactor =
                            correctionFactors.installationFactor *
                                    methodFactor
                    )

                val correctedAmpacityPerRun =
                    adjustedBase *
                            effectiveFactors.total()

                val totalAmpacity =
                    correctedAmpacityPerRun *
                            runs

                if (totalAmpacity < designCurrentA) {
                    continue
                }

                val resistance =
                    copperResistance(
                        size,
                        material
                    )

                val reactance =
                    0.08

                val sinPhi =
                    sqrt(
                        (
                            1.0 -
                                    powerFactor *
                                    powerFactor
                        ).coerceAtLeast(0.0)
                    )

                val voltageDropV =
                    if (threePhase) {

                        sqrt(3.0) *
                                designCurrentA *
                                (
                                    resistance *
                                            powerFactor +
                                            reactance *
                                            sinPhi
                                ) *
                                lengthM /
                                1000.0 /
                                runs

                    } else {

                        2.0 *
                                designCurrentA *
                                (
                                    resistance *
                                            powerFactor +
                                            reactance *
                                            sinPhi
                                ) *
                                lengthM /
                                1000.0 /
                                runs
                    }

                val voltageDropPercent =
                    voltageDropV /
                            voltageV *
                            100.0

                if (
                    voltageDropPercent <=
                    maxVoltageDropPercent
                ) {

                    val cable =
                        CableSpecification(
                            sizeMm2 = size,
                            material = material,
                            insulation = insulation,
                            cores = cores,
                            installationMethod =
                                installationMethod,
                            baseAmpacityA =
                                adjustedBase,
                            resistanceOhmPerKm =
                                resistance,
                            reactanceOhmPerKm =
                                reactance
                        )

                    return CableDesignResult(
                        success = true,
                        cable = cable,
                        correctionFactors =
                            effectiveFactors,
                        correctedAmpacityA =
                            totalAmpacity,
                        voltageDropV =
                            voltageDropV,
                        voltageDropPercent =
                            voltageDropPercent,
                        parallelRuns = runs,
                        status = "PASS"
                    )
                }
            }
        }

        return CableDesignResult(
            false,
            null,
            correctionFactors,
            0.0,
            0.0,
            0.0,
            0,
            "NO CABLE CONFIGURATION SATISFIES THE DESIGN LIMITS"
        )
    }

    private fun copperResistance(
        sizeMm2: Double,
        material: CableMaterial
    ): Double {

        if (sizeMm2 <= 0.0) {
            return 0.0
        }

        val copperR =
            18.1 / sizeMm2

        return when (material) {

            CableMaterial.COPPER ->
                copperR

            CableMaterial.ALUMINIUM ->
                copperR * 1.64
        }
    }

    // ============================================================
    // BREAKER AUTO SELECTION
    // ============================================================

    data class BreakerResult(
        val success: Boolean,
        val breaker: Breaker?,
        val status: String
    )

    fun autoSelectBreaker(
        designCurrentA: Double,
        cableAmpacityA: Double,
        faultCurrentKA: Double,
        poles: Int = 4
    ): BreakerResult {

        if (
            designCurrentA <= 0.0 ||
            cableAmpacityA <= 0.0
        ) {
            return BreakerResult(
                false,
                null,
                "INVALID INPUT"
            )
        }

        for (rating in standardBreakerRatings) {

            if (rating < designCurrentA) {
                continue
            }

            /*
             * Protection coordination:
             *
             * Ib <= In <= Iz
             */
            if (rating > cableAmpacityA) {
                continue
            }

            val icu =
                standardIcuKA.firstOrNull {
                    it >= faultCurrentKA
                }

            if (icu != null) {

                return BreakerResult(
                    true,
                    Breaker(
                        ratingA = rating,
                        icuKA = icu,
                        poles = poles
                    ),
                    "PASS"
                )
            }
        }

        return BreakerResult(
            false,
            null,
            "NO BREAKER SATISFIES CURRENT AND Icu REQUIREMENTS"
        )
    }

    // ============================================================
    // SHORT CIRCUIT
    // ============================================================

    fun transformerShortCircuitKA(
        transformerKVA: Double,
        voltageV: Double,
        impedancePercent: Double
    ): Double {

        if (
            transformerKVA <= 0.0 ||
            voltageV <= 0.0 ||
            impedancePercent <= 0.0
        ) {
            return 0.0
        }

        val ratedCurrent =
            threePhaseCurrent(
                transformerKVA,
                voltageV
            )

        return ratedCurrent /
                (impedancePercent / 100.0) /
                1000.0
    }

    /**
     * Approximate downstream fault current.
     *
     * This is a simplified preliminary model.
     * Final project implementation should include transformer,
     * cable impedance, source impedance and motor contribution.
     */
    fun downstreamShortCircuitKA(
        upstreamFaultKA: Double,
        cableLengthM: Double,
        cableSizeMm2: Double,
        parallelRuns: Int = 1
    ): Double {

        if (
            upstreamFaultKA <= 0.0 ||
            cableLengthM < 0.0 ||
            cableSizeMm2 <= 0.0
        ) {
            return 0.0
        }

        val resistance =
            copperResistance(
                cableSizeMm2,
                CableMaterial.COPPER
            ) *
                    cableLengthM /
                    1000.0 /
                    parallelRuns.coerceAtLeast(1)

        val upstreamZ =
            if (upstreamFaultKA > 0.0) {
                1.0 /
                        upstreamFaultKA
            } else {
                0.0
            }

        val totalZ =
            upstreamZ +
                    resistance

        if (totalZ <= 0.0) {
            return 0.0
        }

        return 1.0 /
                totalZ
    }

    // ============================================================
    // AUTOMATIC DESIGN
    // ============================================================

    data class AutomaticDesignResult(
        val load: TotalLoadResult,
        val cable: CableDesignResult,
        val breaker: BreakerResult,
        val faultCurrentKA: Double,
        val overallPass: Boolean,
        val messages: List<String>
    )

    fun autoDesign(
        loads: List<LoadInput>,
        voltageV: Double,
        threePhase: Boolean,
        lengthM: Double,
        diversityFactor: Double,
        coincidenceFactor: Double,
        transformerKVA: Double,
        transformerImpedancePercent: Double,
        material: CableMaterial = CableMaterial.COPPER,
        insulation: InsulationType = InsulationType.XLPE,
        cores: Int = 4,
        installationMethod: InstallationMethod =
            InstallationMethod.CABLE_TRAY,
        correctionFactors: CorrectionFactors =
            CorrectionFactors(),
        maxVoltageDropPercent: Double =
            DEFAULT_MAX_VOLTAGE_DROP_PERCENT
    ): AutomaticDesignResult {

        val messages =
            mutableListOf<String>()

        val load =
            calculateTotalLoad(
                loads = loads,
                voltageV = voltageV,
                threePhase = threePhase,
                diversityFactor =
                    diversityFactor,
                coincidenceFactor =
                    coincidenceFactor
            )

        messages.add(
            "Connected Load = %.2f kW"
                .format(load.connectedKW)
        )

        messages.add(
            "Demand Load = %.2f kW"
                .format(load.demandKW)
        )

        messages.add(
            "Demand = %.2f kVA"
                .format(load.demandKVA)
        )

        messages.add(
            "Design Current = %.2f A"
                .format(load.currentA)
        )

        val faultCurrentKA =
            transformerShortCircuitKA(
                transformerKVA =
                    transformerKVA,
                voltageV =
                    voltageV,
                impedancePercent =
                    transformerImpedancePercent
            )

        messages.add(
            "Transformer Fault Current = %.2f kA"
                .format(faultCurrentKA)
        )

        val cable =
            autoSelectCable(
                designCurrentA =
                    load.currentA,
                lengthM =
                    lengthM,
                voltageV =
                    voltageV,
                powerFactor =
                    load.effectivePF,
                threePhase =
                    threePhase,
                material =
                    material,
                insulation =
                    insulation,
                cores =
                    cores,
                installationMethod =
                    installationMethod,
                correctionFactors =
                    correctionFactors,
                maxVoltageDropPercent =
                    maxVoltageDropPercent
            )

        if (cable.success) {

            messages.add(
                "Cable = %.0f mm²"
                    .format(
                        cable.cable!!.sizeMm2
                    )
            )

            messages.add(
                "Parallel Runs = ${cable.parallelRuns}"
            )

            messages.add(
                "Corrected Iz = %.2f A"
                    .format(
                        cable.correctedAmpacityA
                    )
            )

            messages.add(
                "Voltage Drop = %.2f%%"
                    .format(
                        cable.voltageDropPercent
                    )
            )

        } else {

            messages.add(
                "Cable selection FAILED"
            )
        }

        val breaker =
            if (cable.success) {

                autoSelectBreaker(
                    designCurrentA =
                        load.currentA,
                    cableAmpacityA =
                        cable.correctedAmpacityA,
                    faultCurrentKA =
                        faultCurrentKA
                )

            } else {

                BreakerResult(
                    false,
                    null,
                    "BREAKER NOT CALCULATED"
                )
            }

        if (breaker.success) {

            messages.add(
                "Breaker = ${breaker.breaker!!.ratingA} A"
            )

            messages.add(
                "Icu = %.1f kA"
                    .format(
                        breaker.breaker.icuKA
                    )
            )

        } else {

            messages.add(
                "Breaker selection FAILED"
            )
        }

        val overallPass =
            load.currentA > 0.0 &&
                    cable.success &&
                    breaker.success &&
                    cable.voltageDropPercent <=
                    maxVoltageDropPercent

        messages.add(
            if (overallPass) {
                "OVERALL DESIGN = PASS"
            } else {
                "OVERALL DESIGN = CHECK REQUIRED"
            }
        )

        return AutomaticDesignResult(
            load = load,
            cable = cable,
            breaker = breaker,
            faultCurrentKA = faultCurrentKA,
            overallPass = overallPass,
            messages = messages
        )
    }
}
