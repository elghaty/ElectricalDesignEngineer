package com.electricaldesignengineer.app

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * ============================================================
 * PROFESSIONAL ENGINEERING CORE
 * ============================================================
 *
 * SINGLE ENGINEERING CALCULATION CORE
 *
 * UI
 *  ↓
 * AutoDesignService / ProjectManager
 *  ↓
 * ProfessionalEngineeringCore
 *  ↓
 * Engineering Catalog / Verified Data
 *
 * No engineering formula should be duplicated outside this core.
 */
object ProfessionalEngineeringCore {

    // ============================================================
    // SYSTEM
    // ============================================================

    enum class PhaseSystem {
        SINGLE_PHASE,
        THREE_PHASE
    }

    data class SystemInput(
        val voltageV: Double,
        val frequencyHz: Double,
        val phaseSystem: PhaseSystem,
        val powerFactor: Double
    )

    // ============================================================
    // INTERNAL VALIDATION
    // ============================================================

    private fun isFinite(value: Double): Boolean =
        value.isFinite()

    private fun isPositive(value: Double): Boolean =
        value.isFinite() && value > 0.0

    private fun validPf(value: Double): Boolean =
        value.isFinite() && value in 0.01..1.0

    private fun validFactor(value: Double): Boolean =
        value.isFinite() && value > 0.0 && value <= 1.0

    private fun check(
        name: String,
        status: EngineeringStatus,
        calculated: Double? = null,
        required: Double? = null,
        unit: String? = null,
        message: String,
        standardCode: String? = null,
        dataSource: String? = null
    ): EngineeringCheck =
        EngineeringCheck(
            name = name,
            status = status,
            calculatedValue = calculated,
            requiredValue = required,
            unit = unit,
            message = message,
            standardCode = standardCode,
            dataSource = dataSource
        )

    // ============================================================
    // CURRENT CALCULATIONS
    // ============================================================

    fun threePhaseCurrent(
        kva: Double,
        voltageV: Double
    ): Double {
        if (!isPositive(kva) || !isPositive(voltageV)) return 0.0
        return kva * 1000.0 / (sqrt(3.0) * voltageV)
    }

    fun singlePhaseCurrent(
        kva: Double,
        voltageV: Double
    ): Double {
        if (!isPositive(kva) || !isPositive(voltageV)) return 0.0
        return kva * 1000.0 / voltageV
    }

    fun currentFromPower(
        powerKW: Double,
        voltageV: Double,
        powerFactor: Double,
        phaseSystem: PhaseSystem
    ): Double {
        if (!isPositive(powerKW) || !isPositive(voltageV) || !validPf(powerFactor)) {
            return 0.0
        }

        return when (phaseSystem) {
            PhaseSystem.THREE_PHASE ->
                powerKW * 1000.0 /
                        (sqrt(3.0) * voltageV * powerFactor)

            PhaseSystem.SINGLE_PHASE ->
                powerKW * 1000.0 /
                        (voltageV * powerFactor)
        }
    }

    // ============================================================
    // POWER CALCULATIONS
    // ============================================================

