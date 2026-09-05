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
import kotlin.math.sqrt

private val standardGeneratorRatings = listOf(
    10.0,
    15.0,
    20.0,
    25.0,
    30.0,
    40.0,
    50.0,
    60.0,
    75.0,
    80.0,
    100.0,
    125.0,
    150.0,
    175.0,
    200.0,
    250.0,
    300.0,
    350.0,
    400.0,
    450.0,
    500.0,
    600.0,
    750.0,
    800.0,
    1000.0,
    1250.0,
    1500.0,
    2000.0,
    2500.0,
    3000.0,
    3500.0,
    4000.0,
    5000.0,
    6000.0
)

@Composable
fun GeneratorSizingScreen(
    onBack: () -> Unit = {}
) {

    var loadKW by remember { mutableStateOf("") }
    var powerFactor by remember { mutableStateOf("0.8") }
    var loadFactor by remember { mutableStateOf("80") }
    var motorAllowance by remember { mutableStateOf("20") }
    var voltage by remember { mutableStateOf("400") }

    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text("Generator Sizing")

        OutlinedTextField(
            value = loadKW,
            onValueChange = { loadKW = it },
            label = { Text("Required Load (kW)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = powerFactor,
            onValueChange = { powerFactor = it },
            label = { Text("Power Factor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = loadFactor,
            onValueChange = { loadFactor = it },
            label = { Text("Generator Loading (%)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = motorAllowance,
            onValueChange = { motorAllowance = it },
            label = { Text("Motor Starting Allowance (%)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = voltage,
            onValueChange = { voltage = it },
            label = { Text("Generator Voltage (V)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val kw = loadKW.toDoubleOrNull() ?: 0.0
                val pf = powerFactor.toDoubleOrNull() ?: 0.0
                val loading = loadFactor.toDoubleOrNull() ?: 0.0
                val motor = motorAllowance.toDoubleOrNull() ?: 0.0
                val v = voltage.toDoubleOrNull() ?: 0.0

                if (
                    kw <= 0 ||
                    pf <= 0 ||
                    pf > 1 ||
                    loading <= 0 ||
                    loading > 100 ||
                    motor < 0 ||
                    v <= 0
                ) {
                    result = "Please enter valid values."
                    return@Button
                }

                val loadKVA =
                    kw / pf

                val motorAdjustedKVA =
                    loadKVA * (1.0 + motor / 100.0)

                val requiredGeneratorKVA =
                    motorAdjustedKVA / (loading / 100.0)

                val selectedGenerator =
                    standardGeneratorRatings.firstOrNull {
                        it >= requiredGeneratorKVA
                    }

                if (selectedGenerator == null) {
                    result =
                        "Required generator size is above the available range."
                    return@Button
                }

                val generatorCurrent =
                    (selectedGenerator * 1000.0) /
                            (sqrt(3.0) * v)

                val generatorKW =
                    selectedGenerator * pf

                result = """
                    Generator Sizing Result
                    -------------------------
                    
                    Load: %.2f kW
                    
                    Power Factor: %.2f
                    
                    Base Load:
                    %.2f kVA
                    
                    Motor Starting Allowance:
                    %.1f %%
                    
                    Adjusted Requirement:
                    %.2f kVA
                    
                    Generator Loading:
                    %.1f %%
                    
                    Required Generator Capacity:
                    %.2f kVA
                    
                    Recommended Standard Generator:
                    %.0f kVA
                    
                    Approx. Generator Output:
                    %.2f kW
                    
                    Generator Voltage:
                    %.0f V
                    
                    Approx. Full Load Current:
                    %.2f A
                """.trimIndent().format(
                    kw,
                    pf,
                    loadKVA,
                    motor,
                    motorAdjustedKVA,
                    loading,
                    requiredGeneratorKVA,
                    selectedGenerator,
                    generatorKW,
                    v,
                    generatorCurrent
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Generator")
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
