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
        mutableStateOf<Int?>(null)
    }

    var selectedIcu by remember {
        mutableStateOf<Double?>(null)
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
            text =
                "Project: ${
                    project.projectName.ifBlank {
                        "Current Project"
                    }
                }",
            style =
                MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text(
            text = "Protection Inputs",
            style =
                MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = current,
            onValueChange = {
                current = it
            },
            label = {
                Text(
                    "Design Current (A)"
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = faultCurrent,
            onValueChange = {
                faultCurrent = it
            },
            label = {
                Text(
                    "Short Circuit Current (kA)"
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        // =========================
        // AUTO SELECTION
        // =========================

        Button(
            onClick = {

                val i =
                    current.toDoubleOrNull()
                        ?: 0.0

                val isc =
                    faultCurrent.toDoubleOrNull()
                        ?: 0.0

                if (
                    i <= 0.0 ||
                    isc <= 0.0
                ) {

                    result =
                        "Please calculate Load and Short Circuit first."

                } else {

                    val breaker =
                        ElectricalCalculator
                            .selectBreaker(
                                currentA = i,
                                faultCurrentKA = isc
                            )

                    if (breaker.success) {

                        selectedBreaker =
                            breaker.ratingA

                        selectedIcu =
                            breaker.icuKA

                    } else {

                        selectedBreaker =
                            if (breaker.ratingA > 0) {
                                breaker.ratingA
                            } else {
                                null
                            }

                        selectedIcu = null
                    }

                    ProjectManager.setBreaker(
                        breakerRatingA =
                            breaker.ratingA,
                        breakerIcuKA =
                            breaker.icuKA
                    )

                    result =
                        """
                        AUTOMATIC BREAKER SELECTION
                        
                        Design Current:
                        %.2f A
                        
                        Recommended Breaker:
                        %d A
                        
                        Short Circuit:
                        %.2f kA
                        
                        Required Icu:
                        %.1f kA
                        
                        Status:
                        %s
                        """.trimIndent()
                            .format(
                                i,
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
        // MANUAL BREAKER
        // =========================

        HorizontalDivider()

        Text(
            text = "Manual Breaker Selection",
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
                                "Breaker selected: $rating A\nPress CHECK SELECTED BREAKER."
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
                                } kA\nPress CHECK SELECTED BREAKER."
                        }
                    )
                }
        }

        // =========================
        // CHECK MANUAL SELECTION
        // =========================

        Button(
            onClick = {

                val i =
                    current.toDoubleOrNull()
                        ?: 0.0

                val isc =
                    faultCurrent.toDoubleOrNull()
                        ?: 0.0

                val breaker =
                    selectedBreaker

                val icu =
                    selectedIcu

                if (
                    i <= 0.0 ||
                    isc <= 0.0
                ) {

                    result =
                        "Please enter Design Current and Short Circuit Current."

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
                                    icu
                            )

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
                        
                        Design Current:
                        %.2f A
                        
                        Selected Breaker:
                        %d A
                        
                        Short Circuit:
                        %.2f kA
                        
                        Selected Icu:
                        %.1f kA
                        
                        Rating Check:
                        %s
                        
                        Icu Check:
                        %s
                        
                        FINAL STATUS:
                        %s
                        """.trimIndent()
                            .format(
                                i,
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
        // CURRENT PROJECT
        // =========================

        HorizontalDivider()

        Text(
            text = "Current Project Protection",
            style =
                MaterialTheme.typography.titleMedium
        )

        Text(
            text =
                "Design Current: %.2f A".format(
                    ProjectManager
                        .calculation
                        .designCurrentA
                )
        )

        Text(
            text =
                "Short Circuit: %.2f kA".format(
                    ProjectManager
                        .calculation
                        .shortCircuitKA
                )
        )

        Text(
            text =
                "Breaker Rating: %d A".format(
                    ProjectManager
                        .calculation
                        .breakerRatingA
                )
        )

        Text(
            text =
                "Breaker Icu: %.1f kA".format(
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
