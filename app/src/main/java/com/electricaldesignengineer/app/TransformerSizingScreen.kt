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

private val standardTransformerRatings = listOf(
    25.0,
    50.0,
    100.0,
    160.0,
    250.0,
    315.0,
    400.0,
    500.0,
    630.0,
    800.0,
    1000.0,
    1250.0,
    1600.0,
    2000.0,
    2500.0,
    3150.0,
    4000.0,
    5000.0,
    6300.0
)

@Composable
fun TransformerSizingScreen(
    onBack: () -> Unit = {}
) {

    var loadKW by remember { mutableStateOf("") }
    var powerFactor by remember { mutableStateOf("0.9") }
    var growthMargin by remember { mutableStateOf("20") }
    var primaryVoltage by remember { mutableStateOf("11000") }
    var secondaryVoltage by remember { mutableStateOf("400") }

    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text("Transformer Sizing")

        OutlinedTextField(
            value = loadKW,
            onValueChange = { loadKW = it },
            label = { Text("Total Demand Load (kW)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = powerFactor,
            onValueChange = { powerFactor = it },
            label = { Text("Power Factor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = growthMargin,
            onValueChange = { growthMargin = it },
            label = { Text("Future Growth Margin (%)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = primaryVoltage,
            onValueChange = { primaryVoltage = it },
            label = { Text("Primary Voltage (V)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = secondaryVoltage,
            onValueChange = { secondaryVoltage = it },
            label = { Text("Secondary Voltage (V)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val kw = loadKW.toDoubleOrNull() ?: 0.0
                val pf = powerFactor.toDoubleOrNull() ?: 0.0
                val growth = growthMargin.toDoubleOrNull() ?: 0.0
                val hv = primaryVoltage.toDoubleOrNull() ?: 0.0
                val lv = secondaryVoltage.toDoubleOrNull() ?: 0.0

                if (
                    kw <= 0 ||
                    pf <= 0 ||
                    pf > 1 ||
                    growth < 0 ||
                    hv <= 0 ||
                    lv <= 0
                ) {
                    result = "Please enter valid values."
                    return@Button
                }

                val requiredKVA =
                    kw / pf

                val futureKVA =
                    requiredKVA * (1.0 + growth / 100.0)

                val selectedTransformer =
                    standardTransformerRatings.firstOrNull {
                        it >= futureKVA
                    }

                if (selectedTransformer == null) {
                    result =
                        "Required transformer size is above the available range."
                    return@Button
                }

                val secondaryCurrent =
                    (selectedTransformer * 1000.0) /
                            (kotlin.math.sqrt(3.0) * lv)

                result = """
                    Transformer Sizing Result
                    -------------------------
                    
                    Demand Load: %.2f kW
                    
                    Power Factor: %.2f
                    
                    Required Transformer:
                    %.2f kVA
                    
                    Future Growth:
                    %.1f %%
                    
                    Design Transformer Capacity:
                    %.2f kVA
                    
                    Recommended Standard Rating:
                    %.0f kVA
                    
                    Primary Voltage:
                    %.0f V
                    
                    Secondary Voltage:
                    %.0f V
                    
                    Approx. LV Full Load Current:
                    %.2f A
                """.trimIndent().format(
                    kw,
                    pf,
                    requiredKVA,
                    growth,
                    futureKVA,
                    selectedTransformer,
                    hv,
                    lv,
                    secondaryCurrent
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Transformer")
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
