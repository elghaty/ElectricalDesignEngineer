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
fun LoadScheduleScreen(
    onBack: () -> Unit = {}
) {

    val project = ProjectManager.calculation

    var loadName by remember {
        mutableStateOf("")
    }

    var quantity by remember {
        mutableStateOf("1")
    }

    var powerKW by remember {
        mutableStateOf("")
    }

    var demandFactor by remember {
        mutableStateOf("1.0")
    }

    var powerFactor by remember {
        mutableStateOf("0.90")
    }

    var message by remember {
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
            text = "Load Schedule",
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
            text = "Add Load",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = loadName,
            onValueChange = {
                loadName = it
            },
            label = {
                Text("Load Description")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = quantity,
            onValueChange = {
                quantity = it
            },
            label = {
                Text("Quantity")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = powerKW,
            onValueChange = {
                powerKW = it
            },
            label = {
                Text("Unit Power (kW)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = demandFactor,
            onValueChange = {
                demandFactor = it
            },
            label = {
                Text("Demand Factor")
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

        Button(
            onClick = {

                val name =
                    loadName.trim()

                val qty =
                    quantity.toDoubleOrNull() ?: 0.0

                val kw =
                    powerKW.toDoubleOrNull() ?: 0.0

                val df =
                    demandFactor
                        .toDoubleOrNull()
                        ?.coerceIn(0.0, 1.0)
                        ?: 1.0

                val pf =
                    powerFactor
                        .toDoubleOrNull()
                        ?.coerceIn(0.01, 1.0)
                        ?: 0.90

                if (
                    name.isBlank() ||
                    qty <= 0.0 ||
                    kw <= 0.0
                ) {

                    message =
                        "Please enter valid load data."

                } else {

                    ProjectManager.addLoad(
                        LoadItem(
                            name = name,
                            quantity = qty,
                            powerKW = kw,
                            demandFactor = df,
                            powerFactor = pf
                        )
                    )

                    ProjectManager.calculateFromLoads()

                    loadName = ""
                    quantity = "1"
                    powerKW = ""
                    demandFactor = "1.0"
                    powerFactor = "0.90"

                    message =
                        "✓ Load added and project recalculated."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Load")
        }

        if (message.isNotEmpty()) {

            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Project Load Schedule",
            style = MaterialTheme.typography.titleMedium
        )

        if (ProjectManager.loads.isEmpty()) {

            Text(
                text = "No loads added yet.",
                style = MaterialTheme.typography.bodyMedium
            )

        } else {

            ProjectManager.loads.forEachIndexed { index, load ->

                val connected =
                    load.quantity * load.powerKW

                val demand =
                    connected * load.demandFactor

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {

                    Text(
                        text = "${index + 1}. ${load.name}",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Qty: %.1f".format(
                                load.quantity
                            )
                        )

                        Text(
                            text = "Unit: %.2f kW".format(
                                load.powerKW
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Connected: %.2f kW".format(
                                connected
                            )
                        )

                        Text(
                            text = "Demand: %.2f kW".format(
                                demand
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "DF: %.2f".format(
                                load.demandFactor
                            )
                        )

                        Text(
                            text = "PF: %.2f".format(
                                load.powerFactor
                            )
                        )
                    }

                    HorizontalDivider()
                }
            }
        }

        HorizontalDivider()

        Text(
            text = "Project Summary",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Connected Load: %.2f kW".format(
                ProjectManager.calculation.connectedKW
            )
        )

        Text(
            text = "Demand Load: %.2f kW".format(
                ProjectManager.calculation.demandKW
            )
        )

        Text(
            text = "Total Demand: %.2f kVA".format(
                ProjectManager.calculation.totalKVA
            )
        )

        Text(
            text = "Design Current: %.2f A".format(
                ProjectManager.calculation.designCurrentA
            )
        )

        Text(
            text = "Power Factor: %.3f".format(
                ProjectManager.calculation.powerFactor
            )
        )

        Text(
            text = "Transformer: %.0f kVA".format(
                ProjectManager.calculation.transformerKVA
            )
        )

        Text(
            text = "Generator: %.0f kVA".format(
                ProjectManager.calculation.generatorKVA
            )
        )

        Text(
            text = "Breaker: %d A".format(
                ProjectManager.calculation.breakerRatingA
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
