package com.electricaldesignengineer.app

import kotlin.math.sqrt

/**
 * Professional engineering calculation foundation.
 *
 * Design philosophy:
 *
 * 1. No hidden engineering assumptions.
 * 2. Important engineering inputs are supplied explicitly.
 * 3. Results expose PASS / FAIL / WARNING / DATA_REQUIRED.
 * 4. Manufacturer data is separated from engineering formulas.
 * 5. Standard references are attached to engineering results.
 *
 * IMPORTANT:
 * This class does NOT contain copied IEC tables.
 *
 * Standard tabulated values and manufacturer data must come from
 * licensed/project-approved engineering data sources.
 */
object ProfessionalEngineeringCore {

    // ============================================================
    // SYSTEM
    // ============================================================

    enum class PhaseSystem {
        SINGLE_PHASE,
        THREE_PHASE
    }

    data class SystemInput(
        val voltageV: Double,
        val frequencyHz: Double,
        val phaseSystem: PhaseSystem,
        val powerFactor: Double
    )

    // ============================================================
    // CABLE DESIGN INPUT
    // ============================================================

    data class CableDesignInput(
        val designCurrentA: Double,
        val lengthM: Double,
        val voltageV: Double,
        val powerFactor: Double,
        val phaseSystem: PhaseSystem,

        val conductorMaterial: CableMaterial,

        val insulation: InsulationType,

        val installationMethod: InstallationMethod,

        val numberOfLoadedConductors: Int,

        val ambientTemperatureC: Double,

        val groupingFactor: Double,

        val thermalInsulationFactor: Double,

        val soilCorrectionFactor: Double,

        val maximumVoltageDropPercent: Double,

        val maximumParallelRuns: Int = 8
    )

    // ============================================================
    // CABLE DATA
    // ============================================================

    data class CableData(
        val sizeMm2: Double,
        val baseAmpacityA: Double,
        val resistanceOhmPerKm: Double,
        val reactanceOhmPerKm: Double,
        val source: String,
        val revision: String
    )

    interface CableDataProvider {

        fun availableCables(
            material: CableMaterial,
            insulation: InsulationType,
            installationMethod: InstallationMethod
        ): List<CableData>
    }

    // ============================================================
    // CABLE RESULT
    // ============================================================

