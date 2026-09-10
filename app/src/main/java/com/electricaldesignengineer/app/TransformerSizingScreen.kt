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
            val pf = powerFactor.toDoubleOrNull()
            val marginPercent = margin.toDoubleOrNull()

            if (kw == null || kw <= 0.0) {
                result =
                    "DATA_REQUIRED: Enter a valid demand load greater than zero."
                return@Button
            }

            if (pf == null || pf !in 0.01..1.0) {
                result =
                    "DATA_REQUIRED: Power factor must be between 0.01 and 1.00."
                return@Button
            }

            if (marginPercent == null || marginPercent < 0.0) {
                result =
                    "DATA_REQUIRED: Design margin must be zero or greater."
                return@Button
            }

            /*
             * ============================================================
             * STEP 1
             * ============================================================
             *
             * Calculate transformer required capacity inside the
             * ProfessionalEngineeringCore.
             *
             * No engineering formula is performed in the UI.
             */
            val requiredKVAResult =
                ProfessionalEngineeringCore.requiredTransformerKVA(
                    demandKW = kw,
                    powerFactor = pf,
                    designMarginPercent = marginPercent
                )

            if (requiredKVAResult.status != EngineeringStatus.PASS) {
                result = buildString {
                    appendLine("TRANSFORMER REQUIRED KVA")
                    appendLine()
                    appendLine("Status: ${requiredKVAResult.status}")
                    appendLine()

                    requiredKVAResult.checks.forEach { check ->
                        appendLine(
                            "${check.name}: ${check.message}"
                        )
                    }
                }

                return@Button
            }

            /*
             * ============================================================
             * STEP 2
             * ============================================================
             *
             * Obtain verified transformer catalog data.
             *
             * The calculation core selects the transformer.
             * The UI does not contain a hardcoded transformer rating list.
             */
            val transformerData =
                EngineeringCatalogRepository
                    .allTransformers()
                    .filter { it.verified }
                    .map { transformer ->

                        ProfessionalEngineeringCore.TransformerData(
                            id = transformer.id,
                            manufacturerId = transformer.manufacturerId,
                            manufacturerName = transformer.manufacturerName,
                            catalogId = transformer.catalogId,
                            catalogName = transformer.catalogName,
                            catalogRevision = transformer.catalogRevision,
                            productFamily = transformer.productFamily,
                            partNumber = transformer.partNumber,
                            ratedPowerKVA = transformer.ratedPowerKVA,
                            primaryVoltageV = transformer.primaryVoltageV,
                            secondaryVoltageV = transformer.secondaryVoltageV,
                            frequencyHz = transformer.frequencyHz,
                            vectorGroup = transformer.vectorGroup,
                            impedancePercent = transformer.impedancePercent,
                            noLoadLossKW = transformer.noLoadLossKW,
                            loadLossKW = transformer.loadLossKW,
                            coolingClass = transformer.coolingClass,
                            standardCode = transformer.standardCode,
                            sourceUrl = transformer.sourceUrl,
                            verified = transformer.verified
                        )
                    }

            /*
             * ============================================================
             * STEP 3
             * ============================================================
             *
             * Select the smallest verified transformer satisfying the
             * calculated requirement.
             *
             * The margin is already included in requiredKVAResult.
             * Therefore designTransformer receives the calculated
             * capacity and zero additional margin.
             *
             * This prevents applying the design margin twice.
             */
            val transformerResult =
                ProfessionalEngineeringCore.designTransformer(
                    input =
                        ProfessionalEngineeringCore.TransformerDesignInput(
                            requiredKVA =
                                requiredKVAResult.requiredKVAWithoutMargin,
                            designMarginPercent = marginPercent,
                            requiredPrimaryVoltageV = null,
                            requiredSecondaryVoltageV =
                                project.voltageV.takeIf { it > 0.0 },
                            requiredFrequencyHz =
                                project.frequencyHz.takeIf { it > 0.0 }
                        ),
                    transformers = transformerData
                )

            val transformer =
                transformerResult.selectedTransformer

            if (
                transformerResult.status ==
                EngineeringStatus.PASS &&
                transformer != null
            ) {

                ProjectManager.updateSystem(
                    powerFactor = pf
                )

                ProjectManager.setTransformer(
                    transformerKVA =
                        transformer.ratedPowerKVA
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

                    Required kVA Without Margin:
                    %.2f kVA

                    Design Margin:
                    %.1f %%

                    Required Transformer Capacity:
                    %.2f kVA

                    Selected Transformer:
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

                    Cooling Class:
                    %s

                    Standard:
                    %s

                    Design Status:
                    %s

                    ✓ Calculation performed by ProfessionalEngineeringCore
                    ✓ Verified catalog data used
                    ✓ Transformer result saved to ProjectManager
                """.trimIndent().format(
                    requiredKVAResult.demandKW,
                    requiredKVAResult.powerFactor,
                    requiredKVAResult.requiredKVAWithoutMargin,
                    requiredKVAResult.designMarginPercent,
                    requiredKVAResult.requiredKVA,
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
                    transformer.coolingClass ?: "N/A",
                    transformer.standardCode ?: "N/A",
                    transformerResult.status
                )

            } else {

                result = buildString {

                    appendLine("TRANSFORMER DESIGN RESULT")
                    appendLine()
                    appendLine(
                        "Status: ${transformerResult.status}"
                    )
                    appendLine()

                    transformerResult.checks.forEach { check ->

                        appendLine(
                            "${check.name}: ${check.message}"
                        )

                        check.calculatedValue?.let {
                            appendLine(
                                "Calculated: %.3f ${check.unit}".format(it)
                            )
                        }

                        check.requiredValue?.let {
                            appendLine(
                                "Required: %.3f ${check.unit}".format(it)
                            )
                        }

                        appendLine()
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
