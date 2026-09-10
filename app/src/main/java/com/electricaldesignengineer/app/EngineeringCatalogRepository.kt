package com.electricaldesignengineer.app

/**
 * ================================================================
 * ENGINEERING CATALOG REPOSITORY
 * ================================================================
 *
 * Embedded engineering/product database.
 *
 * Architecture:
 *
 * UI
 *   ↓
 * AutoDesignService / ProjectManager
 *   ↓
 * ProfessionalEngineeringCore
 *   ↓
 * EngineeringCatalogRepository
 *
 * RULES:
 *
 * 1. This repository contains engineering/product DATA only.
 * 2. Engineering formulas remain in ProfessionalEngineeringCore.
 * 3. Only verified records are exposed to the calculation core.
 * 4. Manufacturer data does not replace IEC/Egyptian design rules.
 * 5. Correction factors remain explicit engineering inputs.
 * 6. Exact manufacturer part numbers are not invented.
 * 7. Transformer impedance is never invented.
 *
 * REFERENCES:
 *
 * IEC 60364-5-52:2009 + AMD1:2024
 * IEC 60947-2:2024
 * IEC 60909-0:2026
 * IEC 60076 series
 * IEC 60034 series
 * Egyptian Electrical Installation Code
 *
 * ================================================================
 */

object EngineeringCatalogRepository {

    // ================================================================
    // CABLE
    // ================================================================

    data class CableRecord(
        val id: String,
        val manufacturerId: String,
        val manufacturerName: String,
        val catalogId: String,
        val catalogName: String,
        val catalogRevision: String,
        val productFamily: String,
        val partNumber: String?,
        val material: CableMaterial,
        val insulation: InsulationType,
        val cores: Int,
        val sizeMm2: Double,
        val voltageRatingV: Int,
        val installationMethod: InstallationMethod,
        val baseAmpacityA: Double?,
        val resistanceOhmPerKm: Double?,
        val reactanceOhmPerKm: Double?,
        val referenceTemperatureC: Double?,
        val shortCircuitKA: Double?,
        val shortCircuitDurationS: Double?,
        val standardCode: String?,
        val sourceUrl: String?,
        val verified: Boolean
    )

    // ================================================================
    // BREAKER
    // ================================================================

    data class BreakerRecord(
        val id: String,
        val manufacturerId: String,
        val manufacturerName: String,
        val catalogId: String,
        val catalogName: String,
        val catalogRevision: String,
        val productFamily: String,
        val partNumber: String?,
        val type: BreakerType,
        val poles: Int,
        val ratedCurrentA: Double,
        val ratedVoltageV: Double,
        val frequencyHz: Double,
        val icuKA: Double,
        val icsKA: Double?,
        val shortTimeWithstandKA: Double?,
        val ratedShortTimeS: Double?,
        val frameSizeA: Double?,
        val tripUnit: String?,
        val adjustableLongTime: Boolean,
        val adjustableShortTime: Boolean,
        val instantaneousProtection: Boolean,
        val standardCode: String?,
        val sourceUrl: String?,
        val verified: Boolean
    )

    enum class BreakerType {
        MCB,
        MCCB,
        ACB,
        OTHER
    }

    // ================================================================
    // TRANSFORMER
    // ================================================================

    data class TransformerRecord(
        val id: String,
        val manufacturerId: String,
        val manufacturerName: String,
        val catalogId: String,
        val catalogName: String,
        val catalogRevision: String,
        val productFamily: String,
        val partNumber: String?,
        val ratedPowerKVA: Double,
        val primaryVoltageV: Double,
        val secondaryVoltageV: Double,
        val frequencyHz: Double,
        val vectorGroup: String?,
        val impedancePercent: Double?,
        val noLoadLossKW: Double?,
        val loadLossKW: Double?,
        val coolingClass: String?,
        val standardCode: String?,
        val sourceUrl: String?,
        val verified: Boolean
    )

    // ================================================================
    // GENERATOR
    // ================================================================

    data class GeneratorRecord(
        val id: String,
        val manufacturerId: String,
        val manufacturerName: String,
        val catalogId: String,
        val catalogName: String,
        val catalogRevision: String,
        val productFamily: String,
        val model: String?,
        val ratedPowerKVA: Double,
        val ratedPowerKW: Double?,
        val ratedVoltageV: Double,
        val frequencyHz: Double,
        val powerFactor: Double,
        val standbyRating: Boolean,
        val primeRating: Boolean,
        val shortCircuitDataAvailable: Boolean,
        val standardCode: String?,
        val sourceUrl: String?,
        val verified: Boolean
    )

    // ================================================================
    // BUSBAR
    // ================================================================

    data class BusbarRecord(
        val id: String,
        val manufacturerId: String,
        val manufacturerName: String,
        val catalogId: String,
        val catalogName: String,
        val catalogRevision: String,
        val productFamily: String,
        val partNumber: String?,
        val ratedCurrentA: Double,
        val ratedVoltageV: Double,
        val shortCircuitKA: Double?,
        val shortCircuitDurationS: Double?,
        val ipRating: String?,
        val standardCode: String?,
        val sourceUrl: String?,
        val verified: Boolean
    )

    // ================================================================
    // PANEL
    // ================================================================

    data class PanelRecord(
        val id: String,
        val manufacturerId: String,
        val manufacturerName: String,
        val catalogId: String,
        val catalogName: String,
        val catalogRevision: String,
        val productFamily: String,
        val partNumber: String?,
        val ratedCurrentA: Double,
        val ratedVoltageV: Double,
        val shortCircuitKA: Double?,
        val ipRating: String?,
        val formOfSeparation: String?,
        val standardCode: String?,
        val sourceUrl: String?,
        val verified: Boolean
    )

