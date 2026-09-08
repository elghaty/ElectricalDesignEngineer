package com.electricaldesignengineer.app

import kotlin.math.sqrt

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
 * IMPORTANT:
 * - Does NOT use EngineeringDesignEngine.
 * - Does NOT invent transformer impedance.
 * - Cable is selected automatically by the ProfessionalEngineeringCore.
 * - Engineer can later override the selected cable from the UI.
 */
object AutoDesignService {

    // ============================================================
    // RESULT MODELS
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

    data class SystemResult(
        val system: DistributionSystem,
        val feeders: List<FeederResult>,
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
    // MAIN DESIGN
    // ============================================================

    fun designSystem(
        system: DistributionSystem
    ): SystemResult {

        val systemWarnings = mutableListOf<String>()
        val feederResults = mutableListOf<FeederResult>()

        return try {

            /*
             * Design every node having a feeder.
             *
             * Transformer itself is not designed as a feeder.
             */
            system.nodes
                .filter { node ->
                    node.type != DistributionNodeType.TRANSFORMER
                }
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
                            message =
                                "DATA_REQUIRED: feeder data is missing."
                        )

                    } else {

                        val result =
                            designFeeder(
                                system = system,
                                node = node,
                                feeder = feeder
                            )

                        feederResults += result
                        systemWarnings += result.warnings
                    }
                }

            val valid =
                feederResults.isNotEmpty() &&
                        feederResults.all { it.valid }

            val message =
                if (valid) {
                    "Automatic electrical design completed successfully."
                } else {
                    "Design completed with one or more engineering issues."
                }

