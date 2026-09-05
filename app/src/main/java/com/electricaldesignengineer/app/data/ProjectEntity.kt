package com.electricaldesignengineer.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val clientName: String,

    val projectLocation: String,

    val engineerName: String,

    val createdDate: String,

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
