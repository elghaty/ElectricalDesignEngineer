package com.electricaldesignengineer.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistent engineering circuit-breaker catalogue record.
 *
 * This table stores source-backed manufacturer data.
 * It does not contain calculated protection settings.
 */
@Entity(
    tableName = "breaker_catalog",
    indices = [
        Index(value = ["manufacturerId"]),
        Index(value = ["catalogId"]),
        Index(value = ["productFamily"]),
        Index(value = ["ratedCurrentA"]),
        Index(value = ["poles"]),
        Index(value = ["ratedVoltageV"])
    ]
)
data class BreakerCatalogEntity(

    @PrimaryKey
    val id: String,

    val manufacturerId: String,

    val manufacturerName: String,

    val catalogId: String,

    val productFamily: String,

    val partNumber: String?,

    /**
     * Rated uninterrupted current In.
     */
    val ratedCurrentA: Double,

    /**
     * Rated operational voltage.
     */
    val ratedVoltageV: Double,

    /**
     * Number of poles.
     */
    val poles: Int,

    /**
     * Rated ultimate short-circuit breaking capacity Icu.
     */
    val icuKA: Double,

    /**
     * Rated service short-circuit breaking capacity Ics.
     */
    val icsKA: Double?,

    /**
     * Rated frequency.
     */
    val frequencyHz: Double?,

    /**
     * Trip-unit family/type.
     */
    val tripUnit: String?,

    /**
     * Adjustable long-time protection setting Ir,
     * when applicable to the product family.
     */
    val adjustableIr: Boolean,

    /**
     * Adjustable instantaneous protection setting Im,
     * when applicable.
     */
    val adjustableIm: Boolean,

    /**
     * Number of auxiliary/accessory positions is not
     * assumed here; accessory data will be modeled separately.
     */

    /**
     * Applicable engineering standard.
     */
    val standardCode: String?,

    /**
     * Source document or official manufacturer URL.
     */
    val sourceUrl: String?,

    /**
     * Catalogue/document revision.
     */
    val revision: String,

    /**
     * Only verified records can be selected automatically
     * by the engineering calculation engine.
     */
    val verified: Boolean
)
