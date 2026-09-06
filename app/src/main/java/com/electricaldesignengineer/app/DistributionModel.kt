package com.electricaldesignengineer.app

import java.util.UUID

/**
 * DistributionModel
 *
 * Electrical distribution hierarchy:
 *
 * Transformer
 *     ↓
 * MDB
 *     ↓
 * SMDB
 *     ↓
 * DB
 *     ↓
 * SUB_DB
 *     ↓
 * FINAL_CIRCUIT
 *     ↓
 * LOAD
 *
 * This model is used by:
 * - Design Engine
 * - SLD Generator
 * - Load Schedule
 * - Cable Sizing
 * - Breaker Selection
 * - Short Circuit
 * - Reports
 */

enum class DistributionNodeType {
    TRANSFORMER,
    MDB,
    SMDB,
    DB,
    SUB_DB,
    FINAL_CIRCUIT,
    LOAD
}

enum class ElectricalSystem {
    THREE_PHASE_400V,
    THREE_PHASE_415V,
    SINGLE_PHASE_230V,
    SINGLE_PHASE_240V
}

enum class PhaseType {
    THREE_PHASE,
    SINGLE_PHASE_L1,
    SINGLE_PHASE_L2,
    SINGLE_PHASE_L3
}

enum class NodeStatus {
    NOT_CALCULATED,
    CALCULATED,
    WARNING,
    ERROR
}

/**
 * Basic electrical load attached to a distribution node.
 */
data class DistributionLoad(

    val id: String = UUID.randomUUID().toString(),

    var name: String,

    var description: String = "",

    var quantity: Int = 1,

    /**
     * Unit load in kW
     */
    var unitKW: Double = 0.0,

    /**
     * Power factor
     */
    var powerFactor: Double = 0.9,

    /**
     * Demand factor
     * Typical range 0.0 - 1.0
     */
    var demandFactor: Double = 1.0,

    /**
     * Phase assignment
     */
    var phase: PhaseType = PhaseType.THREE_PHASE,

    /**
     * Optional circuit number
     */
    var circuitNumber: String = "",

    /**
     * Optional panel name
     */
    var panelName: String = "",

    /**
     * Load voltage
     */
    var voltage: Double = 400.0,

    /**
     * Optional motor information
     */
    var isMotor: Boolean = false,

    var motorEfficiency: Double = 0.9,

    /**
     * Starting current multiplier
     */
    var startingCurrentMultiplier: Double = 6.0
) {

    fun connectedKW(): Double {
        return quantity.coerceAtLeast(0) * unitKW.coerceAtLeast(0.0)
    }

    fun demandKW(): Double {
        return connectedKW() *
                demandFactor.coerceIn(0.0, 1.0)
    }

    fun demandKVA(): Double {
        val pf = powerFactor.coerceIn(0.1, 1.0)
        return demandKW() / pf
    }
}


/**
 * Feeder design information.
 *
 * This information is filled automatically by the engineering
 * calculation engine.
 */
data class FeederDesign(

    var lengthMeters: Double = 0.0,

    var installationMethod: InstallationMethod =
        InstallationMethod.CABLE_TRAY,

    var cableMaterial: CableMaterial =
        CableMaterial.COPPER,

    var insulation: InsulationType =
        InsulationType.XLPE,

    var numberOfCores: Int = 4,

    var parallelRuns: Int = 1,

    var conductorSizeMm2: Double = 0.0,

    var ampacityIz: Double = 0.0,

    var designCurrentIb: Double = 0.0,

    var breakerRatingIn: Double = 0.0,

    var breakerIcuKA: Double = 0.0,

    var breakerIcsKA: Double = 0.0,

    var voltageDropPercent: Double = 0.0,

    var maximumVoltageDropPercent: Double = 5.0,

    var shortCircuitCurrentKA: Double = 0.0,

    var correctionFactorTotal: Double = 1.0,

    var cableSelectedAutomatically: Boolean = true,

    var breakerSelectedAutomatically: Boolean = true
)


/**
 * Distribution node.
 *
 * Each node can have:
 * - parent
 * - children
 * - loads
 * - feeder
 */
