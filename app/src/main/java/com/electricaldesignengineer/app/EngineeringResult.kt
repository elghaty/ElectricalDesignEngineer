package com.electricaldesignengineer.app

enum class EngineeringStatus {
    PASS,
    FAIL,
    WARNING,
    NOT_CALCULATED,
    DATA_REQUIRED
}

data class EngineeringCheck(
    val name: String,
    val status: EngineeringStatus,
    val calculatedValue: Double? = null,
    val requiredValue: Double? = null,
    val unit: String = "",
    val message: String,
    val standardCode: String? = null,
    val dataSource: String? = null
)

data class EngineeringTrace(
    val calculationName: String,
    val standard: EngineeringStandards.StandardReference?,
    val checks: List<EngineeringCheck>,
    val assumptions: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {

    val status: EngineeringStatus
        get() = when {
            checks.any { it.status == EngineeringStatus.FAIL } ->
                EngineeringStatus.FAIL

            checks.any { it.status == EngineeringStatus.DATA_REQUIRED } ->
                EngineeringStatus.DATA_REQUIRED

            checks.any { it.status == EngineeringStatus.WARNING } ->
                EngineeringStatus.WARNING

            checks.isNotEmpty() &&
                    checks.all { it.status == EngineeringStatus.PASS } ->
                EngineeringStatus.PASS

            else ->
                EngineeringStatus.NOT_CALCULATED
        }
}