            SystemResult(
                system = system,
                feeders = feederResults,
                valid = valid,
                message = message,
                warnings = systemWarnings.distinct()
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
                        .plus("Automatic design calculation failed.")
                        .distinct()
            )
        }
    }

    // ============================================================
    // FEEDER DESIGN
    // ============================================================

    private fun designFeeder(
        system: DistributionSystem,
        node: DistributionNode,
        feeder: FeederDesign
    ): FeederResult {

        val warnings = mutableListOf<String>()

        // --------------------------------------------------------
        // 1. Calculate design current
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

        feeder.designCurrentIb = designCurrent

        // --------------------------------------------------------
        // 2. Validate feeder length
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
        // 3. Validate voltage
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
        // 4. Determine phase system
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
        // 5. Number of loaded conductors
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
                .coerceIn(0.0001, 1.0)

        // --------------------------------------------------------
        // 8. Maximum parallel runs
        // --------------------------------------------------------

        val maximumParallelRuns =
            when {
                feeder.parallelRuns > 1 ->
                    feeder.parallelRuns.coerceIn(1, 8)

                else ->
                    8
            }

        // --------------------------------------------------------
        // 9. Cable design input
        // --------------------------------------------------------

        val cableInput =
            ProfessionalEngineeringCore.CableDesignInput(

                designCurrentA = designCurrent,

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
                    if (feeder.maximumVoltageDropPercent > 0.0) {
                        feeder.maximumVoltageDropPercent
                    } else {
                        5.0
                    },

                maximumParallelRuns =
                    maximumParallelRuns
            )

        // --------------------------------------------------------
        // 10. Automatic cable selection
        // --------------------------------------------------------

        val cableResult =
            ProfessionalEngineeringCore.designCable(
                input = cableInput,
                provider = cableProvider
            )

        val selectedCable =
            cableResult.selectedCable

        if (
            cableResult.status !=
            EngineeringStatus.PASS ||
            selectedCable == null
        ) {

            return FeederResult(
                nodeId = node.id,
                nodeName = node.name,
                designCurrentA = designCurrent,
                cableSizeMm2 = null,
                parallelRuns = cableResult.parallelRuns,
                ampacityIzA =
                    if (cableResult.totalAmpacityA > 0.0) {
                        cableResult.totalAmpacityA
                    } else {
                        null
                    },
                voltageDropPercent =
                    if (cableResult.voltageDropPercent > 0.0) {
                        cableResult.voltageDropPercent
                    } else {
                        null
                    },
                breakerRatingA = null,
                breakerIcuKA = null,
                breakerIcsKA = null,
                shortCircuitCurrentKA = null,
                protectionPassed = false,
                valid = false,
                message =
                    cableStatusMessage(cableResult.status),
                warnings =
                    cableResult.trace.warnings
            )
        }

        // --------------------------------------------------------
        // 11. Store automatically selected cable
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
        // 12. Short-circuit source
        // --------------------------------------------------------
        //
        // Current DistributionModel does not yet contain transformer
        // impedance/source impedance fields.
        //
        // Therefore:
        // - use explicitly supplied feeder.shortCircuitCurrentKA
        //   if it is greater than zero.
        // - otherwise do NOT invent a transformer %Z.
        //

        val shortCircuitKA =
            if (feeder.shortCircuitCurrentKA > 0.0) {

                feeder.shortCircuitCurrentKA

            } else {

                warnings +=
                    "Short-circuit source data is not available. " +
                            "Transformer/source impedance must be entered."

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
                        "DATA_REQUIRED: upstream short-circuit current " +
                                "or transformer/source impedance is required.",
                    warnings =
                        warnings.distinct()
                )
            }

        // --------------------------------------------------------
        // 13. Store short-circuit current
        // --------------------------------------------------------

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
            breakerResult.status !=
            EngineeringStatus.PASS ||
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
        // 18. Voltage-drop check
        // --------------------------------------------------------

        val voltageDropPassed =
            feeder.voltageDropPercent <=
                    feeder.maximumVoltageDropPercent

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
                            protectionChecks.second
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

        /*
         * Keep an already calculated value if it exists.
         */
        if (feeder.designCurrentIb > 0.0) {
            return feeder.designCurrentIb
        }

        /*
         * Calculate directly from the node demand loads.
         *
         * This uses the actual DistributionLoad model:
         *
         * quantity
         * unitKW
         * demandFactor
         * powerFactor
         */

        val demandKVA =
            node.loads.sumOf {
                it.demandKVA()
            }

        if (demandKVA <= 0.0) {
            return 0.0
        }

        val effectivePowerFactor =
            determinePowerFactor(node)

        return when (node.phaseType) {

            PhaseType.THREE_PHASE -> {

                demandKVA * 1000.0 /
                        (
                            sqrt(3.0) *
                                    node.voltage *
                                    effectivePowerFactor
                            )
            }

            PhaseType.SINGLE_PHASE_L1,
            PhaseType.SINGLE_PHASE_L2,
            PhaseType.SINGLE_PHASE_L3 -> {

                demandKVA * 1000.0 /
                        (
                            node.voltage *
                                    effectivePowerFactor
                            )
            }
        }
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
                .map { it.powerFactor }
                .filter { it > 0.0 }
                .average()
                .takeIf { !it.isNaN() }
                ?.coerceIn(0.1, 1.0)
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

        /*
         * Ib <= In <= Iz
         */

        val currentCoordinationPassed =
            designCurrentA <=
                    breakerRatingA &&
                    breakerRatingA <=
                    cableAmpacityA

        if (!currentCoordinationPassed) {

            warnings +=
                "Protection coordination failed: " +
                        "Ib <= In <= Iz is not satisfied."
        }

        /*
         * Breaking capacity must be >= prospective fault current.
         */

        val breakingCapacityPassed =
            breakerIcuKA >= shortCircuitKA

        if (!breakingCapacityPassed) {

            warnings +=
                "Breaker Icu is lower than the prospective short-circuit current."
        }

        /*
         * If Ics is provided, verify it as well.
         */
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
                message
        )
    }

    // ============================================================
    // STATUS MESSAGES
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
