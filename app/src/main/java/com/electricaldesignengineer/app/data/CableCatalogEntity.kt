package com.electricaldesignengineer.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent engineering cable catalogue record.
 *
 * This table stores source-backed technical data.
 * It does not contain calculated design results.
 */
@Entity(
    tableName = "cable_catalog",
    indices = [
        Index(value = ["manufacturerId"]),
        Index(value = ["catalogId"]),
        Index(value = ["sizeMm2"]),
        Index(
            value = [
                "material",
                "insulation",
                "installationMethod"
            ]
        )
    ]
)
data class CableCatalogEntity(

    @PrimaryKey
    val id: String,

    val manufacturerId: String,

    val manufacturerName: String,

    val catalogId: String,

    val productFamily: String,

    val partNumber: String?,

    /**
     * Stored as String to keep Room independent from
     * application enum implementations.
     */
    val material: String,

    val insulation: String,

    val installationMethod: String,

    val cores: Int,

    val sizeMm2: Double,

    val voltageRatingV: Int,

    /**
     * Source-backed ampacity.
     */
    val ampacityA: Double,

    /**
     * AC resistance in ohm/km.
     */
    val resistanceOhmPerKm: Double,

    /**
     * Reactance in ohm/km.
     */
    val reactanceOhmPerKm: Double,

    /**
     * Reference temperature used by the source data.
     */
    val referenceTemperatureC: Double?,

    /**
     * Short-circuit withstand current in kA.
     */
    val shortCircuitKA: Double?,

    /**
     * Short-circuit withstand duration in seconds.
     */
    val shortCircuitDurationS: Double?,

    /**
     * Applicable engineering standard.
     */
    val standardCode: String?,

    /**
     * Source document or official catalogue URL.
     */
    val sourceUrl: String?,

    /**
     * Catalogue/document revision.
     */
    val revision: String,

    /**
     * Only verified records should be used
     * automatically by the calculation engine.
     */
    val verified: Boolean
)