    // ================================================================
    // MANUFACTURERS
    // ================================================================

    data class ManufacturerRecord(
        val id: String,
        val name: String,
        val country: String,
        val officialWebsite: String,
        val active: Boolean = true
    )

    val manufacturers: List<ManufacturerRecord> = listOf(

        ManufacturerRecord(
            "ELSEWEDY",
            "Elsewedy Electric",
            "Egypt",
            "https://www.elsewedy.com/"
        ),

        ManufacturerRecord(
            "SCHNEIDER",
            "Schneider Electric",
            "France",
            "https://www.se.com/"
        ),

        ManufacturerRecord(
            "ABB",
            "ABB",
            "Switzerland",
            "https://www.abb.com/"
        ),

        ManufacturerRecord(
            "SIEMENS",
            "Siemens",
            "Germany",
            "https://www.siemens.com/"
        ),

        ManufacturerRecord(
            "PRYSMIAN",
            "Prysmian",
            "Italy",
            "https://www.prysmian.com/"
        ),

        ManufacturerRecord(
            "PRAMAC",
            "Pramac",
            "Italy",
            "https://www.pramac.com/"
        ),

        ManufacturerRecord(
            "ECE",
            "Electro Cable Egypt",
            "Egypt",
            "https://www.ece.com.eg/"
        ),

        ManufacturerRecord(
            "GIZA_CABLES",
            "Giza Cable Industries",
            "Egypt",
            "https://www.gizacables.com/"
        ),

        ManufacturerRecord(
            "CHINT",
            "CHINT",
            "China",
            "https://www.chintglobal.com/"
        ),

        ManufacturerRecord(
            "CUMMINS",
            "Cummins",
            "United States",
            "https://www.cummins.com/"
        ),

        ManufacturerRecord(
            "CAT",
            "Caterpillar",
            "United States",
            "https://www.cat.com/"
        )
    )

    // ================================================================
    // INTERNAL DATABASE
    // ================================================================

    private val cableRecords =
        mutableListOf<CableRecord>()

    private val breakerRecords =
        mutableListOf<BreakerRecord>()

    private val transformerRecords =
        mutableListOf<TransformerRecord>()

    private val generatorRecords =
        mutableListOf<GeneratorRecord>()

    private val busbarRecords =
        mutableListOf<BusbarRecord>()

    private val panelRecords =
        mutableListOf<PanelRecord>()

    private var initialized = false

    // ================================================================
    // INITIALIZATION
    // ================================================================

    @Synchronized
    fun initialize() {

        if (initialized) {
            return
        }

        cableRecords.clear()
        breakerRecords.clear()
        transformerRecords.clear()
        generatorRecords.clear()
        busbarRecords.clear()
        panelRecords.clear()

        loadEmbeddedDatabase()

        initialized = true
    }

    private fun ensureInitialized() {

        if (!initialized) {
            initialize()
        }
    }

    // ================================================================
    // EMBEDDED DATABASE
    // ================================================================

    private fun loadEmbeddedDatabase() {

        loadPrysmianCables()

        loadElsewedyBreakers()

        loadSchneiderBreakers()

        loadABBBreakers()

        loadSiemensBreakers()

        loadPramacGenerators()

        /*
         * Transformer database intentionally remains empty until
         * complete verified manufacturer data is available.
         *
         * NEVER invent transformer impedance.
         */
    }

    // ================================================================
    // PRYSMIAN CABLES
    // ================================================================

