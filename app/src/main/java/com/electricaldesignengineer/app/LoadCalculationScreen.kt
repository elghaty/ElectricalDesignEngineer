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
fun LoadCalculationScreen(
    onBack: () -> Unit = {}
) {
    var loadName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var power by remember { mutableStateOf("") }
    var powerFactor by remember { mutableStateOf("0.90") }
    var demandFactor by remember { mutableStateOf("1.00") }

    var voltage by remember {
        mutableStateOf(
            ProjectManager.calculation.voltageV.toString()
        )
    }

    var isThreePhase by remember {
        mutableStateOf(
            ProjectManager.calculation.isThreePhase
        )
    }

    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Load Calculation",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Project: ${
                ProjectManager.calculation.projectName.ifBlank {
                    "Current Project"
                }
            }",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        OutlinedTextField(
            value = loadName,
            onValueChange = { loadName = it },
            label = { Text("Load Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = power,
            onValueChange = { power = it },
            label = { Text("Power per Load (kW)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = powerFactor,
            onValueChange = { powerFactor = it },
            label = { Text("Power Factor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = demandFactor,
            onValueChange = { demandFactor = it },
            label = { Text("Demand Factor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = voltage,
            onValueChange = { voltage = it },
            label = {
                Text(
                    if (isThreePhase) {
                        "Voltage L-L (V)"
                    } else {
                        "Voltage (V)"
                    }
                )
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
                val q = quantity.toDoubleOrNull() ?: 0.0
                val p = power.toDoubleOrNull() ?: 0.0

                val pf = (
                    powerFactor.toDoubleOrNull()
                        ?: 0.90
                    ).coerceIn(0.01, 1.0)

                val df = (
                    demandFactor.toDoubleOrNull()
                        ?: 1.0
                    ).coerceIn(0.0, 1.0)

                val v =
                    voltage.toDoubleOrNull()
                        ?: ProjectManager.calculation.voltageV

                ProjectManager.updateSystem(
                    voltageV = v,
                    powerFactor = pf,
                    isThreePhase = isThreePhase
                )

                ProjectManager.addLoad(
                    LoadItem(
                        name = loadName.ifBlank {
                            "Unnamed Load"
                        },
                        quantity = q,
                        powerKW = p,
                        demandFactor = df,
                        powerFactor = pf
                    )
                )

                val calculation =
                    ProjectManager.calculateFromLoads()

                result = """
                    PROJECT LOAD SUMMARY
                    
                    Connected Load:
                    %.2f kW
                    
                    Demand Load:
                    %.2f kW
                    
                    Total Apparent Power:
                    %.2f kVA
                    
                    Design Current:
                    %.2f A
                    
                    Effective Power Factor:
                    %.3f
                    
                    System Voltage:
                    %.0f V
                    
                    Loads in Project:
                    %d
                """.trimIndent().format(
                    calculation.connectedKW,
                    calculation.demandKW,
                    calculation.totalKVA,
                    calculation.designCurrentA,
                    calculation.powerFactor,
                    calculation.voltageV,
                    ProjectManager.loads.size
                )

                loadName = ""
                quantity = "1"
                power = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Load & Calculate Project")
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
                text = "✓ Load saved to ProjectManager",
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = "✓ Results available to next design modules",
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Current Project Results",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Connected: %.2f kW".format(
                ProjectManager.calculation.connectedKW
            )
        )

        Text(
            text = "Demand: %.2f kW".format(
                ProjectManager.calculation.demandKW
            )
        )

        Text(
            text = "Total: %.2f kVA".format(
                ProjectManager.calculation.totalKVA
            )
        )

        Text(
            text = "Design Current: %.2f A".format(
                ProjectManager.calculation.designCurrentA
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
