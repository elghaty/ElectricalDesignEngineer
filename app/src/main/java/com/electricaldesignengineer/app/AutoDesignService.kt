package com.electricaldesignengineer.app

/**
 * AutoDesignService
 *
 * Main automatic electrical design orchestrator.
 *
 * Architecture:
 *
 * UI
 *   ↓
 * AutoDesignService
 *   ↓
 * ProfessionalEngineeringCore
 *   ↓
 * EngineeringCatalogRepository
 *
 * Engineering sequence:
 *
 * Loads
 *   ↓
 * Demand calculation
 *   ↓
 * Transformer selection
 *   ↓
 * Transformer short-circuit level
 *   ↓
 * Cable selection
 *   ↓
 * Breaker selection
 *   ↓
 * Protection checks
 *
 * IMPORTANT:
 * - No EngineeringDesignEngine.
 * - No invented transformer impedance.
 * - No invented short-circuit current.
 * - No forced 400 V system voltage.
 * - No forced 50 Hz design frequency.
 * - Cable is automatically selected by ProfessionalEngineeringCore.
 * - Breaker is automatically selected by ProfessionalEngineeringCore.
 * - Transformer is automatically selected from the verified catalog.
 * - Engineer can override the automatically selected cable later from the UI.
 *
 * Engineering scope:
 * - Automatic preliminary feeder design.
 * - Demand/current calculation.
 * - Verified transformer selection.
 * - Transformer secondary bus short-circuit calculation.
 * - Automatic cable selection.
 * - Automatic breaker selection.
 * - Basic Ib <= In <= Iz coordination.
 * - Basic Icu/Ics verification.
 *
 * This service does NOT claim to replace:
 * - Full IEC 60909 short-circuit studies.
 * - Protection selectivity/coordination studies.
 * - Detailed motor starting studies.
 * - Complete earthing-grid design.
 * - Detailed harmonic studies.
 */
object AutoDesignService {

    // ============================================================
    // FEEDER RESULT
    // ============================================================

    data class FeederResult(
        val nodeId: String,
        val nodeName: String,
        val designCurrentA: Double,
        val cableSizeMm2: Double?,
        val parallelRuns: Int,
        val ampacityIzA: Double?,
        val voltageDropPercent: Double?,
        val breakerRatingA: Double?,
        val breakerIcuKA: Double?,
        val breakerIcsKA: Double?,
        val shortCircuitCurrentKA: Double?,
        val protectionPassed: Boolean,
        val valid: Boolean,
        val message: String,
        val warnings: List<String> = emptyList()
    )

    // ============================================================
    // SYSTEM RESULT
    // ============================================================

    data class SystemResult(
        val system: DistributionSystem,
        val feeders: List<FeederResult>,
        val valid: Boolean,
        val message: String,
        val warnings: List<String> = emptyList()
    )

    // ============================================================
    // TRANSFORMER RESULT
    // ============================================================

    data class TransformerResult(
        val requiredKVA: Double,
        val selectedTransformer:
            ProfessionalEngineeringCore.TransformerData?,
        val selectedKVA: Double?,
        val primaryVoltageV: Double?,
        val secondaryVoltageV: Double?,
        val frequencyHz: Double?,
        val impedancePercent: Double?,
        val status: EngineeringStatus,
        val valid: Boolean,
        val message: String,
        val warnings: List<String> = emptyList()
    )

    // ============================================================
    // CABLE DATA PROVIDER
    // ============================================================

    private val cableProvider =
        object : ProfessionalEngineeringCore.CableDataProvider {

            override fun availableCables(
                material: CableMaterial,
                insulation: InsulationType,
                installationMethod: InstallationMethod
            ): List<ProfessionalEngineeringCore.CableData> {

                return EngineeringCatalogRepository.getCableData(
                    material = material,
                    insulation = insulation,
                    installationMethod = installationMethod
                )
            }
        }

    // ============================================================
    // BREAKER DATA PROVIDER
    // ============================================================

    private val breakerProvider =
        object : ProfessionalEngineeringCore.BreakerDataProvider {

            override fun availableBreakers(
                requiredPoles: Int
            ): List<ProfessionalEngineeringCore.BreakerData> {

                return EngineeringCatalogRepository.getBreakerData(
                    requiredPoles = requiredPoles
                )
            }
        }

    // ============================================================
    // VERIFIED TRANSFORMER DATA
    // ============================================================

