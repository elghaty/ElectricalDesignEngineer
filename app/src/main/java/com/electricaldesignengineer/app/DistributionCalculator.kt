package com.electricaldesignengineer.app

/**
 * DistributionCalculator
 *
 * Orchestrator only.
 *
 * IMPORTANT:
 * - Electrical calculations are delegated to ProfessionalEngineeringCore.
 * - Equipment data is obtained from EngineeringCatalogRepository.
 * - No cable, breaker, or transformer engineering tables are hardcoded here.
 *
 * Frequency:
 * - 50 Hz
 * - 60 Hz
 *
 * The selected frequency is passed directly to ProfessionalEngineeringCore.
 */
object DistributionCalculator {

    /**
     * Supported system frequencies.
     *
     * 50 Hz is the default because it is the normal operating frequency
     * for the current Egyptian-market catalog data.
     */
    enum class SystemFrequency(
        val valueHz: Double,
        val displayName: String
    ) {
        HZ_50(
            valueHz = 50.0,
            displayName = "50 Hz"
        ),

        HZ_60(
            valueHz = 60.0,
            displayName = "60 Hz"
        )
    }

    data class NodeCalculation(
        val nodeId: String,
        val nodeName: String,
        val nodeType: DistributionNodeType,
        val connectedKW: Double,
        val demandKW: Double,
        val demandKVA: Double,
        val currentA: Double,
        val loadingPercent: Double,
        val status: NodeStatus
    )

    /**
     * Calculate the complete distribution system.
     *
     * Default frequency = 50 Hz.
     *
     * Existing callers can continue using:
     *
     * calculate(system)
     *
     * For 60 Hz:
     *
     * calculate(
     *     system = system,
     *     frequency = SystemFrequency.HZ_60
     * )
     */
    fun calculate(
        system: DistributionSystem,
        frequency: SystemFrequency = SystemFrequency.HZ_50
    ): List<NodeCalculation> {

        val results =
            mutableMapOf<String, NodeCalculation>()

        system.hierarchyOrder()
            .asReversed()
            .forEach { node ->

                results[node.id] =
                    calculateNode(
                        system = system,
                        node = node,
                        frequency = frequency
                    )
            }

        return system.hierarchyOrder()
            .mapNotNull { node ->
                results[node.id]
            }
    }

    /**
     * Convenience overload for direct frequency input.
     *
     * Example:
     *
     * calculate(system, 60.0)
     *
     * Only 50 Hz and 60 Hz are accepted.
     */
    fun calculate(
        system: DistributionSystem,
        frequencyHz: Double
    ): List<NodeCalculation> {

        val frequency =
            when {
                kotlin.math.abs(frequencyHz - 50.0) < 0.001 ->
                    SystemFrequency.HZ_50

                kotlin.math.abs(frequencyHz - 60.0) < 0.001 ->
                    SystemFrequency.HZ_60

                else ->
                    throw IllegalArgumentException(
                        "Unsupported system frequency: $frequencyHz Hz. " +
                                "Only 50 Hz and 60 Hz are supported."
                    )
            }

        return calculate(
            system = system,
            frequency = frequency
        )
    }

    private fun calculateNode(
        system: DistributionSystem,
        node: DistributionNode,
        frequency: SystemFrequency
    ): NodeCalculation {

        val accumulatedLoads =
            collectLoads(
                system = system,
                node = node
            )

        val phaseSystem =
            when (node.phaseType) {

                PhaseType.THREE_PHASE ->
                    ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE

                PhaseType.SINGLE_PHASE_L1,
                PhaseType.SINGLE_PHASE_L2,
                PhaseType.SINGLE_PHASE_L3 ->
                    ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
            }

        val systemInput =
            ProfessionalEngineeringCore.SystemInput(
                voltageV = node.voltage,
                frequencyHz = frequency.valueHz,
                phaseSystem = phaseSystem,
                powerFactor =
                    determineSystemPowerFactor(
                        accumulatedLoads
                    )
            )

        val coreResult =
            ProfessionalEngineeringCore.calculateLoads(
                loads = accumulatedLoads,
                system = systemInput
            )

        val connectedKW =
            coreResult.connectedKW

        val demandKW =
            coreResult.demandKW

        val demandKVA =
            coreResult.demandKVA

        val currentA =
            coreResult.currentA

        val loadingPercent =
            calculateLoading(
                node = node,
                demandKVA = demandKVA,
                currentA = currentA
            )

        val status =
            when {

                coreResult.checks.any {
                    it.status == EngineeringStatus.FAIL
                } ->
                    NodeStatus.ERROR

                loadingPercent > 100.0 ->
                    NodeStatus.ERROR

                loadingPercent > 90.0 ->
                    NodeStatus.WARNING

                else ->
                    NodeStatus.CALCULATED
            }

        return NodeCalculation(
            nodeId = node.id,
            nodeName = node.name,
            nodeType = node.type,
            connectedKW = connectedKW,
            demandKW = demandKW,
            demandKVA = demandKVA,
            currentA = currentA,
            loadingPercent = loadingPercent,
            status = status
        )
    }

    /**
     * Collect own loads and all descendant loads.
     */
    private fun collectLoads(
        system: DistributionSystem,
        node: DistributionNode
    ): List<ProfessionalEngineeringCore.LoadInput> {

        val loads =
            mutableListOf<ProfessionalEngineeringCore.LoadInput>()

        addNodeLoads(
            node = node,
            target = loads
        )

        system.getChildren(node.id)
            .forEach { child ->

                loads +=
                    collectLoads(
                        system = system,
                        node = child
                    )
            }

        return loads
    }

