package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun LoadCalculationScreen(
    onBack: () -> Unit = {}
) {
    var loadName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var power by remember { mutableStateOf("") }
    var powerFactor by remember { mutableStateOf("0.9") }
    var diversityFactor by remember { mutableStateOf("1.0") }
    var voltage by remember { mutableStateOf("400") }
    var isThreePhase by remember { mutableStateOf(true) }

    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Load Calculation"
        )

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
            value = diversityFactor,
            onValueChange = { diversityFactor = it },
            label = { Text("Diversity Factor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = voltage,
            onValueChange = { voltage = it },
            label = {
                Text(
                    if (isThreePhase)
                        "Voltage (V L-L)"
                    else
                        "Voltage (V)"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isThreePhase = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("3 Phase")
            }

            Button(
                onClick = { isThreePhase = false },
                modifier = Modifier.weight(1f)
            ) {
                Text("1 Phase")
            }
        }

        Button(
            onClick = {

                val q = quantity.toDoubleOrNull() ?: 0.0
                val p = power.toDoubleOrNull() ?: 0.0
                val pf = powerFactor.toDoubleOrNull() ?: 0.0
                val df = diversityFactor.toDoubleOrNull() ?: 1.0
                val v = voltage.toDoubleOrNull() ?: 0.0

                val connectedKW = q * p
                val demandKW = connectedKW * df
                val kva = if (pf > 0) demandKW / pf else 0.0

                val current = if (isThreePhase) {
                    if (v > 0) {
                        (kva * 1000) / (sqrt(3.0) * v)
                    } else {
                        0.0
                    }
                } else {
                    if (v > 0) {
                        (kva * 1000) / v
                    } else {
                        0.0
                    }
                }

                result = """
                    Load: ${if (loadName.isBlank()) "Unnamed Load" else loadName}
                    
                    Connected Load: %.2f kW
                    Demand Load: %.2f kW
                    Apparent Power: %.2f kVA
                    Design Current: %.2f A
                """.trimIndent().format(
                    connectedKW,
                    demandKW,
                    kva,
                    current
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate")
        }

        if (result.isNotEmpty()) {
            Text(
                text = result,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
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
