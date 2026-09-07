package com.electricaldesignengineer.app

enum class ManufacturerOrigin {
    EGYPTIAN,
    INTERNATIONAL
}

enum class ProductCategory {
    LV_CABLE,
    MV_CABLE,
    CONTROL_CABLE,
    FIRE_RESISTANT_CABLE,
    MCB,
    MCCB,
    ACB,
    RCD,
    FUSE,
    CONTACTOR,
    MOTOR_STARTER,
    TRANSFORMER,
    GENERATOR,
    LV_PANEL,
    MV_PANEL,
    RMU,
    BUSBAR,
    CABLE_ACCESSORY,
    EARTHING,
    LIGHTING,
    CAPACITOR,
    SURGE_PROTECTION,
    OTHER
}

data class Manufacturer(
    val id: String,
    val name: String,
    val origin: ManufacturerOrigin,
    val country: String,
    val officialWebsite: String,
    val catalogWebsite: String? = null,
    val active: Boolean = true
)

data class CatalogDocument(
    val id: String,
    val manufacturerId: String,
    val title: String,
    val revision: String,
    val language: String,
    val documentType: String,
    val officialUrl: String,
    val verified: Boolean = false
)

data class CableProduct(
    val manufacturerId: String,
    val catalogId: String,
    val productFamily: String,
    val partNumber: String,
    val conductorMaterial: String,
    val conductorClass: String?,
    val insulation: String,
    val sheath: String?,
    val voltageRatingV: Int,
    val cores: Int,
    val crossSectionMm2: Double,
    val standard: String,
    val conductorTemperatureC: Double?,
    val resistanceOhmPerKm: Double?,
    val reactanceOhmPerKm: Double?,
    val shortCircuitCurrentKA: Double?,
    val shortCircuitTimeSeconds: Double?,
    val sourceRevision: String
)

data class BreakerProduct(
    val manufacturerId: String,
    val catalogId: String,
    val productFamily: String,
    val partNumber: String,
    val breakerType: ProductCategory,
    val poles: Int,
    val ratedCurrentA: Double,
    val ratedVoltageV: Double,
    val frequencyHz: Double,
    val icuKA: Double?,
    val icsKA: Double?,
    val ratedShortTimeWithstandKA: Double?,
    val tripUnit: String?,
    val frameSizeA: Double?,
    val standard: String,
    val sourceRevision: String
)

object ManufacturerCatalog {

    val manufacturers: List<Manufacturer> = listOf(

        Manufacturer(
            id = "ELSEWEDY_SEI",
            name = "ElSewedy Engineering Industries",
            origin = ManufacturerOrigin.EGYPTIAN,
            country = "Egypt",
            officialWebsite = "https://www.elsewedy.net/",
            catalogWebsite = "https://www.elsewedy.net/ar/التنزيلات/"
        ),

        Manufacturer(
            id = "ELSEWEDY_CABLES",
            name = "Elsewedy Cables",
            origin = ManufacturerOrigin.EGYPTIAN,
            country = "Egypt",
            officialWebsite = "https://www.elsewedy.net/",
            catalogWebsite = "https://www.elsewedy.net/cables-wires/"
        ),

        Manufacturer(
            id = "EEC",
            name = "Electro Cable Egypt",
            origin = ManufacturerOrigin.EGYPTIAN,
            country = "Egypt",
            officialWebsite = "https://www.ece.com.eg/"
        ),

        Manufacturer(
            id = "GIZA_CABLES",
            name = "Giza Cable Industries",
            origin = ManufacturerOrigin.EGYPTIAN,
            country = "Egypt",
            officialWebsite = "https://www.gizacables.com/"
        ),

        Manufacturer(
            id = "EGYTRAFO",
            name = "EgyTrafo",
            origin = ManufacturerOrigin.EGYPTIAN,
            country = "Egypt",
            officialWebsite = "https://www.egytrafo.com/"
        ),

        Manufacturer(
            id = "ELMACO",
            name = "ElMACO",
            origin = ManufacturerOrigin.EGYPTIAN,
            country = "Egypt",
            officialWebsite = "https://www.elmaco.com/"
        ),

        Manufacturer(
            id = "SEGA_M",
            name = "SEGA-M",
            origin = ManufacturerOrigin.EGYPTIAN,
            country = "Egypt",
            officialWebsite = "https://sega-m.com/"
        ),

        Manufacturer(
            id = "SCHNEIDER",
            name = "Schneider Electric",
            origin = ManufacturerOrigin.INTERNATIONAL,
            country = "France",
            officialWebsite = "https://www.se.com/"
        ),

        Manufacturer(
            id = "ABB",
            name = "ABB",
            origin = ManufacturerOrigin.INTERNATIONAL,
            country = "Switzerland",
            officialWebsite = "https://www.abb.com/"
        ),

        Manufacturer(
            id = "SIEMENS",
            name = "Siemens",
            origin = ManufacturerOrigin.INTERNATIONAL,
            country = "Germany",
            officialWebsite = "https://www.siemens.com/"
        ),

        Manufacturer(
            id = "CHINT",
            name = "CHINT",
            origin = ManufacturerOrigin.INTERNATIONAL,
            country = "China",
            officialWebsite = "https://www.chintglobal.com/"
        )
    )

    val documents: List<CatalogDocument> = listOf(

        CatalogDocument(
            id = "ELSEWEDY_DOWNLOADS",
            manufacturerId = "ELSEWEDY_SEI",
            title = "ElSewedy Engineering Industries Official Downloads",
            revision = "Current website revision",
            language = "Arabic/English",
            documentType = "Official catalog index",
            officialUrl = "https://www.elsewedy.net/ar/التنزيلات/",
            verified = true
        ),

        CatalogDocument(
            id = "ELSEWEDY_PANELS",
            manufacturerId = "ELSEWEDY_SEI",
            title = "ElSewedy Panels Catalog",
            revision = "Official website revision",
            language = "Arabic/English",
            documentType = "LV/MV Panels Catalog",
            officialUrl = "https://www.elsewedy.net/ar/المنتجات/اللوحات/",
            verified = true
        ),

        CatalogDocument(
            id = "ELSEWEDY_CABLES",
            manufacturerId = "ELSEWEDY_CABLES",
            title = "ElSewedy Cables & Wires",
            revision = "Official website revision",
            language = "English",
            documentType = "Cable product source",
            officialUrl = "https://www.elsewedy.net/product/cables-wires/",
            verified = true
        )
    )

    fun egyptianManufacturers(): List<Manufacturer> =
        manufacturers.filter {
            it.origin == ManufacturerOrigin.EGYPTIAN
        }

    fun internationalManufacturers(): List<Manufacturer> =
        manufacturers.filter {
            it.origin == ManufacturerOrigin.INTERNATIONAL
        }

    fun manufacturer(id: String): Manufacturer? =
        manufacturers.firstOrNull {
            it.id == id
        }

    fun documentsForManufacturer(
        manufacturerId: String
    ): List<CatalogDocument> =
        documents.filter {
            it.manufacturerId == manufacturerId
        }
}
