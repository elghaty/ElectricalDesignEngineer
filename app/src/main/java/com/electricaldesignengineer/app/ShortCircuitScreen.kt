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

    // =========================
    // TRANSFORMER KVA
    // =========================

    var transformerKVA by remember {

        mutableStateOf(

            if (project.transformerKVA > 0.0) {

                "%.0f".format(
                    project.transformerKVA
                )

            } else {

                "1000"
            }
        )
    }

    // =========================
    // VOLTAGE
    // =========================

    var voltage by remember {

        mutableStateOf(

            if (project.voltageV > 0.0) {

                "%.0f".format(
                    project.voltageV
                )

            } else {

                "400"
            }
        )
    }

    // =========================
    // TRANSFORMER IMPEDANCE
    // =========================

    var impedance by remember {

        mutableStateOf(

            "%.2f".format(
                project.transformerImpedancePercent
            )
        )
    }

    // =========================
    // RESULT
    // =========================

    var result by remember {
        mutableStateOf("")
    }

    // =========================
    // CALCULATION
    // =========================

    var ratedCurrentA by remember {
        mutableStateOf(0.0)
    }

    var faultCurrentKA by remember {
        mutableStateOf(0.0)
    }

    Column(

        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)

    ) {

        // =========================
        // TITLE
        // =========================

        Text(
            text = "Short Circuit Current",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Project: ${
                project.projectName.ifBlank {
                    "Current Project"
                }
            }",
            style =
                MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        // =========================
        // ENGINEERING INPUTS
        // =========================

        Text(
            text = "Transformer Data",
            style =
                MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(

            value = transformerKVA,

            onValueChange = {
                transformerKVA = it
            },

            label = {
                Text(
                    "Transformer Rating (kVA)"
                )
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        OutlinedTextField(

            value = voltage,

            onValueChange = {
                voltage = it
            },

            label = {
                Text(
                    "LV Voltage (V)"
                )
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        OutlinedTextField(

            value = impedance,

            onValueChange = {
                impedance = it
            },

            label = {
                Text(
                    "Transformer Impedance (%Z)"
                )
            },

            modifier =
                Modifier.fillMaxWidth()
        )

        Text(
            text =
                "Final %Z must be taken from the transformer nameplate or manufacturer data.",
            style =
                MaterialTheme.typography.bodySmall
        )

        // =========================
        // CALCULATE
        // =========================

        Button(

            onClick = {

                val kva =
                    transformerKVA
                        .toDoubleOrNull()
                        ?.coerceAtLeast(0.0)
                        ?: 0.0

                val v =
                    voltage
                        .toDoubleOrNull()
                        ?.coerceAtLeast(0.0)
                        ?: 0.0

                val z =
                    impedance
                        .toDoubleOrNull()
                        ?.coerceAtLeast(0.01)
                        ?: 0.0

                if (
                    kva <= 0.0 ||
                    v <= 0.0 ||
                    z <= 0.0
                ) {

                    result =
                        "Please enter valid transformer data."

                    ratedCurrentA = 0.0
                    faultCurrentKA = 0.0

                } else {

                    // -------------------------
                    // SAVE SYSTEM VOLTAGE
                    // -------------------------

                    ProjectManager.updateSystem(
                        voltageV = v
                    )

                    // -------------------------
                    // SAVE TRANSFORMER
                    // -------------------------

                    ProjectManager.setTransformer(

                        transformerKVA = kva,

                        transformerImpedancePercent =
                            z
                    )

                    // -------------------------
                    // RATED CURRENT
                    // -------------------------

                    ratedCurrentA =
                        ElectricalCalculator
                            .threePhaseCurrent(
                                kva = kva,
                                voltage = v
                            )

                    // -------------------------
                    // SHORT CIRCUIT
                    // -------------------------

                    faultCurrentKA =
                        ElectricalCalculator
                            .transformerShortCircuit(

                                transformerKVA =
                                    kva,

                                voltage =
                                    v,

                                impedancePercent =
                                    z
                            )

                    // -------------------------
                    // SAVE RESULT
                    // -------------------------

                    ProjectManager.setShortCircuit(
                        shortCircuitKA =
                            faultCurrentKA
                    )

                    result = """
                        SHORT CIRCUIT CALCULATION

                        Transformer Rating:
                        %.0f kVA

                        LV Voltage:
                        %.0f V

                        Transformer Impedance:
                        %.2f %%

                        Transformer Rated Current:
                        %.2f A

                        Prospective Short Circuit:
                        %.2f kA

                        Formula:
                        Icc = In / (%Z / 100)

                        Design Status:
                        %s

                        ✓ Transformer data saved
                        ✓ Short-circuit result saved
                        ✓ Breaker Selection can use this Icc
                    """.trimIndent().format(

                        kva,

                        v,

                        z,

                        ratedCurrentA,

                        faultCurrentKA,

                        ProjectManager
                            .calculation
                            .designStatus
                    )
                }
            },

            modifier =
                Modifier.fillMaxWidth()

        ) {
            Text(
                "Calculate & Save Short Circuit"
            )
        }

        // =========================
        // RESULT
        // =========================

        if (result.isNotEmpty()) {

            HorizontalDivider()

            Text(
                text = result,
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        // =========================
        // PROJECT RESULT
        // =========================

        HorizontalDivider()

        Text(
            text = "Current Project Result",
            style =
                MaterialTheme.typography.titleMedium
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
                    .format(
                        ratedCurrentA
                    )
        )

        Text(
            text =
                "Short Circuit: %.2f kA"
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
            modifier =
                Modifier.height(10.dp)
        )

        // =========================
        // BACK
        // =========================

        Button(

            onClick = onBack,

            modifier =
                Modifier.fillMaxWidth()

        ) {
            Text("Back")
        }
    }
}
