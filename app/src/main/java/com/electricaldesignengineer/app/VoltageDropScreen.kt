package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VoltageDropScreen(
    onBack: () -> Unit = {}
) {

    var current by remember {
        mutableStateOf(
            if (ElectricalCalculator.designCurrentA > 0)
                "%.2f".format(ElectricalCalculator.designCurrentA)
            else ""
        )
    }

    var cableSize by remember {
        mutableStateOf(
            if (ElectricalCalculator.cableSizeMm2 > 0)
                "%.1f".format(ElectricalCalculator.cableSizeMm2)
            else ""
        )
    }

    var length by remember {
        mutableStateOf(
            if (ElectricalCalculator.cableLengthM > 0)
                "%.1f".format(ElectricalCalculator.cableLengthM)
            else "30"
        )
    }

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
            text = "Voltage Drop",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            label = { Text("Current (A)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cableSize,
            onValueChange = { cableSize = it },
            label = { Text("Cable Size (mm²)") },
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
            label = { Text("System Voltage (V)") },
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

                val i =
                    current.toDoubleOrNull() ?: 0.0

                val s =
                    cableSize.toDoubleOrNull() ?: 0.0

                val l =
                    length.toDoubleOrNull() ?: 0.0

                val v =
                    voltage.toDoubleOrNull() ?: 0.0

                val pf =
                    powerFactor.toDoubleOrNull() ?: 0.90

                if (i <= 0 || s <= 0 || l <= 0 || v <= 0) {

                    result = "Please enter valid values."

                } else {

                    val calculation =
                        ElectricalCalculator.selectCable(
                            currentA = i,
                            lengthM = l,
                            voltage = v,
                            pf = pf,
                            threePhase = isThreePhase
                        )

                    result = """
                        Voltage Drop Result
                        -------------------------
                        
                        Cable:
                        %.1f mm²
                        
                        Voltage Drop:
                        %.2f V
                        
                        Voltage Drop:
                        %.2f %%
                        
                        Limit:
                        3.00 %%
                        
                        Status:
                        %s
                    """.trimIndent().format(
                        s,
                        calculation.voltageDropV,
                        calculation.voltageDropPercent,
                        calculation.status
                    )
                }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Voltage Drop")
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
