package com.electricaldesignengineer.app

/**
 * ================================================================
 * DISTRIBUTION CALCULATOR
 * ================================================================
 *
 * Orchestration layer for the electrical distribution model.
 *
 * Responsibilities:
 * - Read DistributionSystem / DistributionNode data.
 * - Pass load calculations to ProfessionalEngineeringCore.
 * - Respect the selected 50 Hz / 60 Hz system frequency.
 * - Provide distribution-level calculation results.
 *
 * Engineering formulas are NOT implemented here.
 * ProfessionalEngineeringCore remains the authoritative
 * engineering calculation engine.
 *
 * ================================================================
 */
object DistributionCalculator {

    // ================================================================
    // COMPLETE SYSTEM CALCULATION
    // ================================================================

    /**
     * Calculate the complete distribution system using the
     * frequency stored in DistributionSystem.
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
     * Calculate the complete distribution system using an
     * explicitly selected frequency.
     */
    fun calculate(
        system: DistributionSystem,
        frequency: DistributionFrequency
    ): List<NodeCalculation> {

        val validation =
            system.validate()

        if (!validation.isValid) {
            throw IllegalArgumentException(
                validation.errors.joinToString(
                    separator = "\n"
                )
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
     * Compatibility overload for existing callers that pass
     * frequency as a numeric value.
     *
     * Only 50 Hz and 60 Hz are accepted.
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

    // ================================================================
    // NODE CALCULATION
    // ================================================================

    /**
     * Calculate one distribution node.
     *
     * DistributionLoad is converted into the canonical
     * ProfessionalEngineeringCore.LoadInput model.
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
         * Node voltage is the authoritative voltage for this
         * distribution node.
         */
        val systemInput =
            ProfessionalEngineeringCore.SystemInput(

                voltageV =
                    node.voltage,

                frequencyHz =
                    frequency.valueHz,

                phaseSystem =
                    phaseSystem,

                /*
                 * The core requires a system power factor.
                 *
                 * For a node with loads, use the first valid load PF
                 * as the initial system input. The individual load
                 * PFs are still passed independently to the core.
                 *
                 * If there are no loads, use 1.0 so the core receives
                 * a valid neutral value.
                 */
                powerFactor =
                    node.loads
                        .firstOrNull {
                            it.powerFactor > 0.0
                        }
                        ?.powerFactor
                        ?.coerceIn(0.01, 1.0)
                        ?: 1.0
            )

        /*
         * Convert distribution loads to the canonical engineering
         * load model.
         */
        val loads =
            node.loads.map { load ->

                ProfessionalEngineeringCore.LoadInput(

                    name =
                        load.name,

                    quantity =
                        load.quantity.toDouble(),

                    unitPowerKW =
                        load.unitKW,

                    demandFactor =
                        load.demandFactor,

                    powerFactor =
                        load.powerFactor
                )
            }

        val result =
            ProfessionalEngineeringCore.calculateLoads(
                system = systemInput,
                loads = loads
            )

        return NodeCalculation(

            nodeId =
                node.id,

            nodeName =
                node.name,

            frequencyHz =
                frequency.valueHz,

            connectedKW =
                result.connectedKW,

            demandKW =
                result.demandKW,

            demandKVA =
                result.demandKVA,

            currentA =
                result.currentA,

            powerFactor =
                result.effectivePowerFactor,

            checks =
                result.checks
        )
    }

    // ================================================================
    // TRANSFORMER LOADING
    // ================================================================

    /**
     * Calculate transformer loading percentage using the
     * distribution system frequency.
     *
     * Returns null when:
     * - there is no demand,
     * - or no verified matching transformer exists.
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
     * Calculate transformer loading percentage for a selected
     * system frequency.
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

        /*
         * The current repository intentionally contains no verified
         * transformer records until manufacturer impedance data is
         * available.
         *
         * Therefore this may legitimately return null.
         */
        val transformer =
            EngineeringCatalogRepository
                .searchTransformers(
                    minimumKVA = totalDemandKVA
                )
                .firstOrNull {

                    it.verified &&

                    approximatelyEqual(
                        it.frequencyHz,
                        frequency.valueHz
                    )
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

    // ================================================================
    // RECOMMENDED TRANSFORMER
    // ================================================================

    /**
     * Return the smallest verified transformer capable of supplying
     * the calculated demand at the system frequency.
     *
     * No transformer is invented when verified catalog data is absent.
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

    // ================================================================
    // FREQUENCY
    // ================================================================

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

    /**
     * Floating-point comparison helper.
     */
    private fun approximatelyEqual(
        a: Double,
        b: Double,
        tolerance: Double = 0.001
    ): Boolean {

        return kotlin.math.abs(a - b) <= tolerance
    }
}

// ================================================================
// NODE CALCULATION RESULT
// ================================================================

/**
 * Engineering result for one distribution node.
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

هذه المرة الملف متوافق مع الـ "EngineeringCatalogRepository" الذي أرسلته: لا يوجد "TransformerRecord" مستقل، ولا "isThreePhase"، ولا "voltageV"، ولا "unitPowerKW"، ولا "isValid()". كما أن اختيار 50/60 Hz يعتمد على "DistributionFrequency" الموجودة فعليًا في "DistributionModel.kt".

استبدله → Commit → Build.
