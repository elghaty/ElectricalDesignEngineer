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
fun PowerFactorCorrectionScreen(
    onBack: () -> Unit = {}
) {

    val project = ProjectManager.calculation

    var activePower by remember {
        mutableStateOf(
            if (project.demandKW > 0.0) {
                "%.2f".format(project.demandKW)
            } else {
                ""
            }
        )
    }

    var existingPF by remember {
        mutableStateOf(
            if (project.powerFactor > 0.0) {
                "%.3f".format(project.powerFactor)
            } else {
                "0.80"
            }
        )
    }

    var targetPF by remember {
        mutableStateOf("0.95")
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
            text = "Power Factor Correction",
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
            text = "Power Factor Correction Input",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = activePower,
            onValueChange = {
                activePower = it
            },
            label = {
                Text("Active Power (kW)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = existingPF,
            onValueChange = {
                existingPF = it
            },
            label = {
                Text("Existing Power Factor")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = targetPF,
            onValueChange = {
                targetPF = it
            },
            label = {
                Text("Target Power Factor")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val kw =
                    activePower.toDoubleOrNull() ?: 0.0

                val pfOld =
                    existingPF
                        .toDoubleOrNull()
                        ?.coerceIn(0.01, 0.999)
                        ?: 0.80

                val pfTarget =
                    targetPF
                        .toDoubleOrNull()
                        ?.coerceIn(0.01, 0.999)
                        ?: 0.95

                if (kw <= 0.0) {

                    result =
                        "Please calculate Load first."

                } else if (pfTarget <= pfOld) {

                    result =
                        "Target PF must be higher than existing PF."

                } else {

                    ProjectManager.updateSystem(
                        powerFactor = pfTarget
                    )

                    val capacitor =
                        ElectricalCalculator.capacitorBank(
                            activePowerKW = kw,
                            existingPF = pfOld,
                            targetPF = pfTarget
                        )

                    ProjectManager.setCapacitorBank(
                        capacitorKVAR = capacitor
                    )

                    result = """
                        POWER FACTOR CORRECTION
                        
                        Active Power:
                        %.2f kW
                        
                        Existing Power Factor:
                        %.3f
                        
                        Target Power Factor:
                        %.3f
                        
                        Required Capacitor Bank:
                        %.2f kVAR
                        
                        Design Status:
                        %s
                        
                        ✓ Capacitor bank result saved to ProjectManager
                        ✓ Power factor updated in project
                    """.trimIndent().format(
                        kw,
                        pfOld,
                        pfTarget,
                        capacitor,
                        ProjectManager.calculation.designStatus
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate & Save Capacitor Bank")
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
                text = "✓ Power factor correction saved to ProjectManager",
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Current Project Power Factor",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Demand Load: %.2f kW".format(
                ProjectManager.calculation.demandKW
            )
        )

        Text(
            text = "Power Factor: %.3f".format(
                ProjectManager.calculation.powerFactor
            )
        )

        Text(
            text = "Capacitor Bank: %.2f kVAR".format(
                ProjectManager.calculation.capacitorKVAR
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
