package com.electricaldesignengineer.app

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt
import kotlin.math.tan

/**

* Professional engineering calculation foundation.

* 

* ARCHITECTURE

* 

* UI

* ↓

* ProjectManager / AutoDesignService

* ↓

* ProfessionalEngineeringCore

* ↓

* Engineering data providers / catalog

* 

* IMPORTANT

* ---

* This is the SINGLE engineering calculation core.

* 

* No calculation engine, legacy calculator, or extension core

* should duplicate engineering formulas outside this object.

* 

* Engineering product ratings must come from verified catalog data.

* The core never invents manufacturer ratings.
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
  
  private fun isValidFinite(value: Double): Boolean =
  !value.isNaN() && !value.isInfinite()
  
  private fun isPositiveFinite(value: Double): Boolean =
  isValidFinite(value) && value > 0.0
  
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
     val dataCheck = EngineeringCheck(
         name = "Load data",
         status = EngineeringStatus.DATA_REQUIRED,
         message = "At least one electrical load is required before engineering calculation."
     )

     return LoadCalculationResult(
         connectedKW = 0.0,
         demandKW = 0.0,
         demandKVA = 0.0,
         currentA = 0.0,
         effectivePowerFactor = 1.0,
         checks = listOf(dataCheck),
         trace = EngineeringTrace(
             calculationName = "LOAD CALCULATION",
             standard = null,
             checks = listOf(dataCheck),
             warnings = listOf(
                 "No loads have been entered. The calculation is not considered successful."
             )
         )
     )
 }

 if (!isPositiveFinite(system.voltageV)) {
     checks += EngineeringCheck(
         name = "System voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = system.voltageV,
         unit = "V",
         message = "System voltage must be a valid value greater than zero."
     )
 }

 if (!isPositiveFinite(system.frequencyHz)) {
     checks += EngineeringCheck(
         name = "Frequency",
         status = EngineeringStatus.FAIL,
         calculatedValue = system.frequencyHz,
         unit = "Hz",
         message = "Frequency must be a valid value greater than zero."
     )
 }

 if (
     !isValidFinite(system.powerFactor) ||
     system.powerFactor !in 0.01..1.0
 ) {
     checks += EngineeringCheck(
         name = "Power factor",
         status = EngineeringStatus.FAIL,
         calculatedValue = system.powerFactor,
         message = "Power factor must be a finite value between 0.01 and 1.00."
     )
 }

 loads.forEach { load ->

     if (load.name.isBlank()) {
         checks += EngineeringCheck(
             name = "Load name",
             status = EngineeringStatus.FAIL,
             message = "Load name cannot be empty."
         )
     }

     if (!isPositiveFinite(load.quantity)) {
         checks += EngineeringCheck(
             name = "Load quantity: ${load.name}",
             status = EngineeringStatus.FAIL,
             calculatedValue = load.quantity,
             message = "Load quantity must be a finite value greater than zero."
         )
     }

     if (!isPositiveFinite(load.unitPowerKW)) {
         checks += EngineeringCheck(
             name = "Unit power: ${load.name}",
             status = EngineeringStatus.FAIL,
             calculatedValue = load.unitPowerKW,
             unit = "kW",
             message = "Unit power must be a finite value greater than zero."
         )
     }

     if (
         !isValidFinite(load.demandFactor) ||
         load.demandFactor !in 0.0..1.0
     ) {
         checks += EngineeringCheck(
             name = "Demand factor: ${load.name}",
             status = EngineeringStatus.FAIL,
             calculatedValue = load.demandFactor,
             message = "Demand factor must be a finite value between 0.00 and 1.00."
         )
     }

     if (
         !isValidFinite(load.powerFactor) ||
         load.powerFactor !in 0.01..1.0
     ) {
         checks += EngineeringCheck(
             name = "Power factor: ${load.name}",
             status = EngineeringStatus.FAIL,
             calculatedValue = load.powerFactor,
             message = "Power factor must be a finite value between 0.01 and 1.00."
         )
     }
 }

 if (checks.any { it.status == EngineeringStatus.FAIL }) {
     return LoadCalculationResult(
         connectedKW = 0.0,
         demandKW = 0.0,
         demandKVA = 0.0,
         currentA = 0.0,
         effectivePowerFactor = 1.0,
         checks = checks,
         trace = EngineeringTrace(
             calculationName = "LOAD CALCULATION",
             standard = null,
             checks = checks,
             warnings = listOf(
                 "Invalid load/system data must be corrected before engineering calculation."
             )
         )
     )
 }

 var connectedKW = 0.0
 var demandKW = 0.0
 var totalReactiveKVAR = 0.0

 loads.forEach { load ->

     val connectedKWForLoad =
         load.quantity * load.unitPowerKW

     val demandKWForLoad =
         connectedKWForLoad * load.demandFactor

     val reactiveKVARForLoad =
         demandKWForLoad *
                 sqrt(
                     (
                         1.0 /
                                 (load.powerFactor * load.powerFactor)
                     ) - 1.0
                 )

     connectedKW += connectedKWForLoad
     demandKW += demandKWForLoad
     totalReactiveKVAR += reactiveKVARForLoad
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

 checks += EngineeringCheck(
     name = "Load calculation",
     status = EngineeringStatus.PASS,
     calculatedValue = demandKVA,
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
             "Reactive power is calculated from each load power factor.",
             "Demand factor is applied to connected active power.",
             "The individual load power factors are used to calculate total reactive power."
         )
     )
 )
  
  }
  
  // ============================================================
  // CURRENT
  // ============================================================
  
  fun threePhaseCurrent(
  kva: Double,
  voltageV: Double
  ): Double {
  
   if (
     !isPositiveFinite(kva) ||
     !isPositiveFinite(voltageV)
 ) {
     return 0.0
 }

 return kva * 1000.0 /
         (sqrt(3.0) * voltageV)
  
  }
  
  fun singlePhaseCurrent(
  kva: Double,
  voltageV: Double
  ): Double {
  
   if (
     !isPositiveFinite(kva) ||
     !isPositiveFinite(voltageV)
 ) {
     return 0.0
 }

 return kva * 1000.0 / voltageV
  
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
  val adiabaticK: Double? = null
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
     value: Double?,
     unit: String,
     message: String
 ) {
     checks += EngineeringCheck(
         name = name,
         status = EngineeringStatus.FAIL,
         calculatedValue = value,
         unit = unit,
         message = message
     )
 }

 if (!isPositiveFinite(input.designCurrentA)) {
     fail(
         "Design current",
         input.designCurrentA,
         "A",
         "Design current must be a finite value greater than zero."
     )
 }

 if (!isPositiveFinite(input.lengthM)) {
     fail(
         "Cable length",
         input.lengthM,
         "m",
         "Cable length must be a finite value greater than zero."
     )
 }

 if (!isPositiveFinite(input.voltageV)) {
     fail(
         "Voltage",
         input.voltageV,
         "V",
         "Voltage must be a finite value greater than zero."
     )
 }

 if (
     !isValidFinite(input.powerFactor) ||
     input.powerFactor !in 0.01..1.0
 ) {
     fail(
         "Power factor",
         input.powerFactor,
         "",
         "Power factor must be a finite value between 0.01 and 1.00."
     )
 }

 if (input.numberOfLoadedConductors < 1) {
     fail(
         "Loaded conductors",
         input.numberOfLoadedConductors.toDouble(),
         "",
         "Number of loaded conductors must be at least one."
     )
 }

 if (!isValidFinite(input.ambientTemperatureC)) {
     fail(
         "Ambient temperature",
         null,
         "°C",
         "Ambient temperature must be a valid finite value."
     )
 }

 if (!isPositiveFinite(input.maximumVoltageDropPercent)) {
     fail(
         "Maximum voltage drop",
         input.maximumVoltageDropPercent,
         "%",
         "Voltage-drop limit must be a finite value greater than zero."
     )
 }

 if (
     !isValidFinite(input.groupingFactor) ||
     input.groupingFactor <= 0.0 ||
     input.groupingFactor > 1.0
 ) {
     fail(
         "Grouping factor",
         input.groupingFactor,
         "",
         "Grouping factor must be a finite value greater than 0 and not greater than 1."
     )
 }

 if (
     !isValidFinite(input.thermalInsulationFactor) ||
     input.thermalInsulationFactor <= 0.0 ||
     input.thermalInsulationFactor > 1.0
 ) {
     fail(
         "Thermal insulation factor",
         input.thermalInsulationFactor,
         "",
         "Thermal insulation factor must be a finite value greater than 0 and not greater than 1."
     )
 }

 if (
     !isValidFinite(input.soilCorrectionFactor) ||
     input.soilCorrectionFactor <= 0.0 ||
     input.soilCorrectionFactor > 1.0
 ) {
     fail(
         "Soil correction factor",
         input.soilCorrectionFactor,
         "",
         "Soil correction factor must be a finite value greater than 0 and not greater than 1."
     )
 }

 if (input.maximumParallelRuns < 1) {
     fail(
         "Maximum parallel runs",
         input.maximumParallelRuns.toDouble(),
         "",
         "Maximum parallel runs must be at least one."
     )
 }

 if (
     !isValidFinite(input.ambientTemperatureCorrectionFactor) ||
     input.ambientTemperatureCorrectionFactor <= 0.0 ||
     input.ambientTemperatureCorrectionFactor > 1.0
 ) {
     fail(
         "Ambient temperature correction factor",
         input.ambientTemperatureCorrectionFactor,
         "",
         "Ambient temperature correction factor must be a finite value greater than 0 and not greater than 1."
     )
 }

 if (
     !isValidFinite(input.loadedConductorsCorrectionFactor) ||
     input.loadedConductorsCorrectionFactor <= 0.0 ||
     input.loadedConductorsCorrectionFactor > 1.0
 ) {
     fail(
         "Loaded conductors correction factor",
         input.loadedConductorsCorrectionFactor,
         "",
         "Loaded-conductor correction factor must be a finite value greater than 0 and not greater than 1."
     )
 }

 if (
     input.shortCircuitCurrentKA != null &&
     !isPositiveFinite(input.shortCircuitCurrentKA)
 ) {
     fail(
         "Short-circuit current",
         input.shortCircuitCurrentKA,
         "kA",
         "Short-circuit current must be a finite value greater than zero."
     )
 }

 if (
     input.shortCircuitCurrentKA != null &&
     !isPositiveFinite(input.shortCircuitDurationS)
 ) {
     fail(
         "Short-circuit duration",
         input.shortCircuitDurationS,
         "s",
         "Short-circuit duration must be a finite value greater than zero."
     )
 }

 if (
     input.shortCircuitCurrentKA != null &&
     input.adiabaticK != null &&
     !isPositiveFinite(input.adiabaticK)
 ) {
     fail(
         "Adiabatic k factor",
         input.adiabaticK,
         "",
         "Adiabatic k factor must be a finite value greater than zero."
     )
 }

 if (checks.any { it.status == EngineeringStatus.FAIL }) {
     return CableDesignResult(
         status = EngineeringStatus.FAIL,
         selectedCable = null,
         parallelRuns = 0,
         correctedAmpacityPerRunA = 0.0,
         totalAmpacityA = 0.0,
         voltageDropV = 0.0,
         voltageDropPercent = 0.0,
         checks = checks,
         trace = EngineeringTrace(
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
             isPositiveFinite(it.sizeMm2) &&
                     isPositiveFinite(it.baseAmpacityA) &&
                     isValidFinite(it.resistanceOhmPerKm) &&
                     isValidFinite(it.reactanceOhmPerKm) &&
                     it.resistanceOhmPerKm >= 0.0 &&
                     it.reactanceOhmPerKm >= 0.0 &&
                     it.source.isNotBlank() &&
                     it.revision.isNotBlank()
         }
         .sortedBy {
             it.sizeMm2
         }

 if (cables.isEmpty()) {

     val dataCheck =
         EngineeringCheck(
             name = "Cable engineering database",
             status = EngineeringStatus.DATA_REQUIRED,
             message =
                 "No verified cable data is available for the selected construction and installation method.",
             standardCode =
                 EngineeringStandards.cableSelection.code
         )

     return CableDesignResult(
         status = EngineeringStatus.DATA_REQUIRED,
         selectedCable = null,
         parallelRuns = 0,
         correctedAmpacityPerRunA = 0.0,
         totalAmpacityA = 0.0,
         voltageDropV = 0.0,
         voltageDropPercent = 0.0,
         checks = checks + dataCheck,
         trace = EngineeringTrace(
             calculationName = "CABLE DESIGN",
             standard = EngineeringStandards.cableSelection,
             checks = checks + dataCheck,
             warnings = listOf(
                 "Verified engineering cable data is required."
             )
         )
     )
 }

 /*
  * Overall correction:
  *
  * K =
  * grouping × thermal insulation × soil
  * × ambient-temperature correction
  * × loaded-conductor correction
  */
 val correctionFactor =
     input.groupingFactor *
             input.thermalInsulationFactor *
             input.soilCorrectionFactor *
             input.ambientTemperatureCorrectionFactor *
             input.loadedConductorsCorrectionFactor

 val correctionWarnings = mutableListOf<String>()

 if (input.ambientTemperatureCorrectionFactor == 1.0) {
     correctionWarnings +=
         "Ambient-temperature correction factor is 1.0. Verify the applicable standard table for the specified ambient temperature of ${input.ambientTemperatureC} °C."
 }

 if (input.loadedConductorsCorrectionFactor == 1.0) {
     correctionWarnings +=
         "Loaded-conductor correction factor is 1.0. Verify the applicable standard table for ${input.numberOfLoadedConductors} loaded conductors."
 }

 for (runs in 1..input.maximumParallelRuns) {

     for (cable in cables) {

         val correctedPerRun =
             cable.baseAmpacityA *
                     correctionFactor

         val totalAmpacity =
             correctedPerRun * runs

         if (totalAmpacity < input.designCurrentA) {
             continue
         }

         val sinPhi =
             sqrt(
                 (
                     1.0 -
                             input.powerFactor *
                             input.powerFactor
                 ).coerceAtLeast(0.0)
             )

         val impedanceComponent =
             cable.resistanceOhmPerKm *
                     input.powerFactor +
                     cable.reactanceOhmPerKm *
                     sinPhi

         val dropV =
             when (input.phaseSystem) {

                 PhaseSystem.THREE_PHASE ->
                     sqrt(3.0) *
                             input.designCurrentA *
                             impedanceComponent *
                             input.lengthM /
                             1000.0 /
                             runs

                 PhaseSystem.SINGLE_PHASE ->
                     2.0 *
                             input.designCurrentA *
                             impedanceComponent *
                             input.lengthM /
                             1000.0 /
                             runs
             }

         val dropPercent =
             dropV /
                     input.voltageV *
                     100.0

         val thermalCheck: EngineeringCheck?

         if (
             input.shortCircuitCurrentKA != null &&
             input.adiabaticK != null
         ) {

             val faultCurrentPerRunA =
                 input.shortCircuitCurrentKA * 1000.0 / runs

             val requiredConductorAreaMm2 =
                 faultCurrentPerRunA *
                         sqrt(input.shortCircuitDurationS) /
                         input.adiabaticK

             val thermalPass =
                 cable.sizeMm2 >= requiredConductorAreaMm2

             thermalCheck =
                 EngineeringCheck(
                     name = "Short-circuit thermal withstand",
                     status =
                         if (thermalPass) {
                             EngineeringStatus.PASS
                         } else {
                             EngineeringStatus.FAIL
                         },
                     calculatedValue = cable.sizeMm2,
                     requiredValue = requiredConductorAreaMm2,
                     unit = "mm²",
                     message =
                         if (thermalPass) {
                             "Cable conductor cross-section satisfies the adiabatic short-circuit thermal criterion."
                         } else {
                             "Cable conductor cross-section does not satisfy the adiabatic short-circuit thermal criterion."
                         },
                     standardCode =
                         EngineeringStandards.cableSelection.code,
                     dataSource =
                         "${cable.source} / ${cable.revision}"
                 )

             if (!thermalPass) {
                 continue
             }

         } else {
             thermalCheck = null
         }

         val ampacityCheck =
             EngineeringCheck(
                 name = "Cable ampacity",
                 status =
                     if (totalAmpacity >= input.designCurrentA) {
                         EngineeringStatus.PASS
                     } else {
                         EngineeringStatus.FAIL
                     },
                 calculatedValue = totalAmpacity,
                 requiredValue = input.designCurrentA,
                 unit = "A",
                 message =
                     "Corrected cable ampacity verification.",
                 standardCode =
                     EngineeringStandards.cableSelection.code,
                 dataSource =
                     "${cable.source} / ${cable.revision}"
             )

         val voltageDropCheck =
             EngineeringCheck(
                 name = "Voltage drop",
                 status =
                     if (dropPercent <= input.maximumVoltageDropPercent) {
                         EngineeringStatus.PASS
                     } else {
                         EngineeringStatus.FAIL
                     },
                 calculatedValue = dropPercent,
                 requiredValue = input.maximumVoltageDropPercent,
                 unit = "%",
                 message = "Voltage-drop verification.",
                 standardCode =
                     EngineeringStandards.cableSelection.code,
                 dataSource =
                     "${cable.source} / ${cable.revision}"
             )

         if (
             ampacityCheck.status == EngineeringStatus.PASS &&
             voltageDropCheck.status == EngineeringStatus.PASS
         ) {

             val resultChecks =
                 buildList {
                     add(ampacityCheck)
                     add(voltageDropCheck)

                     if (thermalCheck != null) {
                         add(thermalCheck)
                     }
                 }

             return CableDesignResult(
                 status = EngineeringStatus.PASS,
                 selectedCable = cable,
                 parallelRuns = runs,
                 correctedAmpacityPerRunA = correctedPerRun,
                 totalAmpacityA = totalAmpacity,
                 voltageDropV = dropV,
                 voltageDropPercent = dropPercent,
                 checks = resultChecks,
                 trace = EngineeringTrace(
                     calculationName = "CABLE DESIGN",
                     standard = EngineeringStandards.cableSelection,
                     checks = resultChecks,
                     warnings = correctionWarnings,
                     assumptions = listOf(
                         "Correction factors are supplied explicitly.",
                         "Ambient temperature correction must be obtained from the applicable standard/catalog data.",
                         "Loaded-conductor correction must be obtained from the applicable standard/catalog data.",
                         "Cable electrical characteristics are supplied by the engineering data provider.",
                         "Voltage drop is calculated using conductor resistance and reactance.",
                         "Parallel identical runs divide the calculated feeder impedance contribution.",
                         "Short-circuit thermal verification is performed only when short-circuit current, duration and verified adiabatic k factor are supplied."
                     )
                 )
             )
         }
     }
 }

 val finalCheck =
     EngineeringCheck(
         name = "Cable selection",
         status = EngineeringStatus.FAIL,
         message =
             "No available cable configuration satisfies the specified design constraints.",
         standardCode =
             EngineeringStandards.cableSelection.code
     )

 return CableDesignResult(
     status = EngineeringStatus.FAIL,
     selectedCable = null,
     parallelRuns = 0,
     correctedAmpacityPerRunA = 0.0,
     totalAmpacityA = 0.0,
     voltageDropV = 0.0,
     voltageDropPercent = 0.0,
     checks = listOf(finalCheck),
     trace = EngineeringTrace(
         calculationName = "CABLE DESIGN",
         standard = EngineeringStandards.cableSelection,
         checks = listOf(finalCheck),
         warnings = correctionWarnings
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

 if (!isPositiveFinite(input.designCurrentA)) {
     checks += EngineeringCheck(
         name = "Design current",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.designCurrentA,
         unit = "A",
         message = "Design current must be a finite value greater than zero."
     )
 }

 if (!isPositiveFinite(input.cableAmpacityA)) {
     checks += EngineeringCheck(
         name = "Cable ampacity",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.cableAmpacityA,
         unit = "A",
         message = "Verified cable ampacity must be a finite value greater than zero."
     )
 }

 if (!isPositiveFinite(input.prospectiveShortCircuitKA)) {
     checks += EngineeringCheck(
         name = "Short-circuit current",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.prospectiveShortCircuitKA,
         unit = "kA",
         message = "Prospective short-circuit current must be a finite value greater than zero."
     )
 }

 if (input.requiredPoles < 1) {
     checks += EngineeringCheck(
         name = "Required poles",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredPoles.toDouble(),
         message = "Required poles must be at least one."
     )
 }

 if (
     input.systemVoltageV != null &&
     !isPositiveFinite(input.systemVoltageV)
 ) {
     checks += EngineeringCheck(
         name = "System voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.systemVoltageV,
         unit = "V",
         message =
             "System voltage must be a finite value greater than zero."
     )
 }

 if (checks.any { it.status == EngineeringStatus.FAIL }) {
     return BreakerDesignResult(
         status = EngineeringStatus.FAIL,
         selectedBreaker = null,
         checks = checks,
         trace = EngineeringTrace(
             calculationName = "BREAKER DESIGN",
             standard = EngineeringStandards.circuitBreakers,
             checks = checks
         )
     )
 }

 /*
  * EXACT pole matching is intentional.
  *
  * If the design requires 3P, the core must not silently
  * select a 4P breaker.
  *
  * A 4P breaker must be an explicit design requirement.
  */
 val breakers =
     provider.availableBreakers(input.requiredPoles)
         .filter {
             it.poles == input.requiredPoles
         }
         .filter {
             isPositiveFinite(it.ratedCurrentA)
         }
         .filter {
             isPositiveFinite(it.ratedVoltageV)
         }
         .filter {
             isPositiveFinite(it.icuKA)
         }
         .filter {
             it.source.isNotBlank() &&
                     it.revision.isNotBlank()
         }
         .sortedBy {
             it.ratedCurrentA
         }

 if (breakers.isEmpty()) {

     val dataCheck =
         EngineeringCheck(
             name = "Breaker engineering database",
             status = EngineeringStatus.DATA_REQUIRED,
             message =
                 "No verified breaker product data is available for the exact required pole count."
         )

     return BreakerDesignResult(
         status = EngineeringStatus.DATA_REQUIRED,
         selectedBreaker = null,
         checks = checks + dataCheck,
         trace = EngineeringTrace(
             calculationName = "BREAKER DESIGN",
             standard = EngineeringStandards.circuitBreakers,
             checks = checks + dataCheck
         )
     )
 }

 for (breaker in breakers) {

     val currentCoordination =
         input.designCurrentA <= breaker.ratedCurrentA &&
                 breaker.ratedCurrentA <= input.cableAmpacityA

     val breakingCapacity =
         breaker.icuKA >= input.prospectiveShortCircuitKA

     val voltageCompatibility =
         input.systemVoltageV?.let {
             breaker.ratedVoltageV >= it
         }

     if (
         currentCoordination &&
         breakingCapacity &&
         (
             voltageCompatibility == null ||
                     voltageCompatibility
             )
     ) {

         val currentCheck =
             EngineeringCheck(
                 name = "Ib ≤ In ≤ Iz",
                 status = EngineeringStatus.PASS,
                 calculatedValue = breaker.ratedCurrentA,
                 requiredValue = input.cableAmpacityA,
                 unit = "A",
                 message =
                     "Breaker rated current is coordinated with design current and cable ampacity.",
                 standardCode =
                     EngineeringStandards.circuitBreakers.code,
                 dataSource =
                     "${breaker.source} / ${breaker.revision}"
             )

         val icuCheck =
             EngineeringCheck(
                 name = "Icu ≥ Ik",
                 status = EngineeringStatus.PASS,
                 calculatedValue = breaker.icuKA,
                 requiredValue = input.prospectiveShortCircuitKA,
                 unit = "kA",
                 message =
                     "Breaker ultimate breaking capacity satisfies the specified prospective fault current.",
                 standardCode =
                     EngineeringStandards.circuitBreakers.code,
                 dataSource =
                     "${breaker.source} / ${breaker.revision}"
             )

         val resultChecks = mutableListOf(
             currentCheck,
             icuCheck
         )

         resultChecks += EngineeringCheck(
             name = "Breaker poles",
             status = EngineeringStatus.PASS,
             calculatedValue = breaker.poles.toDouble(),
             requiredValue = input.requiredPoles.toDouble(),
             message =
                 "Breaker pole count exactly matches the design requirement.",
             standardCode =
                 EngineeringStandards.circuitBreakers.code,
             dataSource =
                 "${breaker.source} / ${breaker.revision}"
         )

         if (input.systemVoltageV != null) {

             resultChecks += EngineeringCheck(
                 name = "Breaker rated voltage",
                 status =
                     if (voltageCompatibility == true) {
                         EngineeringStatus.PASS
                     } else {
                         EngineeringStatus.FAIL
                     },
                 calculatedValue = breaker.ratedVoltageV,
                 requiredValue = input.systemVoltageV,
                 unit = "V",
                 message =
                     if (voltageCompatibility == true) {
                         "Breaker rated voltage is compatible with the specified system voltage."
                     } else {
                         "Breaker rated voltage is below the specified system voltage."
                     },
                 standardCode =
                     EngineeringStandards.circuitBreakers.code,
                 dataSource =
                     "${breaker.source} / ${breaker.revision}"
             )
         }

         return BreakerDesignResult(
             status = EngineeringStatus.PASS,
             selectedBreaker = breaker,
             checks = resultChecks,
             trace = EngineeringTrace(
                 calculationName = "BREAKER DESIGN",
                 standard = EngineeringStandards.circuitBreakers,
                 checks = resultChecks,
                 assumptions = listOf(
                     "Basic current coordination: Ib ≤ In ≤ Iz.",
                     "Ultimate breaking capacity: Icu ≥ Ik.",
                     "Breaker pole count must exactly match the requested design pole count.",
                     "When system voltage is supplied, breaker rated voltage is checked against it.",
                     "Ics is checked separately by the protection stage.",
                     "Discrimination/selectivity is a separate study.",
                     "Breaker catalog data must be verified against the applicable manufacturer revision."
                 )
             )
         )
     }
 }

 val finalCheck =
     EngineeringCheck(
         name = "Breaker selection",
         status = EngineeringStatus.FAIL,
         message =
             "No verified breaker product satisfies the specified current, voltage, exact pole-count and short-circuit requirements.",
         standardCode =
             EngineeringStandards.circuitBreakers.code
     )

 return BreakerDesignResult(
     status = EngineeringStatus.FAIL,
     selectedBreaker = null,
     checks = listOf(finalCheck),
     trace = EngineeringTrace(
         calculationName = "BREAKER DESIGN",
         standard = EngineeringStandards.circuitBreakers,
         checks = listOf(finalCheck)
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
  
  /**
  
  * Required kVA = Demand kW / PF × (1 + margin)
  
  * 
  
  * This function returns the FINAL required kVA including margin.
    */
    fun requiredTransformerKVA(
    demandKW: Double,
    powerFactor: Double,
    designMarginPercent: Double = 0.0
    ): TransformerRequiredKVAResult {
    
    val checks = mutableListOf<EngineeringCheck>()
    
    if (!isPositiveFinite(demandKW)) {
    checks += EngineeringCheck(
    name = "Transformer demand",
    status = EngineeringStatus.FAIL,
    calculatedValue = demandKW,
    unit = "kW",
    message = "Demand power must be a finite value greater than zero."
    )
    }
    
    if (
    !isValidFinite(powerFactor) ||
    powerFactor !in 0.01..1.0
    ) {
    checks += EngineeringCheck(
    name = "Transformer power factor",
    status = EngineeringStatus.FAIL,
    calculatedValue = powerFactor,
    message = "Power factor must be a finite value between 0.01 and 1.00."
    )
    }
    
    if (
    !isValidFinite(designMarginPercent) ||
    designMarginPercent < 0.0
    ) {
    checks += EngineeringCheck(
    name = "Transformer design margin",
    status = EngineeringStatus.FAIL,
    calculatedValue = designMarginPercent,
    unit = "%",
    message = "Design margin must be a finite value and cannot be negative."
    )
    }
    
    if (checks.any { it.status == EngineeringStatus.FAIL }) {
    return TransformerRequiredKVAResult(
    status = EngineeringStatus.FAIL,
    demandKW = demandKW,
    powerFactor = powerFactor,
    designMarginPercent = designMarginPercent,
    requiredKVAWithoutMargin = 0.0,
    requiredKVA = 0.0,
    checks = checks,
    trace = EngineeringTrace(
    calculationName = "TRANSFORMER REQUIRED KVA",
    standard = EngineeringStandards.transformer,
    checks = checks
    )
    )
    }
    
    val requiredKVAWithoutMargin =
    demandKW / powerFactor
    
    val requiredKVA =
    requiredKVAWithoutMargin *
    (1.0 + designMarginPercent / 100.0)
    
    checks += EngineeringCheck(
    name = "Transformer required kVA",
    status = EngineeringStatus.PASS,
    calculatedValue = requiredKVA,
    unit = "kVA",
    message =
    "Required transformer capacity calculated from demand, power factor and design margin.",
    standardCode =
    EngineeringStandards.transformer.code
    )
    
    return TransformerRequiredKVAResult(
    status = EngineeringStatus.PASS,
    demandKW = demandKW,
    powerFactor = powerFactor,
    designMarginPercent = designMarginPercent,
    requiredKVAWithoutMargin = requiredKVAWithoutMargin,
    requiredKVA = requiredKVA,
    checks = checks,
    trace = EngineeringTrace(
    calculationName = "TRANSFORMER REQUIRED KVA",
    standard = EngineeringStandards.transformer,
    checks = checks,
    assumptions = listOf(
    "Transformer required capacity is calculated from active demand and operating power factor.",
    "Design margin is explicitly applied once in this calculation.",
    "The returned requiredKVA is the final requirement including the specified margin.",
    "Actual transformer rating is selected only from verified catalog data."
    )
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

 if (!isPositiveFinite(input.requiredKVA)) {
     checks += EngineeringCheck(
         name = "Required transformer capacity",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredKVA,
         unit = "kVA",
         message =
             "Required transformer capacity must be a finite value greater than zero."
     )
 }

 if (
     !isValidFinite(input.designMarginPercent) ||
     input.designMarginPercent < 0.0
 ) {
     checks += EngineeringCheck(
         name = "Design margin",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.designMarginPercent,
         unit = "%",
         message = "Design margin must be a finite value and cannot be negative."
     )
 }

 if (
     input.requiredPrimaryVoltageV != null &&
     !isPositiveFinite(input.requiredPrimaryVoltageV)
 ) {
     checks += EngineeringCheck(
         name = "Primary voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredPrimaryVoltageV,
         unit = "V",
         message =
             "Required primary voltage must be a finite value greater than zero."
     )
 }

 if (
     input.requiredSecondaryVoltageV != null &&
     !isPositiveFinite(input.requiredSecondaryVoltageV)
 ) {
     checks += EngineeringCheck(
         name = "Secondary voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredSecondaryVoltageV,
         unit = "V",
         message =
             "Required secondary voltage must be a finite value greater than zero."
     )
 }

 if (
     input.requiredFrequencyHz != null &&
     !isPositiveFinite(input.requiredFrequencyHz)
 ) {
     checks += EngineeringCheck(
         name = "Transformer frequency",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredFrequencyHz,
         unit = "Hz",
         message =
             "Required frequency must be a finite value greater than zero."
     )
 }

 if (checks.any { it.status == EngineeringStatus.FAIL }) {
     return TransformerDesignResult(
         status = EngineeringStatus.FAIL,
         selectedTransformer = null,
         requiredKVA = 0.0,
         checks = checks,
         trace = EngineeringTrace(
             calculationName = "TRANSFORMER DESIGN",
             standard = EngineeringStandards.transformer,
             checks = checks
         )
     )
 }

 /*
  * IMPORTANT:
  *
  * requiredKVA is treated as the FINAL required capacity.
  *
  * This prevents double application of the design margin when
  * requiredTransformerKVA() is used upstream.
  *
  * designMarginPercent is retained for source compatibility
  * and is validated, but it is NOT applied again here.
  */
 val requiredFinalKVA =
     input.requiredKVA

 val candidates =
     transformers
         .filter { it.verified }
         .filter {
             isPositiveFinite(it.ratedPowerKVA) &&
                     it.ratedPowerKVA >= requiredFinalKVA
         }
         .filter {
             isPositiveFinite(it.primaryVoltageV) &&
                     isPositiveFinite(it.secondaryVoltageV) &&
                     isPositiveFinite(it.frequencyHz)
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
         .sortedBy {
             it.ratedPowerKVA
         }

 if (candidates.isEmpty()) {

     val dataCheck =
         EngineeringCheck(
             name = "Transformer engineering database",
             status = EngineeringStatus.DATA_REQUIRED,
             requiredValue = requiredFinalKVA,
             unit = "kVA",
             message =
                 "No verified transformer in the engineering catalog satisfies the required capacity and voltage/frequency constraints.",
             standardCode =
                 EngineeringStandards.transformer.code
         )

     return TransformerDesignResult(
         status = EngineeringStatus.DATA_REQUIRED,
         selectedTransformer = null,
         requiredKVA = requiredFinalKVA,
         checks = checks + dataCheck,
         trace = EngineeringTrace(
             calculationName = "TRANSFORMER DESIGN",
             standard = EngineeringStandards.transformer,
             checks = checks + dataCheck,
             warnings = listOf(
                 "A verified manufacturer or project-approved transformer catalog is required."
             )
         )
     )
 }

 val selected =
     candidates.first()

 val capacityCheck =
     EngineeringCheck(
         name = "Transformer capacity",
         status = EngineeringStatus.PASS,
         calculatedValue = selected.ratedPowerKVA,
         requiredValue = requiredFinalKVA,
         unit = "kVA",
         message =
             "Selected transformer capacity satisfies the final required design capacity.",
         standardCode =
             EngineeringStandards.transformer.code,
         dataSource =
             "${selected.manufacturerName} / ${selected.catalogName} / ${selected.catalogRevision}"
     )

 val verificationCheck =
     EngineeringCheck(
         name = "Catalog verification",
         status =
             if (selected.verified) {
                 EngineeringStatus.PASS
             } else {
                 EngineeringStatus.DATA_REQUIRED
             },
         message =
             "Transformer selection is based on verified catalog data.",
         dataSource =
             "${selected.manufacturerName} / ${selected.catalogName}"
     )

 checks += capacityCheck
 checks += verificationCheck

 return TransformerDesignResult(
     status = EngineeringStatus.PASS,
     selectedTransformer = selected,
     requiredKVA = requiredFinalKVA,
     checks = checks,
     trace = EngineeringTrace(
         calculationName = "TRANSFORMER DESIGN",
         standard = EngineeringStandards.transformer,
         checks = checks,
         assumptions = listOf(
             "Only verified transformer catalog records are eligible.",
             "The smallest verified transformer satisfying the final design requirement is selected.",
             "requiredKVA is treated as the final requirement to prevent double application of design margin.",
             "No hardcoded transformer rating list is used."
         )
     )
 )
  
  }
  
  // ============================================================
  // GENERATOR SIZING
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

 if (!isPositiveFinite(input.demandKW)) {
     checks += EngineeringCheck(
         name = "Generator demand",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.demandKW,
         unit = "kW",
         message = "Generator demand must be a finite value greater than zero."
     )
 }

 if (
     !isValidFinite(input.powerFactor) ||
     input.powerFactor !in 0.01..1.0
 ) {
     checks += EngineeringCheck(
         name = "Generator power factor",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.powerFactor,
         message = "Power factor must be a finite value between 0.01 and 1.00."
     )
 }

 if (
     !isValidFinite(input.loadingPercent) ||
     input.loadingPercent <= 0.0 ||
     input.loadingPercent > 100.0
 ) {
     checks += EngineeringCheck(
         name = "Generator loading",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.loadingPercent,
         unit = "%",
         message =
             "Generator allowable loading must be a finite value greater than 0 and not greater than 100%."
     )
 }

 if (
     !isValidFinite(input.motorAllowancePercent) ||
     input.motorAllowancePercent < 0.0
 ) {
     checks += EngineeringCheck(
         name = "Motor allowance",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.motorAllowancePercent,
         unit = "%",
         message = "Motor allowance must be a finite value and cannot be negative."
     )
 }

 if (
     !isValidFinite(input.designMarginPercent) ||
     input.designMarginPercent < 0.0
 ) {
     checks += EngineeringCheck(
         name = "Generator design margin",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.designMarginPercent,
         unit = "%",
         message = "Design margin must be a finite value and cannot be negative."
     )
 }

 if (
     input.requiredVoltageV != null &&
     !isPositiveFinite(input.requiredVoltageV)
 ) {
     checks += EngineeringCheck(
         name = "Generator voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredVoltageV,
         unit = "V",
         message =
             "Required generator voltage must be a finite value greater than zero."
     )
 }

 if (
     input.requiredFrequencyHz != null &&
     !isPositiveFinite(input.requiredFrequencyHz)
 ) {
     checks += EngineeringCheck(
         name = "Generator frequency",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredFrequencyHz,
         unit = "Hz",
         message =
             "Required generator frequency must be a finite value greater than zero."
     )
 }

 if (checks.any { it.status == EngineeringStatus.FAIL }) {
     return GeneratorDesignResult(
         status = EngineeringStatus.FAIL,
         selectedGenerator = null,
         baseDemandKVA = 0.0,
         motorAdjustedKVA = 0.0,
         requiredGeneratorKVA = 0.0,
         checks = checks,
         trace = EngineeringTrace(
             calculationName = "GENERATOR DESIGN",
             standard = null,
             checks = checks
         )
     )
 }

 val baseDemandKVA =
     input.demandKW / input.powerFactor

 val motorAdjustedKVA =
     baseDemandKVA *
             (1.0 + input.motorAllowancePercent / 100.0)

 val requiredBeforeMargin =
     motorAdjustedKVA /
             (input.loadingPercent / 100.0)

 val requiredGeneratorKVA =
     requiredBeforeMargin *
             (1.0 + input.designMarginPercent / 100.0)

 checks += EngineeringCheck(
     name = "Generator required capacity",
     status = EngineeringStatus.PASS,
     calculatedValue = requiredGeneratorKVA,
     unit = "kVA",
     message =
         "Generator required capacity calculated from demand, power factor, motor allowance, loading and design margin."
 )

 /*
  * Voltage/frequency are strict requirements when supplied.
  * A missing catalog value is NOT treated as a match.
  */
 val candidates =
     generators
         .filter { it.verified }
         .filter {
             isPositiveFinite(it.ratedPowerKVA) &&
                     it.ratedPowerKVA >= requiredGeneratorKVA
         }
         .filter {
             input.requiredVoltageV == null ||
                     (
                         it.ratedVoltageV != null &&
                                 isValidFinite(it.ratedVoltageV) &&
                                 it.ratedVoltageV > 0.0 &&
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
                                 isValidFinite(it.frequencyHz) &&
                                 it.frequencyHz > 0.0 &&
                                 abs(
                                     it.frequencyHz -
                                             input.requiredFrequencyHz
                                 ) < 0.01
                         )
         }
         .filter {
             !input.requirePrimeRating ||
                     it.primeRating
         }
         .filter {
             !input.requireStandbyRating ||
                     it.standbyRating
         }
         .sortedBy {
             it.ratedPowerKVA
         }

 if (candidates.isEmpty()) {

     val dataCheck =
         EngineeringCheck(
             name = "Generator engineering database",
             status = EngineeringStatus.DATA_REQUIRED,
             requiredValue = requiredGeneratorKVA,
             unit = "kVA",
             message =
                 "No verified generator catalog record satisfies the calculated requirement and all specified voltage/frequency/rating constraints."
         )

     return GeneratorDesignResult(
         status = EngineeringStatus.DATA_REQUIRED,
         selectedGenerator = null,
         baseDemandKVA = baseDemandKVA,
         motorAdjustedKVA = motorAdjustedKVA,
         requiredGeneratorKVA = requiredGeneratorKVA,
         checks = checks + dataCheck,
         trace = EngineeringTrace(
             calculationName = "GENERATOR DESIGN",
             standard = null,
             checks = checks + dataCheck,
             warnings = listOf(
                 "Verified generator manufacturer/catalog data is required.",
                 "A specified voltage or frequency cannot be accepted when the catalog record does not contain that value."
             )
         )
     )
 }

 val selected =
     candidates.first()

 val capacityCheck =
     EngineeringCheck(
         name = "Generator capacity",
         status = EngineeringStatus.PASS,
         calculatedValue = selected.ratedPowerKVA,
         requiredValue = requiredGeneratorKVA,
         unit = "kVA",
         message =
             "Selected generator rating satisfies the calculated requirement.",
         dataSource =
             "${selected.manufacturerName} / ${selected.catalogName} / ${selected.catalogRevision}"
     )

 checks += capacityCheck

 if (input.requiredVoltageV != null) {
     checks += EngineeringCheck(
         name = "Generator rated voltage",
         status = EngineeringStatus.PASS,
         calculatedValue = selected.ratedVoltageV,
         requiredValue = input.requiredVoltageV,
         unit = "V",
         message =
             "Generator rated voltage matches the specified requirement.",
         dataSource =
             "${selected.manufacturerName} / ${selected.catalogName}"
     )
 }

 if (input.requiredFrequencyHz != null) {
     checks += EngineeringCheck(
         name = "Generator frequency",
         status = EngineeringStatus.PASS,
         calculatedValue = selected.frequencyHz,
         requiredValue = input.requiredFrequencyHz,
         unit = "Hz",
         message =
             "Generator frequency matches the specified requirement.",
         dataSource =
             "${selected.manufacturerName} / ${selected.catalogName}"
     )
 }

 return GeneratorDesignResult(
     status = EngineeringStatus.PASS,
     selectedGenerator = selected,
     baseDemandKVA = baseDemandKVA,
     motorAdjustedKVA = motorAdjustedKVA,
     requiredGeneratorKVA = requiredGeneratorKVA,
     checks = checks,
     trace = EngineeringTrace(
         calculationName = "GENERATOR DESIGN",
         standard = null,
         checks = checks,
         assumptions = listOf(
             "Generator ratings are taken only from verified catalog records.",
             "No generic manufacturer rating list is hardcoded.",
             "Motor allowance and generator loading are explicit engineering inputs.",
             "When voltage or frequency is specified, the corresponding catalog value must be present and compatible."
         )
     )
 )
  
  }
  
  // ============================================================
  // CAPACITOR BANK / POWER FACTOR CORRECTION
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

 if (!isPositiveFinite(input.activePowerKW)) {
     checks += EngineeringCheck(
         name = "Active power",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.activePowerKW,
         unit = "kW",
         message =
             "Active power must be a finite value greater than zero."
     )
 }

 if (
     !isValidFinite(input.existingPowerFactor) ||
     input.existingPowerFactor !in 0.01..1.0
 ) {
     checks += EngineeringCheck(
         name = "Existing power factor",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.existingPowerFactor,
         message =
             "Existing power factor must be a finite value between 0.01 and 1.00."
     )
 }

 if (
     !isValidFinite(input.targetPowerFactor) ||
     input.targetPowerFactor !in 0.01..1.0
 ) {
     checks += EngineeringCheck(
         name = "Target power factor",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.targetPowerFactor,
         message =
             "Target power factor must be a finite value between 0.01 and 1.00."
     )
 }

 if (
     input.existingPowerFactor in 0.01..1.0 &&
     input.targetPowerFactor in 0.01..1.0 &&
     input.targetPowerFactor <= input.existingPowerFactor
 ) {
     checks += EngineeringCheck(
         name = "Power factor improvement",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.targetPowerFactor,
         requiredValue = input.existingPowerFactor,
         message =
             "Target power factor must be greater than existing power factor."
     )
 }

 if (checks.any { it.status == EngineeringStatus.FAIL }) {
     return CapacitorBankResult(
         status = EngineeringStatus.FAIL,
         activePowerKW = input.activePowerKW,
         existingPowerFactor = input.existingPowerFactor,
         targetPowerFactor = input.targetPowerFactor,
         existingReactivePowerKVAR = 0.0,
         targetReactivePowerKVAR = 0.0,
         requiredCompensationKVAR = 0.0,
         checks = checks,
         trace = EngineeringTrace(
             calculationName = "CAPACITOR BANK",
             standard = null,
             checks = checks
         )
     )
 }

 val phiExisting =
     acos(input.existingPowerFactor)

 val phiTarget =
     acos(input.targetPowerFactor)

 val existingReactivePowerKVAR =
     input.activePowerKW * tan(phiExisting)

 val targetReactivePowerKVAR =
     input.activePowerKW * tan(phiTarget)

 val requiredCompensationKVAR =
     (
         existingReactivePowerKVAR -
                 targetReactivePowerKVAR
         ).coerceAtLeast(0.0)

 checks += EngineeringCheck(
     name = "Required reactive compensation",
     status = EngineeringStatus.PASS,
     calculatedValue = requiredCompensationKVAR,
     unit = "kVAr",
     message =
         "Required theoretical reactive compensation calculated."
 )

 return CapacitorBankResult(
     status = EngineeringStatus.PASS,
     activePowerKW = input.activePowerKW,
     existingPowerFactor = input.existingPowerFactor,
     targetPowerFactor = input.targetPowerFactor,
     existingReactivePowerKVAR = existingReactivePowerKVAR,
     targetReactivePowerKVAR = targetReactivePowerKVAR,
     requiredCompensationKVAR = requiredCompensationKVAR,
     checks = checks,
     trace = EngineeringTrace(
         calculationName = "CAPACITOR BANK",
         standard = null,
         checks = checks,
         assumptions = listOf(
             "Calculation assumes balanced sinusoidal fundamental-frequency conditions.",
             "The result is theoretical reactive compensation.",
             "Actual capacitor steps, detuning reactors and switching equipment require separate equipment selection."
         )
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

 if (!isPositiveFinite(input.earthResistanceOhm)) {
     checks += EngineeringCheck(
         name = "Earth resistance",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.earthResistanceOhm,
         unit = "Ω",
         message =
             "Earth resistance must be a finite value greater than zero."
     )
 }

 if (!isPositiveFinite(input.faultCurrentA)) {
     checks += EngineeringCheck(
         name = "Earth fault current",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.faultCurrentA,
         unit = "A",
         message =
             "Earth fault current must be a finite value greater than zero."
     )
 }

 if (!isPositiveFinite(input.permissibleTouchVoltageV)) {
     checks += EngineeringCheck(
         name = "Permissible touch voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.permissibleTouchVoltageV,
         unit = "V",
         message =
             "Permissible touch voltage must be a finite value greater than zero."
     )
 }

 if (checks.any { it.status == EngineeringStatus.FAIL }) {

     return EarthingResult(
         status = EngineeringStatus.FAIL,
         earthResistanceOhm = input.earthResistanceOhm,
         faultCurrentA = input.faultCurrentA,
         earthPotentialRiseV = 0.0,
         maximumResistanceOhm = 0.0,
         checks = checks,
         trace = EngineeringTrace(
             calculationName = "EARTHING CHECK",
             standard = null,
             checks = checks,
             warnings = listOf(
                 "Invalid earthing input must be corrected before calculation."
             )
         )
     )
 }

 val earthPotentialRiseV =
     input.earthResistanceOhm *
             input.faultCurrentA

 val maximumResistanceOhm =
     input.permissibleTouchVoltageV /
             input.faultCurrentA

 val resistancePass =
     input.earthResistanceOhm <=
             maximumResistanceOhm

 val resistanceCheck =
     EngineeringCheck(
         name = "Earth resistance safety check",
         status =
             if (resistancePass) {
                 EngineeringStatus.PASS
             } else {
                 EngineeringStatus.FAIL
             },
         calculatedValue =
             input.earthResistanceOhm,
         requiredValue =
             maximumResistanceOhm,
         unit = "Ω",
         message =
             if (resistancePass) {
                 "Earth resistance is within the maximum permissible value."
             } else {
                 "Earth resistance exceeds the maximum permissible value."
             }
     )

 checks += resistanceCheck

 val eprCheck =
     EngineeringCheck(
         name = "Earth Potential Rise",
         status = EngineeringStatus.PASS,
         calculatedValue = earthPotentialRiseV,
         unit = "V",
         message =
             "Earth Potential Rise calculated from earth resistance and earth fault current."
     )

 checks += eprCheck

 return EarthingResult(
     status =
         if (resistancePass) {
             EngineeringStatus.PASS
         } else {
             EngineeringStatus.FAIL
         },
     earthResistanceOhm =
         input.earthResistanceOhm,
     faultCurrentA =
         input.faultCurrentA,
     earthPotentialRiseV =
         earthPotentialRiseV,
     maximumResistanceOhm =
         maximumResistanceOhm,
     checks = checks,
     trace = EngineeringTrace(
         calculationName = "EARTHING CHECK",
         standard = null,
         checks = checks,
         assumptions = listOf(
             "Earth Potential Rise = Earth Resistance × Earth Fault Current.",
             "Maximum permissible earth resistance = Permissible Touch Voltage ÷ Earth Fault Current.",
             "Acceptance criterion is Earth Resistance ≤ Maximum Permissible Earth Resistance.",
             "This calculation verifies the supplied values and is not a complete earthing-system design study."
         )
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

 if (
     input.faultType !=
     ShortCircuitFaultType.THREE_PHASE
 ) {

     checks += EngineeringCheck(
         name = "Fault type",
         status = EngineeringStatus.DATA_REQUIRED,
         message =
             "The selected fault type is not implemented by the present calculation core."
     )

     return shortCircuitDataRequired(
         input,
         checks
     )
 }

 if (!isPositiveFinite(input.source.voltageV)) {

     checks += EngineeringCheck(
         name = "Fault calculation voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.source.voltageV,
         unit = "V",
         message =
             "Calculation voltage must be a finite value greater than zero."
     )
 }

 if (
     input.source.transformerKVA != null &&
     !isPositiveFinite(input.source.transformerKVA)
 ) {

     checks += EngineeringCheck(
         name = "Transformer rating",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.source.transformerKVA,
         unit = "kVA",
         message =
             "Transformer rating must be a finite value greater than zero."
     )
 }

 if (
     input.source.transformerImpedancePercent != null &&
     !isPositiveFinite(input.source.transformerImpedancePercent)
 ) {

     checks += EngineeringCheck(
         name = "Transformer impedance",
         status = EngineeringStatus.FAIL,
         calculatedValue =
             input.source.transformerImpedancePercent,
         unit = "%",
         message =
             "Transformer impedance must be a finite value greater than zero."
     )
 }

 if (
     input.source.transformerResistancePercent != null &&
     (
         !isValidFinite(input.source.transformerResistancePercent) ||
                 input.source.transformerResistancePercent < 0.0
         )
 ) {
     checks += EngineeringCheck(
         name = "Transformer resistance percent",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.source.transformerResistancePercent,
         unit = "%",
         message =
             "Transformer resistance percent must be a finite non-negative value."
     )
 }

 if (
     input.source.transformerReactancePercent != null &&
     (
         !isValidFinite(input.source.transformerReactancePercent) ||
                 input.source.transformerReactancePercent < 0.0
         )
 ) {
     checks += EngineeringCheck(
         name = "Transformer reactance percent",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.source.transformerReactancePercent,
         unit = "%",
         message =
             "Transformer reactance percent must be a finite non-negative value."
     )
 }

 if (
     input.source.upstreamShortCircuitKA != null &&
     !isPositiveFinite(input.source.upstreamShortCircuitKA)
 ) {

     checks += EngineeringCheck(
         name = "Upstream short-circuit level",
         status = EngineeringStatus.FAIL,
         calculatedValue =
             input.source.upstreamShortCircuitKA,
         unit = "kA",
         message =
             "Upstream short-circuit current must be a finite value greater than zero."
     )
 }

 if (
     input.source.upstreamResistanceOhm != null &&
     (
         !isValidFinite(input.source.upstreamResistanceOhm) ||
                 input.source.upstreamResistanceOhm < 0.0
         )
 ) {
     checks += EngineeringCheck(
         name = "Upstream resistance",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.source.upstreamResistanceOhm,
         unit = "Ω",
         message =
             "Upstream resistance must be a finite non-negative value."
     )
 }

 if (
     input.source.upstreamReactanceOhm != null &&
     (
         !isValidFinite(input.source.upstreamReactanceOhm) ||
                 input.source.upstreamReactanceOhm < 0.0
         )
 ) {
     checks += EngineeringCheck(
         name = "Upstream reactance",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.source.upstreamReactanceOhm,
         unit = "Ω",
         message =
             "Upstream reactance must be a finite non-negative value."
     )
 }

 if (checks.any { it.status == EngineeringStatus.FAIL }) {

     return ShortCircuitResult(
         status = EngineeringStatus.FAIL,
         faultCurrentA = 0.0,
         faultCurrentKA = 0.0,
         equivalentImpedance = null,
         checks = checks,
         trace = EngineeringTrace(
             calculationName = "SHORT CIRCUIT",
             standard = EngineeringStandards.shortCircuit,
             checks = checks
         )
     )
 }

 val cable =
     input.cable

 val hasDownstreamCable =
     cable != null

 /*
  * Transformer %Z alone cannot be combined with downstream
  * R/X feeder because %Z gives magnitude only.
  */
 if (
     hasDownstreamCable &&
     input.source.transformerKVA != null &&
     input.source.transformerImpedancePercent != null &&
     (
         input.source.transformerResistancePercent == null ||
                 input.source.transformerReactancePercent == null
         )
 ) {

     val dataCheck =
         EngineeringCheck(
             name = "Transformer R/X data",
             status = EngineeringStatus.DATA_REQUIRED,
             message =
                 "Transformer %Z alone is insufficient for a downstream complex-impedance calculation. Provide transformer R% and X% or equivalent source R/X data."
         )

     return shortCircuitDataRequired(
         input,
         checks + dataCheck
     )
 }

 if (
     hasDownstreamCable &&
     input.source.upstreamShortCircuitKA != null &&
     (
         input.source.upstreamResistanceOhm == null ||
                 input.source.upstreamReactanceOhm == null
         )
 ) {

     val dataCheck =
         EngineeringCheck(
             name = "Upstream R/X data",
             status = EngineeringStatus.DATA_REQUIRED,
             message =
                 "Upstream fault current alone gives impedance magnitude. Explicit upstream R/X data is required before adding downstream cable impedance."
         )

     return shortCircuitDataRequired(
         input,
         checks + dataCheck
     )
 }

 val sourceImpedance =
     calculateSourceImpedance(
         input.source
     )

 if (sourceImpedance == null) {

     val dataCheck =
         EngineeringCheck(
             name = "Source impedance data",
             status = EngineeringStatus.DATA_REQUIRED,
             message =
                 "A valid source impedance cannot be determined from the supplied data."
         )

     return shortCircuitDataRequired(
         input,
         checks + dataCheck
     )
 }

 var totalR =
     sourceImpedance.resistanceOhm

 var totalX =
     sourceImpedance.reactanceOhm

 if (cable != null) {

     if (
         !isPositiveFinite(cable.lengthM) ||
         cable.parallelRuns < 1 ||
         !isValidFinite(cable.resistanceOhmPerKm) ||
         !isValidFinite(cable.reactanceOhmPerKm) ||
         cable.resistanceOhmPerKm < 0.0 ||
         cable.reactanceOhmPerKm < 0.0
     ) {

         val fail =
             EngineeringCheck(
                 name = "Feeder impedance data",
                 status = EngineeringStatus.FAIL,
                 message =
                     "Cable length, parallel runs, resistance and reactance must contain valid engineering data."
             )

         return ShortCircuitResult(
             status = EngineeringStatus.FAIL,
             faultCurrentA = 0.0,
             faultCurrentKA = 0.0,
             equivalentImpedance = null,
             checks = checks + fail,
             trace = EngineeringTrace(
                 calculationName = "SHORT CIRCUIT",
                 standard = EngineeringStandards.shortCircuit,
                 checks = checks + fail
             )
         )
     }

     val cableR =
         cable.resistanceOhmPerKm *
                 cable.lengthM /
                 1000.0 /
                 cable.parallelRuns

     val cableX =
         cable.reactanceOhmPerKm *
                 cable.lengthM /
                 1000.0 /
                 cable.parallelRuns

     totalR += cableR
     totalX += cableX

     checks += EngineeringCheck(
         name = "Feeder resistance",
         status = EngineeringStatus.PASS,
         calculatedValue = cableR,
         unit = "Ω",
         message =
             "Feeder resistance added to equivalent source impedance."
     )

     checks += EngineeringCheck(
         name = "Feeder reactance",
         status = EngineeringStatus.PASS,
         calculatedValue = cableX,
         unit = "Ω",
         message =
             "Feeder reactance added to equivalent source impedance."
     )
 }

 val magnitude =
     sqrt(
         totalR * totalR +
                 totalX * totalX
     )

 if (
     !isPositiveFinite(magnitude)
 ) {

     val fail =
         EngineeringCheck(
             name = "Equivalent impedance",
             status = EngineeringStatus.FAIL,
             calculatedValue = magnitude,
             unit = "Ω",
             message =
                 "Equivalent impedance must be a finite value greater than zero."
         )

     return ShortCircuitResult(
         status = EngineeringStatus.FAIL,
         faultCurrentA = 0.0,
         faultCurrentKA = 0.0,
         equivalentImpedance = null,
         checks = checks + fail,
         trace = EngineeringTrace(
             calculationName = "SHORT CIRCUIT",
             standard = EngineeringStandards.shortCircuit,
             checks = checks + fail
         )
     )
 }

 val equivalent =
     ShortCircuitImpedance(
         resistanceOhm = totalR,
         reactanceOhm = totalX,
         magnitudeOhm = magnitude
     )

 /*
  * Present calculation:
  *
  * Ik = V / (sqrt(3) × |Z|)
  *
  * Full IEC 60909 maximum/minimum voltage-factor treatment
  * requires additional input fields in ShortCircuitInput.
  */
 val faultCurrentA =
     input.source.voltageV /
             (
                 sqrt(3.0) *
                         magnitude
                 )

 val faultCurrentKA =
     faultCurrentA / 1000.0

 if (!isPositiveFinite(faultCurrentA)) {

     val fail =
         EngineeringCheck(
             name = "Calculated short-circuit current",
             status = EngineeringStatus.FAIL,
             calculatedValue = faultCurrentA,
             unit = "A",
             message =
                 "Calculated short-circuit current is not a valid finite positive value."
         )

     return ShortCircuitResult(
         status = EngineeringStatus.FAIL,
         faultCurrentA = 0.0,
         faultCurrentKA = 0.0,
         equivalentImpedance = equivalent,
         checks = checks + fail,
         trace = EngineeringTrace(
             calculationName = "SHORT CIRCUIT",
             standard = EngineeringStandards.shortCircuit,
             checks = checks + fail
         )
     )
 }

 checks += EngineeringCheck(
     name = "Equivalent impedance",
     status = EngineeringStatus.PASS,
     calculatedValue = magnitude,
     unit = "Ω",
     message =
         "Equivalent positive-sequence impedance magnitude used for the three-phase fault calculation."
 )

 checks += EngineeringCheck(
     name = "Prospective short-circuit current",
     status = EngineeringStatus.PASS,
     calculatedValue = faultCurrentKA,
     unit = "kA",
     message =
         "Three-phase prospective short-circuit current calculated from supplied source and feeder impedance."
 )

 return ShortCircuitResult(
     status = EngineeringStatus.PASS,
     faultCurrentA = faultCurrentA,
     faultCurrentKA = faultCurrentKA,
     equivalentImpedance = equivalent,
     checks = checks,
     trace = EngineeringTrace(
         calculationName = "SHORT CIRCUIT",
         standard = EngineeringStandards.shortCircuit,
         checks = checks,
         assumptions = listOf(
             "Three-phase balanced fault calculation.",
             "Ik = V/(sqrt(3) × |Z|).",
             "Cable R and X are supplied by engineering data.",
             "Parallel identical feeder runs reduce R and X by the number of runs.",
             "No transformer R/X split is invented.",
             "Transformer %Z alone is not treated as known transformer R/X when downstream feeder impedance must be added.",
             "Upstream fault current alone is not treated as known upstream R/X when downstream feeder impedance must be added.",
             "Voltage-factor maximum/minimum cases require the corresponding input data and are not invented by this core.",
             "This calculation is not a complete unbalanced-fault or protection study."
         )
     )
 )
  
  }
  
  private fun calculateSourceImpedance(
  source: ShortCircuitSourceInput
  ): ShortCircuitImpedance? {
  
   // --------------------------------------------------------
 // EXPLICIT TRANSFORMER R% + X%
 // --------------------------------------------------------

 if (
     source.transformerKVA != null &&
     source.transformerResistancePercent != null &&
     source.transformerReactancePercent != null
 ) {

     if (
         !isPositiveFinite(source.transformerKVA) ||
         !isValidFinite(source.transformerResistancePercent) ||
         !isValidFinite(source.transformerReactancePercent) ||
         source.transformerResistancePercent < 0.0 ||
         source.transformerReactancePercent < 0.0
     ) {
         return null
     }

     val baseZ =
         source.voltageV *
                 source.voltageV /
                 (
                     source.transformerKVA *
                             1000.0
                     )

     val r =
         baseZ *
                 source.transformerResistancePercent /
                 100.0

     val x =
         baseZ *
                 source.transformerReactancePercent /
                 100.0

     val magnitude =
         sqrt(
             r * r +
                     x * x
         )

     if (!isPositiveFinite(magnitude)) {
         return null
     }

     return ShortCircuitImpedance(
         resistanceOhm = r,
         reactanceOhm = x,
         magnitudeOhm = magnitude
     )
 }

 // --------------------------------------------------------
 // TRANSFORMER %Z ONLY
 // --------------------------------------------------------

 if (
     source.transformerKVA != null &&
     source.transformerImpedancePercent != null
 ) {

     if (
         !isPositiveFinite(source.transformerKVA) ||
         !isPositiveFinite(source.transformerImpedancePercent)
     ) {
         return null
     }

     val baseZ =
         source.voltageV *
                 source.voltageV /
                 (
                     source.transformerKVA *
                             1000.0
                     )

     val z =
         baseZ *
                 source.transformerImpedancePercent /
                 100.0

     if (!isPositiveFinite(z)) {
         return null
     }

     /*
      * %Z gives impedance magnitude only.
      *
      * It is represented as X-only internally solely for
      * source-bus calculation compatibility.
      *
      * It is NOT treated as known transformer R/X data.
      */
     return ShortCircuitImpedance(
         resistanceOhm = 0.0,
         reactanceOhm = z,
         magnitudeOhm = z
     )
 }

 // --------------------------------------------------------
 // EXPLICIT UPSTREAM R/X
 // --------------------------------------------------------

 if (
     source.upstreamResistanceOhm != null &&
     source.upstreamReactanceOhm != null
 ) {

     if (
         !isValidFinite(source.upstreamResistanceOhm) ||
         !isValidFinite(source.upstreamReactanceOhm) ||
         source.upstreamResistanceOhm < 0.0 ||
         source.upstreamReactanceOhm < 0.0
     ) {
         return null
     }

     val r =
         source.upstreamResistanceOhm

     val x =
         source.upstreamReactanceOhm

     val magnitude =
         sqrt(
             r * r +
                     x * x
         )

     if (!isPositiveFinite(magnitude)) {
         return null
     }

     return ShortCircuitImpedance(
         resistanceOhm = r,
         reactanceOhm = x,
         magnitudeOhm = magnitude
     )
 }

 // --------------------------------------------------------
 // UPSTREAM FAULT LEVEL ONLY
 // --------------------------------------------------------

 if (
     source.upstreamShortCircuitKA != null
 ) {

     val ikA =
         source.upstreamShortCircuitKA

     if (
         !isPositiveFinite(ikA) ||
         !isPositiveFinite(source.voltageV)
     ) {
         return null
     }

     val z =
         source.voltageV /
                 (
                     sqrt(3.0) *
                             ikA *
                             1000.0
                     )

     if (!isPositiveFinite(z)) {
         return null
     }

     return ShortCircuitImpedance(
         resistanceOhm = 0.0,
         reactanceOhm = z,
         magnitudeOhm = z
     )
 }

 return null
  
  }
  
  private fun shortCircuitDataRequired(
  input: ShortCircuitInput,
  checks: List<EngineeringCheck>
  ): ShortCircuitResult {
  
   return ShortCircuitResult(
     status = EngineeringStatus.DATA_REQUIRED,
     faultCurrentA = 0.0,
     faultCurrentKA = 0.0,
     equivalentImpedance = null,
     checks = checks,
     trace = EngineeringTrace(
         calculationName = "SHORT CIRCUIT",
         standard = EngineeringStandards.shortCircuit,
         checks = checks,
         warnings = listOf(
             "Additional source impedance data is required before a professional downstream short-circuit calculation can be completed."
         )
     )
 )
  
  }
  
  // ============================================================
  // PROTECTION CHECK
  // ============================================================
  
  fun checkProtection(
  input: ProtectionCheckInput
  ): ProtectionCheckResult {
  
   val checks = mutableListOf<EngineeringCheck>()

 if (!isPositiveFinite(input.designCurrentA)) {

     checks += EngineeringCheck(
         name = "Design current Ib",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.designCurrentA,
         unit = "A",
         message =
             "Design current must be a finite value greater than zero."
     )
 }

 if (!isPositiveFinite(input.cableAmpacityA)) {

     checks += EngineeringCheck(
         name = "Cable ampacity Iz",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.cableAmpacityA,
         unit = "A",
         message =
             "Verified cable ampacity must be a finite value greater than zero."
     )
 }

 if (!isPositiveFinite(input.breakerRatedCurrentA)) {

     checks += EngineeringCheck(
         name = "Breaker rated current In",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.breakerRatedCurrentA,
         unit = "A",
         message =
             "Breaker rated current must be a finite value greater than zero."
     )
 }

 if (!isPositiveFinite(input.prospectiveShortCircuitKA)) {

     checks += EngineeringCheck(
         name = "Prospective short-circuit current Ik",
         status = EngineeringStatus.FAIL,
         calculatedValue =
             input.prospectiveShortCircuitKA,
         unit = "kA",
         message =
             "Prospective short-circuit current must be a finite value greater than zero."
     )
 }

 if (!isPositiveFinite(input.breakerIcuKA)) {

     checks += EngineeringCheck(
         name = "Breaker Icu",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.breakerIcuKA,
         unit = "kA",
         message =
             "Breaker Icu must be a finite value greater than zero."
     )
 }

 if (
     input.breakerIcsKA != null &&
     !isPositiveFinite(input.breakerIcsKA)
 ) {

     checks += EngineeringCheck(
         name = "Breaker Ics",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.breakerIcsKA,
         unit = "kA",
         message =
             "Breaker Ics must be a finite value greater than zero when supplied."
     )
 }

 if (checks.any { it.status == EngineeringStatus.FAIL }) {

     return ProtectionCheckResult(
         status = EngineeringStatus.FAIL,
         overloadProtectionPass = false,
         breakingCapacityPass = false,
         serviceBreakingCapacityPass = null,
         checks = checks,
         trace = EngineeringTrace(
             calculationName = "PROTECTION CHECK",
             standard = EngineeringStandards.circuitBreakers,
             checks = checks
         )
     )
 }

 /*
  * Basic coordination:
  *
  * Ib ≤ In ≤ Iz
  */
 val overloadPass =
     input.designCurrentA <=
             input.breakerRatedCurrentA &&
             input.breakerRatedCurrentA <=
             input.cableAmpacityA

 /*
  * Breaking capacity:
  *
  * Icu ≥ Ik
  */
 val breakingPass =
     input.breakerIcuKA >=
             input.prospectiveShortCircuitKA

 /*
  * Service breaking capacity:
  *
  * Ics ≥ Ik
  *
  * only when Ics data is supplied.
  */
 val servicePass =
     input.breakerIcsKA?.let {
         it >= input.prospectiveShortCircuitKA
     }

 checks += EngineeringCheck(
     name = "Ib ≤ In ≤ Iz",
     status =
         if (overloadPass) {
             EngineeringStatus.PASS
         } else {
             EngineeringStatus.FAIL
         },
     calculatedValue =
         input.breakerRatedCurrentA,
     requiredValue =
         input.cableAmpacityA,
     unit = "A",
     message =
         if (overloadPass) {
             "Basic current coordination passed."
         } else {
             "Basic current coordination failed."
         },
     standardCode =
         EngineeringStandards.circuitBreakers.code
 )

 checks += EngineeringCheck(
     name = "Icu ≥ Ik",
     status =
         if (breakingPass) {
             EngineeringStatus.PASS
         } else {
             EngineeringStatus.FAIL
         },
     calculatedValue =
         input.breakerIcuKA,
     requiredValue =
         input.prospectiveShortCircuitKA,
     unit = "kA",
     message =
         if (breakingPass) {
             "Ultimate breaking capacity is adequate."
         } else {
             "Ultimate breaking capacity is insufficient."
         },
     standardCode =
         EngineeringStandards.circuitBreakers.code
 )

 if (input.breakerIcsKA != null) {

     checks += EngineeringCheck(
         name = "Ics ≥ Ik",
         status =
             if (servicePass == true) {
                 EngineeringStatus.PASS
             } else {
                 EngineeringStatus.FAIL
             },
         calculatedValue =
             input.breakerIcsKA,
         requiredValue =
             input.prospectiveShortCircuitKA,
         unit = "kA",
         message =
             if (servicePass == true) {
                 "Service breaking capacity is adequate."
             } else {
                 "Service breaking capacity is insufficient."
             },
         standardCode =
             EngineeringStandards.circuitBreakers.code
     )
 }

 val overallPass =
     overloadPass &&
             breakingPass &&
             (
                 servicePass == null ||
                         servicePass
                 )

 return ProtectionCheckResult(
     status =
         if (overallPass) {
             EngineeringStatus.PASS
         } else {
             EngineeringStatus.FAIL
         },
     overloadProtectionPass =
         overloadPass,
     breakingCapacityPass =
         breakingPass,
     serviceBreakingCapacityPass =
         servicePass,
     checks = checks,
     trace = EngineeringTrace(
         calculationName = "PROTECTION CHECK",
         standard = EngineeringStandards.circuitBreakers,
         checks = checks,
         assumptions = listOf(
             "Basic protection coordination: Ib ≤ In ≤ Iz.",
             "Ultimate breaking capacity: Icu ≥ Ik.",
             "When Ics is supplied, service breaking capacity is checked against Ik.",
             "Discrimination/selectivity requires a separate study.",
             "Manufacturer data must be verified against the applicable catalog revision."
         )
     )
 )
  
  }
  }
