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
import kotlin.math.abs

private enum class TransformerSource {
    VERIFIED_CATALOG,
    EEHC_STANDARD,
    NAMEPLATE
}

@Composable
fun ShortCircuitScreen(
    onBack: () -> Unit = {}
) {

    val project = ProjectManager.calculation

    val verifiedTransformers =
        remember {
            EngineeringCatalogRepository
                .allTransformers()
                .filter { it.verified }
                .sortedBy { it.ratedPowerKVA }
        }

    val standardTransformers =
        remember {
            EngineeringCatalogRepository
                .allStandardTransformers()
                .sortedBy { it.ratingKVA }
        }

    val defaultSource =
        if (verifiedTransformers.isNotEmpty()) {
            TransformerSource.VERIFIED_CATALOG
        } else {
            TransformerSource.EEHC_STANDARD
        }

    var transformerSource by remember {
        mutableStateOf(defaultSource)
    }

    var selectedCatalogTransformer by remember {
        mutableStateOf(
            verifiedTransformers.firstOrNull {
                project.transformerKVA > 0.0 &&
                    abs(
                        it.ratedPowerKVA -
                            project.transformerKVA
                    ) < 0.001
            } ?: verifiedTransformers.firstOrNull()
        )
    }

    var catalogMenuExpanded by remember {
        mutableStateOf(false)
    }

    val requiredKVA =
        project.totalKVA
            .takeIf { it > 0.0 }
            ?: project.demandKW
                .takeIf { it > 0.0 }
                ?.div(
                    project.powerFactor
                        .coerceIn(0.01, 1.0)
                )
            ?: 0.0

    val recommendedStandard =
        remember(
            requiredKVA,
            standardTransformers
        ) {
            EngineeringCatalogRepository
                .recommendStandardTransformer(
                    requiredKVA = requiredKVA
                )
        }

    var selectedStandardTransformer by remember {
        mutableStateOf(recommendedStandard)
    }

    var standardMenuExpanded by remember {
        mutableStateOf(false)
    }

    var nameplateKVA by remember {
        mutableStateOf(
            if (project.transformerKVA > 0.0) {
                "%.0f".format(
                    project.transformerKVA
                )
            } else {
                ""
            }
        )
    }

    var nameplateVoltage by remember {
        mutableStateOf(
            if (project.voltageV > 0.0) {
                "%.0f".format(
                    project.voltageV
                )
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
                "%.2f".format(
                    project.transformerImpedancePercent
                )
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

    fun sourceName(
        source: TransformerSource
    ): String {
        return when (source) {

            TransformerSource.VERIFIED_CATALOG ->
                "Verified Manufacturer Catalog"

            TransformerSource.EEHC_STANDARD ->
                "EEHC Standard Rating"

            TransformerSource.NAMEPLATE ->
                "Existing Transformer / Nameplate"
        }
    }

    fun calculateShortCircuit(
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
                "DATA REQUIRED\n\n" +
                    "Transformer rating, LV voltage and " +
                    "transformer impedance must be valid " +
                    "positive engineering values."

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
                        transformerKVA =
                            kva,

                        voltageV =
                            voltageV,

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

        val impedance =
            calculation.equivalentImpedance

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
                    "Transformer Source: " +
                        sourceName(
                            transformerSource
                        )
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
                        "Equivalent Impedance:"
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

                appendLine(
                    checksText
                )

                appendLine()

                appendLine(
                    "Standard: " +
                        (
                            calculation
                                .trace
                                .standard
                                ?.code
                                ?: "Not specified"
                        )
                )

                if (referenceMode) {

                    appendLine()

                    appendLine(
                        "REFERENCE DATA NOTICE"
                    )

                    appendLine(
                        "The transformer rating and %Z " +
                            "are based on the EEHC " +
                            "standard/reference transformer table."
                    )

                    appendLine(
                        "This is suitable for preliminary " +
                            "short-circuit assessment only."
                    )

                    appendLine(
                        "Final design must use the actual " +
                            "transformer manufacturer nameplate %Z."
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
                "Project: ${
                    project.projectName.ifBlank {
                        "Current Project"
                    }
                }",
            style =
                MaterialTheme
                    .typography
                    .bodyMedium
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
                    "Project demand: %.2f kVA"
                        .format(requiredKVA)
                } else {
                    "Project demand is not available. " +
                        "Select transformer manually."
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
                            .VERIFIED_CATALOG
                },
                modifier =
                    Modifier.weight(1f),
                enabled =
                    verifiedTransformers.isNotEmpty()
            ) {

                Text(
                    "Manufacturer"
                )
            }

            OutlinedButton(
                onClick = {
                    transformerSource =
                        TransformerSource
                            .EEHC_STANDARD
                },
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    "EEHC Standard"
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
        }

        Text(
            text =
                "Selected: ${
                    sourceName(
                        transformerSource
                    )
                }",
            style =
                MaterialTheme
                    .typography
                    .bodySmall
        )

        // ========================================================
        // VERIFIED MANUFACTURER CATALOG
        // ========================================================

        if (
            transformerSource ==
            TransformerSource.VERIFIED_CATALOG
        ) {

            HorizontalDivider()

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
                    text =
                        "No verified manufacturer transformer " +
                            "records are currently available."
                )

                Text(
                    text =
                        "Use EEHC Standard Rating or " +
                            "Existing Transformer / Nameplate."
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
                                "${it.ratedPowerKVA.toInt()} kVA - " +
                                    "${it.manufacturerName} - " +
                                    "${it.productFamily}"
                            }
                            ?: "Select transformer"
                ) {

                    verifiedTransformers.forEach {
                        transformer ->

                        DropdownMenuItem(
                            text = {

                                Text(
                                    "${transformer.ratedPowerKVA.toInt()} kVA - " +
                                        "${transformer.manufacturerName} - " +
                                        "${transformer.productFamily}" +
                                        (
                                            transformer.partNumber
                                                ?.let {
                                                    " - $it"
                                                }
                                                ?: ""
                                            )
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
                                transformer.ratedPowerKVA
                            )
                    )

                    Text(
                        "LV Voltage: %.0f V"
                            .format(
                                transformer.secondaryVoltageV
                            )
                    )

                    Text(
                        "Frequency: %.0f Hz"
                            .format(
                                transformer.frequencyHz
                            )
                    )

                    Text(
                        "Impedance: ${
                            transformer.impedancePercent
                                ?.let {
                                    "%.2f %%".format(it)
                                }
                                ?: "DATA REQUIRED"
                        }"
                    )

                    Text(
                        text =
                            "Source: ${
                                transformer.sourceUrl
                                    ?: transformer.catalogName
                            }",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        }

        // ========================================================
        // EEHC STANDARD
        // ========================================================

        if (
            transformerSource ==
            TransformerSource.EEHC_STANDARD
        ) {

            HorizontalDivider()

            Text(
                text =
                    "EEHC Standard Transformer Rating",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            if (
                recommendedStandard != null
            ) {

                Text(
                    "Recommended rating: %.0f kVA"
                        .format(
                            recommendedStandard.ratingKVA
                        )
                )

                Text(
                    "Reference %Z: %.2f %%"
                        .format(
                            recommendedStandard
                                .referenceImpedancePercent
                        )
                )
            }

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
                            "${it.ratingKVA.toInt()} kVA - " +
                                "Reference %Z " +
                                "%.1f %%"
                                    .format(
                                        it.referenceImpedancePercent
                                    )
                        }
                        ?: "Select standard rating"
            ) {

                standardTransformers.forEach {
                    transformer ->

                    DropdownMenuItem(
                        text = {

                            Text(
                                "${transformer.ratingKVA.toInt()} kVA - " +
                                    "%Z " +
                                    "%.1f %%"
                                        .format(
                                            transformer
                                                .referenceImpedancePercent
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

            Text(
                text =
                    "Standard: EEHC EDMS-08-100-4",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            Text(
                text =
                    "Reference values are not manufacturer " +
                        "nameplate data.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )

            Text(
                text =
                    "Final short-circuit design must use " +
                        "the actual transformer %Z.",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }

        // ========================================================
        // NAMEPLATE
        // ========================================================

        if (
            transformerSource ==
            TransformerSource.NAMEPLATE
        ) {

            HorizontalDivider()

            Text(
                text =
                    "Existing Transformer / Nameplate",
                style =
                    MaterialTheme
                        .typography
                        .titleMedium
            )

            OutlinedTextField(
                value = nameplateKVA,
                onValueChange = {
                    nameplateKVA = it
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
                value = nameplateVoltage,
                onValueChange = {
                    nameplateVoltage = it
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
                value = nameplateImpedance,
                onValueChange = {
                    nameplateImpedance = it
                },
                label = {
                    Text(
                        "Actual Transformer Impedance (%Z)"
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Text(
                text =
                    "All three values must come from the actual " +
                        "transformer nameplate or verified " +
                        "manufacturer technical data."
            )
        }

        // ========================================================
        // CALCULATE
        // ========================================================

        Button(
            onClick = {

                when (transformerSource) {

                    TransformerSource.VERIFIED_CATALOG -> {

                        val transformer =
                            selectedCatalogTransformer

                        if (transformer == null) {

                            resultText =
                                "DATA REQUIRED\n\n" +
                                    "Select a verified manufacturer " +
                                    "transformer before calculation."

                            ratedCurrentA = 0.0

                        } else {

                            val z =
                                transformer.impedancePercent

                            if (
                                transformer.ratedPowerKVA <= 0.0 ||
                                transformer.secondaryVoltageV <= 0.0 ||
                                z == null ||
                                z <= 0.0
                            ) {

                                resultText =
                                    "DATA REQUIRED\n\n" +
                                        "The selected manufacturer " +
                                        "transformer does not have " +
                                        "complete verified impedance data."

                                ratedCurrentA = 0.0

                            } else {

                                calculateShortCircuit(
                                    kva =
                                        transformer.ratedPowerKVA,

                                    voltageV =
                                        transformer.secondaryVoltageV,

                                    impedancePercent =
                                        z,

                                    referenceMode =
                                        false
                                )
                            }
                        }
                    }

                    TransformerSource.EEHC_STANDARD -> {

                        val transformer =
                            selectedStandardTransformer

                        if (transformer == null) {

                            resultText =
                                "DATA REQUIRED\n\n" +
                                    "Select an EEHC standard " +
                                    "transformer rating."

                            ratedCurrentA = 0.0

                        } else {

                            calculateShortCircuit(
                                kva =
                                    transformer.ratingKVA,

                                voltageV =
                                    transformer.secondaryVoltageV,

                                impedancePercent =
                                    transformer.referenceImpedancePercent,

                                referenceMode =
                                    true
                            )
                        }
                    }

                    TransformerSource.NAMEPLATE -> {

                        val kva =
                            nameplateKVA
                                .toDoubleOrNull()
                                ?.takeIf {
                                    it > 0.0
                                }

                        val voltageV =
                            nameplateVoltage
                                .toDoubleOrNull()
                                ?.takeIf {
                                    it > 0.0
                                }

                        val z =
                            nameplateImpedance
                                .toDoubleOrNull()
                                ?.takeIf {
                                    it > 0.0
                                }

                        if (
                            kva == null ||
                            voltageV == null ||
                            z == null
                        ) {

                            resultText =
                                "DATA REQUIRED\n\n" +
                                    "Please provide:\n\n" +
                                    "• Transformer rating\n" +
                                    "• LV voltage\n" +
                                    "• Actual transformer %Z\n\n" +
                                    "No engineering value was assumed."

                            ratedCurrentA = 0.0

                        } else {

                            calculateShortCircuit(
                                kva = kva,
                                voltageV = voltageV,
                                impedancePercent = z,
                                referenceMode = false
                            )
                        }
                    }
                }
            },

            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Calculate & Save Short Circuit"
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
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }

        // ========================================================
        // PROJECT RESULT
        // ========================================================

        HorizontalDivider()

        Text(
            text =
                "Current Project Result",
            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Text(
            "Transformer: %.0f kVA"
                .format(
                    ProjectManager
                        .calculation
                        .transformerKVA
                )
        )

        Text(
            "Transformer %Z: ${
                if (
                    ProjectManager
                        .calculation
                        .transformerImpedancePercent >
                    0.0
                ) {

                    "%.2f %%".format(
                        ProjectManager
                            .calculation
                            .transformerImpedancePercent
                    )

                } else {

                    "N/A - Actual Data Required"
                }
            }"
        )

        Text(
            "Voltage: %.0f V"
                .format(
                    ProjectManager
                        .calculation
                        .voltageV
                )
        )

        Text(
            "Transformer Rated Current: %.2f A"
                .format(
                    ratedCurrentA
                )
        )

        Text(
            "Short Circuit: %.3f kA"
                .format(
                    ProjectManager
                        .calculation
                        .shortCircuitKA
                )
        )

        Text(
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

        Button(
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
            onClick = onExpandedChange,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = title
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onExpandedChange
        ) {

            content()
        }
    }
}
