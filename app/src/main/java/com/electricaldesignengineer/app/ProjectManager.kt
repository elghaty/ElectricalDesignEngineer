package com.electricaldesignengineer.app

/**
 * Central project state.
 *
 * All electrical design modules use this object so that
 * the result of one calculation becomes the input
 * of the following calculation.
 */
object ProjectManager {

    var calculation: ProjectCalculation = ProjectCalculation()
        private set

    private val _loads = mutableListOf<LoadItem>()

    val loads: List<LoadItem>
        get() = _loads.toList()

    // --------------------------------------------------
    // PROJECT
    // --------------------------------------------------

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

        ElectricalCalculator.reset()
    }

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

    // --------------------------------------------------
    // ELECTRICAL SYSTEM
    // --------------------------------------------------

    fun updateSystem(
        voltageV: Double = calculation.voltageV,
        frequencyHz: Double = calculation.frequencyHz,
        powerFactor: Double = calculation.powerFactor,
        isThreePhase: Boolean = calculation.isThreePhase
    ) {
        calculation = calculation.copy(
            voltageV = voltageV,
            frequencyHz = frequencyHz,
            powerFactor = powerFactor,
            isThreePhase = isThreePhase
        )

        syncToElectricalCalculator()
    }

    // --------------------------------------------------
    // LOADS
    // --------------------------------------------------

    fun addLoad(load: LoadItem) {
        _loads.add(load)
    }

    fun removeLoad(load: LoadItem) {
        _loads.remove(load)
    }

    fun clearLoads() {
        _loads.clear()
    }

    // --------------------------------------------------
    // LOAD CALCULATION
    // --------------------------------------------------

    fun calculateFromLoads(): ProjectCalculation {

        var connectedKW = 0.0
        var demandKW = 0.0
        var totalKVA = 0.0

        _loads.forEach { load ->

            val connected =
                load.quantity.coerceAtLeast(0.0) *
                        load.powerKW.coerceAtLeast(0.0)

            val demand =
                connected *
                        load.demandFactor.coerceIn(0.0, 1.0)

            val pf =
                load.powerFactor.coerceIn(0.01, 1.0)

            connectedKW += connected
            demandKW += demand
            totalKVA += demand / pf
        }

        val effectivePF =
            if (totalKVA > 0.0) {
                demandKW / totalKVA
            } else {
                calculation.powerFactor
            }

        val designCurrentA =
            if (calculation.isThreePhase) {

                ElectricalCalculator.threePhaseCurrent(
                    totalKVA,
                    calculation.voltageV
                )

            } else {

                ElectricalCalculator.singlePhaseCurrent(
                    totalKVA,
                    calculation.voltageV
                )
            }

        calculation = calculation.copy(

            connectedKW = connectedKW,

            demandKW = demandKW,

            totalKVA = totalKVA,

            designCurrentA = designCurrentA,

            powerFactor = effectivePF,

            designStatus =
                if (_loads.isEmpty()) {
                    "NO LOADS"
                } else {
                    "LOAD CALCULATION COMPLETE"
                }
        )

        syncToElectricalCalculator()

        return calculation
    }

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

            connectedKW = connectedKW,

            demandKW = demandKW,

            totalKVA = totalKVA,

            designCurrentA = designCurrentA,

            voltageV = voltageV,

            powerFactor = powerFactor,

            isThreePhase = isThreePhase,

            designStatus = "LOAD CALCULATION COMPLETE"
        )

        syncToElectricalCalculator()
    }

    // --------------------------------------------------
    // CABLE
    // --------------------------------------------------

    fun setCableResult(
        cableSizeMm2: Double,
        cableAmpacityA: Double,
        cableLengthM: Double,
        voltageDropV: Double,
        voltageDropPercent: Double
    ) {

        calculation = calculation.copy(

            cableSizeMm2 = cableSizeMm2,

            cableAmpacityA = cableAmpacityA,

            cableLengthM = cableLengthM,

            voltageDropV = voltageDropV,

            voltageDropPercent = voltageDropPercent,

            designStatus = "CABLE CALCULATION COMPLETE"
        )

        syncToElectricalCalculator()
    }

    // --------------------------------------------------
    // SHORT CIRCUIT
    // --------------------------------------------------

    fun setShortCircuit(
        shortCircuitKA: Double
    ) {

        calculation = calculation.copy(

            shortCircuitKA = shortCircuitKA,

            designStatus =
                "SHORT CIRCUIT CALCULATION COMPLETE"
        )

        syncToElectricalCalculator()
    }

    // --------------------------------------------------
    // BREAKER
    // --------------------------------------------------

    fun setBreaker(
        breakerRatingA: Int,
        breakerIcuKA: Double
    ) {

        calculation = calculation.copy(

            breakerRatingA = breakerRatingA,

            breakerIcuKA = breakerIcuKA,

            designStatus =
                "BREAKER SELECTION COMPLETE"
        )

        syncToElectricalCalculator()
    }

    // --------------------------------------------------
    // TRANSFORMER
    // --------------------------------------------------

    fun setTransformer(
        transformerKVA: Double
    ) {

        calculation = calculation.copy(

            transformerKVA = transformerKVA,

            designStatus =
                "TRANSFORMER SIZING COMPLETE"
        )

        syncToElectricalCalculator()
    }

    // --------------------------------------------------
    // GENERATOR
    // --------------------------------------------------

    fun setGenerator(
        generatorKVA: Double
    ) {

        calculation = calculation.copy(

            generatorKVA = generatorKVA,

            designStatus =
                "GENERATOR SIZING COMPLETE"
        )

        syncToElectricalCalculator()
    }

    // --------------------------------------------------
    // POWER FACTOR
    // --------------------------------------------------

    fun setCapacitorBank(
        capacitorKVAR: Double
    ) {

        calculation = calculation.copy(

            capacitorKVAR = capacitorKVAR,

            designStatus =
                "POWER FACTOR CORRECTION COMPLETE"
        )

        syncToElectricalCalculator()
    }

    // --------------------------------------------------
    // EARTHING
    // --------------------------------------------------

    fun setEarthing(
        earthResistanceOhm: Double,
        earthFaultCurrentA: Double,
        earthPotentialRiseV: Double,
        maximumEarthResistanceOhm: Double
    ) {

        calculation = calculation.copy(

            earthResistanceOhm =
                earthResistanceOhm,

            earthFaultCurrentA =
                earthFaultCurrentA,

            earthPotentialRiseV =
                earthPotentialRiseV,

            maximumEarthResistanceOhm =
                maximumEarthResistanceOhm,

            designStatus =
                "EARTHING CHECK COMPLETE"
        )

        syncToElectricalCalculator()
    }

    // --------------------------------------------------
    // SYNCHRONIZE WITH OLD CALCULATION ENGINE
    // --------------------------------------------------

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

    // --------------------------------------------------
    // IMPORT OLD VALUES
    // --------------------------------------------------

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
                ElectricalCalculator.powerFactor,

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

    // --------------------------------------------------
    // RESET
    // --------------------------------------------------

    fun reset() {

        calculation =
            ProjectCalculation()

        _loads.clear()

        ElectricalCalculator.reset()
    }
}
