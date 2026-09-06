package com.electricaldesignengineer.app

/**
 * AutoDesignService
 *
 * Complete automatic design workflow:
 *
 * Load
 *   ↓
 * Demand Load
 *   ↓
 * Design Current Ib
 *   ↓
 * Cable Selection
 *   ↓
 * Correction Factors
 *   ↓
 * Voltage Drop
 *   ↓
 * Breaker Selection
 *   ↓
 * Short Circuit
 *   ↓
 * Protection Check
 *   ↓
 * Design Status
 */
object AutoDesignService {

    data class FeederResult(
        val nodeId: String,
        val nodeName: String,

        val designCurrentA: Double,

        val cableSizeMm2: Double,
        val cableMaterial: EngineeringDesignEngine.CableMaterial,
        val insulation: EngineeringDesignEngine.InsulationType,
        val installationMethod:
            EngineeringDesignEngine.InstallationMethod,

        val parallelRuns: Int,

        val cableAmpacityA: Double,
        val correctionFactor: Double,

        val voltageDropPercent: Double,
        val maximumVoltageDropPercent: Double,

        val breakerRatingA: Double,
        val breakerIcuKA: Double,

        val shortCircuitKA: Double,

        val protectionValid: Boolean,
        val designValid: Boolean,

        val warnings: List<String>,
        val errors: List<String>
    )

    data class SystemResult(
        val feeders: List<FeederResult>,

        val transformer:
            DistributionCalculator.TransformerLoadingResult?,

        val totalConnectedKW: Double,
        val totalDemandKW: Double,
        val totalDemandKVA: Double,

        val systemValid: Boolean,

        val warnings: List<String>,
        val errors: List<String>
    )

