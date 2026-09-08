package com.electricaldesignengineer.app

/**

* DistributionCalculator

* 

* Orchestrator only.

* 

* IMPORTANT:

* - Electrical calculations are delegated to ProfessionalEngineeringCore.

* - Equipment data is obtained from EngineeringCatalogRepository.

* - No cable, breaker, or transformer engineering tables are hardcoded here.
    */
    object DistributionCalculator {
  
  data class NodeCalculation(
  val nodeId: String,
  val nodeName: String,
  val nodeType: DistributionNodeType,
  val connectedKW: Double,
  val demandKW: Double,
  val demandKVA: Double,
  val currentA: Double,
  val loadingPercent: Double,
  val status: NodeStatus
  )
  
  fun calculate(
  system: DistributionSystem
  ): List<NodeCalculation> {
  
   val results =
     mutableMapOf<String, NodeCalculation>()

 system.hierarchyOrder()
     .asReversed()
     .forEach { node ->

         results[node.id] =
             calculateNode(
                 system = system,
                 node = node
             )
     }

 return system.hierarchyOrder()
     .mapNotNull { node ->
         results[node.id]
     }
  
  }
  
  private fun calculateNode(
  system: DistributionSystem,
  node: DistributionNode
  ): NodeCalculation {
  
   val accumulatedLoads =
     collectLoads(
         system = system,
         node = node
     )

 val phaseSystem =
     when (node.phaseType) {

         PhaseType.THREE_PHASE ->
             ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE

         PhaseType.SINGLE_PHASE_L1,
         PhaseType.SINGLE_PHASE_L2,
         PhaseType.SINGLE_PHASE_L3 ->
             ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
     }

 val systemInput =
     ProfessionalEngineeringCore.SystemInput(
         voltageV = node.voltage,
         frequencyHz = 50.0,
         phaseSystem = phaseSystem,
         powerFactor =
             determineSystemPowerFactor(
                 accumulatedLoads
             )
     )

 val coreResult =
     ProfessionalEngineeringCore.calculateLoads(
         loads = accumulatedLoads,
         system = systemInput
     )

 val connectedKW =
     coreResult.connectedKW

 val demandKW =
     coreResult.demandKW

 val demandKVA =
     coreResult.demandKVA

 val currentA =
     coreResult.currentA

 val loadingPercent =
     calculateLoading(
         node = node,
         demandKVA = demandKVA,
         currentA = currentA
     )

 val status =
     when {

         coreResult.checks.any {
             it.status == EngineeringStatus.FAIL
         } ->
             NodeStatus.ERROR

         loadingPercent > 100.0 ->
             NodeStatus.ERROR

         loadingPercent > 90.0 ->
             NodeStatus.WARNING

         else ->
             NodeStatus.CALCULATED
     }

 return NodeCalculation(
     nodeId = node.id,
     nodeName = node.name,
     nodeType = node.type,
     connectedKW = connectedKW,
     demandKW = demandKW,
     demandKVA = demandKVA,
     currentA = currentA,
     loadingPercent = loadingPercent,
     status = status
 )
  
  }
  
  /**
  
  * Collect own loads and all descendant loads.
    */
    private fun collectLoads(
    system: DistributionSystem,
    node: DistributionNode
    ): List<ProfessionalEngineeringCore.LoadInput> {
    
    val loads =
    mutableListOf<ProfessionalEngineeringCore.LoadInput>()
    
    addNodeLoads(
    node = node,
    target = loads
    )
    
    system.getChildren(node.id)
    .forEach { child ->
    
         loads +=
         collectLoads(
             system = system,
             node = child
         )
 }
    
    return loads
    }
  
  /**
  
  * Convert DistributionLoad into Core LoadInput.
    */
    private fun addNodeLoads(
    node: DistributionNode,
    target:
    MutableList<ProfessionalEngineeringCore.LoadInput>
    ) {
    
    node.loads.forEach { load ->
    
     target +=
     ProfessionalEngineeringCore.LoadInput(
         name = load.name,
         quantity = load.quantity.toDouble(),
         unitPowerKW = load.unitKW,
         demandFactor = load.demandFactor,
         powerFactor = load.powerFactor
     )
    
    }
    }
  
  /**
  
  * Provides a representative PF input to the Core.
  
  * 
  
  * The actual electrical calculation of effective PF
  
  * remains inside ProfessionalEngineeringCore.
    */
    private fun determineSystemPowerFactor(
    loads:
    List<ProfessionalEngineeringCore.LoadInput>
    ): Double {
    
    if (loads.isEmpty()) {
    return 1.0
    }
    
    val weightedPower =
    loads.sumOf { load ->
    
         load.quantity
         .coerceAtLeast(0.0) *
             load.unitPowerKW
                 .coerceAtLeast(0.0) *
             load.demandFactor
                 .coerceIn(0.0, 1.0)
 }
    
    if (weightedPower <= 0.0) {
    return 1.0
    }
    
    val weightedPF =
    loads.sumOf { load ->
    
         val quantity =
         load.quantity
             .coerceAtLeast(0.0)

     val power =
         load.unitPowerKW
             .coerceAtLeast(0.0)

     val demandFactor =
         load.demandFactor
             .coerceIn(0.0, 1.0)

     val pf =
         load.powerFactor
             .coerceIn(0.01, 1.0)

     quantity *
             power *
             demandFactor *
             pf
 } / weightedPower
    
    return weightedPF.coerceIn(
    0.01,
    1.0
    )
    }
  
  /**
  
  * Loading is an assessment/presentation value.
  
  * 
  
  * Transformer:
  
  * demand kVA / transformer kVA
  
  * 
  
  * Other nodes:
  
  * current / rated capacity
    */
    private fun calculateLoading(
    node: DistributionNode,
    demandKVA: Double,
    currentA: Double
    ): Double {
    
    if (node.ratedCapacity <= 0.0) {
    return 0.0
    }
    
    return when (node.type) {
    
     DistributionNodeType.TRANSFORMER -> {

     demandKVA /
             node.ratedCapacity *
             100.0
 }

 else -> {

     currentA /
             node.ratedCapacity *
             100.0
 }
    
    }
    }
  
  /**
  
  * Calculate transformer loading and recommend
  
  * the smallest VERIFIED transformer catalog item
  
  * capable of carrying the calculated demand.
  
  * 
  
  * No transformer ratings are hardcoded here.
    */
    fun transformerLoading(
    system: DistributionSystem
    ): TransformerLoadingResult? {
    
    val transformer =
    system.getTransformer()
    ?: return null
    
    val calculation =
    calculate(system)
    .firstOrNull {
    it.nodeId == transformer.id
    }
    ?: return null
    
    val demandKVA =
    calculation.demandKVA
    
    val transformerRating =
    transformer.ratedCapacity
    
    val loadingPercent =
    if (transformerRating > 0.0) {
    
         demandKVA /
             transformerRating *
             100.0

 } else {
     0.0
 }
    
    val recommendedTransformer =
    findRecommendedTransformer(
    demandKVA = demandKVA
    )
    
    val recommendedKVA =
    recommendedTransformer
    ?.ratedPowerKVA
    ?: 0.0
    
    return TransformerLoadingResult(
    transformerId = transformer.id,
    transformerName = transformer.name,
    transformerRatingKVA =
    transformerRating,
    demandKVA =
    demandKVA,
    loadingPercent =
    loadingPercent,
    recommendedKVA =
    recommendedKVA,
    overloaded =
    loadingPercent > 100.0,
    warning =
    loadingPercent > 85.0
    )
    }
  
  /**
  
  * Find the smallest verified transformer in the
  
  * engineering catalog whose rated power is not less
  
  * than the calculated demand.
  
  * 
  
  * The catalog is the source of transformer ratings.
    */
    private fun findRecommendedTransformer(
    demandKVA: Double
    ): EngineeringCatalogRepository.TransformerRecord? {
    
    if (demandKVA <= 0.0) {
    return null
    }
    
    return EngineeringCatalogRepository
    .searchTransformers(
    minimumKVA = demandKVA
    )
    .minByOrNull {
    it.ratedPowerKVA
    }
    }
    }

/**

* Transformer loading result.
  */
  data class TransformerLoadingResult(
  val transformerId: String,
  val transformerName: String,
  val transformerRatingKVA: Double,
  val demandKVA: Double,
  val loadingPercent: Double,
  val recommendedKVA: Double,
  val overloaded: Boolean,
  val warning: Boolean
  )
