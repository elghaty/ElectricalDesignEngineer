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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
        mutableStateOf(project.voltageV.toString())
    }

    var powerFactor by remember {
        mutableStateOf(project.powerFactor.toString())
    }

    var isThreePhase by remember {
        mutableStateOf(project.isThreePhase)
    }

    var selectedCableSize by remember {
        mutableStateOf<Double?>(null)
    }

    var isCableMenuExpanded by remember {
        mutableStateOf(false)
    }

    var result by remember {
        mutableStateOf("")
    }

    var hasCalculated by remember {
        mutableStateOf(false)
    }

    /*
     * Calculates the cable automatically or recalculates the
     * user-selected cable.
     */
    fun calculateCable(requestedSize: Double? = null) {

        val i = current.toDoubleOrNull() ?: 0.0
        val l = length.toDoubleOrNull() ?: 0.0
        val v = voltage.toDoubleOrNull() ?: 0.0

        val pf =
            powerFactor
                .toDoubleOrNull()
                ?.coerceIn(0.01, 1.0)
                ?: project.powerFactor

        if (
            i <= 0.0 ||
            l <= 0.0 ||
            v <= 0.0
        ) {
            result = "Please enter valid values."
            hasCalculated = false
            return
        }

        ProjectManager.updateSystem(
            voltageV = v,
            powerFactor = pf,
            isThreePhase = isThreePhase
        )

        /*
         * If requestedSize is null:
         * the calculator selects the smallest available cable
         * according to ampacity.
         *
         * If requestedSize is supplied:
         * that exact cable is checked.
         */
        val calculation =
            ElectricalCalculator.selectCable(
                currentA = i,
                lengthM = l,
                voltage = v,
                pf = pf,
                threePhase = isThreePhase,
                requestedCableSizeMm2 = requestedSize
            )

        if (!calculation.success &&
            calculation.sizeMm2 <= 0.0
        ) {

            result = """
                CABLE SELECTION

                Status:
                ${calculation.status}

                No suitable cable was found.
            """.trimIndent()

            hasCalculated = true
            return
        }

        selectedCableSize = calculation.sizeMm2

        val ampacityPass =
            calculation.ampacityA >= i

        val voltageDropPass =
            calculation.voltageDropPercent <= 3.0

        val finalStatus =
            when {
                !ampacityPass &&
                        !voltageDropPass ->
                    "FAIL: AMPACITY + VOLTAGE DROP"

                !ampacityPass ->
                    "FAIL: CABLE AMPACITY"

                !voltageDropPass ->
                    "CHECK VOLTAGE DROP"

                requestedSize == null ->
                    "PASS - AUTO SELECTED"

                else ->
                    "PASS - USER SELECTED"
            }

        ProjectManager.setCableResult(
            cableSizeMm2 = calculation.sizeMm2,
            cableAmpacityA = calculation.ampacityA,
            cableLengthM = l,
            voltageDropV = calculation.voltageDropV,
            voltageDropPercent = calculation.voltageDropPercent
        )

        result = """
            VOLTAGE DROP RESULT

            Recommended / Selected Cable:
            %.1f mm²

            Cable Ampacity:
            %.2f A

            Design Current:
            %.2f A

            Cable Length:
            %.1f m

            System Voltage:
            %.0f V

            Power Factor:
            %.2f

            System:
            %s

            Voltage Drop:
            %.2f V

            Voltage Drop:
            %.2f %%

            Ampacity Check:
            %s

            Voltage Drop Check:
            %s

            Status:
            %s
        """.trimIndent().format(
            calculation.sizeMm2,
            calculation.ampacityA,
            i,
            l,
            v,
            pf,
            if (isThreePhase) {
                "3 Phase"
            } else {
                "1 Phase"
            },
            calculation.voltageDropV,
            calculation.voltageDropPercent,
            if (ampacityPass) {
                "PASS"
            } else {
                "FAIL"
            },
            if (voltageDropPass) {
                "PASS"
            } else {
                "CHECK"
            },
            finalStatus
        )

        hasCalculated = true
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
            text = "Voltage Drop & Cable Sizing",
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
            text = "Electrical Design Inputs",
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
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
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

        /*
         * Main automatic cable sizing button.
         */
        Button(
            onClick = {
                selectedCableSize = null
                calculateCable()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("AUTO SELECT CABLE")
        }

        /*
         * Show the recommended cable after calculation.
         */
        if (hasCalculated &&
            selectedCableSize != null
        ) {

            HorizontalDivider()

            Text(
                text = "Cable Selection",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Recommended Cable: %.1f mm²"
                    .format(selectedCableSize),
                style = MaterialTheme.typography.bodyLarge
            )

            /*
             * User can change the automatically selected cable.
             */
            ExposedDropdownMenuBox(
                expanded = isCableMenuExpanded,
                onExpandedChange = {
                    isCableMenuExpanded =
                        !isCableMenuExpanded
                }
            ) {

                OutlinedButton(
                    onClick = {
                        isCableMenuExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedCableSize?.let {
                            "Change Cable: %.1f mm²"
                                .format(it)
                        } ?: "Change Cable"
                    )
                }

                ExposedDropdownMenu(
                    expanded = isCableMenuExpanded,
                    onDismissRequest = {
                        isCableMenuExpanded = false
                    }
                ) {

                    ElectricalCalculator.cables.forEach { cable ->

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "%.1f mm²  |  %.0f A"
                                        .format(
                                            cable.sizeMm2,
                                            cable.ampacityA
                                        )
                                )
                            },
                            onClick = {

                                selectedCableSize =
                                    cable.sizeMm2

                                isCableMenuExpanded =
                                    false

                                calculateCable(
                                    requestedSize =
                                        cable.sizeMm2
                                )
                            }
                        )
                    }
                }
            }
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
                text =
                    "✓ Cable calculation saved to ProjectManager",
                style =
                    MaterialTheme.typography.labelLarge
            )

            Text(
                text =
                    "✓ Cable can be changed and recalculated",
                style =
                    MaterialTheme.typography.labelLarge
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
            text = "Selected Cable: %.1f mm²".format(
                ProjectManager.calculation.cableSizeMm2
            )
        )

        Text(
            text = "Cable Ampacity: %.2f A".format(
                ProjectManager.calculation.cableAmpacityA
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
