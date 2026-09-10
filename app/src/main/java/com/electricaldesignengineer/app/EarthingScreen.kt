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
                earthResistance.toDoubleOrNull()

            val fault =
                faultCurrent.toDoubleOrNull()

            val touchVoltage =
                permissibleTouchVoltage.toDoubleOrNull()

            if (
                resistance == null ||
                fault == null ||
                touchVoltage == null
            ) {
                result =
                    "Please enter valid numeric values."

            } else {

                val earth =
                    ProfessionalEngineeringCore.calculateEarthing(
                        input =
                            ProfessionalEngineeringCore.EarthingInput(
                                earthResistanceOhm = resistance,
                                faultCurrentA = fault,
                                permissibleTouchVoltageV = touchVoltage
                            )
                    )

                if (
                    earth.status ==
                    EngineeringStatus.PASS
                ) {

                    ProjectManager.setEarthing(
                        earthResistanceOhm =
                            resistance,
                        earthFaultCurrentA =
                            fault,
                        earthPotentialRiseV =
                            earth.earthPotentialRiseV,
                        maximumEarthResistanceOhm =
                            earth.maximumResistanceOhm
                    )
                }

                result = buildString {

                    appendLine("EARTHING CHECK")
                    appendLine()

                    appendLine(
                        "Earth Resistance:"
                    )
                    appendLine(
                        "%.2f Ω".format(
                            resistance
                        )
                    )
                    appendLine()

                    appendLine(
                        "Earth Fault Current:"
                    )
                    appendLine(
                        "%.2f A".format(
                            fault
                        )
                    )
                    appendLine()

                    appendLine(
                        "Permissible Touch Voltage:"
                    )
                    appendLine(
                        "%.2f V".format(
                            touchVoltage
                        )
                    )
                    appendLine()

                    appendLine(
                        "Earth Potential Rise:"
                    )
                    appendLine(
                        "%.2f V".format(
                            earth.earthPotentialRiseV
                        )
                    )
                    appendLine()

                    appendLine(
                        "Maximum Earth Resistance:"
                    )
                    appendLine(
                        "%.2f Ω".format(
                            earth.maximumResistanceOhm
                        )
                    )
                    appendLine()

                    appendLine(
                        "Design Result:"
                    )
                    appendLine(
                        if (
                            earth.status ==
                            EngineeringStatus.PASS
                        ) {
                            "EARTHING CHECK PASS"
                        } else {
                            "EARTHING RESISTANCE TOO HIGH"
                        }
                    )
                    appendLine()

                    appendLine(
                        "Calculation Status:"
                    )
                    appendLine(
                        earth.status.toString()
                    )

                    if (
                        earth.status ==
                        EngineeringStatus.PASS
                    ) {
                        appendLine()
                        appendLine(
                            "✓ Earthing result saved to ProjectManager"
                        )
                    }
                }
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

        if (
            ProjectManager.calculation
                .earthResistanceOhm > 0.0
        ) {
            Text(
                text =
                    "✓ Earthing result saved to ProjectManager",
                style =
                    MaterialTheme.typography.labelLarge
            )
        }
    }

    HorizontalDivider()

    Text(
        text = "Current Project Earthing",
        style = MaterialTheme.typography.titleMedium
    )

    Text(
        text =
            "Earth Resistance: %.2f Ω".format(
                ProjectManager.calculation
                    .earthResistanceOhm
            )
    )

    Text(
        text =
            "Earth Fault Current: %.2f A".format(
                ProjectManager.calculation
                    .earthFaultCurrentA
            )
    )

    Text(
        text =
            "Earth Potential Rise: %.2f V".format(
                ProjectManager.calculation
                    .earthPotentialRiseV
            )
    )

    Text(
        text =
            "Maximum Earth Resistance: %.2f Ω".format(
                ProjectManager.calculation
                    .maximumEarthResistanceOhm
            )
    )

    Text(
        text =
            "Design Status: ${
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
