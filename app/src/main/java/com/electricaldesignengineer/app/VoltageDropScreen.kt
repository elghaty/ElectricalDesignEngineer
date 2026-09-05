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
fun VoltageDropScreen(
    onBack: () -> Unit = {}
) {
    var current by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var cableSize by remember { mutableStateOf("") }
    var voltage by remember { mutableStateOf("400") }
    var powerFactor by remember { mutableStateOf("0.9") }

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
            text = "Voltage Drop Calculation"
        )

        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            label = { Text("Design Current (A)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = length,
            onValueChange = { length = it },
            label = { Text("Cable Length (m)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cableSize,
            onValueChange = { cableSize = it },
            label = { Text("Cable Size (mm²)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = voltage,
            onValueChange = { voltage = it },
            label = {
                Text(
                    if (isThreePhase)
                        "Voltage L-L (V)"
                    else
                        "Voltage (V)"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = powerFactor,
            onValueChange = { powerFactor = it },
            label = { Text("Power Factor") },
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

                val i = current.toDoubleOrNull() ?: 0.0
                val l = length.toDoubleOrNull() ?: 0.0
                val s = cableSize.toDoubleOrNull() ?: 0.0
                val v = voltage.toDoubleOrNull() ?: 0.0
                val pf = powerFactor.toDoubleOrNull() ?: 0.0

                if (i <= 0 || l <= 0 || s <= 0 || v <= 0 || pf <= 0 || pf > 1) {
                    result = "Please enter valid values."
                    return@Button
                }

                val resistance = 18.1 / s

                val sinPhi = sqrt(
                    (1.0 - pf * pf).coerceAtLeast(0.0)
                )

                // Simplified reactance for initial calculation.
                val reactance = 0.08

                val voltageDrop = if (isThreePhase) {

                    sqrt(3.0) * i *
                            (
                                resistance * pf +
                                        reactance * sinPhi
                                ) *
                            l / 1000.0

                } else {

                    2.0 * i *
                            (
                                resistance * pf +
                                        reactance * sinPhi
                                ) *
                            l / 1000.0
                }

                val percentage = voltageDrop / v * 100.0

                val status =
                    if (percentage <= 3.0)
                        "PASS"
                    else
                        "CHECK"

                result = """
                    Voltage Drop Result
                    -------------------------
                    Voltage Drop: %.2f V
                    Voltage Drop: %.2f %%
                    
                    Final Voltage: %.2f V
                    
                    Status: %s
                """.trimIndent().format(
                    voltageDrop,
                    percentage,
                    v - voltageDrop,
                    status
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
