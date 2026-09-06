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
fun ShortCircuitScreen(
    onBack: () -> Unit = {}
) {

    val project = ProjectManager.calculation

    var transformerKVA by remember {
        mutableStateOf(
            if (project.transformerKVA > 0.0) {
                project.transformerKVA.toString()
            } else {
                "1000"
            }
        )
    }

    var voltage by remember {
        mutableStateOf(
            project.voltageV.toString()
        )
    }

    var impedance by remember {
        mutableStateOf("6.0")
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
            text = "Short Circuit Current",
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
            text = "Transformer Data",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = transformerKVA,
            onValueChange = {
                transformerKVA = it
            },
            label = {
                Text("Transformer Rating (kVA)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = voltage,
            onValueChange = {
                voltage = it
            },
            label = {
                Text("LV Voltage (V)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = impedance,
            onValueChange = {
                impedance = it
            },
            label = {
                Text("Transformer Impedance (%)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val kva =
                    transformerKVA.toDoubleOrNull() ?: 0.0

                val v =
                    voltage.toDoubleOrNull() ?: 0.0

                val z =
                    impedance
                        .toDoubleOrNull()
                        ?.coerceAtLeast(0.01)
                        ?: 6.0

                if (
                    kva <= 0.0 ||
                    v <= 0.0
                ) {

                    result =
                        "Please enter valid transformer and voltage values."

                } else {

                    ProjectManager.updateSystem(
                        voltageV = v
                    )

                    ProjectManager.setTransformer(
                        transformerKVA = kva
                    )

                    val faultCurrent =
                        ElectricalCalculator
                            .transformerShortCircuit(
                                transformerKVA = kva,
                                voltage = v,
                                impedancePercent = z
                            )

                    ProjectManager.setShortCircuit(
                        shortCircuitKA = faultCurrent
                    )

                    result = """
                        SHORT CIRCUIT CALCULATION
                        
                        Transformer:
                        %.0f kVA
                        
                        Voltage:
                        %.0f V
                        
                        Transformer Impedance:
                        %.2f %%
                        
                        Prospective Short Circuit:
                        %.2f kA
                        
                        Design Status:
                        %s
                        
                        ✓ Short-circuit result saved to ProjectManager
                        ✓ Breaker Selection can use this fault current
                    """.trimIndent().format(
                        kva,
                        v,
                        z,
                        faultCurrent,
                        ProjectManager.calculation.designStatus
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate & Save Short Circuit")
        }

        if (result.isNotEmpty()) {

            HorizontalDivider()

            Text(
                text = result,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Current Project Result",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Transformer: %.0f kVA".format(
                ProjectManager.calculation.transformerKVA
            )
        )

        Text(
            text = "Voltage: %.0f V".format(
                ProjectManager.calculation.voltageV
            )
        )

        Text(
            text = "Short Circuit: %.2f kA".format(
                ProjectManager.calculation.shortCircuitKA
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
