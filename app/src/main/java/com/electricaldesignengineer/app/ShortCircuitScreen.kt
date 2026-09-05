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
fun ShortCircuitScreen(
    onBack: () -> Unit = {}
) {
    var voltage by remember { mutableStateOf("400") }
    var transformerKva by remember { mutableStateOf("") }
    var impedance by remember { mutableStateOf("6") }
    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text("Short Circuit Current")

        OutlinedTextField(
            value = voltage,
            onValueChange = { voltage = it },
            label = { Text("Secondary Voltage (V)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = transformerKva,
            onValueChange = { transformerKva = it },
            label = { Text("Transformer Rating (kVA)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = impedance,
            onValueChange = { impedance = it },
            label = { Text("Transformer Impedance (%)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val v = voltage.toDoubleOrNull() ?: 0.0
                val kva = transformerKva.toDoubleOrNull() ?: 0.0
                val zPercent = impedance.toDoubleOrNull() ?: 0.0

                if (v <= 0 || kva <= 0 || zPercent <= 0) {
                    result = "Please enter valid values."
                    return@Button
                }

                val fullLoadCurrent =
                    (kva * 1000.0) / (sqrt(3.0) * v)

                val shortCircuitCurrent =
                    fullLoadCurrent * (100.0 / zPercent)

                result = """
                    Short Circuit Result
                    -------------------------
                    Transformer: %.0f kVA
                    
                    Full Load Current: %.2f A
                    
                    Transformer Z: %.2f %%
                    
                    Prospective Short Circuit:
                    %.2f A
                    %.2f kA
                """.trimIndent().format(
                    kva,
                    fullLoadCurrent,
                    zPercent,
                    shortCircuitCurrent,
                    shortCircuitCurrent / 1000.0
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate")
        }

        if (result.isNotEmpty()) {
            Text(
                result,
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
