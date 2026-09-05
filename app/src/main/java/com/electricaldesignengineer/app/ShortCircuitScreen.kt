package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ShortCircuitScreen(
    onBack: () -> Unit = {}
) {

    var transformerKVA by remember {
        mutableStateOf(
            if (ElectricalCalculator.transformerKVA > 0)
                ElectricalCalculator.transformerKVA.toString()
            else "1000"
        )
    }

    var voltage by remember {
        mutableStateOf(
            ElectricalCalculator.voltageV.toString()
        )
    }

    var impedance by remember {
        mutableStateOf("6.0")
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
            text = "Short Circuit Current",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = transformerKVA,
            onValueChange = { transformerKVA = it },
            label = { Text("Transformer Rating (kVA)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = voltage,
            onValueChange = { voltage = it },
            label = { Text("LV Voltage (V)") },
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

                val kva =
                    transformerKVA.toDoubleOrNull() ?: 0.0

                val v =
                    voltage.toDoubleOrNull() ?: 0.0

                val z =
                    impedance.toDoubleOrNull() ?: 0.0

                val faultCurrent =
                    ElectricalCalculator
                        .transformerShortCircuit(
                            transformerKVA = kva,
                            voltage = v,
                            impedancePercent = z
                        )

                result = """
                    Short Circuit Calculation
                    -------------------------
                    
                    Transformer:
                    %.0f kVA
                    
                    Voltage:
                    %.0f V
                    
                    Impedance:
                    %.2f %%
                    
                    Prospective Short Circuit:
                    %.2f kA
                    
                    ✓ Result saved for Breaker Selection
                """.trimIndent().format(
                    kva,
                    v,
                    z,
                    faultCurrent
                )

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Short Circuit")
        }

        if (result.isNotEmpty()) {

            HorizontalDivider()

            Text(
                text = result,
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
