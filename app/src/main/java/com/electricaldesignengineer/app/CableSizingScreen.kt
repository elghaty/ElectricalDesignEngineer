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
fun CableSizingScreen(
    onBack: () -> Unit = {}
) {

    val project = ProjectManager.calculation

    var designCurrent by remember {
        mutableStateOf(
            if (project.designCurrentA > 0.0) {
                "%.2f".format(project.designCurrentA)
            } else {
                ""
            }
        )
    }

    var length by remember {
        mutableStateOf(
            if (project.cableLengthM > 0.0) {
                project.cableLengthM.toString()
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
        mutableStateOf(project.isThreePhase)
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
            text = "Cable Sizing",
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
            text = "Load Calculation Input",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = designCurrent,
            onValueChange = {
                designCurrent = it
            },
            label = {
                Text("Design Current (A)")
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
                Text("Voltage (V)")
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

                val current =
                    designCurrent.toDoubleOrNull() ?: 0.0

                val cableLength =
                    length.toDoubleOrNull() ?: 0.0

                val v =
                    voltage.toDoubleOrNull() ?: 0.0

                val pf =
                    powerFactor
                        .toDoubleOrNull()
                        ?.coerceIn(0.01, 1.0)
                        ?: ProjectManager.calculation.powerFactor

                if (
                    current <= 0.0 ||
                    cableLength <= 0.0 ||
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

                    val cableCalculation =
                        ElectricalCalculator.selectCable(
                            currentA = current,
                            lengthM = cableLength,
                            voltage = v,
                            pf = pf,
                            threePhase = isThreePhase
                        )

                    ProjectManager.setCableResult(
                        cableSizeMm2 =
                            cableCalculation.sizeMm2,

                        cableAmpacityA =
                            cableCalculation.ampacityA,

                        cableLengthM =
                            cableLength,

                        voltageDropV =
                            cableCalculation.voltageDropV,

                        voltageDropPercent =
                            cableCalculation.voltageDropPercent
                    )

                    result = """
                        RECOMMENDED CABLE
                        
                        Cross Section:
                        %.1f mm²
                        
                        Approx. Ampacity:
                        %.1f A
                        
                        Voltage Drop:
                        %.2f V
                        
                        Voltage Drop:
                        %.2f %%
                        
                        Design Current:
                        %.2f A
                        
                        Status:
                        %s
                    """.trimIndent().format(
                        cableCalculation.sizeMm2,
                        cableCalculation.ampacityA,
                        cableCalculation.voltageDropV,
                        cableCalculation.voltageDropPercent,
                        current,
                        cableCalculation.status
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate & Save Cable")
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
                text = "✓ Cable saved to ProjectManager",
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = "✓ Voltage Drop and Protection can use this result",
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Current Project",
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
            text = "Ampacity: %.1f A".format(
                ProjectManager.calculation.cableAmpacityA
            )
        )

        Text(
            text = "Cable Length: %.1f m".format(
                ProjectManager.calculation.cableLengthM
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
