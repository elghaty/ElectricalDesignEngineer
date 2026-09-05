package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import kotlin.math.acos
import kotlin.math.tan

private val standardCapacitorSteps = listOf(
    5.0,
    10.0,
    12.5,
    15.0,
    20.0,
    25.0,
    30.0,
    40.0,
    50.0,
    60.0,
    75.0,
    100.0,
    125.0,
    150.0,
    200.0,
    250.0,
    300.0,
    400.0,
    500.0
)

@Composable
fun PowerFactorCorrectionScreen(
    onBack: () -> Unit = {}
) {

    var loadKW by remember { mutableStateOf("") }
    var existingPF by remember { mutableStateOf("0.75") }
    var targetPF by remember { mutableStateOf("0.95") }

    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text("Power Factor Correction")

        OutlinedTextField(
            value = loadKW,
            onValueChange = { loadKW = it },
            label = { Text("Load Power (kW)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = existingPF,
            onValueChange = { existingPF = it },
            label = { Text("Existing Power Factor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = targetPF,
            onValueChange = { targetPF = it },
            label = { Text("Target Power Factor") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val kw = loadKW.toDoubleOrNull() ?: 0.0
                val pf1 = existingPF.toDoubleOrNull() ?: 0.0
                val pf2 = targetPF.toDoubleOrNull() ?: 0.0

                if (
                    kw <= 0 ||
                    pf1 <= 0 ||
                    pf1 > 1 ||
                    pf2 <= 0 ||
                    pf2 > 1 ||
                    pf2 <= pf1
                ) {
                    result = """
                        Please enter valid values.

                        Target PF must be greater
                        than the existing PF.
                    """.trimIndent()

                    return@Button
                }

                val phi1 = acos(pf1)
                val phi2 = acos(pf2)

                val currentReactivePower =
                    kw * tan(phi1)

                val targetReactivePower =
                    kw * tan(phi2)

                val requiredKVAR =
                    currentReactivePower - targetReactivePower

                val selectedStep =
                    standardCapacitorSteps.firstOrNull {
                        it >= requiredKVAR
                    }

                if (selectedStep == null) {

                    result =
                        "Required capacitor bank is above the available range."

                    return@Button
                }

                val originalKVA =
                    kw / pf1

                val correctedKVA =
                    kw / pf2

                result = """
                    Power Factor Correction
                    -------------------------
                    
                    Load Power: %.2f kW
                    
                    Existing PF: %.3f
                    
                    Target PF: %.3f
                    
                    Existing Reactive Power:
                    %.2f kVAr
                    
                    Target Reactive Power:
                    %.2f kVAr
                    
                    Required Capacitor:
                    %.2f kVAr
                    
                    Recommended Standard Bank:
                    %.1f kVAr
                    
                    Existing Apparent Power:
                    %.2f kVA
                    
                    Corrected Apparent Power:
                    %.2f kVA
                    
                    Estimated kVA Reduction:
                    %.2f kVA
                """.trimIndent().format(
                    kw,
                    pf1,
                    pf2,
                    currentReactivePower,
                    targetReactivePower,
                    requiredKVAR,
                    selectedStep,
                    originalKVA,
                    correctedKVA,
                    originalKVA - correctedKVA
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Capacitor Bank")
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
