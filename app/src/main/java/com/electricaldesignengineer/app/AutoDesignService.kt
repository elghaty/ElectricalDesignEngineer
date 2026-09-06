package com.electricaldesignengineer.app

/**
 * AutoDesignService
 *
 * Main automatic design workflow:
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
 * Design Status
 *
 * The service uses EngineeringDesignEngine for
 * the actual electrical calculations.
 */
object AutoDesignService {

    data class FeederResult(

        val nodeId: String,

        val nodeName: String,

        val designCurrentA: Double,

        val cableSizeMm2: Double,

        val cableMaterial: CableMaterial,

        val insulation: InsulationType,

        val installationMethod: InstallationMethod,

        val parallelRuns: Int,

        val cableAmpacityA: Double,

        val correctionFactor: Double,

        val voltageDropPercent: Double,

        val maximumVoltageDropPercent: Double,

        val breakerRatingA: Double,

        val breakerIcuKA: Double,

        val breakerIcsKA: Double,

        val shortCircuitKA: Double,

        val protectionValid: Boolean,

        val designValid: Boolean,

        val warnings: List<String>,

        val errors: List<String>
    )


    data class SystemResult(

        val feeders: List<FeederResult>,

        val transformer: TransformerLoadingResult?,

        val totalConnectedKW: Double,

        val totalDemandKW: Double,

        val totalDemandKVA: Double,

        val systemValid: Boolean,

        val warnings: List<String>,

        val errors: List<String>
    )


    /**
     * Run complete automatic design.
     *
     * Existing feeder selections are respected where possible.
     * Cable size and parallel runs are automatically increased
     * when the selected cable is insufficient.
     */
    fun design(
        system: DistributionSystem,
        defaultMaximumVoltageDropPercent: Double = 5.0
    ): SystemResult {

        val systemWarnings =
            mutableListOf<String>()

        val systemErrors =
            mutableListOf<String>()

        /*
         * Validate hierarchy first.
         */
        val validation =
            system.validate()

        systemWarnings.addAll(
            validation.warnings
        )

        systemErrors.addAll(
            validation.errors
        )

        /*
         * Calculate accumulated loads.
         */
        val distributionResults =
            DistributionCalculator.calculate(
                system
            )

        val feederResults =
            mutableListOf<FeederResult>()


        /*
         * Design every node that has a feeder.
         */
        system.hierarchyOrder()
            .filter {
                it.type != DistributionNodeType.TRANSFORMER
            }
            .forEach { node ->

                val feeder =
                    node.feeder

                /*
                 * A feeder is required for
                 * downstream distribution nodes.
                 */
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


                /*
                 * Design current.
                 */
                val ib =
                    calculation.currentA


                /*
                 * Use user-defined VD limit.
                 * If invalid, use 5%.
                 */
                val maxVD =
                    if (
                        feeder.maximumVoltageDropPercent > 0.0
                    ) {
                        feeder.maximumVoltageDropPercent
                    } else {
                        defaultMaximumVoltageDropPercent
                    }


                /*
                 * Cable selection.
                 */
                val cableResult =
                    EngineeringDesignEngine.autoSelectCable(
                        designCurrentA = ib,
                        lengthMeters =
                            feeder.lengthMeters.coerceAtLeast(0.1),
                        material =
                            feeder.cableMaterial,
                        insulation =
                            feeder.insulation,
                        installationMethod =
                            feeder.installationMethod,
                        cores =
                            feeder.numberOfCores,
                        maxVoltageDropPercent =
                            maxVD
                    )


                /*
                 * Store automatic cable result.
                 */
                feeder.conductorSizeMm2 =
                    cableResult.cable.sizeMm2

                feeder.parallelRuns =
                    cableResult.parallelRuns

                feeder.ampacityIz =
                    cableResult.correctedAmpacityA

                feeder.designCurrentIb =
                    ib

                feeder.voltageDropPercent =
                    cableResult.voltageDropPercent

                feeder.correctionFactorTotal =
                    cableResult.totalCorrectionFactor

                feeder.cableSelectedAutomatically =
                    true


                /*
                 * Short circuit at this node.
                 *
                 * This uses the current engine's
                 * available short-circuit model.
                 */
                val shortCircuitKA =
                    calculateShortCircuit(
                        system = system,
                        node = node
                    )


                feeder.shortCircuitCurrentKA =
                    shortCircuitKA


                /*
                 * Breaker selection.
                 */
                val breakerResult =
                    EngineeringDesignEngine.autoSelectBreaker(
                        designCurrentA = ib,
                        cableAmpacityA =
                            cableResult.correctedAmpacityA,
                        shortCircuitKA =
                            shortCircuitKA
                    )


                feeder.breakerRatingIn =
                    breakerResult.ratingA

                feeder.breakerIcuKA =
                    breakerResult.icuKA

                feeder.breakerIcsKA =
                    breakerResult.icsKA

                feeder.breakerSelectedAutomatically =
                    true


                /*
                 * Protection check:
                 *
                 * Ib ≤ In ≤ Iz
                 *
                 * and
                 *
                 * Icu/Ics ≥ Ik
                 */
                val protectionValid =
                    checkProtection(
                        ib = ib,
                        breakerIn =
                            breakerResult.ratingA,
                        iz =
                            cableResult.correctedAmpacityA,
                        icu =
                            breakerResult.icuKA,
                        ics =
                            breakerResult.icsKA,
                        ik =
                            shortCircuitKA
                    )


                val warnings =
                    mutableListOf<String>()

                val errors =
                    mutableListOf<String>()


                /*
                 * Voltage drop warning.
                 */
                if (
                    cableResult.voltageDropPercent >
                    maxVD
                ) {

                    errors.add(
                        "Voltage drop ${format(
                            cableResult.voltageDropPercent
                        )}% exceeds maximum ${format(
                            maxVD
                        )}%."
                    )
                }


