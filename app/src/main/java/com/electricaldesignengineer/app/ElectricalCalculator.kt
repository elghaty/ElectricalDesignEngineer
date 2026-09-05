package com.electricaldesignengineer.app

import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.acos
import kotlin.math.max

/**
 * Central electrical calculation engine.
 *
 * All modules share these values so that the result of one calculation
 * becomes the input of the next calculation.
 *
 * NOTE:
 * This is the first engineering engine version.
 * Final project-grade design will later add IEC tables, installation
 * methods, correction factors, coordination and detailed equipment data.
 */
object ElectricalCalculator {

    // =========================
    // PROJECT SHARED VALUES
    // =========================

    var connectedKW: Double = 0.0
    var demandKW: Double = 0.0
    var totalKVA: Double = 0.0
    var designCurrentA: Double = 0.0

    var voltageV: Double = 400.0
    var powerFactor: Double = 0.90
    var isThreePhase: Boolean = true

    var cableSizeMm2: Double = 0.0
    var cableAmpacityA: Double = 0.0
    var cableLengthM: Double = 0.0
    var voltageDropV: Double = 0.0
    var voltageDropPercent: Double = 0.0

    var shortCircuitKA: Double = 0.0
    var breakerRatingA: Int = 0
    var breakerIcuKA: Double = 0.0

    var transformerKVA: Double = 0.0
    var generatorKVA: Double = 0.0
    var capacitorKVAR: Double = 0.0

    // =========================
    // STANDARD CABLE DATA
    // Preliminary values
    // =========================

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

    // =========================
    // STANDARD BREAKERS
    // =========================

    val breakerRatings = listOf(
        6, 10, 16, 20, 25, 32, 40, 50, 63,
        80, 100, 125, 160, 200, 250, 315,
        400, 500, 630, 800, 1000, 1250,
        1600, 2000, 2500, 3200, 4000
    )

    // =========================
    // STANDARD TRANSFORMERS
    // =========================

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

    // =========================
    // STANDARD GENERATORS
    // =========================

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

    // =========================
    // LOAD CALCULATION
    // =========================

    fun calculateLoad(
        quantity: Double,
        powerKWPerLoad: Double,
        demandFactor: Double,
        pf: Double,
        voltage: Double,
        threePhase: Boolean
    ): LoadResult {

        val connected = max(0.0, quantity) *
                max(0.0, powerKWPerLoad)

        val demand = connected *
                demandFactor.coerceIn(0.0, 1.0)

        val kva = if (pf > 0.0) {
            demand / pf
        } else {
            0.0
        }

        val current = if (threePhase) {
            threePhaseCurrent(kva, voltage)
        } else {
            singlePhaseCurrent(kva, voltage)
        }

        connectedKW = connected
        demandKW = demand
        totalKVA = kva
        designCurrentA = current
        powerFactor = pf
        voltageV = voltage
        isThreePhase = threePhase

        return LoadResult(
            connectedKW = connected,
            demandKW = demand,
            kva = kva,
            currentA = current
        )
    }

    data class LoadResult(
        val connectedKW: Double,
        val demandKW: Double,
        val kva: Double,
        val currentA: Double
    )

    // =========================
    // CURRENT
    // =========================

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

    // =========================
    // CABLE SIZING
    // =========================

    fun selectCable(
        currentA: Double,
        lengthM: Double,
        voltage: Double,
        pf: Double,
        threePhase: Boolean
    ): CableResult {

        val cable = cables.firstOrNull {
            it.ampacityA >= currentA
        }

        if (cable == null) {
            return CableResult(
                false,
                0.0,
                0.0,
                0.0,
                0.0,
                "No available cable size"
            )
        }

        val rOhmPerKm =
            18.1 / cable.sizeMm2

        val xOhmPerKm = 0.08

        val sinPhi = sqrt(
            (1.0 - pf * pf)
                .coerceAtLeast(0.0)
        )

        val impedanceTerm =
            rOhmPerKm * pf +
                    xOhmPerKm * sinPhi

        val drop = if (threePhase) {

            sqrt(3.0) *
                    currentA *
                    impedanceTerm *
                    lengthM / 1000.0

        } else {

            2.0 *
                    currentA *
                    impedanceTerm *
                    lengthM / 1000.0
        }

        val dropPercent =
            if (voltage > 0.0) {
                drop / voltage * 100.0
            } else {
                0.0
            }

        cableSizeMm2 = cable.sizeMm2
        cableAmpacityA = cable.ampacityA
        cableLengthM = lengthM
        voltageDropV = drop
        voltageDropPercent = dropPercent

        return CableResult(
            true,
            cable.sizeMm2,
            cable.ampacityA,
            drop,
            dropPercent,
            if (dropPercent <= 3.0) {
                "PASS"
            } else {
                "CHECK VOLTAGE DROP"
            }
        )
    }

    data class CableResult(
        val success: Boolean,
        val sizeMm2: Double,
        val ampacityA: Double,
        val voltageDropV: Double,
        val voltageDropPercent: Double,
        val status: String
    )

    // =========================
    // SHORT CIRCUIT
    // =========================

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

        shortCircuitKA =
            faultCurrent / 1000.0

