package com.electricaldesignengineer.app

/**
 * ================================================================
 * ENGINEERING CATALOG REPOSITORY
 * ================================================================
 *
 * Self-contained engineering catalog.
 *
 * The application does NOT require downloading manufacturer
 * catalogues during normal operation.
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
 * IMPORTANT:
 * 1. Manufacturer/product data is kept separate from formulas.
 * 2. Only records marked verified are supplied to the calculation core.
 * 3. IEC design rules are NOT replaced by manufacturer data.
 * 4. Manufacturer data is used for actual product selection.
 * 5. Engineering design factors remain inputs to the calculation.
 *
 * Standards referenced by this embedded database:
 *
 * - IEC 60364-5-52:2009 + AMD1:2024
 *   Wiring systems / current carrying capacity / voltage drop.
 *
 * - IEC 60947-2:2024
 *   Low-voltage circuit-breakers.
 *
 * - IEC 60076 series
 *   Power transformers.
 *
 * - IEC 60034 series
 *   Rotating electrical machines / generators.
 *
 * - Egyptian Code for Electrical Installations
 *   Relevant Egyptian electrical installation requirements.
 *
 * COPYRIGHT / DATA POLICY:
 *
 * This file does not reproduce complete copyrighted IEC standards
 * or complete manufacturer catalogues.
 *
 * It stores the engineering/product parameters needed by the
 * application for equipment selection and calculation.
 *
 * ================================================================
 */

object EngineeringCatalogRepository {

    // ================================================================
    // RECORD DEFINITIONS
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

        /**
         * Reference ampacity.
         *
         * This is catalog/table data for the defined reference
         * installation condition.
         *
         * It must NOT be confused with the final installed
         * ampacity after correction factors.
         */
        val baseAmpacityA: Double?,

        /**
         * AC resistance at reference condition.
         *
         * ohm/km
         */
        val resistanceOhmPerKm: Double?,

        /**
         * Reactance at reference configuration.
         *
         * ohm/km
         */
        val reactanceOhmPerKm: Double?,

        val referenceTemperatureC: Double?,

        /**
         * Short circuit withstand.
         *
         * kA
         */
        val shortCircuitKA: Double?,

        val shortCircuitDurationS: Double?,

        val standardCode: String?,

        val sourceUrl: String?,