data class DistributionNode(

    val id: String = UUID.randomUUID().toString(),

    var name: String,

    var type: DistributionNodeType,

    /**
     * Parent node ID.
     *
     * Transformer root has null parent.
     */
    var parentId: String? = null,

    var description: String = "",

    var electricalSystem: ElectricalSystem =
        ElectricalSystem.THREE_PHASE_400V,

    var phaseType: PhaseType =
        PhaseType.THREE_PHASE,

    var voltage: Double = 400.0,

    /**
     * Rated capacity.
     *
     * Transformer:
     * kVA
     *
     * Panel:
     * A
     */
    var ratedCapacity: Double = 0.0,

    /**
     * Connected load directly assigned to this node.
     */
    var loads: MutableList<DistributionLoad> = mutableListOf(),

    /**
     * Feeder connecting this node to its parent.
     */
    var feeder: FeederDesign? = null,

    var status: NodeStatus = NodeStatus.NOT_CALCULATED,

    var notes: String = ""
) {

    fun isRoot(): Boolean {
        return parentId == null
    }

    fun connectedLoadKW(): Double {
        return loads.sumOf {
            it.connectedKW()
        }
    }

    fun demandLoadKW(): Double {
        return loads.sumOf {
            it.demandKW()
        }
    }

    fun demandLoadKVA(): Double {
        return loads.sumOf {
            it.demandKVA()
        }
    }
}


/**
 * Complete distribution system.
 */
