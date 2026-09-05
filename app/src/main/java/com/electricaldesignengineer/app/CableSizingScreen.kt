package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CableSizingScreen(
    onBack: () -> Unit = {}
) {

    var designCurrent by remember {
        mutableStateOf(
            if (ElectricalCalculator.designCurrentA > 0)
                "%.2f".format(ElectricalCalculator.designCurrentA)
            else ""
        )
    }

    var length by remember { mutableStateOf("30") }

    var voltage by remember {
        mutableStateOf(
            ElectricalCalculator.voltageV.toString()
        )
    }

    var powerFactor by remember {
        mutableStateOf(
            ElectricalCalculator.powerFactor.toString()
        )
    }

    var isThreePhase by remember {
        mutableStateOf(ElectricalCalculator.isThreePhase)
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
            text = "Cable Sizing",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = designCurrent,
            onValueChange = { designCurrent = it },
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
            value = voltage,
            onValueChange = { voltage = it },
            label = { Text("Voltage (V)") },
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

                val current =
                    designCurrent.toDoubleOrNull() ?: 0.0

                val cableLength =
                    length.toDoubleOrNull() ?: 0.0

                val v =
                    voltage.toDoubleOrNull() ?: 0.0

                val pf =
                    powerFactor.toDoubleOrNull() ?: 0.90

                if (
                    current <= 0 ||
                    cableLength <= 0 ||
                    v <= 0
                ) {

                    result = "Please enter valid values."

                } else {

                    val calculation =
                        ElectricalCalculator.selectCable(
                            currentA = current,
                            lengthM = cableLength,
                            voltage = v,
                            pf = pf,
                            threePhase = isThreePhase
                        )

                    result = """
                        Recommended Cable
                        -------------------------
                        
                        Cross Section:
                        %.1f mm²
                        
                        Approx. Ampacity:
                        %.1f A
                        
                        Voltage Drop:
                        %.2f V
                        
                        Voltage Drop:
                        %.2f %%
                        
                        Status:
                        %s
                    """.trimIndent().format(
                        calculation.sizeMm2,
                        calculation.ampacityA,
                        calculation.voltageDropV,
                        calculation.voltageDropPercent,
                        calculation.status
                    )
                }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate & Save Cable")
        }

        if (result.isNotEmpty()) {

            HorizontalDivider()

            Text(
                text = result,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "✓ Cable result saved for Voltage Drop / Protection",
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