    /**
     * Convert DistributionLoad into Core LoadInput.
     */
    private fun addNodeLoads(
        node: DistributionNode,
        target:
        MutableList<ProfessionalEngineeringCore.LoadInput>
    ) {

        node.loads.forEach { load ->

            target +=
                ProfessionalEngineeringCore.LoadInput(
                    name = load.name,
                    quantity = load.quantity.toDouble(),
                    unitPowerKW = load.unitKW,
                    demandFactor = load.demandFactor,
                    powerFactor = load.powerFactor
                )
        }
    }

    /**
     * Provides a representative PF input to the Core.
     *
     * The actual electrical calculation of effective PF
     * remains inside ProfessionalEngineeringCore.
     */
    private fun determineSystemPowerFactor(
        loads:
        List<ProfessionalEngineeringCore.LoadInput>
    ): Double {

        if (loads.isEmpty()) {
            return 1.0
        }

        val weightedPower =
            loads.sumOf { load ->

                load.quantity
                    .coerceAtLeast(0.0) *
                        load.unitPowerKW
                            .coerceAtLeast(0.0) *
                        load.demandFactor
                            .coerceIn(0.0, 1.0)
            }

        if (weightedPower <= 0.0) {
            return 1.0
        }

        val weightedPF =
            loads.sumOf { load ->

                val quantity =
                    load.quantity
                        .coerceAtLeast(0.0)

                val power =
                    load.unitPowerKW
                        .coerceAtLeast(0.0)

                val demandFactor =
                    load.demandFactor
                        .coerceIn(0.0, 1.0)

                val pf =
                    load.powerFactor
                        .coerceIn(0.01, 1.0)

                quantity *
                        power *
                        demandFactor *
                        pf
            } / weightedPower

        return weightedPF.coerceIn(
            0.01,
            1.0
        )
    }

    /**
     * Loading is an assessment/presentation value.
     *
     * Transformer:
     * demand kVA / transformer kVA
     *
     * Other nodes:
     * current / rated capacity
     */
    private fun calculateLoading(
        node: DistributionNode,
        demandKVA: Double,
        currentA: Double
    ): Double {

        if (node.ratedCapacity <= 0.0) {
            return 0.0
        }

        return when (node.type) {

            DistributionNodeType.TRANSFORMER -> {

                demandKVA /
                        node.ratedCapacity *
                        100.0
            }

            else -> {

                currentA /
                        node.ratedCapacity *
                        100.0
            }
        }
    }

    /**
     * Calculate transformer loading and recommend
     * the smallest VERIFIED transformer catalog item
     * capable of carrying the calculated demand.
     *
     * No transformer ratings are hardcoded here.
     *
     * Frequency is included so future transformer catalog
     * filtering can distinguish 50 Hz from 60 Hz equipment.
     */
    fun transformerLoading(
        system: DistributionSystem,
        frequency: SystemFrequency = SystemFrequency.HZ_50
    ): TransformerLoadingResult? {

        val transformer =
            system.getTransformer()
                ?: return null

        val calculation =
            calculate(
                system = system,
                frequency = frequency
            )
                .firstOrNull {
                    it.nodeId == transformer.id
                }
                ?: return null

        val demandKVA =
            calculation.demandKVA

        val transformerRating =
            transformer.ratedCapacity

        val loadingPercent =
            if (transformerRating > 0.0) {

                demandKVA /
                        transformerRating *
                        100.0

            } else {
                0.0
            }

        val recommendedTransformer =
            findRecommendedTransformer(
                demandKVA = demandKVA
            )

        val recommendedKVA =
            recommendedTransformer
                ?.ratedPowerKVA
                ?: 0.0

        return TransformerLoadingResult(
            transformerId = transformer.id,
            transformerName = transformer.name,
            transformerRatingKVA =
                transformerRating,
            demandKVA =
                demandKVA,
            loadingPercent =
                loadingPercent,
            recommendedKVA =
                recommendedKVA,
            overloaded =
                loadingPercent > 100.0,
            warning =
                loadingPercent > 85.0
        )
    }

    /**
     * Direct frequency overload for transformer loading.
     */
    fun transformerLoading(
        system: DistributionSystem,
        frequencyHz: Double
    ): TransformerLoadingResult? {

        val frequency =
            when {
                kotlin.math.abs(frequencyHz - 50.0) < 0.001 ->
                    SystemFrequency.HZ_50

                kotlin.math.abs(frequencyHz - 60.0) < 0.001 ->
                    SystemFrequency.HZ_60

                else ->
                    throw IllegalArgumentException(
                        "Unsupported system frequency: $frequencyHz Hz. " +
                                "Only 50 Hz and 60 Hz are supported."
                    )
            }

        return transformerLoading(
            system = system,
            frequency = frequency
        )
    }

    /**
     * Find the smallest verified transformer in the
     * engineering catalog whose rated power is not less
     * than the calculated demand.
     *
     * The catalog is the source of transformer ratings.
     */
    private fun findRecommendedTransformer(
        demandKVA: Double
    ): EngineeringCatalogRepository.TransformerRecord? {

        if (demandKVA <= 0.0) {
            return null
        }

        return EngineeringCatalogRepository
            .searchTransformers(
                minimumKVA = demandKVA
            )
            .minByOrNull {
                it.ratedPowerKVA
            }
    }
}

/**
 * Transformer loading result.
 */
data class TransformerLoadingResult(
    val transformerId: String,
    val transformerName: String,
    val transformerRatingKVA: Double,
    val demandKVA: Double,
    val loadingPercent: Double,
    val recommendedKVA: Double,
    val overloaded: Boolean,
    val warning: Boolean
)
