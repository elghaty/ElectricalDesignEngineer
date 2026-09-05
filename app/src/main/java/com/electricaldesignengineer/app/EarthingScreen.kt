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

@Composable
fun EarthingScreen(
    onBack: () -> Unit = {}
) {

    var earthResistance by remember { mutableStateOf("") }
    var earthFaultCurrent by remember { mutableStateOf("") }
    var allowableTouchVoltage by remember { mutableStateOf("50") }

    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text("Earthing Calculation")

        OutlinedTextField(
            value = earthResistance,
            onValueChange = { earthResistance = it },
            label = { Text("Earth Resistance (Ω)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = earthFaultCurrent,
            onValueChange = { earthFaultCurrent = it },
            label = { Text("Earth Fault Current (A)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = allowableTouchVoltage,
            onValueChange = { allowableTouchVoltage = it },
            label = { Text("Allowable Touch Voltage (V)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val resistance =
                    earthResistance.toDoubleOrNull() ?: 0.0

                val faultCurrent =
                    earthFaultCurrent.toDoubleOrNull() ?: 0.0

                val touchVoltage =
                    allowableTouchVoltage.toDoubleOrNull() ?: 0.0

                if (
                    resistance <= 0 ||
                    faultCurrent <= 0 ||
                    touchVoltage <= 0
                ) {
                    result = "Please enter valid values."
                    return@Button
                }

                val calculatedTouchVoltage =
                    resistance * faultCurrent

                val permissibleResistance =
                    touchVoltage / faultCurrent

                val status =
                    if (calculatedTouchVoltage <= touchVoltage)
                        "PASS"
                    else
                        "CHECK"

                result = """
                    Earthing Calculation
                    -------------------------
                    
                    Earth Resistance:
                    %.3f Ω
                    
                    Earth Fault Current:
                    %.2f A
                    
                    Calculated Earth Potential Rise:
                    %.2f V
                    
                    Allowable Touch Voltage:
                    %.2f V
                    
                    Maximum Permissible Earth Resistance:
                    %.3f Ω
                    
                    Status:
                    %s
                """.trimIndent().format(
                    resistance,
                    faultCurrent,
                    calculatedTouchVoltage,
                    touchVoltage,
                    permissibleResistance,
                    status
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Earthing")
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
