package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TransformerSizingScreen(
    onBack: () -> Unit = {}
) {

    val project = ProjectManager.calculation

    var demandKW by remember {
        mutableStateOf(
            if (project.demandKW > 0.0) {
                "%.2f".format(project.demandKW)
            } else {
                ""
            }
        )
    }

    var powerFactor by remember {
        mutableStateOf(
            if (project.powerFactor > 0.0) {
                "%.3f".format(project.powerFactor)
            } else {
                "0.900"
            }
        )
    }

    var margin by remember {
        mutableStateOf("20")
    }

    var result by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Transformer Sizing",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Project: ${
                project.projectName.ifBlank {
                    "Current Project"
                }
            }",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text(
            text = "Transformer Design Input",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = demandKW,
            onValueChange = {
                demandKW = it
            },
            label = {
                Text("Demand Load (kW)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = powerFactor,
            onValueChange = {
                powerFactor = it
            },
            label = {
                Text("Power Factor")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = margin,
            onValueChange = {
                margin = it
            },
            label = {
                Text("Design Margin (%)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val kw =
                    demandKW.toDoubleOrNull() ?: 0.0

                val pf =
                    powerFactor
                        .toDoubleOrNull()
                        ?.coerceIn(0.01, 1.0)
                        ?: 0.90

                val marginPercent =
                    margin.toDoubleOrNull()
                        ?.coerceAtLeast(0.0)
                        ?: 20.0

                if (kw <= 0.0) {

                    result =
                        "Please calculate Load first."

                } else {

                    ProjectManager.updateSystem(
                        powerFactor = pf
                    )

                    val transformer =
                        ElectricalCalculator.selectTransformer(
                            demandKW = kw,
                            pf = pf,
                            marginPercent = marginPercent
                        )

                    ProjectManager.setTransformer(
                        transformerKVA = transformer
                    )

                    result = """
                        TRANSFORMER SIZING
                        
                        Demand Load:
                        %.2f kW
                        
                        Power Factor:
                        %.3f
                        
                        Design Margin:
                        %.1f %%
                        
                        Required Transformer:
                        %.0f kVA
                        
                        Design Status:
                        %s
                        
                        ✓ Transformer result saved to ProjectManager
                        ✓ Transformer data available to Short Circuit
                    """.trimIndent().format(
                        kw,
                        pf,
                        marginPercent,
                        transformer,
                        ProjectManager.calculation.designStatus
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate & Save Transformer")
        }

        if (result.isNotEmpty()) {

            HorizontalDivider()

            Text(
                text = result,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "✓ Transformer saved to ProjectManager",
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = "✓ Short Circuit can use transformer rating",
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Current Project Transformer",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Demand Load: %.2f kW".format(
                ProjectManager.calculation.demandKW
            )
        )

        Text(
            text = "Power Factor: %.3f".format(
                ProjectManager.calculation.powerFactor
            )
        )

        Text(
            text = "Transformer: %.0f kVA".format(
                ProjectManager.calculation.transformerKVA
            )
        )

        Text(
            text = "Design Current: %.2f A".format(
                ProjectManager.calculation.designCurrentA
            )
        )

        Text(
            text = "Design Status: ${
                ProjectManager.calculation.designStatus
            }"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