    private fun loadPrysmianCables() {

        val singleCoreSource =
            "https://australia.prysmian.com/sites/australia.prysmian.com/files/media/documents/xlpe-singlecore-90-copper-sdi.pdf"

        val twoCoreSource =
            "https://australia.prysmian.com/sites/australia.prysmian.com/files/media/documents/2ce-xlpe-pvc-circular.pdf"

        /*
         * Single-core CU XLPE/PVC 0.6/1kV
         *
         * Mapping:
         *
         * CABLE_TRAY    = unenclosed / spaced installation
         * DIRECT_BURIED = buried direct
         * DUCT          = underground in duct
         *
         * We do NOT map these values to conduit/trunking/free-air.
         */

        val singleCoreData = listOf(

            CablePoint(25.0, 125.0, 150.0, 115.0, 0.727, 0.102),

            CablePoint(35.0, 155.0, 180.0, 140.0, 0.524, 0.0982),

            CablePoint(50.0, 190.0, 215.0, 170.0, 0.387, 0.0924),

            CablePoint(70.0, 240.0, 260.0, 210.0, 0.268, 0.0893),

            CablePoint(95.0, 300.0, 315.0, 250.0, 0.193, 0.0868),

            CablePoint(120.0, 350.0, 355.0, 290.0, 0.153, 0.0844),

            CablePoint(150.0, 405.0, 400.0, 330.0, 0.124, 0.0844),

            CablePoint(185.0, 470.0, 450.0, 375.0, 0.0991, 0.0835),

            CablePoint(240.0, 560.0, 520.0, 440.0, 0.0754, 0.0818),

            CablePoint(300.0, 650.0, 590.0, 510.0, 0.0601, 0.0809),

            CablePoint(400.0, 760.0, 670.0, 580.0, 0.0470, 0.0802),

            CablePoint(500.0, 870.0, 750.0, 670.0, 0.0366, 0.0796),

            CablePoint(630.0, 1010.0, 840.0, 760.0, 0.0283, 0.0787)
        )

        singleCoreData.forEachIndexed { index, p ->

            addCableInternal(
                cable(
                    id = "PRYSMIAN_SC_${p.size}_${index}_TRAY",
                    family = "CU/XLPE/PVC 0.6/1kV Single Core",
                    size = p.size,
                    installation = InstallationMethod.CABLE_TRAY,
                    ampacity = p.air,
                    resistance = p.r,
                    reactance = p.x,
                    source = singleCoreSource
                )
            )

            addCableInternal(
                cable(
                    id = "PRYSMIAN_SC_${p.size}_${index}_BURIED",
                    family = "CU/XLPE/PVC 0.6/1kV Single Core",
                    size = p.size,
                    installation = InstallationMethod.DIRECT_BURIED,
                    ampacity = p.buried,
                    resistance = p.r,
                    reactance = p.x,
                    source = singleCoreSource
                )
            )

            addCableInternal(
                cable(
                    id = "PRYSMIAN_SC_${p.size}_${index}_DUCT",
                    family = "CU/XLPE/PVC 0.6/1kV Single Core",
                    size = p.size,
                    installation = InstallationMethod.DUCT,
                    ampacity = p.duct,
                    resistance = p.r,
                    reactance = p.x,
                    source = singleCoreSource
                )
            )
        }

        /*
         * 2C + E CU XLPE/PVC
         */

        val twoCoreData = listOf(

            CablePoint(1.5, 24.0, 33.0, 25.0, 13.6, 0.107),

            CablePoint(2.5, 34.0, 46.0, 35.0, 7.41, 0.0988),

            CablePoint(4.0, 45.0, 60.0, 46.0, 4.61, 0.0930),

            CablePoint(6.0, 57.0, 75.0, 57.0, 3.08, 0.0887),

            CablePoint(10.0, 78.0, 100.0, 77.0, 1.83, 0.0840),

            CablePoint(16.0, 105.0, 130.0, 100.0, 1.15, 0.0805)
        )

        twoCoreData.forEachIndexed { index, p ->

            addCableInternal(
                cable(
                    id = "PRYSMIAN_2CE_${p.size}_${index}_TRAY",
                    family = "CU/XLPE/PVC 0.6/1kV 2C+E",
                    size = p.size,
                    cores = 2,
                    installation = InstallationMethod.CABLE_TRAY,
                    ampacity = p.air,
                    resistance = p.r,
                    reactance = p.x,
                    source = twoCoreSource
                )
            )

            addCableInternal(
                cable(
                    id = "PRYSMIAN_2CE_${p.size}_${index}_BURIED",
                    family = "CU/XLPE/PVC 0.6/1kV 2C+E",
                    size = p.size,
                    cores = 2,
                    installation = InstallationMethod.DIRECT_BURIED,
                    ampacity = p.buried,
                    resistance = p.r,
                    reactance = p.x,
                    source = twoCoreSource
                )
            )

            addCableInternal(
                cable(
                    id = "PRYSMIAN_2CE_${p.size}_${index}_DUCT",
                    family = "CU/XLPE/PVC 0.6/1kV 2C+E",
                    size = p.size,
                    cores = 2,
                    installation = InstallationMethod.DUCT,
                    ampacity = p.duct,
                    resistance = p.r,
                    reactance = p.x,
                    source = twoCoreSource
                )
            )
        }
    }

    private data class CablePoint(
        val size: Double,
        val air: Double,
        val buried: Double,
        val duct: Double,
        val r: Double,
        val x: Double
    )

    private fun cable(
        id: String,
        family: String,
        size: Double,
        cores: Int = 1,
        installation: InstallationMethod,
        ampacity: Double,
        resistance: Double,
        reactance: Double,
        source: String
    ): CableRecord {

        return CableRecord(
            id = id,
            manufacturerId = "PRYSMIAN",
            manufacturerName = "Prysmian",
            catalogId = "PRYSMIAN_XLPE_LV",
            catalogName = family,
            catalogRevision = "Embedded verified dataset",
            productFamily = family,
            partNumber = null,
            material = CableMaterial.COPPER,
            insulation = InsulationType.XLPE,
            cores = cores,
            sizeMm2 = size,
            voltageRatingV = 1000,
            installationMethod = installation,
            baseAmpacityA = ampacity,
            resistanceOhmPerKm = resistance,
            reactanceOhmPerKm = reactance,
            referenceTemperatureC = 40.0,
            shortCircuitKA = null,
            shortCircuitDurationS = null,
            standardCode = "Manufacturer technical data",
            sourceUrl = source,
            verified = true
        )
    }

    // ================================================================
    // ELSEWEDY
    // ================================================================

    private fun loadElsewedyBreakers() {

        val source =
            "https://elsewedy.net/wp-content/uploads/2023/07/ELSEWEDY-Braker-Control_Final-catalogue.pdf"

        addElsewedyBreaker(
            family = "SE-100",
            current = 100.0,
            frame = 100.0,
            icu = 35.0,
            source = source
        )

        addElsewedyBreaker(
            family = "SE-250",
            current = 250.0,
            frame = 250.0,
            icu = 35.0,
            source = source
        )

        addElsewedyBreaker(
            family = "SE-400",
            current = 400.0,
            frame = 400.0,
            icu = 50.0,
            source = source
        )

        addElsewedyBreaker(
            family = "SE-630",
            current = 630.0,
            frame = 630.0,
            icu = 50.0,
            source = source
        )

        addElsewedyBreaker(
            family = "SE-800",
            current = 800.0,
            frame = 800.0,
            icu = 50.0,
            source = source
        )
    }

