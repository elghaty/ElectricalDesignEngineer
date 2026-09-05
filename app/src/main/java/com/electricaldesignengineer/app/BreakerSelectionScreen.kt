package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BreakerSelectionScreen(
    onBack: () -> Unit = {}
) {
    var current by remember {
        mutableStateOf(
            if (ElectricalCalculator.designCurrentA > 0)
                "%.2f".format(ElectricalCalculator.designCurrentA)
            else ""
        )
    }

    var faultCurrent by remember {
        mutableStateOf(
            if (ElectricalCalculator.shortCircuitKA > 0)
                "%.2f".format(ElectricalCalculator.shortCircuitKA)
            else ""
        )
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
            "Breaker Selection",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = current,
            onValueChange = { current = it },
            label = { Text("Design Current (A)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = faultCurrent,
            onValueChange = { faultCurrent = it },
            label = { Text("Short Circuit Current (kA)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val i = current.toDoubleOrNull() ?: 0.0
                val isc = faultCurrent.toDoubleOrNull() ?: 0.0

                if (i <= 0 || isc <= 0) {
                    result = "Please calculate Load and Short Circuit first."
                } else {

                    val r = ElectricalCalculator.selectBreaker(
                        currentA = i,
                        faultCurrentKA = isc
                    )

                    result = """
                        BREAKER SELECTION
                        -------------------------
                        
                        Design Current:
                        %.2f A
                        
                        Recommended Rating:
                        %d A
                        
                        Short Circuit:
                        %.2f kA
                        
                        Required Icu:
                        %.1f kA
                        
                        Status:
                        %s
                    """.trimIndent().format(
                        i,
                        r.ratingA,
                        isc,
                        r.icuKA,
                        r.status
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select Breaker")
        }

        if (result.isNotEmpty()) {
            HorizontalDivider()

            Text(
                result,
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
