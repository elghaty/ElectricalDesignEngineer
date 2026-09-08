package com.electricaldesignengineer.app

/**
 * Engineering Catalog Repository
 *
 * Responsibilities:
 * - Provide engineering equipment data to the calculation engine.
 * - Keep manufacturer/catalog information separate from formulas.
 * - Prevent the calculation engine from inventing missing engineering data.
 *
 * IMPORTANT:
 * This repository does NOT contain generic IEC ampacity values,
 * resistance approximations, or guessed manufacturer data.
 *
 * Real product data should be inserted from:
 * 1. Approved manufacturer catalogues.
 * 2. Project-approved technical submittals.
 * 3. Verified engineering databases.
 *
 * Every record carries source and revision information.
 */
object EngineeringCatalogRepository {

    // ============================================================
    // CABLE RECORD
    // ============================================================

    data class CableRecord(
        val id: String,

        val manufacturerId: String,
        val manufacturerName: String,

        val catalogId: String,
        val catalogName: String,
        val catalogRevision: String,

        val productFamily: String,
        val partNumber: String?,

        val material:
            CableMaterial,

        val insulation:
            InsulationType,

        val cores: Int,

        val sizeMm2: Double,

        val voltageRatingV: Int,

        val installationMethod:
            InstallationMethod,

        /**
         * Ampacity obtained from the selected engineering data source.
         *
         * It is NOT calculated here.
         */
        val baseAmpacityA: Double?,

        /**
         * AC resistance at the stated reference condition.
         *
         * Unit: ohm/km
         */
        val resistanceOhmPerKm: Double?,

        /**
         * Reactance at the stated reference configuration.
         *
         * Unit: ohm/km
         */
        val reactanceOhmPerKm: Double?,

        /**
         * Reference conductor temperature.
         */
        val referenceTemperatureC: Double?,

        /**
         * Short-circuit withstand current.
         *
         * Unit: kA
         */
        val shortCircuitKA: Double?,

        /**
         * Duration corresponding to short-circuit withstand.
         *
         * Unit: seconds
         */
        val shortCircuitDurationS: Double?,

        val standardCode: String?,

        val sourceUrl: String?,

        val verified: Boolean
    )

    // ============================================================
    // BREAKER RECORD
    // ============================================================

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

    // ============================================================
    // TRANSFORMER RECORD
    // ============================================================

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

    // ============================================================
    // GENERATOR RECORD
    // ============================================================

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

    // ============================================================
    // BUSBAR RECORD
    // ============================================================

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

    // ============================================================
    // PANEL RECORD
    // ============================================================

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

    // ============================================================
    // DATABASE COLLECTIONS
    // ============================================================

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

    // ============================================================
    // CABLE PROVIDER
    // ============================================================

