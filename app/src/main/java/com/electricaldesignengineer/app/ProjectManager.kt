package com.electricaldesignengineer.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Project state manager.
 *
 * Architecture:
 *
 * UI
 *   ↓
 * ProjectManager
 *   ↓
 * ProfessionalEngineeringCore
 *
 * IMPORTANT:
 * ProjectManager stores project state only.
 * Engineering calculations are delegated to
 * ProfessionalEngineeringCore.
 *
 * ElectricalCalculator is retained temporarily only
 * for legacy compatibility with screens that have not
 * yet been migrated.
 */
object ProjectManager {

    // ============================================================
    // CURRENT PROJECT
    // ============================================================

    var calculation: ProjectCalculation by mutableStateOf(
        ProjectCalculation()
    )
        private set

    private val _loads = mutableStateListOf<LoadItem>()

    val loads: List<LoadItem>
        get() = _loads

    // ============================================================
    // NEW PROJECT
    // ============================================================

    fun startNewProject(
        projectName: String = "",
        clientName: String = "",
        projectLocation: String = "",
        engineerName: String = ""
    ) {
        calculation = ProjectCalculation(
            projectName = projectName,
            clientName = clientName,
            projectLocation = projectLocation,
            engineerName = engineerName
        )

        _loads.clear()

        /*
         * Temporary legacy compatibility.
         *
         * This will be removed after all remaining
         * legacy screens are migrated away from
         * ElectricalCalculator.
         */
        ElectricalCalculator.reset()
    }

    // ============================================================
    // PROJECT INFORMATION
    // ============================================================

    fun updateProjectInfo(
        projectName: String = calculation.projectName,
        clientName: String = calculation.clientName,
        projectLocation: String = calculation.projectLocation,
        engineerName: String = calculation.engineerName
    ) {
        calculation = calculation.copy(
            projectName = projectName,
            clientName = clientName,
            projectLocation = projectLocation,
            engineerName = engineerName
        )
    }

    // ============================================================
    // SYSTEM
    // ============================================================

    fun updateSystem(
        voltageV: Double = calculation.voltageV,
        frequencyHz: Double = calculation.frequencyHz,
        powerFactor: Double = calculation.powerFactor,
        isThreePhase: Boolean = calculation.isThreePhase
    ) {
        calculation = calculation.copy(
            voltageV = voltageV.coerceAtLeast(0.0),
            frequencyHz = frequencyHz.coerceAtLeast(0.0),
            powerFactor = powerFactor.coerceIn(0.01, 1.0),
            isThreePhase = isThreePhase
        )
    }

    // ============================================================
    // LOADS
    // ============================================================

    fun addLoad(load: LoadItem) {
        _loads.add(load)
    }

    fun removeLoad(load: LoadItem) {
        _loads.remove(load)
    }

    fun clearLoads() {
        _loads.clear()
        calculateFromLoads()
    }

    /**
     * Calculates the complete project load through
     * ProfessionalEngineeringCore.
     *
     * ProjectManager does NOT perform:
     * - connected load calculation
     * - demand load calculation
     * - kVA calculation
     * - effective PF calculation
     * - current calculation
     */
    fun calculateFromLoads(): ProjectCalculation {

        val phaseSystem =
            if (calculation.isThreePhase) {
                ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE
            } else {
                ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
            }

        val systemInput =
            ProfessionalEngineeringCore.SystemInput(
                voltageV = calculation.voltageV,
                frequencyHz = calculation.frequencyHz,
                phaseSystem = phaseSystem,
                powerFactor = calculation.powerFactor
            )

        val loadInputs =
            _loads.map { load ->
                ProfessionalEngineeringCore.LoadInput(
                    name = load.name,
                    quantity = load.quantity,
                    unitPowerKW = load.powerKW,
                    demandFactor = load.demandFactor,
                    powerFactor = load.powerFactor
                )
            }

        val result =
            ProfessionalEngineeringCore.calculateLoads(
                loads = loadInputs,
                system = systemInput
            )

        val status =
            when {
                _loads.isEmpty() ->
                    "NO LOADS"

                result.trace.status == EngineeringStatus.FAIL ->
                    "LOAD CALCULATION FAILED"

                result.trace.status == EngineeringStatus.WARNING ->
                    "LOAD CALCULATION WARNING"

                result.trace.status == EngineeringStatus.DATA_REQUIRED ->
                    "LOAD DATA REQUIRED"

                else ->
                    "LOAD CALCULATION COMPLETE"
            }

        calculation = calculation.copy(

            connectedKW =
                result.connectedKW,

            demandKW =
                result.demandKW,

            totalKVA =
                result.demandKVA,

            designCurrentA =
                result.currentA,

            powerFactor =
                result.effectivePowerFactor,

            designStatus =
                status
        )

        return calculation
    }

