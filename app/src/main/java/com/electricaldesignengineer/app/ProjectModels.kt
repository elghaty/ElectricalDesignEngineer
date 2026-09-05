package com.electricaldesignengineer.app

data class LoadItem(
    val name: String,
    val quantity: Double,
    val powerKW: Double,
    val demandFactor: Double,
    val powerFactor: Double
)

data class ProjectCalculation(

    // Project basic data
    val projectName: String = "",
    val clientName: String = "",
    val projectLocation: String = "",
    val engineerName: String = "",

    // Electrical system
    val voltageV: Double = 400.0,
    val frequencyHz: Double = 50.0,
    val powerFactor: Double = 0.90,
    val isThreePhase: Boolean = true,

    // Load calculation
    val connectedKW: Double = 0.0,
    val demandKW: Double = 0.0,
    val totalKVA: Double = 0.0,
    val designCurrentA: Double = 0.0,

    // Cable
    val cableSizeMm2: Double = 0.0,
    val cableAmpacityA: Double = 0.0,
    val cableLengthM: Double = 0.0,
    val voltageDropV: Double = 0.0,
    val voltageDropPercent: Double = 0.0,

    // Short circuit
    val shortCircuitKA: Double = 0.0,

    // Breaker
    val breakerRatingA: Int = 0,
    val breakerIcuKA: Double = 0.0,

    // Transformer
    val transformerKVA: Double = 0.0,

    // Generator
    val generatorKVA: Double = 0.0,

    // Power factor correction
    val capacitorKVAR: Double = 0.0,

    // Earthing
    val earthResistanceOhm: Double = 0.0,
    val earthFaultCurrentA: Double = 0.0,
    val earthPotentialRiseV: Double = 0.0,
    val maximumEarthResistanceOhm: Double = 0.0,

    // Design status
    val designStatus: String = "NOT STARTED"
)