        return shortCircuitKA
    }

    // =========================
    // BREAKER
    // =========================

    fun selectBreaker(
        currentA: Double,
        faultCurrentKA: Double
    ): BreakerResult {

        val rating =
            breakerRatings.firstOrNull {
                it >= currentA
            } ?: return BreakerResult(
                false,
                0,
                0.0,
                "No suitable breaker"
            )

        /*
         * Preliminary selection:
         * Icu must be >= prospective short circuit current.
         *
         * We select the smallest common breaking capacity
         * that satisfies the calculated fault current.
         */

        val icu = when {
            faultCurrentKA <= 6.0 -> 6.0
            faultCurrentKA <= 10.0 -> 10.0
            faultCurrentKA <= 15.0 -> 15.0
            faultCurrentKA <= 25.0 -> 25.0
            faultCurrentKA <= 36.0 -> 36.0
            faultCurrentKA <= 50.0 -> 50.0
            faultCurrentKA <= 65.0 -> 65.0
            faultCurrentKA <= 100.0 -> 100.0
            else -> 0.0
        }

        if (icu == 0.0) {
            return BreakerResult(
                false,
                rating,
                0.0,
                "Fault current exceeds preliminary breaker data"
            )
        }

        breakerRatingA = rating
        breakerIcuKA = icu

        return BreakerResult(
            true,
            rating,
            icu,
            "PASS"
        )
    }

    data class BreakerResult(
        val success: Boolean,
        val ratingA: Int,
        val icuKA: Double,
        val status: String
    )

    // =========================
    // TRANSFORMER SIZING
    // =========================

    fun selectTransformer(
        demandKW: Double,
        pf: Double,
        marginPercent: Double = 20.0
    ): Double {

        if (demandKW <= 0.0 || pf <= 0.0) {
            return 0.0
        }

        val requiredKVA =
            demandKW / pf

        val withMargin =
            requiredKVA *
                    (1.0 + marginPercent / 100.0)

        val selected =
            transformerRatings.firstOrNull {
                it >= withMargin
            } ?: withMargin

        transformerKVA = selected

        return selected
    }

    // =========================
    // GENERATOR SIZING
    // =========================

    fun selectGenerator(
        demandKW: Double,
        pf: Double,
        loadingPercent: Double = 80.0,
        motorAllowancePercent: Double = 15.0
    ): Double {

        if (
            demandKW <= 0.0 ||
            pf <= 0.0 ||
            loadingPercent <= 0.0
        ) {
            return 0.0
        }

        val loadKVA =
            demandKW / pf

        val motorAllowance =
            loadKVA *
                    motorAllowancePercent / 100.0

        val required =
            (loadKVA + motorAllowance) /
                    (loadingPercent / 100.0)

        val selected =
            generatorRatings.firstOrNull {
                it >= required
            } ?: required

        generatorKVA = selected

        return selected
    }

    // =========================
    // POWER FACTOR CORRECTION
    // =========================

    fun capacitorBank(
        activePowerKW: Double,
        existingPF: Double,
        targetPF: Double
    ): Double {

        if (
            activePowerKW <= 0.0 ||
            existingPF <= 0.0 ||
            targetPF <= 0.0
        ) {
            return 0.0
        }

        val pf1 =
            existingPF.coerceIn(0.01, 0.9999)

        val pf2 =
            targetPF.coerceIn(0.01, 0.9999)

        val phi1 = acos(pf1)
        val phi2 = acos(pf2)

        val q1 =
            activePowerKW * tan(phi1)

        val q2 =
            activePowerKW * tan(phi2)

        val qc =
            (q1 - q2).coerceAtLeast(0.0)

        capacitorKVAR = qc

        return qc
    }

    // =========================
    // EARTHING
    // =========================

    fun earthCheck(
        earthResistanceOhm: Double,
        faultCurrentA: Double,
        permissibleTouchVoltageV: Double = 50.0
    ): EarthingResult {

        if (
            earthResistanceOhm < 0.0 ||
            faultCurrentA <= 0.0
        ) {
            return EarthingResult(
                0.0,
                0.0,
                "INVALID INPUT"
            )
        }

        val epr =
            earthResistanceOhm *
                    faultCurrentA

        val maxResistance =
            permissibleTouchVoltageV /
                    faultCurrentA

        val status =
            if (earthResistanceOhm <= maxResistance) {
                "PASS"
            } else {
                "CHECK EARTHING"
            }

        return EarthingResult(
            epr,
            maxResistance,
            status
        )
    }

    data class EarthingResult(
        val earthPotentialRiseV: Double,
        val maximumResistanceOhm: Double,
        val status: String
    )

    // =========================
    // RESET PROJECT
    // =========================

    fun reset() {

        connectedKW = 0.0
        demandKW = 0.0
        totalKVA = 0.0
        designCurrentA = 0.0

        voltageV = 400.0
        powerFactor = 0.90
        isThreePhase = true

        cableSizeMm2 = 0.0
        cableAmpacityA = 0.0
        cableLengthM = 0.0
        voltageDropV = 0.0
        voltageDropPercent = 0.0

        shortCircuitKA = 0.0
        breakerRatingA = 0
        breakerIcuKA = 0.0

        transformerKVA = 0.0
        generatorKVA = 0.0
        capacitorKVAR = 0.0
    }
}
