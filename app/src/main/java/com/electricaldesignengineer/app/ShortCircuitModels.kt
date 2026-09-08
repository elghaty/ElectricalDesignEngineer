package com.electricaldesignengineer.app

/**
 * Short-circuit and protection calculation models.
 *
 * IMPORTANT:
 * No transformer impedance, upstream fault level or conductor
 * temperature is assumed by the software.
 */

/**
 * Fault type supported by the present calculation engine.
 *
 * More fault types can be added later when their required
 * network parameters are explicitly modeled.
 */
enum class ShortCircuitFaultType {
    THREE_PHASE
}

/**
 * Source information used by the short-circuit calculation.
 */
data class ShortCircuitSourceInput(

    /**
     * Transformer apparent power in kVA.
     *
     * Required when transformer impedance is used as the
     * source impedance.
     */
    val transformerKVA: Double? = null,

    /**
     * Voltage at the calculation bus in volts.
     */
    val voltageV: Double,

    /**
     * Transformer percentage impedance.
     *
     * Example:
     * 6.0 = 6 %
     */
    val transformerImpedancePercent: Double? = null,

    /**
     * Transformer/source resistance percentage.
     *
     * If supplied together with reactance percentage, the
     * transformer impedance is represented explicitly as R + jX.
     */
    val transformerResistancePercent: Double? = null,

    /**
     * Transformer/source reactance percentage.
     */
    val transformerReactancePercent: Double? = null,

    /**
     * Available upstream short-circuit current at the same
     * voltage level as voltageV.
     *
     * This is NOT silently converted through an unknown
     * transformer ratio.
     */
    val upstreamShortCircuitKA: Double? = null,

    /**
     * Upstream source resistance in ohms at the calculation
     * voltage level.
     *
     * Optional. When upstreamShortCircuitKA is used without
     * explicit R/X, the source impedance magnitude can be
     * calculated, but downstream complex impedance calculation
     * remains DATA_REQUIRED unless an explicit R/X split is supplied.
     */
    val upstreamResistanceOhm: Double? = null,

    /**
     * Upstream source reactance in ohms at the calculation
     * voltage level.
     */
    val upstreamReactanceOhm: Double? = null
)

/**
 * Cable impedance data used for short-circuit calculation.
 *
 * Resistance and reactance are specified per km.
 */
data class ShortCircuitCableInput(

    /**
     * Cable route length in meters.
     */
    val lengthM: Double,

    /**
     * Resistance in ohm/km.
     */
    val resistanceOhmPerKm: Double,

    /**
     * Reactance in ohm/km.
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
     * Optional feeder cable.
     *
     * Null means calculation at the source bus.
     */
    val cable: ShortCircuitCableInput? = null
)

/**
 * Equivalent impedance used by the calculation.
 */
data class ShortCircuitImpedance(

    val resistanceOhm: Double,

    val reactanceOhm: Double,

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

    val equivalentImpedance: ShortCircuitImpedance?,

    val checks: List<EngineeringCheck>,

    val trace: EngineeringTrace
)

/**
 * Protection verification input.
 *
 * Ib <= In <= Iz
 *
 * and
 *
 * Icu >= Ik
 *
 * are verified explicitly.
 */
data class ProtectionCheckInput(

    /**
     * Design current Ib.
     */
    val designCurrentA: Double,

    /**
     * Cable allowable current Iz.
     */
    val cableAmpacityA: Double,

    /**
     * Protective device rated current In.
     */
    val breakerRatedCurrentA: Double,

    /**
     * Prospective short-circuit current Ik.
     */
    val prospectiveShortCircuitKA: Double,

    /**
     * Ultimate breaking capacity Icu.
     */
    val breakerIcuKA: Double,

    /**
     * Service breaking capacity Ics.
     *
     * Optional because not every catalog entry may provide it.
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

    /**
     * Null means Ics was not provided.
     */
    val serviceBreakingCapacityPass: Boolean?,

    val checks: List<EngineeringCheck>,

    val trace: EngineeringTrace
)