    /**
     * Run complete automatic design.
     */
    fun design(
        system: DistributionSystem,
        defaultMaximumVoltageDropPercent: Double = 5.0
    ): SystemResult {

        val systemWarnings = mutableListOf<String>()
        val systemErrors = mutableListOf<String>()

        // ---------------------------------------------------------
        // 1. Validate distribution hierarchy
        // ---------------------------------------------------------

        val validation = system.validate()

        systemWarnings.addAll(validation.warnings)
        systemErrors.addAll(validation.errors)

        // ---------------------------------------------------------
        // 2. Calculate distribution loads
        // ---------------------------------------------------------

        val distributionResults =
            DistributionCalculator.calculate(system)

        val feederResults = mutableListOf<FeederResult>()

        // ---------------------------------------------------------
        // 3. Design feeders
        // ---------------------------------------------------------

        system.hierarchyOrder()
            .filter {
                it.type != DistributionNodeType.TRANSFORMER
            }
            .forEach { node ->

                val feeder = node.feeder

                // No feeder
                if (feeder == null) {

                    if (
                        node.type !=
                        DistributionNodeType.LOAD
                    ) {
                        systemWarnings.add(
                            "No feeder defined for ${node.name}."
                        )
                    }

                    return@forEach
                }

                // Find calculated node
                val calculation =
                    distributionResults.firstOrNull {
                        it.nodeId == node.id
                    }

                if (calculation == null) {

                    systemErrors.add(
                        "Unable to calculate ${node.name}."
                    )

                    return@forEach
                }

                // -------------------------------------------------
                // Design current Ib
                // -------------------------------------------------

                val ib =
                    calculation.currentA

                if (ib <= 0.0) {

                    node.status =
                        NodeStatus.WARNING

                    systemWarnings.add(
                        "${node.name}: design current is zero."
                    )

                    return@forEach
                }

                // -------------------------------------------------
                // Maximum voltage drop
                // -------------------------------------------------

                val maxVD =
                    if (
                        feeder.maximumVoltageDropPercent > 0.0
                    ) {
                        feeder.maximumVoltageDropPercent
                    } else {
                        defaultMaximumVoltageDropPercent
                    }

                // -------------------------------------------------
                // Power factor
                // -------------------------------------------------

                val powerFactor =
                    calculatePowerFactor(node)

                // -------------------------------------------------
                // Phase system
                // -------------------------------------------------

                val threePhase =
                    node.phaseType ==
                            PhaseType.THREE_PHASE

                // -------------------------------------------------
                // Cable selection
                // -------------------------------------------------

                val cableResult =
                    EngineeringDesignEngine.autoSelectCable(
                        designCurrentA = ib,

                        lengthM =
                            feeder.lengthMeters
                                .coerceAtLeast(0.1),

                        voltageV =
                            node.voltage
                                .coerceAtLeast(1.0),

                        powerFactor =
                            powerFactor,

                        threePhase =
                            threePhase,

                        material =
                            feeder.cableMaterial,

                        insulation =
                            feeder.insulation,

                        cores =
                            feeder.numberOfCores,

                        installationMethod =
                            feeder.installationMethod,

                        maxVoltageDropPercent =
                            maxVD
                    )

                // -------------------------------------------------
                // Cable failure
                // -------------------------------------------------

                if (
                    !cableResult.success ||
                    cableResult.cable == null
                ) {

                    node.status =
                        NodeStatus.ERROR

                    val error =
                        "No suitable cable found for " +
                                "${node.name}. " +
                                cableResult.status

                    systemErrors.add(error)

                    feederResults.add(
                        FeederResult(
                            nodeId = node.id,
                            nodeName = node.name,

                            designCurrentA = ib,

                            cableSizeMm2 = 0.0,

                            cableMaterial =
                                feeder.cableMaterial,

                            insulation =
                                feeder.insulation,

                            installationMethod =
                                feeder.installationMethod,

                            parallelRuns = 0,

                            cableAmpacityA = 0.0,

                            correctionFactor = 0.0,

                            voltageDropPercent = 0.0,

                            maximumVoltageDropPercent = maxVD,

                            breakerRatingA = 0.0,

                            breakerIcuKA = 0.0,

                            shortCircuitKA = 0.0,

                            protectionValid = false,
                            designValid = false,

                            warnings = emptyList(),

                            errors =
                                listOf(error)
                        )
                    )

                    return@forEach
                }

                // -------------------------------------------------
                // Store cable result
                // -------------------------------------------------

                val cable =
                    cableResult.cable

                feeder.conductorSizeMm2 =
                    cable.sizeMm2

                feeder.parallelRuns =
                    cableResult.parallelRuns

                feeder.ampacityIz =
                    cableResult.correctedAmpacityA

                feeder.designCurrentIb =
                    ib

                feeder.voltageDropPercent =
                    cableResult.voltageDropPercent

                feeder.correctionFactorTotal =
                    cableResult.correctionFactors.total()

                feeder.cableSelectedAutomatically =
                    true

                // -------------------------------------------------
                // Short circuit
                // -------------------------------------------------

                val shortCircuitKA =
                    calculateShortCircuit(
                        system = system,
                        node = node
                    )

                feeder.shortCircuitCurrentKA =
                    shortCircuitKA

                // -------------------------------------------------
                // Breaker selection
                // -------------------------------------------------

                val breakerResult =
                    EngineeringDesignEngine.autoSelectBreaker(
                        designCurrentA = ib,

                        cableAmpacityA =
                            cableResult.correctedAmpacityA,

                        faultCurrentKA =
                            shortCircuitKA
                    )

                // -------------------------------------------------
                // Breaker failure
                // -------------------------------------------------

                if (
                    !breakerResult.success ||
                    breakerResult.breaker == null
                ) {

                    node.status =
                        NodeStatus.ERROR

                    val error =
                        "No suitable breaker found for " +
                                "${node.name}. " +
                                breakerResult.status

                    systemErrors.add(error)

                    feederResults.add(
                        FeederResult(
                            nodeId = node.id,
                            nodeName = node.name,

                            designCurrentA = ib,

                            cableSizeMm2 =
                                cable.sizeMm2,

                            cableMaterial =
                                feeder.cableMaterial,

                            insulation =
                                feeder.insulation,

                            installationMethod =
                                feeder.installationMethod,

                            parallelRuns =
                                cableResult.parallelRuns,

                            cableAmpacityA =
                                cableResult.correctedAmpacityA,

                            correctionFactor =
                                cableResult.correctionFactors.total(),

                            voltageDropPercent =
                                cableResult.voltageDropPercent,

                            maximumVoltageDropPercent =
                                maxVD,

                            breakerRatingA = 0.0,

                            breakerIcuKA = 0.0,

                            shortCircuitKA =
                                shortCircuitKA,

                            protectionValid = false,
                            designValid = false,

                            warnings = emptyList(),

                            errors =
                                listOf(error)
                        )
                    )

                    return@forEach
                }

                // -------------------------------------------------
                // Store breaker result
                // -------------------------------------------------

                val breaker =
                    breakerResult.breaker

                feeder.breakerRatingIn =
                    breaker.ratingA.toDouble()

                feeder.breakerIcuKA =
                    breaker.icuKA

                feeder.breakerSelectedAutomatically =
                    true

                // -------------------------------------------------
                // Protection check
                //
                // Ib ≤ In ≤ Iz
                //
                // Icu ≥ Ik
                // -------------------------------------------------

                val protectionValid =
                    checkProtection(
                        ib = ib,

                        breakerIn =
                            breaker.ratingA.toDouble(),

                        iz =
                            cableResult.correctedAmpacityA,

                        icu =
                            breaker.icuKA,

                        ik =
                            shortCircuitKA
                    )

                // -------------------------------------------------
                // Warnings / Errors
                // -------------------------------------------------

                val warnings =
                    mutableListOf<String>()

                val errors =
                    mutableListOf<String>()

                // Voltage drop
                if (
                    cableResult.voltageDropPercent >
                    maxVD
                ) {

                    errors.add(
                        "Voltage drop " +
                                "${format(
                                    cableResult.voltageDropPercent
                                )}% exceeds maximum " +
                                "${format(maxVD)}%."
                    )
                }