    private fun addElsewedyBreaker(
        family: String,
        current: Double,
        frame: Double,
        icu: Double,
        source: String
    ) {

        breakerRecords += BreakerRecord(

            id = "ELSEWEDY_${family}_${current.toInt()}A_3P",

            manufacturerId = "ELSEWEDY",

            manufacturerName = "Elsewedy Electric",

            catalogId = "ELSEWEDY_SE_MCCB",

            catalogName = "SE Electronic Molded Case Circuit Breaker",

            catalogRevision = "Official catalogue",

            productFamily = family,

            partNumber = null,

            type = BreakerType.MCCB,

            poles = 3,

            ratedCurrentA = current,

            ratedVoltageV = 400.0,

            frequencyHz = 50.0,

            icuKA = icu,

            icsKA = icu * 0.75,

            shortTimeWithstandKA = null,

            ratedShortTimeS = null,

            frameSizeA = frame,

            tripUnit = "Electronic",

            adjustableLongTime = true,

            adjustableShortTime = false,

            instantaneousProtection = true,

            standardCode = "IEC 60947-2",

            sourceUrl = source,

            verified = true
        )
    }

    // ================================================================
    // SCHNEIDER ELECTRIC
    // ================================================================

    private fun loadSchneiderBreakers() {

        val source =
            "https://productinfo.se.com/compactnsxuserguide/"

        /*
         * ComPacT NSX N performance:
         *
         * Icu = 50 kA at 415 V
         *
         * Applicable to the NSX 100-250 and NSX 400-630
         * ranges according to the manufacturer performance table.
         */

        val sizes = listOf(
            100.0,
            160.0,
            250.0,
            400.0,
            630.0
        )

        sizes.forEach { current ->

            addSchneiderBreaker(
                current = current,
                family =
                    if (current <= 250.0)
                        "ComPacT NSX${current.toInt()}N"
                    else
                        "ComPacT NSX${current.toInt()}N",
                icu = 50.0,
                source = source
            )
        }
    }

    private fun addSchneiderBreaker(
        current: Double,
        family: String,
        icu: Double,
        source: String
    ) {

        breakerRecords += BreakerRecord(

            id = "SCHNEIDER_${family}_3P",

            manufacturerId = "SCHNEIDER",

            manufacturerName = "Schneider Electric",

            catalogId = "SCHNEIDER_COMPACT_NSX",

            catalogName = "ComPacT NSX",

            catalogRevision = "2026",

            productFamily = family,

            partNumber = null,

            type = BreakerType.MCCB,

            poles = 3,

            ratedCurrentA = current,

            ratedVoltageV = 415.0,

            frequencyHz = 50.0,

            icuKA = icu,

            icsKA = icu,

            shortTimeWithstandKA = null,

            ratedShortTimeS = null,

            frameSizeA = current,

            tripUnit = null,

            adjustableLongTime = true,

            adjustableShortTime = true,

            instantaneousProtection = true,

            standardCode = "IEC 60947-2",

            sourceUrl = source,

            verified = true
        )
    }

    // ================================================================
    // ABB
    // ================================================================

    private fun loadABBBreakers() {

        val source =
            "https://library.e.abb.com/public/32b829ec48034078b7a53e3ad5b0164a/Leaflet%20scelta%20rapida%20TmaxXT.pdf"

        /*
         * ABB Tmax XT
         *
         * N performance = 36 kA at 415 V
         * S performance = 50 kA at 415 V
         */

        addABB(
            family = "Tmax XT3N",
            current = 63.0,
            icu = 36.0,
            frame = 160.0,
            source = source
        )

        addABB(
            family = "Tmax XT3N",
            current = 100.0,
            icu = 36.0,
            frame = 160.0,
            source = source
        )

        addABB(
            family = "Tmax XT2N",
            current = 160.0,
            icu = 36.0,
            frame = 160.0,
            source = source
        )

        addABB(
            family = "Tmax XT2S",
            current = 160.0,
            icu = 50.0,
            frame = 160.0,
            source = source
        )

        addABB(
            family = "Tmax XT4N",
            current = 250.0,
            icu = 36.0,
            frame = 250.0,
            source = source
        )

        addABB(
            family = "Tmax XT4S",
            current = 250.0,
            icu = 50.0,
            frame = 250.0,
            source = source
        )

        addABB(
            family = "Tmax XT5N",
            current = 400.0,
            icu = 36.0,
            frame = 400.0,
            source = source
        )

        addABB(
            family = "Tmax XT5S",
            current = 400.0,
            icu = 50.0,
            frame = 400.0,
            source = source
        )

        addABB(
            family = "Tmax XT6N",
            current = 630.0,
            icu = 36.0,
            frame = 630.0,
            source = source
        )

        addABB(
            family = "Tmax XT6S",
            current = 630.0,
            icu = 50.0,
            frame = 630.0,
            source = source
        )
    }

    private fun addABB(
        family: String,
        current: Double,
        icu: Double,
        frame: Double,
        source: String
    ) {

        breakerRecords += BreakerRecord(

            id = "ABB_${family}_${current.toInt()}A_3P",

            manufacturerId = "ABB",

            manufacturerName = "ABB",

            catalogId = "ABB_TMAX_XT",

            catalogName = "Tmax XT",

            catalogRevision = "Embedded verified dataset",

            productFamily = family,

            partNumber = null,

            type = BreakerType.MCCB,

            poles = 3,

            ratedCurrentA = current,

            ratedVoltageV = 415.0,

            frequencyHz = 50.0,

            icuKA = icu,

            icsKA = icu,

            shortTimeWithstandKA = null,

            ratedShortTimeS = null,

            frameSizeA = frame,

            tripUnit = null,

            adjustableLongTime = true,

            adjustableShortTime = true,

            instantaneousProtection = true,

            standardCode = "IEC 60947-2",

            sourceUrl = source,

            verified = true
        )
    }

