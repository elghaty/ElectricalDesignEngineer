package com.electricaldesignengineer.app

data class LoadItem(
    val name: String,
    val quantity: Double,
    val powerKW: Double,
    val demandFactor: Double,
    val powerFactor: Double
)

data class ProjectCalculation(
    val connectedKW: Double = 0.0,
    val demandKW: Double = 0.0,
    val totalKVA: Double = 0.0,
    val designCurrentA: Double = 0.0,
    val voltageV: Double = 400.0,
    val powerFactor: Double = 0.90,
    val cableSizeMm2: Double = 0.0,
    val voltageDropPercent: Double = 0.0,
    val shortCircuitKA: Double = 0.0,
    val breakerRatingA: Int = 0,
    val breakerIcuKA: Double = 0.0,
    val transformerKVA: Double = 0.0,
    val generatorKVA: Double = 0.0,
    val capacitorKVAR: Double = 0.0
)
