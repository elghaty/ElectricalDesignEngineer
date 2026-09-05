package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PowerFactorCorrectionScreen(
    onBack: () -> Unit = {}
) {
    var powerKW by remember {
        mutableStateOf(
            if (ElectricalCalculator.demandKW > 0)
                "%.2f".format(ElectricalCalculator.demandKW)
            else ""
        )
    }

    var existingPF by remember {
        mutableStateOf(
            "%.2f".format(ElectricalCalculator.powerFactor)
        )
    }

    var targetPF by remember {
        mutableStateOf("0.95")
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
            "Power Factor Correction",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = powerKW,
            onValueChange = { powerKW = it },
            label = { Text("Active Power (kW)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = existingPF,
            onValueChange = { existingPF = it },
            label = { Text("Existing Power Factor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = targetPF,
            onValueChange = { targetPF = it },
            label = { Text("Target Power Factor") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val p = powerKW.toDoubleOrNull() ?: 0.0
                val pf1 = existingPF.toDoubleOrNull() ?: 0.0
                val pf2 = targetPF.toDoubleOrNull() ?: 0.0

                if (
                    p <= 0 ||
                    pf1 <= 0 ||
                    pf2 <= 0 ||
                    pf2 <= pf1
                ) {

                    result =
                        "Enter valid values. Target PF should be higher than existing PF."

                } else {

                    val qc =
                        ElectricalCalculator.capacitorBank(
                            activePowerKW = p,
                            existingPF = pf1,
                            targetPF = pf2
                        )

                    result = """
                        POWER FACTOR CORRECTION
                        -------------------------
                        
                        Active Power:
                        %.2f kW
                        
                        Existing PF:
                        %.2f
                        
                        Target PF:
                        %.2f
                        
                        Required Capacitor:
                        %.2f kVAr
                        
                        Recommended:
                        %.0f kVAr
                    """.trimIndent().format(
                        p,
                        pf1,
                        pf2,
                        qc,
                        kotlin.math.ceil(qc / 5.0) * 5.0
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate Capacitor Bank")
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
