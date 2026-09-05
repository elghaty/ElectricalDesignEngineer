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

data class CableOption(
    val size: Double,
    val ampacity: Double
)

private val cableOptions = listOf(
    CableOption(1.0, 14.0),
    CableOption(1.5, 18.0),
    CableOption(2.5, 24.0),
    CableOption(4.0, 32.0),
    CableOption(6.0, 41.0),
    CableOption(10.0, 57.0),
    CableOption(16.0, 76.0),
    CableOption(25.0, 101.0),
    CableOption(35.0, 125.0),
    CableOption(50.0, 150.0),
    CableOption(70.0, 192.0),
    CableOption(95.0, 232.0),
    CableOption(120.0, 269.0),
    CableOption(150.0, 309.0),
    CableOption(185.0, 353.0),
    CableOption(240.0, 415.0),
    CableOption(300.0, 473.0),
    CableOption(400.0, 557.0)
)

@Composable
fun CableSizingScreen(
    onBack: () -> Unit = {}
) {

    var designCurrent by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
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
            text = "Cable Sizing"
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
                onClick = {
                    isThreePhase = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("3 Phase")
            }

            Button(
                onClick = {
                    isThreePhase = false
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("1 Phase")
            }
        }

        Button(
            onClick = {

                val current = designCurrent.toDoubleOrNull() ?: 0.0
                val cableLength = length.toDoubleOrNull() ?: 0.0
                val v = voltage.toDoubleOrNull() ?: 0.0

                if (current <= 0 || cableLength <= 0 || v <= 0) {
                    result = "Please enter valid values."
                    return@Button
                }

                val requiredCable = cableOptions.firstOrNull {
                    it.ampacity >= current
                }

                if (requiredCable == null) {
                    result = "No cable size available for this current."
                    return@Button
                }

                /*
                 * Simplified voltage-drop check.
                 *
                 * This is an initial engineering model.
                 * Final cable selection will later use:
                 * - installation method
                 * - correction factors
                 * - conductor temperature
                 * - grouping
                 * - cable construction
                 * - IEC tables
                 */

                val resistanceOhmPerKm =
                    18.1 / requiredCable.size

                val voltageDrop = if (isThreePhase) {

                    sqrt(3.0) *
                            current *
                            (resistanceOhmPerKm / 1000.0) *
                            cableLength

                } else {

                    2.0 *
                            current *
                            (resistanceOhmPerKm / 1000.0) *
                            cableLength
                }

                val voltageDropPercent =
                    voltageDrop / v * 100.0

                result = """
                    Recommended Cable
                    -------------------------
                    Cross Section: ${requiredCable.size} mm²
                    Approx. Ampacity: %.1f A
                    
                    Voltage Drop: %.2f V
                    Voltage Drop: %.2f %%
                    
                    Current: %.2f A
                    Length: %.1f m
                """.trimIndent().format(
                    requiredCable.ampacity,
                    voltageDrop,
                    voltageDropPercent,
                    current,
                    cableLength
                )

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Cable")
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