    /**
     * Returns only verified cable records containing the electrical
     * parameters required by the professional calculation engine.
     */
    fun getCableData(
        material:
            CableMaterial,

        insulation:
            InsulationType,

        installationMethod:
            InstallationMethod
    ): List<ProfessionalEngineeringCore.CableData> {

        return cableRecords
            .filter {
                it.verified &&
                        it.material == material &&
                        it.insulation == insulation &&
                        it.installationMethod ==
                        installationMethod
            }
            .filter {
                it.baseAmpacityA != null &&
                        it.resistanceOhmPerKm != null &&
                        it.reactanceOhmPerKm != null
            }
            .map {
                ProfessionalEngineeringCore.CableData(
                    sizeMm2 = it.sizeMm2,
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

    // ============================================================
    // BREAKER PROVIDER
    // ============================================================

    /**
     * Returns only verified breakers with the minimum electrical
     * information required for selection.
     */
    fun getBreakerData(
        requiredPoles: Int
    ): List<ProfessionalEngineeringCore.BreakerData> {

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

    // ============================================================
    // CATALOG ACCESS
    // ============================================================

    fun allCables(): List<CableRecord> =
        cableRecords.toList()

    fun allBreakers(): List<BreakerRecord> =
        breakerRecords.toList()

    fun allTransformers(): List<TransformerRecord> =
        transformerRecords.toList()

    fun allGenerators(): List<GeneratorRecord> =
        generatorRecords.toList()

    fun allBusbars(): List<BusbarRecord> =
        busbarRecords.toList()

    fun allPanels(): List<PanelRecord> =
        panelRecords.toList()

    // ============================================================
    // SEARCH
    // ============================================================

    fun searchCables(
        manufacturerId: String? = null,
        material:
            CableMaterial? = null,
        insulation:
            InsulationType? = null,
        minimumSizeMm2: Double? = null,
        maximumSizeMm2: Double? = null
    ): List<CableRecord> {

        return cableRecords
            .filter { it.verified }
            .filter {
                manufacturerId == null ||
                        it.manufacturerId ==
                        manufacturerId
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

        return breakerRecords
            .filter { it.verified }
            .filter {
                manufacturerId == null ||
                        it.manufacturerId ==
                        manufacturerId
            }
            .filter {
                type == null ||
                        it.type == type
            }
            .filter {
                minimumCurrentA == null ||
                        it.ratedCurrentA >=
                        minimumCurrentA
            }
            .filter {
                maximumCurrentA == null ||
                        it.ratedCurrentA <=
                        maximumCurrentA
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

        return transformerRecords
            .filter { it.verified }
            .filter {
                manufacturerId == null ||
                        it.manufacturerId ==
                        manufacturerId
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

    // ============================================================
    // INSERTION
    // ============================================================

    /**
     * Adds a cable record after external validation.
     *
     * Invalid records are rejected instead of being silently accepted.
     */
    fun addCable(
        record: CableRecord
    ): Boolean {

        if (!validateCableRecord(record)) {
            return false
        }

        val existing =
            cableRecords.any {
                it.id == record.id
            }

        if (existing) {
            return false
        }

        cableRecords += record

        return true
    }

    fun addBreaker(
        record: BreakerRecord
    ): Boolean {

        if (!validateBreakerRecord(record)) {
            return false
        }

        val existing =
            breakerRecords.any {
                it.id == record.id
            }

        if (existing) {
            return false
        }

        breakerRecords += record

        return true
    }

    fun addTransformer(
        record: TransformerRecord
    ): Boolean {

        if (!validateTransformerRecord(record)) {
            return false
        }

        val existing =
            transformerRecords.any {
                it.id == record.id
            }

        if (existing) {
            return false
        }

        transformerRecords += record

        return true
    }

    fun addGenerator(
        record: GeneratorRecord
    ): Boolean {

        if (!validateGeneratorRecord(record)) {
            return false
        }

        val existing =
            generatorRecords.any {
                it.id == record.id
            }

        if (existing) {
            return false
        }

        generatorRecords += record

        return true
    }

    fun addBusbar(
        record: BusbarRecord
    ): Boolean {

        if (!validateBusbarRecord(record)) {
            return false
        }

        val existing =
            busbarRecords.any {
                it.id == record.id
            }

        if (existing) {
            return false
        }

        busbarRecords += record

        return true
    }

    fun addPanel(
        record: PanelRecord
    ): Boolean {

        if (!validatePanelRecord(record)) {
            return false
        }

        val existing =
            panelRecords.any {
                it.id == record.id
            }

        if (existing) {
            return false
        }

        panelRecords += record

        return true
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    private fun validateCableRecord(
        record: CableRecord
    ): Boolean {

        if (record.id.isBlank()) return false

        if (record.manufacturerId.isBlank()) return false

        if (record.catalogId.isBlank()) return false

        if (record.sizeMm2 <= 0.0) return false

        if (record.cores <= 0) return false

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

        if (
            record.shortTimeWithstandKA != null &&
            record.shortTimeWithstandKA <= 0.0
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

        if (
            record.shortCircuitKA != null &&
            record.shortCircuitKA <= 0.0
        ) {
            return false
        }

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

        if (
            record.shortCircuitKA != null &&
            record.shortCircuitKA <= 0.0
        ) {
            return false
        }

        return true
    }

    // ============================================================
    // REMOVE
    // ============================================================

    fun removeCable(
        id: String
    ): Boolean {
        return cableRecords.removeIf {
            it.id == id
        }
    }

    fun removeBreaker(
        id: String
    ): Boolean {
        return breakerRecords.removeIf {
            it.id == id
        }
    }

    fun removeTransformer(
        id: String
    ): Boolean {
        return transformerRecords.removeIf {
            it.id == id
        }
    }

    fun removeGenerator(
        id: String
    ): Boolean {
        return generatorRecords.removeIf {
            it.id == id
        }
    }

    fun removeBusbar(
        id: String
    ): Boolean {
        return busbarRecords.removeIf {
            it.id == id
        }
    }

    fun removePanel(
        id: String
    ): Boolean {
        return panelRecords.removeIf {
            it.id == id
        }
    }

    // ============================================================
    // CLEAR
    // ============================================================

    /**
     * Clears only the in-memory repository.
     *
     * This does NOT delete the Room database.
     */
    fun clearMemoryCache() {

        cableRecords.clear()
        breakerRecords.clear()
        transformerRecords.clear()
        generatorRecords.clear()
        busbarRecords.clear()
        panelRecords.clear()
    }

    // ============================================================
    // STATISTICS
    // ============================================================

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

        return CatalogStatistics(
            cables = cableRecords.size,
            breakers = breakerRecords.size,
            transformers = transformerRecords.size,
            generators = generatorRecords.size,
            busbars = busbarRecords.size,
            panels = panelRecords.size,

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
}
