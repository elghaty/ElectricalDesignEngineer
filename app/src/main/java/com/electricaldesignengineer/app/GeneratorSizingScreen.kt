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
fun GeneratorSizingScreen(
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

    var loadingPercent by remember {
        mutableStateOf("80")
    }

    var motorAllowance by remember {
        mutableStateOf("15")
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
            text = "Generator Sizing",
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
            text = "Generator Design Input",
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
            value = loadingPercent,
            onValueChange = {
                loadingPercent = it
            },
            label = {
                Text("Generator Loading (%)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = motorAllowance,
            onValueChange = {
                motorAllowance = it
            },
            label = {
                Text("Motor Starting Allowance (%)")
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

                val loading =
                    loadingPercent
                        .toDoubleOrNull()
                        ?.coerceIn(1.0, 100.0)
                        ?: 80.0

                val motor =
                    motorAllowance
                        .toDoubleOrNull()
                        ?.coerceAtLeast(0.0)
                        ?: 15.0

                if (kw <= 0.0) {

                    result =
                        "Please calculate Load first."

                } else {

                    ProjectManager.updateSystem(
                        powerFactor = pf
                    )

                    val generator =
                        ElectricalCalculator.selectGenerator(
                            demandKW = kw,
                            pf = pf,
                            loadingPercent = loading,
                            motorAllowancePercent = motor
                        )

                    ProjectManager.setGenerator(
                        generatorKVA = generator
                    )

                    result = """
                        GENERATOR SIZING
                        
                        Demand Load:
                        %.2f kW
                        
                        Power Factor:
                        %.3f
                        
                        Generator Loading:
                        %.1f %%
                        
                        Motor Starting Allowance:
                        %.1f %%
                        
                        Recommended Generator:
                        %.0f kVA
                        
                        Design Status:
                        %s
                        
                        ✓ Generator result saved to ProjectManager
                        ✓ Generator data available to project
                    """.trimIndent().format(
                        kw,
                        pf,
                        loading,
                        motor,
                        generator,
                        ProjectManager.calculation.designStatus
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate & Save Generator")
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
                text = "✓ Generator saved to ProjectManager",
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Current Project Generator",
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
            text = "Generator: %.0f kVA".format(
                ProjectManager.calculation.generatorKVA
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
