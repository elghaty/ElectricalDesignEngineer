package com.electricaldesignengineer.app

import kotlin.math.sqrt

/**
 * DistributionCalculator
 *
 * Orchestration layer for distribution-system calculations.
 *
 * IMPORTANT:
 * - Engineering calculations remain centralized in ProfessionalEngineeringCore.
 * - Frequency is taken from DistributionSystem.frequency.
 * - Supported system frequencies are 50 Hz and 60 Hz.
 * - No independent engineering calculation engine is implemented here.
 */
object DistributionCalculator {

    /**
     * Calculates all nodes using the frequency stored in the distribution system.
     */
    fun calculate(
        system: DistributionSystem
    ): List<NodeCalculation> {
        return calculate(system, system.frequency)
    }

    /**
     * Calculates all nodes using an explicitly selected frequency.
     */
    fun calculate(
        system: DistributionSystem,
        frequency: DistributionFrequency
    ): List<NodeCalculation> {

        require(system.isValid()) {
            "Invalid distribution system data."
        }

        return system.nodes.map { node ->
            calculateNode(
                node = node,
                frequency = frequency
            )
        }
    }

    /**
     * Compatibility overload for callers that still provide frequency as Double.
     */
    fun calculate(
        system: DistributionSystem,
        frequencyHz: Double
    ): List<NodeCalculation> {

        val frequency = frequencyFromHz(frequencyHz)

        return calculate(
            system = system,
            frequency = frequency
        )
    }

    /**
     * Calculates one distribution node.
     *
     * The actual engineering load calculation is delegated to
     * ProfessionalEngineeringCore.
     */
    private fun calculateNode(
        node: DistributionNode,
        frequency: DistributionFrequency
    ): NodeCalculation {

        val phaseSystem =
            if (node.isThreePhase) {
                ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE
            } else {
                ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
            }

        val systemInput =
            ProfessionalEngineeringCore.SystemInput(
                voltageV = node.voltageV,
                frequencyHz = frequency.valueHz,
                phaseSystem = phaseSystem,
                powerFactor = node.powerFactor
            )

        val loads =
            node.loads.map { load ->
                ProfessionalEngineeringCore.LoadInput(
                    name = load.name,
                    quantity = load.quantity,
                    unitPowerKW = load.unitPowerKW,
                    demandFactor = load.demandFactor,
                    powerFactor = load.powerFactor
                )
            }

        val result =
            ProfessionalEngineeringCore.calculateLoads(
                system = systemInput,
                loads = loads
            )

        return NodeCalculation(
            nodeId = node.id,
            nodeName = node.name,
            frequencyHz = frequency.valueHz,
            connectedKW = result.connectedKW,
            demandKW = result.demandKW,
            demandKVA = result.demandKVA,
            currentA = result.currentA,
            powerFactor = result.effectivePowerFactor,
            checks = result.checks
        )
    }

    /**
     * Returns transformer loading percentage based on the selected
     * transformer catalog rating.
     *
     * This function is intentionally kept as an orchestration/helper
     * function and does not implement a separate engineering engine.
     */
    fun transformerLoading(
        system: DistributionSystem
    ): Double? {

        return transformerLoading(
            system = system,
            frequency = system.frequency
        )
    }

    /**
     * Transformer loading using an explicit frequency.
     *
     * Only verified catalog transformers matching the requested
     * voltage/frequency are considered.
     */
    fun transformerLoading(
        system: DistributionSystem,
        frequency: DistributionFrequency
    ): Double? {

        val calculations =
            calculate(
                system = system,
                frequency = frequency
            )

        val totalDemandKVA =
            calculations.sumOf { it.demandKVA }

        if (totalDemandKVA <= 0.0) {
            return null
        }

        val transformer =
            EngineeringCatalogRepository
                .searchTransformers(
                    minimumKVA = totalDemandKVA
                )
                .firstOrNull {
                    it.verified &&
                        it.frequencyHz == frequency.valueHz &&
                        approximatelyEqual(
                            it.primaryVoltageV,
                            system.nodes.firstOrNull()?.voltageV
                                ?: it.primaryVoltageV
                        )
                }
                ?: return null

        if (transformer.ratedPowerKVA <= 0.0) {
            return null
        }

        return totalDemandKVA /
            transformer.ratedPowerKVA *
            100.0
    }

    /**
     * Compatibility overload for callers using Double frequency.
     */
    fun transformerLoading(
        system: DistributionSystem,
        frequencyHz: Double
    ): Double? {

        return transformerLoading(
            system = system,
            frequency = frequencyFromHz(frequencyHz)
        )
    }

    /**
     * Finds the smallest verified transformer capable of supplying
     * the calculated distribution demand.
     */
    fun findRecommendedTransformer(
        system: DistributionSystem
    ): TransformerRecord? {

        val calculations =
            calculate(
                system = system,
                frequency = system.frequency
            )

        val demandKVA =
            calculations.sumOf { it.demandKVA }

        if (demandKVA <= 0.0) {
            return null
        }

        return EngineeringCatalogRepository
            .searchTransformers(
                minimumKVA = demandKVA
            )
            .filter {
                it.verified &&
                    it.frequencyHz == system.frequency.valueHz
            }
            .minByOrNull {
                it.ratedPowerKVA
            }
    }

    /**
     * Converts a numeric frequency to the canonical distribution
     * frequency enum.
     */
    private fun frequencyFromHz(
        frequencyHz: Double
    ): DistributionFrequency {

        return when {
            approximatelyEqual(frequencyHz, 50.0) ->
                DistributionFrequency.HZ_50

            approximatelyEqual(frequencyHz, 60.0) ->
                DistributionFrequency.HZ_60

            else ->
                throw IllegalArgumentException(
                    "Only 50 Hz and 60 Hz are supported."
                )
        }
    }

    /**
     * Small tolerance for floating-point frequency comparisons.
     */
    private fun approximatelyEqual(
        a: Double,
        b: Double,
        tolerance: Double = 0.001
    ): Boolean {
        return kotlin.math.abs(a - b) <= tolerance
    }
}

/**
 * Result of calculating one distribution node.
 */
data class NodeCalculation(
    val nodeId: String,
    val nodeName: String,
    val frequencyHz: Double,
    val connectedKW: Double,
    val demandKW: Double,
    val demandKVA: Double,
    val currentA: Double,
    val powerFactor: Double,
    val checks: List<EngineeringCheck>
)

مهم: الملف يفترض أن "DistributionModel.kt" الحالي يحتوي بالفعل على:

enum class DistributionFrequency

وأن "DistributionNode" يحتوي على:

isThreePhase
voltageV
powerFactor
loads

بعد الاستبدال اعمل Commit فقط، وسيبدأ GitHub Actions Build تلقائيًا.
