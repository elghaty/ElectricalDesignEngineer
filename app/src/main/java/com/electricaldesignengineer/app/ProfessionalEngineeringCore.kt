package com.electricaldesignengineer.app

import kotlin.math.sqrt

/**
 * Professional engineering calculation foundation.
 *
 * Architecture:
 *
 * UI
 *   ↓
 * AutoDesignService
 *   ↓
 * ProfessionalEngineeringCore
 *   ↓
 * Engineering data providers / catalog
 *
 * This object is the single source of engineering calculations.
 *
 * IMPORTANT:
 * - No hardcoded manufacturer ratings.
 * - No guessed cable or transformer data.
 * - No hidden engineering assumptions.
 * - Missing engineering data returns DATA_REQUIRED.
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
    // LOAD
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

        val checks = mutableListOf<EngineeringCheck>()

        // --------------------------------------------------------
        // EMPTY LOAD LIST
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // SYSTEM VALIDATION
        // --------------------------------------------------------

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

        // --------------------------------------------------------
        // LOAD DATA VALIDATION
        //
        // Do NOT silently correct engineering input.
        // Invalid data must be reported to the user.
        // --------------------------------------------------------

        loads.forEach { load ->

            if (load.name.isBlank()) {
                checks += EngineeringCheck(
                    name = "Load name",
                    status = EngineeringStatus.FAIL,
                    message =
                        "Load name cannot be empty."
                )
            }

            if (load.quantity <= 0.0) {
                checks += EngineeringCheck(
                    name = "Load quantity: ${load.name}",
                    status = EngineeringStatus.FAIL,
                    calculatedValue = load.quantity,
                    unit = "",
                    message =
                        "Load quantity must be greater than zero."
                )
            }

            if (load.unitPowerKW <= 0.0) {
                checks += EngineeringCheck(
                    name = "Unit power: ${load.name}",
                    status = EngineeringStatus.FAIL,
                    calculatedValue = load.unitPowerKW,
                    unit = "kW",
                    message =
                        "Unit power must be greater than zero."
                )
            }

            if (load.demandFactor !in 0.0..1.0) {
                checks += EngineeringCheck(
                    name = "Demand factor: ${load.name}",
                    status = EngineeringStatus.FAIL,
                    calculatedValue = load.demandFactor,
                    message =
                        "Demand factor must be between 0.00 and 1.00."
                )
            }

            if (load.powerFactor !in 0.01..1.0) {
                checks += EngineeringCheck(
                    name = "Power factor: ${load.name}",
                    status = EngineeringStatus.FAIL,
                    calculatedValue = load.powerFactor,
                    message =
                        "Power factor must be between 0.01 and 1.00."
                )
            }
        }

        // --------------------------------------------------------
        // STOP ON INVALID DATA
        // --------------------------------------------------------

        if (
            checks.any {
                it.status == EngineeringStatus.FAIL
            }
        ) {
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
                    checks = checks,
                    warnings = listOf(
                        "Invalid load data must be corrected before engineering calculation."
                    )
                )
            )
        }

        // --------------------------------------------------------
        // LOAD CALCULATION
        // --------------------------------------------------------

        var connectedKW = 0.0
        var demandKW = 0.0
        var totalReactiveKVAR = 0.0

        loads.forEach { load ->

            val connected =
                load.quantity *
                        load.unitPowerKW

            val demand =
                connected *
                        load.demandFactor

            val reactive =
                demand *
                        sqrt(
                            1.0 /
                                    (
                                        load.powerFactor *
                                                load.powerFactor
                                    ) -
                                    1.0
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
                (
                    sqrt(3.0) *
                            voltageV
                    )
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
    // CABLE
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

    fun designCable(
        input: CableDesignInput,
        provider: CableDataProvider
    ): CableDesignResult {

        val checks = mutableListOf<EngineeringCheck>()

        fun fail(
            name: String,
            value: Double?,
            unit: String,
            message: String
        ) {
            checks += EngineeringCheck(
                name = name,
                status = EngineeringStatus.FAIL,
                calculatedValue = value,
                unit = unit,
                message = message
            )
        }

        // --------------------------------------------------------
        // INPUT VALIDATION
        // --------------------------------------------------------

        if (input.designCurrentA <= 0.0) {
            fail(
                "Design current",
                input.designCurrentA,
                "A",
                "Design current must be greater than zero."
            )
        }

        if (input.lengthM <= 0.0) {
            fail(
                "Cable length",
                input.lengthM,
                "m",
                "Cable length must be greater than zero."
            )
        }

        if (input.voltageV <= 0.0) {
            fail(
                "Voltage",
                input.voltageV,
                "V",
                "Voltage must be greater than zero."
            )
        }

        if (input.powerFactor !in 0.01..1.0) {
            fail(
                "Power factor",
                input.powerFactor,
                "",
                "Power factor must be between 0.01 and 1.00."
            )
        }

        if (input.numberOfLoadedConductors < 1) {
            fail(
                "Loaded conductors",
                input.numberOfLoadedConductors.toDouble(),
                "",
                "Number of loaded conductors must be at least one."
            )
        }

        if (input.ambientTemperatureC.isNaN()) {
            fail(
                "Ambient temperature",
                null,
                "°C",
                "Ambient temperature must be a valid engineering value."
            )
        }

        if (input.maximumVoltageDropPercent <= 0.0) {
            fail(
                "Maximum voltage drop",
                input.maximumVoltageDropPercent,
                "%",
                "Voltage-drop limit must be greater than zero."
            )
        }

        if (
            input.groupingFactor <= 0.0 ||
            input.groupingFactor > 1.0
        ) {
            fail(
                "Grouping factor",
                input.groupingFactor,
                "",
                "Grouping factor must be greater than 0 and not greater than 1."
            )
        }

        if (
            input.thermalInsulationFactor <= 0.0 ||
            input.thermalInsulationFactor > 1.0
        ) {
            fail(
                "Thermal insulation factor",
                input.thermalInsulationFactor,
                "",
                "Thermal insulation factor must be greater than 0 and not greater than 1."
            )
        }

        if (
            input.soilCorrectionFactor <= 0.0 ||
            input.soilCorrectionFactor > 1.0
        ) {
            fail(
                "Soil correction factor",
                input.soilCorrectionFactor,
                "",
                "Soil correction factor must be greater than 0 and not greater than 1."
            )
        }

        if (input.maximumParallelRuns < 1) {
            fail(
                "Maximum parallel runs",
                input.maximumParallelRuns.toDouble(),
                "",
                "Maximum parallel runs must be at least one."
            )
        }

        if (
            checks.any {
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
                checks = checks,
                trace = EngineeringTrace(
                    calculationName = "CABLE DESIGN",
                    standard =
                        EngineeringStandards.cableSelection,
                    checks = checks
                )
            )
        }

        // --------------------------------------------------------
        // VERIFIED CABLE DATABASE
        // --------------------------------------------------------

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
                    name = "Cable engineering database",
                    status =
                        EngineeringStatus.DATA_REQUIRED,
                    message =
                        "No verified cable data is available for the selected construction and installation method.",
                    standardCode =
                        EngineeringStandards.cableSelection.code
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
                    checks + dataCheck,
                trace =
                    EngineeringTrace(
                        calculationName = "CABLE DESIGN",
                        standard =
                            EngineeringStandards.cableSelection,
                        checks =
                            checks + dataCheck,
                        warnings =
                            listOf(
                                "Verified engineering cable data is required."
                            )
                    )
            )
        }

        // --------------------------------------------------------
        // CORRECTION FACTOR
        //
        // The actual correction factors must be supplied by the
        // engineering data/catalog layer.
        // --------------------------------------------------------

        val correctionFactor =
            input.groupingFactor *
                    input.thermalInsulationFactor *
                    input.soilCorrectionFactor

        // --------------------------------------------------------
        // CABLE SELECTION
        // --------------------------------------------------------

        for (
            runs in
            1..input.maximumParallelRuns
        ) {

            for (cable in cables) {

                val correctedPerRun =
                    cable.baseAmpacityA *
                            correctionFactor

                val totalAmpacity =
                    correctedPerRun *
                            runs

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

                if (
                    ampacityCheck.status ==
                    EngineeringStatus.PASS &&
                    voltageDropCheck.status ==
                    EngineeringStatus.PASS
                ) {

                    val resultChecks =
                        listOf(
                            ampacityCheck,
                            voltageDropCheck
                        )

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
                            resultChecks,
                        trace =
                            EngineeringTrace(
                                calculationName =
                                    "CABLE DESIGN",
                                standard =
                                    EngineeringStandards
                                        .cableSelection,
                                checks =
                                    resultChecks,
                                assumptions =
                                    listOf(
                                        "Correction factors are supplied explicitly.",
                                        "Cable electrical characteristics are supplied by the engineering data provider.",
                                        "No generic manufacturer data is invented."
                                    )
                            )
                    )
                }
            }
        }

        val finalCheck =
            EngineeringCheck(
                name = "Cable selection",
                status = EngineeringStatus.FAIL,
                message =
                    "No available cable configuration satisfies the specified design constraints.",
                standardCode =
                    EngineeringStandards.cableSelection.code
            )

        return CableDesignResult(
            status = EngineeringStatus.FAIL,
            selectedCable = null,
            parallelRuns = 0,
            correctedAmpacityPerRunA = 0.0,
            totalAmpacityA = 0.0,
            voltageDropV = 0.0,
            voltageDropPercent = 0.0,
            checks = listOf(finalCheck),
            trace = EngineeringTrace(
                calculationName = "CABLE DESIGN",
                standard =
                    EngineeringStandards.cableSelection,
                checks =
                    listOf(finalCheck)
            )
        )
    }

    // ============================================================
    // BREAKER
    // ============================================================

    data class BreakerDesignInput(
        val designCurrentA: Double,
        val cableAmpacityA: Double,
        val prospectiveShortCircuitKA: Double,
        val requiredPoles: Int
    )

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

    fun designBreaker(
        input: BreakerDesignInput,
        provider: BreakerDataProvider
    ): BreakerDesignResult {

        val checks =
            mutableListOf<EngineeringCheck>()

        // --------------------------------------------------------
        // INPUT VALIDATION
        // --------------------------------------------------------

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
                name = "Short-circuit current",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.prospectiveShortCircuitKA,
                unit = "kA",
                message =
                    "Short-circuit current cannot be negative."
            )
        }

        if (input.requiredPoles < 1) {
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

        // --------------------------------------------------------
        // VERIFIED BREAKER DATABASE
        // --------------------------------------------------------

        val breakers =
            provider.availableBreakers(
                input.requiredPoles
            ).filter {
                it.poles >=
                        input.requiredPoles
            }.filter {
                it.ratedCurrentA > 0.0
            }.filter {
                it.icuKA > 0.0
            }.sortedBy {
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

        // --------------------------------------------------------
        // BREAKER SELECTION
        // --------------------------------------------------------

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

                val icuCheck =
                    EngineeringCheck(
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
                                    "Breaker voltage compatibility must be verified from the project system voltage and manufacturer catalog."
                                )
                        )
                )
            }
        }

        val finalCheck =
            EngineeringCheck(
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
            status = EngineeringStatus.FAIL,
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
    // TRANSFORMER DESIGN
    // ============================================================

    data class TransformerData(
        val id: String,
        val manufacturerId: String,
        val manufacturerName: String,
        val catalogId: String,
        val catalogName: String,
        val catalogRevision: String,
        val productFamily: String,
        val partNumber: String?,
        val ratedPowerKVA: Double,
        val primaryVoltageV: Double,
        val secondaryVoltageV: Double,
        val frequencyHz: Double,
        val vectorGroup: String?,
        val impedancePercent: Double?,
        val noLoadLossKW: Double?,
        val loadLossKW: Double?,
        val coolingClass: String?,
        val standardCode: String?,
        val sourceUrl: String?,
        val verified: Boolean
    )

    data class TransformerDesignInput(
        val requiredKVA: Double,
        val designMarginPercent: Double = 0.0,
        val requiredPrimaryVoltageV: Double? = null,
        val requiredSecondaryVoltageV: Double? = null,
        val requiredFrequencyHz: Double? = null
    )

    data class TransformerDesignResult(
        val status: EngineeringStatus,
        val selectedTransformer: TransformerData?,
        val requiredKVA: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun designTransformer(
        input: TransformerDesignInput,
        transformers: List<TransformerData>
    ): TransformerDesignResult {

        val checks =
            mutableListOf<EngineeringCheck>()

        if (input.requiredKVA <= 0.0) {

            val check =
                EngineeringCheck(
                    name =
                        "Required transformer capacity",
                    status =
                        EngineeringStatus.FAIL,
                    calculatedValue =
                        input.requiredKVA,
                    unit = "kVA",
                    message =
                        "Required transformer capacity must be greater than zero."
                )

            return TransformerDesignResult(
                status =
                    EngineeringStatus.FAIL,
                selectedTransformer = null,
                requiredKVA = 0.0,
                checks = listOf(check),
                trace =
                    EngineeringTrace(
                        calculationName =
                            "TRANSFORMER DESIGN",
                        standard = null,
                        checks = listOf(check)
                    )
            )
        }

        if (input.designMarginPercent < 0.0) {

            checks += EngineeringCheck(
                name = "Design margin",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.designMarginPercent,
                unit = "%",
                message =
                    "Design margin cannot be negative."
            )
        }

        if (
            input.requiredPrimaryVoltageV != null &&
            input.requiredPrimaryVoltageV <= 0.0
        ) {
            checks += EngineeringCheck(
                name = "Primary voltage",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.requiredPrimaryVoltageV,
                unit = "V",
                message =
                    "Required primary voltage must be greater than zero."
            )
        }

        if (
            input.requiredSecondaryVoltageV != null &&
            input.requiredSecondaryVoltageV <= 0.0
        ) {
            checks += EngineeringCheck(
                name = "Secondary voltage",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.requiredSecondaryVoltageV,
                unit = "V",
                message =
                    "Required secondary voltage must be greater than zero."
            )
        }

        if (
            input.requiredFrequencyHz != null &&
            input.requiredFrequencyHz <= 0.0
        ) {
            checks += EngineeringCheck(
                name = "Transformer frequency",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.requiredFrequencyHz,
                unit = "Hz",
                message =
                    "Required frequency must be greater than zero."
            )
        }

        if (
            checks.any {
                it.status == EngineeringStatus.FAIL
            }
        ) {
            return TransformerDesignResult(
                status =
                    EngineeringStatus.FAIL,
                selectedTransformer = null,
                requiredKVA = 0.0,
                checks = checks,
                trace =
                    EngineeringTrace(
                        calculationName =
                            "TRANSFORMER DESIGN",
                        standard = null,
                        checks = checks
                    )
            )
        }

        val requiredWithMargin =
            input.requiredKVA *
                    (
                        1.0 +
                                input.designMarginPercent /
                                100.0
                        )

        val candidates =
            transformers
                .filter {
                    it.verified
                }
                .filter {
                    it.ratedPowerKVA >=
                            requiredWithMargin
                }
                .filter {
                    input.requiredPrimaryVoltageV == null ||
                            kotlin.math.abs(
                                it.primaryVoltageV -
                                        input.requiredPrimaryVoltageV
                            ) < 0.01
                }
                .filter {
                    input.requiredSecondaryVoltageV == null ||
                            kotlin.math.abs(
                                it.secondaryVoltageV -
                                        input.requiredSecondaryVoltageV
                            ) < 0.01
                }
                .filter {
                    input.requiredFrequencyHz == null ||
                            kotlin.math.abs(
                                it.frequencyHz -
                                        input.requiredFrequencyHz
                            ) < 0.01
                }
                .sortedBy {
                    it.ratedPowerKVA
                }

        if (candidates.isEmpty()) {

            val dataCheck =
                EngineeringCheck(
                    name =
                        "Transformer engineering database",
                    status =
                        EngineeringStatus.DATA_REQUIRED,
                    requiredValue =
                        requiredWithMargin,
                    unit = "kVA",
                    message =
                        "No verified transformer in the engineering catalog satisfies the required capacity and voltage/frequency constraints."
                )

            return TransformerDesignResult(
                status =
                    EngineeringStatus.DATA_REQUIRED,
                selectedTransformer = null,
                requiredKVA =
                    requiredWithMargin,
                checks =
                    checks + dataCheck,
                trace =
                    EngineeringTrace(
                        calculationName =
                            "TRANSFORMER DESIGN",
                        standard = null,
                        checks =
                            checks + dataCheck,
                        warnings =
                            listOf(
                                "A verified manufacturer or project-approved transformer catalog is required."
                            )
                    )
            )
        }

        val selected =
            candidates.first()

        val capacityCheck =
            EngineeringCheck(
                name = "Transformer capacity",
                status =
                    EngineeringStatus.PASS,
                calculatedValue =
                    selected.ratedPowerKVA,
                requiredValue =
                    requiredWithMargin,
                unit = "kVA",
                message =
                    "Selected transformer capacity satisfies the required design capacity.",
                dataSource =
                    "${selected.manufacturerName} / ${selected.catalogName} / ${selected.catalogRevision}"
            )

        val verificationCheck =
            EngineeringCheck(
                name = "Catalog verification",
                status =
                    if (selected.verified) {
                        EngineeringStatus.PASS
                    } else {
                        EngineeringStatus.DATA_REQUIRED
                    },
                message =
                    "Transformer selection is based on verified catalog data.",
                dataSource =
                    "${selected.manufacturerName} / ${selected.catalogName}"
            )

        checks += capacityCheck
        checks += verificationCheck

        return TransformerDesignResult(
            status =
                EngineeringStatus.PASS,
            selectedTransformer =
                selected,
            requiredKVA =
                requiredWithMargin,
            checks =
                checks,
            trace =
                EngineeringTrace(
                    calculationName =
                        "TRANSFORMER DESIGN",
                    standard = null,
                    checks =
                        checks,
                    assumptions =
                        listOf(
                            "Only verified transformer catalog records are eligible.",
                            "The smallest verified transformer satisfying the design requirement is selected.",
                            "No hardcoded transformer rating list is used.",
                            "Manufacturer and catalog information remains external to the calculation formula."
                        )
                )
        )
    }

    // ============================================================
    // SHORT CIRCUIT
    // ============================================================

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
                input,
                checks
            )
        }

        if (input.source.voltageV <= 0.0) {

            checks += EngineeringCheck(
                name = "Fault calculation voltage",
                status = EngineeringStatus.FAIL,
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
                status = EngineeringStatus.FAIL,
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
                name = "Transformer impedance",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.source.transformerImpedancePercent,
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
                name = "Upstream short-circuit level",
                status = EngineeringStatus.FAIL,
                calculatedValue =
                    input.source.upstreamShortCircuitKA,
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

        val cable =
            input.cable

        /*
         * Transformer %Z or upstream fault current alone gives
         * impedance magnitude only.
         *
         * It must NOT be used as an artificial R/X split when
         * downstream cable impedance is being added.
         */

        val hasDownstreamCable =
            cable != null

        if (
            hasDownstreamCable &&
            input.source.transformerKVA != null &&
            input.source.transformerImpedancePercent != null &&
            (
                input.source.transformerResistancePercent == null ||
                        input.source.transformerReactancePercent == null
                )
        ) {

            val dataCheck =
                EngineeringCheck(
                    name = "Transformer R/X data",
                    status =
                        EngineeringStatus.DATA_REQUIRED,
                    message =
                        "Transformer %Z alone is insufficient for a downstream complex-impedance calculation. Provide transformer R% and X% or equivalent source R/X data."
                )

            return shortCircuitDataRequired(
                input,
                checks + dataCheck
            )
        }

        if (
            hasDownstreamCable &&
            input.source.upstreamShortCircuitKA != null &&
            (
                input.source.upstreamResistanceOhm == null ||
                        input.source.upstreamReactanceOhm == null
                )
        ) {

            val dataCheck =
                EngineeringCheck(
                    name = "Upstream R/X data",
                    status =
                        EngineeringStatus.DATA_REQUIRED,
                    message =
                        "Upstream fault current alone gives impedance magnitude. Explicit upstream R/X data is required before adding downstream cable impedance."
                )

            return shortCircuitDataRequired(
                input,
                checks + dataCheck
            )
        }

        val sourceImpedance =
            calculateSourceImpedance(
                input.source
            )

        if (sourceImpedance == null) {

            val dataCheck =
                EngineeringCheck(
                    name = "Source impedance data",
                    status =
                        EngineeringStatus.DATA_REQUIRED,
                    message =
                        "A valid source impedance cannot be determined from the supplied data."
                )

            return shortCircuitDataRequired(
                input,
                checks + dataCheck
            )
        }

        var totalR =
            sourceImpedance.resistanceOhm

        var totalX =
            sourceImpedance.reactanceOhm

        if (cable != null) {

            if (
                cable.lengthM <= 0.0 ||
                cable.parallelRuns < 1 ||
                cable.resistanceOhmPerKm < 0.0 ||
                cable.reactanceOhmPerKm < 0.0
            ) {

                val fail =
                    EngineeringCheck(
                        name = "Feeder impedance data",
                        status =
                            EngineeringStatus.FAIL,
                        message =
                            "Cable length, parallel runs, resistance and reactance must contain valid engineering data."
                    )

                return ShortCircuitResult(
                    status =
                        EngineeringStatus.FAIL,
                    faultCurrentA = 0.0,
                    faultCurrentKA = 0.0,
                    equivalentImpedance = null,
                    checks =
                        checks + fail,
                    trace =
                        EngineeringTrace(
                            calculationName =
                                "SHORT CIRCUIT",
                            standard =
                                EngineeringStandards
                                    .shortCircuit,
                            checks =
                                checks + fail
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
                name = "Feeder resistance",
                status =
                    EngineeringStatus.PASS,
                calculatedValue =
                    cableR,
                unit = "Ω",
                message =
                    "Feeder resistance added to equivalent source impedance."
            )

            checks += EngineeringCheck(
                name = "Feeder reactance",
                status =
                    EngineeringStatus.PASS,
                calculatedValue =
                    cableX,
                unit = "Ω",
                message =
                    "Feeder reactance added to equivalent source impedance."
            )
        }

        val magnitude =
            sqrt(
                totalR * totalR +
                        totalX * totalX
            )

        if (magnitude <= 0.0) {

            val fail =
                EngineeringCheck(
                    name = "Equivalent impedance",
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
                checks =
                    checks + fail,
                trace =
                    EngineeringTrace(
                        calculationName =
                            "SHORT CIRCUIT",
                        standard =
                            EngineeringStandards
                                .shortCircuit,
                        checks =
                            checks + fail
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
                    (
                        sqrt(3.0) *
                                magnitude
                        )

        val faultCurrentKA =
            faultCurrentA / 1000.0

        checks += EngineeringCheck(
            name = "Equivalent impedance",
            status =
                EngineeringStatus.PASS,
            calculatedValue =
                magnitude,
            unit = "Ω",
            message =
                "Equivalent positive-sequence impedance magnitude used for the three-phase fault calculation."
        )

        checks += EngineeringCheck(
            name = "Prospective short-circuit current",
            status =
                EngineeringStatus.PASS,
            calculatedValue =
                faultCurrentKA,
            unit = "kA",
            message =
                "Three-phase prospective short-circuit current calculated from supplied source and feeder impedance."
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
                            "Ik = V/(sqrt(3) × |Z|).",
                            "Cable R and X are supplied by engineering data.",
                            "Parallel identical feeder runs reduce R and X by the number of runs.",
                            "No transformer R/X split is invented."
                        )
                )
        )
    }

    private fun calculateSourceImpedance(
        source: ShortCircuitSourceInput
    ): ShortCircuitImpedance? {

        // --------------------------------------------------------
        // EXPLICIT TRANSFORMER R% + X%
        // --------------------------------------------------------

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
                        (
                            source.transformerKVA *
                                    1000.0
                            )

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
                    sqrt(
                        r * r +
                                x * x
                    )
            )
        }

        // --------------------------------------------------------
        // TRANSFORMER %Z ONLY
        //
        // Allowed only for source-bus calculation.
        // --------------------------------------------------------

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
                        (
                            source.transformerKVA *
                                    1000.0
                            )

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

        // --------------------------------------------------------
        // EXPLICIT UPSTREAM R/X
        // --------------------------------------------------------

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
                    sqrt(
                        r * r +
                                x * x
                    )
            )
        }

        // --------------------------------------------------------
        // UPSTREAM FAULT LEVEL ONLY
        // --------------------------------------------------------

        if (
            source.upstreamShortCircuitKA != null
        ) {

            val ikA =
                source.upstreamShortCircuitKA

            if (ikA <= 0.0) {
                return null
            }

            val z =
                source.voltageV /
                        (
                            sqrt(3.0) *
                                    ikA *
                                    1000.0
                            )

            return ShortCircuitImpedance(
                resistanceOhm = 0.0,
                reactanceOhm = z,
                magnitudeOhm = z
            )
        }

        return null
    }

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
    // PROTECTION
    // ============================================================

    fun checkProtection(
        input: ProtectionCheckInput
    ): ProtectionCheckResult {

        val checks =
            mutableListOf<EngineeringCheck>()

        if (input.designCurrentA <= 0.0) {

            checks += EngineeringCheck(
                name = "Design current Ib",
                status =
                    EngineeringStatus.FAIL,
                calculatedValue =
                    input.designCurrentA,
                unit = "A",
                message =
                    "Design current must be greater than zero."
            )
        }

        if (input.cableAmpacityA <= 0.0) {

            checks += EngineeringCheck(
                name = "Cable ampacity Iz",
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
                name = "Breaker rated current In",
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

        if (input.breakerIcuKA <= 0.0) {

            checks += EngineeringCheck(
                name = "Breaker Icu",
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
                name = "Breaker Ics",
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
                overloadProtectionPass = false,
                breakingCapacityPass = false,
                serviceBreakingCapacityPass = null,
                checks = checks,
                trace =
                    EngineeringTrace(
                        calculationName =
                            "PROTECTION CHECK",
                        standard =
                            EngineeringStandards
                                .circuitBreakers,
                        checks = checks
                    )
            )
        }

        // --------------------------------------------------------
        // BASIC CURRENT COORDINATION
        // --------------------------------------------------------

        val overloadPass =
            input.designCurrentA <=
                    input.breakerRatedCurrentA &&
                    input.breakerRatedCurrentA <=
                    input.cableAmpacityA

        // --------------------------------------------------------
        // BREAKING CAPACITY
        // --------------------------------------------------------

        val breakingPass =
            input.breakerIcuKA >=
                    input.prospectiveShortCircuitKA

        // --------------------------------------------------------
        // SERVICE BREAKING CAPACITY
        // --------------------------------------------------------

        val servicePass =
            input.breakerIcsKA?.let {
                it >=
                        input.prospectiveShortCircuitKA
            }

        checks += EngineeringCheck(
            name = "Ib ≤ In ≤ Iz",
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
            name = "Icu ≥ Ik",
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
                    "Ultimate breaking capacity is adequate."
                } else {
                    "Ultimate breaking capacity is insufficient."
                },
            standardCode =
                EngineeringStandards
                    .circuitBreakers
                    .code
        )

        if (input.breakerIcsKA != null) {

            checks += EngineeringCheck(
                name = "Ics ≥ Ik",
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
                        "Service breaking capacity is adequate."
                    } else {
                        "Service breaking capacity is insufficient."
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

        return ProtectionCheckResult(
            status =
                if (overallPass) {
                    EngineeringStatus.PASS
                } else {
                    EngineeringStatus.FAIL
                },
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
                            "This is a basic protection coordination check.",
                            "Discrimination/selectivity requires a separate study.",
                            "Manufacturer data must be verified against the applicable catalog revision."
                        )
                )
        )
    }
}
