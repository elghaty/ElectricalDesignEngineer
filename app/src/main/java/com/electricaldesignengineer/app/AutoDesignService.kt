package com.electricaldesignengineer.app

/**
 * AutoDesignService
 *
 * Automatic electrical distribution design service.
 *
 * Design sequence:
 *
 * Loads
 *   ↓
 * Demand Load
 *   ↓
 * Demand Current (Ib)
 *   ↓
 * Cable Selection
 *   ↓
 * Correction Factors
 *   ↓
 * Voltage Drop
 *   ↓
 * Short Circuit
 *   ↓
 * Breaker Selection
 *   ↓
 * Protection Check
 *   ↓
 * Final Design Status
 */
object AutoDesignService {

    // ============================================================
    // FEEDER RESULT
    // ============================================================

    data class FeederResult(

        val nodeId: String,

        val nodeName: String,

        val designCurrentA: Double,

        val cableSizeMm2: Double,

        val cableMaterial:
            EngineeringDesignEngine.CableMaterial,

        val insulation:
            EngineeringDesignEngine.InsulationType,

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

    // ============================================================
    // SYSTEM RESULT
    // ============================================================

    data class SystemResult(

        val feeders: List<FeederResult>,

        val transformer:
            TransformerLoadingResult?,

        val totalConnectedKW: Double,

        val totalDemandKW: Double,

        val totalDemandKVA: Double,

        val systemValid: Boolean,

        val warnings: List<String>,

        val errors: List<String>
    )

    // ============================================================
    // MAIN AUTO DESIGN
    // ============================================================

    fun design(
        system: DistributionSystem,

        defaultMaximumVoltageDropPercent: Double = 5.0

    ): SystemResult {

        val systemWarnings =
            mutableListOf<String>()

        val systemErrors =
            mutableListOf<String>()

        // --------------------------------------------------------
        // 1. VALIDATE SYSTEM
        // --------------------------------------------------------

        val validation =
            system.validate()

        systemWarnings.addAll(
            validation.warnings
        )

        systemErrors.addAll(
            validation.errors
        )

        // --------------------------------------------------------
        // 2. CALCULATE DISTRIBUTION
        // --------------------------------------------------------

        val calculations =
            DistributionCalculator.calculate(
                system
            )

        val feederResults =
            mutableListOf<FeederResult>()

        // --------------------------------------------------------
        // 3. DESIGN ALL FEEDERS
        // --------------------------------------------------------

        system.hierarchyOrder()
            .filter {
                it.type !=
                        DistributionNodeType.TRANSFORMER
            }
            .forEach { node ->

                val feeder =
                    node.feeder

                // ------------------------------------------------
                // No feeder
                // ------------------------------------------------

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

                // ------------------------------------------------
                // Find calculation
                // ------------------------------------------------

                val calculation =
                    calculations.firstOrNull {

                        it.nodeId ==
                                node.id
                    }

                if (calculation == null) {

                    val error =
                        "Unable to calculate ${node.name}."

                    systemErrors.add(
                        error
                    )

                    node.status =
                        NodeStatus.ERROR

                    return@forEach
                }

                // ------------------------------------------------
                // DESIGN CURRENT
                // ------------------------------------------------

                val ib =
                    calculation.currentA

                if (ib <= 0.0) {

                    val warning =
                        "${node.name}: design current is zero."

                    systemWarnings.add(
                        warning
                    )

                    node.status =
                        NodeStatus.WARNING

                    return@forEach
                }

                // ------------------------------------------------
                // MAXIMUM VOLTAGE DROP
                // ------------------------------------------------

                val maxVoltageDrop =
                    if (
                        feeder.maximumVoltageDropPercent >
                        0.0
                    ) {

                        feeder.maximumVoltageDropPercent

                    } else {

                        defaultMaximumVoltageDropPercent
                    }

                // ------------------------------------------------
                // POWER FACTOR
                // ------------------------------------------------

                val powerFactor =
                    calculatePowerFactor(
                        node
                    )

                // ------------------------------------------------
                // PHASE
                // ------------------------------------------------

                val threePhase =
                    node.phaseType ==
                            PhaseType.THREE_PHASE

                // ------------------------------------------------
                // CABLE SELECTION
                // ------------------------------------------------

                val cableResult =
                    EngineeringDesignEngine.autoSelectCable(

                        designCurrentA =
                            ib,

                        lengthM =
                            feeder.lengthMeters
                                .coerceAtLeast(
                                    0.1
                                ),

                        voltageV =
                            node.voltage
                                .coerceAtLeast(
                                    1.0
                                ),

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
                            maxVoltageDrop
                    )

                // ------------------------------------------------
                // CABLE NOT FOUND
                // ------------------------------------------------

                if (
                    !cableResult.success ||
                    cableResult.cable == null
                ) {

                    val error =
                        "No suitable cable found for " +
                                "${node.name}. " +
                                cableResult.status

                    systemErrors.add(
                        error
                    )

                    node.status =
                        NodeStatus.ERROR

                    feederResults.add(

                        FeederResult(

                            nodeId =
                                node.id,

                            nodeName =
                                node.name,

                            designCurrentA =
                                ib,

                            cableSizeMm2 =
                                0.0,

                            cableMaterial =
                                feeder.cableMaterial,

                            insulation =
                                feeder.insulation,

                            installationMethod =
                                feeder.installationMethod,

                            parallelRuns =
                                0,

                            cableAmpacityA =
                                0.0,

                            correctionFactor =
                                0.0,

                            voltageDropPercent =
                                0.0,

                            maximumVoltageDropPercent =
                                maxVoltageDrop,

                            breakerRatingA =
                                0.0,

                            breakerIcuKA =
                                0.0,

                            shortCircuitKA =
                                0.0,

                            protectionValid =
                                false,

                            designValid =
                                false,

                            warnings =
                                emptyList(),

                            errors =
                                listOf(
                                    error
                                )
                        )
                    )

                    return@forEach
                }

                // ------------------------------------------------
                // CABLE FOUND
                // ------------------------------------------------

                val cable =
                    cableResult.cable

                // ------------------------------------------------
                // SAVE CABLE DATA
                // ------------------------------------------------

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
                    cableResult
                        .correctionFactors
                        .total()

                feeder.cableSelectedAutomatically =
                    true

                // ------------------------------------------------
                // SHORT CIRCUIT
                // ------------------------------------------------

                val shortCircuitKA =
                    calculateShortCircuit(
                        system = system,
                        node = node
                    )

                feeder.shortCircuitCurrentKA =
                    shortCircuitKA

                // ------------------------------------------------
                // BREAKER SELECTION
                // ------------------------------------------------

                val breakerResult =
                    EngineeringDesignEngine.autoSelectBreaker(

                        designCurrentA =
                            ib,

                        cableAmpacityA =
                            cableResult.correctedAmpacityA,

                        faultCurrentKA =
                            shortCircuitKA
                    )

                // ------------------------------------------------
                // BREAKER NOT FOUND
                // ------------------------------------------------

                if (
                    !breakerResult.success ||
                    breakerResult.breaker == null
                ) {

                    val error =
                        "No suitable breaker found for " +
                                "${node.name}. " +
                                breakerResult.status

                    systemErrors.add(
                        error
                    )

                    node.status =
                        NodeStatus.ERROR

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
                                cableResult
                                    .correctionFactors
                                    .total(),

                            voltageDropPercent =
                                cableResult
                                    .voltageDropPercent,

                            maximumVoltageDropPercent =
                                maxVoltageDrop,

                            breakerRatingA =
                                0.0,

                            breakerIcuKA =
                                0.0,

                            shortCircuitKA =
                                shortCircuitKA,

                            protectionValid =
                                false,

                            designValid =
                                false,

                            warnings =
                                emptyList(),

                            errors =
                                listOf(
                                    error
                                )
                        )
                    )

                    return@forEach
                }

                // ------------------------------------------------
                // BREAKER FOUND
                // ------------------------------------------------

                val breaker =
                    breakerResult.breaker

                feeder.breakerRatingIn =
                    breaker.ratingA.toDouble()

                feeder.breakerIcuKA =
                    breaker.icuKA

                feeder.breakerSelectedAutomatically =
                    true

                // ------------------------------------------------
                // PROTECTION CHECK
                // ------------------------------------------------

                val protectionValid =
                    checkProtection(

                        ib =
                            ib,

                        breakerIn =
                            breaker.ratingA
                                .toDouble(),

                        iz =
                            cableResult
                                .correctedAmpacityA,

                        icu =
                            breaker.icuKA,

                        ik =
                            shortCircuitKA
                    )

                // ------------------------------------------------
                // WARNINGS
                // ------------------------------------------------

                val warnings =
                    mutableListOf<String>()

                // ------------------------------------------------
                // ERRORS
                // ------------------------------------------------

                val errors =
                    mutableListOf<String>()

                // ------------------------------------------------
                // VOLTAGE DROP
                // ------------------------------------------------

                if (
                    cableResult
                        .voltageDropPercent >
                    maxVoltageDrop
                ) {

                    errors.add(

                        "Voltage drop " +
                                "${format(
                                    cableResult
                                        .voltageDropPercent
                                )}% exceeds maximum " +
                                "${format(
                                    maxVoltageDrop
                                )}%."
                    )
                }

                // ------------------------------------------------
                // CABLE AMPACITY
                // ------------------------------------------------

                if (
                    cableResult
                        .correctedAmpacityA <
                    ib
                ) {

                    errors.add(

                        "Cable ampacity is insufficient: " +
                                "Ib=${format(ib)} A, " +
                                "Iz=${format(
                                    cableResult
                                        .correctedAmpacityA
                                )} A."
                    )
                }

                // ------------------------------------------------
                // Ib <= In
                // ------------------------------------------------

                if (
                    ib >
                    breaker.ratingA
                        .toDouble()
                ) {

                    errors.add(
                        "Breaker rating is below design current."
                    )
                }

                // ------------------------------------------------
                // In <= Iz
                // ------------------------------------------------

                if (
                    breaker.ratingA
                        .toDouble() >
                    cableResult
                        .correctedAmpacityA
                ) {

                    errors.add(
                        "Breaker rating exceeds cable ampacity."
                    )
                }

                // ------------------------------------------------
                // Icu >= Ik
                // ------------------------------------------------

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

                // ------------------------------------------------
                // PROTECTION
                // ------------------------------------------------

                if (!protectionValid) {

                    errors.add(
                        "Protection coordination failed."
                    )
                }

                // ------------------------------------------------
                // VOLTAGE DROP WARNING
                // ------------------------------------------------

                if (
                    cableResult
                        .voltageDropPercent >
                    maxVoltageDrop * 0.90 &&
                    cableResult
                        .voltageDropPercent <=
                    maxVoltageDrop
                ) {

                    warnings.add(
                        "Voltage drop is close to the design limit."
                    )
                }

                // ------------------------------------------------
                // PANEL LOADING
                // ------------------------------------------------

                if (
                    calculation.loadingPercent >
                    90.0
                ) {

                    warnings.add(

                        "${node.name} loading exceeds 90%."
                    )
                }

                // ------------------------------------------------
                // FINAL STATUS
                // ------------------------------------------------

                val designValid =
                    errors.isEmpty()

                node.status =
                    when {

                        !designValid ->
                            NodeStatus.ERROR

                        warnings.isNotEmpty() ->
                            NodeStatus.WARNING

                        else ->
                            NodeStatus.CALCULATED
                    }

                // ------------------------------------------------
                // RESULT
                // ------------------------------------------------

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
                            cableResult
                                .correctedAmpacityA,

                        correctionFactor =
                            cableResult
                                .correctionFactors
                                .total(),

                        voltageDropPercent =
                            cableResult
                                .voltageDropPercent,

                        maximumVoltageDropPercent =
                            maxVoltageDrop,

                        breakerRatingA =
                            breaker.ratingA
                                .toDouble(),

                        breakerIcuKA =
                            breaker.icuKA,

                        shortCircuitKA =
                            shortCircuitKA,

                        protectionValid =
                            protectionValid,

                        designValid =
                            designValid,

                        warnings =
                            warnings,

                        errors =
                            errors
                    )
                )
            }

        // ========================================================
        // TRANSFORMER
        // ========================================================

        val transformerResult =
            DistributionCalculator
                .transformerLoading(
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
                                transformerResult
                                    .loadingPercent
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
                                transformerResult
                                    .recommendedKVA
                            )} kVA."
                )
            }
        }

        // ========================================================
        // SYSTEM VALIDATION
        // ========================================================

        val feedersValid =
            feederResults.all {
                it.designValid
            }

        val systemValid =
            validation.isValid &&
                    feedersValid &&
                    systemErrors.isEmpty()

        // ========================================================
        // RETURN
        // ========================================================

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

    // ============================================================
    // POWER FACTOR
    // ============================================================

    private fun calculatePowerFactor(
        node: DistributionNode
    ): Double {

        val loads =
            node.loads

        if (loads.isEmpty()) {
            return 0.90
        }

        var totalKW =
            0.0

        var totalKVA =
            0.0

        loads.forEach { load ->

            val quantity =
                load.quantity
                    .coerceAtLeast(0)

            val unitKW =
                load.unitKW
                    .coerceAtLeast(0.0)

            val demandFactor =
                load.demandFactor
                    .coerceIn(
                        0.0,
                        1.0
                    )

            val powerFactor =
                load.powerFactor
                    .coerceIn(
                        0.10,
                        1.0
                    )

            val demandKW =
                quantity *
                        unitKW *
                        demandFactor

            val demandKVA =
                demandKW /
                        powerFactor

            totalKW +=
                demandKW

            totalKVA +=
                demandKVA
        }

        return if (
            totalKVA > 0.0
        ) {

            (
                totalKW /
                        totalKVA
            ).coerceIn(
                0.10,
                1.0
            )

        } else {

            0.90
        }
    }

    // ============================================================
    // PROTECTION CHECK
    // ============================================================

    private fun checkProtection(

        ib: Double,

        breakerIn: Double,

        iz: Double,

        icu: Double,

        ik: Double

    ): Boolean {

        val thermalCondition =
            ib <= breakerIn &&
                    breakerIn <= iz

        val shortCircuitCondition =
            icu >= ik

        return thermalCondition &&
                shortCircuitCondition
    }

    // ============================================================
    // SHORT CIRCUIT CALCULATION
    // ============================================================

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

        /*
         * Preliminary transformer short-circuit calculation.
         *
         * Default transformer impedance = 6%.
         *
         * This value should later be replaced by the actual
         * transformer nameplate impedance.
         */

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

        if (
            transformerFaultKA <= 0.0
        ) {

            return 0.0
        }

        // --------------------------------------------------------
        // BUILD PATH FROM TRANSFORMER TO NODE
        // --------------------------------------------------------

        val path =
            buildPath(
                system = system,
                node = node
            )

        var faultKA =
            transformerFaultKA

        // --------------------------------------------------------
        // APPLY EACH FEEDER
        // --------------------------------------------------------

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

    // ============================================================
    // BUILD PATH
    // ============================================================

    private fun buildPath(

        system: DistributionSystem,

        node: DistributionNode

    ): List<DistributionNode> {

        val path =
            mutableListOf<DistributionNode>()

        var current:
                DistributionNode? =
            node

        while (
            current != null
        ) {

            if (
                current.type !=
                DistributionNodeType.TRANSFORMER
            ) {

                path.add(
                    current
                )
            }

            current =
                current.parentId?.let {
                    system.getNode(it)
                }
        }

        return path.asReversed()
    }

    // ============================================================
    // NUMBER FORMAT
    // ============================================================

    private fun format(
        value: Double
    ): String {

        return "%.2f".format(
            value
        )
    }
}
