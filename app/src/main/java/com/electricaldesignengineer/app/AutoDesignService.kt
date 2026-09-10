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
 * Protection coordination
 *
 * IMPORTANT:
 * - No EngineeringDesignEngine.
 * - No invented transformer impedance.
 * - No hardcoded short-circuit current.
 * - Cable is automatically selected by ProfessionalEngineeringCore.
 * - Breaker is automatically selected by ProfessionalEngineeringCore.
 * - Transformer is automatically selected from the verified catalog.
 * - Engineer can override the cable later from the UI.
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
            .filter { it.verified }
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
                transformers = getTransformerData()
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
                        "FAIL: transformer requirements are invalid."

                    EngineeringStatus.WARNING ->
                        "WARNING: transformer selection requires engineering review."

                    EngineeringStatus.NOT_CALCULATED ->
                        "Transformer selection was not calculated."
                },
            warnings = result.trace.warnings
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
                        validationResult.errors +
                                validationResult.warnings
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
            // 3. Find transformer
            // ----------------------------------------------------

            val transformerNode =
                system.getTransformer()

            val transformerVoltage =
                transformerNode?.voltage
                    ?.takeIf { it > 0.0 }
                    ?: 400.0

            val transformerDesign =
                designTransformer(
                    demandKVA = demandKVA,
                    designMarginPercent = 0.0,
                    primaryVoltageV = null,
                    secondaryVoltageV = transformerVoltage,
                    frequencyHz = 50.0
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
                        transformerDesign.warnings +
                                "Automatic design cannot continue without a verified transformer."
                )
            }

            val selectedTransformer =
                transformerDesign.selectedTransformer

            // ----------------------------------------------------
            // 4. Calculate transformer secondary short circuit
            //
            // This is the transformer secondary bus fault level.
            //
            // We intentionally do NOT add downstream cable
            // impedance here because transformer catalog data
            // normally gives only %Z.
            //
            // Therefore this value is conservative for breaker
            // interrupting-capacity selection.
            // ----------------------------------------------------

            val transformerShortCircuit =
                calculateTransformerShortCircuit(
                    transformer = selectedTransformer,
                    voltageV =
                        selectedTransformer.secondaryVoltageV
                            .takeIf { it > 0.0 }
                            ?: transformerVoltage
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
            // 5. Design every feeder
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
            // 6. Final system status
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
                    "DATA_REQUIRED: system voltage is required."
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

        // --------------------------------------------------------
        // 7. Correction factor
        // --------------------------------------------------------

        val correctionFactor =
            feeder.correctionFactorTotal
                .coerceIn(
                    0.0001,
                    1.0
                )

        // --------------------------------------------------------
        // 8. Maximum parallel runs
        // --------------------------------------------------------

        val maximumParallelRuns =
            8

        // --------------------------------------------------------
        // 9. Maximum voltage drop
        // --------------------------------------------------------

        val maximumVoltageDrop =
            if (feeder.maximumVoltageDropPercent > 0.0) {

                feeder.maximumVoltageDropPercent

            } else {

                5.0
            }

        feeder.maximumVoltageDropPercent =
            maximumVoltageDrop

        // --------------------------------------------------------
        // 10. Cable design input
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
        // We use the transformer secondary bus fault level
        // conservatively for breaker interrupting-capacity
        // selection.
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
        // --------------------------------------------------------

        val requiredPoles =
            when (phaseSystem) {

                ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE ->
                    4

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
                    requiredPoles
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
                    warnings +
                            breakerResult.trace.warnings
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

        val voltageDropPassed =
            feeder.voltageDropPercent <=
                    maximumVoltageDrop

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
                                "Short-circuit level used for breaker selection is the transformer secondary bus fault level."
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

        val systemInput =
            ProfessionalEngineeringCore.SystemInput(
                voltageV = node.voltage,
                frequencyHz = 50.0,
                phaseSystem = phaseSystem,
                powerFactor =
                    determinePowerFactor(node)
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

        val weightedPower =
            node.loads.sumOf {
                it.demandKW()
            }

        if (weightedPower <= 0.0) {

            return node.loads
                .map {
                    it.powerFactor
                }
                .filter {
                    it > 0.0
                }
                .average()
                .takeIf {
                    !it.isNaN()
                }
                ?.coerceIn(
                    0.1,
                    1.0
                )
                ?: 0.9
        }

        val weightedPF =
            node.loads.sumOf { load ->

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

        val currentCoordinationPassed =
            designCurrentA <=
                    breakerRatingA &&
                    breakerRatingA <=
                    cableAmpacityA

        if (!currentCoordinationPassed) {

            warnings +=
                "Protection coordination failed: Ib <= In <= Iz is not satisfied."
        }

        val breakingCapacityPassed =
            breakerIcuKA >=
                    shortCircuitKA

        if (!breakingCapacityPassed) {

            warnings +=
                "Breaker Icu is lower than the prospective short-circuit current."
        }

        val serviceBreakingCapacityPassed =
            if (breakerIcsKA > 0.0) {

                breakerIcsKA >=
                        shortCircuitKA

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
                message
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

            else ->
                "Cable design requires review."
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

            else ->
                "Breaker design requires review."
        }
    }
}
