package com.electricaldesignengineer.app

import kotlin.math.acos
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Compatibility facade for legacy screens.
 *
 * IMPORTANT:
 * Engineering calculations must be performed by:
 *
 * UI
 *   -> AutoDesignService
 *       -> ProfessionalEngineeringCore
 *           -> EngineeringCatalogRepository
 *
 * This object is kept only to preserve compatibility with older
 * screens and ProjectManager until the remaining legacy calls are migrated.
 *
 * No generic hardcoded cable or breaker engineering data is used here.
 */
object ElectricalCalculator {

    // =========================================================
    // SYSTEM STATE
    // =========================================================

    var voltageV: Double = 400.0
    var powerFactor: Double = 0.90
    var isThreePhase: Boolean = true

    // =========================================================
    // LOAD STATE
    // =========================================================

    var connectedKW: Double = 0.0
    var demandKW: Double = 0.0
    var totalKVA: Double = 0.0
    var designCurrentA: Double = 0.0

    // =========================================================
    // CABLE STATE
    // =========================================================

    var cableSizeMm2: Double = 0.0
    var cableAmpacityA: Double = 0.0
    var cableLengthM: Double = 0.0
    var voltageDropV: Double = 0.0
    var voltageDropPercent: Double = 0.0

    // =========================================================
    // SHORT CIRCUIT STATE
    // =========================================================

    var shortCircuitKA: Double = 0.0

    // =========================================================
    // BREAKER STATE
    // =========================================================

    var breakerRatingA: Int = 0
    var breakerIcuKA: Double = 0.0

    // =========================================================
    // TRANSFORMER STATE
    // =========================================================

    var transformerKVA: Double = 0.0

    /**
     * Kept for UI/backward compatibility only.
     *
     * DO NOT use this value to invent a transformer short-circuit
     * calculation. The professional short-circuit calculation requires
     * appropriate source impedance data.
     */
    var transformerImpedancePercent: Double = 0.0

    // =========================================================
    // GENERATOR STATE
    // =========================================================

    var generatorKVA: Double = 0.0

    // =========================================================
    // CAPACITOR BANK STATE
    // =========================================================

    var capacitorKVAR: Double = 0.0

    // =========================================================
    // LEGACY RESULT CLASSES
    // =========================================================

    data class LoadResult(
        val connectedKW: Double,
        val demandKW: Double,
        val totalKVA: Double,
        val currentA: Double
    )

    data class CableResult(
        val success: Boolean,
        val sizeMm2: Double,
        val ampacityA: Double,
        val voltageDropV: Double,
        val voltageDropPercent: Double,
        val status: String
    )

    data class BreakerResult(
        val success: Boolean,
        val ratingA: Int,
        val icuKA: Double,
        val status: String
    )

    data class EarthingResult(
        val success: Boolean,
        val earthResistanceOhm: Double,
        val faultCurrentA: Double,
        val earthPotentialRiseV: Double,
        val maximumResistanceOhm: Double,
        val status: String
    )

    // =========================================================
    // LOAD CALCULATION
    // =========================================================

    fun calculateLoad(
        loads: List<LoadItem>,
        voltage: Double = voltageV,
        pf: Double = powerFactor,
        threePhase: Boolean = isThreePhase
    ): LoadResult {

        if (loads.isEmpty()) {
            connectedKW = 0.0
            demandKW = 0.0
            totalKVA = 0.0
            designCurrentA = 0.0

            return LoadResult(
                connectedKW = 0.0,
                demandKW = 0.0,
                totalKVA = 0.0,
                currentA = 0.0
            )
        }

        val v = voltage.takeIf { it > 0.0 } ?: 400.0
        val safePF = pf.coerceIn(0.01, 1.0)

        connectedKW = loads.sumOf { load ->
            max(0.0, load.quantity.toDouble()) *
                max(0.0, load.powerKW)
        }

        demandKW = loads.sumOf { load ->
            max(0.0, load.quantity.toDouble()) *
                max(0.0, load.powerKW) *
                load.demandFactor.coerceIn(0.0, 1.0)
        }

        totalKVA = demandKW / safePF

        designCurrentA =
            if (threePhase) {
                threePhaseCurrent(
                    kva = totalKVA,
                    voltage = v
                )
            } else {
                singlePhaseCurrent(
                    kva = totalKVA,
                    voltage = v
                )
            }

        voltageV = v
        powerFactor = safePF
        isThreePhase = threePhase

        return LoadResult(
            connectedKW = connectedKW,
            demandKW = demandKW,
            totalKVA = totalKVA,
            currentA = designCurrentA
        )
    }

