package com.electricaldesignengineer.app

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan

object ElectricalCalculator {

    // =========================================================
    // SYSTEM
    // =========================================================

    var voltageV: Double = 400.0
    var powerFactor: Double = 0.90
    var isThreePhase: Boolean = true

    // =========================================================
    // LOAD
    // =========================================================

    var connectedKW: Double = 0.0
    var demandKW: Double = 0.0
    var totalKVA: Double = 0.0
    var designCurrentA: Double = 0.0

    // =========================================================
    // CABLE
    // =========================================================

    var cableSizeMm2: Double = 0.0
    var cableAmpacityA: Double = 0.0
    var cableLengthM: Double = 0.0
    var voltageDropV: Double = 0.0
    var voltageDropPercent: Double = 0.0

    // =========================================================
    // SHORT CIRCUIT
    // =========================================================

    var shortCircuitKA: Double = 0.0

    // =========================================================
    // BREAKER
    // =========================================================

    var breakerRatingA: Int = 0
    var breakerIcuKA: Double = 0.0

    // =========================================================
    // TRANSFORMER
    // =========================================================

    var transformerKVA: Double = 0.0
    var transformerImpedancePercent: Double = 6.0

    // =========================================================
    // GENERATOR
    // =========================================================

    var generatorKVA: Double = 0.0

    // =========================================================
    // CAPACITOR BANK
    // =========================================================

    var capacitorKVAR: Double = 0.0

    // =========================================================
    // CABLE DATA
    // Preliminary ampacity table
    // =========================================================

    data class Cable(
        val sizeMm2: Double,
        val ampacityA: Double
    )

    val cables = listOf(
        Cable(1.0, 14.0),
        Cable(1.5, 18.0),
        Cable(2.5, 24.0),
        Cable(4.0, 32.0),
        Cable(6.0, 41.0),
        Cable(10.0, 57.0),
        Cable(16.0, 76.0),
        Cable(25.0, 101.0),
        Cable(35.0, 125.0),
        Cable(50.0, 150.0),
        Cable(70.0, 192.0),
        Cable(95.0, 232.0),
        Cable(120.0, 269.0),
        Cable(150.0, 309.0),
        Cable(185.0, 353.0),
        Cable(240.0, 415.0),
        Cable(300.0, 473.0),
        Cable(400.0, 557.0)
    )

    val cableTable = cables

    // =========================================================
    // BREAKER DATA
    // =========================================================

    val breakerRatings = listOf(
        6,
        10,
        16,
        20,
        25,
        32,
        40,
        50,
        63,
        80,
        100,
        125,
        160,
        200,
        250,
        315,
        400,
        500,
        630,
        800,
        1000,
        1250,
        1600,
        2000,
        2500,
        3200,
        4000
    )

    val breakerIcuRatings = listOf(
        6.0,
        10.0,
        15.0,
        25.0,
        36.0,
        50.0,
        65.0,
        100.0
    )

    // =========================================================
    // TRANSFORMER DATA
    // =========================================================