    // ================================================================
    // SIEMENS
    // ================================================================

    private fun loadSiemensBreakers() {

        val source =
            "https://cache.industry.siemens.com/dl/files/637/109750637/att_1309204/v1/02_MoldedCaseCircuitBreakers_LV10_2025_EN_202412200153522186.pdf"

        /*
         * SENTRON 3VA2
         *
         * Class M
         * Icu = Ics = 55 kA at 415 V
         *
         * These are family-level selection records.
         * Exact article number depends on trip unit/accessories.
         */

        addSiemens(
            current = 160.0,
            frame = 250.0,
            family = "SENTRON 3VA2 Class M",
            source = source
        )

        addSiemens(
            current = 250.0,
            frame = 250.0,
            family = "SENTRON 3VA2 Class M",
            source = source
        )

        addSiemens(
            current = 400.0,
            frame = 630.0,
            family = "SENTRON 3VA2 Class M",
            source = source
        )

        addSiemens(
            current = 630.0,
            frame = 630.0,
            family = "SENTRON 3VA2 Class M",
            source = source
        )

        addSiemens(
            current = 1000.0,
            frame = 1000.0,
            family = "SENTRON 3VA2 Class M",
            source = source
        )
    }

    private fun addSiemens(
        current: Double,
        frame: Double,
        family: String,
        source: String
    ) {

        breakerRecords += BreakerRecord(

            id = "SIEMENS_3VA2_${current.toInt()}A_3P",

            manufacturerId = "SIEMENS",

            manufacturerName = "Siemens",

            catalogId = "SIEMENS_3VA",

            catalogName = "SENTRON 3VA",

            catalogRevision = "LV10 2025",

            productFamily = family,

            partNumber = null,

            type = BreakerType.MCCB,

            poles = 3,

            ratedCurrentA = current,

            ratedVoltageV = 415.0,

            frequencyHz = 50.0,

            icuKA = 55.0,

            icsKA = 55.0,

            shortTimeWithstandKA = null,

            ratedShortTimeS = null,

            frameSizeA = frame,

            tripUnit = null,

            adjustableLongTime = true,

            adjustableShortTime = true,

            instantaneousProtection = true,

            standardCode = "IEC 60947-2",

            sourceUrl = source,

            verified = true
        )
    }

    // ================================================================
    // PRAMAC GENERATORS
    // ================================================================

    private fun loadPramacGenerators() {

        val source =
            "https://www.pramac.com/product-category?folder=355"

        addPramac(
            model = "GRW100I/S5",
            kVA = 100.0,
            kW = 80.0,
            standbyKVA = 104.3,
            source = source
        )

        addPramac(
            model = "GRW150I/S5",
            kVA = 150.0,
            kW = 120.0,
            standbyKVA = 165.0,
            source = source
        )

        addPramac(
            model = "GRW250I/S5",
            kVA = 250.0,
            kW = 200.0,
            standbyKVA = 275.0,
            source = source
        )

        addPramac(
            model = "GRW300I/S5",
            kVA = 306.7,
            kW = 245.36,
            standbyKVA = 337.2,
            source = source
        )

        addPramac(
            model = "GRW350S/S5",
            kVA = 352.9,
            kW = 282.32,
            standbyKVA = 389.4,
            source = source
        )
    }

    private fun addPramac(
        model: String,
        kVA: Double,
        kW: Double,
        standbyKVA: Double,
        source: String
    ) {

        /*
         * Core GeneratorData represents one rating.
         *
         * We store the PRIME rating as the engineering selection
         * rating and keep standby information in the product family
         * description.
         */

        generatorRecords += GeneratorRecord(

            id = "PRAMAC_$model",

            manufacturerId = "PRAMAC",

            manufacturerName = "Pramac",

            catalogId = "PRAMAC_GRW",

            catalogName = "GRW Series",

            catalogRevision = "Stage V",

            productFamily =
                "GRW - ESP ${standbyKVA} kVA / PRP ${kVA} kVA",

            model = model,

            ratedPowerKVA = kVA,

            ratedPowerKW = kW,

            ratedVoltageV = 400.0,

            frequencyHz = 50.0,

            powerFactor = 0.8,

            standbyRating = false,

            primeRating = true,

            shortCircuitDataAvailable = false,

            standardCode = "Manufacturer technical data",

            sourceUrl = source,

            verified = true
        )
    }

    // ================================================================
    // CORE PROVIDERS
    // ================================================================

    fun getCableData(
        material: CableMaterial,
        insulation: InsulationType,
        installationMethod: InstallationMethod
    ): List<ProfessionalEngineeringCore.CableData> {

        ensureInitialized()

        return cableRecords
            .filter {
                it.verified &&
                        it.material == material &&
                        it.insulation == insulation &&
                        it.installationMethod == installationMethod
            }
            .filter {
                it.baseAmpacityA != null &&
                        it.resistanceOhmPerKm != null &&
                        it.reactanceOhmPerKm != null
            }
            .map {

                ProfessionalEngineeringCore.CableData(

                    sizeMm2 =
                        it.sizeMm2,

                    baseAmpacityA =
                        it.baseAmpacityA!!,

                    resistanceOhmPerKm =
                        it.resistanceOhmPerKm!!,

                    reactanceOhmPerKm =
                        it.reactanceOhmPerKm!!,

                    source =
                        "${it.manufacturerName} - ${it.productFamily}",

                    revision =
                        it.catalogRevision
                )
            }
            .sortedBy {
                it.sizeMm2
            }
    }