data class DistributionSystem(

    val id: String = UUID.randomUUID().toString(),

    var name: String = "Electrical Distribution System",

    var projectName: String = "",

    var nodes: MutableList<DistributionNode> = mutableListOf()
) {

    /**
     * Add node to system.
     */
    fun addNode(node: DistributionNode): Boolean {

        if (nodes.any { it.id == node.id }) {
            return false
        }

        if (node.parentId != null &&
            nodes.none { it.id == node.parentId }
        ) {
            return false
        }

        nodes.add(node)

        return true
    }


    /**
     * Remove node and all descendants.
     */
    fun removeNode(nodeId: String): Boolean {

        if (nodes.none { it.id == nodeId }) {
            return false
        }

        val idsToRemove = mutableSetOf<String>()

        fun collect(id: String) {

            idsToRemove.add(id)

            nodes
                .filter { it.parentId == id }
                .forEach {
                    collect(it.id)
                }
        }

        collect(nodeId)

        nodes.removeAll {
            idsToRemove.contains(it.id)
        }

        return true
    }


    /**
     * Get node by ID.
     */
    fun getNode(nodeId: String): DistributionNode? {
        return nodes.firstOrNull {
            it.id == nodeId
        }
    }


    /**
     * Get direct children.
     */
    fun getChildren(nodeId: String): List<DistributionNode> {

        return nodes.filter {
            it.parentId == nodeId
        }
    }


    /**
     * Get parent.
     */
    fun getParent(nodeId: String): DistributionNode? {

        val node = getNode(nodeId) ?: return null

        return node.parentId?.let {
            getNode(it)
        }
    }


    /**
     * Get root transformer.
     */
    fun getTransformer(): DistributionNode? {

        return nodes.firstOrNull {
            it.type == DistributionNodeType.TRANSFORMER &&
                    it.parentId == null
        }
    }


    /**
     * Get complete descendants.
     */
    fun getDescendants(nodeId: String): List<DistributionNode> {

        val result = mutableListOf<DistributionNode>()

        fun collect(id: String) {

            val children = getChildren(id)

            children.forEach { child ->

                result.add(child)

                collect(child.id)
            }
        }

        collect(nodeId)

        return result
    }


    /**
     * Calculate total connected load of entire system.
     */
    fun totalConnectedLoadKW(): Double {

        return nodes
            .filter {
                it.type == DistributionNodeType.LOAD ||
                        it.loads.isNotEmpty()
            }
            .sumOf {
                it.connectedLoadKW()
            }
    }


    /**
     * Calculate total demand load.
     */
    fun totalDemandLoadKW(): Double {

        return nodes
            .filter {
                it.loads.isNotEmpty()
            }
            .sumOf {
                it.demandLoadKW()
            }
    }


    /**
     * Calculate total demand kVA.
     */
    fun totalDemandLoadKVA(): Double {

        return nodes
            .filter {
                it.loads.isNotEmpty()
            }
            .sumOf {
                it.demandLoadKVA()
            }
    }


    /**
     * Validate hierarchy.
     */
    fun validate(): DistributionValidationResult {

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        /*
         * Transformer check
         */
        val transformers = nodes.filter {
            it.type == DistributionNodeType.TRANSFORMER
        }

        if (transformers.isEmpty()) {

            errors.add(
                "No transformer found in distribution system."
            )

        } else if (transformers.size > 1) {

            warnings.add(
                "More than one transformer exists."
            )
        }


        /*
         * Root check
         */
        val roots = nodes.filter {
            it.parentId == null
        }

        if (roots.size > 1) {

            warnings.add(
                "More than one root node exists."
            )
        }


        /*
         * Orphan nodes
         */
        nodes.forEach { node ->

            if (node.parentId != null &&
                nodes.none { it.id == node.parentId }
            ) {

                errors.add(
                    "Node '${node.name}' has an invalid parent."
                )
            }
        }


        /*
         * Cycle detection
         */
        nodes.forEach { node ->

            val visited = mutableSetOf<String>()

            var current: DistributionNode? = node

            while (current != null) {

                if (!visited.add(current.id)) {

                    errors.add(
                        "Circular hierarchy detected at '${node.name}'."
                    )

                    break
                }

                current = current.parentId?.let {
                    getNode(it)
                }
            }
        }


        /*
         * Hierarchy validation
         */
        nodes.forEach { node ->

            val parent = getParent(node.id)

            if (parent != null) {

                if (!isValidParentChild(
                        parent.type,
                        node.type
                    )
                ) {

                    errors.add(
                        "Invalid hierarchy: " +
                                "${parent.type} → ${node.type}"
                    )
                }
            }
        }


        return DistributionValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }


    /**
     * Validate parent-child relationship.
     */
    private fun isValidParentChild(
        parent: DistributionNodeType,
        child: DistributionNodeType
    ): Boolean {

        return when (parent) {

            DistributionNodeType.TRANSFORMER -> {
                child == DistributionNodeType.MDB
            }

            DistributionNodeType.MDB -> {
                child == DistributionNodeType.SMDB ||
                        child == DistributionNodeType.DB ||
                        child == DistributionNodeType.FINAL_CIRCUIT
            }

            DistributionNodeType.SMDB -> {
                child == DistributionNodeType.DB ||
                        child == DistributionNodeType.SUB_DB ||
                        child == DistributionNodeType.FINAL_CIRCUIT
            }

            DistributionNodeType.DB -> {
                child == DistributionNodeType.SUB_DB ||
                        child == DistributionNodeType.FINAL_CIRCUIT ||
                        child == DistributionNodeType.LOAD
            }

            DistributionNodeType.SUB_DB -> {
                child == DistributionNodeType.FINAL_CIRCUIT ||
                        child == DistributionNodeType.LOAD
            }

            DistributionNodeType.FINAL_CIRCUIT -> {
                child == DistributionNodeType.LOAD
            }

            DistributionNodeType.LOAD -> {
                false
            }
        }
    }


    /**
     * Return nodes in hierarchy order.
     *
     * Useful for SLD generation.
     */
    fun hierarchyOrder(): List<DistributionNode> {

        val result = mutableListOf<DistributionNode>()

        val root = getTransformer()

        if (root == null) {
            return emptyList()
        }

        fun visit(node: DistributionNode) {

            result.add(node)

            getChildren(node.id)
                .forEach { child ->
                    visit(child)
                }
        }

        visit(root)

        return result
    }
}


/**
 * Validation result.
 */
data class DistributionValidationResult(

    val isValid: Boolean,

    val errors: List<String> = emptyList(),

    val warnings: List<String> = emptyList()
)


/**
 * SLD connection.
 */
data class SldConnection(

    val fromNodeId: String,

    val toNodeId: String
)


/**
 * SLD model.
 *
 * The UI can later render this automatically.
 */
data class SldModel(

    val nodes: List<DistributionNode>,

    val connections: List<SldConnection>
)


/**
 * Generate SLD model automatically from distribution hierarchy.
 */
object SldGenerator {

    fun generate(
        system: DistributionSystem
    ): SldModel {

        val orderedNodes =
            system.hierarchyOrder()

        val connections =
            orderedNodes
                .mapNotNull { node ->

                    node.parentId?.let { parentId ->

                        SldConnection(
                            fromNodeId = parentId,
                            toNodeId = node.id
                        )
                    }
                }

        return SldModel(
            nodes = orderedNodes,
            connections = connections
        )
    }
}