                // Cable capacity
                if (
                    cableResult.correctedAmpacityA <
                    ib
                ) {

                    errors.add(
                        "Cable ampacity is insufficient: " +
                                "Ib=${format(ib)} A, " +
                                "Iz=${format(
                                    cableResult.correctedAmpacityA
                                )} A."
                    )
                }

                // Ib ≤ In ≤ Iz
                if (
                    ib >
                    breaker.ratingA.toDouble()
                ) {

                    errors.add(
                        "Breaker rating is below design current."
                    )
                }

                if (
                    breaker.ratingA.toDouble() >
                    cableResult.correctedAmpacityA
                ) {

                    errors.add(
                        "Breaker rating exceeds cable ampacity."
                    )
                }

                // Icu ≥ Ik
                if (
                    breaker.icuKA <
                    shortCircuitKA
                ) {

                    errors.add(
                        "Breaker Icu " +
                                "${format(
                                    breaker.icuKA
                                )} kA is lower than " +
                                "short-circuit current " +
                                "${format(
                                    shortCircuitKA
                                )} kA."
                    )
                }

                // Protection summary
                if (!protectionValid) {

                    errors.add(
                        "Protection coordination failed."
                    )
                }

                // VD close to limit
                if (
                    cableResult.voltageDropPercent >
                    maxVD * 0.9 &&
                    cableResult.voltageDropPercent <=
                    maxVD
                ) {

                    warnings.add(
                        "Voltage drop is close to the design limit."
                    )
                }

                // Panel loading
                if (
                    calculation.loadingPercent >
                    90.0
                ) {

                    warnings.add(
                        "${node.name} loading exceeds 90%."
                    )
                }

                // -------------------------------------------------
                // Final feeder status
                // -------------------------------------------------

                val valid =
                    errors.isEmpty()

                node.status =
                    when {

                        !valid ->
                            NodeStatus.ERROR

                        warnings.isNotEmpty() ->
                            NodeStatus.WARNING

                        else ->
                            NodeStatus.CALCULATED
                    }

                // -------------------------------------------------
                // Add result
                // -------------------------------------------------

                feederResults.add(
                    FeederResult(

                        nodeId =
                            node.id,

                        nodeName =
                            node.name,

                        designCurrentA =
                            ib,

                        cableSizeMm2 =
                            cable.sizeMm2,

                        cableMaterial =
                            feeder.cableMaterial,

                        insulation =
                            feeder.insulation,

                        installationMethod =
                            feeder.installationMethod,

                        parallelRuns =
                            cableResult.parallelRuns,

                        cableAmpacityA =
                            cableResult.correctedAmpacityA,

                        correctionFactor =
                            cableResult.correctionFactors.total(),

                        voltageDropPercent =
                            cableResult.voltageDropPercent,

                        maximumVoltageDropPercent =
                            maxVD,

                        breakerRatingA =
                            breaker.ratingA.toDouble(),

                        breakerIcuKA =
                            breaker.icuKA,

                        shortCircuitKA =
                            shortCircuitKA,

                        protectionValid =
                            protectionValid,

                        designValid =
                            valid,

                        warnings =
                            warnings,

                        errors =
                            errors
                    )
                )
            }

        // ---------------------------------------------------------
        // 4. Transformer loading
        // ---------------------------------------------------------

        val transformerResult =
            DistributionCalculator.transformerLoading(
                system
            )

        if (
            transformerResult != null
        ) {

            if (
                transformerResult.warning
            ) {

                systemWarnings.add(
                    "Transformer loading is " +
                            "${format(
                                transformerResult.loadingPercent
                            )}%."
                )
            }

            if (
                transformerResult.overloaded
            ) {

                systemErrors.add(
                    "Transformer is overloaded. " +
                            "Recommended rating: " +
                            "${format(
                                transformerResult.recommendedKVA
                            )} kVA."
                )
            }
        }

        // ---------------------------------------------------------
        // 5. Final system validity
        // ---------------------------------------------------------

        val feedersValid =
            feederResults.all {
                it.designValid
            }

        val systemValid =
            validation.isValid &&
                    feedersValid &&
                    systemErrors.isEmpty()

        // ---------------------------------------------------------
        // 6. Return complete result
        // ---------------------------------------------------------

        return SystemResult(

            feeders =
                feederResults,

            transformer =
                transformerResult,

            totalConnectedKW =
                system.totalConnectedLoadKW(),

            totalDemandKW =
                system.totalDemandLoadKW(),

            totalDemandKVA =
                system.totalDemandLoadKVA(),

            systemValid =
                systemValid,

            warnings =
                systemWarnings,

            errors =
                systemErrors
        )
    }

    // =============================================================
    // POWER FACTOR
    // =============================================================

    private fun calculatePowerFactor(
        node: DistributionNode
    ): Double {

        val loads =
            node.loads

        if (loads.isEmpty()) {
            return 0.90
        }

        var totalKVA = 0.0
        var totalKW = 0.0

        loads.forEach { load ->

            val quantity =
                load.quantity
                    .coerceAtLeast(0.0)

            val unitKW =
                load.unitKW
                    .coerceAtLeast(0.0)

            val pf =
                load.powerFactor
                    .coerceIn(0.01, 1.0)

            val demandFactor =
                load.demandFactor
                    .coerceIn(0.0, 1.0)

            val kw =
                quantity *
                        unitKW *
                        demandFactor

            val kva =
                kw / pf

            totalKW += kw
            totalKVA += kva
        }

        return if (totalKVA > 0.0) {

            (totalKW / totalKVA)
                .coerceIn(0.01, 1.0)

        } else {

            0.90
        }
    }

    // =============================================================
    // PROTECTION CHECK
    // =============================================================

    private fun checkProtection(
        ib: Double,
        breakerIn: Double,
        iz: Double,
        icu: Double,
        ik: Double
    ): Boolean {

        val thermal =
            ib <= breakerIn &&
                    breakerIn <= iz

        val shortCircuit =
            icu >= ik

        return thermal &&
                shortCircuit
    }

    // =============================================================
    // SHORT CIRCUIT
    // =============================================================

    private fun calculateShortCircuit(
        system: DistributionSystem,
        node: DistributionNode
    ): Double {

        val transformer =
            system.getTransformer()
                ?: return 0.0

        if (
            transformer.ratedCapacity <= 0.0 ||
            transformer.voltage <= 0.0
        ) {
            return 0.0
        }

        // Default transformer impedance = 6%
        val transformerFaultKA =
            EngineeringDesignEngine
                .transformerShortCircuitKA(
                    transformerKVA =
                        transformer.ratedCapacity,

                    voltageV =
                        transformer.voltage,

                    impedancePercent =
                        6.0
                )

        if (transformerFaultKA <= 0.0) {
            return 0.0
        }

        val path =
            buildPath(
                system = system,
                node = node
            )

        var faultKA =
            transformerFaultKA

        path.forEach { pathNode ->

            val feeder =
                pathNode.feeder
                    ?: return@forEach

            if (
                feeder.lengthMeters <= 0.0 ||
                feeder.conductorSizeMm2 <= 0.0
            ) {
                return@forEach
            }

            faultKA =
                EngineeringDesignEngine
                    .downstreamShortCircuitKA(

                        upstreamFaultKA =
                            faultKA,

                        cableLengthM =
                            feeder.lengthMeters,

                        cableSizeMm2 =
                            feeder.conductorSizeMm2,

                        parallelRuns =
                            feeder.parallelRuns
                                .coerceAtLeast(1)
                    )
        }

        return faultKA
    }

    // =============================================================
    // BUILD HIERARCHY PATH
    // =============================================================

    private fun buildPath(
        system: DistributionSystem,
        node: DistributionNode
    ): List<DistributionNode> {

        val result =
            mutableListOf<DistributionNode>()

        var current:
                DistributionNode? = node

        while (current != null) {

            if (
                current.type !=
                DistributionNodeType.TRANSFORMER
            ) {

                result.add(current)
            }

            current =
                current.parentId?.let {
                    system.getNode(it)
                }
        }

        return result.asReversed()
    }

    // =============================================================
    // FORMAT
    // =============================================================

    private fun format(
        value: Double
    ): String {

        return "%.2f".format(value)
    }
}
