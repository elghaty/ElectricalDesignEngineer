package com.electricaldesignengineer.app

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan

object ElectricalCalculator {

    // =========================================================
    // SYSTEM DATA
    // =========================================================

    var voltageV: Double = 400.0
    var powerFactor: Double = 0.90
    var isThreePhase: Boolean = true

    // =========================================================
    // LOAD RESULTS
    // =========================================================

    var connectedKW: Double = 0.0
    var demandKW: Double = 0.0
    var totalKVA: Double = 0.0
    var designCurrentA: Double = 0.0

    // =========================================================
    // CABLE RESULTS
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
    // POWER FACTOR CORRECTION
    // =========================================================

    var capacitorKVAR: Double = 0.0

    // =========================================================
    // CABLE DATA
    //
    // Preliminary ampacity table.
    // Final engineering design must consider:
    // installation method
    // ambient temperature
    // grouping
    // conductor material
    // insulation
    // soil conditions
    // applicable IEC/local standards
    // =========================================================

    data class Cable(
        val sizeMm2: Double,
        val ampacityA: Double
    )

    val cableTable = listOf(

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

    // =========================================================
    // STANDARD BREAKER RATINGS
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

    // =========================================================
    // STANDARD BREAKER Icu
    // =========================================================

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
    // STANDARD TRANSFORMER RATINGS
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
    // STANDARD GENERATOR RATINGS
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

    data class TransformerResult(
        val success: Boolean,
        val transformerKVA: Double,
        val loadingPercent: Double,
        val status: String
    )

    data class GeneratorResult(
        val success: Boolean,
        val generatorKVA: Double,
        val loadingPercent: Double,
        val status: String
    )

    data class CapacitorResult(
        val success: Boolean,
        val capacitorKVAR: Double,
        val status: String
    )

    data class EarthingResult(
        val success: Boolean,
        val earthResistanceOhm: Double,
        val faultCurrentA: Double,
        val earthPotentialRiseV: Double,
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

        val safeVoltage =
            if (voltage > 0.0) voltage else 400.0

        val safePF =
            pf.coerceIn(0.01, 1.0)

        connectedKW =
            loads.sumOf {
                max(0.0, it.quantity) *
                        max(0.0, it.powerKW)
            }

        demandKW =
            loads.sumOf {
                max(0.0, it.quantity) *
                        max(0.0, it.powerKW) *
                        it.demandFactor.coerceIn(0.0, 1.0)
            }

        totalKVA =
            demandKW / safePF

        designCurrentA =
            if (threePhase) {

                totalKVA * 1000.0 /
                        (sqrt(3.0) * safeVoltage)

            } else {

                totalKVA * 1000.0 /
                        safeVoltage
            }

        voltageV = safeVoltage
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
    // THREE PHASE CURRENT
    // =========================================================

    fun threePhaseCurrent(
        kva: Double,
        voltage: Double
    ): Double {

        if (
            kva <= 0.0 ||
            voltage <= 0.0
        ) {
            return 0.0
        }

        return kva * 1000.0 /
                (sqrt(3.0) * voltage)
    }

    // =========================================================
    // SINGLE PHASE CURRENT
    // =========================================================

    fun singlePhaseCurrent(
        kva: Double,
        voltage: Double
    ): Double {

        if (
            kva <= 0.0 ||
            voltage <= 0.0
        ) {
            return 0.0
        }

        return kva * 1000.0 /
                voltage
    }

    // =========================================================
    // CABLE SELECTION
    //
    // Basic checks:
    //
    // Iz >= Ib
    // Voltage drop <= 3%
    //
    // This is preliminary engineering logic.
    // =========================================================

    fun selectCable(
        currentA: Double,
        lengthM: Double,
        voltage: Double,
        pf: Double = powerFactor,
        threePhase: Boolean = true,
        manualSizeMm2: Double? = null
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
            if (manualSizeMm2 != null) {

                cableTable.firstOrNull {
                    abs(
                        it.sizeMm2 -
                                manualSizeMm2
                    ) < 0.0001
                }

            } else {

                cableTable.firstOrNull {
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

        val safePF =
            pf.coerceIn(0.01, 1.0)

        val sinPhi =
            sqrt(
                max(
                    0.0,
                    1.0 - safePF.pow(2.0)
                )
            )

        // Preliminary conductor resistance model.
        val resistance =
            18.1 /
                    selectedCable.sizeMm2

        val reactance =
            0.08

        val impedanceComponent =
            resistance * safePF +
                    reactance * sinPhi

        val voltageDrop =
            if (threePhase) {

                sqrt(3.0) *
                        currentA *
                        lengthM *
                        impedanceComponent /
                        1000.0

            } else {

                2.0 *
                        currentA *
                        lengthM *
                        impedanceComponent /
                        1000.0
            }

        val voltageDropPercentCalculated =
            voltageDrop /
                    voltage *
                    100.0

        val ampacityPass =
            selectedCable.ampacityA >= currentA

        val voltageDropPass =
            voltageDropPercentCalculated <= 3.0

        val success =
            ampacityPass &&
                    voltageDropPass

        cableSizeMm2 =
            selectedCable.sizeMm2

        cableAmpacityA =
            selectedCable.ampacityA

        cableLengthM =
            lengthM

        voltageDropV =
            voltageDrop

        voltageDropPercent =
            voltageDropPercentCalculated

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
            voltageDropV = voltageDrop,
            voltageDropPercent =
                voltageDropPercentCalculated,
            status = status
        )
    }

    // =========================================================
    // TRANSFORMER SHORT CIRCUIT
    //
    // Icc = In / (%Z / 100)
    //
    // Result is kA.
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
                kva = transformerKVA,
                voltage = voltage
            )

        val shortCircuitCurrent =
            ratedCurrent /
                    (impedancePercent / 100.0)

        return shortCircuitCurrent /
                1000.0
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
    // MANUAL BREAKER VALIDATION
    //
    // Ib <= In <= Iz
    // Icu >= Icc
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

        val ratingIsStandard =
            breakerRatings.contains(
                breakerRatingA
            )

        val icuIsStandard =
            breakerIcuRatings.any {
                abs(
                    it - breakerIcuKA
                ) < 0.0001
            }

        val ibInPass =
            breakerRatingA >= currentA

        val inIzPass =
            breakerRatingA <= cableAmpacityA

        val icuPass =
            breakerIcuKA >= faultCurrentKA

        val success =
            ratingIsStandard &&
                    icuIsStandard &&
                    ibInPass &&
                    inIzPass &&
                    icuPass

        val status =
            when {

                success ->
                    "PASS - BREAKER FULL PROTECTION CHECK"

                !ratingIsStandard ->
                    "FAIL: NON-STANDARD BREAKER RATING"

                !icuIsStandard ->
                    "FAIL: NON-STANDARD ICU"

                !ibInPass ->
                    "FAIL: BREAKER RATING BELOW DESIGN CURRENT"

                !inIzPass ->
                    "FAIL: BREAKER RATING EXCEEDS CABLE AMPACITY"

                !icuPass ->
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
        requiredKVA: Double
    ): TransformerResult {

        if (requiredKVA <= 0.0) {

            return TransformerResult(
                success = false,
                transformerKVA = 0.0,
                loadingPercent = 0.0,
                status = "INVALID LOAD"
            )
        }

        val selected =
            transformerRatings.firstOrNull {
                it >= requiredKVA
            }

        if (selected == null) {

            return TransformerResult(
                success = false,
                transformerKVA = 0.0,
                loadingPercent = 0.0,
                status =
                    "NO STANDARD TRANSFORMER"
            )
        }

        val loading =
            requiredKVA /
                    selected *
                    100.0

        transformerKVA =
            selected

        return TransformerResult(
            success = true,
            transformerKVA = selected,
            loadingPercent = loading,
            status =
                "TRANSFORMER SELECTION PASS"
        )
    }

    // =========================================================
    // GENERATOR SELECTION
    // =========================================================

    fun selectGenerator(
        requiredKVA: Double
    ): GeneratorResult {

        if (requiredKVA <= 0.0) {

            return GeneratorResult(
                success = false,
                generatorKVA = 0.0,
                loadingPercent = 0.0,
                status = "INVALID LOAD"
            )
        }

        val selected =
            generatorRatings.firstOrNull {
                it >= requiredKVA
            }

        if (selected == null) {

            return GeneratorResult(
                success = false,
                generatorKVA = 0.0,
                loadingPercent = 0.0,
                status =
                    "NO STANDARD GENERATOR"
            )
        }

        val loading =
            requiredKVA /
                    selected *
                    100.0

        generatorKVA =
            selected

        return GeneratorResult(
            success = true,
            generatorKVA = selected,
            loadingPercent = loading,
            status =
                "GENERATOR SELECTION PASS"
        )
    }

    // =========================================================
    // CAPACITOR BANK CALCULATION
    //
    // Qc = P (tan φ1 - tan φ2)
    // =========================================================

    fun calculateCapacitorBank(
        activePowerKW: Double,
        initialPF: Double,
        targetPF: Double
    ): CapacitorResult {

        if (
            activePowerKW <= 0.0 ||
            initialPF <= 0.0 ||
            targetPF <= 0.0 ||
            initialPF > 1.0 ||
            targetPF > 1.0
        ) {

            return CapacitorResult(
                success = false,
                capacitorKVAR = 0.0,
                status = "INVALID INPUT"
            )
        }

        if (targetPF <= initialPF) {

            capacitorKVAR = 0.0

            return CapacitorResult(
                success = true,
                capacitorKVAR = 0.0,
                status = "NO CAPACITOR REQUIRED"
            )
        }

        val phi1 =
            acos(initialPF)

        val phi2 =
            acos(targetPF)

        val q1 =
            activePowerKW *
                    tan(phi1)

        val q2 =
            activePowerKW *
                    tan(phi2)

        val capacitor =
            max(
                0.0,
                q1 - q2
            )

        capacitorKVAR =
            capacitor

        return CapacitorResult(
            success = true,
            capacitorKVAR = capacitor,
            status =
                "CAPACITOR BANK CALCULATED"
        )
    }

    // =========================================================
    // EARTHING CHECK
    //
    // Fault current:
    // If = V / R
    //
    // Earth Potential Rise:
    // EPR = If × R
    // =========================================================

    fun earthCheck(
        earthResistanceOhm: Double,
        faultVoltageV: Double,
        maximumResistanceOhm: Double
    ): EarthingResult {

        if (
            earthResistanceOhm <= 0.0 ||
            faultVoltageV <= 0.0 ||
            maximumResistanceOhm <= 0.0
        ) {

            return EarthingResult(
                success = false,
                earthResistanceOhm =
                    earthResistanceOhm,
                faultCurrentA = 0.0,
                earthPotentialRiseV = 0.0,
                status = "INVALID INPUT"
            )
        }

        val faultCurrent =
            faultVoltageV /
                    earthResistanceOhm

        val earthPotentialRise =
            faultCurrent *
                    earthResistanceOhm

        val success =
            earthResistanceOhm <=
                    maximumResistanceOhm

        return EarthingResult(
            success = success,
            earthResistanceOhm =
                earthResistanceOhm,
            faultCurrentA =
                faultCurrent,
            earthPotentialRiseV =
                earthPotentialRise,
            status =
                if (success) {
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