    // =========================================================
    // CURRENT CALCULATIONS
    // =========================================================

    fun threePhaseCurrent(
        kva: Double,
        voltage: Double
    ): Double {

        if (kva <= 0.0 || voltage <= 0.0) {
            return 0.0
        }

        return kva * 1000.0 /
            (sqrt(3.0) * voltage)
    }

    fun singlePhaseCurrent(
        kva: Double,
        voltage: Double
    ): Double {

        if (kva <= 0.0 || voltage <= 0.0) {
            return 0.0
        }

        return kva * 1000.0 / voltage
    }

    // =========================================================
    // CABLE SELECTION
    //
    // LEGACY COMPATIBILITY ONLY
    //
    // The real cable selection is now performed by:
    //
    // ProfessionalEngineeringCore.designCable(...)
    //
    // using EngineeringCatalogRepository.
    //
    // This method intentionally does NOT contain a hardcoded
    // cable table.
    // =========================================================

    fun selectCable(
        currentA: Double,
        lengthM: Double,
        voltage: Double,
        pf: Double = powerFactor,
        threePhase: Boolean = true,
        requestedCableSizeMm2: Double? = null
    ): CableResult {

        if (
            currentA <= 0.0 ||
            lengthM <= 0.0 ||
            voltage <= 0.0
        ) {
            return CableResult(
                success = false,
                sizeMm2 = 0.0,
                ampacityA = 0.0,
                voltageDropV = 0.0,
                voltageDropPercent = 0.0,
                status = "INVALID INPUT"
            )
        }

        val material = CableMaterial.COPPER
        val insulation = InsulationType.XLPE
        val installationMethod = InstallationMethod.CABLE_TRAY

        val catalog =
            EngineeringCatalogRepository.getCableData(
                material = material,
                insulation = insulation,
                installationMethod = installationMethod
            )

        if (catalog.isEmpty()) {
            return CableResult(
                success = false,
                sizeMm2 = requestedCableSizeMm2 ?: 0.0,
                ampacityA = 0.0,
                voltageDropV = 0.0,
                voltageDropPercent = 0.0,
                status = "NO VERIFIED CABLE DATA AVAILABLE"
            )
        }

        val available =
            if (requestedCableSizeMm2 != null) {
                catalog.filter {
                    kotlin.math.abs(
                        it.sizeMm2 - requestedCableSizeMm2
                    ) < 0.0001
                }
            } else {
                catalog
            }.sortedBy { it.sizeMm2 }

        if (available.isEmpty()) {
            return CableResult(
                success = false,
                sizeMm2 = requestedCableSizeMm2 ?: 0.0,
                ampacityA = 0.0,
                voltageDropV = 0.0,
                voltageDropPercent = 0.0,
                status = "REQUESTED CABLE SIZE NOT FOUND IN VERIFIED CATALOG"
            )
        }

        val selected =
            if (requestedCableSizeMm2 != null) {
                available.first()
            } else {
                available.firstOrNull {
                    it.baseAmpacityA >= currentA
                }
            }

        if (selected == null) {
            return CableResult(
                success = false,
                sizeMm2 = 0.0,
                ampacityA = 0.0,
                voltageDropV = 0.0,
                voltageDropPercent = 0.0,
                status = "NO SUITABLE VERIFIED CABLE"
            )
        }

        val safePF = pf.coerceIn(0.01, 1.0)

        val phaseSystem =
            if (threePhase) {
                ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE
            } else {
                ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
            }

        val input =
            ProfessionalEngineeringCore.CableDesignInput(
                designCurrentA = currentA,
                lengthM = lengthM,
                voltageV = voltage,
                powerFactor = safePF,
                phaseSystem = phaseSystem,
                conductorMaterial = material,
                insulation = insulation,
                installationMethod = installationMethod,
                numberOfLoadedConductors =
                    if (threePhase) 3 else 2,
                ambientTemperatureC = 30.0,
                groupingFactor = 1.0,
                thermalInsulationFactor = 1.0,
                soilCorrectionFactor = 1.0,
                maximumVoltageDropPercent = 5.0,
                maximumParallelRuns = 8
            )

        val provider =
            object : ProfessionalEngineeringCore.CableDataProvider {

                override fun availableCables(
                    material: CableMaterial,
                    insulation: InsulationType,
                    installationMethod: InstallationMethod
                ): List<ProfessionalEngineeringCore.CableData> {
                    return listOf(selected)
                }
            }

        val result =
            ProfessionalEngineeringCore.designCable(
                input = input,
                provider = provider
            )

        val cable = result.selectedCable

        if (cable == null) {
            return CableResult(
                success = false,
                sizeMm2 = selected.sizeMm2,
                ampacityA = selected.baseAmpacityA,
                voltageDropV = result.voltageDropV,
                voltageDropPercent = result.voltageDropPercent,
                status =
                    result.trace.warnings.firstOrNull()
                        ?: "CABLE DESIGN FAILED"
            )
        }

        val success =
            result.status == EngineeringStatus.PASS

        cableSizeMm2 = cable.sizeMm2
        cableAmpacityA = result.totalAmpacityA
        cableLengthM = lengthM
        voltageDropV = result.voltageDropV
        voltageDropPercent = result.voltageDropPercent

        return CableResult(
            success = success,
            sizeMm2 = cable.sizeMm2,
            ampacityA = result.totalAmpacityA,
            voltageDropV = result.voltageDropV,
            voltageDropPercent = result.voltageDropPercent,
            status =
                when (result.status) {
                    EngineeringStatus.PASS ->
                        "CABLE SELECTION PASS"

                    EngineeringStatus.WARNING ->
                        "CABLE SELECTION WARNING"

                    EngineeringStatus.DATA_REQUIRED ->
                        "CABLE DATA REQUIRED"

                    EngineeringStatus.FAIL ->
                        "CABLE SELECTION FAIL"

                    else ->
                        "CABLE NOT CALCULATED"
                }
        )
    }

