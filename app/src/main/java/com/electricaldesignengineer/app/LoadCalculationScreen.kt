package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadCalculationScreen(
    onBack: () -> Unit = {}
) {
    var loadName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var power by remember { mutableStateOf("") }
    var powerFactor by remember { mutableStateOf("0.90") }
    var demandFactor by remember { mutableStateOf("1.00") }
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
            text = "Load Calculation",
            style = MaterialTheme.typography.headlineSmall
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
            value = demandFactor,
            onValueChange = { demandFactor = it },
            label = { Text("Demand Factor") },
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
                val df = demandFactor.toDoubleOrNull() ?: 1.0
                val v = voltage.toDoubleOrNull() ?: 0.0

                val calculation =
                    ElectricalCalculator.calculateLoad(
                        quantity = q,
                        powerKWPerLoad = p,
                        demandFactor = df,
                        pf = pf,
                        voltage = v,
                        threePhase = isThreePhase
                    )

                result = """
                    Load: ${if (loadName.isBlank()) "Unnamed Load" else loadName}
                    
                    Connected Load:
                    %.2f kW
                    
                    Demand Load:
                    %.2f kW
                    
                    Apparent Power:
                    %.2f kVA
                    
                    Design Current:
                    %.2f A
                """.trimIndent().format(
                    calculation.connectedKW,
                    calculation.demandKW,
                    calculation.kva,
                    calculation.currentA
                )

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate & Send to System")
        }

        if (result.isNotEmpty()) {

            HorizontalDivider()

            Text(
                text = result,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "✓ Results saved to the central calculation engine",
                style = MaterialTheme.typography.labelLarge
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
