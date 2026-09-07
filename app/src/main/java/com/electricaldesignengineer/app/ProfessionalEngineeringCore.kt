package com.electricaldesignengineer.app

import kotlin.math.sqrt

/**
 * Professional engineering calculation foundation.
 *
 * Design philosophy:
 * 1. No hidden engineering assumptions.
 * 2. Every important calculation receives its design inputs explicitly.
 * 3. Results expose PASS / FAIL / DATA_REQUIRED.
 * 4. Manufacturer data is never mixed with engineering formulas.
 * 5. Standard references are attached to the result.
 *
 * IMPORTANT:
 * This class intentionally does NOT contain copied IEC tables.
 * Standard tabulated values must come from licensed/project-approved
 * engineering data sources.
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

        val conductorMaterial:
            EngineeringDesignEngine.CableMaterial,

        val insulation:
            EngineeringDesignEngine.InsulationType,

        val installationMethod:
            EngineeringDesignEngine.InstallationMethod,

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

    /**
     * This repository is deliberately separated from the calculation
     * engine.
     *
     * In the next phase these records will come from the catalog/data
     * database rather than being embedded in the calculator.
     */
    interface CableDataProvider {

        fun availableCables(
            material:
                EngineeringDesignEngine.CableMaterial,

            insulation:
                EngineeringDesignEngine.InsulationType,

            installationMethod:
                EngineeringDesignEngine.InstallationMethod
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

        val validVoltage =
            system.voltageV > 0.0

        val validFrequency =
            system.frequencyHz > 0.0

        val validPF =
            system.powerFactor in 0.01..1.0

        if (!validVoltage) {
            checks += EngineeringCheck(
                name = "System voltage",
                status = EngineeringStatus.FAIL,
                calculatedValue = system.voltageV,
                message = "System voltage must be greater than zero."
            )
        }

        if (!validFrequency) {
            checks += EngineeringCheck(
                name = "Frequency",
                status = EngineeringStatus.FAIL,
                calculatedValue = system.frequencyHz,
                message = "Frequency must be greater than zero."
            )
        }

        if (!validPF) {
            checks += EngineeringCheck(
                name = "Power factor",
                status = EngineeringStatus.FAIL,
                calculatedValue = system.powerFactor,
                message = "Power factor must be between 0.01 and 1.00."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {

            return LoadCalculationResult(
                0.0,
                0.0,
                0.0,
                0.0,
                1.0,
                checks,
                EngineeringTrace(
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
                                1.0 /
                                        (pf * pf) -
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
            message = "Load calculation completed."
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

        val checks = mutableListOf<EngineeringCheck>()

        if (input.designCurrentA <= 0.0) {

            checks += EngineeringCheck(
                name = "Design current",
                status = EngineeringStatus.FAIL,
                calculatedValue = input.designCurrentA,
                unit = "A",
                message = "Design current must be greater than zero.",
                standardCode =
                    EngineeringStandards.cableSelection.code
            )
        }

        if (input.lengthM <= 0.0) {

            checks += EngineeringCheck(
                name = "Cable length",
                status = EngineeringStatus.FAIL,
                calculatedValue = input.lengthM,
                unit = "m",
                message = "Cable length must be greater than zero."
            )
        }

        if (input.maximumVoltageDropPercent <= 0.0) {

            checks += EngineeringCheck(
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

            checks += EngineeringCheck(
                name = "Grouping factor",
                status = EngineeringStatus.FAIL,
                calculatedValue = input.groupingFactor,
                message =
                    "Grouping factor must be greater than 0 and not greater than 1."
            )
        }

        if (
            input.thermalInsulationFactor <= 0.0 ||
            input.thermalInsulationFactor > 1.0
        ) {

            checks += EngineeringCheck(
                name = "Thermal insulation factor",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.thermalInsulationFactor,
                message =
                    "Thermal insulation factor is outside the accepted range."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {

            return CableDesignResult(
                status = EngineeringStatus.FAIL,
                selectedCable = null,
                parallelRuns = 0,
                correctedAmpacityPerRunA = 0.0,
                totalAmpacityA = 0.0,
                voltageDropV = 0.0,
                voltageDropPercent = 0.0,
                checks = checks,
                trace = EngineeringTrace(
                    calculationName = "CABLE DESIGN",
                    standard =
                        EngineeringStandards.cableSelection,
                    checks = checks
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

            checks += EngineeringCheck(
                name = "Cable engineering database",
                status = EngineeringStatus.DATA_REQUIRED,
                message =
                    "No verified cable data is available for the selected construction and installation method.",
                standardCode =
                    EngineeringStandards.cableSelection.code
            )

            return CableDesignResult(
                status = EngineeringStatus.DATA_REQUIRED,
                selectedCable = null,
                parallelRuns = 0,
                correctedAmpacityPerRunA = 0.0,
                totalAmpacityA = 0.0,
                voltageDropV = 0.0,
                voltageDropPercent = 0.0,
                checks = checks,
                trace = EngineeringTrace(
                    calculationName = "CABLE DESIGN",
                    standard =
                        EngineeringStandards.cableSelection,
                    checks = checks,
                    warnings = listOf(
                        "Verified engineering cable data is required."
                    )
                )
            )
        }

        /*
         * Important:
         *
         * Correction factors are deliberately supplied by the caller.
         * The core does not silently invent IEC table values.
         */

        val correctionFactor =
            input.groupingFactor *
                    input.thermalInsulationFactor *
                    input.soilCorrectionFactor

        for (runs in 1..input.maximumParallelRuns) {

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
                    when (input.phaseSystem) {

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
                        name = "Cable ampacity",
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
                        name = "Voltage drop",
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

                checks += ampacityCheck
                checks += voltageDropCheck

                if (
                    ampacityCheck.status ==
                    EngineeringStatus.PASS &&
                    voltageDropCheck.status ==
                    EngineeringStatus.PASS
                ) {

                    val trace =
                        EngineeringTrace(
                            calculationName =
                                "CABLE DESIGN",
                            standard =
                                EngineeringStandards
                                    .cableSelection,
                            checks = checks.toList(),
                            assumptions = listOf(
                                "Correction factors supplied explicitly by the design input.",
                                "Cable electrical characteristics supplied by the selected data provider.",
                                "Final project design must use the applicable project/utility requirements."
                            )
                        )

                    return CableDesignResult(
                        status =
                            EngineeringStatus.PASS,
                        selectedCable = cable,
                        parallelRuns = runs,
                        correctedAmpacityPerRunA =
                            correctedPerRun,
                        totalAmpacityA =
                            totalAmpacity,
                        voltageDropV =
                            dropV,
                        voltageDropPercent =
                            dropPercent,
                        checks = checks.toList(),
                        trace = trace
                    )
                }

                checks.clear()
            }
        }

        val finalCheck =
            EngineeringCheck(
                name = "Cable selection",
                status = EngineeringStatus.FAIL,
                message =
                    "No available cable configuration satisfies the specified design constraints.",
                standardCode =
                    EngineeringStandards
                        .cableSelection
                        .code
            )

        checks += finalCheck

        return CableDesignResult(
            status = EngineeringStatus.FAIL,
            selectedCable = null,
            parallelRuns = 0,
            correctedAmpacityPerRunA = 0.0,
            totalAmpacityA = 0.0,
            voltageDropV = 0.0,
            voltageDropPercent = 0.0,
            checks = checks,
            trace = EngineeringTrace(
                calculationName = "CABLE DESIGN",
                standard =
                    EngineeringStandards.cableSelection,
                checks = checks
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

        val checks = mutableListOf<EngineeringCheck>()

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

        if (input.prospectiveShortCircuitKA < 0.0) {

            checks += EngineeringCheck(
                name = "Short-circuit current",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.prospectiveShortCircuitKA,
                unit = "kA",
                message =
                    "Short-circuit current cannot be negative."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {

            return BreakerDesignResult(
                status = EngineeringStatus.FAIL,
                selectedBreaker = null,
                checks = checks,
                trace = EngineeringTrace(
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
            ).sortedBy {
                it.ratedCurrentA
            }

        if (breakers.isEmpty()) {

            val dataCheck =
                EngineeringCheck(
                    name = "Breaker engineering database",
                    status =
                        EngineeringStatus.DATA_REQUIRED,
                    message =
                        "No verified breaker product data is available."
                )

            checks += dataCheck

            return BreakerDesignResult(
                status =
                    EngineeringStatus.DATA_REQUIRED,
                selectedBreaker = null,
                checks = checks,
                trace = EngineeringTrace(
                    calculationName =
                        "BREAKER DESIGN",
                    standard =
                        EngineeringStandards
                            .circuitBreakers,
                    checks = checks
                )
            )
        }

        for (breaker in breakers) {

            val ibInIz =
                input.designCurrentA <=
                        breaker.ratedCurrentA &&
                        breaker.ratedCurrentA <=
                        input.cableAmpacityA

            val icuCheck =
                breaker.icuKA >=
                        input.prospectiveShortCircuitKA

            if (ibInIz && icuCheck) {

                checks += EngineeringCheck(
                    name = "Ib ≤ In ≤ Iz",
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

                checks += EngineeringCheck(
                    name = "Icu ≥ Ik",
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
                        checks.toList(),
                    trace =
                        EngineeringTrace(
                            calculationName =
                                "BREAKER DESIGN",
                            standard =
                                EngineeringStandards
                                    .circuitBreakers,
                            checks =
                                checks.toList(),
                            assumptions = listOf(
                                "Protection settings and discrimination are separate verification stages.",
                                "Manufacturer product data must be verified against the current catalog revision."
                            )
                        )
                )
            }
        }

        checks += EngineeringCheck(
            name = "Breaker selection",
            status = EngineeringStatus.FAIL,
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
            checks = checks,
            trace = EngineeringTrace(
                calculationName =
                    "BREAKER DESIGN",
                standard =
                    EngineeringStandards
                        .circuitBreakers,
                checks = checks
            )
        )
    }
}
