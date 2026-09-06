package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
fun VoltageDropScreen(
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

    var cableSize by remember {
        mutableStateOf(
            if (project.cableSizeMm2 > 0.0) {
                "%.1f".format(project.cableSizeMm2)
            } else {
                ""
            }
        )
    }

    var length by remember {
        mutableStateOf(
            if (project.cableLengthM > 0.0) {
                "%.1f".format(project.cableLengthM)
            } else {
                "30"
            }
        )
    }

    var voltage by remember {
        mutableStateOf(
            project.voltageV.toString()
        )
    }

    var powerFactor by remember {
        mutableStateOf(
            project.powerFactor.toString()
        )
    }

    var isThreePhase by remember {
        mutableStateOf(
            project.isThreePhase
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
            text = "Voltage Drop",
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
            text = "Project Electrical Data",
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
            value = cableSize,
            onValueChange = {
                cableSize = it
            },
            label = {
                Text("Cable Size (mm²)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = length,
            onValueChange = {
                length = it
            },
            label = {
                Text("Cable Length (m)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = voltage,
            onValueChange = {
                voltage = it
            },
            label = {
                Text("System Voltage (V)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = powerFactor,
            onValueChange = {
                powerFactor = it
            },
            label = {
                Text("Power Factor")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {
                    isThreePhase = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("3 Phase")
            }

            Button(
                onClick = {
                    isThreePhase = false
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("1 Phase")
            }
        }

        Button(
            onClick = {

                val i =
                    current.toDoubleOrNull() ?: 0.0

                val s =
                    cableSize.toDoubleOrNull() ?: 0.0

                val l =
                    length.toDoubleOrNull() ?: 0.0

                val v =
                    voltage.toDoubleOrNull() ?: 0.0

                val pf =
                    powerFactor
                        .toDoubleOrNull()
                        ?.coerceIn(0.01, 1.0)
                        ?: project.powerFactor

                if (
                    i <= 0.0 ||
                    s <= 0.0 ||
                    l <= 0.0 ||
                    v <= 0.0
                ) {

                    result =
                        "Please enter valid values."

                } else {

                    ProjectManager.updateSystem(
                        voltageV = v,
                        powerFactor = pf,
                        isThreePhase = isThreePhase
                    )

                    val calculation =
                        ElectricalCalculator.selectCable(
                            currentA = i,
                            lengthM = l,
                            voltage = v,
                            pf = pf,
                            threePhase = isThreePhase
                        )

                    ProjectManager.setCableResult(
                        cableSizeMm2 = s,
                        cableAmpacityA =
                            ProjectManager.calculation.cableAmpacityA,
                        cableLengthM = l,
                        voltageDropV =
                            calculation.voltageDropV,
                        voltageDropPercent =
                            calculation.voltageDropPercent
                    )

                    result = """
                        VOLTAGE DROP RESULT
                        
                        Cable:
                        %.1f mm²
                        
                        Current:
                        %.2f A
                        
                        Cable Length:
                        %.1f m
                        
                        System Voltage:
                        %.0f V
                        
                        Voltage Drop:
                        %.2f V
                        
                        Voltage Drop:
                        %.2f %%
                        
                        Design Status:
                        %s
                    """.trimIndent().format(
                        s,
                        i,
                        l,
                        v,
                        calculation.voltageDropV,
                        calculation.voltageDropPercent,
                        ProjectManager.calculation.designStatus
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate & Save Voltage Drop")
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
                text = "✓ Voltage Drop saved to ProjectManager",
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = "✓ Result available for protection and final design",
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Current Project Result",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Design Current: %.2f A".format(
                ProjectManager.calculation.designCurrentA
            )
        )

        Text(
            text = "Cable: %.1f mm²".format(
                ProjectManager.calculation.cableSizeMm2
            )
        )

        Text(
            text = "Cable Length: %.1f m".format(
                ProjectManager.calculation.cableLengthM
            )
        )

        Text(
            text = "Voltage Drop: %.2f V".format(
                ProjectManager.calculation.voltageDropV
            )
        )

        Text(
            text = "Voltage Drop: %.2f %%".format(
                ProjectManager.calculation.voltageDropPercent
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
