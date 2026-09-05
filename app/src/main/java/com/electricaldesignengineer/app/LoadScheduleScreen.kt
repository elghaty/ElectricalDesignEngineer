package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

data class ScheduleLoad(
    val name: String,
    val quantity: Int,
    val powerKW: Double,
    val pf: Double,
    val demandFactor: Double
)

@Composable
fun LoadScheduleScreen(
    onBack: () -> Unit = {}
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var power by remember { mutableStateOf("") }
    var pf by remember { mutableStateOf("0.90") }
    var demandFactor by remember { mutableStateOf("1.00") }

    val loads = remember {
        mutableStateListOf<ScheduleLoad>()
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
            "Load Schedule",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Load Description") },
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
            label = { Text("Power / Unit (kW)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = pf,
            onValueChange = { pf = it },
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

                val q =
                    quantity.toIntOrNull() ?: 0

                val p =
                    power.toDoubleOrNull() ?: 0.0

                val powerFactor =
                    pf.toDoubleOrNull() ?: 0.0

                val df =
                    demandFactor.toDoubleOrNull() ?: 1.0

                if (
                    q <= 0 ||
                    p <= 0 ||
                    powerFactor <= 0
                ) {
                    result = "Please enter valid load data."
                } else {

                    loads.add(
                        ScheduleLoad(
                            name = if (name.isBlank())
                                "Load ${loads.size + 1}"
                            else name,
                            quantity = q,
                            powerKW = p,
                            pf = powerFactor,
                            demandFactor = df.coerceIn(0.0, 1.0)
                        )
                    )

                    name = ""
                    power = ""

                    result = "Load added successfully."
                }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Load")
        }

        Button(
            onClick = {

                if (loads.isEmpty()) {
                    result = "No loads in schedule."
                    return@Button
                }

                var connected = 0.0
                var demand = 0.0
                var apparent = 0.0

                loads.forEach { load ->

                    val connectedLoad =
                        load.quantity * load.powerKW

                    val demandLoad =
                        connectedLoad *
                                load.demandFactor

                    val kva =
                        if (load.pf > 0)
                            demandLoad / load.pf
                        else 0.0

                    connected += connectedLoad
                    demand += demandLoad
                    apparent += kva
                }

                val overallPF =
                    if (apparent > 0)
                        demand / apparent
                    else 0.0

                val current =
                    if (ElectricalCalculator.voltageV > 0)
                        apparent * 1000.0 /
                                (sqrt(3.0) *
                                        ElectricalCalculator.voltageV)
                    else 0.0

                ElectricalCalculator.connectedKW = connected
                ElectricalCalculator.demandKW = demand
                ElectricalCalculator.totalKVA = apparent
                ElectricalCalculator.powerFactor = overallPF
                ElectricalCalculator.designCurrentA = current

                result = """
                    LOAD SCHEDULE SUMMARY
                    -------------------------
                    
                    Number of Load Items:
                    ${loads.size}
                    
                    Connected Load:
                    %.2f kW
                    
                    Demand Load:
                    %.2f kW
                    
                    Total Apparent Power:
                    %.2f kVA
                    
                    Overall Power Factor:
                    %.3f
                    
                    Estimated 3-Phase Current:
                    %.2f A
                    
                    ✓ Results transferred to all design modules
                """.trimIndent().format(
                    connected,
                    demand,
                    apparent,
                    overallPF,
                    current
                )

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Complete Schedule")
        }

        if (loads.isNotEmpty()) {

            HorizontalDivider()

            Text(
                "Loads: ${loads.size}",
                style = MaterialTheme.typography.titleMedium
            )

            loads.forEachIndexed { index, load ->

                Text(
                    "${index + 1}. ${load.name} | " +
                            "${load.quantity} × " +
                            "${load.powerKW} kW | " +
                            "DF ${load.demandFactor}"
                )
            }
        }

        if (result.isNotEmpty()) {

            HorizontalDivider()

            Text(
                result,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
