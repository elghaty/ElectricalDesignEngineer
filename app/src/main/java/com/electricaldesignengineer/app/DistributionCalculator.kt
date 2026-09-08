package com.electricaldesignengineer.app

/**
 * DistributionCalculator
 *
 * Orchestrator only.
 *
 * IMPORTANT:
 * All electrical load and current calculations are delegated
 * to ProfessionalEngineeringCore.
 *
 * This class must NOT contain duplicated electrical formulas
 * or hardcoded engineering tables.
 */
object DistributionCalculator {

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

    fun calculate(
        system: DistributionSystem
    ): List<NodeCalculation> {

        val results = mutableMapOf<String, NodeCalculation>()

        system.hierarchyOrder()
            .asReversed()
            .forEach { node ->

                results[node.id] =
                    calculateNode(
                        system = system,
                        node = node
                    )
            }

        return system.hierarchyOrder()
            .mapNotNull { node ->
                results[node.id]
            }
    }

    private fun calculateNode(
        system: DistributionSystem,
        node: DistributionNode
    ): NodeCalculation {

        /*
         * Build one engineering load list containing
         * the node's own loads and all descendant loads.
         */
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
                frequencyHz = 50.0,
                phaseSystem = phaseSystem,
                powerFactor = determineSystemPowerFactor(
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
     * Collect own loads + all descendant loads.
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
        target: MutableList<ProfessionalEngineeringCore.LoadInput>
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
     * Determine representative PF without performing
     * an electrical calculation outside the Core.
     *
     * The Core performs the actual PF calculation from
     * active/reactive power.
     */
    private fun determineSystemPowerFactor(
        loads: List<ProfessionalEngineeringCore.LoadInput>
    ): Double {

        if (loads.isEmpty()) {
            return 1.0
        }

        val weighted =
            loads.sumOf { load ->

                val quantity =
                    load.quantity.coerceAtLeast(0.0)

                val power =
                    load.unitPowerKW.coerceAtLeast(0.0)

                val demandFactor =
                    load.demandFactor.coerceIn(0.0, 1.0)

                quantity *
                        power *
                        demandFactor
            }

        if (weighted <= 0.0) {
            return 1.0
        }

        val weightedPF =
            loads.sumOf { load ->

                val quantity =
                    load.quantity.coerceAtLeast(0.0)

                val power =
                    load.unitPowerKW.coerceAtLeast(0.0)

                val demandFactor =
                    load.demandFactor.coerceIn(0.0, 1.0)

                val pf =
                    load.powerFactor.coerceIn(0.01, 1.0)

                quantity *
                        power *
                        demandFactor *
                        pf
            } / weighted

        return weightedPF.coerceIn(0.01, 1.0)
    }

    /**
     * Loading is a presentation/assessment value.
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
     * Calculate transformer loading.
     *
     * Transformer sizing itself will be moved to the
     * engineering core/catalog layer in the next stage.
     */
    fun transformerLoading(
        system: DistributionSystem
    ): TransformerLoadingResult? {

        val transformer =
            system.getTransformer()
                ?: return null

        val calculation =
            calculate(system)
                .firstOrNull {
                    it.nodeId == transformer.id
                }
                ?: return null

        val demandKVA =
            calculation.demandKVA

        val loadingPercent =
            if (transformer.ratedCapacity > 0.0) {
                demandKVA /
                        transformer.ratedCapacity *
                        100.0
            } else {
                0.0
            }

        val recommendedKVA =
            recommendTransformerSize(
                demandKVA
            )

        return TransformerLoadingResult(
            transformerId = transformer.id,
            transformerName = transformer.name,
            transformerRatingKVA =
                transformer.ratedCapacity,
            demandKVA = demandKVA,
            loadingPercent = loadingPercent,
            recommendedKVA = recommendedKVA,
            overloaded = loadingPercent > 100.0,
            warning = loadingPercent > 85.0
        )
    }

    /**
     * Temporary standard transformer selection.
     *
     * This is NOT an electrical calculation.
     * It is a catalog/standard-rating lookup.
     *
     * This list will later be moved into the
     * EngineeringCatalogRepository.
     */
    private fun recommendTransformerSize(
        demandKVA: Double
    ): Double {

        if (demandKVA <= 0.0) {
            return 0.0
        }

        val requiredKVA =
            demandKVA * 1.15

        val standardRatings =
            listOf(
                100.0,
                160.0,
                250.0,
                315.0,
                400.0,
                500.0,
                630.0,
                800.0,
                1000.0,
                1250.0,
                1600.0,
                2000.0,
                2500.0,
                3150.0,
                4000.0
            )

        return standardRatings
            .firstOrNull {
                it >= requiredKVA
            }
            ?: standardRatings.last()
    }
}

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