    data class PowerResult(
        val status: EngineeringStatus,
        val activePowerKW: Double,
        val apparentPowerKVA: Double,
        val reactivePowerKVAR: Double,
        val powerFactor: Double,
        val phaseAngleDegrees: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun calculatePower(
        activePowerKW: Double,
        powerFactor: Double
    ): PowerResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (!isPositive(activePowerKW)) {
            checks += check(
                "Active power",
                EngineeringStatus.FAIL,
                activePowerKW,
                unit = "kW",
                message = "Active power must be greater than zero."
            )
        }

        if (!validPf(powerFactor)) {
            checks += check(
                "Power factor",
                EngineeringStatus.FAIL,
                powerFactor,
                message = "Power factor must be between 0.01 and 1.00."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return PowerResult(
                EngineeringStatus.FAIL,
                activePowerKW,
                0.0,
                0.0,
                powerFactor,
                0.0,
                checks,
                EngineeringTrace(
                    calculationName = "POWER CALCULATION",
                    standard = null,
                    checks = checks
                )
            )
        }

        val apparent =
            activePowerKW / powerFactor

        val reactive =
            activePowerKW *
                    sqrt(
                        (1.0 / (powerFactor * powerFactor) - 1.0)
                            .coerceAtLeast(0.0)
                    )

        val angle =
            Math.toDegrees(acos(powerFactor))

        checks += check(
            "Apparent power",
            EngineeringStatus.PASS,
            apparent,
            unit = "kVA",
            message = "Apparent power calculated."
        )

        checks += check(
            "Reactive power",
            EngineeringStatus.PASS,
            reactive,
            unit = "kVAr",
            message = "Reactive power calculated."
        )

        return PowerResult(
            EngineeringStatus.PASS,
            activePowerKW,
            apparent,
            reactive,
            powerFactor,
            angle,
            checks,
            EngineeringTrace(
                calculationName = "POWER CALCULATION",
                standard = null,
                checks = checks,
                assumptions = listOf(
                    "Balanced sinusoidal steady-state conditions.",
                    "Power factor is treated as displacement power factor."
                )
            )
        )
    }

    fun apparentPower(
        activePowerKW: Double,
        powerFactor: Double
    ): Double {
        if (!isPositive(activePowerKW) || !validPf(powerFactor)) return 0.0
        return activePowerKW / powerFactor
    }

    fun reactivePower(
        activePowerKW: Double,
        powerFactor: Double
    ): Double {
        if (!isPositive(activePowerKW) || !validPf(powerFactor)) return 0.0

        return activePowerKW *
                sqrt(
                    (1.0 / (powerFactor * powerFactor) - 1.0)
                        .coerceAtLeast(0.0)
                )
    }

    fun powerFactorFromPower(
        activePowerKW: Double,
        apparentPowerKVA: Double
    ): Double {
        if (!isPositive(activePowerKW) || !isPositive(apparentPowerKVA)) {
            return 0.0
        }

        return (activePowerKW / apparentPowerKVA)
            .coerceIn(0.0, 1.0)
    }

    fun activePowerFromCurrent(
        currentA: Double,
        voltageV: Double,
        powerFactor: Double,
        phaseSystem: PhaseSystem
    ): Double {
        if (!isPositive(currentA) ||
            !isPositive(voltageV) ||
            !validPf(powerFactor)
        ) {
            return 0.0
        }

        return when (phaseSystem) {
            PhaseSystem.THREE_PHASE ->
                sqrt(3.0) * voltageV * currentA * powerFactor / 1000.0

            PhaseSystem.SINGLE_PHASE ->
                voltageV * currentA * powerFactor / 1000.0
        }
    }

    fun apparentPowerFromCurrent(
        currentA: Double,
        voltageV: Double,
        phaseSystem: PhaseSystem
    ): Double {
        if (!isPositive(currentA) || !isPositive(voltageV)) return 0.0

        return when (phaseSystem) {
            PhaseSystem.THREE_PHASE ->
                sqrt(3.0) * voltageV * currentA / 1000.0

            PhaseSystem.SINGLE_PHASE ->
                voltageV * currentA / 1000.0
        }
    }

    // ============================================================
    // LOAD CALCULATION
    // ============================================================

    data class LoadInput(
        val name: String,
        val quantity: Double,
        val unitPowerKW: Double,
        val demandFactor: Double,
        val powerFactor: Double
    )

    data class LoadCalculationResult(
        val connectedKW: Double,
        val demandKW: Double,
        val demandKVA: Double,
        val currentA: Double,
        val effectivePowerFactor: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun calculateLoads(
        loads: List<LoadInput>,
        system: SystemInput
    ): LoadCalculationResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (loads.isEmpty()) {
            val c = check(
                "Load data",
                EngineeringStatus.DATA_REQUIRED,
                message = "At least one load is required."
            )

            return LoadCalculationResult(
                0.0,
                0.0,
                0.0,
                0.0,
                1.0,
                listOf(c),
                EngineeringTrace(
                    calculationName = "LOAD CALCULATION",
                    standard = null,
                    checks = listOf(c)
                )
            )
        }

        if (!isPositive(system.voltageV)) {
            checks += check(
                "System voltage",
                EngineeringStatus.FAIL,
                system.voltageV,
                unit = "V",
                message = "System voltage must be greater than zero."
            )
        }

        if (!isPositive(system.frequencyHz)) {
            checks += check(
                "Frequency",
                EngineeringStatus.FAIL,
                system.frequencyHz,
                unit = "Hz",
                message = "Frequency must be greater than zero."
            )
        }

        if (!validPf(system.powerFactor)) {
            checks += check(
                "System power factor",
                EngineeringStatus.FAIL,
                system.powerFactor,
                message = "Power factor must be between 0.01 and 1.00."
            )
        }

        loads.forEach { load ->

            if (load.name.isBlank()) {
                checks += check(
                    "Load name",
                    EngineeringStatus.FAIL,
                    message = "Load name cannot be empty."
                )
            }

            if (!isPositive(load.quantity)) {
                checks += check(
                    "Load quantity",
                    EngineeringStatus.FAIL,
                    load.quantity,
                    message = "Load quantity must be greater than zero."
                )
            }

            if (!isPositive(load.unitPowerKW)) {
                checks += check(
                    "Unit power",
                    EngineeringStatus.FAIL,
                    load.unitPowerKW,
                    unit = "kW",
                    message = "Unit power must be greater than zero."
                )
            }

            if (!load.demandFactor.isFinite() ||
                load.demandFactor !in 0.0..1.0
            ) {
                checks += check(
                    "Demand factor",
                    EngineeringStatus.FAIL,
                    load.demandFactor,
                    message = "Demand factor must be between 0 and 1."
                )
            }

            if (!validPf(load.powerFactor)) {
                checks += check(
                    "Load power factor",
                    EngineeringStatus.FAIL,
                    load.powerFactor,
                    message = "Power factor must be between 0.01 and 1.00."
                )
            }
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return LoadCalculationResult(
                0.0,
                0.0,
                0.0,
                0.0,
                1.0,
                checks,
                EngineeringTrace(
                    calculationName = "LOAD CALCULATION",
                    standard = null,
                    checks = checks
                )
            )
        }

        var connectedKW = 0.0
        var demandKW = 0.0
        var totalReactiveKVAR = 0.0

        loads.forEach { load ->

            val connected =
                load.quantity * load.unitPowerKW

            val demand =
                connected * load.demandFactor

            val reactive =
                demand *
                        sqrt(
                            (1.0 /
                                    (load.powerFactor * load.powerFactor) -
                                    1.0)
                                .coerceAtLeast(0.0)
                        )

            connectedKW += connected
            demandKW += demand
            totalReactiveKVAR += reactive
        }

        val demandKVA =
            sqrt(
                demandKW * demandKW +
                        totalReactiveKVAR * totalReactiveKVAR
            )

        val effectivePF =
            if (demandKVA > 0.0) {
                demandKW / demandKVA
            } else {
                1.0
            }

        val currentA =
            when (system.phaseSystem) {
                PhaseSystem.THREE_PHASE ->
                    threePhaseCurrent(
                        demandKVA,
                        system.voltageV
                    )

                PhaseSystem.SINGLE_PHASE ->
                    singlePhaseCurrent(
                        demandKVA,
                        system.voltageV
                    )
            }

        checks += check(
            "Load calculation",
            EngineeringStatus.PASS,
            demandKVA,
            unit = "kVA",
            message = "Load calculation completed."
        )

        return LoadCalculationResult(
            connectedKW = connectedKW,
            demandKW = demandKW,
            demandKVA = demandKVA,
            currentA = currentA,
            effectivePowerFactor = effectivePF,
            checks = checks,
            trace = EngineeringTrace(
                calculationName = "LOAD CALCULATION",
                standard = null,
                checks = checks,
                assumptions = listOf(
                    "Demand factor is applied to connected active power.",
                    "Reactive power is calculated per load.",
                    "Total kVA is calculated from total kW and total kVAr."
                )
            )
        )
    }

    // ============================================================
    // VOLTAGE DROP
    // ============================================================

    data class VoltageDropInput(
        val currentA: Double,
        val lengthM: Double,
        val voltageV: Double,
        val powerFactor: Double,
        val resistanceOhmPerKm: Double,
        val reactanceOhmPerKm: Double,
        val phaseSystem: PhaseSystem,
        val parallelRuns: Int = 1
    )

    data class VoltageDropResult(
        val status: EngineeringStatus,
        val voltageDropV: Double,
        val voltageDropPercent: Double,
        val receivingVoltageV: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun calculateVoltageDrop(
        input: VoltageDropInput
    ): VoltageDropResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (!isPositive(input.currentA)) {
            checks += check(
                "Current",
                EngineeringStatus.FAIL,
                input.currentA,
                unit = "A",
                message = "Current must be greater than zero."
            )
        }

        if (!isPositive(input.lengthM)) {
            checks += check(
                "Length",
                EngineeringStatus.FAIL,
                input.lengthM,
                unit = "m",
                message = "Length must be greater than zero."
            )
        }

        if (!isPositive(input.voltageV)) {
            checks += check(
                "Voltage",
                EngineeringStatus.FAIL,
                input.voltageV,
                unit = "V",
                message = "Voltage must be greater than zero."
            )
        }

        if (!validPf(input.powerFactor)) {
            checks += check(
                "Power factor",
                EngineeringStatus.FAIL,
                input.powerFactor,
                message = "Power factor must be between 0.01 and 1.00."
            )
        }

        if (input.resistanceOhmPerKm < 0.0 ||
            !input.resistanceOhmPerKm.isFinite()
        ) {
            checks += check(
                "Resistance",
                EngineeringStatus.FAIL,
                input.resistanceOhmPerKm,
                unit = "Ω/km",
                message = "Resistance must be valid and non-negative."
            )
        }

        if (input.reactanceOhmPerKm < 0.0 ||
            !input.reactanceOhmPerKm.isFinite()
        ) {
            checks += check(
                "Reactance",
                EngineeringStatus.FAIL,
                input.reactanceOhmPerKm,
                unit = "Ω/km",
                message = "Reactance must be valid and non-negative."
            )
        }

        if (input.parallelRuns < 1) {
            checks += check(
                "Parallel runs",
                EngineeringStatus.FAIL,
                input.parallelRuns.toDouble(),
                message = "Parallel runs must be at least one."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return VoltageDropResult(
                EngineeringStatus.FAIL,
                0.0,
                0.0,
                0.0,
                checks,
                EngineeringTrace(
                    calculationName = "VOLTAGE DROP",
                    standard = null,
                    checks = checks
                )
            )
        }

        val sinPhi =
            sqrt(
                (1.0 - input.powerFactor * input.powerFactor)
                    .coerceAtLeast(0.0)
            )

        val impedanceDropPerAmp =
            input.resistanceOhmPerKm * input.powerFactor +
                    input.reactanceOhmPerKm * sinPhi

        val dropV =
            when (input.phaseSystem) {

                PhaseSystem.THREE_PHASE ->
                    sqrt(3.0) *
                            input.currentA *
                            impedanceDropPerAmp *
                            input.lengthM /
                            1000.0 /
                            input.parallelRuns

                PhaseSystem.SINGLE_PHASE ->
                    2.0 *
                            input.currentA *
                            impedanceDropPerAmp *
                            input.lengthM /
                            1000.0 /
                            input.parallelRuns
            }

        val percent =
            dropV / input.voltageV * 100.0

        val receiving =
            input.voltageV - dropV

        checks += check(
            "Voltage drop",
            EngineeringStatus.PASS,
            dropV,
            unit = "V",
            message = "Voltage drop calculated."
        )

        return VoltageDropResult(
            EngineeringStatus.PASS,
            dropV,
            percent,
            receiving,
            checks,
            EngineeringTrace(
                calculationName = "VOLTAGE DROP",
                standard = null,
                checks = checks
            )
        )
    }

    // ============================================================
    // IMPEDANCE
    // ============================================================

    data class ImpedanceResult(
        val resistanceOhm: Double,
        val reactanceOhm: Double,
        val magnitudeOhm: Double,
        val phaseAngleDegrees: Double
    )

    fun calculateImpedance(
        resistanceOhm: Double,
        reactanceOhm: Double
    ): ImpedanceResult {

        val magnitude =
            sqrt(
                resistanceOhm * resistanceOhm +
                        reactanceOhm * reactanceOhm
            )

        val angle =
            if (resistanceOhm == 0.0) {
                if (reactanceOhm >= 0.0) 90.0 else -90.0
            } else {
                Math.toDegrees(
                    kotlin.math.atan2(
                        reactanceOhm,
                        resistanceOhm
                    )
                )
            }

        return ImpedanceResult(
            resistanceOhm,
            reactanceOhm,
            magnitude,
            angle
        )
    }

    // ============================================================
    // CABLE DERATING
    // ============================================================

    fun ambientAirCorrectionFactor(
        insulation: InsulationType,
        temperatureC: Double
    ): Double {

        val pvc =
            doubleArrayOf(
                10.0 to 1.22,
                15.0 to 1.17,
                20.0 to 1.12,
                25.0 to 1.06,
                30.0 to 1.00,
                35.0 to 0.94,
                40.0 to 0.87,
                45.0 to 0.79,
                50.0 to 0.71,
                55.0 to 0.61,
                60.0 to 0.50
            )

        val xlpe =
            doubleArrayOf(
                10.0 to 1.15,
                15.0 to 1.12,
                20.0 to 1.08,
                25.0 to 1.04,
                30.0 to 1.00,
                35.0 to 0.96,
                40.0 to 0.91,
                45.0 to 0.87,
                50.0 to 0.82,
                55.0 to 0.76,
                60.0 to 0.71,
                65.0 to 0.65,
                70.0 to 0.58,
                75.0 to 0.50,
                80.0 to 0.41
            )

        val table =
            if (
                insulation.name.contains("XLPE", true) ||
                insulation.name.contains("EPR", true)
            ) {
                xlpe
            } else {
                pvc
            }

        if (temperatureC <= table.first().first) {
            return table.first().second
        }

        if (temperatureC >= table.last().first) {
            return table.last().second
        }

        for (i in 0 until table.lastIndex) {

            val low = table[i]
            val high = table[i + 1]

            if (temperatureC in low.first..high.first) {

                val ratio =
                    (temperatureC - low.first) /
                            (high.first - low.first)

                return low.second +
                        ratio * (high.second - low.second)
            }
        }

        return 1.0
    }

    fun groupingCorrectionFactor(
        circuits: Int,
        installationMethod: InstallationMethod
    ): Double {

        if (circuits < 1) return 0.0
        if (circuits == 1) return 1.0

        val method = installationMethod.name.uppercase()

        val enclosed =
            method.contains("CONDUIT") ||
                    method.contains("TRUNKING") ||
                    method.contains("DUCT")

        val tray =
            method.contains("TRAY")

        val ladder =
            method.contains("LADDER")

        val table =
            when {
                enclosed ->
                    mapOf(
                        2 to 0.80,
                        3 to 0.70,
                        4 to 0.65,
                        5 to 0.60,
                        6 to 0.57,
                        7 to 0.54,
                        8 to 0.52,
                        9 to 0.50,
                        12 to 0.45,
                        16 to 0.41,
                        20 to 0.38
                    )

                tray ->
                    mapOf(
                        2 to 0.85,
                        3 to 0.79,
                        4 to 0.75,
                        5 to 0.73,
                        6 to 0.72
                    )

                ladder ->
                    mapOf(
                        2 to 0.87,
                        3 to 0.82,
                        4 to 0.80,
                        5 to 0.80,
                        6 to 0.80
                    )

                else ->
                    mapOf(
                        2 to 0.80,
                        3 to 0.70,
                        4 to 0.65,
                        5 to 0.60,
                        6 to 0.57
                    )
            }

        table[circuits]?.let { return it }

        val highest =
            table.keys.maxOrNull() ?: return 1.0

        return if (circuits > highest) {
            table[highest] ?: 1.0
        } else {
            1.0
        }
    }

    // ============================================================
    // CABLE DESIGN
    // ============================================================

    data class CableDesignInput(
        val designCurrentA: Double,
        val lengthM: Double,
        val voltageV: Double,
        val powerFactor: Double,
        val phaseSystem: PhaseSystem,
        val conductorMaterial: CableMaterial,
        val insulation: InsulationType,
        val installationMethod: InstallationMethod,
        val numberOfLoadedConductors: Int,
        val ambientTemperatureC: Double,
        val groupingFactor: Double,
        val thermalInsulationFactor: Double,
        val soilCorrectionFactor: Double,
        val maximumVoltageDropPercent: Double,
        val maximumParallelRuns: Int = 8,
        val ambientTemperatureCorrectionFactor: Double = 1.0,
        val loadedConductorsCorrectionFactor: Double = 1.0,
        val shortCircuitCurrentKA: Double? = null,
        val shortCircuitDurationS: Double = 1.0,
        val adiabaticK: Double? = null,
        val circuitsInSameConduit: Int = 1
    )

    data class CableData(
        val sizeMm2: Double,
        val baseAmpacityA: Double,
        val resistanceOhmPerKm: Double,
        val reactanceOhmPerKm: Double,
        val source: String,
        val revision: String
    )

    interface CableDataProvider {
        fun availableCables(
            material: CableMaterial,
            insulation: InsulationType,
            installationMethod: InstallationMethod
        ): List<CableData>
    }

    data class CableDesignResult(
        val status: EngineeringStatus,
        val selectedCable: CableData?,
        val parallelRuns: Int,
        val correctedAmpacityPerRunA: Double,
        val totalAmpacityA: Double,
        val voltageDropV: Double,
        val voltageDropPercent: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun designCable(
        input: CableDesignInput,
        provider: CableDataProvider
    ): CableDesignResult {

        val checks = mutableListOf<EngineeringCheck>()

        fun fail(
            name: String,
            value: Double? = null,
            unit: String? = null,
            message: String
        ) {
            checks += check(
                name = name,
                status = EngineeringStatus.FAIL,
                calculated = value,
                unit = unit,
                message = message
            )
        }

        if (!isPositive(input.designCurrentA)) {
            fail(
                "Design current",
                input.designCurrentA,
                "A",
                "Design current must be greater than zero."
            )
        }

        if (!isPositive(input.lengthM)) {
            fail(
                "Cable length",
                input.lengthM,
                "m",
                "Cable length must be greater than zero."
            )
        }

        if (!isPositive(input.voltageV)) {
            fail(
                "Voltage",
                input.voltageV,
                "V",
                "Voltage must be greater than zero."
            )
        }

        if (!validPf(input.powerFactor)) {
            fail(
                "Power factor",
                input.powerFactor,
                message = "Power factor must be between 0.01 and 1.00."
            )
        }

        if (input.numberOfLoadedConductors < 1) {
            fail(
                "Loaded conductors",
                input.numberOfLoadedConductors.toDouble(),
                message = "Loaded conductors must be at least one."
            )
        }

        if (input.circuitsInSameConduit < 1) {
            fail(
                "Circuits in same conduit",
                input.circuitsInSameConduit.toDouble(),
                message = "Circuits in same conduit must be at least one."
            )
        }

        if (!isPositive(input.maximumVoltageDropPercent)) {
            fail(
                "Maximum voltage drop",
                input.maximumVoltageDropPercent,
                "%",
                "Maximum voltage drop must be greater than zero."
            )
        }

        if (input.maximumParallelRuns < 1) {
            fail(
                "Maximum parallel runs",
                input.maximumParallelRuns.toDouble(),
                message = "Maximum parallel runs must be at least one."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return CableDesignResult(
                EngineeringStatus.FAIL,
                null,
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                checks,
                EngineeringTrace(
                    calculationName = "CABLE DESIGN",
                    standard = EngineeringStandards.cableSelection,
                    checks = checks
                )
            )
        }

        val cables =
            provider.availableCables(
                input.conductorMaterial,
                input.insulation,
                input.installationMethod
            )
                .filter {
                    isPositive(it.sizeMm2) &&
                            isPositive(it.baseAmpacityA) &&
                            it.resistanceOhmPerKm.isFinite() &&
                            it.reactanceOhmPerKm.isFinite() &&
                            it.resistanceOhmPerKm >= 0.0 &&
                            it.reactanceOhmPerKm >= 0.0 &&
                            it.source.isNotBlank() &&
                            it.revision.isNotBlank()
                }
                .sortedBy { it.sizeMm2 }

        if (cables.isEmpty()) {

            val c =
                check(
                    "Cable engineering database",
                    EngineeringStatus.DATA_REQUIRED,
                    message = "No verified cable data is available for the selected configuration.",
                    standardCode = EngineeringStandards.cableSelection.code
                )

            return CableDesignResult(
                EngineeringStatus.DATA_REQUIRED,
                null,
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                checks + c,
                EngineeringTrace(
                    calculationName = "CABLE DESIGN",
                    standard = EngineeringStandards.cableSelection,
                    checks = checks + c
                )
            )
        }

        /*
         * Core-owned automatic correction factors.
         */

        val automaticAmbientFactor =
            ambientAirCorrectionFactor(
                input.insulation,
                input.ambientTemperatureC
            )

        val automaticGroupingFactor =
            groupingCorrectionFactor(
                input.circuitsInSameConduit,
                input.installationMethod
            )

        val suppliedThermal =
            if (validFactor(input.thermalInsulationFactor)) {
                input.thermalInsulationFactor
            } else {
                1.0
            }

        val suppliedSoil =
            if (validFactor(input.soilCorrectionFactor)) {
                input.soilCorrectionFactor
            } else {
                1.0
            }

        val correctionFactor =
            automaticAmbientFactor *
                    automaticGroupingFactor *
                    suppliedThermal *
                    suppliedSoil

        if (!validFactor(correctionFactor)) {

            val c =
                check(
                    "Cable correction factor",
                    EngineeringStatus.FAIL,
                    correctionFactor,
                    message = "Calculated correction factor is invalid."
                )

            return CableDesignResult(
                EngineeringStatus.FAIL,
                null,
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                checks + c,
                EngineeringTrace(
                    calculationName = "CABLE DESIGN",
                    standard = EngineeringStandards.cableSelection,
                    checks = checks + c
                )
            )
        }

        val warnings = mutableListOf<String>()

        if (input.installationMethod.name.contains("BUR", true) ||
            input.installationMethod.name.contains("DUCT", true)
        ) {
            warnings +=
                "Verify soil thermal resistivity and burial geometry against the applicable installation standard."
        }

        for (runs in 1..input.maximumParallelRuns) {

            for (cable in cables) {

                val correctedAmpacity =
                    cable.baseAmpacityA * correctionFactor

                val totalAmpacity =
                    correctedAmpacity * runs

                if (totalAmpacity < input.designCurrentA) {
                    continue
                }

                val voltageDrop =
                    calculateVoltageDrop(
                        VoltageDropInput(
                            currentA = input.designCurrentA,
                            lengthM = input.lengthM,
                            voltageV = input.voltageV,
                            powerFactor = input.powerFactor,
                            resistanceOhmPerKm = cable.resistanceOhmPerKm,
                            reactanceOhmPerKm = cable.reactanceOhmPerKm,
                            phaseSystem = input.phaseSystem,
                            parallelRuns = runs
                        )
                    )

                if (voltageDrop.status != EngineeringStatus.PASS) {
                    continue
                }

                if (voltageDrop.voltageDropPercent >
                    input.maximumVoltageDropPercent
                ) {
                    continue
                }

                if (
                    input.shortCircuitCurrentKA != null &&
                    input.adiabaticK != null
                ) {

                    val faultCurrentPerRunA =
                        input.shortCircuitCurrentKA * 1000.0 / runs

                    val requiredArea =
                        faultCurrentPerRunA *
                                sqrt(input.shortCircuitDurationS) /
                                input.adiabaticK

                    if (cable.sizeMm2 < requiredArea) {
                        continue
                    }
                }

                val resultChecks =
                    mutableListOf<EngineeringCheck>()

                resultChecks += check(
                    "Cable ampacity",
                    EngineeringStatus.PASS,
                    totalAmpacity,
                    input.designCurrentA,
                    "A",
                    "Corrected cable ampacity satisfies the design current.",
                    EngineeringStandards.cableSelection.code,
                    "${cable.source} / ${cable.revision}"
                )

                resultChecks += check(
                    "Voltage drop",
                    EngineeringStatus.PASS,
                    voltageDrop.voltageDropPercent,
                    input.maximumVoltageDropPercent,
                    "%",
                    "Voltage drop is within the specified limit.",
                    EngineeringStandards.cableSelection.code,
                    "${cable.source} / ${cable.revision}"
                )

                if (
                    input.shortCircuitCurrentKA != null &&
                    input.adiabaticK != null
                ) {

                    val faultCurrentPerRunA =
                        input.shortCircuitCurrentKA * 1000.0 / runs

                    val requiredArea =
                        faultCurrentPerRunA *
                                sqrt(input.shortCircuitDurationS) /
                                input.adiabaticK

                    resultChecks += check(
                        "Short-circuit thermal withstand",
                        EngineeringStatus.PASS,
                        cable.sizeMm2,
                        requiredArea,
                        "mm²",
                        "Cable satisfies the adiabatic thermal criterion.",
                        EngineeringStandards.cableSelection.code,
                        "${cable.source} / ${cable.revision}"
                    )
                }

                return CableDesignResult(
                    status = EngineeringStatus.PASS,
                    selectedCable = cable,
                    parallelRuns = runs,
                    correctedAmpacityPerRunA = correctedAmpacity,
                    totalAmpacityA = totalAmpacity,
                    voltageDropV = voltageDrop.voltageDropV,
                    voltageDropPercent = voltageDrop.voltageDropPercent,
                    checks = resultChecks,
                    trace = EngineeringTrace(
                        calculationName = "CABLE DESIGN",
                        standard = EngineeringStandards.cableSelection,
                        checks = resultChecks,
                        warnings = warnings,
                        assumptions = listOf(
                            "Ambient correction is calculated by the engineering core.",
                            "Grouping correction is calculated by the engineering core.",
                            "Thermal insulation and soil factors remain explicit engineering inputs when required by installation conditions.",
                            "Cable resistance and reactance are taken from the engineering catalog.",
                            "Parallel identical runs divide feeder impedance contribution.",
                            "The smallest passing verified cable is selected."
                        )
                    )
                )
            }
        }

        val finalCheck =
            check(
                "Cable selection",
                EngineeringStatus.FAIL,
                message = "No verified cable configuration satisfies all design requirements.",
                standardCode = EngineeringStandards.cableSelection.code
            )

        return CableDesignResult(
            EngineeringStatus.FAIL,
            null,
            0,
            0.0,
            0.0,
            0.0,
            0.0,
            listOf(finalCheck),
            EngineeringTrace(
                calculationName = "CABLE DESIGN",
                standard = EngineeringStandards.cableSelection,
                checks = listOf(finalCheck),
                warnings = warnings
            )
        )
    }

    // ============================================================
    // BREAKER DESIGN
    // ============================================================

    data class BreakerDesignInput(
        val designCurrentA: Double,
        val cableAmpacityA: Double,
        val prospectiveShortCircuitKA: Double,
        val requiredPoles: Int,
        val systemVoltageV: Double? = null
    )

    data class BreakerData(
        val manufacturerId: String?,
        val productFamily: String?,
        val partNumber: String?,
        val ratedCurrentA: Double,
        val ratedVoltageV: Double,
        val poles: Int,
        val icuKA: Double,
        val icsKA: Double?,
        val source: String,
        val revision: String
    )

    interface BreakerDataProvider {
        fun availableBreakers(
            requiredPoles: Int
        ): List<BreakerData>
    }

    data class BreakerDesignResult(
        val status: EngineeringStatus,
        val selectedBreaker: BreakerData?,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun designBreaker(
        input: BreakerDesignInput,
        provider: BreakerDataProvider
    ): BreakerDesignResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (!isPositive(input.designCurrentA)) {
            checks += check(
                "Design current",
                EngineeringStatus.FAIL,
                input.designCurrentA,
                unit = "A",
                message = "Design current must be greater than zero."
            )
        }

        if (!isPositive(input.cableAmpacityA)) {
            checks += check(
                "Cable ampacity",
                EngineeringStatus.FAIL,
                input.cableAmpacityA,
                unit = "A",
                message = "Cable ampacity must be greater than zero."
            )
        }

        if (!isPositive(input.prospectiveShortCircuitKA)) {
            checks += check(
                "Short circuit",
                EngineeringStatus.FAIL,
                input.prospectiveShortCircuitKA,
                unit = "kA",
                message = "Prospective short-circuit current must be greater than zero."
            )
        }

        if (input.requiredPoles < 1) {
            checks += check(
                "Breaker poles",
                EngineeringStatus.FAIL,
                input.requiredPoles.toDouble(),
                message = "Required poles must be at least one."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return BreakerDesignResult(
                EngineeringStatus.FAIL,
                null,
                checks,
                EngineeringTrace(
                    calculationName = "BREAKER DESIGN",
                    standard = EngineeringStandards.circuitBreakers,
                    checks = checks
                )
            )
        }

        val breakers =
            provider.availableBreakers(input.requiredPoles)
                .filter { it.poles == input.requiredPoles }
                .filter { isPositive(it.ratedCurrentA) }
                .filter { isPositive(it.ratedVoltageV) }
                .filter { isPositive(it.icuKA) }
                .filter {
                    it.source.isNotBlank() &&
                            it.revision.isNotBlank()
                }
                .filter {
                    input.systemVoltageV == null ||
                            it.ratedVoltageV >= input.systemVoltageV
                }
                .sortedBy { it.ratedCurrentA }

        if (breakers.isEmpty()) {

            val c =
                check(
                    "Breaker engineering database",
                    EngineeringStatus.DATA_REQUIRED,
                    message = "No verified breaker data satisfies the requested conditions."
                )

            return BreakerDesignResult(
                EngineeringStatus.DATA_REQUIRED,
                null,
                checks + c,
                EngineeringTrace(
                    calculationName = "BREAKER DESIGN",
                    standard = EngineeringStandards.circuitBreakers,
                    checks = checks + c
                )
            )
        }

        for (breaker in breakers) {

            val currentPass =
                input.designCurrentA <= breaker.ratedCurrentA &&
                        breaker.ratedCurrentA <= input.cableAmpacityA

            val icuPass =
                breaker.icuKA >= input.prospectiveShortCircuitKA

            if (currentPass && icuPass) {

                val resultChecks =
                    listOf(
                        check(
                            "Ib ≤ In ≤ Iz",
                            EngineeringStatus.PASS,
                            breaker.ratedCurrentA,
                            input.cableAmpacityA,
                            "A",
                            "Breaker and cable current coordination passed.",
                            EngineeringStandards.circuitBreakers.code,
                            "${breaker.source} / ${breaker.revision}"
                        ),
                        check(
                            "Icu ≥ Ik",
                            EngineeringStatus.PASS,
                            breaker.icuKA,
                            input.prospectiveShortCircuitKA,
                            "kA",
                            "Breaker breaking capacity passed.",
                            EngineeringStandards.circuitBreakers.code,
                            "${breaker.source} / ${breaker.revision}"
                        )
                    )

                return BreakerDesignResult(
                    EngineeringStatus.PASS,
                    breaker,
                    resultChecks,
                    EngineeringTrace(
                        calculationName = "BREAKER DESIGN",
                        standard = EngineeringStandards.circuitBreakers,
                        checks = resultChecks
                    )
                )
            }
        }

        val c =
            check(
                "Breaker selection",
                EngineeringStatus.FAIL,
                message = "No breaker satisfies current and short-circuit requirements.",
                standardCode = EngineeringStandards.circuitBreakers.code
            )

        return BreakerDesignResult(
            EngineeringStatus.FAIL,
            null,
            listOf(c),
            EngineeringTrace(
                calculationName = "BREAKER DESIGN",
                standard = EngineeringStandards.circuitBreakers,
                checks = listOf(c)
            )
        )
    }

    // ============================================================
    // TRANSFORMER
    // ============================================================

    data class TransformerData(
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

    data class TransformerRequiredKVAResult(
        val status: EngineeringStatus,
        val demandKW: Double,
        val powerFactor: Double,
        val designMarginPercent: Double,
        val requiredKVAWithoutMargin: Double,
        val requiredKVA: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun requiredTransformerKVA(
        demandKW: Double,
        powerFactor: Double,
        designMarginPercent: Double = 0.0
    ): TransformerRequiredKVAResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (!isPositive(demandKW)) {
            checks += check(
                "Transformer demand",
                EngineeringStatus.FAIL,
                demandKW,
                "kW",
                message = "Demand must be greater than zero."
            )
        }

        if (!validPf(powerFactor)) {
            checks += check(
                "Power factor",
                EngineeringStatus.FAIL,
                powerFactor,
                message = "Power factor must be between 0.01 and 1.00."
            )
        }

        if (!designMarginPercent.isFinite() ||
            designMarginPercent < 0.0
        ) {
            checks += check(
                "Design margin",
                EngineeringStatus.FAIL,
                designMarginPercent,
                "%",
                message = "Design margin cannot be negative."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return TransformerRequiredKVAResult(
                EngineeringStatus.FAIL,
                demandKW,
                powerFactor,
                designMarginPercent,
                0.0,
                0.0,
                checks,
                EngineeringTrace(
                    calculationName = "TRANSFORMER REQUIRED KVA",
                    standard = EngineeringStandards.transformer,
                    checks = checks
                )
            )
        }

        val withoutMargin =
            demandKW / powerFactor

        val required =
            withoutMargin *
                    (1.0 + designMarginPercent / 100.0)

        checks += check(
            "Required transformer capacity",
            EngineeringStatus.PASS,
            required,
            unit = "kVA",
            message = "Transformer capacity calculated.",
            standardCode = EngineeringStandards.transformer.code
        )

        return TransformerRequiredKVAResult(
            EngineeringStatus.PASS,
            demandKW,
            powerFactor,
            designMarginPercent,
            withoutMargin,
            required,
            checks,
            EngineeringTrace(
                calculationName = "TRANSFORMER REQUIRED KVA",
                standard = EngineeringStandards.transformer,
                checks = checks
            )
        )
    }

    data class TransformerDesignInput(
        val requiredKVA: Double,
        val designMarginPercent: Double = 0.0,
        val requiredPrimaryVoltageV: Double? = null,
        val requiredSecondaryVoltageV: Double? = null,
        val requiredFrequencyHz: Double? = null
    )

    data class TransformerDesignResult(
        val status: EngineeringStatus,
        val selectedTransformer: TransformerData?,
        val requiredKVA: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun designTransformer(
        input: TransformerDesignInput,
        transformers: List<TransformerData>
    ): TransformerDesignResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (!isPositive(input.requiredKVA)) {
            checks += check(
                "Required transformer kVA",
                EngineeringStatus.FAIL,
                input.requiredKVA,
                "kVA",
                message = "Required kVA must be greater than zero."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return TransformerDesignResult(
                EngineeringStatus.FAIL,
                null,
                0.0,
                checks,
                EngineeringTrace(
                    calculationName = "TRANSFORMER DESIGN",
                    standard = EngineeringStandards.transformer,
                    checks = checks
                )
            )
        }

        val candidates =
            transformers
                .filter { it.verified }
                .filter {
                    isPositive(it.ratedPowerKVA) &&
                            it.ratedPowerKVA >= input.requiredKVA
                }
                .filter {
                    input.requiredPrimaryVoltageV == null ||
                            abs(
                                it.primaryVoltageV -
                                        input.requiredPrimaryVoltageV
                            ) < 0.01
                }
                .filter {
                    input.requiredSecondaryVoltageV == null ||
                            abs(
                                it.secondaryVoltageV -
                                        input.requiredSecondaryVoltageV
                            ) < 0.01
                }
                .filter {
                    input.requiredFrequencyHz == null ||
                            abs(
                                it.frequencyHz -
                                        input.requiredFrequencyHz
                            ) < 0.01
                }
                .sortedBy { it.ratedPowerKVA }

        if (candidates.isEmpty()) {

            val c =
                check(
                    "Transformer database",
                    EngineeringStatus.DATA_REQUIRED,
                    required = input.requiredKVA,
                    unit = "kVA",
                    message = "No verified transformer satisfies the design requirement."
                )

            return TransformerDesignResult(
                EngineeringStatus.DATA_REQUIRED,
                null,
                input.requiredKVA,
                listOf(c),
                EngineeringTrace(
                    calculationName = "TRANSFORMER DESIGN",
                    standard = EngineeringStandards.transformer,
                    checks = listOf(c)
                )
            )
        }

        val selected = candidates.first()

        val c =
            check(
                "Transformer capacity",
                EngineeringStatus.PASS,
                selected.ratedPowerKVA,
                input.requiredKVA,
                "kVA",
                "Transformer capacity requirement passed.",
                EngineeringStandards.transformer.code,
                "${selected.manufacturerName} / ${selected.catalogName}"
            )

        return TransformerDesignResult(
            EngineeringStatus.PASS,
            selected,
            input.requiredKVA,
            listOf(c),
            EngineeringTrace(
                calculationName = "TRANSFORMER DESIGN",
                standard = EngineeringStandards.transformer,
                checks = listOf(c)
            )
        )
    }

    // ============================================================
    // GENERATOR
    // ============================================================

    data class GeneratorData(
        val id: String,
        val manufacturerId: String,
        val manufacturerName: String,
        val catalogId: String,
        val catalogName: String,
        val catalogRevision: String,
        val productFamily: String?,
        val model: String?,
        val ratedPowerKVA: Double,
        val ratedPowerKW: Double?,
        val ratedVoltageV: Double?,
        val frequencyHz: Double?,
        val powerFactor: Double?,
        val standbyRating: Boolean,
        val primeRating: Boolean,
        val shortCircuitDataAvailable: Boolean,
        val standardCode: String?,
        val sourceUrl: String?,
        val verified: Boolean
    )

    data class GeneratorDesignInput(
        val demandKW: Double,
        val powerFactor: Double,
        val loadingPercent: Double = 80.0,
        val motorAllowancePercent: Double = 0.0,
        val designMarginPercent: Double = 0.0,
        val requiredVoltageV: Double? = null,
        val requiredFrequencyHz: Double? = null,
        val requirePrimeRating: Boolean = false,
        val requireStandbyRating: Boolean = false
    )

    data class GeneratorDesignResult(
        val status: EngineeringStatus,
        val selectedGenerator: GeneratorData?,
        val baseDemandKVA: Double,
        val motorAdjustedKVA: Double,
        val requiredGeneratorKVA: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun designGenerator(
        input: GeneratorDesignInput,
        generators: List<GeneratorData>
    ): GeneratorDesignResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (!isPositive(input.demandKW)) {
            checks += check(
                "Generator demand",
                EngineeringStatus.FAIL,
                input.demandKW,
                "kW",
                message = "Demand must be greater than zero."
            )
        }

        if (!validPf(input.powerFactor)) {
            checks += check(
                "Generator power factor",
                EngineeringStatus.FAIL,
                input.powerFactor,
                message = "Power factor must be between 0.01 and 1.00."
            )
        }

        if (!input.loadingPercent.isFinite() ||
            input.loadingPercent <= 0.0 ||
            input.loadingPercent > 100.0
        ) {
            checks += check(
                "Generator loading",
                EngineeringStatus.FAIL,
                input.loadingPercent,
                "%",
                message = "Generator loading must be greater than zero and not exceed 100%."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return GeneratorDesignResult(
                EngineeringStatus.FAIL,
                null,
                0.0,
                0.0,
                0.0,
                checks,
                EngineeringTrace(
                    calculationName = "GENERATOR DESIGN",
                    standard = null,
                    checks = checks
                )
            )
        }

        val baseKVA =
            input.demandKW / input.powerFactor

        val motorAdjusted =
            baseKVA *
                    (1.0 + input.motorAllowancePercent / 100.0)

        val required =
            motorAdjusted /
                    (input.loadingPercent / 100.0) *
                    (1.0 + input.designMarginPercent / 100.0)

        val candidates =
            generators
                .filter { it.verified }
                .filter {
                    isPositive(it.ratedPowerKVA) &&
                            it.ratedPowerKVA >= required
                }
                .filter {
                    input.requiredVoltageV == null ||
                            (
                                it.ratedVoltageV != null &&
                                        abs(
                                            it.ratedVoltageV -
                                                    input.requiredVoltageV
                                        ) < 0.01
                                )
                }
                .filter {
                    input.requiredFrequencyHz == null ||
                            (
                                it.frequencyHz != null &&
                                        abs(
                                            it.frequencyHz -
                                                    input.requiredFrequencyHz
                                        ) < 0.01
                                )
                }
                .filter {
                    !input.requirePrimeRating || it.primeRating
                }
                .filter {
                    !input.requireStandbyRating || it.standbyRating
                }
                .sortedBy { it.ratedPowerKVA }

        if (candidates.isEmpty()) {

            val c =
                check(
                    "Generator database",
                    EngineeringStatus.DATA_REQUIRED,
                    required,
                    "kVA",
                    message = "No verified generator satisfies the design requirements."
                )

            return GeneratorDesignResult(
                EngineeringStatus.DATA_REQUIRED,
                null,
                baseKVA,
                motorAdjusted,
                required,
                listOf(c),
                EngineeringTrace(
                    calculationName = "GENERATOR DESIGN",
                    standard = null,
                    checks = listOf(c)
                )
            )
        }

        val selected = candidates.first()

        val c =
            check(
                "Generator capacity",
                EngineeringStatus.PASS,
                selected.ratedPowerKVA,
                required,
                "kVA",
                "Generator capacity requirement passed."
            )

        return GeneratorDesignResult(
            EngineeringStatus.PASS,
            selected,
            baseKVA,
            motorAdjusted,
            required,
            listOf(c),
            EngineeringTrace(
                calculationName = "GENERATOR DESIGN",
                standard = null,
                checks = listOf(c)
            )
        )
    }

    // ============================================================
    // CAPACITOR BANK
    // ============================================================

    data class CapacitorBankInput(
        val activePowerKW: Double,
        val existingPowerFactor: Double,
        val targetPowerFactor: Double
    )

    data class CapacitorBankResult(
        val status: EngineeringStatus,
        val activePowerKW: Double,
        val existingPowerFactor: Double,
        val targetPowerFactor: Double,
        val existingReactivePowerKVAR: Double,
        val targetReactivePowerKVAR: Double,
        val requiredCompensationKVAR: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun calculateCapacitorBank(
        input: CapacitorBankInput
    ): CapacitorBankResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (!isPositive(input.activePowerKW)) {
            checks += check(
                "Active power",
                EngineeringStatus.FAIL,
                input.activePowerKW,
                "kW",
                message = "Active power must be greater than zero."
            )
        }

        if (!validPf(input.existingPowerFactor)) {
            checks += check(
                "Existing power factor",
                EngineeringStatus.FAIL,
                input.existingPowerFactor,
                message = "Existing PF must be between 0.01 and 1.00."
            )
        }

        if (!validPf(input.targetPowerFactor)) {
            checks += check(
                "Target power factor",
                EngineeringStatus.FAIL,
                input.targetPowerFactor,
                message = "Target PF must be between 0.01 and 1.00."
            )
        }

        if (
            validPf(input.existingPowerFactor) &&
            validPf(input.targetPowerFactor) &&
            input.targetPowerFactor <= input.existingPowerFactor
        ) {
            checks += check(
                "Power factor correction",
                EngineeringStatus.FAIL,
                input.targetPowerFactor,
                input.existingPowerFactor,
                message = "Target PF must be greater than existing PF."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return CapacitorBankResult(
                EngineeringStatus.FAIL,
                input.activePowerKW,
                input.existingPowerFactor,
                input.targetPowerFactor,
                0.0,
                0.0,
                0.0,
                checks,
                EngineeringTrace(
                    calculationName = "CAPACITOR BANK",
                    standard = null,
                    checks = checks
                )
            )
        }

        val existingQ =
            input.activePowerKW *
                    tan(acos(input.existingPowerFactor))

        val targetQ =
            input.activePowerKW *
                    tan(acos(input.targetPowerFactor))

        val compensation =
            (existingQ - targetQ).coerceAtLeast(0.0)

        checks += check(
            "Required capacitor compensation",
            EngineeringStatus.PASS,
            compensation,
            unit = "kVAr",
            message = "Required theoretical reactive compensation calculated."
        )

        return CapacitorBankResult(
            EngineeringStatus.PASS,
            input.activePowerKW,
            input.existingPowerFactor,
            input.targetPowerFactor,
            existingQ,
            targetQ,
            compensation,
            checks,
            EngineeringTrace(
                calculationName = "CAPACITOR BANK",
                standard = null,
                checks = checks
            )
        )
    }

    // ============================================================
    // EARTHING
    // ============================================================

    data class EarthingInput(
        val earthResistanceOhm: Double,
        val faultCurrentA: Double,
        val permissibleTouchVoltageV: Double
    )

    data class EarthingResult(
        val status: EngineeringStatus,
        val earthResistanceOhm: Double,
        val faultCurrentA: Double,
        val earthPotentialRiseV: Double,
        val maximumResistanceOhm: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun calculateEarthing(
        input: EarthingInput
    ): EarthingResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (!isPositive(input.earthResistanceOhm)) {
            checks += check(
                "Earth resistance",
                EngineeringStatus.FAIL,
                input.earthResistanceOhm,
                "Ω",
                message = "Earth resistance must be greater than zero."
            )
        }

        if (!isPositive(input.faultCurrentA)) {
            checks += check(
                "Fault current",
                EngineeringStatus.FAIL,
                input.faultCurrentA,
                "A",
                message = "Fault current must be greater than zero."
            )
        }

        if (!isPositive(input.permissibleTouchVoltageV)) {
            checks += check(
                "Touch voltage",
                EngineeringStatus.FAIL,
                input.permissibleTouchVoltageV,
                "V",
                message = "Permissible touch voltage must be greater than zero."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return EarthingResult(
                EngineeringStatus.FAIL,
                input.earthResistanceOhm,
                input.faultCurrentA,
                0.0,
                0.0,
                checks,
                EngineeringTrace(
                    calculationName = "EARTHING",
                    standard = null,
                    checks = checks
                )
            )
        }

        val epr =
            input.earthResistanceOhm *
                    input.faultCurrentA

        val maximumR =
            input.permissibleTouchVoltageV /
                    input.faultCurrentA

        val pass =
            input.earthResistanceOhm <= maximumR

        checks += check(
            "Earth resistance safety",
            if (pass) EngineeringStatus.PASS else EngineeringStatus.FAIL,
            input.earthResistanceOhm,
            maximumR,
            "Ω",
            if (pass)
                "Earth resistance is within permissible value."
            else
                "Earth resistance exceeds permissible value."
        )

        checks += check(
            "Earth Potential Rise",
            EngineeringStatus.PASS,
            epr,
            unit = "V",
            message = "Earth Potential Rise calculated."
        )

        return EarthingResult(
            if (pass) EngineeringStatus.PASS else EngineeringStatus.FAIL,
            input.earthResistanceOhm,
            input.faultCurrentA,
            epr,
            maximumR,
            checks,
            EngineeringTrace(
                calculationName = "EARTHING",
                standard = null,
                checks = checks
            )
        )
    }

    // ============================================================
    // SHORT CIRCUIT
    // ============================================================

    fun calculateShortCircuit(
        input: ShortCircuitInput
    ): ShortCircuitResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (input.faultType != ShortCircuitFaultType.THREE_PHASE) {

            val c =
                check(
                    "Fault type",
                    EngineeringStatus.DATA_REQUIRED,
                    message = "Only balanced three-phase short-circuit calculation is currently supported."
                )

            return shortCircuitDataRequired(
                input,
                listOf(c)
            )
        }

        if (!isPositive(input.source.voltageV)) {
            checks += check(
                "Source voltage",
                EngineeringStatus.FAIL,
                input.source.voltageV,
                "V",
                message = "Voltage must be greater than zero."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return ShortCircuitResult(
                EngineeringStatus.FAIL,
                0.0,
                0.0,
                null,
                checks,
                EngineeringTrace(
                    calculationName = "SHORT CIRCUIT",
                    standard = EngineeringStandards.shortCircuit,
                    checks = checks
                )
            )
        }

        if (
            input.cable != null &&
            input.source.transformerKVA != null &&
            input.source.transformerImpedancePercent != null &&
            (
                input.source.transformerResistancePercent == null ||
                        input.source.transformerReactancePercent == null
                )
        ) {

            val c =
                check(
                    "Transformer R/X data",
                    EngineeringStatus.DATA_REQUIRED,
                    message = "Transformer R% and X% are required when downstream cable impedance is included."
                )

            return shortCircuitDataRequired(
                input,
                checks + c
            )
        }

        if (
            input.cable != null &&
            input.source.upstreamShortCircuitKA != null &&
            (
                input.source.upstreamResistanceOhm == null ||
                        input.source.upstreamReactanceOhm == null
                )
        ) {

            val c =
                check(
                    "Upstream R/X data",
                    EngineeringStatus.DATA_REQUIRED,
                    message = "Upstream R/X data is required when downstream cable impedance is included."
                )

            return shortCircuitDataRequired(
                input,
                checks + c
            )
        }

        val sourceZ =
            calculateSourceImpedance(input.source)

        if (sourceZ == null) {

            val c =
                check(
                    "Source impedance",
                    EngineeringStatus.DATA_REQUIRED,
                    message = "Valid source impedance data is required."
                )

            return shortCircuitDataRequired(
                input,
                checks + c
            )
        }

        var totalR = sourceZ.resistanceOhm
        var totalX = sourceZ.reactanceOhm

        input.cable?.let { cable ->

            if (
                !isPositive(cable.lengthM) ||
                cable.parallelRuns < 1 ||
                cable.resistanceOhmPerKm < 0.0 ||
                cable.reactanceOhmPerKm < 0.0
            ) {

                val c =
                    check(
                        "Cable impedance",
                        EngineeringStatus.FAIL,
                        message = "Cable impedance data is invalid."
                    )

                return ShortCircuitResult(
                    EngineeringStatus.FAIL,
                    0.0,
                    0.0,
                    null,
                    checks + c,
                    EngineeringTrace(
                        calculationName = "SHORT CIRCUIT",
                        standard = EngineeringStandards.shortCircuit,
                        checks = checks + c
                    )
                )
            }

            totalR +=
                    cable.resistanceOhmPerKm *
                            cable.lengthM /
                            1000.0 /
                            cable.parallelRuns

            totalX +=
                    cable.reactanceOhmPerKm *
                            cable.lengthM /
                            1000.0 /
                            cable.parallelRuns
        }

        val magnitude =
            sqrt(
                totalR * totalR +
                        totalX * totalX
            )

        if (!isPositive(magnitude)) {
            val c =
                check(
                    "Equivalent impedance",
                    EngineeringStatus.FAIL,
                    magnitude,
                    "Ω",
                    message = "Equivalent impedance is invalid."
                )

            return ShortCircuitResult(
                EngineeringStatus.FAIL,
                0.0,
                0.0,
                null,
                checks + c,
                EngineeringTrace(
                    calculationName = "SHORT CIRCUIT",
                    standard = EngineeringStandards.shortCircuit,
                    checks = checks + c
                )
            )
        }

        val faultCurrentA =
            input.source.voltageV /
                    (sqrt(3.0) * magnitude)

        val faultCurrentKA =
            faultCurrentA / 1000.0

        val equivalent =
            ShortCircuitImpedance(
                resistanceOhm = totalR,
                reactanceOhm = totalX,
                magnitudeOhm = magnitude
            )

        checks += check(
            "Equivalent impedance",
            EngineeringStatus.PASS,
            magnitude,
            "Ω",
            message = "Equivalent fault impedance calculated."
        )

        checks += check(
            "Prospective short circuit",
            EngineeringStatus.PASS,
            faultCurrentKA,
            "kA",
            message = "Three-phase prospective short-circuit current calculated."
        )

        return ShortCircuitResult(
            EngineeringStatus.PASS,
            faultCurrentA,
            faultCurrentKA,
            equivalent,
            checks,
            EngineeringTrace(
                calculationName = "SHORT CIRCUIT",
                standard = EngineeringStandards.shortCircuit,
                checks = checks,
                assumptions = listOf(
                    "Balanced three-phase fault.",
                    "Ik = V / (sqrt(3) × Z).",
                    "Cable R and X are taken from engineering data.",
                    "Parallel identical cable runs divide feeder impedance."
                )
            )
        )
    }

    private fun calculateSourceImpedance(
        source: ShortCircuitSourceInput
    ): ShortCircuitImpedance? {

        if (
            source.transformerKVA != null &&
            source.transformerResistancePercent != null &&
            source.transformerReactancePercent != null
        ) {

            if (
                !isPositive(source.transformerKVA) ||
                source.transformerResistancePercent < 0.0 ||
                source.transformerReactancePercent < 0.0
            ) {
                return null
            }

            val baseZ =
                source.voltageV * source.voltageV /
                        (source.transformerKVA * 1000.0)

            val r =
                baseZ *
                        source.transformerResistancePercent /
                        100.0

            val x =
                baseZ *
                        source.transformerReactancePercent /
                        100.0

            val magnitude =
                sqrt(r * r + x * x)

            return ShortCircuitImpedance(
                r,
                x,
                magnitude
            )
        }

        if (
            source.transformerKVA != null &&
            source.transformerImpedancePercent != null
        ) {

            if (
                !isPositive(source.transformerKVA) ||
                !isPositive(source.transformerImpedancePercent)
            ) {
                return null
            }

            val baseZ =
                source.voltageV * source.voltageV /
                        (source.transformerKVA * 1000.0)

            val z =
                baseZ *
                        source.transformerImpedancePercent /
                        100.0

            return ShortCircuitImpedance(
                resistanceOhm = 0.0,
                reactanceOhm = z,
                magnitudeOhm = z
            )
        }

        if (
            source.upstreamResistanceOhm != null &&
            source.upstreamReactanceOhm != null
        ) {

            val r = source.upstreamResistanceOhm
            val x = source.upstreamReactanceOhm

            if (
                !r.isFinite() ||
                !x.isFinite() ||
                r < 0.0 ||
                x < 0.0
            ) {
                return null
            }

            val magnitude =
                sqrt(r * r + x * x)

            return ShortCircuitImpedance(
                r,
                x,
                magnitude
            )
        }

        if (source.upstreamShortCircuitKA != null) {

            if (!isPositive(source.upstreamShortCircuitKA)) {
                return null
            }

            val z =
                source.voltageV /
                        (
                            sqrt(3.0) *
                                    source.upstreamShortCircuitKA *
                                    1000.0
                            )

            return ShortCircuitImpedance(
                0.0,
                z,
                z
            )
        }

        return null
    }

    private fun shortCircuitDataRequired(
        input: ShortCircuitInput,
        checks: List<EngineeringCheck>
    ): ShortCircuitResult {

        return ShortCircuitResult(
            EngineeringStatus.DATA_REQUIRED,
            0.0,
            0.0,
            null,
            checks,
            EngineeringTrace(
                calculationName = "SHORT CIRCUIT",
                standard = EngineeringStandards.shortCircuit,
                checks = checks,
                warnings = listOf(
                    "Additional source impedance data is required."
                )
            )
        )
    }

    // ============================================================
    // PROTECTION
    // ============================================================

    fun checkProtection(
        input: ProtectionCheckInput
    ): ProtectionCheckResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (!isPositive(input.designCurrentA)) {
            checks += check(
                "Ib",
                EngineeringStatus.FAIL,
                input.designCurrentA,
                "A",
                message = "Design current must be greater than zero."
            )
        }

        if (!isPositive(input.cableAmpacityA)) {
            checks += check(
                "Iz",
                EngineeringStatus.FAIL,
                input.cableAmpacityA,
                "A",
                message = "Cable ampacity must be greater than zero."
            )
        }

        if (!isPositive(input.breakerRatedCurrentA)) {
            checks += check(
                "In",
                EngineeringStatus.FAIL,
                input.breakerRatedCurrentA,
                "A",
                message = "Breaker rated current must be greater than zero."
            )
        }

        if (!isPositive(input.prospectiveShortCircuitKA)) {
            checks += check(
                "Ik",
                EngineeringStatus.FAIL,
                input.prospectiveShortCircuitKA,
                "kA",
                message = "Prospective short-circuit current must be greater than zero."
            )
        }

        if (!isPositive(input.breakerIcuKA)) {
            checks += check(
                "Icu",
                EngineeringStatus.FAIL,
                input.breakerIcuKA,
                "kA",
                message = "Icu must be greater than zero."
            )
        }

        if (
            input.breakerIcsKA != null &&
            !isPositive(input.breakerIcsKA)
        ) {
            checks += check(
                "Ics",
                EngineeringStatus.FAIL,
                input.breakerIcsKA,
                "kA",
                message = "Ics must be greater than zero."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return ProtectionCheckResult(
                EngineeringStatus.FAIL,
                false,
                false,
                null,
                checks,
                EngineeringTrace(
                    calculationName = "PROTECTION CHECK",
                    standard = EngineeringStandards.circuitBreakers,
                    checks = checks
                )
            )
        }

        val overloadPass =
            input.designCurrentA <= input.breakerRatedCurrentA &&
                    input.breakerRatedCurrentA <= input.cableAmpacityA

        val breakingPass =
            input.breakerIcuKA >=
                    input.prospectiveShortCircuitKA

        val servicePass =
            input.breakerIcsKA?.let {
                it >= input.prospectiveShortCircuitKA
            }

        checks += check(
            "Ib ≤ In ≤ Iz",
            if (overloadPass)
                EngineeringStatus.PASS
            else
                EngineeringStatus.FAIL,
            input.breakerRatedCurrentA,
            input.cableAmpacityA,
            "A",
            if (overloadPass)
                "Overload coordination passed."
            else
                "Overload coordination failed.",
            EngineeringStandards.circuitBreakers.code
        )

        checks += check(
            "Icu ≥ Ik",
            if (breakingPass)
                EngineeringStatus.PASS
            else
                EngineeringStatus.FAIL,
            input.breakerIcuKA,
            input.prospectiveShortCircuitKA,
            "kA",
            if (breakingPass)
                "Ultimate breaking capacity passed."
            else
                "Ultimate breaking capacity failed.",
            EngineeringStandards.circuitBreakers.code
        )

        if (servicePass != null) {
            checks += check(
                "Ics ≥ Ik",
                if (servicePass)
                    EngineeringStatus.PASS
                else
                    EngineeringStatus.FAIL,
                input.breakerIcsKA,
                input.prospectiveShortCircuitKA,
                "kA",
                if (servicePass)
                    "Service breaking capacity passed."
                else
                    "Service breaking capacity failed.",
                EngineeringStandards.circuitBreakers.code
            )
        }

        val overall =
            overloadPass &&
                    breakingPass &&
                    (servicePass == null || servicePass)

        return ProtectionCheckResult(
            if (overall)
                EngineeringStatus.PASS
            else
                EngineeringStatus.FAIL,
            overloadPass,
            breakingPass,
            servicePass,
            checks,
            EngineeringTrace(
                calculationName = "PROTECTION CHECK",
                standard = EngineeringStandards.circuitBreakers,
                checks = checks,
                assumptions = listOf(
                    "Ib ≤ In ≤ Iz.",
                    "Icu ≥ Ik.",
                    "Ics ≥ Ik when Ics is supplied.",
                    "Selectivity/discrimination requires a dedicated study."
                )
            )
        )
    }

    // ============================================================
    // MOTOR CALCULATIONS
    // ============================================================

    data class MotorCalculationInput(
        val outputPowerKW: Double,
        val efficiency: Double,
        val powerFactor: Double,
        val voltageV: Double,
        val phaseSystem: PhaseSystem
    )

    data class MotorCalculationResult(
        val status: EngineeringStatus,
        val inputPowerKW: Double,
        val currentA: Double,
        val checks: List<EngineeringCheck>,
        val trace: EngineeringTrace
    )

    fun calculateMotorCurrent(
        input: MotorCalculationInput
    ): MotorCalculationResult {

        val checks = mutableListOf<EngineeringCheck>()

        if (!isPositive(input.outputPowerKW)) {
            checks += check(
                "Motor output power",
                EngineeringStatus.FAIL,
                input.outputPowerKW,
                "kW",
                message = "Motor power must be greater than zero."
            )
        }

        if (
            !input.efficiency.isFinite() ||
            input.efficiency <= 0.0 ||
            input.efficiency > 1.0
        ) {
            checks += check(
                "Motor efficiency",
                EngineeringStatus.FAIL,
                input.efficiency,
                message = "Motor efficiency must be between 0 and 1."
            )
        }

        if (!validPf(input.powerFactor)) {
            checks += check(
                "Motor power factor",
                EngineeringStatus.FAIL,
                input.powerFactor,
                message = "Motor power factor must be between 0.01 and 1.00."
            )
        }

        if (!isPositive(input.voltageV)) {
            checks += check(
                "Motor voltage",
                EngineeringStatus.FAIL,
                input.voltageV,
                "V",
                message = "Motor voltage must be greater than zero."
            )
        }

        if (checks.any { it.status == EngineeringStatus.FAIL }) {
            return MotorCalculationResult(
                EngineeringStatus.FAIL,
                0.0,
                0.0,
                checks,
                EngineeringTrace(
                    calculationName = "MOTOR CURRENT",
                    standard = null,
                    checks = checks
                )
            )
        }

        val inputPower =
            input.outputPowerKW / input.efficiency

        val current =
            currentFromPower(
                inputPower,
                input.voltageV,
                input.powerFactor,
                input.phaseSystem
            )

        checks += check(
            "Motor current",
            EngineeringStatus.PASS,
            current,
            "A",
            message = "Motor full-load current calculated."
        )

        return MotorCalculationResult(
            EngineeringStatus.PASS,
            inputPower,
            current,
            checks,
            EngineeringTrace(
                calculationName = "MOTOR CURRENT",
                standard = null,
                checks = checks
            )
        )
    }

    // ============================================================
    // TRANSFORMER IMPEDANCE
    // ============================================================

    fun transformerImpedanceOhm(
        voltageV: Double,
        ratedKVA: Double,
        impedancePercent: Double
    ): Double {

        if (
            !isPositive(voltageV) ||
            !isPositive(ratedKVA) ||
            !isPositive(impedancePercent)
        ) {
            return 0.0
        }

        val baseZ =
            voltageV * voltageV /
                    (ratedKVA * 1000.0)

        return baseZ *
                impedancePercent /
                100.0
    }

    // ============================================================
    // TRANSFORMER FULL-LOAD CURRENT
    // ============================================================

    fun transformerFullLoadCurrent(
        ratedKVA: Double,
        voltageV: Double,
        phaseSystem: PhaseSystem
    ): Double {

        if (!isPositive(ratedKVA) || !isPositive(voltageV)) {
            return 0.0
        }

        return when (phaseSystem) {

            PhaseSystem.THREE_PHASE ->
                ratedKVA * 1000.0 /
                        (sqrt(3.0) * voltageV)

            PhaseSystem.SINGLE_PHASE ->
                ratedKVA * 1000.0 /
                        voltageV
        }
    }

    // ============================================================
    // GENERATOR CURRENT
    // ============================================================

    fun generatorFullLoadCurrent(
        ratedKVA: Double,
        voltageV: Double,
        phaseSystem: PhaseSystem
    ): Double {

        return when (phaseSystem) {

            PhaseSystem.THREE_PHASE ->
                threePhaseCurrent(
                    ratedKVA,
                    voltageV
                )

            PhaseSystem.SINGLE_PHASE ->
                singlePhaseCurrent(
                    ratedKVA,
                    voltageV
                )
        }
    }

    // ============================================================
    // CABLE POWER-CARRYING CAPACITY
    // ============================================================

    fun correctedCableAmpacity(
        baseAmpacityA: Double,
        ambientFactor: Double,
        groupingFactor: Double,
        thermalInsulationFactor: Double = 1.0,
        soilCorrectionFactor: Double = 1.0
    ): Double {

        if (!isPositive(baseAmpacityA)) return 0.0

        val totalFactor =
            ambientFactor *
                    groupingFactor *
                    thermalInsulationFactor *
                    soilCorrectionFactor

        if (!validFactor(totalFactor)) return 0.0

        return baseAmpacityA * totalFactor
    }

    // ============================================================
    // POWER TRIANGLE
    // ============================================================

    fun reactiveFromApparent(
        apparentKVA: Double,
        activeKW: Double
    ): Double {

        if (!isPositive(apparentKVA) ||
            !isPositive(activeKW)
        ) {
            return 0.0
        }

        return sqrt(
            (apparentKVA * apparentKVA -
                    activeKW * activeKW)
                .coerceAtLeast(0.0)
        )
    }

    fun apparentFromReactive(
        activeKW: Double,
        reactiveKVAR: Double
    ): Double {

        if (!isPositive(activeKW)) return 0.0

        return sqrt(
            activeKW * activeKW +
                    reactiveKVAR * reactiveKVAR
        )
    }

    // ============================================================
    // ENERGY
    // ============================================================

    fun energyKWh(
        powerKW: Double,
        operatingHours: Double
    ): Double {

        if (!isPositive(powerKW) ||
            !isPositive(operatingHours)
        ) {
            return 0.0
        }

        return powerKW * operatingHours
    }

    // ============================================================
    // FREQUENCY / REACTANCE
    // ============================================================

    fun inductiveReactance(
        frequencyHz: Double,
        inductanceH: Double
    ): Double {

        if (!isPositive(frequencyHz) ||
            !isPositive(inductanceH)
        ) {
            return 0.0
        }

        return 2.0 *
                Math.PI *
                frequencyHz *
                inductanceH
    }

    fun capacitiveReactance(
        frequencyHz: Double,
        capacitanceF: Double
    ): Double {

        if (!isPositive(frequencyHz) ||
            !isPositive(capacitanceF)
        ) {
            return 0.0
        }

        return 1.0 /
                (
                    2.0 *
                            Math.PI *
                            frequencyHz *
                            capacitanceF
                    )
    }

    // ============================================================
    // THREE-PHASE LINE VOLTAGE / PHASE VOLTAGE
    // ============================================================

    fun lineToPhaseVoltage(
        lineVoltageV: Double
    ): Double {

        if (!isPositive(lineVoltageV)) return 0.0

        return lineVoltageV / sqrt(3.0)
    }

    fun phaseToLineVoltage(
        phaseVoltageV: Double
    ): Double {

        if (!isPositive(phaseVoltageV)) return 0.0

        return phaseVoltageV * sqrt(3.0)
    }

    // ============================================================
    // RESISTANCE
    // ============================================================

    fun conductorResistance(
        resistivityOhmMm2PerM: Double,
        lengthM: Double,
        areaMm2: Double
    ): Double {

        if (
            !isPositive(resistivityOhmMm2PerM) ||
            !isPositive(lengthM) ||
            !isPositive(areaMm2)
        ) {
            return 0.0
        }

        return resistivityOhmMm2PerM *
                lengthM /
                areaMm2
    }

    // ============================================================
    // ELECTRICAL LOSSES
    // ============================================================

    fun copperLossKW(
        currentA: Double,
        resistanceOhm: Double,
        phaseSystem: PhaseSystem
    ): Double {

        if (!isPositive(currentA) ||
            resistanceOhm < 0.0 ||
            !resistanceOhm.isFinite()
        ) {
            return 0.0
        }

        val watts =
            when (phaseSystem) {
                PhaseSystem.THREE_PHASE ->
                    3.0 *
                            currentA *
                            currentA *
                            resistanceOhm

                PhaseSystem.SINGLE_PHASE ->
                    2.0 *
                            currentA *
                            currentA *
                            resistanceOhm
            }

        return watts / 1000.0
    }

    // ============================================================
    // END
    // ============================================================
}
