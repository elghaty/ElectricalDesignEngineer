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
            val kw = demandKW.toDoubleOrNull()

            val pf = powerFactor
                .toDoubleOrNull()
                ?.takeIf { it > 0.0 && it <= 1.0 }

            val marginPercent = margin
                .toDoubleOrNull()
                ?.takeIf { it >= 0.0 }

            if (kw == null || kw <= 0.0) {
                result = "DATA_REQUIRED: Enter a valid demand load in kW."
                return@Button
            }

            if (pf == null) {
                result = "DATA_REQUIRED: Enter a valid power factor between 0 and 1."
                return@Button
            }

            if (marginPercent == null) {
                result = "DATA_REQUIRED: Enter a valid design margin."
                return@Button
            }

            ProjectManager.updateSystem(
                powerFactor = pf
            )

            val transformerResult =
                AutoDesignService.designTransformer(
                    demandKVA = kw / pf,
                    designMarginPercent = marginPercent,
                    primaryVoltageV = null,
                    secondaryVoltageV = project.voltageV,
                    frequencyHz = project.frequencyHz
                )

            val transformer =
                transformerResult.selectedTransformer

            if (transformer != null &&
                transformerResult.status == EngineeringStatus.PASS
            ) {
                ProjectManager.setTransformer(
                    transformerKVA = transformer.ratedPowerKVA
                )

                transformer.impedancePercent?.let {
                    ProjectManager.setTransformerImpedance(it)
                }

                result = """
                    TRANSFORMER SIZING

                    Demand Load:
                    %.2f kW

                    Power Factor:
                    %.3f

                    Demand:
                    %.2f kVA

                    Design Margin:
                    %.1f %%

                    Required Transformer:
                    %.0f kVA

                    Selected Manufacturer:
                    %s

                    Product Family:
                    %s

                    Part Number:
                    %s

                    Primary Voltage:
                    %.0f V

                    Secondary Voltage:
                    %.0f V

                    Frequency:
                    %.1f Hz

                    Vector Group:
                    %s

                    Impedance:
                    %s

                    Standard:
                    %s

                    Design Status:
                    %s

                    ✓ Transformer result saved to ProjectManager
                """.trimIndent().format(
                    kw,
                    pf,
                    kw / pf,
                    marginPercent,
                    transformer.ratedPowerKVA,
                    transformer.manufacturerName,
                    transformer.productFamily,
                    transformer.partNumber ?: "N/A",
                    transformer.primaryVoltageV,
                    transformer.secondaryVoltageV,
                    transformer.frequencyHz,
                    transformer.vectorGroup ?: "N/A",
                    transformer.impedancePercent?.let {
                        "%.2f %%".format(it)
                    } ?: "N/A",
                    transformer.standardCode ?: "N/A",
                    transformerResult.status
                )
            } else {
                result = buildString {
                    append("Transformer Design Result\n\n")
                    append("Status: ${transformerResult.status}\n\n")
                    append(transformerResult.message)

                    if (transformerResult.warnings.isNotEmpty()) {
                        append("\n\nWarnings:\n")
                        transformerResult.warnings.forEach {
                            append("• ")
                            append(it)
                            append('\n')
                        }
                    }
                }
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
        text = "Transformer Impedance: %.2f %%".format(
            ProjectManager.calculation.transformerImpedancePercent
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