    /**
     * Stores an externally calculated load result.
     *
     * This method is retained for compatibility with
     * existing screens.
     *
     * New engineering calculations should call
     * ProfessionalEngineeringCore directly.
     */
    fun setLoadCalculation(
        connectedKW: Double,
        demandKW: Double,
        totalKVA: Double,
        designCurrentA: Double,
        voltageV: Double = calculation.voltageV,
        powerFactor: Double = calculation.powerFactor,
        isThreePhase: Boolean = calculation.isThreePhase
    ) {
        calculation = calculation.copy(

            connectedKW =
                connectedKW.coerceAtLeast(0.0),

            demandKW =
                demandKW.coerceAtLeast(0.0),

            totalKVA =
                totalKVA.coerceAtLeast(0.0),

            designCurrentA =
                designCurrentA.coerceAtLeast(0.0),

            voltageV =
                voltageV.coerceAtLeast(0.0),

            powerFactor =
                powerFactor.coerceIn(
                    0.01,
                    1.0
                ),

            isThreePhase =
                isThreePhase,

            designStatus =
                "LOAD CALCULATION COMPLETE"
        )
    }

    // ============================================================
    // CABLE
    // ============================================================

    fun setCableResult(
        cableSizeMm2: Double,
        cableAmpacityA: Double,
        cableLengthM: Double,
        voltageDropV: Double,
        voltageDropPercent: Double
    ) {
        calculation = calculation.copy(

            cableSizeMm2 =
                cableSizeMm2.coerceAtLeast(0.0),

            cableAmpacityA =
                cableAmpacityA.coerceAtLeast(0.0),

            cableLengthM =
                cableLengthM.coerceAtLeast(0.0),

            voltageDropV =
                voltageDropV.coerceAtLeast(0.0),

            voltageDropPercent =
                voltageDropPercent.coerceAtLeast(0.0),

            designStatus =
                "CABLE CALCULATION COMPLETE"
        )
    }

    // ============================================================
    // SHORT CIRCUIT
    // ============================================================

    fun setShortCircuit(
        shortCircuitKA: Double
    ) {
        calculation = calculation.copy(

            shortCircuitKA =
                shortCircuitKA.coerceAtLeast(0.0),

            designStatus =
                "SHORT CIRCUIT CALCULATION COMPLETE"
        )
    }

    // ============================================================
    // BREAKER
    // ============================================================

    fun setBreaker(
        breakerRatingA: Int,
        breakerIcuKA: Double
    ) {
        calculation = calculation.copy(

            breakerRatingA =
                breakerRatingA.coerceAtLeast(0),

            breakerIcuKA =
                breakerIcuKA.coerceAtLeast(0.0),

            designStatus =
                "BREAKER SELECTION COMPLETE"
        )
    }

    // ============================================================
    // TRANSFORMER
    // ============================================================

    fun setTransformer(
        transformerKVA: Double,
        transformerImpedancePercent: Double =
            calculation.transformerImpedancePercent
    ) {
        calculation = calculation.copy(

            transformerKVA =
                transformerKVA.coerceAtLeast(0.0),

            transformerImpedancePercent =
                transformerImpedancePercent.coerceAtLeast(
                    0.01
                ),

            designStatus =
                "TRANSFORMER SIZING COMPLETE"
        )
    }

    fun setTransformerImpedance(
        transformerImpedancePercent: Double
    ) {
        calculation = calculation.copy(

            transformerImpedancePercent =
                transformerImpedancePercent.coerceAtLeast(
                    0.01
                )
        )
    }

    // ============================================================
    // GENERATOR
    // ============================================================

    fun setGenerator(
        generatorKVA: Double
    ) {
        calculation = calculation.copy(

            generatorKVA =
                generatorKVA.coerceAtLeast(0.0),

            designStatus =
                "GENERATOR SIZING COMPLETE"
        )
    }

    // ============================================================
    // POWER FACTOR CORRECTION
    // ============================================================

    fun setCapacitorBank(
        capacitorKVAR: Double
    ) {
        calculation = calculation.copy(

            capacitorKVAR =
                capacitorKVAR.coerceAtLeast(0.0),

            designStatus =
                "POWER FACTOR CORRECTION COMPLETE"
        )
    }

    // ============================================================
    // EARTHING
    // ============================================================

