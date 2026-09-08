package com.electricaldesignengineer.app

/**
 * Short-circuit and protection calculation models.
 *
 * IMPORTANT:
 * - No transformer impedance is assumed.
 * - No upstream fault level is assumed.
 * - All source and equipment data must be supplied explicitly.
 *
 * The present model is intended as the data contract for the
 * ProfessionalEngineeringCore. The actual engineering calculation
 * will be performed by the core calculation engine.
 */

/**
 * Fault type currently supported by the calculation contract.
 *
 * Additional fault types will be added after their required
 * network parameters are explicitly modeled.
 */
enum class ShortCircuitFaultType {
    THREE_PHASE
}

/**
 * Source information.
 *
 * Either transformer data or an upstream short-circuit level
 * must be supplied.
 */
data class ShortCircuitSourceInput(

    /**
     * Transformer apparent power.
     *
     * Required when transformer impedance is used.
     */
    val transformerKVA: Double? = null,

    /**
     * Transformer secondary voltage.
     */
    val voltageV: Double,

    /**
     * Transformer percentage impedance.
     *
     * Example:
     * 6.0 means 6 %.
     *
     * This value is intentionally nullable.
     * The software must not assume a transformer %Z.
     */
    val transformerImpedancePercent: Double? = null,

    /**
     * Upstream prospective short-circuit current in kA.
     *
     * If supplied, it represents the available upstream fault level
     * at the source bus and can be converted to an equivalent
     * upstream impedance.
     */
    val upstreamShortCircuitKA: Double? = null
)

/**
 * Cable impedance data used for short-circuit calculation.
 *
 * Resistance and reactance are specified per km.
 */
data class ShortCircuitCableInput(

    /**
     * Cable route length.
     */
    val lengthM: Double,

    /**
     * Resistance at the engineering calculation reference
     * condition.
     */
    val resistanceOhmPerKm: Double,

    /**
     * Reactance at the engineering calculation reference
     * condition.
     */
    val reactanceOhmPerKm: Double,

    /**
     * Number of identical parallel cable runs.
     */
    val parallelRuns: Int = 1
)

/**
 * Complete short-circuit calculation input.
 */
data class ShortCircuitInput(

    val faultType: ShortCircuitFaultType =
        ShortCircuitFaultType.THREE_PHASE,

    val source: ShortCircuitSourceInput,

    /**
     * Optional feeder impedance.
     *
     * Null means the calculation is being performed at the
     * source bus rather than downstream of a feeder.
     */
    val cable: ShortCircuitCableInput? = null
)

/**
 * Impedance result used for traceability.
 */
data class ShortCircuitImpedance(

    /**
     * Total equivalent resistance in ohms.
     */
    val resistanceOhm: Double,

    /**
     * Total equivalent reactance in ohms.
     */
    val reactanceOhm: Double,

    /**
     * Magnitude of total impedance in ohms.
     */
    val magnitudeOhm: Double
)

/**
 * Short-circuit calculation result.
 */
data class ShortCircuitResult(

    val status: EngineeringStatus,

    /**
     * Prospective fault current in amperes.
     */
    val faultCurrentA: Double,

    /**
     * Prospective fault current in kA.
     */
    val faultCurrentKA: Double,

    /**
     * Equivalent impedance used by the calculation.
     */
    val equivalentImpedance: ShortCircuitImpedance?,

    val checks: List<EngineeringCheck>,

    val trace: EngineeringTrace
)

/**
 * Input for a protection-device verification.
 *
 * This model intentionally remains independent from a particular
 * manufacturer.
 */
data class ProtectionCheckInput(

    /**
     * Design/load current Ib.
     */
    val designCurrentA: Double,

    /**
     * Cable allowable current Iz.
     */
    val cableAmpacityA: Double,

    /**
     * Selected protective-device rated current In.
     */
    val breakerRatedCurrentA: Double,

    /**
     * Prospective short-circuit current at the installation point.
     */
    val prospectiveShortCircuitKA: Double,

    /**
     * Protective device ultimate breaking capacity Icu.
     */
    val breakerIcuKA: Double,

    /**
     * Optional service breaking capacity Ics.
     */
    val breakerIcsKA: Double? = null
)

/**
 * Result of protection verification.
 */
data class ProtectionCheckResult(

    val status: EngineeringStatus,

    val overloadProtectionPass: Boolean,

    val breakingCapacityPass: Boolean,

    val serviceBreakingCapacityPass: Boolean?,

    val checks: List<EngineeringCheck>,

    val trace: EngineeringTrace
)
