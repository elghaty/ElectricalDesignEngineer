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
     return LoadCalculationResult(
         connectedKW = 0.0,
         demandKW = 0.0,
         demandKVA = 0.0,
         currentA = 0.0,
         effectivePowerFactor = 1.0,
         checks = emptyList(),
         trace = EngineeringTrace(
             calculationName = "LOAD CALCULATION",
             standard = null,
             checks = emptyList(),
             warnings = listOf(
                 "No loads have been entered."
             )
         )
     )
 }

 if (system.voltageV <= 0.0) {
     checks += EngineeringCheck(
         name = "System voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = system.voltageV,
         unit = "V",
         message = "System voltage must be greater than zero."
     )
 }

 if (system.frequencyHz <= 0.0) {
     checks += EngineeringCheck(
         name = "Frequency",
         status = EngineeringStatus.FAIL,
         calculatedValue = system.frequencyHz,
         unit = "Hz",
         message = "Frequency must be greater than zero."
     )
 }

 if (system.powerFactor !in 0.01..1.0) {
     checks += EngineeringCheck(
         name = "Power factor",
         status = EngineeringStatus.FAIL,
         calculatedValue = system.powerFactor,
         message = "Power factor must be between 0.01 and 1.00."
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

     if (load.quantity <= 0.0) {
         checks += EngineeringCheck(
             name = "Load quantity: ${load.name}",
             status = EngineeringStatus.FAIL,
             calculatedValue = load.quantity,
             message = "Load quantity must be greater than zero."
         )
     }

     if (load.unitPowerKW <= 0.0) {
         checks += EngineeringCheck(
             name = "Unit power: ${load.name}",
             status = EngineeringStatus.FAIL,
             calculatedValue = load.unitPowerKW,
             unit = "kW",
             message = "Unit power must be greater than zero."
         )
     }

     if (load.demandFactor !in 0.0..1.0) {
         checks += EngineeringCheck(
             name = "Demand factor: ${load.name}",
             status = EngineeringStatus.FAIL,
             calculatedValue = load.demandFactor,
             message = "Demand factor must be between 0.00 and 1.00."
         )
     }

     if (load.powerFactor !in 0.01..1.0) {
         checks += EngineeringCheck(
             name = "Power factor: ${load.name}",
             status = EngineeringStatus.FAIL,
             calculatedValue = load.powerFactor,
             message = "Power factor must be between 0.01 and 1.00."
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
                 "Invalid load data must be corrected before engineering calculation."
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
             "Demand factor is applied to connected active power."
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
  
   if (kva <= 0.0 || voltageV <= 0.0) {
     return 0.0
 }

 return kva * 1000.0 /
         (sqrt(3.0) * voltageV)
  
  }
  
  fun singlePhaseCurrent(
  kva: Double,
  voltageV: Double
  ): Double {
  
   if (kva <= 0.0 || voltageV <= 0.0) {
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
  val maximumParallelRuns: Int = 8
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

 if (input.designCurrentA <= 0.0) {
     fail(
         "Design current",
         input.designCurrentA,
         "A",
         "Design current must be greater than zero."
     )
 }

 if (input.lengthM <= 0.0) {
     fail(
         "Cable length",
         input.lengthM,
         "m",
         "Cable length must be greater than zero."
     )
 }

 if (input.voltageV <= 0.0) {
     fail(
         "Voltage",
         input.voltageV,
         "V",
         "Voltage must be greater than zero."
     )
 }

 if (input.powerFactor !in 0.01..1.0) {
     fail(
         "Power factor",
         input.powerFactor,
         "",
         "Power factor must be between 0.01 and 1.00."
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

 if (input.ambientTemperatureC.isNaN()) {
     fail(
         "Ambient temperature",
         null,
         "°C",
         "Ambient temperature must be a valid value."
     )
 }

 if (input.maximumVoltageDropPercent <= 0.0) {
     fail(
         "Maximum voltage drop",
         input.maximumVoltageDropPercent,
         "%",
         "Voltage-drop limit must be greater than zero."
     )
 }

 if (
     input.groupingFactor <= 0.0 ||
     input.groupingFactor > 1.0
 ) {
     fail(
         "Grouping factor",
         input.groupingFactor,
         "",
         "Grouping factor must be greater than 0 and not greater than 1."
     )
 }

 if (
     input.thermalInsulationFactor <= 0.0 ||
     input.thermalInsulationFactor > 1.0
 ) {
     fail(
         "Thermal insulation factor",
         input.thermalInsulationFactor,
         "",
         "Thermal insulation factor must be greater than 0 and not greater than 1."
     )
 }

 if (
     input.soilCorrectionFactor <= 0.0 ||
     input.soilCorrectionFactor > 1.0
 ) {
     fail(
         "Soil correction factor",
         input.soilCorrectionFactor,
         "",
         "Soil correction factor must be greater than 0 and not greater than 1."
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
             it.sizeMm2 > 0.0 &&
                     it.baseAmpacityA > 0.0 &&
                     it.resistanceOhmPerKm >= 0.0 &&
                     it.reactanceOhmPerKm >= 0.0
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
  * Correction factors are explicit engineering inputs.
  *
  * Temperature/grouping/construction correction values must
  * come from the applicable standard/catalog data.
  *
  * The core does not invent correction factors.
  */
 val correctionFactor =
     input.groupingFactor *
             input.thermalInsulationFactor *
             input.soilCorrectionFactor

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
                 listOf(
                     ampacityCheck,
                     voltageDropCheck
                 )

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
                     assumptions = listOf(
                         "Correction factors are supplied explicitly.",
                         "Cable electrical characteristics are supplied by the engineering data provider.",
                         "No generic manufacturer data is invented."
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
         checks = listOf(finalCheck)
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
  val requiredPoles: Int
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

 if (input.designCurrentA <= 0.0) {
     checks += EngineeringCheck(
         name = "Design current",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.designCurrentA,
         unit = "A",
         message = "Design current must be greater than zero."
     )
 }

 if (input.cableAmpacityA <= 0.0) {
     checks += EngineeringCheck(
         name = "Cable ampacity",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.cableAmpacityA,
         unit = "A",
         message = "Verified cable ampacity is required."
     )
 }

 if (input.prospectiveShortCircuitKA < 0.0) {
     checks += EngineeringCheck(
         name = "Short-circuit current",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.prospectiveShortCircuitKA,
         unit = "kA",
         message = "Short-circuit current cannot be negative."
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

 val breakers =
     provider.availableBreakers(input.requiredPoles)
         .filter {
             it.poles >= input.requiredPoles
         }
         .filter {
             it.ratedCurrentA > 0.0
         }
         .filter {
             it.icuKA > 0.0
         }
         .sortedBy {
             it.ratedCurrentA
         }

 if (breakers.isEmpty()) {

     val dataCheck =
         EngineeringCheck(
             name = "Breaker engineering database",
             status = EngineeringStatus.DATA_REQUIRED,
             message = "No verified breaker product data is available."
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

     if (currentCoordination && breakingCapacity) {

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

         return BreakerDesignResult(
             status = EngineeringStatus.PASS,
             selectedBreaker = breaker,
             checks = listOf(
                 currentCheck,
                 icuCheck
             ),
             trace = EngineeringTrace(
                 calculationName = "BREAKER DESIGN",
                 standard = EngineeringStandards.circuitBreakers,
                 checks = listOf(
                     currentCheck,
                     icuCheck
                 ),
                 assumptions = listOf(
                     "Discrimination/selectivity is a separate study.",
                     "Breaker voltage compatibility must be verified against the system voltage and manufacturer catalog."
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
             "No verified breaker product satisfies the specified current and short-circuit requirements.",
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
  
  * Calculates transformer required capacity.
  
  * 
  
  * Required kVA = Demand kW / PF × (1 + margin)
  
  * 
  
  * This function performs calculation only.
  
  * Actual transformer selection is performed by designTransformer()
  
  * using verified catalog data.
    */
    fun requiredTransformerKVA(
    demandKW: Double,
    powerFactor: Double,
    designMarginPercent: Double = 0.0
    ): TransformerRequiredKVAResult {
    
    val checks = mutableListOf<EngineeringCheck>()
    
    if (demandKW <= 0.0) {
    checks += EngineeringCheck(
    name = "Transformer demand",
    status = EngineeringStatus.FAIL,
    calculatedValue = demandKW,
    unit = "kW",
    message = "Demand power must be greater than zero."
    )
    }
    
    if (powerFactor !in 0.01..1.0) {
    checks += EngineeringCheck(
    name = "Transformer power factor",
    status = EngineeringStatus.FAIL,
    calculatedValue = powerFactor,
    message = "Power factor must be between 0.01 and 1.00."
    )
    }
    
    if (designMarginPercent < 0.0) {
    checks += EngineeringCheck(
    name = "Transformer design margin",
    status = EngineeringStatus.FAIL,
    calculatedValue = designMarginPercent,
    unit = "%",
    message = "Design margin cannot be negative."
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
    "Required transformer capacity calculated from demand power, power factor and design margin.",
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
    "Design margin is explicitly supplied by the engineer.",
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

 if (input.requiredKVA <= 0.0) {
     checks += EngineeringCheck(
         name = "Required transformer capacity",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredKVA,
         unit = "kVA",
         message =
             "Required transformer capacity must be greater than zero."
     )
 }

 if (input.designMarginPercent < 0.0) {
     checks += EngineeringCheck(
         name = "Design margin",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.designMarginPercent,
         unit = "%",
         message = "Design margin cannot be negative."
     )
 }

 if (
     input.requiredPrimaryVoltageV != null &&
     input.requiredPrimaryVoltageV <= 0.0
 ) {
     checks += EngineeringCheck(
         name = "Primary voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredPrimaryVoltageV,
         unit = "V",
         message =
             "Required primary voltage must be greater than zero."
     )
 }

 if (
     input.requiredSecondaryVoltageV != null &&
     input.requiredSecondaryVoltageV <= 0.0
 ) {
     checks += EngineeringCheck(
         name = "Secondary voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredSecondaryVoltageV,
         unit = "V",
         message =
             "Required secondary voltage must be greater than zero."
     )
 }

 if (
     input.requiredFrequencyHz != null &&
     input.requiredFrequencyHz <= 0.0
 ) {
     checks += EngineeringCheck(
         name = "Transformer frequency",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.requiredFrequencyHz,
         unit = "Hz",
         message =
             "Required frequency must be greater than zero."
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

 val requiredWithMargin =
     input.requiredKVA *
             (1.0 + input.designMarginPercent / 100.0)

 val candidates =
     transformers
         .filter { it.verified }
         .filter {
             it.ratedPowerKVA >= requiredWithMargin
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
             requiredValue = requiredWithMargin,
             unit = "kVA",
             message =
                 "No verified transformer in the engineering catalog satisfies the required capacity and voltage/frequency constraints.",
             standardCode =
                 EngineeringStandards.transformer.code
         )

     return TransformerDesignResult(
         status = EngineeringStatus.DATA_REQUIRED,
         selectedTransformer = null,
         requiredKVA = requiredWithMargin,
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
         requiredValue = requiredWithMargin,
         unit = "kVA",
         message =
             "Selected transformer capacity satisfies the required design capacity.",
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
     requiredKVA = requiredWithMargin,
     checks = checks,
     trace = EngineeringTrace(
         calculationName = "TRANSFORMER DESIGN",
         standard = EngineeringStandards.transformer,
         checks = checks,
         assumptions = listOf(
             "Only verified transformer catalog records are eligible.",
             "The smallest verified transformer satisfying the design requirement is selected.",
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
  
  /**
  
  * Generator sizing formula:
  
  * 
  
  * Base kVA = demand kW / PF
  
  * 
  
  * Motor adjusted kVA =
  
  * Base kVA × (1 + motor allowance)
  
  * 
  
  * Required generator kVA =
  
  * Motor adjusted kVA / allowable loading
  
  * 
  
  * Optional design margin is then applied.
  
  * 
  
  * No manufacturer rating list is hardcoded here.
    */
    fun designGenerator(
    input: GeneratorDesignInput,
    generators: List<GeneratorData>
    ): GeneratorDesignResult {
    
    val checks = mutableListOf<EngineeringCheck>()
    
    if (input.demandKW <= 0.0) {
    checks += EngineeringCheck(
    name = "Generator demand",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.demandKW,
    unit = "kW",
    message = "Generator demand must be greater than zero."
    )
    }
    
    if (input.powerFactor !in 0.01..1.0) {
    checks += EngineeringCheck(
    name = "Generator power factor",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.powerFactor,
    message = "Power factor must be between 0.01 and 1.00."
    )
    }
    
    if (
    input.loadingPercent <= 0.0 ||
    input.loadingPercent > 100.0
    ) {
    checks += EngineeringCheck(
    name = "Generator loading",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.loadingPercent,
    unit = "%",
    message =
    "Generator allowable loading must be greater than 0 and not greater than 100%."
    )
    }
    
    if (input.motorAllowancePercent < 0.0) {
    checks += EngineeringCheck(
    name = "Motor allowance",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.motorAllowancePercent,
    unit = "%",
    message = "Motor allowance cannot be negative."
    )
    }
    
    if (input.designMarginPercent < 0.0) {
    checks += EngineeringCheck(
    name = "Generator design margin",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.designMarginPercent,
    unit = "%",
    message = "Design margin cannot be negative."
    )
    }
    
    if (
    input.requiredVoltageV != null &&
    input.requiredVoltageV <= 0.0
    ) {
    checks += EngineeringCheck(
    name = "Generator voltage",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.requiredVoltageV,
    unit = "V",
    message =
    "Required generator voltage must be greater than zero."
    )
    }
    
    if (
    input.requiredFrequencyHz != null &&
    input.requiredFrequencyHz <= 0.0
    ) {
    checks += EngineeringCheck(
    name = "Generator frequency",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.requiredFrequencyHz,
    unit = "Hz",
    message =
    "Required generator frequency must be greater than zero."
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
    
    val candidates =
    generators
    .filter { it.verified }
    .filter {
    it.ratedPowerKVA >= requiredGeneratorKVA
    }
    .filter {
    input.requiredVoltageV == null ||
    it.ratedVoltageV == null ||
    abs(
    it.ratedVoltageV -
    input.requiredVoltageV
    ) < 0.01
    }
    .filter {
    input.requiredFrequencyHz == null ||
    it.frequencyHz == null ||
    abs(
    it.frequencyHz -
    input.requiredFrequencyHz
    ) < 0.01
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
             "No verified generator catalog record satisfies the calculated requirement."
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
             "Verified generator manufacturer/catalog data is required."
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
    "Motor allowance and generator loading are explicit engineering inputs."
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
  
  /**
  
  * Power-factor correction:
  
  * 
  
  * Qc = P × (tan φ1 - tan φ2)
  
  * 
  
  * where:
  
  * 
  
  * φ1 = acos(existing PF)
  
  * φ2 = acos(target PF)
  
  * 
  
  * P is active power in kW.
  
  * 
  
  * Result is the theoretical reactive compensation required in kVAr.
  
  * 
  
  * Actual capacitor bank step selection is a separate equipment
  
  * selection stage and must use verified manufacturer/catalog data.
    */
    fun calculateCapacitorBank(
    input: CapacitorBankInput
    ): CapacitorBankResult {
    
    val checks = mutableListOf<EngineeringCheck>()
    
    if (input.activePowerKW <= 0.0) {
    checks += EngineeringCheck(
    name = "Active power",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.activePowerKW,
    unit = "kW",
    message =
    "Active power must be greater than zero."
    )
    }
    
    if (input.existingPowerFactor !in 0.01..1.0) {
    checks += EngineeringCheck(
    name = "Existing power factor",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.existingPowerFactor,
    message =
    "Existing power factor must be between 0.01 and 1.00."
    )
    }
    
    if (input.targetPowerFactor !in 0.01..1.0) {
    checks += EngineeringCheck(
    name = "Target power factor",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.targetPowerFactor,
    message =
    "Target power factor must be between 0.01 and 1.00."
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
  
  /**
  
  * Earthing verification.
  
  * 
  
  * Earth Potential Rise:
  
  * 
  
  * EPR = Rearth × Ifault
  
  * 
  
  * Maximum permissible earth resistance:
  
  * 
  
  * Rmax = Vtouch / Ifault
  
  * 
  
  * Acceptance criterion:
  
  * 
  
  * Rearth ≤ Rmax
  
  * 
  
  * This preserves the calculation used by the legacy
  
  * ElectricalCalculator.earthCheck() implementation.
  
  * 
  
  * IMPORTANT
  
  * ---
  
  * This function verifies the supplied earth resistance against
  
  * the supplied permissible touch voltage and fault current.
  
  * 
  
  * It does not invent an earth resistance value and does not
  
  * replace the complete earthing-system design, touch/step
  
  * voltage study, soil model, electrode design, or applicable
  
  * protection/disconnection study.
    */
    fun calculateEarthing(
    input: EarthingInput
    ): EarthingResult {
    
    val checks = mutableListOf<EngineeringCheck>()
    
    if (input.earthResistanceOhm <= 0.0) {
    checks += EngineeringCheck(
    name = "Earth resistance",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.earthResistanceOhm,
    unit = "Ω",
    message =
    "Earth resistance must be greater than zero."
    )
    }
    
    if (input.faultCurrentA <= 0.0) {
    checks += EngineeringCheck(
    name = "Earth fault current",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.faultCurrentA,
    unit = "A",
    message =
    "Earth fault current must be greater than zero."
    )
    }
    
    if (input.permissibleTouchVoltageV <= 0.0) {
    checks += EngineeringCheck(
    name = "Permissible touch voltage",
    status = EngineeringStatus.FAIL,
    calculatedValue = input.permissibleTouchVoltageV,
    unit = "V",
    message =
    "Permissible touch voltage must be greater than zero."
    )
    }
    
    if (checks.any { it.status == EngineeringStatus.FAIL }) {
    
     return EarthingResult(
     status = EngineeringStatus.FAIL,
     earthResistanceOhm =
         input.earthResistanceOhm,
     faultCurrentA =
         input.faultCurrentA,
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
    
    /*
    
    * Legacy engineering calculation preserved exactly:
    * 
    * EPR = R × I
      */
      val earthPotentialRiseV =
      input.earthResistanceOhm *
      input.faultCurrentA
    
    /*
    
    * Maximum permissible earth resistance:
    * 
    * Rmax = Vtouch / Ifault
      */
      val maximumResistanceOhm =
      input.permissibleTouchVoltageV /
      input.faultCurrentA
    
    /*
    
    * Acceptance:
    * 
    * Rearth ≤ Rmax
      */
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

 if (input.source.voltageV <= 0.0) {

     checks += EngineeringCheck(
         name = "Fault calculation voltage",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.source.voltageV,
         unit = "V",
         message =
             "Calculation voltage must be greater than zero."
     )
 }

 if (
     input.source.transformerKVA != null &&
     input.source.transformerKVA <= 0.0
 ) {

     checks += EngineeringCheck(
         name = "Transformer rating",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.source.transformerKVA,
         unit = "kVA",
         message =
             "Transformer rating must be greater than zero."
     )
 }

 if (
     input.source.transformerImpedancePercent != null &&
     input.source.transformerImpedancePercent <= 0.0
 ) {

     checks += EngineeringCheck(
         name = "Transformer impedance",
         status = EngineeringStatus.FAIL,
         calculatedValue =
             input.source.transformerImpedancePercent,
         unit = "%",
         message =
             "Transformer impedance must be greater than zero."
     )
 }

 if (
     input.source.upstreamShortCircuitKA != null &&
     input.source.upstreamShortCircuitKA <= 0.0
 ) {

     checks += EngineeringCheck(
         name = "Upstream short-circuit level",
         status = EngineeringStatus.FAIL,
         calculatedValue =
             input.source.upstreamShortCircuitKA,
         unit = "kA",
         message =
             "Upstream short-circuit current must be greater than zero."
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

 /*
  * Transformer %Z or upstream fault current alone gives
  * impedance magnitude only.
  *
  * It must not be combined with downstream cable R/X
  * unless an actual R/X split is available.
  */

 val hasDownstreamCable =
     cable != null

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
         cable.lengthM <= 0.0 ||
         cable.parallelRuns < 1 ||
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

 if (magnitude <= 0.0) {

     val fail =
         EngineeringCheck(
             name = "Equivalent impedance",
             status = EngineeringStatus.FAIL,
             calculatedValue = magnitude,
             unit = "Ω",
             message =
                 "Equivalent impedance must be greater than zero."
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

 val faultCurrentA =
     input.source.voltageV /
             (
                 sqrt(3.0) *
                         magnitude
                 )

 val faultCurrentKA =
     faultCurrentA / 1000.0

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
             "No transformer R/X split is invented."
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
         source.transformerKVA <= 0.0 ||
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

     return ShortCircuitImpedance(
         resistanceOhm = r,
         reactanceOhm = x,
         magnitudeOhm =
             sqrt(
                 r * r +
                         x * x
             )
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
         source.transformerKVA <= 0.0 ||
         source.transformerImpedancePercent <= 0.0
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

     /*
      * For a source-bus calculation, %Z gives the impedance
      * magnitude. We represent it as X-only internally because
      * no R/X split has been supplied.
      *
      * This representation must not be used to pretend that
      * actual transformer R/X is known downstream.
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
         source.upstreamResistanceOhm < 0.0 ||
         source.upstreamReactanceOhm < 0.0
     ) {
         return null
     }

     val r =
         source.upstreamResistanceOhm

     val x =
         source.upstreamReactanceOhm

     return ShortCircuitImpedance(
         resistanceOhm = r,
         reactanceOhm = x,
         magnitudeOhm =
             sqrt(
                 r * r +
                         x * x
             )
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

     if (ikA <= 0.0) {
         return null
     }

     val z =
         source.voltageV /
                 (
                     sqrt(3.0) *
                             ikA *
                             1000.0
                     )

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

 if (input.designCurrentA <= 0.0) {

     checks += EngineeringCheck(
         name = "Design current Ib",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.designCurrentA,
         unit = "A",
         message =
             "Design current must be greater than zero."
     )
 }

 if (input.cableAmpacityA <= 0.0) {

     checks += EngineeringCheck(
         name = "Cable ampacity Iz",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.cableAmpacityA,
         unit = "A",
         message =
             "Verified cable ampacity is required."
     )
 }

 if (input.breakerRatedCurrentA <= 0.0) {

     checks += EngineeringCheck(
         name = "Breaker rated current In",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.breakerRatedCurrentA,
         unit = "A",
         message =
             "Breaker rated current must be greater than zero."
     )
 }

 if (input.prospectiveShortCircuitKA < 0.0) {

     checks += EngineeringCheck(
         name = "Prospective short-circuit current Ik",
         status = EngineeringStatus.FAIL,
         calculatedValue =
             input.prospectiveShortCircuitKA,
         unit = "kA",
         message =
             "Prospective short-circuit current cannot be negative."
     )
 }

 if (input.breakerIcuKA <= 0.0) {

     checks += EngineeringCheck(
         name = "Breaker Icu",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.breakerIcuKA,
         unit = "kA",
         message =
             "Breaker Icu must be greater than zero."
     )
 }

 if (
     input.breakerIcsKA != null &&
     input.breakerIcsKA < 0.0
 ) {

     checks += EngineeringCheck(
         name = "Breaker Ics",
         status = EngineeringStatus.FAIL,
         calculatedValue = input.breakerIcsKA,
         unit = "kA",
         message =
             "Breaker Ics cannot be negative."
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
             "This is a basic protection coordination check.",
             "Discrimination/selectivity requires a separate study.",
             "Manufacturer data must be verified against the applicable catalog revision."
         )
     )
 )
  
  }
  }