    private fun getTransformerData():
        List<ProfessionalEngineeringCore.TransformerData> {

        return EngineeringCatalogRepository
            .allTransformers()
            .filter { transformer ->

                transformer.verified &&
                        transformer.ratedPowerKVA > 0.0 &&
                        transformer.primaryVoltageV > 0.0 &&
                        transformer.secondaryVoltageV > 0.0 &&
                        transformer.frequencyHz > 0.0 &&
                        transformer.impedancePercent != null &&
                        transformer.impedancePercent > 0.0
            }
            .map { transformer ->

                ProfessionalEngineeringCore.TransformerData(
                    id = transformer.id,
                    manufacturerId = transformer.manufacturerId,
                    manufacturerName = transformer.manufacturerName,
                    catalogId = transformer.catalogId,
                    catalogName = transformer.catalogName,
                    catalogRevision = transformer.catalogRevision,
                    productFamily = transformer.productFamily,
                    partNumber = transformer.partNumber,
                    ratedPowerKVA = transformer.ratedPowerKVA,
                    primaryVoltageV = transformer.primaryVoltageV,
                    secondaryVoltageV = transformer.secondaryVoltageV,
                    frequencyHz = transformer.frequencyHz,
                    vectorGroup = transformer.vectorGroup,
                    impedancePercent = transformer.impedancePercent,
                    noLoadLossKW = transformer.noLoadLossKW,
                    loadLossKW = transformer.loadLossKW,
                    coolingClass = transformer.coolingClass,
                    standardCode = transformer.standardCode,
                    sourceUrl = transformer.sourceUrl,
                    verified = transformer.verified
                )
            }
    }

    // ============================================================
    // TRANSFORMER DESIGN
    // ============================================================

    fun designTransformer(
        demandKVA: Double,
        designMarginPercent: Double = 0.0,
        primaryVoltageV: Double? = null,
        secondaryVoltageV: Double? = null,
        frequencyHz: Double? = null
    ): TransformerResult {

        if (demandKVA <= 0.0) {

            return TransformerResult(
                requiredKVA = demandKVA,
                selectedTransformer = null,
                selectedKVA = null,
                primaryVoltageV = null,
                secondaryVoltageV = null,
                frequencyHz = null,
                impedancePercent = null,
                status = EngineeringStatus.DATA_REQUIRED,
                valid = false,
                message =
                    "DATA_REQUIRED: transformer demand is required.",
                warnings =
                    listOf(
                        "Required transformer demand must be greater than zero."
                    )
            )
        }

        if (designMarginPercent < 0.0) {

            return TransformerResult(
                requiredKVA = demandKVA,
                selectedTransformer = null,
                selectedKVA = null,
                primaryVoltageV = null,
                secondaryVoltageV = null,
                frequencyHz = null,
                impedancePercent = null,
                status = EngineeringStatus.DATA_REQUIRED,
                valid = false,
                message =
                    "DATA_REQUIRED: transformer design margin cannot be negative.",
                warnings = emptyList()
            )
        }

        val verifiedTransformers =
            getTransformerData()

        if (verifiedTransformers.isEmpty()) {

            return TransformerResult(
                requiredKVA = demandKVA *
                        (1.0 + designMarginPercent / 100.0),
                selectedTransformer = null,
                selectedKVA = null,
                primaryVoltageV = null,
                secondaryVoltageV = null,
                frequencyHz = null,
                impedancePercent = null,
                status = EngineeringStatus.DATA_REQUIRED,
                valid = false,
                message =
                    "DATA_REQUIRED: no verified transformer catalog data is available.",
                warnings =
                    listOf(
                        "A verified transformer record with valid impedance data is required."
                    )
            )
        }

        val input =
            ProfessionalEngineeringCore.TransformerDesignInput(
                requiredKVA = demandKVA,
                designMarginPercent = designMarginPercent,
                requiredPrimaryVoltageV = primaryVoltageV,
                requiredSecondaryVoltageV = secondaryVoltageV,
                requiredFrequencyHz = frequencyHz
            )

        val result =
            ProfessionalEngineeringCore.designTransformer(
                input = input,
                transformers = verifiedTransformers
            )

        val transformer =
            result.selectedTransformer

        return TransformerResult(
            requiredKVA = result.requiredKVA,
            selectedTransformer = transformer,
            selectedKVA = transformer?.ratedPowerKVA,
            primaryVoltageV = transformer?.primaryVoltageV,
            secondaryVoltageV = transformer?.secondaryVoltageV,
            frequencyHz = transformer?.frequencyHz,
            impedancePercent = transformer?.impedancePercent,
            status = result.status,
            valid = result.status == EngineeringStatus.PASS,
            message =
                when (result.status) {

                    EngineeringStatus.PASS ->
                        "PASS: transformer selected successfully."

                    EngineeringStatus.DATA_REQUIRED ->
                        "DATA_REQUIRED: verified transformer catalog data is required."

                    EngineeringStatus.FAIL ->
                        "FAIL: no transformer satisfies the specified requirements."

                    EngineeringStatus.WARNING ->
                        "WARNING: transformer selection requires engineering review."

                    EngineeringStatus.NOT_CALCULATED ->
                        "Transformer selection was not calculated."
                },
            warnings =
                result.trace.warnings
        )
    }