    // =========================================================
    // TRANSFORMER SHORT CIRCUIT
    //
    // DEPRECATED
    //
    // A transformer %Z value alone is not sufficient for the
    // downstream network model used by the professional core.
    //
    // Therefore this compatibility function no longer fabricates
    // a short-circuit result.
    // =========================================================

    @Deprecated(
        message =
            "Do not calculate downstream short-circuit from transformer %Z alone. " +
            "Use ProfessionalEngineeringCore.calculateShortCircuit with complete source data."
    )
    fun transformerShortCircuit(
        transformerKVA: Double,
        voltage: Double,
        impedancePercent: Double
    ): Double {

        return 0.0
    }

    // =========================================================
    // BREAKER AUTO SELECTION
    //
    // LEGACY COMPATIBILITY ONLY
    //
    // Real breaker selection belongs to ProfessionalEngineeringCore.
    // =========================================================

    fun selectBreaker(
        currentA: Double,
        faultCurrentKA: Double,
        cableAmpacityA: Double = 0.0
    ): BreakerResult {

        if (
            currentA <= 0.0 ||
            faultCurrentKA <= 0.0
        ) {
            return BreakerResult(
                success = false,
                ratingA = 0,
                icuKA = 0.0,
                status = "INVALID INPUT"
            )
        }

        if (cableAmpacityA <= 0.0) {
            return BreakerResult(
                success = false,
                ratingA = 0,
                icuKA = 0.0,
                status = "CABLE NOT SELECTED"
            )
        }

        val provider =
            object : ProfessionalEngineeringCore.BreakerDataProvider {

                override fun availableBreakers(
                    requiredPoles: Int
                ): List<ProfessionalEngineeringCore.BreakerData> {
                    return EngineeringCatalogRepository
                        .getBreakerData(requiredPoles)
                }
            }

        val poles =
            if (isThreePhase) 4 else 2

        val result =
            ProfessionalEngineeringCore.designBreaker(
                input =
                    ProfessionalEngineeringCore.BreakerDesignInput(
                        designCurrentA = currentA,
                        cableAmpacityA = cableAmpacityA,
                        prospectiveShortCircuitKA = faultCurrentKA,
                        requiredPoles = poles
                    ),
                provider = provider
            )

        val breaker = result.selectedBreaker

        if (breaker == null) {
            return BreakerResult(
                success = false,
                ratingA = 0,
                icuKA = 0.0,
                status =
                    result.trace.warnings.firstOrNull()
                        ?: "NO SUITABLE VERIFIED BREAKER"
            )
        }

        val success =
            result.status == EngineeringStatus.PASS

        if (success) {
            breakerRatingA = breaker.ratedCurrentA.toInt()
            breakerIcuKA = breaker.icuKA
        }

        return BreakerResult(
            success = success,
            ratingA = breaker.ratedCurrentA.toInt(),
            icuKA = breaker.icuKA,
            status =
                when (result.status) {
                    EngineeringStatus.PASS ->
                        "AUTO SELECTED - PASS"

                    EngineeringStatus.WARNING ->
                        "BREAKER SELECTION WARNING"

                    EngineeringStatus.DATA_REQUIRED ->
                        "BREAKER DATA REQUIRED"

                    EngineeringStatus.FAIL ->
                        "BREAKER SELECTION FAIL"

                    else ->
                        "BREAKER NOT CALCULATED"
                }
        )
    }

