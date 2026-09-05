package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

data class LoadScheduleItem(
    val name: String,
    val quantity: Int,
    val powerKW: Double,
    val powerFactor: Double,
    val demandFactor: Double
)

@Composable
fun LoadScheduleScreen(
    onBack: () -> Unit = {}
) {

    var loadName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var powerKW by remember { mutableStateOf("") }
    var powerFactor by remember { mutableStateOf("0.9") }
    var demandFactor by remember { mutableStateOf("1.0") }

    var loads by remember {
        mutableStateOf(listOf<LoadScheduleItem>())
    }

    var result by remember { mutableStateOf("") }

    val totalConnectedKW = loads.sumOf {
        it.quantity * it.powerKW
    }

    val totalDemandKW = loads.sumOf {
        it.quantity * it.powerKW * it.demandFactor
    }

    val totalKVA = loads.sumOf {
        if (it.powerFactor > 0) {
            (it.quantity * it.powerKW * it.demandFactor) /
                    it.powerFactor
        } else {
            0.0
        }
    }

    val overallPF =
        if (totalKVA > 0)
            totalDemandKW / totalKVA
        else
            0.0

    val totalCurrent =
        if (totalKVA > 0) {
            (totalKVA * 1000.0) /
                    (sqrt(3.0) * 400.0)
        } else {
            0.0
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text("Load Schedule")

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
            value = powerKW,
            onValueChange = { powerKW = it },
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

        Button(
            onClick = {

                val q = quantity.toIntOrNull() ?: 0
                val p = powerKW.toDoubleOrNull() ?: 0.0
                val pf = powerFactor.toDoubleOrNull() ?: 0.0
                val df = demandFactor.toDoubleOrNull() ?: 0.0

                if (
                    loadName.isBlank() ||
                    q <= 0 ||
                    p <= 0 ||
                    pf <= 0 ||
                    pf > 1 ||
                    df <= 0 ||
                    df > 1
                ) {
                    result = "Please enter valid values."
                    return@Button
                }

                val newLoad = LoadScheduleItem(
                    name = loadName,
                    quantity = q,
                    powerKW = p,
                    powerFactor = pf,
                    demandFactor = df
                )

                loads = loads + newLoad

                loadName = ""
                quantity = "1"
                powerKW = ""
                powerFactor = "0.9"
                demandFactor = "1.0"

                result = "Load added successfully."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Load")
        }

        Text(
            text = "Load Schedule Items: ${loads.size}"
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            items(loads) { load ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                ) {

                    Text(
                        "${load.name} | Qty: ${load.quantity}"
                    )

                    Text(
                        "Power: %.2f kW | PF: %.2f | DF: %.2f".format(
                            load.powerKW,
                            load.powerFactor,
                            load.demandFactor
                        )
                    )
                }
            }
        }

        if (loads.isNotEmpty()) {

            Text(
                """
                Connected Load: %.2f kW
                Demand Load: %.2f kW
                Total Demand: %.2f kVA
                Overall PF: %.3f
                Estimated Current @ 400V: %.2f A
                """.trimIndent().format(
                    totalConnectedKW,
                    totalDemandKW,
                    totalKVA,
                    overallPF,
                    totalCurrent
                )
            )
        }

        if (result.isNotEmpty()) {
            Text(result)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {
                    loads = emptyList()
                    result = "Load schedule cleared."
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }

            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
        }
    }
}
