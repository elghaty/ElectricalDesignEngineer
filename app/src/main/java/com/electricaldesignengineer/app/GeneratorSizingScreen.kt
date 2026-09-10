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

    var designMargin by remember {
        mutableStateOf("0")
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

        OutlinedTextField(
            value = designMargin,
            onValueChange = {
                designMargin = it
            },
            label = {
                Text("Design Margin (%)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val kw =
                    demandKW.toDoubleOrNull()

                val pf =
                    powerFactor.toDoubleOrNull()

                val loading =
                    loadingPercent.toDoubleOrNull()

                val motor =
                    motorAllowance.toDoubleOrNull()

                val margin =
                    designMargin.toDoubleOrNull()

                if (
                    kw == null ||
                    pf == null ||
                    loading == null ||
                    motor == null ||
                    margin == null
                ) {

                    result =
                        "Please enter valid numeric values."

                } else {

                    /*
                     * IMPORTANT:
                     *
                     * The screen performs NO generator engineering
                     * calculation.
                     *
                     * All calculations are delegated to
                     * ProfessionalEngineeringCore.
                     */

                    val input =
                        ProfessionalEngineeringCore.GeneratorDesignInput(
                            demandKW = kw,
                            powerFactor = pf,
                            loadingPercent = loading,
                            motorAllowancePercent = motor,
                            designMarginPercent = margin,

                            /*
                             * Voltage and frequency are intentionally
                             * taken from the current project system.
                             */
                            requiredVoltageV =
                                project.voltageV
                                    .takeIf { it > 0.0 },

                            requiredFrequencyHz =
                                project.frequencyHz
                                    .takeIf { it > 0.0 },

                            requirePrimeRating = false,
                            requireStandbyRating = false
                        )

                    /*
                     * Convert verified catalog records into the
                     * ProfessionalEngineeringCore data model.
                     *
                     * No generator rating is invented here.
                     */
                    val generatorData =
                        EngineeringCatalogRepository
                            .allGenerators()
                            .map { record ->

                                ProfessionalEngineeringCore.GeneratorData(

                                    id =
                                        record.id,

                                    manufacturerId =
                                        record.manufacturerId,

                                    manufacturerName =
                                        record.manufacturerName,

                                    catalogId =
                                        record.catalogId,

                                    catalogName =
                                        record.catalogName,

                                    catalogRevision =
                                        record.catalogRevision,

                                    productFamily =
                                        record.productFamily,

                                    model =
                                        record.model,

                                    ratedPowerKVA =
                                        record.ratedPowerKVA,

                                    ratedPowerKW =
                                        record.ratedPowerKW,

                                    ratedVoltageV =
                                        record.ratedVoltageV.toDouble(),

                                    frequencyHz =
                                        record.frequencyHz,

                                    powerFactor =
                                        record.powerFactor,

                                    standbyRating =
                                        record.standbyRating,

                                    primeRating =
                                        record.primeRating,

                                    shortCircuitDataAvailable =
                                        record.shortCircuitDataAvailable,

                                    standardCode =
                                        record.standardCode,

                                    sourceUrl =
                                        record.sourceUrl,

                                    verified =
                                        record.verified
                                )
                            }

                    val generatorResult =
                        ProfessionalEngineeringCore.designGenerator(
                            input = input,
                            generators = generatorData
                        )

                    when (generatorResult.status) {

                        EngineeringStatus.PASS -> {

                            val selected =
                                generatorResult.selectedGenerator

                            if (selected != null) {

                                ProjectManager.setGenerator(
                                    generatorKVA =
                                        selected.ratedPowerKVA
                                )

                                result = """
                                    GENERATOR SIZING

                                    Demand Load:
                                    %.2f kW

                                    Power Factor:
                                    %.3f

                                    Base Demand:
                                    %.2f kVA

                                    Motor Adjusted:
                                    %.2f kVA

                                    Required Generator:
                                    %.2f kVA

                                    Selected Generator:
                                    %.2f kVA

                                    Manufacturer:
                                    %s

                                    Product Family:
                                    %s

                                    Model:
                                    %s

                                    Loading:
                                    %.1f %%

                                    Motor Starting Allowance:
                                    %.1f %%

                                    Design Margin:
                                    %.1f %%

                                    Design Status:
                                    PASS

                                    ✓ Selected from verified catalog data
                                    ✓ Calculation performed by ProfessionalEngineeringCore
                                    ✓ Generator result saved to ProjectManager
                                """.trimIndent().format(
                                    generatorResult.baseDemandKVA *
                                            pf,
                                    pf,
                                    generatorResult.baseDemandKVA,
                                    generatorResult.motorAdjustedKVA,
                                    generatorResult.requiredGeneratorKVA,
                                    selected.ratedPowerKVA,
                                    selected.manufacturerName,
                                    selected.productFamily ?: "-",
                                    selected.model ?: "-",
                                    loading,
                                    motor,
                                    margin
                                )

                            } else {

                                result =
                                    "Generator calculation passed, but no generator record was returned."
                            }
                        }

                        EngineeringStatus.DATA_REQUIRED -> {

                            result = """
                                GENERATOR SIZING

                                Required Generator:
                                %.2f kVA

                                Status:
                                DATA REQUIRED

                                No verified generator catalog record
                                satisfies the calculated requirement.

                                Add verified manufacturer/project-approved
                                generator data to EngineeringCatalogRepository.
                            """.trimIndent().format(
                                generatorResult.requiredGeneratorKVA
                            )
                        }

                        EngineeringStatus.FAIL -> {

                            result =
                                buildString {

                                    appendLine("GENERATOR SIZING")
                                    appendLine()
                                    appendLine("Status: FAIL")
                                    appendLine()

                                    generatorResult.checks.forEach { check ->

                                        appendLine(
                                            "${check.name}: ${check.message}"
                                        )
                                    }
                                }
                        }

                        EngineeringStatus.WARNING -> {

                            result =
                                "Generator calculation returned WARNING.\n\n" +
                                        generatorResult.checks.joinToString(
                                            separator = "\n"
                                        ) {
                                            "${it.name}: ${it.message}"
                                        }
                        }

                        EngineeringStatus.NOT_CALCULATED -> {

                            result =
                                "Generator calculation was not completed."
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate & Select Generator")
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
            text = "Generator: %.2f kVA".format(
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
