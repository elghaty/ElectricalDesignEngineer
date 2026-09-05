package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EarthingScreen(
    onBack: () -> Unit = {}
) {
    var earthResistance by remember { mutableStateOf("1.0") }

    var faultCurrent by remember {
        mutableStateOf(
            if (ElectricalCalculator.shortCircuitKA > 0)
                "%.2f".format(
                    ElectricalCalculator.shortCircuitKA * 1000.0
                )
            else ""
        )
    }

    var touchVoltage by remember { mutableStateOf("50") }

    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            "Earthing",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = earthResistance,
            onValueChange = { earthResistance = it },
            label = { Text("Earth Resistance (Ω)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = faultCurrent,
            onValueChange = { faultCurrent = it },
            label = { Text("Fault Current (A)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = touchVoltage,
            onValueChange = { touchVoltage = it },
            label = { Text("Permissible Touch Voltage (V)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val r =
                    earthResistance.toDoubleOrNull() ?: 0.0

                val fault =
                    faultCurrent.toDoubleOrNull() ?: 0.0

                val touch =
                    touchVoltage.toDoubleOrNull() ?: 50.0

                val calculation =
                    ElectricalCalculator.earthCheck(
                        earthResistanceOhm = r,
                        faultCurrentA = fault,
                        permissibleTouchVoltageV = touch
                    )

                result = """
                    EARTHING CHECK
                    -------------------------
                    
                    Earth Resistance:
                    %.3f Ω
                    
                    Fault Current:
                    %.2f A
                    
                    Earth Potential Rise:
                    %.2f V
                    
                    Maximum Calculated Resistance:
                    %.4f Ω
                    
                    Status:
                    %s
                """.trimIndent().format(
                    r,
                    fault,
                    calculation.earthPotentialRiseV,
                    calculation.maximumResistanceOhm,
                    calculation.status
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Earthing")
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
