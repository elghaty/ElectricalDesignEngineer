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

    var current by remember {
        mutableStateOf(
            if (project.designCurrentA > 0.0) {
                "%.2f".format(
                    project.designCurrentA
                )
            } else {
                ""
            }
        )
    }

    var faultCurrent by remember {
        mutableStateOf(
            if (project.shortCircuitKA > 0.0) {
                "%.2f".format(
                    project.shortCircuitKA
                )
            } else {
                ""
            }
        )
    }

    var selectedBreaker by remember {
        mutableStateOf<Int?>(
            if (project.breakerRatingA > 0) {
                project.breakerRatingA
            } else {
                null
            }
        )
    }

    var selectedIcu by remember {
        mutableStateOf<Double?>(
            if (project.breakerIcuKA > 0.0) {
                project.breakerIcuKA
            } else {
                null
            }
        )
    }

    var showBreakerMenu by remember {
        mutableStateOf(false)
    }

    var showIcuMenu by remember {
        mutableStateOf(false)
    }

    var result by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
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
            text = "Project: ${
                project.projectName.ifBlank {
                    "Current Project"
                }
            }",
            style =
                MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text(
            text = "Protection Design",
            style =
                MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Protection sequence:",
            style =
                MaterialTheme.typography.bodyMedium
        )

        Text(
            text =
                "Ib ≤ In ≤ Iz",
            style =
                MaterialTheme.typography.titleMedium
        )

        Text(
            text =
                "Icu ≥ Icc",
            style =
                MaterialTheme.typography.titleMedium
        )

        HorizontalDivider()

        // =========================
        // DESIGN CURRENT
        // =========================

        OutlinedTextField(
            value = current,
            onValueChange = {
                current = it
            },
            label = {
                Text(
                    "Design Current Ib (A)"
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        // =========================
        // SHORT CIRCUIT
        // =========================

        OutlinedTextField(
            value = faultCurrent,
            onValueChange = {
                faultCurrent = it
            },
            label = {
                Text(
                    "Short Circuit Icc (kA)"
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        // =========================
        // CABLE AMPACITY
        // =========================

        Text(
            text =
                "Selected Cable Iz: %.2f A".format(
                    project.cableAmpacityA
                ),
            style =
                MaterialTheme.typography.bodyLarge
        )

        Text(
            text =
                if (project.cableSizeMm2 > 0.0) {
                    "Cable Size: %.1f mm²".format(
                        project.cableSizeMm2
                    )
                } else {
                    "Cable: NOT SELECTED"
                },
            style =
                MaterialTheme.typography.bodyMedium
        )

        // =========================
        // AUTO
        // =========================

        Button(
            onClick = {

                val i =
                    current
                        .toDoubleOrNull()
                        ?: 0.0

                val isc =
                    faultCurrent
                        .toDoubleOrNull()
                        ?: 0.0

                val iz =
                    ProjectManager
                        .calculation
                        .cableAmpacityA

                if (
                    i <= 0.0 ||
                    isc <= 0.0
                ) {

                    result =
                        "Please calculate Load and Short Circuit first."

                } else if (
                    iz <= 0.0
                ) {

                    result =
                        "Please select and calculate a cable first."

                } else {

                    val breaker =
                        ElectricalCalculator
                            .selectBreaker(
                                currentA = i,
                                faultCurrentKA = isc,
                                cableAmpacityA = iz
                            )

                    if (breaker.success) {

                        selectedBreaker =
                            breaker.ratingA

                        selectedIcu =
                            breaker.icuKA

                        ProjectManager.setBreaker(
                            breakerRatingA =
                                breaker.ratingA,
                            breakerIcuKA =
                                breaker.icuKA
                        )

                    } else {

                        selectedBreaker =
                            if (
                                breaker.ratingA > 0
                            ) {
                                breaker.ratingA
                            } else {
                                null
                            }

                        selectedIcu = null
                    }

                    result =
                        """
                        AUTOMATIC BREAKER SELECTION

                        Design Current Ib:
                        %.2f A

                        Cable Ampacity Iz:
                        %.2f A

                        Recommended Breaker In:
                        %d A

                        Short Circuit Icc:
                        %.2f kA

                        Required Icu:
                        %.1f kA

                        Protection Check:
                        Ib ≤ In ≤ Iz

                        Icu ≥ Icc

                        FINAL STATUS:
                        %s
                        """.trimIndent()
                            .format(
                                i,
                                iz,
                                breaker.ratingA,
                                isc,
                                breaker.icuKA,
                                breaker.status
                            )
                }
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                "AUTO SELECT BREAKER"
            )
        }

        // =========================
        // MANUAL
        // =========================

        HorizontalDivider()

        Text(
            text =
                "Manual Breaker Selection",
            style =
                MaterialTheme.typography.titleMedium
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
                text =
                    if (
                        selectedBreaker != null
                    ) {
                        "Breaker: ${selectedBreaker} A"
                    } else {
                        "CHANGE BREAKER"
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

            ElectricalCalculator
                .breakerRatings
                .forEach { rating ->

                    DropdownMenuItem(
                        text = {
                            Text(
                                "$rating A"
                            )
                        },
                        onClick = {

                            selectedBreaker =
                                rating

                            showBreakerMenu =
                                false

                            result =
                                "Breaker selected: $rating A"
                        }
                    )
                }
        }

        OutlinedButton(
            onClick = {
                showIcuMenu =
                    !showIcuMenu
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    if (
                        selectedIcu != null
                    ) {
                        "Icu: ${
                            "%.1f".format(
                                selectedIcu
                            )
                        } kA"
                    } else {
                        "CHANGE Icu"
                    }
            )
        }

        DropdownMenu(
            expanded =
                showIcuMenu,
            onDismissRequest = {
                showIcuMenu = false
            }
        ) {

            ElectricalCalculator
                .breakerIcuRatings
                .forEach { icu ->

                    DropdownMenuItem(
                        text = {
                            Text(
                                "%.1f kA".format(
                                    icu
                                )
                            )
                        },
                        onClick = {

                            selectedIcu =
                                icu

                            showIcuMenu =
                                false

                            result =
                                "Icu selected: ${
                                    "%.1f".format(
                                        icu
                                    )
                                } kA"
                        }
                    )
                }
        }

        // =========================
        // MANUAL CHECK
        // =========================

        Button(
            onClick = {

                val i =
                    current
                        .toDoubleOrNull()
                        ?: 0.0

                val isc =
                    faultCurrent
                        .toDoubleOrNull()
                        ?: 0.0

                val iz =
                    ProjectManager
                        .calculation
                        .cableAmpacityA

                val breaker =
                    selectedBreaker

                val icu =
                    selectedIcu

                if (
                    i <= 0.0 ||
                    isc <= 0.0
                ) {

                    result =
                        "Please enter valid Design Current and Short Circuit Current."

                } else if (
                    iz <= 0.0
                ) {

                    result =
                        "Please select and calculate a cable first."

                } else if (
                    breaker == null
                ) {

                    result =
                        "Please select a breaker."

                } else if (
                    icu == null
                ) {

                    result =
                        "Please select breaker Icu."

                } else {

                    val check =
                        ElectricalCalculator
                            .validateBreaker(
                                currentA = i,
                                faultCurrentKA = isc,
                                breakerRatingA =
                                    breaker,
                                breakerIcuKA =
                                    icu,
                                cableAmpacityA =
                                    iz
                            )

                    /*
                     * Save ONLY if the complete
                     * protection check passes.
                     */

                    if (check.success) {

                        ProjectManager.setBreaker(
                            breakerRatingA =
                                check.ratingA,
                            breakerIcuKA =
                                check.icuKA
                        )
                    }

                    result =
                        """
                        MANUAL BREAKER CHECK

                        Design Current Ib:
                        %.2f A

                        Cable Ampacity Iz:
                        %.2f A

                        Selected Breaker In:
                        %d A

                        Short Circuit Icc:
                        %.2f kA

                        Selected Icu:
                        %.1f kA

                        Ib ≤ In:
                        %s

                        In ≤ Iz:
                        %s

                        Icu ≥ Icc:
                        %s

                        FINAL STATUS:
                        %s
                        """.trimIndent()
                            .format(
                                i,
                                iz,
                                check.ratingA,
                                isc,
                                check.icuKA,

                                if (
                                    check.ratingA >= i
                                ) {
                                    "PASS"
                                } else {
                                    "FAIL"
                                },

                                if (
                                    check.ratingA <= iz
                                ) {
                                    "PASS"
                                } else {
                                    "FAIL"
                                },

                                if (
                                    check.icuKA >= isc
                                ) {
                                    "PASS"
                                } else {
                                    "FAIL"
                                },

                                check.status
                            )
                }
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                "CHECK SELECTED BREAKER"
            )
        }

        // =========================
        // RESULT
        // =========================

        if (result.isNotEmpty()) {

            HorizontalDivider()

            Text(
                text = result,
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        // =========================
        // PROJECT PROTECTION
        // =========================

        HorizontalDivider()

        Text(
            text =
                "Current Project Protection",
            style =
                MaterialTheme.typography.titleMedium
        )

        Text(
            text =
                "Design Current Ib: %.2f A"
                    .format(
                        ProjectManager
                            .calculation
                            .designCurrentA
                    )
        )

        Text(
            text =
                "Cable Size: %.1f mm²"
                    .format(
                        ProjectManager
                            .calculation
                            .cableSizeMm2
                    )
        )

        Text(
            text =
                "Cable Ampacity Iz: %.2f A"
                    .format(
                        ProjectManager
                            .calculation
                            .cableAmpacityA
                    )
        )

        Text(
            text =
                "Short Circuit Icc: %.2f kA"
                    .format(
                        ProjectManager
                            .calculation
                            .shortCircuitKA
                    )
        )

        Text(
            text =
                "Breaker In: %d A"
                    .format(
                        ProjectManager
                            .calculation
                            .breakerRatingA
                    )
        )

        Text(
            text =
                "Breaker Icu: %.1f kA"
                    .format(
                        ProjectManager
                            .calculation
                            .breakerIcuKA
                    )
        )

        Text(
            text =
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
