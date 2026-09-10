package com.electricaldesignengineer.app

data class LoadItem(
val name: String,
val quantity: Double,
val powerKW: Double,
val demandFactor: Double,
val powerFactor: Double
)

data class ProjectCalculation(
val projectName: String = "",
val clientName: String = "",
val projectLocation: String = "",
val engineerName: String = "",

// =========================
// SYSTEM DATA
// =========================

val voltageV: Double = 400.0,
val frequencyHz: Double = 50.0,
val powerFactor: Double = 0.90,
val isThreePhase: Boolean = true,

// =========================
// LOAD CALCULATION
// =========================

val connectedKW: Double = 0.0,
val demandKW: Double = 0.0,
val totalKVA: Double = 0.0,
val designCurrentA: Double = 0.0,

// =========================
// CABLE
// =========================

val cableSizeMm2: Double = 0.0,
val cableAmpacityA: Double = 0.0,
val cableLengthM: Double = 0.0,
val voltageDropV: Double = 0.0,
val voltageDropPercent: Double = 0.0,

// =========================
// SHORT CIRCUIT
// =========================

val shortCircuitKA: Double = 0.0,

// =========================
// BREAKER
// =========================

val breakerRatingA: Int = 0,
val breakerIcuKA: Double = 0.0,

// =========================
// TRANSFORMER
// =========================

val transformerKVA: Double = 0.0,

/**
 * Transformer percentage impedance.
 *
 * The value must come from the transformer
 * manufacturer's nameplate or verified
 * technical/catalog data.
 *
 * 0.0 means that the impedance value has
 * not yet been supplied or verified.
 *
 * No assumed value is used.
 */
val transformerImpedancePercent: Double = 0.0,

// =========================
// GENERATOR
// =========================

val generatorKVA: Double = 0.0,

// =========================
// POWER FACTOR CORRECTION
// =========================

val capacitorKVAR: Double = 0.0,

// =========================
// EARTHING
// =========================

val earthResistanceOhm: Double = 0.0,
val earthFaultCurrentA: Double = 0.0,
val earthPotentialRiseV: Double = 0.0,
val maximumEarthResistanceOhm: Double = 0.0,

// =========================
// DESIGN STATUS
// =========================

val designStatus: String = "NOT STARTED"

)