/**
 * Factory for common distribution nodes.
 */
object DistributionNodeFactory {

    fun transformer(
        name: String,
        kva: Double,
        voltage: Double = 400.0
    ): DistributionNode {

        return DistributionNode(

            name = name,

            type =
                DistributionNodeType.TRANSFORMER,

            parentId = null,

            voltage = voltage,

            ratedCapacity = kva
        )
    }


    fun mdb(
        name: String,
        parentId: String,
        voltage: Double = 400.0
    ): DistributionNode {

        return DistributionNode(

            name = name,

            type =
                DistributionNodeType.MDB,

            parentId = parentId,

            voltage = voltage
        )
    }


    fun smdb(
        name: String,
        parentId: String
    ): DistributionNode {

        return DistributionNode(

            name = name,

            type =
                DistributionNodeType.SMDB,

            parentId = parentId,

            voltage = 400.0
        )
    }


    fun db(
        name: String,
        parentId: String
    ): DistributionNode {

        return DistributionNode(

            name = name,

            type =
                DistributionNodeType.DB,

            parentId = parentId,

            voltage = 400.0
        )
    }


    fun subDb(
        name: String,
        parentId: String
    ): DistributionNode {

        return DistributionNode(

            name = name,

            type =
                DistributionNodeType.SUB_DB,

            parentId = parentId,

            voltage = 400.0
        )
    }


    fun finalCircuit(
        name: String,
        parentId: String,
        phase: PhaseType = PhaseType.THREE_PHASE
    ): DistributionNode {

        return DistributionNode(

            name = name,

            type =
                DistributionNodeType.FINAL_CIRCUIT,

            parentId = parentId,

            phaseType = phase,

            voltage =
                if (phase == PhaseType.THREE_PHASE)
                    400.0
                else
                    230.0
        )
    }


    fun load(
        name: String,
        parentId: String,
        kw: Double,
        quantity: Int = 1,
        pf: Double = 0.9,
        df: Double = 1.0,
        phase: PhaseType = PhaseType.THREE_PHASE
    ): DistributionNode {

        val node = DistributionNode(

            name = name,

            type =
                DistributionNodeType.LOAD,

            parentId = parentId,

            phaseType = phase,

            voltage =
                if (phase == PhaseType.THREE_PHASE)
                    400.0
                else
                    230.0
        )

        node.loads.add(

            DistributionLoad(

                name = name,

                quantity = quantity,

                unitKW = kw,

                powerFactor = pf,

                demandFactor = df,

                phase = phase,

                voltage = node.voltage
            )
        )

        return node
    }
}


/**
 * Example builder.
 *
 * Creates:
 *
 * Transformer
 *      ↓
 * MDB
 *      ↓
 * SMDB
 *      ↓
 * DB
 *      ↓
 * Final Circuit
 *      ↓
 * Load
 */
object DistributionExample {

    fun create(): DistributionSystem {

        val system =
            DistributionSystem(
                name = "Main Electrical Distribution"
            )


        val transformer =
            DistributionNodeFactory.transformer(
                name = "TR-01",
                kva = 1000.0
            )

        system.addNode(transformer)


        val mdb =
            DistributionNodeFactory.mdb(
                name = "MDB-01",
                parentId = transformer.id
            )

        system.addNode(mdb)


        val smdb =
            DistributionNodeFactory.smdb(
                name = "SMDB-01",
                parentId = mdb.id
            )

        system.addNode(smdb)


        val db =
            DistributionNodeFactory.db(
                name = "DB-01",
                parentId = smdb.id
            )

        system.addNode(db)


        val finalCircuit =
            DistributionNodeFactory.finalCircuit(
                name = "FC-01",
                parentId = db.id,
                phase = PhaseType.THREE_PHASE
            )

        system.addNode(finalCircuit)


        val load =
            DistributionNodeFactory.load(
                name = "AHU-01",
                parentId = finalCircuit.id,
                kw = 22.0,
                quantity = 1,
                pf = 0.9,
                df = 1.0,
                phase = PhaseType.THREE_PHASE
            )

        system.addNode(load)


        return system
    }
}
