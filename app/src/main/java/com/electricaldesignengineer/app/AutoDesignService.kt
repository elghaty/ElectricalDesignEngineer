package com.electricaldesignengineer.app

/**
 * AutoDesignService
 *
 * Orchestrates the automatic electrical distribution design.
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
 * IMPORTANT:
 * - This service must NOT use EngineeringDesignEngine.
 * - No transformer impedance is invented.
 * - If source short-circuit data is unavailable, the result is reported
 *   as DATA_REQUIRED instead of using an arbitrary default.
 */

object AutoDesignService {

    // ------------------------------------------------------------------------
    // PUBLIC RESULT MODELS
    // ------------------------------------------------------------------------

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

    data class SystemResult(
        val system: ElectricalSystem,
        val feeders: List<FeederResult>,
        val valid: Boolean,
        val message: String,
        val warnings: List<String> = emptyList()
    )

    // ------------------------------------------------------------------------
    // PROFESSIONAL DATA PROVIDERS
    // ------------------------------------------------------------------------

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

    // ------------------------------------------------------------------------
    // MAIN ENTRY POINT
    // ------------------------------------------------------------------------

    fun designSystem(
        system: ElectricalSystem
    ): SystemResult {

        val warnings = mutableListOf<String>()

        return try {

            // ------------------------------------------------------------
            // 1. Calculate distribution loads
            // ------------------------------------------------------------

            DistributionCalculator.calculate(system)

            // ------------------------------------------------------------
            // 2. Design every feeder
            // ------------------------------------------------------------

            val feederResults = mutableListOf<FeederResult>()

            system.nodes
                .filter { it.type != DistributionNodeType.TRANSFORMER }
                .forEach { node ->

                    val feeder = node.feeder

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
                            message = "DATA_REQUIRED: feeder data is missing"
                        )

                        return@forEach
                    }

                    val result = designFeeder(
                        system = system,
                        node = node,
                        feeder = feeder
                    )

                    feederResults += result

                    if (result.warnings.isNotEmpty()) {
                        warnings += result.warnings
                    }
                }

            // ------------------------------------------------------------
            // 3. Validate final system
            // ------------------------------------------------------------

            val allValid = feederResults.all { it.valid }

            val finalMessage =
                if (allValid) {
                    "Automatic electrical design completed successfully."
                } else {
                    "Design completed with one or more engineering issues."
                }

