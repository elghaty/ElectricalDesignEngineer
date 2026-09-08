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
fun ShortCircuitScreen(
    onBack: () -> Unit = {}
) {

    val project = ProjectManager.calculation

    // ============================================================
    // INPUTS
    // ============================================================

    var transformerKVA by remember {
        mutableStateOf(
            if (project.transformerKVA > 0.0)
                "%.0f".format(project.transformerKVA)
            else
                ""
        )
    }

    var voltage by remember {
        mutableStateOf(
            if (project.voltageV > 0.0)
                "%.0f".format(project.voltageV)
            else
                ""
        )
    }

    var impedancePercent by remember {
        mutableStateOf(
            if (project.transformerImpedancePercent > 0.0)
                "%.2f".format(project.transformerImpedancePercent)
            else
                ""
        )
    }

    // ============================================================
    // RESULT
    // ============================================================

    var resultText by remember {
        mutableStateOf("")
    }

    var faultCurrentKA by remember {
        mutableStateOf(project.shortCircuitKA)
    }

    var ratedCurrentA by remember {
        mutableStateOf(0.0)
    }

    // ============================================================
    // UI
    // ============================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),

        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Short Circuit Current",
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

        // ========================================================
        // TRANSFORMER DATA
        // ========================================================

        Text(
            text = "Transformer Data",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = transformerKVA,
            onValueChange = {
                transformerKVA = it
            },
            label = {
                Text("Transformer Rating (kVA)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = voltage,
            onValueChange = {
                voltage = it
            },
            label = {
                Text("LV Voltage (V)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = impedancePercent,
            onValueChange = {
                impedancePercent = it
            },
            label = {
                Text("Transformer Impedance (%Z)")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Use the transformer nameplate/manufacturer value for %Z.",
            style = MaterialTheme.typography.bodySmall
        )

        // ========================================================
        // CALCULATE
        // ========================================================

        Button(
            onClick = {

                val kva =
                    transformerKVA
                        .toDoubleOrNull()
                        ?.takeIf { it > 0.0 }

                val v =
                    voltage
                        .toDoubleOrNull()
                        ?.takeIf { it > 0.0 }

                val z =
                    impedancePercent
                        .toDoubleOrNull()
                        ?.takeIf { it > 0.0 }

                if (kva == null || v == null || z == null) {

                    resultText =
                        """
                        DATA REQUIRED

                        Please enter:

                        • Transformer rating
                        • LV voltage
                        • Transformer %Z

                        No engineering value was assumed.
                        """.trimIndent()

                    faultCurrentKA = 0.0
                    ratedCurrentA = 0.0

                } else {

                    // ====================================================
                    // SAVE SYSTEM DATA
                    // ====================================================

                    ProjectManager.updateSystem(
                        voltageV = v
                    )

                    ProjectManager.setTransformer(
                        transformerKVA = kva,
                        transformerImpedancePercent = z
                    )

                    // ====================================================
                    // TRANSFORMER RATED CURRENT
                    // ====================================================

                    ratedCurrentA =
                        ProfessionalEngineeringCore.threePhaseCurrent(
                            kva = kva,
                            voltageV = v
                        )

                    // ====================================================
                    // PROFESSIONAL SHORT-CIRCUIT ENGINE
                    // ====================================================

                    val input =
                        ShortCircuitInput(

                            faultType =
                                ShortCircuitFaultType.THREE_PHASE,

                            source =
                                ShortCircuitSourceInput(

                                    transformerKVA = kva,

                                    voltageV = v,

                                    transformerImpedancePercent = z,

                                    transformerResistancePercent = null,

                                    transformerReactancePercent = null,

                                    upstreamShortCircuitKA = null,

                                    upstreamResistanceOhm = null,

                                    upstreamReactanceOhm = null
                                ),

                            cable = null
                        )

                    val calculation =
                        ProfessionalEngineeringCore
                            .calculateShortCircuit(input)

                    // ====================================================
                    // RESULT
                    // ====================================================

                    faultCurrentKA =
                        calculation.faultCurrentKA

                    // ====================================================
                    // SAVE RESULT ONLY IF VALID
                    // ====================================================

                    if (
                        calculation.status ==
                        EngineeringStatus.PASS
                    ) {

                        ProjectManager.setShortCircuit(
                            shortCircuitKA =
                                calculation.faultCurrentKA
                        )
                    }

                    // ====================================================
                    // BUILD RESULT TEXT
                    // ====================================================

                    val impedance =
                        calculation.equivalentImpedance

                    val statusText =
                        when (calculation.status) {

                            EngineeringStatus.PASS ->
                                "PASS"

                            EngineeringStatus.FAIL ->
                                "FAIL"

                            EngineeringStatus.WARNING ->
                                "WARNING"

                            EngineeringStatus.DATA_REQUIRED ->
                                "DATA REQUIRED"

                            EngineeringStatus.NOT_CALCULATED ->
                                "NOT CALCULATED"
                        }

                    val checksText =
                        if (calculation.checks.isEmpty()) {

                            "No checks returned."

                        } else {

                            calculation.checks.joinToString(
                                separator = "\n"
                            ) { check ->

                                val value =
                                    check.calculatedValue
                                        ?.let {
                                            "%.4f".format(it)
                                        }
                                        ?: "-"

                                "• ${check.name}: " +
                                        "${check.status} | " +
                                        "$value ${check.unit} | " +
                                        check.message
                            }
                        }

                    resultText =
                        buildString {

                            appendLine(
                                "SHORT CIRCUIT CALCULATION"
                            )

                            appendLine()

                            appendLine(
                                "Status: $statusText"
                            )

                            appendLine()

                            appendLine(
                                "Transformer Rating: " +
                                        "%.0f kVA".format(kva)
                            )

                            appendLine(
                                "LV Voltage: " +
                                        "%.0f V".format(v)
                            )

                            appendLine(
                                "Transformer %Z: " +
                                        "%.2f %%".format(z)
                            )

                            appendLine()

                            appendLine(
                                "Transformer Rated Current: " +
                                        "%.2f A"
                                            .format(ratedCurrentA)
                            )

                            appendLine()

                            appendLine(
                                "Prospective Short Circuit: " +
                                        "%.3f kA"
                                            .format(
                                                calculation
                                                    .faultCurrentKA
                                            )
                            )

                            if (impedance != null) {

                                appendLine()

                                appendLine(
                                    "Equivalent Impedance:"
                                )

                                appendLine(
                                    "R = %.6f Ω"
                                        .format(
                                            impedance
                                                .resistanceOhm
                                        )
                                )

                                appendLine(
                                    "X = %.6f Ω"
                                        .format(
                                            impedance
                                                .reactanceOhm
                                        )
                                )

                                appendLine(
                                    "|Z| = %.6f Ω"
                                        .format(
                                            impedance
                                                .magnitudeOhm
                                        )
                                )
                            }

                            appendLine()

                            appendLine(
                                "ENGINEERING CHECKS"
                            )

                            appendLine()

                            appendLine(checksText)

                            appendLine()

                            appendLine(
                                "Standard: " +
                                        (
                                            calculation.trace.standard
                                                ?.code
                                                ?: "Not specified"
                                        )
                            )

                            if (
                                calculation.trace
                                    .assumptions
                                    .isNotEmpty()
                            ) {

                                appendLine()

                                appendLine(
                                    "ASSUMPTIONS"
                                )

                                calculation.trace
                                    .assumptions
                                    .forEach {

                                        appendLine(
                                            "• $it"
                                        )
                                    }
                            }

                            if (
                                calculation.trace
                                    .warnings
                                    .isNotEmpty()
                            ) {

                                appendLine()

                                appendLine(
                                    "WARNINGS"
                                )

                                calculation.trace
                                    .warnings
                                    .forEach {

                                        appendLine(
                                            "• $it"
                                        )
                                    }
                            }
                        }
                }
            },

            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = "Calculate & Save Short Circuit"
            )
        }

        // ========================================================
        // RESULT
        // ========================================================

        if (resultText.isNotBlank()) {

            HorizontalDivider()

            Text(
                text = resultText,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // ========================================================
        // CURRENT PROJECT RESULT
        // ========================================================

        HorizontalDivider()

        Text(
            text = "Current Project Result",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text =
                "Transformer: %.0f kVA"
                    .format(
                        ProjectManager
                            .calculation
                            .transformerKVA
                    )
        )

        Text(
            text =
                "Transformer %Z: %.2f %%"
                    .format(
                        ProjectManager
                            .calculation
                            .transformerImpedancePercent
                    )
        )

        Text(
            text =
                "Voltage: %.0f V"
                    .format(
                        ProjectManager
                            .calculation
                            .voltageV
                    )
        )

        Text(
            text =
                "Transformer Rated Current: %.2f A"
                    .format(ratedCurrentA)
        )

        Text(
            text =
                "Short Circuit: %.3f kA"
                    .format(
                        ProjectManager
                            .calculation
                            .shortCircuitKA
                    )
        )

        Text(
            text =
                "Design Status: ${
                    ProjectManager
                        .calculation
                        .designStatus
                }"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        // ========================================================
        // BACK
        // ========================================================

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Back")
        }
    }
}