    data class CableDesignResult(
        val status: EngineeringStatus,
        val selectedCable: CableData?,
        val parallelRuns: Int,
        val correctedAmpacityPerRunA: Double,
        val totalAmpacityA: Double,
        val voltageDropV: Double,
        val voltageDropPercent: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    // ============================================================
    // BREAKER INPUT
    // ============================================================

    data class BreakerDesignInput(
        val designCurrentA: Double,
        val cableAmpacityA: Double,
        val prospectiveShortCircuitKA: Double,
        val requiredPoles: Int
    )

    // ============================================================
    // BREAKER DATA
    // ============================================================

    data class BreakerData(
        val manufacturerId: String?,
        val productFamily: String?,
        val partNumber: String?,
        val ratedCurrentA: Double,
        val ratedVoltageV: Double,
        val poles: Int,
        val icuKA: Double,
        val icsKA: Double?,
        val source: String,
        val revision: String
    )

    interface BreakerDataProvider {

        fun availableBreakers(
            requiredPoles: Int
        ): List<BreakerData>
    }

    data class BreakerDesignResult(
        val status: EngineeringStatus,
        val selectedBreaker: BreakerData?,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    // ============================================================
    // LOAD CALCULATION
    // ============================================================

    data class LoadInput(
        val name: String,
        val quantity: Double,
        val unitPowerKW: Double,
        val demandFactor: Double,
        val powerFactor: Double
    )

    data class LoadCalculationResult(
        val connectedKW: Double,
        val demandKW: Double,
        val demandKVA: Double,
        val currentA: Double,
        val effectivePowerFactor: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun calculateLoads(
        loads: List<LoadInput>,
        system: SystemInput
    ): LoadCalculationResult {

        if (loads.isEmpty()) {

            return LoadCalculationResult(
                connectedKW = 0.0,
                demandKW = 0.0,
                demandKVA = 0.0,
                currentA = 0.0,
                effectivePowerFactor = 1.0,
                checks = emptyList(),
                trace = EngineeringTrace(
                    calculationName = "LOAD CALCULATION",
                    standard = null,
                    checks = emptyList(),
                    warnings = listOf(
                        "No loads have been entered."
                    )
                )
            )
        }

        val checks = mutableListOf<EngineeringCheck>()

        if (system.voltageV <= 0.0) {

            checks += EngineeringCheck(
                name = "System voltage",
                status = EngineeringStatus.FAIL,
                calculatedValue = system.voltageV,
                unit = "V",
                message =
                    "System voltage must be greater than zero."
            )
        }

        if (system.frequencyHz <= 0.0) {

            checks += EngineeringCheck(
                name = "Frequency",
                status = EngineeringStatus.FAIL,
                calculatedValue = system.frequencyHz,
                unit = "Hz",
                message =
                    "Frequency must be greater than zero."
            )
        }

        if (system.powerFactor !in 0.01..1.0) {

            checks += EngineeringCheck(
                name = "Power factor",
                status = EngineeringStatus.FAIL,
                calculatedValue = system.powerFactor,
                message =
                    "Power factor must be between 0.01 and 1.00."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {

            return LoadCalculationResult(
                connectedKW = 0.0,
                demandKW = 0.0,
                demandKVA = 0.0,
                currentA = 0.0,
                effectivePowerFactor = 1.0,
                checks = checks,
                trace = EngineeringTrace(
                    calculationName = "LOAD CALCULATION",
                    standard = null,
                    checks = checks
                )
            )
        }

        var connectedKW = 0.0
        var demandKW = 0.0
        var totalReactiveKVAR = 0.0

        loads.forEach { load ->

            val quantity =
                load.quantity.coerceAtLeast(0.0)

            val power =
                load.unitPowerKW.coerceAtLeast(0.0)

            val demandFactor =
                load.demandFactor.coerceIn(0.0, 1.0)

            val pf =
                load.powerFactor.coerceIn(0.01, 1.0)

            val connected =
                quantity * power

            val demand =
                connected * demandFactor

            val reactive =
                demand *
                        sqrt(
                            (
                                1.0 / (pf * pf) -
                                        1.0
                            ).coerceAtLeast(0.0)
                        )

            connectedKW += connected
            demandKW += demand
            totalReactiveKVAR += reactive
        }

        val demandKVA =
            sqrt(
                demandKW * demandKW +
                        totalReactiveKVAR *
                        totalReactiveKVAR
            )

        val effectivePF =
            if (demandKVA > 0.0) {
                demandKW / demandKVA
            } else {
                1.0
            }

        val currentA =
            when (system.phaseSystem) {

                PhaseSystem.THREE_PHASE ->
                    threePhaseCurrent(
                        demandKVA,
                        system.voltageV
                    )

                PhaseSystem.SINGLE_PHASE ->
                    singlePhaseCurrent(
                        demandKVA,
                        system.voltageV
                    )
            }

        checks += EngineeringCheck(
            name = "Load calculation",
            status = EngineeringStatus.PASS,
            calculatedValue = demandKVA,
            unit = "kVA",
            message =
                "Load calculation completed."
        )

        return LoadCalculationResult(
            connectedKW = connectedKW,
            demandKW = demandKW,
            demandKVA = demandKVA,
            currentA = currentA,
            effectivePowerFactor = effectivePF,
            checks = checks,
            trace = EngineeringTrace(
                calculationName = "LOAD CALCULATION",
                standard = null,
                checks = checks
            )
        )
    }

    // ============================================================
    // CURRENT
    // ============================================================

    fun threePhaseCurrent(
        kva: Double,
        voltageV: Double
    ): Double {

        if (
            kva <= 0.0 ||
            voltageV <= 0.0
        ) {
            return 0.0
        }

        return kva * 1000.0 /
                (sqrt(3.0) * voltageV)
    }

    fun singlePhaseCurrent(
        kva: Double,
        voltageV: Double
    ): Double {

        if (
            kva <= 0.0 ||
            voltageV <= 0.0
        ) {
            return 0.0
        }

        return kva * 1000.0 /
                voltageV
    }

    // ============================================================
    // CABLE DESIGN
    // ============================================================

    fun designCable(
        input: CableDesignInput,
        provider: CableDataProvider
    ): CableDesignResult {

        val validationChecks =
            mutableListOf<EngineeringCheck>()

        if (input.designCurrentA <= 0.0) {

            validationChecks += EngineeringCheck(
                name = "Design current",
                status = EngineeringStatus.FAIL,
                calculatedValue = input.designCurrentA,
                unit = "A",
                message =
                    "Design current must be greater than zero.",
                standardCode =
                    EngineeringStandards
                        .cableSelection
                        .code
            )
        }

        if (input.lengthM <= 0.0) {

            validationChecks += EngineeringCheck(
                name = "Cable length",
                status = EngineeringStatus.FAIL,
                calculatedValue = input.lengthM,
                unit = "m",
                message =
                    "Cable length must be greater than zero."
            )
        }

        if (input.voltageV <= 0.0) {

            validationChecks += EngineeringCheck(
                name = "Voltage",
                status = EngineeringStatus.FAIL,
                calculatedValue = input.voltageV,
                unit = "V",
                message =
                    "Voltage must be greater than zero."
            )
        }

        if (
            input.powerFactor !in 0.01..1.0
        ) {

            validationChecks += EngineeringCheck(
                name = "Power factor",
                status = EngineeringStatus.FAIL,
                calculatedValue = input.powerFactor,
                message =
                    "Power factor must be between 0.01 and 1.00."
            )
        }

        if (
            input.maximumVoltageDropPercent <= 0.0
        ) {

            validationChecks += EngineeringCheck(
                name = "Maximum voltage drop",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.maximumVoltageDropPercent,
                unit = "%",
                message =
                    "Voltage-drop limit must be greater than zero."
            )
        }

        if (
            input.groupingFactor <= 0.0 ||
            input.groupingFactor > 1.0
        ) {

            validationChecks += EngineeringCheck(
                name = "Grouping factor",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.groupingFactor,
                message =
                    "Grouping factor must be greater than 0 and not greater than 1."
            )
        }

        if (
            input.thermalInsulationFactor <= 0.0 ||
            input.thermalInsulationFactor > 1.0
        ) {

            validationChecks += EngineeringCheck(
                name = "Thermal insulation factor",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.thermalInsulationFactor,
                message =
                    "Thermal insulation factor is outside the accepted range."
            )
        }

        if (
            input.soilCorrectionFactor <= 0.0 ||
            input.soilCorrectionFactor > 1.0
        ) {

            validationChecks += EngineeringCheck(
                name = "Soil correction factor",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.soilCorrectionFactor,
                message =
                    "Soil correction factor must be greater than 0 and not greater than 1."
            )
        }

        if (
            input.maximumParallelRuns < 1
        ) {

            validationChecks += EngineeringCheck(
                name = "Parallel runs",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.maximumParallelRuns.toDouble(),
                message =
                    "Maximum parallel runs must be at least one."
            )
        }

        if (
            validationChecks.any {
                it.status == EngineeringStatus.FAIL
            }
        ) {

            return CableDesignResult(
                status = EngineeringStatus.FAIL,
                selectedCable = null,
                parallelRuns = 0,
                correctedAmpacityPerRunA = 0.0,
                totalAmpacityA = 0.0,
                voltageDropV = 0.0,
                voltageDropPercent = 0.0,
                checks = validationChecks,
                trace = EngineeringTrace(
                    calculationName = "CABLE DESIGN",
                    standard =
                        EngineeringStandards
                            .cableSelection,
                    checks = validationChecks
                )
            )
        }

        val cables =
            provider.availableCables(
                input.conductorMaterial,
                input.insulation,
                input.installationMethod
            ).sortedBy {
                it.sizeMm2
            }

        if (cables.isEmpty()) {

            val dataCheck =
                EngineeringCheck(
                    name =
                        "Cable engineering database",
                    status =
                        EngineeringStatus.DATA_REQUIRED,
                    message =
                        "No verified cable data is available for the selected construction and installation method.",
                    standardCode =
                        EngineeringStandards
                            .cableSelection
                            .code
                )

            return CableDesignResult(
                status =
                    EngineeringStatus.DATA_REQUIRED,
                selectedCable = null,
                parallelRuns = 0,
                correctedAmpacityPerRunA = 0.0,
                totalAmpacityA = 0.0,
                voltageDropV = 0.0,
                voltageDropPercent = 0.0,
                checks =
                    validationChecks +
                            dataCheck,
                trace =
                    EngineeringTrace(
                        calculationName =
                            "CABLE DESIGN",
                        standard =
                            EngineeringStandards
                                .cableSelection,
                        checks =
                            validationChecks +
                                    dataCheck,
                        warnings =
                            listOf(
                                "Verified engineering cable data is required."
                            )
                    )
            )
        }

        val correctionFactor =
            input.groupingFactor *
                    input.thermalInsulationFactor *
                    input.soilCorrectionFactor

        for (
            runs in
            1..input.maximumParallelRuns
        ) {

            for (cable in cables) {

                val correctedPerRun =
                    cable.baseAmpacityA *
                            correctionFactor

                val totalAmpacity =
                    correctedPerRun * runs

                if (
                    totalAmpacity <
                    input.designCurrentA
                ) {
                    continue
                }

                val sinPhi =
                    sqrt(
                        (
                            1.0 -
                                    input.powerFactor *
                                    input.powerFactor
                        ).coerceAtLeast(0.0)
                    )

                val impedanceComponent =
                    cable.resistanceOhmPerKm *
                            input.powerFactor +
                            cable.reactanceOhmPerKm *
                            sinPhi

                val dropV =
                    when (
                        input.phaseSystem
                    ) {

                        PhaseSystem.THREE_PHASE ->

                            sqrt(3.0) *
                                    input.designCurrentA *
                                    impedanceComponent *
                                    input.lengthM /
                                    1000.0 /
                                    runs

                        PhaseSystem.SINGLE_PHASE ->

                            2.0 *
                                    input.designCurrentA *
                                    impedanceComponent *
                                    input.lengthM /
                                    1000.0 /
                                    runs
                    }

                val dropPercent =
                    dropV /
                            input.voltageV *
                            100.0

                val ampacityCheck =
                    EngineeringCheck(
                        name =
                            "Cable ampacity",
                        status =
                            if (
                                totalAmpacity >=
                                input.designCurrentA
                            ) {
                                EngineeringStatus.PASS
                            } else {
                                EngineeringStatus.FAIL
                            },
                        calculatedValue =
                            totalAmpacity,
                        requiredValue =
                            input.designCurrentA,
                        unit = "A",
                        message =
                            "Corrected cable ampacity verification.",
                        standardCode =
                            EngineeringStandards
                                .cableSelection
                                .code,
                        dataSource =
                            "${cable.source} / ${cable.revision}"
                    )

                val voltageDropCheck =
                    EngineeringCheck(
                        name =
                            "Voltage drop",
                        status =
                            if (
                                dropPercent <=
                                input.maximumVoltageDropPercent
                            ) {
                                EngineeringStatus.PASS
                            } else {
                                EngineeringStatus.FAIL
                            },
                        calculatedValue =
                            dropPercent,
                        requiredValue =
                            input.maximumVoltageDropPercent,
                        unit = "%",
                        message =
                            "Voltage-drop verification.",
                        standardCode =
                            EngineeringStandards
                                .cableSelection
                                .code,
                        dataSource =
                            "${cable.source} / ${cable.revision}"
                    )

                val checks =
                    listOf(
                        ampacityCheck,
                        voltageDropCheck
                    )

                if (
                    ampacityCheck.status ==
                    EngineeringStatus.PASS &&
                    voltageDropCheck.status ==
                    EngineeringStatus.PASS
                ) {

                    return CableDesignResult(
                        status =
                            EngineeringStatus.PASS,
                        selectedCable =
                            cable,
                        parallelRuns =
                            runs,
                        correctedAmpacityPerRunA =
                            correctedPerRun,
                        totalAmpacityA =
                            totalAmpacity,
                        voltageDropV =
                            dropV,
                        voltageDropPercent =
                            dropPercent,
                        checks =
                            checks,
                        trace =
                            EngineeringTrace(
                                calculationName =
                                    "CABLE DESIGN",
                                standard =
                                    EngineeringStandards
                                        .cableSelection,
                                checks =
                                    checks,
                                assumptions =
                                    listOf(
                                        "Correction factors are supplied explicitly by the design input.",
                                        "Cable electrical characteristics are supplied by the selected data provider.",
                                        "Final project design must use applicable project and utility requirements."
                                    )
                            )
                    )
                }
            }
        }

        val finalCheck =
            EngineeringCheck(
                name =
                    "Cable selection",
                status =
                    EngineeringStatus.FAIL,
                message =
                    "No available cable configuration satisfies the specified design constraints.",
                standardCode =
                    EngineeringStandards
                        .cableSelection
                        .code
            )

        return CableDesignResult(
            status =
                EngineeringStatus.FAIL,
            selectedCable = null,
            parallelRuns = 0,
            correctedAmpacityPerRunA = 0.0,
            totalAmpacityA = 0.0,
            voltageDropV = 0.0,
            voltageDropPercent = 0.0,
            checks =
                listOf(finalCheck),
            trace =
                EngineeringTrace(
                    calculationName =
                        "CABLE DESIGN",
                    standard =
                        EngineeringStandards
                            .cableSelection,
                    checks =
                        listOf(finalCheck)
                )
        )
    }

    // ============================================================
    // BREAKER DESIGN
    // ============================================================

    fun designBreaker(
        input: BreakerDesignInput,
        provider: BreakerDataProvider
    ): BreakerDesignResult {

        val checks =
            mutableListOf<EngineeringCheck>()

        if (input.designCurrentA <= 0.0) {

            checks += EngineeringCheck(
                name = "Design current",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.designCurrentA,
                unit = "A",
                message =
                    "Design current must be greater than zero."
            )
        }

        if (input.cableAmpacityA <= 0.0) {

            checks += EngineeringCheck(
                name = "Cable ampacity",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.cableAmpacityA,
                unit = "A",
                message =
                    "Verified cable ampacity is required."
            )
        }

        if (
            input.prospectiveShortCircuitKA < 0.0
        ) {

            checks += EngineeringCheck(
                name =
                    "Short-circuit current",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.prospectiveShortCircuitKA,
                unit = "kA",
                message =
                    "Short-circuit current cannot be negative."
            )
        }

        if (
            input.requiredPoles < 1
        ) {

            checks += EngineeringCheck(
                name = "Required poles",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.requiredPoles.toDouble(),
                message =
                    "Required poles must be at least one."
            )
        }

        if (
            checks.any {
                it.status == EngineeringStatus.FAIL
            }
        ) {

            return BreakerDesignResult(
                status =
                    EngineeringStatus.FAIL,
                selectedBreaker = null,
                checks = checks,
                trace =
                    EngineeringTrace(
                        calculationName =
                            "BREAKER DESIGN",
                        standard =
                            EngineeringStandards
                                .circuitBreakers,
                        checks = checks
                    )
            )
        }

        val breakers =
            provider.availableBreakers(
                input.requiredPoles
            )
                .sortedBy {
                    it.ratedCurrentA
                }

        if (breakers.isEmpty()) {

            val dataCheck =
                EngineeringCheck(
                    name =
                        "Breaker engineering database",
                    status =
                        EngineeringStatus.DATA_REQUIRED,
                    message =
                        "No verified breaker product data is available."
                )

            return BreakerDesignResult(
                status =
                    EngineeringStatus.DATA_REQUIRED,
                selectedBreaker = null,
                checks =
                    checks + dataCheck,
                trace =
                    EngineeringTrace(
                        calculationName =
                            "BREAKER DESIGN",
                        standard =
                            EngineeringStandards
                                .circuitBreakers,
                        checks =
                            checks + dataCheck
                    )
            )
        }

        for (breaker in breakers) {

            val currentCoordination =
                input.designCurrentA <=
                        breaker.ratedCurrentA &&
                        breaker.ratedCurrentA <=
                        input.cableAmpacityA

            val breakingCapacity =
                breaker.icuKA >=
                        input.prospectiveShortCircuitKA

            if (
                currentCoordination &&
                breakingCapacity
            ) {

                val currentCheck =
                    EngineeringCheck(
                        name =
                            "Ib ≤ In ≤ Iz",
                        status =
                            EngineeringStatus.PASS,
                        calculatedValue =
                            breaker.ratedCurrentA,
                        requiredValue =
                            input.cableAmpacityA,
                        unit = "A",
                        message =
                            "Breaker rated current is coordinated with design current and cable ampacity.",
                        standardCode =
                            EngineeringStandards
                                .circuitBreakers
                                .code,
                        dataSource =
                            "${breaker.source} / ${breaker.revision}"
                    )

                val icuCheck =
                    EngineeringCheck(
                        name =
                            "Icu ≥ Ik",
                        status =
                            EngineeringStatus.PASS,
                        calculatedValue =
                            breaker.icuKA,
                        requiredValue =
                            input.prospectiveShortCircuitKA,
                        unit = "kA",
                        message =
                            "Breaker ultimate breaking capacity satisfies the specified prospective fault current.",
                        standardCode =
                            EngineeringStandards
                                .circuitBreakers
                                .code,
                        dataSource =
                            "${breaker.source} / ${breaker.revision}"
                    )

                return BreakerDesignResult(
                    status =
                        EngineeringStatus.PASS,
                    selectedBreaker =
                        breaker,
                    checks =
                        listOf(
                            currentCheck,
                            icuCheck
                        ),
                    trace =
                        EngineeringTrace(
                            calculationName =
                                "BREAKER DESIGN",
                            standard =
                                EngineeringStandards
                                    .circuitBreakers,
                            checks =
                                listOf(
                                    currentCheck,
                                    icuCheck
                                ),
                            assumptions =
                                listOf(
                                    "Protection settings and discrimination are separate verification stages.",
                                    "Manufacturer product data must be verified against the current catalog revision."
                                )
                        )
                )
            }
        }

        val finalCheck =
            EngineeringCheck(
                name =
                    "Breaker selection",
                status =
                    EngineeringStatus.FAIL,
                message =
                    "No verified breaker product satisfies the specified current and short-circuit requirements.",
                standardCode =
                    EngineeringStandards
                        .circuitBreakers
                        .code
            )

        return BreakerDesignResult(
            status =
                EngineeringStatus.FAIL,
            selectedBreaker = null,
            checks =
                listOf(finalCheck),
            trace =
                EngineeringTrace(
                    calculationName =
                        "BREAKER DESIGN",
                    standard =
                        EngineeringStandards
                            .circuitBreakers,
                    checks =
                        listOf(finalCheck)
                )
        )
    }

    // ============================================================
    // SHORT CIRCUIT
    // ============================================================

    /**
     * Calculate three-phase prospective short-circuit current.
     *
     * Formula:
     *
     * Ik = V / (sqrt(3) x |Z|)
     *
     * where V is line-to-line voltage.
     *
     * The source impedance must be explicitly available.
     */
    fun calculateShortCircuit(
        input: ShortCircuitInput
    ): ShortCircuitResult {

        val checks =
            mutableListOf<EngineeringCheck>()

        if (
            input.faultType !=
            ShortCircuitFaultType.THREE_PHASE
        ) {

            checks += EngineeringCheck(
                name = "Fault type",
                status =
                    EngineeringStatus.DATA_REQUIRED,
                message =
                    "The selected fault type is not implemented by the present calculation core."
            )

            return shortCircuitDataRequired(
                input = input,
                checks = checks
            )
        }

        if (
            input.source.voltageV <= 0.0
        ) {

            checks += EngineeringCheck(
                name = "Fault calculation voltage",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.source.voltageV,
                unit = "V",
                message =
                    "Calculation voltage must be greater than zero."
            )
        }

        if (
            input.source.transformerKVA != null &&
            input.source.transformerKVA <= 0.0
        ) {

            checks += EngineeringCheck(
                name = "Transformer rating",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.source.transformerKVA,
                unit = "kVA",
                message =
                    "Transformer rating must be greater than zero."
            )
        }

        if (
            input.source.transformerImpedancePercent != null &&
            input.source.transformerImpedancePercent <= 0.0
        ) {

            checks += EngineeringCheck(
                name =
                    "Transformer impedance",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.source
                        .transformerImpedancePercent,
                unit = "%",
                message =
                    "Transformer impedance must be greater than zero."
            )
        }

        if (
            input.source.upstreamShortCircuitKA != null &&
            input.source.upstreamShortCircuitKA <= 0.0
        ) {

            checks += EngineeringCheck(
                name =
                    "Upstream short-circuit level",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.source
                        .upstreamShortCircuitKA,
                unit = "kA",
                message =
                    "Upstream short-circuit current must be greater than zero."
            )
        }

        if (
            checks.any {
                it.status == EngineeringStatus.FAIL
            }
        ) {

            return ShortCircuitResult(
                status =
                    EngineeringStatus.FAIL,
                faultCurrentA = 0.0,
                faultCurrentKA = 0.0,
                equivalentImpedance = null,
                checks = checks,
                trace =
                    EngineeringTrace(
                        calculationName =
                            "SHORT CIRCUIT",
                        standard =
                            EngineeringStandards
                                .shortCircuit,
                        checks = checks
                    )
            )
        }

        /*
         * Build the source impedance.
         */
        val sourceImpedance =
            calculateSourceImpedance(
                input.source
            )

        if (sourceImpedance == null) {

            checks += EngineeringCheck(
                name =
                    "Source impedance data",
                status =
                    EngineeringStatus.DATA_REQUIRED,
                message =
                    "A valid source impedance cannot be determined from the supplied data. Provide transformer data or an upstream fault level with explicit R/X data when downstream calculation is required."
            )

            return shortCircuitDataRequired(
                input = input,
                checks = checks
            )
        }

        var totalR =
            sourceImpedance.resistanceOhm

        var totalX =
            sourceImpedance.reactanceOhm

        /*
         * Add feeder impedance.
         *
         * Parallel identical runs reduce both R and X
         * by the number of runs.
         */
        val cable =
            input.cable

        if (cable != null) {

            if (
                cable.lengthM <= 0.0 ||
                cable.parallelRuns < 1 ||
                cable.resistanceOhmPerKm < 0.0 ||
                cable.reactanceOhmPerKm < 0.0
            ) {

                checks += EngineeringCheck(
                    name =
                        "Feeder impedance data",
                    status =
                        EngineeringStatus.FAIL,
                    message =
                        "Cable length, parallel runs, resistance and reactance must contain valid non-negative engineering data."
                )

                return ShortCircuitResult(
                    status =
                        EngineeringStatus.FAIL,
                    faultCurrentA = 0.0,
                    faultCurrentKA = 0.0,
                    equivalentImpedance = null,
                    checks = checks,
                    trace =
                        EngineeringTrace(
                            calculationName =
                                "SHORT CIRCUIT",
                            standard =
                                EngineeringStandards
                                    .shortCircuit,
                            checks = checks
                        )
                )
            }

            val cableR =
                cable.resistanceOhmPerKm *
                        cable.lengthM /
                        1000.0 /
                        cable.parallelRuns

            val cableX =
                cable.reactanceOhmPerKm *
                        cable.lengthM /
                        1000.0 /
                        cable.parallelRuns

            totalR += cableR
            totalX += cableX

            checks += EngineeringCheck(
                name =
                    "Feeder resistance",
                status =
                    EngineeringStatus.PASS,
                calculatedValue =
                    cableR,
                unit = "Ω",
                message =
                    "Feeder resistance added to the equivalent source impedance."
            )

            checks += EngineeringCheck(
                name =
                    "Feeder reactance",
                status =
                    EngineeringStatus.PASS,
                calculatedValue =
                    cableX,
                unit = "Ω",
                message =
                    "Feeder reactance added to the equivalent source impedance."
            )
        }

        val magnitude =
            sqrt(
                totalR * totalR +
                        totalX * totalX
            )

        if (
            magnitude <= 0.0
        ) {

            checks += EngineeringCheck(
                name =
                    "Equivalent impedance",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    magnitude,
                unit = "Ω",
                message =
                    "Equivalent impedance must be greater than zero."
            )

            return ShortCircuitResult(
                status =
                    EngineeringStatus.FAIL,
                faultCurrentA = 0.0,
                faultCurrentKA = 0.0,
                equivalentImpedance = null,
                checks = checks,
                trace =
                    EngineeringTrace(
                        calculationName =
                            "SHORT CIRCUIT",
                        standard =
                            EngineeringStandards
                                .shortCircuit,
                        checks = checks
                    )
            )
        }

        val equivalent =
            ShortCircuitImpedance(
                resistanceOhm =
                    totalR,
                reactanceOhm =
                    totalX,
                magnitudeOhm =
                    magnitude
            )

        val faultCurrentA =
            input.source.voltageV /
                    (sqrt(3.0) * magnitude)

        val faultCurrentKA =
            faultCurrentA / 1000.0

        checks += EngineeringCheck(
            name =
                "Equivalent impedance",
            status =
                EngineeringStatus.PASS,
            calculatedValue =
                magnitude,
            unit = "Ω",
            message =
                "Equivalent positive-sequence impedance magnitude used for the three-phase fault calculation."
        )

        checks += EngineeringCheck(
            name =
                "Prospective short-circuit current",
            status =
                EngineeringStatus.PASS,
            calculatedValue =
                faultCurrentKA,
            unit = "kA",
            message =
                "Three-phase prospective short-circuit current calculated from the supplied source and feeder impedance."
        )

        return ShortCircuitResult(
            status =
                EngineeringStatus.PASS,
            faultCurrentA =
                faultCurrentA,
            faultCurrentKA =
                faultCurrentKA,
            equivalentImpedance =
                equivalent,
            checks =
                checks,
            trace =
                EngineeringTrace(
                    calculationName =
                        "SHORT CIRCUIT",
                    standard =
                        EngineeringStandards
                            .shortCircuit,
                    checks =
                        checks,
                    assumptions =
                        listOf(
                            "Three-phase balanced fault calculation.",
                            "Line-to-line voltage is used with Ik = V/(sqrt(3) x |Z|).",
                            "Cable R and X are supplied by engineering data.",
                            "Parallel identical feeder runs are represented by dividing R and X by the number of runs.",
                            "No transformer impedance percentage is assumed."
                        )
                )
        )
    }

    /**
     * Determine source impedance from explicitly supplied source data.
     */
    private fun calculateSourceImpedance(
        source: ShortCircuitSourceInput
    ): ShortCircuitImpedance? {

        /*
         * Case 1:
         *
         * Explicit transformer R% and X%.
         */
        if (
            source.transformerKVA != null &&
            source.transformerResistancePercent != null &&
            source.transformerReactancePercent != null
        ) {

            if (
                source.transformerKVA <= 0.0 ||
                source.transformerResistancePercent < 0.0 ||
                source.transformerReactancePercent < 0.0
            ) {
                return null
            }

            val baseZ =
                source.voltageV *
                        source.voltageV /
                        (source.transformerKVA * 1000.0)

            val r =
                baseZ *
                        source.transformerResistancePercent /
                        100.0

            val x =
                baseZ *
                        source.transformerReactancePercent /
                        100.0

            return ShortCircuitImpedance(
                resistanceOhm = r,
                reactanceOhm = x,
                magnitudeOhm =
                    sqrt(r * r + x * x)
            )
        }

        /*
         * Case 2:
         *
         * Transformer %Z only.
         *
         * This is sufficient for source-bus fault current.
         *
         * If a downstream cable exists, the complex source impedance
         * cannot be reconstructed from %Z alone without an R/X split.
         *
         * Therefore we represent the transformer impedance as a
         * magnitude only and return R=0, X=Z only when no downstream
         * feeder is involved.
         *
         * The caller of calculateShortCircuit prevents unsupported
         * downstream use by requiring explicit R/X below.
         */
        if (
            source.transformerKVA != null &&
            source.transformerImpedancePercent != null
        ) {

            if (
                source.transformerKVA <= 0.0 ||
                source.transformerImpedancePercent <= 0.0
            ) {
                return null
            }

            val baseZ =
                source.voltageV *
                        source.voltageV /
                        (source.transformerKVA * 1000.0)

            val z =
                baseZ *
                        source.transformerImpedancePercent /
                        100.0

            return ShortCircuitImpedance(
                resistanceOhm = 0.0,
                reactanceOhm = z,
                magnitudeOhm = z
            )
        }

        /*
         * Case 3:
         *
         * Explicit upstream R/X.
         */
        if (
            source.upstreamResistanceOhm != null &&
            source.upstreamReactanceOhm != null
        ) {

            if (
                source.upstreamResistanceOhm < 0.0 ||
                source.upstreamReactanceOhm < 0.0
            ) {
                return null
            }

            val r =
                source.upstreamResistanceOhm

            val x =
                source.upstreamReactanceOhm

            return ShortCircuitImpedance(
                resistanceOhm = r,
                reactanceOhm = x,
                magnitudeOhm =
                    sqrt(r * r + x * x)
            )
        }

        /*
         * Case 4:
         *
         * Upstream fault current alone gives impedance magnitude.
         *
         * This is sufficient only for a fault calculation at that
         * same bus. It cannot provide the required R/X split for
         * a downstream complex-impedance calculation.
         */
        if (
            source.upstreamShortCircuitKA != null
        ) {

            val ikA =
                source.upstreamShortCircuitKA

            if (
                ikA <= 0.0
            ) {
                return null
            }

            val z =
                source.voltageV /
                        (sqrt(3.0) *
                                ikA *
                                1000.0)

            return ShortCircuitImpedance(
                resistanceOhm = 0.0,
                reactanceOhm = z,
                magnitudeOhm = z
            )
        }

        return null
    }

    /**
     * Helper used when source data is incomplete.
     */
    private fun shortCircuitDataRequired(
        input: ShortCircuitInput,
        checks: List<EngineeringCheck>
    ): ShortCircuitResult {

        return ShortCircuitResult(
            status =
                EngineeringStatus.DATA_REQUIRED,
            faultCurrentA = 0.0,
            faultCurrentKA = 0.0,
            equivalentImpedance = null,
            checks = checks,
            trace =
                EngineeringTrace(
                    calculationName =
                        "SHORT CIRCUIT",
                    standard =
                        EngineeringStandards
                            .shortCircuit,
                    checks =
                        checks,
                    warnings =
                        listOf(
                            "Additional source impedance data is required before a professional downstream short-circuit calculation can be completed."
                        )
                )
        )
    }

    // ============================================================
    // PROTECTION CHECK
    // ============================================================

    /**
     * Verify basic protective-device coordination.
     *
     * Checks:
     *
     * Ib <= In <= Iz
     *
     * Icu >= Ik
     *
     * Ics >= Ik when Ics is supplied.
     *
     * This is NOT a full discrimination/selectivity study.
     */
    fun checkProtection(
        input: ProtectionCheckInput
    ): ProtectionCheckResult {

        val checks =
            mutableListOf<EngineeringCheck>()

        if (
            input.designCurrentA <= 0.0
        ) {

            checks += EngineeringCheck(
                name =
                    "Design current Ib",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.designCurrentA,
                unit = "A",
                message =
                    "Design current must be greater than zero."
            )
        }

        if (
            input.cableAmpacityA <= 0.0
        ) {

            checks += EngineeringCheck(
                name =
                    "Cable ampacity Iz",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.cableAmpacityA,
                unit = "A",
                message =
                    "Verified cable ampacity is required."
            )
        }

        if (
            input.breakerRatedCurrentA <= 0.0
        ) {

            checks += EngineeringCheck(
                name =
                    "Breaker rated current In",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.breakerRatedCurrentA,
                unit = "A",
                message =
                    "Breaker rated current must be greater than zero."
            )
        }

        if (
            input.prospectiveShortCircuitKA < 0.0
        ) {

            checks += EngineeringCheck(
                name =
                    "Prospective short-circuit current Ik",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.prospectiveShortCircuitKA,
                unit = "kA",
                message =
                    "Prospective short-circuit current cannot be negative."
            )
        }

        if (
            input.breakerIcuKA <= 0.0
        ) {

            checks += EngineeringCheck(
                name =
                    "Breaker Icu",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.breakerIcuKA,
                unit = "kA",
                message =
                    "Breaker Icu must be greater than zero."
            )
        }

        if (
            input.breakerIcsKA != null &&
            input.breakerIcsKA < 0.0
        ) {

            checks += EngineeringCheck(
                name =
                    "Breaker Ics",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.breakerIcsKA,
                unit = "kA",
                message =
                    "Breaker Ics cannot be negative."
            )
        }

        if (
            checks.any {
                it.status == EngineeringStatus.FAIL
            }
        ) {

            return ProtectionCheckResult(
                status =
                    EngineeringStatus.FAIL,
                overloadProtectionPass =
                    false,
                breakingCapacityPass =
                    false,
                serviceBreakingCapacityPass =
                    null,
                checks =
                    checks,
                trace =
                    EngineeringTrace(
                        calculationName =
                            "PROTECTION CHECK",
                        standard =
                            EngineeringStandards
                                .circuitBreakers,
                        checks =
                            checks
                    )
            )
        }

        val overloadPass =
            input.designCurrentA <=
                    input.breakerRatedCurrentA &&
                    input.breakerRatedCurrentA <=
                    input.cableAmpacityA

        val breakingPass =
            input.breakerIcuKA >=
                    input.prospectiveShortCircuitKA

        val servicePass =
            input.breakerIcsKA?.let {
                it >=
                        input.prospectiveShortCircuitKA
            }

        checks += EngineeringCheck(
            name =
                "Ib ≤ In ≤ Iz",
            status =
                if (overloadPass) {
                    EngineeringStatus.PASS
                } else {
                    EngineeringStatus.FAIL
                },
            calculatedValue =
                input.breakerRatedCurrentA,
            requiredValue =
                input.cableAmpacityA,
            unit = "A",
            message =
                if (overloadPass) {
                    "Basic current coordination passed."
                } else {
                    "Basic current coordination failed."
                },
            standardCode =
                EngineeringStandards
                    .circuitBreakers
                    .code
        )

        checks += EngineeringCheck(
            name =
                "Icu ≥ Ik",
            status =
                if (breakingPass) {
                    EngineeringStatus.PASS
                } else {
                    EngineeringStatus.FAIL
                },
            calculatedValue =
                input.breakerIcuKA,
            requiredValue =
                input.prospectiveShortCircuitKA,
            unit = "kA",
            message =
                if (breakingPass) {
                    "Ultimate breaking capacity is adequate for the specified prospective fault current."
                } else {
                    "Ultimate breaking capacity is insufficient for the specified prospective fault current."
                },
            standardCode =
                EngineeringStandards
                    .circuitBreakers
                    .code
        )

        if (
            input.breakerIcsKA != null
        ) {

            checks += EngineeringCheck(
                name =
                    "Ics ≥ Ik",
                status =
                    if (servicePass == true) {
                        EngineeringStatus.PASS
                    } else {
                        EngineeringStatus.FAIL
                    },
                calculatedValue =
                    input.breakerIcsKA,
                requiredValue =
                    input.prospectiveShortCircuitKA,
                unit = "kA",
                message =
                    if (servicePass == true) {
                        "Service breaking capacity satisfies the specified prospective fault current."
                    } else {
                        "Service breaking capacity is insufficient for the specified prospective fault current."
                    },
                standardCode =
                    EngineeringStandards
                        .circuitBreakers
                        .code
            )
        }

        val overallPass =
            overloadPass &&
                    breakingPass &&
                    (
                            servicePass == null ||
                                    servicePass
                            )

        val overallStatus =
            if (overallPass) {
                EngineeringStatus.PASS
            } else {
                EngineeringStatus.FAIL
            }

        return ProtectionCheckResult(
            status =
                overallStatus,
            overloadProtectionPass =
                overloadPass,
            breakingCapacityPass =
                breakingPass,
            serviceBreakingCapacityPass =
                servicePass,
            checks =
                checks,
            trace =
                EngineeringTrace(
                    calculationName =
                        "PROTECTION CHECK",
                    standard =
                        EngineeringStandards
                            .circuitBreakers,
                    checks =
                        checks,
                    assumptions =
                        listOf(
                            "This check verifies basic current coordination and breaking capacity.",
                            "Discrimination/selectivity, backup/cascading and protection settings require separate engineering studies.",
                            "Manufacturer data must be verified against the applicable current product catalog."
                        )
                )
        )
    }
}
