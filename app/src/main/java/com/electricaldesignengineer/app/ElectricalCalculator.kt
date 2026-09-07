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
    // CABLE MODEL
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
    // BREAKER RATINGS
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
    // TRANSFORMER RATINGS
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
    // GENERATOR RATINGS
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
    // RESULT TYPES
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
                0.0,
                0.0,
                0.0,
                0.0
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
            connectedKW,
            demandKW,
            totalKVA,
            designCurrentA
        )
    }

    // =========================================================
    // CURRENT
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
    // Compatible with VoltageDropScreen
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
                false,
                0.0,
                0.0,
                0.0,
                0.0,
                "INVALID INPUT"
            )
        }

        val selected =
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

        if (selected == null) {
            return CableResult(
                false,
                0.0,
                0.0,
                0.0,
                0.0,
                "NO SUITABLE CABLE"
            )
        }

        val safePF = pf.coerceIn(0.01, 1.0)

        val sinPhi =
            sqrt(
                max(
                    0.0,
                    1.0 - safePF.pow(2.0)
                )
            )

        /*
         * Preliminary resistance model.
         * Final design will later include:
         * conductor material,
         * installation method,
         * temperature,
         * grouping,
         * insulation,
         * correction factors.
         */
        val resistance =
            18.1 / selected.sizeMm2

        val reactance = 0.08

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
            dropV / voltage * 100.0

        val ampacityPass =
            selected.ampacityA >= currentA

        val voltageDropPass =
            dropPercent <= 3.0

        val success =
            ampacityPass &&
                    voltageDropPass

        cableSizeMm2 = selected.sizeMm2
        cableAmpacityA = selected.ampacityA
        cableLengthM = lengthM
        voltageDropV = dropV
        voltageDropPercent = dropPercent

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
            success,
            selected.sizeMm2,
            selected.ampacityA,
            dropV,
            dropPercent,
            status
        )
    }

    // Backward-compatible overload
    fun selectCable(
        currentA: Double,
        lengthM: Double,
        voltage: Double,
        pf: Double = powerFactor,
        threePhase: Boolean = true,
        manualSizeMm2: Double? = null
    ): CableResult {

        return selectCable(
            currentA = currentA,
            lengthM = lengthM,
            voltage = voltage,
            pf = pf,
            threePhase = threePhase,
            requestedCableSizeMm2 = manualSizeMm2
        )
    }

    // =========================================================
    // SHORT CIRCUIT
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

        return faultCurrent / 1000.0
    }

    // =========================================================
    // BREAKER AUTO SELECTION
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
                false,
                0,
                0.0,
                "INVALID INPUT"
            )
        }

        if (cableAmpacityA <= 0.0) {
            return BreakerResult(
                false,
                0,
                0.0,
                "CABLE NOT SELECTED"
            )
        }

        if (currentA > cableAmpacityA) {
            return BreakerResult(
                false,
                0,
                0.0,
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
                false,
                0,
                0.0,
                "NO BREAKER SATISFIES Ib <= In <= Iz"
            )
        }

        val icu =
            breakerIcuRatings.firstOrNull {
                it >= faultCurrentKA
            }

        if (icu == null) {
            return BreakerResult(
                false,
                rating,
                0.0,
                "FAULT CURRENT EXCEEDS BREAKER Icu DATA"
            )
        }

        breakerRatingA = rating
        breakerIcuKA = icu

        return BreakerResult(
            true,
            rating,
            icu,
            "AUTO SELECTED - PASS"
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
                false,
                breakerRatingA,
                breakerIcuKA,
                "INVALID INPUT"
            )
        }

        if (cableAmpacityA <= 0.0) {
            return BreakerResult(
                false,
                breakerRatingA,
                breakerIcuKA,
                "CABLE NOT SELECTED"
            )
        }

        val standardRating =
            breakerRatings.contains(
                breakerRatingA
            )

        val standardIcu =
            breakerIcuRatings.any {
                abs(
                    it - breakerIcuKA
                ) < 0.0001
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
            success,
            breakerRatingA,
            breakerIcuKA,
            status
        )
    }

    // =========================================================
    // TRANSFORMER SIZING
    // Compatible with TransformerSizingScreen
    //
    // Returns Double because ProjectManager expects kVA.
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
            } ?: return 0.0

        transformerKVA = selected

        return selected
    }

    // =========================================================
    // GENERATOR SIZING
    // Compatible with GeneratorSizingScreen
    //
    // Returns Double because ProjectManager expects kVA.
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
            demandKW / safePF

        val motorAdjustedKVA =
            baseKVA *
                    (1.0 + motorAllowance / 100.0)

        val requiredGeneratorKVA =
            motorAdjustedKVA /
                    (loading / 100.0)

        val selected =
            generatorRatings.firstOrNull {
                it >= requiredGeneratorKVA
            } ?: return 0.0

        generatorKVA = selected

        return selected
    }

    // =========================================================
    // POWER FACTOR CORRECTION
    // Compatible with PowerFactorCorrectionScreen
    //
    // Returns kVAR.
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

        capacitorKVAR = required

        return required
    }

    // New descriptive alias
    fun calculateCapacitorBank(
        activePowerKW: Double,
        initialPF: Double,
        targetPF: Double
    ): Double {

        return capacitorBank(
            activePowerKW,
            initialPF,
            targetPF
        )
    }

    // =========================================================
    // EARTHING
    // Compatible with EarthingScreen
    //
    // Input:
    // earth resistance
    // fault current
    // permissible touch voltage
    //
    // EPR = If x R
    // Rmax = Vtouch / If
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
                false,
                earthResistanceOhm,
                faultCurrentA,
                0.0,
                0.0,
                "INVALID INPUT"
            )
        }

        val epr =
            faultCurrentA *
                    earthResistanceOhm

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
                epr,
            maximumResistanceOhm =
                maximumResistance,
            status = status
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
        transformerImpedancePercent = 6.0

        generatorKVA = 0.0

        capacitorKVAR = 0.0
    }
}
