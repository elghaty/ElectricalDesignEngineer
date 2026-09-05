package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GeneratorSizingScreen(
    onBack: () -> Unit = {}
) {
    var demandKW by remember {
        mutableStateOf(
            if (ElectricalCalculator.demandKW > 0)
                "%.2f".format(ElectricalCalculator.demandKW)
            else ""
        )
    }

    var pf by remember {
        mutableStateOf(
            "%.2f".format(ElectricalCalculator.powerFactor)
        )
    }

    var loading by remember { mutableStateOf("80") }
    var motorAllowance by remember { mutableStateOf("15") }

    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            "Generator Sizing",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = demandKW,
            onValueChange = { demandKW = it },
            label = { Text("Demand Load (kW)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = pf,
            onValueChange = { pf = it },
            label = { Text("Power Factor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = loading,
            onValueChange = { loading = it },
            label = { Text("Generator Loading (%)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = motorAllowance,
            onValueChange = { motorAllowance = it },
            label = { Text("Motor Starting Allowance (%)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val kw = demandKW.toDoubleOrNull() ?: 0.0
                val powerFactor = pf.toDoubleOrNull() ?: 0.0
                val load = loading.toDoubleOrNull() ?: 80.0
                val motor = motorAllowance.toDoubleOrNull() ?: 15.0

                if (kw <= 0 || powerFactor <= 0) {

                    result = "Please calculate Load first."

                } else {

                    val selected =
                        ElectricalCalculator.selectGenerator(
                            demandKW = kw,
                            pf = powerFactor,
                            loadingPercent = load,
                            motorAllowancePercent = motor
                        )

                    result = """
                        GENERATOR SIZING
                        -------------------------
                        
                        Demand Load:
                        %.2f kW
                        
                        Base Load:
                        %.2f kVA
                        
                        Generator Loading:
                        %.1f %%
                        
                        Motor Allowance:
                        %.1f %%
                        
                        Recommended Generator:
                        %.0f kVA
                        
                        ✓ Result saved to system
                    """.trimIndent().format(
                        kw,
                        kw / powerFactor,
                        load,
                        motor,
                        selected
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Size Generator")
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