    fun getBreakerData(
        requiredPoles: Int
    ): List<ProfessionalEngineeringCore.BreakerData> {

        ensureInitialized()

        return breakerRecords
            .filter {
                it.verified &&
                        it.poles == requiredPoles
            }
            .map {

                ProfessionalEngineeringCore.BreakerData(

                    manufacturerId =
                        it.manufacturerId,

                    productFamily =
                        it.productFamily,

                    partNumber =
                        it.partNumber,

                    ratedCurrentA =
                        it.ratedCurrentA,

                    ratedVoltageV =
                        it.ratedVoltageV,

                    poles =
                        it.poles,

                    icuKA =
                        it.icuKA,

                    icsKA =
                        it.icsKA,

                    source =
                        it.manufacturerName,

                    revision =
                        it.catalogRevision
                )
            }
            .sortedBy {
                it.ratedCurrentA
            }
    }

    fun getTransformerData():
            List<ProfessionalEngineeringCore.TransformerData> {

        ensureInitialized()

        return transformerRecords
            .filter {
                it.verified
            }
            .map {

                ProfessionalEngineeringCore.TransformerData(

                    id =
                        it.id,

                    manufacturerId =
                        it.manufacturerId,

                    manufacturerName =
                        it.manufacturerName,

                    catalogId =
                        it.catalogId,

                    catalogName =
                        it.catalogName,

                    catalogRevision =
                        it.catalogRevision,

                    productFamily =
                        it.productFamily,

                    partNumber =
                        it.partNumber,

                    ratedPowerKVA =
                        it.ratedPowerKVA,

                    primaryVoltageV =
                        it.primaryVoltageV,

                    secondaryVoltageV =
                        it.secondaryVoltageV,

                    frequencyHz =
                        it.frequencyHz,

                    vectorGroup =
                        it.vectorGroup,

                    impedancePercent =
                        it.impedancePercent,

                    noLoadLossKW =
                        it.noLoadLossKW,

                    loadLossKW =
                        it.loadLossKW,

                    coolingClass =
                        it.coolingClass,

                    standardCode =
                        it.standardCode,

                    sourceUrl =
                        it.sourceUrl,

                    verified =
                        it.verified
                )
            }
            .sortedBy {
                it.ratedPowerKVA
            }
    }

    fun getGeneratorData():
            List<ProfessionalEngineeringCore.GeneratorData> {

        ensureInitialized()

        return generatorRecords
            .filter {
                it.verified
            }
            .map {

                ProfessionalEngineeringCore.GeneratorData(

                    id =
                        it.id,

                    manufacturerId =
                        it.manufacturerId,

                    manufacturerName =
                        it.manufacturerName,

                    catalogId =
                        it.catalogId,

                    catalogName =
                        it.catalogName,

                    catalogRevision =
                        it.catalogRevision,

                    productFamily =
                        it.productFamily,

                    model =
                        it.model,

                    ratedPowerKVA =
                        it.ratedPowerKVA,

                    ratedPowerKW =
                        it.ratedPowerKW,

                    ratedVoltageV =
                        it.ratedVoltageV,

                    frequencyHz =
                        it.frequencyHz,

                    powerFactor =
                        it.powerFactor,

                    standbyRating =
                        it.standbyRating,

                    primeRating =
                        it.primeRating,

                    shortCircuitDataAvailable =
                        it.shortCircuitDataAvailable,

                    standardCode =
                        it.standardCode,

                    sourceUrl =
                        it.sourceUrl,

                    verified =
                        it.verified
                )
            }
            .sortedBy {
                it.ratedPowerKVA
            }
    }

    // ================================================================
    // DIRECT ACCESS
    // ================================================================

    fun allCables(): List<CableRecord> {

        ensureInitialized()

        return cableRecords.toList()
    }

    fun allBreakers(): List<BreakerRecord> {

        ensureInitialized()

        return breakerRecords.toList()
    }

    fun allTransformers(): List<TransformerRecord> {

        ensureInitialized()

        return transformerRecords.toList()
    }

    fun allGenerators(): List<GeneratorRecord> {

        ensureInitialized()

        return generatorRecords.toList()
    }

    fun allBusbars(): List<BusbarRecord> {

        ensureInitialized()

        return busbarRecords.toList()
    }

    fun allPanels(): List<PanelRecord> {

        ensureInitialized()

        return panelRecords.toList()
    }

    // ================================================================
    // SEARCH CABLES
    // ================================================================

    fun searchCables(
        manufacturerId: String? = null,
        material: CableMaterial? = null,
        insulation: InsulationType? = null,
        minimumSizeMm2: Double? = null,
        maximumSizeMm2: Double? = null
    ): List<CableRecord> {

        ensureInitialized()

        return cableRecords
            .filter { it.verified }
            .filter {
                manufacturerId == null ||
                        it.manufacturerId == manufacturerId
            }
            .filter {
                material == null ||
                        it.material == material
            }
            .filter {
                insulation == null ||
                        it.insulation == insulation
            }
            .filter {
                minimumSizeMm2 == null ||
                        it.sizeMm2 >= minimumSizeMm2
            }
            .filter {
                maximumSizeMm2 == null ||
                        it.sizeMm2 <= maximumSizeMm2
            }
            .sortedBy {
                it.sizeMm2
            }
    }

    // ================================================================
    // SEARCH BREAKERS
    // ================================================================