    fun setEarthing(
        earthResistanceOhm: Double,
        earthFaultCurrentA: Double,
        earthPotentialRiseV: Double,
        maximumEarthResistanceOhm: Double
    ) {
        calculation = calculation.copy(

            earthResistanceOhm =
                earthResistanceOhm.coerceAtLeast(0.0),

            earthFaultCurrentA =
                earthFaultCurrentA.coerceAtLeast(0.0),

            earthPotentialRiseV =
                earthPotentialRiseV.coerceAtLeast(0.0),

            maximumEarthResistanceOhm =
                maximumEarthResistanceOhm.coerceAtLeast(0.0),

            designStatus =
                "EARTHING CHECK COMPLETE"
        )
    }

    // ============================================================
    // LEGACY COMPATIBILITY
    // ============================================================
    //
    // These two methods are intentionally retained temporarily.
    //
    // They are NOT used by the new engineering calculation path.
    //
    // Once all legacy screens are migrated:
    // - remove syncToElectricalCalculator()
    // - remove syncFromElectricalCalculator()
    // - remove ElectricalCalculator dependency
    //
    // ============================================================

    @Deprecated(
        message = "Legacy compatibility only. Do not use for new calculations."
    )
    fun syncToElectricalCalculator() {

        ElectricalCalculator.connectedKW =
            calculation.connectedKW

        ElectricalCalculator.demandKW =
            calculation.demandKW

        ElectricalCalculator.totalKVA =
            calculation.totalKVA

        ElectricalCalculator.designCurrentA =
            calculation.designCurrentA

        ElectricalCalculator.voltageV =
            calculation.voltageV

        ElectricalCalculator.powerFactor =
            calculation.powerFactor

        ElectricalCalculator.isThreePhase =
            calculation.isThreePhase

        ElectricalCalculator.cableSizeMm2 =
            calculation.cableSizeMm2

        ElectricalCalculator.cableAmpacityA =
            calculation.cableAmpacityA

        ElectricalCalculator.cableLengthM =
            calculation.cableLengthM

        ElectricalCalculator.voltageDropV =
            calculation.voltageDropV

        ElectricalCalculator.voltageDropPercent =
            calculation.voltageDropPercent

        ElectricalCalculator.shortCircuitKA =
            calculation.shortCircuitKA

        ElectricalCalculator.breakerRatingA =
            calculation.breakerRatingA

        ElectricalCalculator.breakerIcuKA =
            calculation.breakerIcuKA

        ElectricalCalculator.transformerKVA =
            calculation.transformerKVA

        ElectricalCalculator.generatorKVA =
            calculation.generatorKVA

        ElectricalCalculator.capacitorKVAR =
            calculation.capacitorKVAR
    }

    @Deprecated(
        message = "Legacy compatibility only. Do not use for new calculations."
    )
    fun syncFromElectricalCalculator() {

        calculation = calculation.copy(

            connectedKW =
                ElectricalCalculator.connectedKW,

            demandKW =
                ElectricalCalculator.demandKW,

            totalKVA =
                ElectricalCalculator.totalKVA,

            designCurrentA =
                ElectricalCalculator.designCurrentA,

            voltageV =
                ElectricalCalculator.voltageV,

            powerFactor =
                ElectricalCalculator.powerFactor.coerceIn(
                    0.01,
                    1.0
                ),

            isThreePhase =
                ElectricalCalculator.isThreePhase,

            cableSizeMm2 =
                ElectricalCalculator.cableSizeMm2,

            cableAmpacityA =
                ElectricalCalculator.cableAmpacityA,

            cableLengthM =
                ElectricalCalculator.cableLengthM,

            voltageDropV =
                ElectricalCalculator.voltageDropV,

            voltageDropPercent =
                ElectricalCalculator.voltageDropPercent,

            shortCircuitKA =
                ElectricalCalculator.shortCircuitKA,

            breakerRatingA =
                ElectricalCalculator.breakerRatingA,

            breakerIcuKA =
                ElectricalCalculator.breakerIcuKA,

            transformerKVA =
                ElectricalCalculator.transformerKVA,

            generatorKVA =
                ElectricalCalculator.generatorKVA,

            capacitorKVAR =
                ElectricalCalculator.capacitorKVAR
        )
    }

    // ============================================================
    // RESET
    // ============================================================

    fun reset() {

        calculation =
            ProjectCalculation()

        _loads.clear()

        /*
         * Temporary legacy cleanup.
         * Will disappear when ElectricalCalculator
         * is completely removed.
         */
        ElectricalCalculator.reset()
    }
}
