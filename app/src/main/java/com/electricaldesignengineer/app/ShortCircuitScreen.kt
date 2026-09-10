package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class TransformerSource {
    STANDARD_RATING,
    NAMEPLATE,
    VERIFIED_CATALOG
}

@Composable
fun ShortCircuitScreen(
    onBack: () -> Unit = {}
) {

    val project = ProjectManager.calculation

    val standardTransformers =
        remember {
            EngineeringCatalogRepository
                .standardTransformerOptions()
                .sortedBy { it.ratedPowerKVA }
        }

    val verifiedTransformers =
        remember {
            EngineeringCatalogRepository
                .allTransformers()
                .filter { it.verified }
                .sortedBy { it.ratedPowerKVA }
        }

    var transformerSource by remember {
        mutableStateOf(
            if (verifiedTransformers.isNotEmpty()) {
                TransformerSource.VERIFIED_CATALOG
            } else {
                TransformerSource.STANDARD_RATING
            }
        )
    }

    val requiredKVA =
        when {
            project.totalKVA > 0.0 ->
                project.totalKVA

            project.demandKW > 0.0 ->
                project.demandKW /
                    project.powerFactor.coerceIn(
                        0.01,
                        1.0
                    )

            else ->
                0.0
        }

    val recommendedStandard =
        standardTransformers
            .firstOrNull {
                it.ratedPowerKVA >= requiredKVA
            }
            ?: standardTransformers.lastOrNull()

    var selectedStandardTransformer by remember {
        mutableStateOf(
            recommendedStandard
        )
    }

    var selectedCatalogTransformer by remember {
        mutableStateOf(
            verifiedTransformers.firstOrNull()
        )
    }

    var standardMenuExpanded by remember {
        mutableStateOf(false)
    }

    var catalogMenuExpanded by remember {
        mutableStateOf(false)
    }

    var nameplateKVA by remember {
        mutableStateOf(
            if (project.transformerKVA > 0.0) {
                project.transformerKVA
                    .toString()
            } else {
                ""
            }
        )
    }

    var nameplateVoltage by remember {
        mutableStateOf(
            if (project.voltageV > 0.0) {
                project.voltageV
                    .toString()
            } else {
                ""
            }
        )
    }

    var nameplateImpedance by remember {
        mutableStateOf(
            if (
                project.transformerImpedancePercent >
                0.0
            ) {
                project.transformerImpedancePercent
                    .toString()
            } else {
                ""
            }
        )
    }

    var resultText by remember {
        mutableStateOf("")
    }

    var ratedCurrentA by remember {
        mutableStateOf(0.0)
    }

    fun sourceName(): String {
        return when (transformerSource) {

            TransformerSource.STANDARD_RATING ->
                "Standard Transformer Rating"

            TransformerSource.NAMEPLATE ->
                "Existing Transformer / Nameplate"

            TransformerSource.VERIFIED_CATALOG ->
                "Verified Manufacturer Catalog"
        }
    }

    fun parsePositive(
        value: String
    ): Double? {
        return value
            .replace(",", ".")
            .trim()
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }
    }

    fun calculate(
        kva: Double,
        voltageV: Double,
        impedancePercent: Double,
        referenceMode: Boolean
    ) {

        if (
            kva <= 0.0 ||
            voltageV <= 0.0 ||
            impedancePercent <= 0.0
        ) {

            resultText =
                """
                DATA REQUIRED

                Transformer rating, LV voltage and transformer %Z
                must all be supplied as positive engineering values.

                IMPORTANT:
                Transformer %Z is not assumed by the software.
                """.trimIndent()

            return
        }

        ProjectManager.updateSystem(
            voltageV = voltageV
        )

        ProjectManager.setTransformer(
            transformerKVA = kva,
            transformerImpedancePercent =
                impedancePercent
        )

        ratedCurrentA =
            ProfessionalEngineeringCore
                .threePhaseCurrent(
                    kva = kva,
                    voltageV = voltageV
                )

        val input =
            ShortCircuitInput(
                faultType =
                    ShortCircuitFaultType.THREE_PHASE,

                source =
                    ShortCircuitSourceInput(
                        transformerKVA = kva,
                        voltageV = voltageV,
                        transformerImpedancePercent =
                            impedancePercent,
                        transformerResistancePercent =
                            null,
                        transformerReactancePercent =
                            null,
                        upstreamShortCircuitKA =
                            null,
                        upstreamResistanceOhm =
                            null,
                        upstreamReactanceOhm =
                            null
                    ),

                cable = null
            )

        val calculation =
            ProfessionalEngineeringCore
                .calculateShortCircuit(
                    input
                )

        val status =
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

        val impedance =
            calculation.equivalentImpedance

        resultText =
            buildString {

                appendLine(
                    "SHORT CIRCUIT CALCULATION"
                )

                appendLine()

                appendLine(
                    "Source: ${sourceName()}"
                )

                appendLine()

                appendLine(
                    "Status: $status"
                )

                appendLine()

                appendLine(
                    "Transformer Rating: " +
                        "%.0f kVA".format(kva)
                )

                appendLine(
                    "LV Voltage: " +
                        "%.0f V".format(voltageV)
                )

                appendLine(
                    "Transformer %Z: " +
                        "%.2f %%".format(
                            impedancePercent
                        )
                )

                appendLine()

                appendLine(
                    "Transformer Rated Current: " +
                        "%.2f A".format(
                            ratedCurrentA
                        )
                )

                appendLine()

                appendLine(
                    "Prospective Short Circuit: " +
                        "%.3f kA".format(
                            calculation.faultCurrentKA
                        )
                )

                if (impedance != null) {

                    appendLine()

                    appendLine(
                        "Equivalent Impedance"
                    )

                    appendLine(
                        "R = %.6f Ω".format(
                            impedance.resistanceOhm
                        )
                    )

                    appendLine(
                        "X = %.6f Ω".format(
                            impedance.reactanceOhm
                        )
                    )

                    appendLine(
                        "|Z| = %.6f Ω".format(
                            impedance.magnitudeOhm
                        )
                    )
                }

                appendLine()

                appendLine(
                    "ENGINEERING CHECKS"
                )

                appendLine()

                if (
                    calculation.checks.isEmpty()
                ) {

                    appendLine(
                        "No engineering checks returned."
                    )

                } else {

                    calculation.checks.forEach {
                        check ->

                        appendLine(
                            "• ${check.name}: " +
                                "${check.status}"
                        )

                        if (
                            check.calculatedValue !=
                            null
                        ) {

                            appendLine(
                                "  Calculated: " +
                                    "%.4f".format(
                                        check.calculatedValue
                                    ) +
                                    " " +
                                    check.unit
                            )
                        }

                        if (
                            check.requiredValue !=
                            null
                        ) {

                            appendLine(
                                "  Required: " +
                                    "%.4f".format(
                                        check.requiredValue
                                    ) +
                                    " " +
                                    check.unit
                            )
                        }

                        appendLine(
                            "  ${check.message}"
                        )
                    }
                }

                calculation.trace.standard?.let {
                    standard ->

                    appendLine()

                    appendLine(
                        "Standard: " +
                            standard.code
                    )
                }

                if (referenceMode) {

                    appendLine()

                    appendLine(
                        "REFERENCE DATA NOTICE"
                    )

                    appendLine(
                        "The transformer rating comes " +
                            "from the standard transformer " +
                            "rating table."
                    )

                    appendLine(
                        "The transformer %Z entered above " +
                            "must still come from the actual " +
                            "nameplate or verified technical data."
                    )
                }

                if (
                    calculation
                        .trace
                        .assumptions
                        .isNotEmpty()
                ) {

                    appendLine()

                    appendLine(
                        "ASSUMPTIONS"
                    )

                    calculation
                        .trace
                        .assumptions
                        .forEach {
                            appendLine(
                                "• $it"
                            )
                        }
                }

                if (
                    calculation
                        .trace
                        .warnings
                        .isNotEmpty()
                ) {

                    appendLine()

                    appendLine(
                        "WARNINGS"
                    )

                    calculation
                        .trace
                        .warnings
                        .forEach {
                            appendLine(
                                "• $it"
                            )
                        }
                }
            }

        if (
            calculation.status ==
            EngineeringStatus.PASS
        ) {

            ProjectManager.setShortCircuit(
                shortCircuitKA =
                    calculation.faultCurrentKA
            )
        }
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

        Text(
            text = "Short Circuit Current",
            style =
                MaterialTheme
                    .typography
                    .headlineSmall
        )

        Text(
            text =
                "Project: " +
                    project.projectName.ifBlank {
                        "Current Project"
                    }
        )

        HorizontalDivider()

        Text(
            text =
                "Required Transformer Capacity",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Text(
            text =
                if (requiredKVA > 0.0) {

                    "Project demand: " +
                        "%.2f kVA".format(
                            requiredKVA
                        )

                } else {

                    "Project transformer demand " +
                        "is not available."
                }
        )

        HorizontalDivider()

        Text(
            text =
                "Transformer Data Source",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            OutlinedButton(
                onClick = {
                    transformerSource =
                        TransformerSource
                            .STANDARD_RATING
                },

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    "Standard"
                )
            }

            OutlinedButton(
                onClick = {
                    transformerSource =
                        TransformerSource
                            .NAMEPLATE
                },

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    "Nameplate"
                )
            }

            OutlinedButton(
                onClick = {
                    transformerSource =
                        TransformerSource
                            .VERIFIED_CATALOG
                },

                enabled =
                    verifiedTransformers
                        .isNotEmpty(),

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    "Manufacturer"
                )
            }
        }

        Text(
            text =
                "Selected source: " +
                    sourceName(),
            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )

        HorizontalDivider()

        if (
            transformerSource ==
            TransformerSource.STANDARD_RATING
        ) {

            Text(
                text =
                    "Standard Transformer Rating",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            if (
                standardTransformers.isEmpty()
            ) {

                Text(
                    "No standard transformer ratings available."
                )

            } else {

                TransformerDropdown(
                    expanded =
                        standardMenuExpanded,

                    onExpandedChange = {
                        standardMenuExpanded =
                            !standardMenuExpanded
                    },

                    title =
                        selectedStandardTransformer
                            ?.let {
                                "%.0f kVA - %.0f V - %.0f Hz"
                                    .format(
                                        it.ratedPowerKVA,
                                        it.secondaryVoltageV,
                                        it.frequencyHz
                                    )
                            }
                            ?: "Select transformer"
                ) {

                    standardTransformers.forEach {
                        transformer ->

                        DropdownMenuItem(
                            text = {

                                Text(
                                    "%.0f kVA - %.0f V - %.0f Hz"
                                        .format(
                                            transformer
                                                .ratedPowerKVA,

                                            transformer
                                                .secondaryVoltageV,

                                            transformer
                                                .frequencyHz
                                        )
                                )
                            },

                            onClick = {

                                selectedStandardTransformer =
                                    transformer

                                standardMenuExpanded =
                                    false
                            }
                        )
                    }
                }

                selectedStandardTransformer?.let {
                    transformer ->

                    Text(
                        "Selected rating: " +
                            "%.0f kVA"
                                .format(
                                    transformer
                                        .ratedPowerKVA
                                )
                    )

                    Text(
                        "LV voltage: " +
                            "%.0f V"
                                .format(
                                    transformer
                                        .secondaryVoltageV
                                )
                    )

                    Text(
                        "Frequency: " +
                            "%.0f Hz"
                                .format(
                                    transformer
                                        .frequencyHz
                                )
                    )

                    Text(
                        "Transformer %Z must be entered " +
                            "from actual transformer data."
                    )
                }
            }
        }

        if (
            transformerSource ==
            TransformerSource.VERIFIED_CATALOG
        ) {

            Text(
                text =
                    "Verified Manufacturer Transformer",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            if (
                verifiedTransformers.isEmpty()
            ) {

                Text(
                    "No verified manufacturer transformer " +
                        "records are available."
                )

            } else {

                TransformerDropdown(
                    expanded =
                        catalogMenuExpanded,

                    onExpandedChange = {
                        catalogMenuExpanded =
                            !catalogMenuExpanded
                    },

                    title =
                        selectedCatalogTransformer
                            ?.let {
                                "${it.manufacturerName} - " +
                                    "${it.ratedPowerKVA.toInt()} kVA"
                            }
                            ?: "Select transformer"
                ) {

                    verifiedTransformers.forEach {
                        transformer ->

                        DropdownMenuItem(
                            text = {

                                Text(
                                    "${transformer.manufacturerName} - " +
                                        "${transformer.ratedPowerKVA.toInt()} kVA"
                                )
                            },

                            onClick = {

                                selectedCatalogTransformer =
                                    transformer

                                catalogMenuExpanded =
                                    false
                            }
                        )
                    }
                }

                selectedCatalogTransformer?.let {
                    transformer ->

                    Text(
                        "Rating: %.0f kVA"
                            .format(
                                transformer
                                    .ratedPowerKVA
                            )
                    )

                    Text(
                        "LV voltage: %.0f V"
                            .format(
                                transformer
                                    .secondaryVoltageV
                            )
                    )

                    Text(
                        "Frequency: %.0f Hz"
                            .format(
                                transformer
                                    .frequencyHz
                            )
                    )

                    Text(
                        "Transformer %Z: " +
                            (
                                transformer
                                    .impedancePercent
                                    ?.let {
                                        "%.2f %%".format(it)
                                    }
                                    ?: "DATA REQUIRED"
                            )
                    )

                    Button(
                        onClick = {

                            val z =
                                transformer
                                    .impedancePercent

                            if (
                                z == null ||
                                z <= 0.0
                            ) {

                                resultText =
                                    "DATA REQUIRED\n\n" +
                                        "The selected manufacturer " +
                                        "transformer has no verified " +
                                        "transformer %Z."

                                return@Button
                            }

                            calculate(
                                kva =
                                    transformer
                                        .ratedPowerKVA,

                                voltageV =
                                    transformer
                                        .secondaryVoltageV,

                                impedancePercent =
                                    z,

                                referenceMode =
                                    false
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "Calculate Short Circuit"
                        )
                    }
                }
            }
        }

        if (
            transformerSource ==
            TransformerSource.STANDARD_RATING
        ) {

            OutlinedTextField(
                value =
                    nameplateImpedance,

                onValueChange = {
                    nameplateImpedance = it
                },

                label = {
                    Text(
                        "Actual Transformer %Z"
                    )
                },

                supportingText = {
                    Text(
                        "Enter the %Z from transformer nameplate " +
                            "or verified technical data."
                    )
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine = true
            )

            Button(
                onClick = {

                    val transformer =
                        selectedStandardTransformer

                    val z =
                        parsePositive(
                            nameplateImpedance
                        )

                    if (
                        transformer == null ||
                        z == null
                    ) {

                        resultText =
                            "DATA REQUIRED\n\n" +
                                "Select transformer rating and " +
                                "enter actual transformer %Z."

                        return@Button
                    }

                    calculate(
                        kva =
                            transformer.ratedPowerKVA,

                        voltageV =
                            transformer.secondaryVoltageV,

                        impedancePercent =
                            z,

                        referenceMode =
                            true
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Calculate Short Circuit"
                )
            }
        }

        if (
            transformerSource ==
            TransformerSource.NAMEPLATE
        ) {

            OutlinedTextField(
                value =
                    nameplateKVA,

                onValueChange = {
                    nameplateKVA = it
                },

                label = {
                    Text(
                        "Transformer Rating (kVA)"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine = true
            )

            OutlinedTextField(
                value =
                    nameplateVoltage,

                onValueChange = {
                    nameplateVoltage = it
                },

                label = {
                    Text(
                        "LV Voltage (V)"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine = true
            )

            OutlinedTextField(
                value =
                    nameplateImpedance,

                onValueChange = {
                    nameplateImpedance = it
                },

                label = {
                    Text(
                        "Transformer %Z"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine = true
            )

            Button(
                onClick = {

                    val kva =
                        parsePositive(
                            nameplateKVA
                        )

                    val voltage =
                        parsePositive(
                            nameplateVoltage
                        )

                    val z =
                        parsePositive(
                            nameplateImpedance
                        )

                    if (
                        kva == null ||
                        voltage == null ||
                        z == null
                    ) {

                        resultText =
                            "DATA REQUIRED\n\n" +
                                "Enter transformer kVA, LV voltage " +
                                "and actual transformer %Z."

                        return@Button
                    }

                    calculate(
                        kva = kva,
                        voltageV = voltage,
                        impedancePercent = z,
                        referenceMode = false
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Calculate Short Circuit"
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        if (
            resultText.isNotBlank()
        ) {

            HorizontalDivider()

            Text(
                text = resultText,
                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        OutlinedButton(
            onClick = onBack,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Back"
            )
        }
    }
}

@Composable
private fun TransformerDropdown(
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    title: String,
    content: @Composable () -> Unit
) {

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        OutlinedButton(
            onClick =
                onExpandedChange,

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                title
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest =
                onExpandedChange
        ) {

            content()
        }
    }
}