    // =========================================================
    // BREAKER VALIDATION
    // =========================================================

    fun validateBreaker(
        currentA: Double,
        faultCurrentKA: Double,
        breakerRatingA: Int,
        breakerIcuKA: Double,
        cableAmpacityA: Double = 0.0
    ): BreakerResult {

        if (
            currentA <= 0.0 ||
            faultCurrentKA <= 0.0 ||
            breakerRatingA <= 0 ||
            breakerIcuKA <= 0.0
        ) {
            return BreakerResult(
                success = false,
                ratingA = breakerRatingA,
                icuKA = breakerIcuKA,
                status = "INVALID INPUT"
            )
        }

        if (cableAmpacityA <= 0.0) {
            return BreakerResult(
                success = false,
                ratingA = breakerRatingA,
                icuKA = breakerIcuKA,
                status = "CABLE NOT SELECTED"
            )
        }

        val ibIn =
            breakerRatingA.toDouble() >= currentA

        val inIz =
            breakerRatingA.toDouble() <= cableAmpacityA

        val icuIcc =
            breakerIcuKA >= faultCurrentKA

        val success =
            ibIn &&
                inIz &&
                icuIcc

        val status =
            when {
                success ->
                    "PASS - BREAKER PROTECTION CHECK"

                !ibIn ->
                    "FAIL: BREAKER RATING BELOW DESIGN CURRENT"

                !inIz ->
                    "FAIL: BREAKER RATING EXCEEDS CABLE AMPACITY"

                !icuIcc ->
                    "FAIL: BREAKER Icu BELOW SHORT CIRCUIT CURRENT"

                else ->
                    "FAIL: PROTECTION CHECK"
            }

        if (success) {
            this.breakerRatingA = breakerRatingA
            this.breakerIcuKA = breakerIcuKA
        }

        return BreakerResult(
            success = success,
            ratingA = breakerRatingA,
            icuKA = breakerIcuKA,
            status = status
        )
    }

    // =========================================================
    // TRANSFORMER SELECTION
    // =========================================================

    fun selectTransformer(
        demandKW: Double,
        pf: Double,
        marginPercent: Double
    ): Double {

        if (demandKW <= 0.0 || pf <= 0.0) {
            return 0.0
        }

        val safePF = pf.coerceIn(0.01, 1.0)
        val margin = max(0.0, marginPercent)

        val requiredKVA =
            demandKW /
                safePF *
                (1.0 + margin / 100.0)

        // Standard transformer sizes are project/catalog data,
        // not an engineering calculation assumption.
        val transformerRatings =
            listOf(
                50.0,
                100.0,
                160.0,
                250.0,
                315.0,
                400.0,
                500.0,
                630.0,
                800.0,
                1000.0,
                1250.0,
                1600.0,
                2000.0,
                2500.0,
                3150.0,
                4000.0
            )

        val selected =
            transformerRatings.firstOrNull {
                it >= requiredKVA
            } ?: return 0.0

        transformerKVA = selected

        return selected
    }

    // =========================================================
    // GENERATOR SELECTION
    // =========================================================