    fun searchBreakers(
        manufacturerId: String? = null,
        type: BreakerType? = null,
        minimumCurrentA: Double? = null,
        maximumCurrentA: Double? = null,
        minimumIcuKA: Double? = null,
        poles: Int? = null
    ): List<BreakerRecord> {

        ensureInitialized()

        return breakerRecords
            .filter { it.verified }
            .filter {
                manufacturerId == null ||
                        it.manufacturerId == manufacturerId
            }
            .filter {
                type == null ||
                        it.type == type
            }
            .filter {
                minimumCurrentA == null ||
                        it.ratedCurrentA >= minimumCurrentA
            }
            .filter {
                maximumCurrentA == null ||
                        it.ratedCurrentA <= maximumCurrentA
            }
            .filter {
                minimumIcuKA == null ||
                        it.icuKA >= minimumIcuKA
            }
            .filter {
                poles == null ||
                        it.poles == poles
            }
            .sortedBy {
                it.ratedCurrentA
            }
    }

    // ================================================================
    // SEARCH TRANSFORMERS
    // ================================================================

    fun searchTransformers(
        manufacturerId: String? = null,
        minimumKVA: Double? = null,
        maximumKVA: Double? = null
    ): List<TransformerRecord> {

        ensureInitialized()

        return transformerRecords
            .filter { it.verified }
            .filter {
                manufacturerId == null ||
                        it.manufacturerId == manufacturerId
            }
            .filter {
                minimumKVA == null ||
                        it.ratedPowerKVA >= minimumKVA
            }
            .filter {
                maximumKVA == null ||
                        it.ratedPowerKVA <= maximumKVA
            }
            .sortedBy {
                it.ratedPowerKVA
            }
    }

    // ================================================================
    // SEARCH GENERATORS
    // ================================================================

    fun searchGenerators(
        manufacturerId: String? = null,
        minimumKVA: Double? = null,
        maximumKVA: Double? = null
    ): List<GeneratorRecord> {

        ensureInitialized()

        return generatorRecords
            .filter { it.verified }
            .filter {
                manufacturerId == null ||
                        it.manufacturerId == manufacturerId
            }
            .filter {
                minimumKVA == null ||
                        it.ratedPowerKVA >= minimumKVA
            }
            .filter {
                maximumKVA == null ||
                        it.ratedPowerKVA <= maximumKVA
            }
            .sortedBy {
                it.ratedPowerKVA
            }
    }

    // ================================================================
    // ADD
    // ================================================================

    fun addCable(
        record: CableRecord
    ): Boolean {

        ensureInitialized()

        if (!validateCableRecord(record)) {
            return false
        }

        if (cableRecords.any { it.id == record.id }) {
            return false
        }

        cableRecords += record

        return true
    }

    fun addBreaker(
        record: BreakerRecord
    ): Boolean {

        ensureInitialized()

        if (!validateBreakerRecord(record)) {
            return false
        }

        if (breakerRecords.any { it.id == record.id }) {
            return false
        }

        breakerRecords += record

        return true
    }

    fun addTransformer(
        record: TransformerRecord
    ): Boolean {

        ensureInitialized()

        if (!validateTransformerRecord(record)) {
            return false
        }

        if (transformerRecords.any { it.id == record.id }) {
            return false
        }

        transformerRecords += record

        return true
    }

    fun addGenerator(
        record: GeneratorRecord
    ): Boolean {

        ensureInitialized()

        if (!validateGeneratorRecord(record)) {
            return false
        }

        if (generatorRecords.any { it.id == record.id }) {
            return false
        }

        generatorRecords += record

        return true
    }

    fun addBusbar(
        record: BusbarRecord
    ): Boolean {

        ensureInitialized()

        if (!validateBusbarRecord(record)) {
            return false
        }

        if (busbarRecords.any { it.id == record.id }) {
            return false
        }

        busbarRecords += record

        return true
    }

    fun addPanel(
        record: PanelRecord
    ): Boolean {

        ensureInitialized()

        if (!validatePanelRecord(record)) {
            return false
        }

        if (panelRecords.any { it.id == record.id }) {
            return false
        }

        panelRecords += record

        return true
    }

    // ================================================================
    // INTERNAL ADD
    // ================================================================

    private fun addCableInternal(
        record: CableRecord
    ) {

        if (validateCableRecord(record)) {

            cableRecords.removeAll {
                it.id == record.id
            }

            cableRecords += record
        }
    }

    // ================================================================
    // VALIDATION
    // ================================================================

    private fun validateCableRecord(
        record: CableRecord
    ): Boolean {

        if (record.id.isBlank()) return false

        if (record.manufacturerId.isBlank()) return false

        if (record.catalogId.isBlank()) return false

        if (record.productFamily.isBlank()) return false

        if (record.cores <= 0) return false

        if (record.sizeMm2 <= 0.0) return false

        if (record.voltageRatingV <= 0) return false

        if (
            record.baseAmpacityA != null &&
            record.baseAmpacityA <= 0.0
        ) {
            return false
        }

        if (
            record.resistanceOhmPerKm != null &&
            record.resistanceOhmPerKm <= 0.0
        ) {
            return false
        }

        if (
            record.reactanceOhmPerKm != null &&
            record.reactanceOhmPerKm < 0.0
        ) {
            return false
        }

        return true
    }

    private fun validateBreakerRecord(
        record: BreakerRecord
    ): Boolean {

        if (record.id.isBlank()) return false

        if (record.manufacturerId.isBlank()) return false

        if (record.catalogId.isBlank()) return false

        if (record.productFamily.isBlank()) return false

        if (record.poles <= 0) return false

        if (record.ratedCurrentA <= 0.0) return false

        if (record.ratedVoltageV <= 0.0) return false

        if (record.frequencyHz <= 0.0) return false

        if (record.icuKA <= 0.0) return false

        if (
            record.icsKA != null &&
            record.icsKA <= 0.0
        ) {
            return false
        }

        return true
    }

