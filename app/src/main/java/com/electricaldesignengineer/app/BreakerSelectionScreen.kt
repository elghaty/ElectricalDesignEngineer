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

private val standardBreakerRatings = listOf(
    6, 10, 16, 20, 25, 32, 40, 50, 63,
    80, 100, 125, 160, 200, 250, 315, 400,
    500, 630, 800, 1000, 1250, 1600, 2000,
    2500, 3200, 4000, 5000, 6300
)

private val breakingCapacitiesKA = listOf(
    6, 10, 15, 18, 25, 36, 50, 65, 85, 100
)

@Composable
fun BreakerSelectionScreen(
    onBack: () -> Unit = {}
) {
    var loadCurrent by remember { mutableStateOf("") }
    var shortCircuitKA by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text("Breaker Selection")

        OutlinedTextField(
            value = loadCurrent,
            onValueChange = { loadCurrent = it },
            label = { Text("Design Current (A)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = shortCircuitKA,
            onValueChange = { shortCircuitKA = it },
            label = { Text("Prospective Short Circuit (kA)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val current = loadCurrent.toDoubleOrNull() ?: 0.0
                val faultKA = shortCircuitKA.toDoubleOrNull() ?: 0.0

                if (current <= 0 || faultKA <= 0) {
                    result = "Please enter valid values."
                    return@Button
                }

                // Select the first standard breaker rating
                // greater than or equal to the design current.
                val breaker = standardBreakerRatings.firstOrNull {
                    it >= current
                }

                // Select a standard breaking capacity
                // greater than or equal to the prospective fault level.
                val breakingCapacity = breakingCapacitiesKA.firstOrNull {
                    it >= faultKA
                }

                if (breaker == null) {
                    result = "Current is above the available breaker range."
                    return@Button
                }

                if (breakingCapacity == null) {
                    result = "Fault level is above the available breaking-capacity range."
                    return@Button
                }

                result = """
                    Breaker Selection
                    -------------------------
                    
                    Design Current: %.2f A
                    
                    Recommended Rating:
                    %d A
                    
                    Short Circuit Level:
                    %.2f kA
                    
                    Minimum Breaking Capacity:
                    %d kA
                    
                    Preliminary Selection:
                    %d A / %d kA
                """.trimIndent().format(
                    current,
                    breaker,
                    faultKA,
                    breakingCapacity,
                    breaker,
                    breakingCapacity
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Breaker")
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
