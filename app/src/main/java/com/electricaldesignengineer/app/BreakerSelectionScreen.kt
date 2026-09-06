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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
                "%.2f".format(project.designCurrentA)
            } else {
                ""
            }
        )
    }

    var faultCurrent by remember {
        mutableStateOf(
            if (project.shortCircuitKA > 0.0) {
                "%.2f".format(project.shortCircuitKA)
            } else {
                ""
            }
        )
    }

    var result by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Breaker Selection",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Project: ${
                project.projectName.ifBlank {
                    "Current Project"
                }
            }",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text(
            text = "Protection Inputs",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = current,
            onValueChange = {
                current = it
            },
            label = {
                Text("Design Current (A)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = faultCurrent,
            onValueChange = {
                faultCurrent = it
            },
            label = {
                Text("Short Circuit Current (kA)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val i =
                    current.toDoubleOrNull() ?: 0.0

                val isc =
                    faultCurrent.toDoubleOrNull() ?: 0.0

                if (
                    i <= 0.0 ||
                    isc <= 0.0
                ) {

                    result =
                        "Please calculate Load and Short Circuit first."

                } else {

                    val breaker =
                        ElectricalCalculator.selectBreaker(
                            currentA = i,
                            faultCurrentKA = isc
                        )

                    ProjectManager.setBreaker(
                        breakerRatingA =
                            breaker.ratingA,
                        breakerIcuKA =
                            breaker.icuKA
                    )

                    result = """
                        BREAKER SELECTION
                        
                        Design Current:
                        %.2f A
                        
                        Recommended Breaker:
                        %d A
                        
                        Short Circuit:
                        %.2f kA
                        
                        Required Icu:
                        %.1f kA
                        
                        Design Status:
                        %s
                        
                        ✓ Breaker saved to ProjectManager
                        ✓ Protection result available to project
                    """.trimIndent().format(
                        i,
                        breaker.ratingA,
                        isc,
                        breaker.icuKA,
                        ProjectManager.calculation.designStatus
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select & Save Breaker")
        }

        if (result.isNotEmpty()) {

            HorizontalDivider()

            Text(
                text = result,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "✓ Breaker result saved to ProjectManager",
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Current Project Protection",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Design Current: %.2f A".format(
                ProjectManager.calculation.designCurrentA
            )
        )

        Text(
            text = "Short Circuit: %.2f kA".format(
                ProjectManager.calculation.shortCircuitKA
            )
        )

        Text(
            text = "Breaker Rating: %d A".format(
                ProjectManager.calculation.breakerRatingA
            )
        )

        Text(
            text = "Breaker Icu: %.1f kA".format(
                ProjectManager.calculation.breakerIcuKA
            )
        )

        Text(
            text = "Design Status: ${
                ProjectManager.calculation.designStatus
            }"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
