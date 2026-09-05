package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TransformerSizingScreen(
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

    var margin by remember {
        mutableStateOf("20")
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
            "Transformer Sizing",
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
            value = margin,
            onValueChange = { margin = it },
            label = { Text("Design Margin (%)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val kw = demandKW.toDoubleOrNull() ?: 0.0
                val powerFactor = pf.toDoubleOrNull() ?: 0.0
                val m = margin.toDoubleOrNull() ?: 20.0

                if (kw <= 0 || powerFactor <= 0) {
                    result = "Please calculate Load first."
                } else {

                    val selected =
                        ElectricalCalculator.selectTransformer(
                            demandKW = kw,
                            pf = powerFactor,
                            marginPercent = m
                        )

                    val baseKVA = kw / powerFactor

                    result = """
                        TRANSFORMER SIZING
                        -------------------------
                        
                        Demand Load:
                        %.2f kW
                        
                        Required Load:
                        %.2f kVA
                        
                        Design Margin:
                        %.1f %%
                        
                        Recommended Transformer:
                        %.0f kVA
                        
                        ✓ Result saved to system
                    """.trimIndent().format(
                        kw,
                        baseKVA,
                        m,
                        selected
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Size Transformer")
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
