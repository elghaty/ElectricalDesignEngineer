package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BreakerSelectionScreen(
    onBack: () -> Unit = {}
) {

    val project = ProjectManager.calculation

    val requiredPoles =
        if (project.isThreePhase) 4 else 2

    var designCurrentText by remember {
        mutableStateOf(
            if (project.designCurrentA > 0.0)
                "%.2f".format(project.designCurrentA)
            else
                ""
        )
    }

    var shortCircuitText by remember {
        mutableStateOf(
            if (project.shortCircuitKA > 0.0)
                "%.3f".format(project.shortCircuitKA)
            else
                ""
        )
    }

    var selectedBreaker by remember {
        mutableStateOf<ProfessionalEngineeringCore.BreakerData?>(null)
    }

    var resultText by remember {
        mutableStateOf("")
    }

    var showBreakerMenu by remember {
        mutableStateOf(false)
    }

    val availableBreakers =
        remember(requiredPoles) {
            EngineeringCatalogRepository
                .getBreakerData(requiredPoles)
                .sortedBy {
                    it.ratedCurrentA
                }
        }

    fun checkBreaker(
        breaker:
            ProfessionalEngineeringCore.BreakerData? = null
    ) {

        val ib =
            designCurrentText
                .toDoubleOrNull()
                ?: 0.0

        val icc =
            shortCircuitText
                .toDoubleOrNull()
                ?: 0.0

        val iz =
            ProjectManager
                .calculation
                .cableAmpacityA

        if (ib <= 0.0) {
            resultText =
                "DATA REQUIRED\n\nValid Design Current Ib is required."
            return
        }

        if (icc <= 0.0) {
            resultText =
                "DATA REQUIRED\n\nValid Short Circuit Current Icc is required.\n\nCalculate Short Circuit first."
            return
        }

        if (iz <= 0.0) {
            resultText =
                "DATA REQUIRED\n\nVerified cable ampacity Iz is required.\n\nCalculate Cable Selection first."
            return
        }

        val provider =
            object :
                ProfessionalEngineeringCore.BreakerDataProvider {

                override fun availableBreakers(
                    requiredPoles: Int
                ):
                    List<ProfessionalEngineeringCore.BreakerData> {

                    return if (breaker == null) {
                        EngineeringCatalogRepository
                            .getBreakerData(requiredPoles)
                    } else {
                        listOf(breaker)
                    }
                }
            }

        val calculation =
            ProfessionalEngineeringCore.designBreaker(

                input =
                    ProfessionalEngineeringCore.BreakerDesignInput(
                        designCurrentA = ib,
                        cableAmpacityA = iz,
                        prospectiveShortCircuitKA = icc,
                        requiredPoles = requiredPoles
                    ),

                provider = provider
            )

        val selected =
            calculation.selectedBreaker

        if (
            calculation.status ==
            EngineeringStatus.PASS &&
            selected != null
        ) {

            selectedBreaker =
                selected

            ProjectManager.setBreaker(

                breakerRatingA =
                    selected.ratedCurrentA.toInt(),

                breakerIcuKA =
                    selected.icuKA
            )
        }

        resultText =
            buildString {

                appendLine(
                    if (breaker == null)
                        "AUTOMATIC BREAKER SELECTION"
                    else
                        "MANUAL BREAKER CHECK"
                )

                appendLine()

                appendLine(
                    "Required Poles: $requiredPoles"
                )

                appendLine(
                    "Design Current Ib: %.2f A"
                        .format(ib)
                )

                appendLine(
                    "Cable Ampacity Iz: %.2f A"
                        .format(iz)
                )

                appendLine(
                    "Short Circuit Icc: %.3f kA"
                        .format(icc)
                )

                appendLine()

                if (selected != null) {

                    appendLine(
                        "SELECTED BREAKER"
                    )

                    appendLine()

                    appendLine(
                        "Manufacturer: ${
                            selected.manufacturerId
                                ?: "N/A"
                        }"
                    )

                    appendLine(
                        "Product Family: ${
                            selected.productFamily
                                ?: "N/A"
                        }"
                    )

                    appendLine(
                        "Part Number: ${
                            selected.partNumber
                                ?: "N/A"
                        }"
                    )

                    appendLine(
                        "Rated Current In: %.0f A"
                            .format(
                                selected.ratedCurrentA
                            )
                    )

                    appendLine(
                        "Icu: %.1f kA"
                            .format(
                                selected.icuKA
                            )
                    )

                    selected.icsKA?.let {

                        appendLine(
                            "Ics: %.1f kA"
                                .format(it)
                        )
                    }

                    appendLine()

                    appendLine(
                        "Source: ${selected.source}"
                    )

                    appendLine(
                        "Revision: ${selected.revision}"
                    )

                } else {

                    appendLine(
                        "NO SUITABLE BREAKER FOUND"
                    )
                }

                appendLine()

                appendLine(
                    "FINAL STATUS: ${
                        calculation.status
                    }"
                )

                calculation.checks
                    .forEach { check ->

                        appendLine()

                        appendLine(
                            "${check.name}: ${
                                check.status
                            }"
                        )

                        appendLine(
                            check.message
                        )
                    }
            }
    }

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)

    ) {

        Text(
            text = "Breaker Selection",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Text(
            text =
                "Project: ${
                    project.projectName.ifBlank {
                        "Current Project"
                    }
                }"
        )

        HorizontalDivider()

        Text(
            text = "Automatic Protection Design",
            style =
                MaterialTheme.typography.titleMedium
        )

        Text("Ib ≤ In ≤ Iz")
        Text("Icu ≥ Icc")

        HorizontalDivider()

        OutlinedTextField(

            value =
                designCurrentText,

            onValueChange = {
                designCurrentText = it
            },

            label = {
                Text("Design Current Ib (A)")
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        OutlinedTextField(

            value =
                shortCircuitText,

            onValueChange = {
                shortCircuitText = it
            },

            label = {
                Text("Short Circuit Icc (kA)")
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        Text(
            text =
                "Cable Ampacity Iz: %.2f A"
                    .format(
                        project.cableAmpacityA
                    )
        )

        Text(
            text =
                "Required Poles: $requiredPoles"
        )

        Button(

            onClick = {

                selectedBreaker = null

                checkBreaker()
            },

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text(
                "AUTO SELECT BREAKER"
            )
        }

        HorizontalDivider()

        Text(
            text = "Engineer Override",
            style =
                MaterialTheme.typography.titleMedium
        )

        Text(
            text =
                "The program selects the first verified breaker that satisfies Ib ≤ In ≤ Iz and Icu ≥ Icc. You may override it and the program will re-check it."
        )

        OutlinedButton(

            onClick = {
                showBreakerMenu =
                    !showBreakerMenu
            },

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text(

                if (selectedBreaker == null) {

                    "CHANGE BREAKER"

                } else {

                    "${selectedBreaker!!.ratedCurrentA.toInt()} A | " +
                            "Icu ${
                                "%.1f"
                                    .format(
                                        selectedBreaker!!.icuKA
                                    )
                            } kA"
                }
            )
        }

        DropdownMenu(

            expanded =
                showBreakerMenu,

            onDismissRequest = {
                showBreakerMenu = false
            }

        ) {

            if (availableBreakers.isEmpty()) {

                DropdownMenuItem(

                    text = {
                        Text(
                            "No verified breaker data available"
                        )
                    },

                    onClick = {
                        showBreakerMenu = false
                    }
                )

            } else {

                availableBreakers.forEach { breaker ->

                    DropdownMenuItem(

                        text = {

                            Text(

                                "${breaker.ratedCurrentA.toInt()} A | " +
                                        "Icu ${
                                            "%.1f"
                                                .format(
                                                    breaker.icuKA
                                                )
                                        } kA | " +
                                        (
                                            breaker.productFamily
                                                ?: "Unknown"
                                        )
                            )
                        },

                        onClick = {

                            selectedBreaker =
                                breaker

                            showBreakerMenu =
                                false

                            checkBreaker(
                                breaker
                            )
                        }
                    )
                }
            }
        }

        if (resultText.isNotBlank()) {

            HorizontalDivider()

            Text(

                text = resultText,

                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Current Project Protection",
            style =
                MaterialTheme.typography.titleMedium
        )

        Text(
            "Design Current: %.2f A"
                .format(
                    ProjectManager
                        .calculation
                        .designCurrentA
                )
        )

        Text(
            "Cable: %.1f mm²"
                .format(
                    ProjectManager
                        .calculation
                        .cableSizeMm2
                )
        )

        Text(
            "Cable Ampacity: %.2f A"
                .format(
                    ProjectManager
                        .calculation
                        .cableAmpacityA
                )
        )

        Text(
            "Short Circuit: %.3f kA"
                .format(
                    ProjectManager
                        .calculation
                        .shortCircuitKA
                )
        )

        Text(
            "Breaker: %d A"
                .format(
                    ProjectManager
                        .calculation
                        .breakerRatingA
                )
        )

        Text(
            "Breaker Icu: %.1f kA"
                .format(
                    ProjectManager
                        .calculation
                        .breakerIcuKA
                )
        )

        Text(
            "Design Status: ${
                ProjectManager
                    .calculation
                    .designStatus
            }"
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Button(

            onClick = onBack,

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text("Back")
        }
    }
}
