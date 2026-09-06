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
fun EarthingScreen(
    onBack: () -> Unit = {}
) {

    val project = ProjectManager.calculation

    var earthResistance by remember {
        mutableStateOf(
            if (project.earthResistanceOhm > 0.0) {
                "%.2f".format(project.earthResistanceOhm)
            } else {
                "1.00"
            }
        )
    }

    var faultCurrent by remember {
        mutableStateOf(
            if (project.earthFaultCurrentA > 0.0) {
                "%.2f".format(project.earthFaultCurrentA)
            } else {
                ""
            }
        )
    }

    var permissibleTouchVoltage by remember {
        mutableStateOf("50")
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
            text = "Earthing",
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
            text = "Earthing Design Input",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = earthResistance,
            onValueChange = {
                earthResistance = it
            },
            label = {
                Text("Earth Resistance (Ω)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = faultCurrent,
            onValueChange = {
                faultCurrent = it
            },
            label = {
                Text("Earth Fault Current (A)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = permissibleTouchVoltage,
            onValueChange = {
                permissibleTouchVoltage = it
            },
            label = {
                Text("Permissible Touch Voltage (V)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val resistance =
                    earthResistance.toDoubleOrNull() ?: 0.0

                val fault =
                    faultCurrent.toDoubleOrNull() ?: 0.0

                val touchVoltage =
                    permissibleTouchVoltage
                        .toDoubleOrNull()
                        ?.coerceAtLeast(0.1)
                        ?: 50.0

                if (resistance <= 0.0 || fault <= 0.0) {

                    result =
                        "Please enter valid earth resistance and fault current."

                } else {

                    val earth =
                        ElectricalCalculator.earthCheck(
                            earthResistanceOhm = resistance,
                            faultCurrentA = fault,
                            permissibleTouchVoltageV = touchVoltage
                        )

                    ProjectManager.setEarthing(
                        earthResistanceOhm = resistance,
                        earthFaultCurrentA = fault,
                        earthPotentialRiseV = earth.potentialRiseV,
                        maximumEarthResistanceOhm =
                            earth.maximumResistanceOhm
                    )

                    val status =
                        if (earth.isAcceptable) {
                            "ACCEPTABLE"
                        } else {
                            "NOT ACCEPTABLE"
                        }

                    result = """
                        EARTHING CHECK
                        
                        Earth Resistance:
                        %.2f Ω
                        
                        Earth Fault Current:
                        %.2f A
                        
                        Permissible Touch Voltage:
                        %.2f V
                        
                        Earth Potential Rise:
                        %.2f V
                        
                        Maximum Earth Resistance:
                        %.2f Ω
                        
                        Design Result:
                        %s
                        
                        Design Status:
                        %s
                        
                        ✓ Earthing result saved to ProjectManager
                    """.trimIndent().format(
                        resistance,
                        fault,
                        touchVoltage,
                        earth.potentialRiseV,
                        earth.maximumResistanceOhm,
                        status,
                        ProjectManager.calculation.designStatus
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate & Save Earthing")
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
                text = "✓ Earthing result saved to ProjectManager",
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Current Project Earthing",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Earth Resistance: %.2f Ω".format(
                ProjectManager.calculation.earthResistanceOhm
            )
        )

        Text(
            text = "Earth Fault Current: %.2f A".format(
                ProjectManager.calculation.earthFaultCurrentA
            )
        )

        Text(
            text = "Earth Potential Rise: %.2f V".format(
                ProjectManager.calculation.earthPotentialRiseV
            )
        )

        Text(
            text = "Maximum Earth Resistance: %.2f Ω".format(
                ProjectManager.calculation.maximumEarthResistanceOhm
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