            SystemResult(
                system = system,
                feeders = feederResults,
                valid = allValid,
                message = finalMessage,
                warnings = warnings.distinct()
            )

        } catch (ex: Exception) {

            SystemResult(
                system = system,
                feeders = emptyList(),
                valid = false,
                message = "DESIGN_ERROR: ${ex.message ?: "Unknown engineering calculation error"}",
                warnings = listOf(
                    "Automatic design could not be completed."
                )
            )
        }
    }

    // ------------------------------------------------------------------------
    // FEEDER DESIGN
    // ------------------------------------------------------------------------

    private fun designFeeder(
        system: ElectricalSystem,
        node: DistributionNode,
        feeder: FeederDesign
    ): FeederResult {

        val warnings = mutableListOf<String>()

        // ------------------------------------------------------------
        // 1. Design current
        // ------------------------------------------------------------

        val designCurrent = calculateDesignCurrent(
            node = node,
            feeder = feeder
        )

        if (designCurrent <= 0.0) {
            return emptyResult(
                node = node,
                message = "DATA_REQUIRED: valid design current is required"
            )
        }

        feeder.designCurrentIb = designCurrent

        // ------------------------------------------------------------
        // 2. Basic input validation
        // ------------------------------------------------------------

        if (feeder.lengthMeters <= 0.0) {
            return emptyResult(
                node = node,
                designCurrent = designCurrent,
                message = "DATA_REQUIRED: feeder length must be greater than zero"
            )
        }

        if (node.voltage <= 0.0) {
            return emptyResult(
                node = node,
                designCurrent = designCurrent,
                message = "DATA_REQUIRED: system voltage is required"
            )
        }

        if (feeder.parallelRuns < 1) {
            feeder.parallelRuns = 1
        }

        // ------------------------------------------------------------
        // 3. Cable design through ProfessionalEngineeringCore
        // ------------------------------------------------------------

        val phaseSystem =
            when (node.phaseType) {
                PhaseType.SINGLE_PHASE ->
                    ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE

                else ->
                    ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE
            }

        val cableInput =
            ProfessionalEngineeringCore.CableDesignInput(
                currentA = designCurrent,
                lengthM = feeder.lengthMeters,
                voltageV = node.voltage,
                powerFactor = determinePowerFactor(node),
                phaseSystem = phaseSystem,
                installationMethod = feeder.installationMethod,
                material = feeder.cableMaterial,
                insulation = feeder.insulation,
                groupingFactor = safeCorrectionFactor(feeder.correctionFactorTotal),
                thermalInsulationFactor = 1.0,
                soilCorrectionFactor = 1.0,
                ambientTemperatureC = 30.0,
                maximumVoltageDropPercent =
                    if (feeder.maximumVoltageDropPercent > 0.0)
                        feeder.maximumVoltageDropPercent
                    else
                        5.0,
                maximumParallelRuns =
                    if (feeder.parallelRuns > 1)
                        feeder.parallelRuns
                    else
                        10
            )

        val cableResult =
            ProfessionalEngineeringCore.designCable(
                input = cableInput,
                provider = cableProvider
            )

        if (!cableResult.success) {

            return FeederResult(
                nodeId = node.id,
                nodeName = node.name,
                designCurrentA = designCurrent,
                cableSizeMm2 = null,
                parallelRuns = feeder.parallelRuns,
                ampacityIzA = null,
                voltageDropPercent = null,
                breakerRatingA = null,
                breakerIcuKA = null,
                breakerIcsKA = null,
                shortCircuitCurrentKA = null,
                protectionPassed = false,
                valid = false,
                message = cableResult.message,
                warnings = cableResult.warnings
            )
        }

        // ------------------------------------------------------------
        // 4. Store selected cable
        // ------------------------------------------------------------

        feeder.conductorSizeMm2 = cableResult.cableSizeMm2
        feeder.ampacityIz = cableResult.ampacityA
        feeder.parallelRuns = cableResult.parallelRuns
        feeder.voltageDropPercent = cableResult.voltageDropPercent
        feeder.cableSelectedAutomatically = true

        // ------------------------------------------------------------
        // 5. Short circuit calculation
        // ------------------------------------------------------------

        val shortCircuitResult =
            calculateShortCircuit(
                system = system,
                node = node,
                feeder = feeder,
                phaseSystem = phaseSystem,
                warnings = warnings
            )

        val shortCircuitKA =
            shortCircuitResult.first

        val shortCircuitAvailable =
            shortCircuitResult.second

        if (!shortCircuitAvailable) {

            return FeederResult(
                nodeId = node.id,
                nodeName = node.name,
                designCurrentA = designCurrent,
                cableSizeMm2 = feeder.conductorSizeMm2,
                parallelRuns = feeder.parallelRuns,
                ampacityIzA = feeder.ampacityIz,
                voltageDropPercent = feeder.voltageDropPercent,
                breakerRatingA = null,
                breakerIcuKA = null,
                breakerIcsKA = null,
                shortCircuitCurrentKA = null,
                protectionPassed = false,
                valid = false,
                message = "DATA_REQUIRED: upstream short-circuit/source impedance data is required.",
                warnings = warnings.distinct()
            )
        }

        feeder.shortCircuitCurrentKA = shortCircuitKA

        // ------------------------------------------------------------
        // 6. Breaker selection
        // ------------------------------------------------------------

        val requiredPoles =
            if (phaseSystem == ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE) {
                4
            } else {
                2
            }

        val breakerInput =
            ProfessionalEngineeringCore.BreakerDesignInput(
                loadCurrentA = designCurrent,
                shortCircuitCurrentKA = shortCircuitKA,
                requiredPoles = requiredPoles
            )

        val breakerResult =
            ProfessionalEngineeringCore.designBreaker(
                input = breakerInput,
                provider = breakerProvider
            )

        if (!breakerResult.success) {

            return FeederResult(
                nodeId = node.id,
                nodeName = node.name,
                designCurrentA = designCurrent,
                cableSizeMm2 = feeder.conductorSizeMm2,
                parallelRuns = feeder.parallelRuns,
                ampacityIzA = feeder.ampacityIz,
                voltageDropPercent = feeder.voltageDropPercent,
                breakerRatingA = null,
                breakerIcuKA = null,
                breakerIcsKA = null,
                shortCircuitCurrentKA = shortCircuitKA,
                protectionPassed = false,
                valid = false,
                message = breakerResult.message,
                warnings = warnings + breakerResult.warnings
            )
        }

        // ------------------------------------------------------------
        // 7. Store breaker
        // ------------------------------------------------------------

        feeder.breakerRatingIn = breakerResult.breaker.ratingA
        feeder.breakerIcuKA = breakerResult.breaker.icuKA
        feeder.breakerIcsKA = breakerResult.breaker.icsKA
        feeder.breakerSelectedAutomatically = true

        // ------------------------------------------------------------
        // 8. Protection coordination/basic protection check
        // ------------------------------------------------------------

        val protectionInput =
            ProfessionalEngineeringCore.ProtectionCheckInput(
                designCurrentIb = designCurrent,
                breakerRatingIn = feeder.breakerRatingIn,
                cableAmpacityIz = feeder.ampacityIz,
                shortCircuitCurrentKA = shortCircuitKA,
                breakerIcuKA = feeder.breakerIcuKA,
                breakerIcsKA = feeder.breakerIcsKA
            )

        val protectionResult =
            ProfessionalEngineeringCore.checkProtection(
                input = protectionInput
            )

        val finalWarnings =
            warnings +
                    cableResult.warnings +
                    breakerResult.warnings +
                    protectionResult.warnings

        val valid =
            protectionResult.passed &&
                    feeder.voltageDropPercent <=
                    feeder.maximumVoltageDropPercent

        val finalMessage =
            when {
                !protectionResult.passed ->
                    protectionResult.message

                feeder.voltageDropPercent >
                        feeder.maximumVoltageDropPercent ->
                    "FAIL: voltage drop exceeds the specified maximum."

                else ->
                    "PASS: feeder cable and breaker design completed."
            }

        return FeederResult(
            nodeId = node.id,
            nodeName = node.name,
            designCurrentA = designCurrent,
            cableSizeMm2 = feeder.conductorSizeMm2,
            parallelRuns = feeder.parallelRuns,
            ampacityIzA = feeder.ampacityIz,
            voltageDropPercent = feeder.voltageDropPercent,
            breakerRatingA = feeder.breakerRatingIn,
            breakerIcuKA = feeder.breakerIcuKA,
            breakerIcsKA = feeder.breakerIcsKA,
            shortCircuitCurrentKA = shortCircuitKA,
            protectionPassed = protectionResult.passed,
            valid = valid,
            message = finalMessage,
            warnings = finalWarnings.distinct()
        )
    }

    // ------------------------------------------------------------------------
    // DESIGN CURRENT
    // ------------------------------------------------------------------------

    private fun calculateDesignCurrent(
        node: DistributionNode,
        feeder: FeederDesign
    ): Double {

        // If a valid design current was already calculated by the distribution
        // calculator, use it.
        if (feeder.designCurrentIb > 0.0) {
            return feeder.designCurrentIb
        }

        // Otherwise calculate from the node loads.
        val totalLoad =
            node.loads.sumOf { load ->

                when {
                    load.designCurrentA > 0.0 ->
                        load.designCurrentA

                    load.powerKW > 0.0 ->
                        calculateCurrentFromPower(
                            powerKW = load.powerKW,
                            voltage = node.voltage,
                            powerFactor = load.powerFactor,
                            phaseType = node.phaseType
                        )

                    else ->
                        0.0
                }
            }

        return totalLoad
    }

    private fun calculateCurrentFromPower(
        powerKW: Double,
        voltage: Double,
        powerFactor: Double,
        phaseType: PhaseType
    ): Double {

        if (powerKW <= 0.0 || voltage <= 0.0) {
            return 0.0
        }

        val pf =
            powerFactor
                .coerceIn(0.1, 1.0)

        return when (phaseType) {

            PhaseType.SINGLE_PHASE ->
                powerKW * 1000.0 /
                        (voltage * pf)

            else ->
                powerKW * 1000.0 /
                        (kotlin.math.sqrt(3.0) * voltage * pf)
        }
    }

    // ------------------------------------------------------------------------
    // POWER FACTOR
    // ------------------------------------------------------------------------

    private fun determinePowerFactor(
        node: DistributionNode
    ): Double {

        val values =
            node.loads
                .map { it.powerFactor }
                .filter { it > 0.0 }

        if (values.isEmpty()) {
            return 0.9
        }

        return values.average()
            .coerceIn(0.1, 1.0)
    }

    // ------------------------------------------------------------------------
    // SHORT CIRCUIT
    // ------------------------------------------------------------------------

    private fun calculateShortCircuit(
        system: ElectricalSystem,
        node: DistributionNode,
        feeder: FeederDesign,
        phaseSystem: ProfessionalEngineeringCore.PhaseSystem,
        warnings: MutableList<String>
    ): Pair<Double, Boolean> {

        /*
         * Professional rule:
         *
         * NEVER invent transformer %Z.
         *
         * The ProfessionalEngineeringCore requires real source information.
         *
         * If the project model does not yet contain transformer impedance or
         * upstream fault level, we must return DATA_REQUIRED.
         */

        val transformer =
            system.nodes.firstOrNull {
                it.type == DistributionNodeType.TRANSFORMER
            }

        if (transformer == null) {

            warnings +=
                "Short-circuit calculation requires an upstream transformer/source."

            return 0.0 to false
        }

        /*
         * The current DistributionNode model does not expose transformer
         * R%, X%, %Z or upstream fault level.
         *
         * Therefore we intentionally do not fabricate these values.
         *
         * The correct next model extension is to add source data to the
         * transformer node and then pass it to ProfessionalEngineeringCore.
         */

        warnings +=
            "Transformer short-circuit/source impedance data is not defined in the current distribution model."

        return 0.0 to false
    }

    // ------------------------------------------------------------------------
    // CORRECTION FACTOR
    // ------------------------------------------------------------------------

    private fun safeCorrectionFactor(
        value: Double
    ): Double {

        return if (value > 0.0) {
            value.coerceAtMost(1.0)
        } else {
            1.0
        }
    }

    // ------------------------------------------------------------------------
    // EMPTY RESULT
    // ------------------------------------------------------------------------

    private fun emptyResult(
        node: DistributionNode,
        designCurrent: Double = 0.0,
        message: String
    ): FeederResult {

        return FeederResult(
            nodeId = node.id,
            nodeName = node.name,
            designCurrentA = designCurrent,
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
            message = message
        )
    }

    // ------------------------------------------------------------------------
    // DISPLAY HELPERS
    // ------------------------------------------------------------------------

    fun formatCableSize(
        sizeMm2: Double?
    ): String {

        if (sizeMm2 == null) {
            return "-"
        }

        return if (sizeMm2 % 1.0 == 0.0) {
            "${sizeMm2.toInt()} mm²"
        } else {
            "${sizeMm2} mm²"
        }
    }

    fun formatCurrent(
        currentA: Double?
    ): String {

        if (currentA == null) {
            return "-"
        }

        return "%.1f A".format(currentA)
    }

    fun formatVoltageDrop(
        voltageDropPercent: Double?
    ): String {

        if (voltageDropPercent == null) {
            return "-"
        }

        return "%.2f %%".format(voltageDropPercent)
    }

    fun formatShortCircuit(
        shortCircuitKA: Double?
    ): String {

        if (shortCircuitKA == null) {
            return "-"
        }

        return "%.2f kA".format(shortCircuitKA)
    }
}

مهم: هذا الملف متعمد ألا يستخدم "EngineeringDesignEngine" نهائيًا، وألا يفترض "%Z = 6%" للمحول. بعد وضعه، الخطوة التالية هي تعديل "ProfessionalEngineeringCore.kt" لإضافة اختيار الكابل اليدوي، ثم تعديل "DistributionModel.kt" لإضافة بيانات المحول (%Z أو R/X أو Ik)، وبعدها نحذف "EngineeringDesignEngine.kt" نهائيًا.
