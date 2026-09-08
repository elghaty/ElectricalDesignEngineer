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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ShortCircuitScreen(
    onBack: () -> Unit = {}
) {

    val project =
        ProjectManager.calculation

    var resultText by remember {
        mutableStateOf("")
    }

    var resultStatus by remember {
        mutableStateOf<EngineeringStatus?>(
            null
        )
    }

    var faultCurrentKA by remember {
        mutableStateOf(0.0)
    }

    var impedanceOhm by remember {
        mutableStateOf(0.0)
    }

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)

    ) {

        // ========================================================
        // TITLE
        // ========================================================

        Text(
            text = "Short Circuit Current",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Text(
            text =
                "Project: ${
                    project.projectName.ifBlank {
                        "Current Project"
                    }
                }"
        )

        HorizontalDivider()

        // ========================================================
        // SOURCE DATA
        // ========================================================

        Text(
            text = "Transformer Source Data",
            style =
                MaterialTheme.typography.titleMedium
        )

        Text(
            text =
                "Transformer Rating: ${
                    if (project.transformerKVA > 0.0)
                        "%.0f kVA"
                            .format(
                                project.transformerKVA
                            )
                    else
                        "NOT AVAILABLE"
                }"
        )

        Text(
            text =
                "LV Voltage: ${
                    if (project.voltageV > 0.0)
                        "%.0f V"
                            .format(
                                project.voltageV
                            )
                    else
                        "NOT AVAILABLE"
                }"
        )

        Text(
            text =
                "Transformer %Z: ${
                    if (
                        project.transformerImpedancePercent > 0.0
                    ) {
                        "%.2f %%"
                            .format(
                                project
                                    .transformerImpedancePercent
                            )
                    } else {
                        "NOT AVAILABLE"
                    }
                }"
        )

        Text(
            text =
                "The transformer impedance must come from the verified transformer catalog/nameplate data."
        )

        // ========================================================
        // CALCULATE
        // ========================================================

        Button(

            onClick = {

                resultText = ""
                resultStatus = null
                faultCurrentKA = 0.0
                impedanceOhm = 0.0

                val kva =
                    project.transformerKVA

                val voltage =
                    project.voltageV

                val impedance =
                    project.transformerImpedancePercent

                if (kva <= 0.0) {

                    resultStatus =
                        EngineeringStatus.DATA_REQUIRED

                    resultText =
                        "DATA_REQUIRED:\n" +
                                "A valid selected transformer is required.\n" +
                                "Complete Transformer Sizing first."

                    return@Button
                }

                if (voltage <= 0.0) {

                    resultStatus =
                        EngineeringStatus.DATA_REQUIRED

                    resultText =
                        "DATA_REQUIRED:\n" +
                                "A valid LV system voltage is required."

                    return@Button
                }

                if (impedance <= 0.0) {

                    resultStatus =
                        EngineeringStatus.DATA_REQUIRED

                    resultText =
                        "DATA_REQUIRED:\n" +
                                "Verified transformer %Z is missing.\n" +
                                "The program will NOT assume 6% or any other default value."

                    return@Button
                }

                val calculation =
                    ShortCircuitDesignService
                        .calculateTransformerBusFault(

                            transformerKVA =
                                kva,

                            voltageV =
                                voltage,

                            transformerImpedancePercent =
                                impedance
                        )

                resultStatus =
                    calculation.status

                faultCurrentKA =
                    calculation.faultCurrentKA

                impedanceOhm =
                    calculation
                        .equivalentImpedanceOhm
                        ?: 0.0

                if (
                    calculation.status ==
                    EngineeringStatus.PASS
                ) {

                    ProjectManager.setShortCircuit(
                        shortCircuitKA =
                            calculation.faultCurrentKA
                    )

                    resultText =
                        buildString {

                            appendLine(
                                "SHORT CIRCUIT CALCULATION"
                            )

                            appendLine()

                            appendLine(
                                "Transformer:"
                            )

                            appendLine(
                                "%.0f kVA"
                                    .format(kva)
                            )

                            appendLine()

                            appendLine(
                                "LV Voltage:"
                            )

                            appendLine(
                                "%.0f V"
                                    .format(voltage)
                            )

                            appendLine()

                            appendLine(
                                "Transformer %Z:"
                            )

                            appendLine(
                                "%.2f %%"
                                    .format(impedance)
                            )

                            appendLine()

                            appendLine(
                                "Equivalent Impedance:"
                            )

                            appendLine(
                                "%.6f Ω"
                                    .format(
                                        impedanceOhm
                                    )
                            )

                            appendLine()

                            appendLine(
                                "Prospective Short Circuit:"
                            )

                            appendLine(
                                "%.3f kA"
                                    .format(
                                        calculation
                                            .faultCurrentKA
                                    )
                            )

                            appendLine()

                            appendLine(
                                "Status:"
                            )

                            appendLine(
                                calculation.status.name
                            )

                            if (
                                calculation
                                    .warnings
                                    .isNotEmpty()
                            ) {

                                appendLine()

                                appendLine(
                                    "Warnings:"
                                )

                                calculation
                                    .warnings
                                    .forEach {
                                        appendLine(
                                            "• $it"
                                        )
                                    }
                            }
                        }

                } else {

                    resultText =
                        buildString {

                            appendLine(
                                calculation.message
                            )

                            appendLine()

                            calculation
                                .checks
                                .forEach { check ->

                                    appendLine(
                                        "${check.name}: " +
                                                check.status.name
                                    )

                                    appendLine(
                                        check.message
                                    )

                                    appendLine()
                                }
                        }
                }
            },

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text(
                "Calculate Short Circuit"
            )
        }

        // ========================================================
        // RESULT
        // ========================================================

        if (resultText.isNotBlank()) {

            HorizontalDivider()

            Text(
                text = resultText,
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        // ========================================================
        // CURRENT PROJECT RESULT
        // ========================================================

        HorizontalDivider()

        Text(
            text = "Current Project Result",
            style =
                MaterialTheme.typography.titleMedium
        )

        Text(
            text =
                "Transformer: ${
                    "%.0f kVA"
                        .format(
                            ProjectManager
                                .calculation
                                .transformerKVA
                        )
                }"
        )

        Text(
            text =
                "Transformer %Z: ${
                    if (
                        ProjectManager
                            .calculation
                            .transformerImpedancePercent > 0.0
                    ) {
                        "%.2f %%"
                            .format(
                                ProjectManager
                                    .calculation
                                    .transformerImpedancePercent
                            )
                    } else {
                        "NOT AVAILABLE"
                    }
                }"
        )

        Text(
            text =
                "Voltage: ${
                    "%.0f V"
                        .format(
                            ProjectManager
                                .calculation
                                .voltageV
                        )
                }"
        )

        Text(
            text =
                "Short Circuit: ${
                    "%.3f kA"
                        .format(
                            ProjectManager
                                .calculation
                                .shortCircuitKA
                        )
                }"
        )

        Text(
            text =
                "Status: ${
                    resultStatus?.name
                        ?: ProjectManager
                            .calculation
                            .designStatus
                }"
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        // ========================================================
        // BACK
        // ========================================================

        Button(

            onClick = onBack,

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text("Back")
        }
    }
}