    private fun validateTransformerRecord(
        record: TransformerRecord
    ): Boolean {

        if (record.id.isBlank()) return false

        if (record.manufacturerId.isBlank()) return false

        if (record.catalogId.isBlank()) return false

        if (record.productFamily.isBlank()) return false

        if (record.ratedPowerKVA <= 0.0) return false

        if (record.primaryVoltageV <= 0.0) return false

        if (record.secondaryVoltageV <= 0.0) return false

        if (record.frequencyHz <= 0.0) return false

        if (
            record.impedancePercent != null &&
            record.impedancePercent <= 0.0
        ) {
            return false
        }

        /*
         * A VERIFIED transformer MUST have impedance.
         */

        if (
            record.verified &&
            record.impedancePercent == null
        ) {
            return false
        }

        return true
    }

    private fun validateGeneratorRecord(
        record: GeneratorRecord
    ): Boolean {

        if (record.id.isBlank()) return false

        if (record.manufacturerId.isBlank()) return false

        if (record.catalogId.isBlank()) return false

        if (record.productFamily.isBlank()) return false

        if (record.ratedPowerKVA <= 0.0) return false

        if (record.ratedVoltageV <= 0.0) return false

        if (record.frequencyHz <= 0.0) return false

        if (
            record.powerFactor <= 0.0 ||
            record.powerFactor > 1.0
        ) {
            return false
        }

        return true
    }

    private fun validateBusbarRecord(
        record: BusbarRecord
    ): Boolean {

        if (record.id.isBlank()) return false

        if (record.manufacturerId.isBlank()) return false

        if (record.catalogId.isBlank()) return false

        if (record.ratedCurrentA <= 0.0) return false

        if (record.ratedVoltageV <= 0.0) return false

        return true
    }

    private fun validatePanelRecord(
        record: PanelRecord
    ): Boolean {

        if (record.id.isBlank()) return false

        if (record.manufacturerId.isBlank()) return false

        if (record.catalogId.isBlank()) return false

        if (record.ratedCurrentA <= 0.0) return false

        if (record.ratedVoltageV <= 0.0) return false

        return true
    }

    // ================================================================
    // REMOVE
    // ================================================================

    fun removeCable(
        id: String
    ): Boolean {

        ensureInitialized()

        return cableRecords.removeIf {
            it.id == id
        }
    }

    fun removeBreaker(
        id: String
    ): Boolean {

        ensureInitialized()

        return breakerRecords.removeIf {
            it.id == id
        }
    }

    fun removeTransformer(
        id: String
    ): Boolean {

        ensureInitialized()

        return transformerRecords.removeIf {
            it.id == id
        }
    }

    fun removeGenerator(
        id: String
    ): Boolean {

        ensureInitialized()

        return generatorRecords.removeIf {
            it.id == id
        }
    }

    fun removeBusbar(
        id: String
    ): Boolean {

        ensureInitialized()

        return busbarRecords.removeIf {
            it.id == id
        }
    }

    fun removePanel(
        id: String
    ): Boolean {

        ensureInitialized()

        return panelRecords.removeIf {
            it.id == id
        }
    }

    // ================================================================
    // CLEAR
    // ================================================================

    fun clearMemoryCache() {

        cableRecords.clear()

        breakerRecords.clear()

        transformerRecords.clear()

        generatorRecords.clear()

        busbarRecords.clear()

        panelRecords.clear()

        initialized = false
    }

    // ================================================================
    // STATISTICS
    // ================================================================

    data class CatalogStatistics(
        val cables: Int,
        val breakers: Int,
        val transformers: Int,
        val generators: Int,
        val busbars: Int,
        val panels: Int,
        val verifiedCables: Int,
        val verifiedBreakers: Int,
        val verifiedTransformers: Int,
        val verifiedGenerators: Int,
        val verifiedBusbars: Int,
        val verifiedPanels: Int
    )

    fun statistics(): CatalogStatistics {

        ensureInitialized()

        return CatalogStatistics(

            cables =
                cableRecords.size,

            breakers =
                breakerRecords.size,

            transformers =
                transformerRecords.size,

            generators =
                generatorRecords.size,

            busbars =
                busbarRecords.size,

            panels =
                panelRecords.size,

            verifiedCables =
                cableRecords.count { it.verified },

            verifiedBreakers =
                breakerRecords.count { it.verified },

            verifiedTransformers =
                transformerRecords.count { it.verified },

            verifiedGenerators =
                generatorRecords.count { it.verified },

            verifiedBusbars =
                busbarRecords.count { it.verified },

            verifiedPanels =
                panelRecords.count { it.verified }
        )
    }

    // ================================================================
    // ENGINEERING REFERENCES
    // ================================================================

    object DesignReferences {

        const val IEC_60364_5_52 =
            "IEC 60364-5-52:2009+AMD1:2024"

        const val IEC_60947_2 =
            "IEC 60947-2:2024"

        const val IEC_60909_0 =
            "IEC 60909-0:2026"

        const val IEC_60076 =
            "IEC 60076 series"

        const val IEC_60034 =
            "IEC 60034 series"

        const val EGYPTIAN_ELECTRICAL_CODE =
            "Egyptian Electrical Installation Code"

        const val EGYPTIAN_CODE_D17 =
            "D17 - Egyptian Electrical Installation Code"

        const val EGYPTIAN_CODE_D18 =
            "D18 - Egyptian Electrical Installation Code"

        const val EGYPTIAN_CODE_D19 =
            "D19 - Egyptian Electrical Installation Code"
    }
}