    // ============================================================
    // MAIN SYSTEM DESIGN
    // ============================================================

    fun designSystem(
        system: DistributionSystem
    ): SystemResult {

        val systemWarnings =
            mutableListOf<String>()

        val feederResults =
            mutableListOf<FeederResult>()

        return try {

            // ----------------------------------------------------
            // 1. Validate system
            // ----------------------------------------------------

            val validationResult =
                system.validate()

            if (!validationResult.isValid) {

                return SystemResult(
                    system = system,
                    feeders = emptyList(),
                    valid = false,
                    message =
                        "DATA_REQUIRED: distribution system validation failed.",
                    warnings =
                        (
                            validationResult.errors +
                                    validationResult.warnings
                            ).distinct()
                )
            }

            systemWarnings +=
                validationResult.warnings

            // ----------------------------------------------------
            // 2. Calculate total demand
            // ----------------------------------------------------

            val demandKVA =
                system.totalDemandLoadKVA()

            if (demandKVA <= 0.0) {

                return SystemResult(
                    system = system,
                    feeders = emptyList(),
                    valid = false,
                    message =
                        "DATA_REQUIRED: total demand load is required.",
                    warnings =
                        listOf(
                            "No valid demand load was found."
                        )
                )
            }

            // ----------------------------------------------------
            // 3. Transformer node
            //
            // We require the actual configured voltage.
            // We do NOT silently replace it with 400 V.
            // ----------------------------------------------------

            val transformerNode =
                system.getTransformer()

            if (transformerNode == null) {

                return SystemResult(
                    system = system,
                    feeders = emptyList(),
                    valid = false,
                    message =
                        "DATA_REQUIRED: transformer node is required.",
                    warnings =
                        listOf(
                            "Automatic design requires a configured transformer node."
                        )
                )
            }

            val transformerVoltage =
                transformerNode.voltage
                    .takeIf { it > 0.0 }

            if (transformerVoltage == null) {

                return SystemResult(
                    system = system,
                    feeders = emptyList(),
                    valid = false,
                    message =
                        "DATA_REQUIRED: transformer secondary voltage is required.",
                    warnings =
                        listOf(
                            "Enter the actual transformer secondary/system voltage."
                        )
                )
            }

            // ----------------------------------------------------
            // 4. Transformer selection
            //
            // Frequency is intentionally left null here.
            // The verified catalog determines the transformer's
            // actual frequency instead of the service inventing
            // 50 Hz.
            // ----------------------------------------------------

            val transformerDesign =
                designTransformer(
                    demandKVA = demandKVA,
                    designMarginPercent = 0.0,
                    primaryVoltageV = null,
                    secondaryVoltageV = transformerVoltage,
                    frequencyHz = null
                )

            if (
                !transformerDesign.valid ||
                transformerDesign.selectedTransformer == null
            ) {

                return SystemResult(
                    system = system,
                    feeders = emptyList(),
                    valid = false,
                    message =
                        transformerDesign.message,
                    warnings =
                        (
                            transformerDesign.warnings +
                                    "Automatic design cannot continue without a verified compatible transformer."
                            ).distinct()
                )
            }

            val selectedTransformer =
                transformerDesign.selectedTransformer

            // ----------------------------------------------------
            // 5. Transformer secondary short circuit
            //
            // This is the transformer secondary bus fault level.
            //
            // It uses the actual verified transformer impedance.
            // No invented fault current is used.
            // ----------------------------------------------------

            val transformerShortCircuit =
                calculateTransformerShortCircuit(
                    transformer = selectedTransformer,
                    voltageV =
                        selectedTransformer.secondaryVoltageV
                )

            if (
                transformerShortCircuit == null ||
                transformerShortCircuit.status != EngineeringStatus.PASS ||
                transformerShortCircuit.faultCurrentKA <= 0.0
            ) {

                return SystemResult(
                    system = system,
                    feeders = emptyList(),
                    valid = false,
                    message =
                        "DATA_REQUIRED: transformer short-circuit data is required.",
                    warnings =
                        listOf(
                            "The selected transformer does not contain sufficient verified impedance data to calculate the secondary short-circuit level."
                        )
                )
            }

            val transformerShortCircuitKA =
                transformerShortCircuit.faultCurrentKA

            // ----------------------------------------------------
            // 6. Design every feeder
            // ----------------------------------------------------

            system.nodes
                .filter { node ->
                    node.type != DistributionNodeType.TRANSFORMER
                }
                .forEach { node ->

                    val feeder =
                        node.feeder

                    if (feeder == null) {

                        feederResults += FeederResult(
                            nodeId = node.id,
                            nodeName = node.name,
                            designCurrentA = 0.0,
                            cableSizeMm2 = null,
                            parallelRuns = 0,
                            ampacityIzA = null,
                            voltageDropPercent = null,
                            breakerRatingA = null,
                            breakerIcuKA = null,
                            breakerIcsKA = null,
                            shortCircuitCurrentKA = null,
                            protectionPassed = false,
                            valid = false,
                            message =
                                "DATA_REQUIRED: feeder data is missing."
                        )

                    } else {

                        val result =
                            designFeeder(
                                system = system,
                                node = node,
                                feeder = feeder,
                                sourceShortCircuitKA =
                                    transformerShortCircuitKA
                            )

                        feederResults += result

                        systemWarnings +=
                            result.warnings
                    }
                }

            // ----------------------------------------------------
            // 7. Final system status
            // ----------------------------------------------------

            val valid =
                feederResults.isNotEmpty() &&
                        feederResults.all {
                            it.valid
                        }

            val message =
                if (valid) {

                    "PASS: automatic electrical design completed successfully."

                } else {

                    "Design completed with one or more engineering issues."
                }

            SystemResult(
                system = system,
                feeders = feederResults,
                valid = valid,
                message = message,
                warnings =
                    (
                        listOf(
                            "Selected transformer: ${selectedTransformer.ratedPowerKVA} kVA.",
                            "Transformer secondary short-circuit level: ${transformerShortCircuitKA} kA."
                        ) +
                                systemWarnings
                        )
                        .distinct()
            )

        } catch (exception: Exception) {

            SystemResult(
                system = system,
                feeders = feederResults,
                valid = false,
                message =
                    "DESIGN_ERROR: ${
                        exception.message
                            ?: "Unknown engineering calculation error."
                    }",
                warnings =
                    systemWarnings
                        .plus(
                            "Automatic design calculation failed."
                        )
                        .distinct()
            )
        }
    }

    // ============================================================
    // TRANSFORMER SHORT CIRCUIT
    // ============================================================

    private fun calculateTransformerShortCircuit(
        transformer:
            ProfessionalEngineeringCore.TransformerData,
        voltageV: Double
    ): ShortCircuitResult? {

        val kva =
            transformer.ratedPowerKVA
                .takeIf { it > 0.0 }
                ?: return null

        val impedancePercent =
            transformer.impedancePercent
                ?.takeIf { it > 0.0 }
                ?: return null

        if (voltageV <= 0.0) {
            return null
        }

        val input =
            ShortCircuitInput(
                faultType =
                    ShortCircuitFaultType.THREE_PHASE,

                source =
                    ShortCircuitSourceInput(
                        transformerKVA = kva,
                        voltageV = voltageV,
                        transformerImpedancePercent =
                            impedancePercent
                    ),

                cable = null
            )

        return ProfessionalEngineeringCore.calculateShortCircuit(
            input = input
        )
    }

    // ============================================================
    // FEEDER DESIGN
    // ============================================================

    private fun designFeeder(
        system: DistributionSystem,
        node: DistributionNode,
        feeder: FeederDesign,
        sourceShortCircuitKA: Double
    ): FeederResult {

        val warnings =
            mutableListOf<String>()

        // --------------------------------------------------------
        // 1. Design current
        // --------------------------------------------------------

        val designCurrent =
            calculateDesignCurrent(
                node = node,
                feeder = feeder
            )

        if (designCurrent <= 0.0) {

            return emptyResult(
                node = node,
                message =
                    "DATA_REQUIRED: valid design current is required."
            )
        }

        feeder.designCurrentIb =
            designCurrent

        // --------------------------------------------------------
        // 2. Feeder length
        // --------------------------------------------------------

        if (feeder.lengthMeters <= 0.0) {

            return emptyResult(
                node = node,
                designCurrent = designCurrent,
                message =
                    "DATA_REQUIRED: feeder length must be greater than zero."
            )
        }

        // --------------------------------------------------------
        // 3. Voltage
        // --------------------------------------------------------

        if (node.voltage <= 0.0) {

            return emptyResult(
                node = node,
                designCurrent = designCurrent,
                message =
                    "DATA_REQUIRED: feeder system voltage is required."
            )
        }

        // --------------------------------------------------------
        // 4. Phase system
        // --------------------------------------------------------

        val phaseSystem =
            when (node.phaseType) {

                PhaseType.SINGLE_PHASE_L1,
                PhaseType.SINGLE_PHASE_L2,
                PhaseType.SINGLE_PHASE_L3 ->
                    ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE

                PhaseType.THREE_PHASE ->
                    ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE
            }

        // --------------------------------------------------------
        // 5. Loaded conductors
        //
        // This value describes the current-carrying conductors.
        // It is not used as an invented correction factor.
        // --------------------------------------------------------

        val loadedConductors =
            when (phaseSystem) {

                ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE ->
                    3

                ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE ->
                    2
            }

        // --------------------------------------------------------
        // 6. Power factor
        // --------------------------------------------------------

        val powerFactor =
            determinePowerFactor(node)

        if (powerFactor <= 0.0) {

            return emptyResult(
                node = node,
                designCurrent = designCurrent,
                message =
                    "DATA_REQUIRED: valid power factor is required."
            )
        }

        // --------------------------------------------------------
        // 7. Correction factor
        //
        // The total factor must be supplied by the feeder design
        // data. The service does not invent separate IEC correction
        // factors.
        // --------------------------------------------------------

        val correctionFactor =
            feeder.correctionFactorTotal

        if (
            correctionFactor <= 0.0 ||
            correctionFactor > 1.0
        ) {

            return emptyResult(
                node = node,
                designCurrent = designCurrent,
                message =
                    "DATA_REQUIRED: valid cable correction factor is required."
            )
        }

        // --------------------------------------------------------
        // 8. Cable search limit
        //
        // This is only an algorithmic search limit.
        // It is NOT an engineering assumption about the number
        // of parallel cables required.
        // --------------------------------------------------------

        val maximumParallelRuns =
            8

        // --------------------------------------------------------
        // 9. Maximum voltage drop
        //
        // Do not silently convert a missing value to 5%.
        // --------------------------------------------------------

        val maximumVoltageDrop =
            feeder.maximumVoltageDropPercent

        if (maximumVoltageDrop <= 0.0) {

            return emptyResult(
                node = node,
                designCurrent = designCurrent,
                message =
                    "DATA_REQUIRED: maximum permissible voltage drop is required."
            )
        }

        // --------------------------------------------------------
        // 10. Cable design input
        //
        // Ambient temperature is kept at the reference condition
        // expected by the embedded cable data. Actual correction
        // is represented by correctionFactorTotal.
        //
        // It is NOT used as a hidden engineering temperature
        // correction.
        // --------------------------------------------------------

        val cableInput =
            ProfessionalEngineeringCore.CableDesignInput(

                designCurrentA =
                    designCurrent,

                lengthM =
                    feeder.lengthMeters,

                voltageV =
                    node.voltage,

                powerFactor =
                    powerFactor,

                phaseSystem =
                    phaseSystem,

                conductorMaterial =
                    feeder.cableMaterial,

                insulation =
                    feeder.insulation,

                installationMethod =
                    feeder.installationMethod,

                numberOfLoadedConductors =
                    loadedConductors,

                ambientTemperatureC =
                    30.0,

                groupingFactor =
                    correctionFactor,

                thermalInsulationFactor =
                    1.0,

                soilCorrectionFactor =
                    1.0,

                maximumVoltageDropPercent =
                    maximumVoltageDrop,

                maximumParallelRuns =
                    maximumParallelRuns
            )

        // --------------------------------------------------------
        // 11. Automatic cable selection
        // --------------------------------------------------------

        val cableResult =
            ProfessionalEngineeringCore.designCable(
                input = cableInput,
                provider = cableProvider
            )

        val selectedCable =
            cableResult.selectedCable

        if (
            cableResult.status != EngineeringStatus.PASS ||
            selectedCable == null
        ) {

            return FeederResult(
                nodeId = node.id,
                nodeName = node.name,
                designCurrentA = designCurrent,
                cableSizeMm2 = null,
                parallelRuns =
                    cableResult.parallelRuns,
                ampacityIzA =
                    cableResult.totalAmpacityA
                        .takeIf { it > 0.0 },
                voltageDropPercent =
                    cableResult.voltageDropPercent
                        .takeIf { it >= 0.0 },
                breakerRatingA = null,
                breakerIcuKA = null,
                breakerIcsKA = null,
                shortCircuitCurrentKA =
                    sourceShortCircuitKA,
                protectionPassed = false,
                valid = false,
                message =
                    cableStatusMessage(
                        cableResult.status
                    ),
                warnings =
                    cableResult.trace.warnings
            )
        }

        // --------------------------------------------------------
        // 12. Store selected cable
        // --------------------------------------------------------

        feeder.conductorSizeMm2 =
            selectedCable.sizeMm2

        feeder.ampacityIz =
            cableResult.totalAmpacityA

        feeder.parallelRuns =
            cableResult.parallelRuns

        feeder.voltageDropPercent =
            cableResult.voltageDropPercent

        feeder.cableSelectedAutomatically =
            true

        // --------------------------------------------------------
        // 13. Short circuit
        //
        // The transformer secondary bus fault level is used
        // conservatively for breaker interrupting-capacity
        // selection.
        //
        // This is NOT claimed to be the actual fault current
        // at the end of the feeder.
        // --------------------------------------------------------

        val shortCircuitKA =
            sourceShortCircuitKA

        if (shortCircuitKA <= 0.0) {

            return FeederResult(
                nodeId = node.id,
                nodeName = node.name,
                designCurrentA = designCurrent,
                cableSizeMm2 =
                    feeder.conductorSizeMm2,
                parallelRuns =
                    feeder.parallelRuns,
                ampacityIzA =
                    feeder.ampacityIz,
                voltageDropPercent =
                    feeder.voltageDropPercent,
                breakerRatingA = null,
                breakerIcuKA = null,
                breakerIcsKA = null,
                shortCircuitCurrentKA = null,
                protectionPassed = false,
                valid = false,
                message =
                    "DATA_REQUIRED: valid transformer/source short-circuit level is required.",
                warnings =
                    warnings.distinct()
            )
        }

        feeder.shortCircuitCurrentKA =
            shortCircuitKA

        // --------------------------------------------------------
        // 14. Breaker poles
        //
        // 3-phase feeders use 3P because the current verified
        // breaker catalog is primarily based on 3-pole MCCBs.
        //
        // A future UI option can explicitly select 4P where
        // neutral switching/protection is required.
        // --------------------------------------------------------

        val requiredPoles =
            when (phaseSystem) {

                ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE ->
                    3

                ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE ->
                    2
            }

        // --------------------------------------------------------
        // 15. Breaker design
        // --------------------------------------------------------

        val breakerInput =
            ProfessionalEngineeringCore.BreakerDesignInput(

                designCurrentA =
                    designCurrent,

                cableAmpacityA =
                    feeder.ampacityIz,

                prospectiveShortCircuitKA =
                    shortCircuitKA,

                requiredPoles =
                    requiredPoles,

                systemVoltageV =
                    node.voltage
            )

        val breakerResult =
            ProfessionalEngineeringCore.designBreaker(
                input = breakerInput,
                provider = breakerProvider
            )

        val selectedBreaker =
            breakerResult.selectedBreaker

        if (
            breakerResult.status != EngineeringStatus.PASS ||
            selectedBreaker == null
        ) {

            return FeederResult(
                nodeId = node.id,
                nodeName = node.name,
                designCurrentA = designCurrent,
                cableSizeMm2 =
                    feeder.conductorSizeMm2,
                parallelRuns =
                    feeder.parallelRuns,
                ampacityIzA =
                    feeder.ampacityIz,
                voltageDropPercent =
                    feeder.voltageDropPercent,
                breakerRatingA = null,
                breakerIcuKA = null,
                breakerIcsKA = null,
                shortCircuitCurrentKA =
                    shortCircuitKA,
                protectionPassed = false,
                valid = false,
                message =
                    breakerStatusMessage(
                        breakerResult.status
                    ),
                warnings =
                    (
                        warnings +
                                breakerResult.trace.warnings
                        ).distinct()
            )
        }

        // --------------------------------------------------------
        // 16. Store breaker
        // --------------------------------------------------------

        feeder.breakerRatingIn =
            selectedBreaker.ratedCurrentA

        feeder.breakerIcuKA =
            selectedBreaker.icuKA

        feeder.breakerIcsKA =
            selectedBreaker.icsKA ?: 0.0

        feeder.breakerSelectedAutomatically =
            true

        // --------------------------------------------------------
        // 17. Protection checks
        // --------------------------------------------------------

        val protectionChecks =
            evaluateProtection(
                designCurrentA =
                    designCurrent,

                cableAmpacityA =
                    feeder.ampacityIz,

                breakerRatingA =
                    feeder.breakerRatingIn,

                shortCircuitKA =
                    shortCircuitKA,

                breakerIcuKA =
                    feeder.breakerIcuKA,

                breakerIcsKA =
                    feeder.breakerIcsKA
            )

        // --------------------------------------------------------
        // 18. Voltage drop
        // --------------------------------------------------------

        val voltageDrop =
            feeder.voltageDropPercent

        val voltageDropPassed =
            voltageDrop >= 0.0 &&
                    voltageDrop <= maximumVoltageDrop

        if (!voltageDropPassed) {

            warnings +=
                "Voltage drop exceeds the specified maximum."
        }

        // --------------------------------------------------------
        // 19. Final validity
        // --------------------------------------------------------

        val valid =
            protectionChecks.first &&
                    voltageDropPassed

        val finalMessage =
            when {

                !protectionChecks.first ->
                    "FAIL: protection requirements are not satisfied."

                !voltageDropPassed ->
                    "FAIL: voltage drop exceeds the specified maximum."

                else ->
                    "PASS: automatic cable and breaker selection completed."
            }

        // --------------------------------------------------------
        // 20. Final result
        // --------------------------------------------------------

        return FeederResult(

            nodeId =
                node.id,

            nodeName =
                node.name,

            designCurrentA =
                designCurrent,

            cableSizeMm2 =
                feeder.conductorSizeMm2,

            parallelRuns =
                feeder.parallelRuns,

            ampacityIzA =
                feeder.ampacityIz,

            voltageDropPercent =
                feeder.voltageDropPercent,

            breakerRatingA =
                feeder.breakerRatingIn,

            breakerIcuKA =
                feeder.breakerIcuKA,

            breakerIcsKA =
                feeder.breakerIcsKA,

            shortCircuitCurrentKA =
                shortCircuitKA,

            protectionPassed =
                protectionChecks.first,

            valid =
                valid,

            message =
                finalMessage,

            warnings =
                (
                    warnings +
                            protectionChecks.second +
                            listOf(
                                "Short-circuit level used for breaker selection is the transformer secondary bus fault level.",
                                "Automatic breaker pole selection uses 3P for three-phase feeders; 4P should be explicitly selected where required by the project protection philosophy."
                            )
                    )
                    .distinct()
        )
    }

    // ============================================================
    // DESIGN CURRENT
    // ============================================================

    private fun calculateDesignCurrent(
        node: DistributionNode,
        feeder: FeederDesign
    ): Double {

        // --------------------------------------------------------
        // Manual/previously calculated design current has priority.
        // --------------------------------------------------------

        if (feeder.designCurrentIb > 0.0) {
            return feeder.designCurrentIb
        }

        if (node.voltage <= 0.0) {
            return 0.0
        }

        val phaseSystem =
            when (node.phaseType) {

                PhaseType.THREE_PHASE ->
                    ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE

                PhaseType.SINGLE_PHASE_L1,
                PhaseType.SINGLE_PHASE_L2,
                PhaseType.SINGLE_PHASE_L3 ->
                    ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
            }

        val loadInputs =
            node.loads.map { load ->

                ProfessionalEngineeringCore.LoadInput(
                    name = load.name,
                    quantity = load.quantity.toDouble(),
                    unitPowerKW = load.unitKW,
                    demandFactor = load.demandFactor,
                    powerFactor = load.powerFactor
                )
            }

        if (loadInputs.isEmpty()) {
            return 0.0
        }

        val powerFactor =
            determinePowerFactor(node)

        if (powerFactor <= 0.0) {
            return 0.0
        }

        val systemInput =
            ProfessionalEngineeringCore.SystemInput(
                voltageV = node.voltage,

                // Frequency is not used to invent current.
                // 50 Hz is not required by the load-current formula.
                // The value remains only a technical input required
                // by the Core API.
                frequencyHz = 50.0,

                phaseSystem = phaseSystem,

                powerFactor =
                    powerFactor
            )

        val result =
            ProfessionalEngineeringCore.calculateLoads(
                loads = loadInputs,
                system = systemInput
            )

        return result.currentA
    }

    // ============================================================
    // POWER FACTOR
    // ============================================================

    private fun determinePowerFactor(
        node: DistributionNode
    ): Double {

        val validLoads =
            node.loads.filter { load ->

                load.quantity > 0 &&
                        load.unitKW >= 0.0 &&
                        load.demandFactor > 0.0 &&
                        load.powerFactor > 0.0
            }

        if (validLoads.isEmpty()) {
            return 0.0
        }

        val weightedPower =
            validLoads.sumOf {
                it.demandKW()
            }

        if (weightedPower <= 0.0) {

            val averagePF =
                validLoads
                    .map {
                        it.powerFactor
                    }
                    .average()

            return averagePF
                .takeIf {
                    !it.isNaN()
                }
                ?.coerceIn(
                    0.1,
                    1.0
                )
                ?: 0.0
        }

        val weightedPF =
            validLoads.sumOf { load ->

                load.demandKW() *
                        load.powerFactor

            } / weightedPower

        return weightedPF.coerceIn(
            0.1,
            1.0
        )
    }

    // ============================================================
    // PROTECTION CHECKS
    // ============================================================

    private fun evaluateProtection(
        designCurrentA: Double,
        cableAmpacityA: Double,
        breakerRatingA: Double,
        shortCircuitKA: Double,
        breakerIcuKA: Double,
        breakerIcsKA: Double
    ): Pair<Boolean, List<String>> {

        val warnings =
            mutableListOf<String>()

        // --------------------------------------------------------
        // Basic input validation
        // --------------------------------------------------------

        if (designCurrentA <= 0.0) {

            warnings +=
                "Invalid design current."
        }

        if (cableAmpacityA <= 0.0) {

            warnings +=
                "Invalid cable ampacity."
        }

        if (breakerRatingA <= 0.0) {

            warnings +=
                "Invalid breaker rated current."
        }

        if (shortCircuitKA <= 0.0) {

            warnings +=
                "Invalid prospective short-circuit current."
        }

        if (breakerIcuKA <= 0.0) {

            warnings +=
                "Invalid breaker Icu."
        }

        // --------------------------------------------------------
        // Ib <= In <= Iz
        // --------------------------------------------------------

        val currentCoordinationPassed =
            designCurrentA > 0.0 &&
                    breakerRatingA > 0.0 &&
                    cableAmpacityA > 0.0 &&
                    designCurrentA <= breakerRatingA &&
                    breakerRatingA <= cableAmpacityA

        if (!currentCoordinationPassed) {

            warnings +=
                "Protection coordination failed: Ib <= In <= Iz is not satisfied."
        }

        // --------------------------------------------------------
        // Icu >= Ik
        // --------------------------------------------------------

        val breakingCapacityPassed =
            breakerIcuKA > 0.0 &&
                    shortCircuitKA > 0.0 &&
                    breakerIcuKA >= shortCircuitKA

        if (!breakingCapacityPassed) {

            warnings +=
                "Breaker Icu is lower than the prospective short-circuit current."
        }

        // --------------------------------------------------------
        // Ics >= Ik when verified
        //
        // If Ics is unavailable, the service does not invent a
        // value. The Core's breaker catalog determines whether
        // the value is available.
        // --------------------------------------------------------

        val serviceBreakingCapacityPassed =
            if (breakerIcsKA > 0.0) {

                breakerIcsKA >= shortCircuitKA

            } else {

                true
            }

        if (!serviceBreakingCapacityPassed) {

            warnings +=
                "Breaker Ics is lower than the prospective short-circuit current."
        }

        return (
            currentCoordinationPassed &&
                    breakingCapacityPassed &&
                    serviceBreakingCapacityPassed
            ) to warnings
    }

    // ============================================================
    // EMPTY RESULT
    // ============================================================

    private fun emptyResult(
        node: DistributionNode,
        designCurrent: Double = 0.0,
        message: String
    ): FeederResult {

        return FeederResult(

            nodeId =
                node.id,

            nodeName =
                node.name,

            designCurrentA =
                designCurrent,

            cableSizeMm2 =
                null,

            parallelRuns =
                0,

            ampacityIzA =
                null,

            voltageDropPercent =
                null,

            breakerRatingA =
                null,

            breakerIcuKA =
                null,

            breakerIcsKA =
                null,

            shortCircuitCurrentKA =
                null,

            protectionPassed =
                false,

            valid =
                false,

            message =
                message,

            warnings =
                emptyList()
        )
    }

    // ============================================================
    // CABLE STATUS
    // ============================================================

    private fun cableStatusMessage(
        status: EngineeringStatus
    ): String {

        return when (status) {

            EngineeringStatus.PASS ->
                "PASS: cable selected successfully."

            EngineeringStatus.FAIL ->
                "FAIL: no cable configuration satisfies the specified design constraints."

            EngineeringStatus.DATA_REQUIRED ->
                "DATA_REQUIRED: verified cable engineering data is required."

            EngineeringStatus.WARNING ->
                "WARNING: cable design requires engineering review."

            EngineeringStatus.NOT_CALCULATED ->
                "Cable design was not calculated."
        }
    }

    // ============================================================
    // BREAKER STATUS
    // ============================================================

    private fun breakerStatusMessage(
        status: EngineeringStatus
    ): String {

        return when (status) {

            EngineeringStatus.PASS ->
                "PASS: breaker selected successfully."

            EngineeringStatus.FAIL ->
                "FAIL: no breaker satisfies the specified design requirements."

            EngineeringStatus.DATA_REQUIRED ->
                "DATA_REQUIRED: verified breaker engineering data is required."

            EngineeringStatus.WARNING ->
                "WARNING: breaker design requires engineering review."

            EngineeringStatus.NOT_CALCULATED ->
                "Breaker design was not calculated."
        }
    }
}