    fun selectGenerator(
        demandKW: Double,
        pf: Double,
        loadingPercent: Double,
        motorAllowancePercent: Double
    ): Double {

        if (demandKW <= 0.0 || pf <= 0.0) {
            return 0.0
        }

        val safePF = pf.coerceIn(0.01, 1.0)

        val loading =
            loadingPercent.coerceIn(1.0, 100.0)

        val motorAllowance =
            max(0.0, motorAllowancePercent)

        val baseKVA =
            demandKW / safePF

        val motorAdjustedKVA =
            baseKVA *
                (1.0 + motorAllowance / 100.0)

        val requiredGeneratorKVA =
            motorAdjustedKVA /
                (loading / 100.0)

        val generatorRatings =
            listOf(
                10.0,
                15.0,
                20.0,
                30.0,
                40.0,
                50.0,
                60.0,
                75.0,
                100.0,
                125.0,
                150.0,
                200.0,
                250.0,
                300.0,
                400.0,
                500.0,
                625.0,
                750.0,
                1000.0,
                1250.0,
                1500.0,
                2000.0
            )

        val selected =
            generatorRatings.firstOrNull {
                it >= requiredGeneratorKVA
            } ?: return 0.0

        generatorKVA = selected

        return selected
    }

    // =========================================================
    // POWER FACTOR CORRECTION
    // =========================================================

    fun capacitorBank(
        activePowerKW: Double,
        existingPF: Double,
        targetPF: Double
    ): Double {

        if (
            activePowerKW <= 0.0 ||
            existingPF <= 0.0 ||
            targetPF <= 0.0 ||
            existingPF > 1.0 ||
            targetPF > 1.0
        ) {
            capacitorKVAR = 0.0
            return 0.0
        }

        if (targetPF <= existingPF) {
            capacitorKVAR = 0.0
            return 0.0
        }

        val phi1 = acos(existingPF)
        val phi2 = acos(targetPF)

        val q1 =
            activePowerKW * tan(phi1)

        val q2 =
            activePowerKW * tan(phi2)

        val required =
            max(0.0, q1 - q2)

        capacitorKVAR = required

        return required
    }

    fun calculateCapacitorBank(
        activePowerKW: Double,
        initialPF: Double,
        targetPF: Double
    ): Double {

        return capacitorBank(
            activePowerKW = activePowerKW,
            existingPF = initialPF,
            targetPF = targetPF
        )
    }

    // =========================================================
    // EARTHING CHECK
    // =========================================================

    fun earthCheck(
        earthResistanceOhm: Double,
        faultCurrentA: Double,
        permissibleTouchVoltageV: Double
    ): EarthingResult {

        if (
            earthResistanceOhm <= 0.0 ||
            faultCurrentA <= 0.0 ||
            permissibleTouchVoltageV <= 0.0
        ) {
            return EarthingResult(
                success = false,
                earthResistanceOhm = earthResistanceOhm,
                faultCurrentA = faultCurrentA,
                earthPotentialRiseV = 0.0,
                maximumResistanceOhm = 0.0,
                status = "INVALID INPUT"
            )
        }

        val earthPotentialRise =
            earthResistanceOhm * faultCurrentA

        val maximumResistance =
            permissibleTouchVoltageV /
                faultCurrentA

        val pass =
            earthResistanceOhm <=
                maximumResistance

        return EarthingResult(
            success = pass,
            earthResistanceOhm = earthResistanceOhm,
            faultCurrentA = faultCurrentA,
            earthPotentialRiseV = earthPotentialRise,
            maximumResistanceOhm = maximumResistance,
            status =
                if (pass) {
                    "EARTHING CHECK PASS"
                } else {
                    "EARTHING RESISTANCE TOO HIGH"
                }
        )
    }

    // =========================================================
    // RESET
    // =========================================================

    fun reset() {

        voltageV = 400.0
        powerFactor = 0.90
        isThreePhase = true

        connectedKW = 0.0
        demandKW = 0.0
        totalKVA = 0.0
        designCurrentA = 0.0

        cableSizeMm2 = 0.0
        cableAmpacityA = 0.0
        cableLengthM = 0.0
        voltageDropV = 0.0
        voltageDropPercent = 0.0

        shortCircuitKA = 0.0

        breakerRatingA = 0
        breakerIcuKA = 0.0

        transformerKVA = 0.0
        transformerImpedancePercent = 0.0

        generatorKVA = 0.0
        capacitorKVAR = 0.0
    }
}