                /*
                 * Cable thermal capacity.
                 */
                if (
                    cableResult.correctedAmpacityA <
                    ib
                ) {

                    errors.add(
                        "Cable ampacity is insufficient."
                    )
                }


                /*
                 * Protection.
                 */
                if (!protectionValid) {

                    errors.add(
                        "Protection condition Ib ≤ In ≤ Iz " +
                                "or Icu/Ics ≥ Ik is not satisfied."
                    )
                }


                /*
                 * Short circuit warning.
                 */
                if (
                    breakerResult.icuKA <
                    shortCircuitKA
                ) {

                    errors.add(
                        "Breaker Icu is lower than short-circuit current."
                    )
                }


                /*
                 * High voltage drop warning.
                 */
                if (
                    cableResult.voltageDropPercent >
                    maxVD * 0.9 &&
                    cableResult.voltageDropPercent <= maxVD
                ) {

                    warnings.add(
                        "Voltage drop is close to the design limit."
                    )
                }


                /*
                 * High loading warning.
                 */
                if (
                    calculation.loadingPercent > 90.0
                ) {

                    warnings.add(
                        "${node.name} loading exceeds 90%."
                    )
                }


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


                feederResults.add(

                    FeederResult(

                        nodeId =
                            node.id,

                        nodeName =
                            node.name,

                        designCurrentA =
                            ib,

                        cableSizeMm2 =
                            cableResult.cable.sizeMm2,

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
                            cableResult.totalCorrectionFactor,

                        voltageDropPercent =
                            cableResult.voltageDropPercent,

                        maximumVoltageDropPercent =
                            maxVD,

                        breakerRatingA =
                            breakerResult.ratingA,

                        breakerIcuKA =
                            breakerResult.icuKA,

                        breakerIcsKA =
                            breakerResult.icsKA,

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


        /*
         * Transformer calculation.
         */
        val transformerResult =
            DistributionCalculator.transformerLoading(
                system
            )


        /*
         * Transformer warning.
         */
        if (
            transformerResult != null &&
            transformerResult.warning
        ) {

            systemWarnings.add(
                "Transformer loading is " +
                        "${format(
                            transformerResult.loadingPercent
                        )}%."
            )
        }


        /*
         * Transformer overload.
         */
        if (
            transformerResult != null &&
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


        /*
         * System validity.
         */
        val feedersValid =
            feederResults.all {
                it.designValid
            }

        val systemValid =
            validation.isValid &&
                    feedersValid &&
                    systemErrors.isEmpty()


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


    /**
     * Protection check.
     *
     * Basic requirement:
     *
     * Ib ≤ In ≤ Iz
     *
     * Icu ≥ Ik
     *
     * Ics ≥ Ik
     */
    private fun checkProtection(
        ib: Double,
        breakerIn: Double,
        iz: Double,
        icu: Double,
        ics: Double,
        ik: Double
    ): Boolean {

        val thermal =
            ib <= breakerIn &&
                    breakerIn <= iz

        val shortCircuit =
            icu >= ik &&
                    ics >= ik

        return thermal &&
                shortCircuit
    }


    /**
     * Calculate short-circuit current.
     *
     * If the transformer exists, use its
     * available short-circuit current as the source.
     *
     * The downstream reduction is handled by the
     * existing EngineeringDesignEngine function.
     */
    private fun calculateShortCircuit(
        system: DistributionSystem,
        node: DistributionNode
    ): Double {

        val transformer =
            system.getTransformer()
                ?: return 0.0


        /*
         * Transformer fault current.
         *
         * Default transformer impedance = 6%.
         *
         * This is a preliminary default and should
         * later become a project input.
         */
        val transformerFaultKA =
            EngineeringDesignEngine
                .transformerShortCircuitCurrent(
                    transformerKVA =
                        transformer.ratedCapacity,
                    voltage =
                        transformer.voltage,
                    impedancePercent =
                        6.0
                )


        /*
         * Build downstream path.
         */
        val path =
            buildPath(
                system = system,
                node = node
            )


        /*
         * Apply feeder impedance effects.
         *
         * The detailed IEC impedance model will
         * be upgraded in the engineering engine.
         */
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
                    .downstreamShortCircuitCurrent(
                        sourceShortCircuitKA =
                            faultKA,
                        cableLengthMeters =
                            feeder.lengthMeters,
                        cableSizeMm2 =
                            feeder.conductorSizeMm2,
                        parallelRuns =
                            feeder.parallelRuns
                    )
        }


        return faultKA
    }


    /**
     * Build path:
     *
     * Transformer → ... → Node
     */
    private fun buildPath(
        system: DistributionSystem,
        node: DistributionNode
    ): List<DistributionNode> {

        val result =
            mutableListOf<DistributionNode>()

        var current: DistributionNode? =
            node


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


        /*
         * Transformer → downstream order.
         */
        return result.asReversed()
    }


    private fun format(
        value: Double
    ): String {

        return "%.2f".format(value)
    }
}