    val transformerRatings = listOf(
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

    // =========================================================
    // GENERATOR DATA
    // =========================================================

    val generatorRatings = listOf(
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

    // =========================================================
    // RESULT CLASSES
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

        val v = if (voltage > 0.0) voltage else 400.0
        val safePF = pf.coerceIn(0.01, 1.0)

        connectedKW = loads.sumOf {
            max(0.0, it.quantity) *
                    max(0.0, it.powerKW)
        }

        demandKW = loads.sumOf {
            max(0.0, it.quantity) *
                    max(0.0, it.powerKW) *
                    it.demandFactor.coerceIn(0.0, 1.0)
        }

        totalKVA = demandKW / safePF

        designCurrentA =
            if (threePhase) {
                totalKVA * 1000.0 /
                        (sqrt(3.0) * v)
            } else {
                totalKVA * 1000.0 / v
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
    // IMPORTANT:
    // There is ONLY ONE selectCable function.
    //
    // Existing screens use:
    // requestedCableSizeMm2
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

        val selectedCable =
            if (requestedCableSizeMm2 != null) {

                cables.firstOrNull {
                    abs(
                        it.sizeMm2 -
                                requestedCableSizeMm2
                    ) < 0.0001
                }

            } else {

                cables.firstOrNull {
                    it.ampacityA >= currentA
                }
            }

        if (selectedCable == null) {
            return CableResult(
                success = false,
                sizeMm2 = 0.0,
                ampacityA = 0.0,
                voltageDropV = 0.0,
                voltageDropPercent = 0.0,
                status = "NO SUITABLE CABLE"
            )
        }

        val safePF = pf.coerceIn(0.01, 1.0)

        val sinPhi = sqrt(
            max(
                0.0,
                1.0 - safePF.pow(2.0)
            )
        )

        // Preliminary resistance model.
        // To be replaced later by IEC/code based tables.

        val resistance =
            18.1 / selectedCable.sizeMm2

        val reactance =
            0.08

        val voltageDropComponent =
            resistance * safePF +
                    reactance * sinPhi

        val dropV =
            if (threePhase) {

                sqrt(3.0) *
                        currentA *
                        lengthM *
                        voltageDropComponent /
                        1000.0

            } else {

                2.0 *
                        currentA *
                        lengthM *
                        voltageDropComponent /
                        1000.0
            }

        val dropPercent =
            dropV /
                    voltage *
                    100.0

        val ampacityPass =
            selectedCable.ampacityA >= currentA

        val voltageDropPass =
            dropPercent <= 3.0

        val success =
            ampacityPass &&
                    voltageDropPass

        // Save selected result

        cableSizeMm2 =
            selectedCable.sizeMm2

        cableAmpacityA =
            selectedCable.ampacityA

        cableLengthM =
            lengthM

        voltageDropV =
            dropV

        voltageDropPercent =
            dropPercent

        val status =
            when {

                success ->
                    "CABLE SELECTION PASS"

                !ampacityPass ->
                    "FAIL: CABLE AMPACITY BELOW DESIGN CURRENT"

                !voltageDropPass ->
                    "FAIL: VOLTAGE DROP EXCEEDS 3%"

                else ->
                    "CABLE SELECTION FAIL"
            }

        return CableResult(
            success = success,
            sizeMm2 = selectedCable.sizeMm2,
            ampacityA = selectedCable.ampacityA,
            voltageDropV = dropV,
            voltageDropPercent = dropPercent,
            status = status
        )
    }

    // =========================================================
    // TRANSFORMER SHORT CIRCUIT
    // =========================================================

    fun transformerShortCircuit(
        transformerKVA: Double,
        voltage: Double,
        impedancePercent: Double
    ): Double {

        if (
            transformerKVA <= 0.0 ||
            voltage <= 0.0 ||
            impedancePercent <= 0.0
        ) {
            return 0.0
        }

        val ratedCurrent =
            threePhaseCurrent(
                transformerKVA,
                voltage
            )

        val faultCurrent =
            ratedCurrent /
                    (impedancePercent / 100.0)

        val faultKA =
            faultCurrent / 1000.0

        shortCircuitKA =
            faultKA

        return faultKA
    }

    // =========================================================
    // BREAKER AUTO SELECTION
    //
    // Ib <= In <= Iz
    // Icu >= Icc
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

        if (currentA > cableAmpacityA) {
            return BreakerResult(
                success = false,
                ratingA = 0,
                icuKA = 0.0,
                status =
                    "FAIL: DESIGN CURRENT EXCEEDS CABLE AMPACITY"
            )
        }

        val rating =
            breakerRatings.firstOrNull {
                it >= currentA &&
                        it <= cableAmpacityA
            }

        if (rating == null) {
            return BreakerResult(
                success = false,
                ratingA = 0,
                icuKA = 0.0,
                status =
                    "NO BREAKER SATISFIES Ib <= In <= Iz"
            )
        }

        val icu =
            breakerIcuRatings.firstOrNull {
                it >= faultCurrentKA
            }

        if (icu == null) {
            return BreakerResult(
                success = false,
                ratingA = rating,
                icuKA = 0.0,
                status =
                    "FAULT CURRENT EXCEEDS BREAKER Icu DATA"
            )
        }

        breakerRatingA =
            rating

        breakerIcuKA =
            icu

        return BreakerResult(
            success = true,
            ratingA = rating,
            icuKA = icu,
            status = "AUTO SELECTED - PASS"
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

        val standardRating =
            breakerRatings.contains(
                breakerRatingA
            )

        val standardIcu =
            breakerIcuRatings.any {
                abs(it - breakerIcuKA) < 0.0001
            }

        val ibIn =
            breakerRatingA >= currentA

        val inIz =
            breakerRatingA <= cableAmpacityA

        val icuIcc =
            breakerIcuKA >= faultCurrentKA

        val success =
            standardRating &&
                    standardIcu &&
                    ibIn &&
                    inIz &&
                    icuIcc

        val status =
            when {

                success ->
                    "PASS - BREAKER FULL PROTECTION CHECK"

                !standardRating ->
                    "FAIL: NON-STANDARD BREAKER RATING"

                !standardIcu ->
                    "FAIL: NON-STANDARD ICU"

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
            this.breakerRatingA =
                breakerRatingA

            this.breakerIcuKA =
                breakerIcuKA
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

        if (
            demandKW <= 0.0 ||
            pf <= 0.0
        ) {
            return 0.0
        }

        val safePF =
            pf.coerceIn(0.01, 1.0)

        val margin =
            max(0.0, marginPercent)

        val requiredKVA =
            demandKW /
                    safePF *
                    (1.0 + margin / 100.0)

        val selected =
            transformerRatings.firstOrNull {
                it >= requiredKVA
            }
                ?: return 0.0

        transformerKVA =
            selected

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

        if (
            demandKW <= 0.0 ||
            pf <= 0.0
        ) {
            return 0.0
        }

        val safePF =
            pf.coerceIn(0.01, 1.0)

        val loading =
            loadingPercent.coerceIn(
                1.0,
                100.0
            )

        val motorAllowance =
            max(
                0.0,
                motorAllowancePercent
            )

        val baseKVA =
            demandKW /
                    safePF

        val motorAdjustedKVA =
            baseKVA *
                    (1.0 +
                            motorAllowance / 100.0)

        val requiredGeneratorKVA =
            motorAdjustedKVA /
                    (loading / 100.0)

        val selected =
            generatorRatings.firstOrNull {
                it >= requiredGeneratorKVA
            }
                ?: return 0.0

        generatorKVA =
            selected

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

        val phi1 =
            acos(existingPF)

        val phi2 =
            acos(targetPF)

        val q1 =
            activePowerKW *
                    tan(phi1)

        val q2 =
            activePowerKW *
                    tan(phi2)

        val required =
            max(
                0.0,
                q1 - q2
            )

        capacitorKVAR =
            required

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
                earthResistanceOhm =
                    earthResistanceOhm,
                faultCurrentA =
                    faultCurrentA,
                earthPotentialRiseV =
                    0.0,
                maximumResistanceOhm =
                    0.0,
                status =
                    "INVALID INPUT"
            )
        }

        val earthPotentialRise =
            earthResistanceOhm *
                    faultCurrentA

        val maximumResistance =
            permissibleTouchVoltageV /
                    faultCurrentA

        val pass =
            earthResistanceOhm <=
                    maximumResistance

        val status =
            if (pass) {
                "EARTHING CHECK PASS"
            } else {
                "EARTHING RESISTANCE TOO HIGH"
            }

        return EarthingResult(
            success = pass,
            earthResistanceOhm =
                earthResistanceOhm,
            faultCurrentA =
                faultCurrentA,
            earthPotentialRiseV =
                earthPotentialRise,
            maximumResistanceOhm =
                maximumResistance,
            status =
                status
        )
    }

    // =========================================================
    // RESET
    // =========================================================

    fun reset() {

        voltageV =
            400.0

        powerFactor =
            0.90

        isThreePhase =
            true

        connectedKW =
            0.0

        demandKW =
            0.0

        totalKVA =
            0.0

        designCurrentA =
            0.0

        cableSizeMm2 =
            0.0

        cableAmpacityA =
            0.0

        cableLengthM =
            0.0

        voltageDropV =
            0.0

        voltageDropPercent =
            0.0

        shortCircuitKA =
            0.0

        breakerRatingA =
            0

        breakerIcuKA =
            0.0

        transformerKVA =
            0.0

        transformerImpedancePercent =
            6.0

        generatorKVA =
            0.0

        capacitorKVAR =
            0.0
    }
}