        val verified: Boolean
    )

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
            id = "ELSEWEDY",
            name = "Elsewedy Electric",
            country = "Egypt",
            officialWebsite = "https://www.elsewedy.com/"
        ),

        ManufacturerRecord(
            id = "ECE",
            name = "Electro Cable Egypt",
            country = "Egypt",
            officialWebsite = "https://www.ece.com.eg/"
        ),

        ManufacturerRecord(
            id = "GIZA_CABLES",
            name = "Giza Cable Industries",
            country = "Egypt",
            officialWebsite = "https://www.gizacables.com/"
        ),

        ManufacturerRecord(
            id = "SCHNEIDER",
            name = "Schneider Electric",
            country = "France",
            officialWebsite = "https://www.se.com/"
        ),

        ManufacturerRecord(
            id = "ABB",
            name = "ABB",
            country = "Switzerland",
            officialWebsite = "https://www.abb.com/"
        ),

        ManufacturerRecord(
            id = "SIEMENS",
            name = "Siemens",
            country = "Germany",
            officialWebsite = "https://www.siemens.com/"
        ),

        ManufacturerRecord(
            id = "CHINT",
            name = "CHINT",
            country = "China",
            officialWebsite = "https://www.chintglobal.com/"
        ),

        ManufacturerRecord(
            id = "PRYSMIAN",
            name = "Prysmian",
            country = "Italy",
            officialWebsite = "https://www.prysmian.com/"
        ),

        ManufacturerRecord(
            id = "CUMMINS",
            name = "Cummins",
            country = "United States",
            officialWebsite = "https://www.cummins.com/"
        ),

        ManufacturerRecord(
            id = "CAT",
            name = "Caterpillar",
            country = "United States",
            officialWebsite = "https://www.cat.com/"
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

    // ================================================================
    // INITIALIZATION
    // ================================================================

    private var initialized = false

    /**
     * Initializes the embedded engineering database.
     *
     * This function is safe to call repeatedly.
     */
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

    /**
     * Loads all built-in engineering/product records.
     */
    private fun loadEmbeddedDatabase() {

        /*
         * ------------------------------------------------------------
         * TRANSFORMERS
         * ------------------------------------------------------------
         *
         * Standard commercial ratings.
         *
         * IMPORTANT:
         * These records are enabled only where all engineering
         * parameters required by the calculation engine are known.
         */

        addTransformerInternal(
            TransformerRecord(
                id = "GENERIC_1000KVA_11_0_4",
                manufacturerId = "ELSEWEDY",
                manufacturerName = "Elsewedy Electric",
                catalogId = "EMBEDDED_TRANSFORMER_DATABASE",
                catalogName = "Embedded Transformer Selection Database",
                catalogRevision = "2026.1",
                productFamily = "Distribution Transformer",
                partNumber = "1000kVA-11/0.4kV",
                ratedPowerKVA = 1000.0,
                primaryVoltageV = 11000.0,
                secondaryVoltageV = 400.0,
                frequencyHz = 50.0,
                vectorGroup = "Dyn11",
                impedancePercent = null,
                noLoadLossKW = null,
                loadLossKW = null,
                coolingClass = "ONAN",
                standardCode = "IEC 60076",
                sourceUrl = null,
                verified = false
            )
        )

        /*
         * The transformer record above is deliberately NOT verified.
         *
         * The program must not invent transformer impedance/loss data.
         *
         * Actual verified transformer records can be inserted here
         * when their complete manufacturer data is available.
         */

        /*
         * ------------------------------------------------------------
         * BREAKERS
         * ------------------------------------------------------------
         *
         * Only records with product-specific interrupting capacity
         * should be marked verified.
         */

        /*
         * Example structure intentionally kept disabled until
         * manufacturer-specific Icu/Ics data is verified.
         */

        /*
         * ------------------------------------------------------------
         * CABLES
         * ------------------------------------------------------------
         *
         * Cable ampacity depends on installation arrangement,
         * ambient conditions, grouping and other factors.
         *
         * Therefore the application must not fabricate an ampacity
         * and call it an IEC value.
         *
         * Verified manufacturer records are inserted through the
         * explicit functions below.
         */

        /*
         * ------------------------------------------------------------
         * GENERATORS
         * ------------------------------------------------------------
         */

        /*
         * Same policy:
         * generator ratings may be embedded only when the actual
         * product data is verified.
         */
    }

    // ================================================================
    // CABLE PROVIDER
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

    // ================================================================
    // BREAKER PROVIDER
    // ================================================================

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

    // ================================================================
    // TRANSFORMER PROVIDER
    // ================================================================

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

    // ================================================================
    // GENERATOR PROVIDER
    // ================================================================

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
    // SEARCH
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
            .filter {
                it.verified
            }
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
            .filter {
                it.verified
            }
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

    fun searchTransformers(
        manufacturerId: String? = null,
        minimumKVA: Double? = null,
        maximumKVA: Double? = null
    ): List<TransformerRecord> {

        ensureInitialized()

        return transformerRecords
            .filter {
                it.verified
            }
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
    // INSERTION
    // ================================================================

    fun addCable(
        record: CableRecord
    ): Boolean {

        ensureInitialized()

        if (!validateCableRecord(record)) {
            return false
        }

        if (
            cableRecords.any {
                it.id == record.id
            }
        ) {
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

        if (
            breakerRecords.any {
                it.id == record.id
            }
        ) {
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

        if (
            transformerRecords.any {
                it.id == record.id
            }
        ) {
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

        if (
            generatorRecords.any {
                it.id == record.id
            }
        ) {
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

        if (
            busbarRecords.any {
                it.id == record.id
            }
        ) {
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

        if (
            panelRecords.any {
                it.id == record.id
            }
        ) {
            return false
        }

        panelRecords += record

        return true
    }

    // ================================================================
    // INTERNAL INSERTION
    // ================================================================

    private fun addTransformerInternal(
        record: TransformerRecord
    ) {

        if (
            validateTransformerRecord(record) &&
            transformerRecords.none {
                it.id == record.id
            }
        ) {
            transformerRecords += record
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

        if (
            record.shortCircuitKA != null &&
            record.shortCircuitKA <= 0.0
        ) {
            return false
        }

        if (
            record.shortCircuitDurationS != null &&
            record.shortCircuitDurationS <= 0.0
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
    // CLEAR MEMORY
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
    // DATABASE STATUS
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
                cableRecords.count {
                    it.verified
                },

            verifiedBreakers =
                breakerRecords.count {
                    it.verified
                },

            verifiedTransformers =
                transformerRecords.count {
                    it.verified
                },

            verifiedGenerators =
                generatorRecords.count {
                    it.verified
                },

            verifiedBusbars =
                busbarRecords.count {
                    it.verified
                },

            verifiedPanels =
                panelRecords.count {
                    it.verified
                }
        )
    }

    // ================================================================
    // STANDARD DATABASE
    // ================================================================

    data class EngineeringStandard(
        val id: String,
        val title: String,
        val edition: String,
        val scope: String
    )

    val engineeringStandards: List<EngineeringStandard> = listOf(

        EngineeringStandard(
            id = "IEC_60364_5_52",
            title = "Low-voltage electrical installations - Wiring systems",
            edition = "IEC 60364-5-52:2009+AMD1:2024",
            scope =
                "Cable selection, installation methods, current carrying capacity and voltage drop"
        ),

        EngineeringStandard(
            id = "IEC_60947_2",
            title = "Low-voltage switchgear and controlgear - Circuit-breakers",
            edition = "IEC 60947-2:2024",
            scope =
                "LV circuit-breakers, ratings and short-circuit performance"
        ),

        EngineeringStandard(
            id = "IEC_60076",
            title = "Power Transformers",
            edition = "IEC 60076 series",
            scope =
                "Power transformer ratings, tests and performance"
        ),

        EngineeringStandard(
            id = "IEC_60034",
            title = "Rotating Electrical Machines",
            edition = "IEC 60034 series",
            scope =
                "Generator and rotating machine requirements"
        ),

        EngineeringStandard(
            id = "EGYPTIAN_ELECTRICAL_CODE",
            title = "Egyptian Code for Electrical Installations",
            edition = "Egyptian Electrical Code",
            scope =
                "Egyptian installation requirements and design practice"
        )
    )

    // ================================================================
    // STANDARDIZED ENGINEERING VALUES
    // ================================================================

    /**
     * These are engineering reference values / limits used as
     * defaults only where the user has not provided project-specific
     * requirements.
     *
     * They are NOT manufacturer catalogue data.
     */
    object DesignReference {

        const val STANDARD_FREQUENCY_HZ = 50.0

        const val STANDARD_LV_VOLTAGE_3PH_V = 400.0

        const val STANDARD_LV_PHASE_VOLTAGE_V = 230.0

        const val DEFAULT_POWER_FACTOR = 0.90

        /**
         * Default design margin is deliberately zero.
         *
         * The engineer must explicitly choose the margin.
         */
        const val DEFAULT_DESIGN_MARGIN_PERCENT = 0.0

        /**
         * Typical design target only.
         *
         * It is NOT automatically a replacement for the applicable
         * project/code requirement.
         */
        const val DEFAULT_TARGET_POWER_FACTOR = 0.95

        /**
         * The voltage-drop requirement must be confirmed against
         * the project and applicable Egyptian/IEC design requirements.
         */
        const val DEFAULT_MAX_VOLTAGE_DROP_PERCENT = 3.0

        /**
         * LV system nominal voltage.
         */
        const val LV_MAX_AC_VOLTAGE = 1000.0

        /**
         * LV circuit-breaker reference limit according to
         * IEC 60947-2 scope.
         */
        const val IEC_LV_BREAKER_MAX_AC_VOLTAGE = 1000.0

        /**
         * Common LV system frequency in Egypt.
         */
        const val EGYPT_STANDARD_FREQUENCY_HZ = 50.0
    }

    // ================================================================
    // INITIALIZATION GUARD
    // ================================================================

    private fun ensureInitialized() {

        if (!initialized) {
            initialize()
        }
    }
}
