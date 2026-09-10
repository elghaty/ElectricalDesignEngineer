package com.electricaldesignengineer.app

/**
 * Distribution calculation orchestration layer.
 *
 * All engineering load calculations are delegated to
 * ProfessionalEngineeringCore.
 *
 * Supported distribution frequencies:
 * - 50 Hz
 * - 60 Hz
 */
object DistributionCalculator {

    /**
     * Calculate all nodes using the frequency stored in the system.
     */
    fun calculate(
        system: DistributionSystem
    ): List<NodeCalculation> {
        return calculate(
            system = system,
            frequency = system.frequency
        )
    }

    /**
     * Calculate all nodes using an explicitly selected frequency.
     */
    fun calculate(
        system: DistributionSystem,
        frequency: DistributionFrequency
    ): List<NodeCalculation> {

        val validation = system.validate()

        if (!validation.isValid) {
            throw IllegalArgumentException(
                validation.errors.joinToString("\n")
            )
        }

        return system.nodes.map { node ->
            calculateNode(
                node = node,
                frequency = frequency
            )
        }
    }

    /**
     * Compatibility overload for callers using numeric frequency.
     */
    fun calculate(
        system: DistributionSystem,
        frequencyHz: Double
    ): List<NodeCalculation> {
        return calculate(
            system = system,
            frequency = frequencyFromHz(frequencyHz)
        )
    }

    /**
     * Calculate one distribution node.
     */
    private fun calculateNode(
        node: DistributionNode,
        frequency: DistributionFrequency
    ): NodeCalculation {

        val phaseSystem =
            when (node.phaseType) {

                PhaseType.THREE_PHASE ->
                    ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE

                PhaseType.SINGLE_PHASE_L1,
                PhaseType.SINGLE_PHASE_L2,
                PhaseType.SINGLE_PHASE_L3 ->
                    ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
            }

        /*
         * DistributionSystem does not contain a separate node-level
         * power-factor input.
         *
         * The individual load power factors are therefore passed to
         * ProfessionalEngineeringCore, which determines the effective
         * power factor from the actual loads.
         */
        val systemPowerFactor =
            node.loads
                .firstOrNull { it.powerFactor > 0.0 }
                ?.powerFactor
                ?.coerceIn(0.01, 1.0)
                ?: 1.0

        val systemInput =
            ProfessionalEngineeringCore.SystemInput(
                voltageV = node.voltage,
                frequencyHz = frequency.valueHz,
                phaseSystem = phaseSystem,
                powerFactor = systemPowerFactor
            )

        val loads =
            node.loads.map { load ->

                ProfessionalEngineeringCore.LoadInput(
                    name = load.name,
                    quantity = load.quantity.toDouble(),
                    unitPowerKW = load.unitKW,
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
     * Calculate transformer loading against a verified catalog
     * transformer at the selected frequency.
     *
     * Returns null when no suitable verified transformer exists.
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
     * Calculate transformer loading using an explicit frequency.
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
            calculations.sumOf {
                it.demandKVA
            }

        if (totalDemandKVA <= 0.0) {
            return null
        }

        val transformer =
            EngineeringCatalogRepository
                .searchTransformers(
                    minimumKVA = totalDemandKVA
                )
                .filter {
                    it.verified &&
                        approximatelyEqual(
                            it.frequencyHz,
                            frequency.valueHz
                        )
                }
                .minByOrNull {
                    it.ratedPowerKVA
                }
                ?: return null

        if (transformer.ratedPowerKVA <= 0.0) {
            return null
        }

        return (
            totalDemandKVA /
                transformer.ratedPowerKVA
            ) * 100.0
    }

    /**
     * Compatibility overload for numeric frequency.
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
     * Find the smallest verified transformer capable of supplying
     * the calculated distribution demand at the selected frequency.
     */
    fun findRecommendedTransformer(
        system: DistributionSystem
    ): EngineeringCatalogRepository.TransformerRecord? {

        val calculations =
            calculate(
                system = system,
                frequency = system.frequency
            )

        val demandKVA =
            calculations.sumOf {
                it.demandKVA
            }

        if (demandKVA <= 0.0) {
            return null
        }

        return EngineeringCatalogRepository
            .searchTransformers(
                minimumKVA = demandKVA
            )
            .filter {
                it.verified &&
                    approximatelyEqual(
                        it.frequencyHz,
                        system.frequency.valueHz
                    )
            }
            .minByOrNull {
                it.ratedPowerKVA
            }
    }

    /**
     * Convert numeric frequency to the canonical distribution
     * frequency type.
     */
    private fun frequencyFromHz(
        frequencyHz: Double
    ): DistributionFrequency {

        return when {

            approximatelyEqual(
                frequencyHz,
                50.0
            ) ->
                DistributionFrequency.HZ_50

            approximatelyEqual(
                frequencyHz,
                60.0
            ) ->
                DistributionFrequency.HZ_60

            else ->
                throw IllegalArgumentException(
                    "Unsupported distribution frequency: " +
                        "$frequencyHz Hz. " +
                        "Only 50 Hz and 60 Hz are supported."
                )
        }
    }

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
