package com.electricaldesignengineer.app

import kotlin.math.sqrt

/**
 * DistributionCalculator
 *
 * Calculates accumulated loads through:
 *
 * LOAD
 *  ↓
 * FINAL CIRCUIT
 *  ↓
 * SUB-DB
 *  ↓
 * DB
 *  ↓
 * SMDB
 *  ↓
 * MDB
 *  ↓
 * TRANSFORMER
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


    /**
     * Calculate complete distribution system.
     */
    fun calculate(
        system: DistributionSystem
    ): List<NodeCalculation> {

        val results = mutableListOf<NodeCalculation>()

        /*
         * Calculate from bottom to top.
         */
        system.hierarchyOrder()
            .asReversed()
            .forEach { node ->

                val result =
                    calculateNode(
                        system = system,
                        node = node
                    )

                results.add(result)
            }

        /*
         * Return in normal hierarchy order.
         */
        return system.hierarchyOrder()
            .mapNotNull { node ->

                results.firstOrNull {
                    it.nodeId == node.id
                }
            }
    }


    /**
     * Calculate one node.
     */
    private fun calculateNode(
        system: DistributionSystem,
        node: DistributionNode
    ): NodeCalculation {

        /*
         * Direct loads.
         */
        var connectedKW =
            node.connectedLoadKW()

        var demandKW =
            node.demandLoadKW()

        var demandKVA =
            node.demandLoadKVA()


        /*
         * Add all child loads.
         *
         * This means:
         *
         * DB = own loads + child loads
         *
         * SMDB = own loads + all DB loads
         *
         * MDB = own loads + all SMDB/DB loads
         */
        val children =
            system.getChildren(node.id)

        children.forEach { child ->

            connectedKW +=
                childAccumulatedConnectedKW(
                    system,
                    child
                )

            demandKW +=
                childAccumulatedDemandKW(
                    system,
                    child
                )

            demandKVA +=
                childAccumulatedDemandKVA(
                    system,
                    child
                )
        }


        /*
         * Calculate current.
         */
        val currentA =
            calculateCurrent(
                node = node,
                demandKVA = demandKVA
            )


        /*
         * Calculate loading.
         */
        val loadingPercent =
            if (node.ratedCapacity > 0.0) {

                when (node.type) {

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

            } else {
                0.0
            }


        /*
         * Determine status.
         */
        val status =

            when {

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
     * Connected load including all descendants.
     */
    private fun childAccumulatedConnectedKW(
        system: DistributionSystem,
        node: DistributionNode
    ): Double {

        var total =
            node.connectedLoadKW()

        system.getChildren(node.id)
            .forEach { child ->

                total +=
                    childAccumulatedConnectedKW(
                        system,
                        child
                    )
            }

        return total
    }


    /**
     * Demand load including descendants.
     */
    private fun childAccumulatedDemandKW(
        system: DistributionSystem,
        node: DistributionNode
    ): Double {

        var total =
            node.demandLoadKW()

        system.getChildren(node.id)
            .forEach { child ->

                total +=
                    childAccumulatedDemandKW(
                        system,
                        child
                    )
            }

        return total
    }


    /**
     * Demand kVA including descendants.
     */
    private fun childAccumulatedDemandKVA(
        system: DistributionSystem,
        node: DistributionNode
    ): Double {

        var total =
            node.demandLoadKVA()

        system.getChildren(node.id)
            .forEach { child ->

                total +=
                    childAccumulatedDemandKVA(
                        system,
                        child
                    )
            }

        return total
    }


    /**
     * Current calculation.
     *
     * Three phase:
     *
     * I = S / (√3 × V)
     *
     * Single phase:
     *
     * I = S / V
     */
    private fun calculateCurrent(
        node: DistributionNode,
        demandKVA: Double
    ): Double {

        val kvaVA =
            demandKVA * 1000.0

        return when (node.phaseType) {

            PhaseType.THREE_PHASE -> {

                if (node.voltage <= 0.0) {
                    0.0
                } else {

                    kvaVA /
                            (
                                sqrt(3.0) *
                                        node.voltage
                                )
                }
            }

            PhaseType.SINGLE_PHASE_L1,
            PhaseType.SINGLE_PHASE_L2,
            PhaseType.SINGLE_PHASE_L3 -> {

                if (node.voltage <= 0.0) {
                    0.0
                } else {

                    kvaVA /
                            node.voltage
                }
            }
        }
    }


    /**
     * Calculate transformer loading.
     */
    fun transformerLoading(
        system: DistributionSystem
    ): TransformerLoadingResult? {

        val transformer =
            system.getTransformer()
                ?: return null

        val demandKVA =
            calculate(system)
                .firstOrNull {
                    it.nodeId == transformer.id
                }
                ?.demandKVA
                ?: 0.0


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

            transformerId =
                transformer.id,

            transformerName =
                transformer.name,

            transformerRatingKVA =
                transformer.ratedCapacity,

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
     * Transformer recommendation.
     *
     * We keep a standard rating sequence.
     */
    private fun recommendTransformerSize(
        demandKVA: Double
    ): Double {

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

        /*
         * Design margin.
         */
        val required =
            demandKVA * 1.15

        return standardRatings
            .firstOrNull {
                it >= required
            }
            ?: standardRatings.last()
    }
}


/**
 * Transformer result.
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
