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

/*
 * ============================================================
 * ENGINEERING DATA
 * ============================================================
 *
 * Transformer catalog data comes exclusively from
 * EngineeringCatalogRepository.
 *
 * Engineering calculations come exclusively from
 * ProfessionalEngineeringCore.
 *
 * This screen must NOT contain independent engineering
 * calculation formulas.
 */

val standardTransformers =
    remember {
        EngineeringCatalogRepository
            .allStandardTransformers()
            .filter {
                it.ratedPowerKVA > 0.0
            }
            .sortedBy {
                it.ratedPowerKVA
            }
    }

val verifiedTransformers =
    remember {
        EngineeringCatalogRepository
            .allTransformers()
            .filter {
                it.verified
            }
            .filter {
                it.ratedPowerKVA > 0.0
            }
            .sortedBy {
                it.ratedPowerKVA
            }
    }

/*
 * Required transformer capacity.
 *
 * This is only a UI reference value.
 * The actual engineering calculations remain in
 * ProfessionalEngineeringCore.
 *
 * No new calculation engine is introduced here.
 */

val requiredKVA =
    when {

        project.totalKVA > 0.0 ->
            project.totalKVA

        project.demandKW > 0.0 &&
                project.powerFactor > 0.0 ->
            project.demandKW /
                    project.powerFactor
                        .coerceIn(
                            0.01,
                            1.0
                        )

        else ->
            0.0
    }

val recommendedStandard =
    if (
        requiredKVA > 0.0
    ) {

        EngineeringCatalogRepository
            .recommendedStandardTransformer(
                minimumKVA = requiredKVA,
                secondaryVoltageV = project.voltageV,
                frequencyHz = project.frequencyHz
            )

    } else {

        standardTransformers.firstOrNull()
    }

var transformerSource by remember {
    mutableStateOf(
        TransformerSource.STANDARD_RATING
    )
}

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
        if (
            project.transformerKVA > 0.0
        ) {
            project.transformerKVA.toString()
        } else {
            ""
        }
    )
}

/*
 * IMPORTANT:
 *
 * Do not silently assume 400 V.
 *
 * If the project voltage is unavailable, the engineer must
 * enter the actual transformer LV voltage.
 */

var nameplateVoltage by remember {
    mutableStateOf(
        if (
            project.voltageV > 0.0
        ) {
            project.voltageV.toString()
        } else {
            ""
        }
    )
}

var nameplateImpedance by remember {
    mutableStateOf(
        if (
            project.transformerImpedancePercent > 0.0
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

/*
 * ============================================================
 * INPUT PARSER
 * ============================================================
 */

fun parsePositive(
    value: String
): Double? {

    return value
        .replace(",", ".")
        .trim()
        .toDoubleOrNull()
        ?.takeIf {
            it > 0.0
        }
}

/*
 * ============================================================
 * SOURCE DESCRIPTION
 * ============================================================
 */

fun sourceName(): String {

    return when (
        transformerSource
    ) {

        TransformerSource.STANDARD_RATING ->
            "Standard Transformer Rating"

        TransformerSource.NAMEPLATE ->
            "Existing Transformer / Nameplate"

        TransformerSource.VERIFIED_CATALOG ->
            "Verified Manufacturer Catalog"
    }
}

/*
 * ============================================================
 * SHORT-CIRCUIT CALCULATION
 * ============================================================
 *
 * NO short-circuit formula is implemented here.
 *
 * All engineering calculation is delegated to
 * ProfessionalEngineeringCore.
 */

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
            """
            DATA REQUIRED

            Transformer rating, LV voltage and transformer %Z
            must all be positive engineering values.

            Transformer %Z must come from:
            • Transformer nameplate
            • Manufacturer technical documentation
            • Verified project documentation

            The software does not invent transformer impedance.
            """.trimIndent()

        return
    }

    /*
     * Update project engineering state.
     */

    ProjectManager.updateSystem(
        voltageV = voltageV
    )

    ProjectManager.setTransformer(
        transformerKVA = kva,
        transformerImpedancePercent =
            impedancePercent
    )

    /*
     * Transformer rated current is calculated by the
     * ProfessionalEngineeringCore.
     */

    ratedCurrentA =
        ProfessionalEngineeringCore
            .threePhaseCurrent(
                kva = kva,
                voltageV = voltageV
            )

    /*
     * Create the standard engineering input model.
     */

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

    /*
     * SINGLE ENGINEERING CORE
     *
     * This is the only place where the actual
     * short-circuit calculation is performed.
     */

    val calculation =
        ProfessionalEngineeringCore
            .calculateShortCircuit(
                input = input
            )

    val statusText =
        when (
            calculation.status
        ) {

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

    /*
     * ========================================================
     * RESULT REPORT
     * ========================================================
     */

    resultText =
        buildString {

            appendLine(
                "SHORT CIRCUIT CALCULATION"
            )

            appendLine()

            /*
             * FIXED KOTLIN STRING INTERPOLATION
             *
             * Wrong:
             * "Source: $sourceName()"
             *
             * Correct:
             * "Source: ${sourceName()}"
             */

            appendLine(
                "Source: ${sourceName()}"
            )

            appendLine()

            appendLine(
                "Status: $statusText"
            )

            appendLine()

            appendLine(
                "Transformer Rating: " +
                        "%.0f kVA"
                            .format(
                                kva
                            )
            )

            appendLine(
                "LV Voltage: " +
                        "%.0f V"
                            .format(
                                voltageV
                            )
            )

            appendLine(
                "Transformer %Z: " +
                        "%.2f %%"
                            .format(
                                impedancePercent
                            )
            )

            appendLine()

            appendLine(
                "Transformer Rated Current: " +
                        "%.2f A"
                            .format(
                                ratedCurrentA
                            )
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

            /*
             * Equivalent impedance returned by the core.
             */

            calculation
                .equivalentImpedance
                ?.let { impedance ->

                    appendLine()

                    appendLine(
                        "Equivalent Impedance"
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

            /*
             * Engineering checks returned by the core.
             */

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

                calculation
                    .checks
                    .forEach { check ->

                        appendLine(
                            "• ${check.name}: " +
                                    "${check.status}"
                        )

                        check.calculatedValue
                            ?.let { value ->

                                appendLine(
                                    "  Calculated: " +
                                            "%.4f"
                                                .format(
                                                    value
                                                ) +
                                            " " +
                                            check.unit
                                )
                            }

                        check.requiredValue
                            ?.let { value ->

                                appendLine(
                                    "  Required: " +
                                            "%.4f"
                                                .format(
                                                    value
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

            /*
             * Standard trace.
             */

            calculation
                .trace
                .standard
                ?.let { standard ->

                    appendLine()

                    appendLine(
                        "Standard: " +
                                standard.code
                    )
                }

            /*
             * Reference-data notice.
             */

            if (
                referenceMode
            ) {

                appendLine()

                appendLine(
                    "REFERENCE DATA NOTICE"
                )

                appendLine(
                    "Transformer rating is taken from " +
                            "the standard transformer rating table."
                )

                appendLine(
                    "Transformer %Z is NOT assumed by the software."
                )

                appendLine(
                    "The entered %Z must be verified against " +
                            "the actual transformer data."
                )
            }

            /*
             * Assumptions returned by the core.
             */

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
                    .forEach { assumption ->

                        appendLine(
                            "• $assumption"
                        )
                    }
            }

            /*
             * Warnings returned by the core.
             */

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
                    .forEach { warning ->

                        appendLine(
                            "• $warning"
                        )
                    }
            }
        }

    /*
     * Save the result only when the core confirms PASS.
     *
     * No duplicate calculation is performed.
     */

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

/*
 * ============================================================
 * USER INTERFACE
 * ============================================================
 */

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

    /*
     * Header
     */

    Row(
        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        OutlinedButton(
            onClick = onBack
        ) {

            Text(
                "Back"
            )
        }

        Text(
            text =
                "Short Circuit Current",

            style =
                MaterialTheme
                    .typography
                    .headlineSmall
        )
    }

    Text(
        text =
            "Project: " +
                    project.projectName
                        .ifBlank {
                            "Current Project"
                        }
    )

    HorizontalDivider()

    /*
     * Required transformer capacity
     */

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
            if (
                requiredKVA > 0.0
            ) {

                "Project demand: " +
                        "%.2f kVA"
                            .format(
                                requiredKVA
                            )

            } else {

                "Project transformer demand " +
                        "is not available."
            }
    )

    HorizontalDivider()

    /*
     * Transformer source
     */

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
            Arrangement.spacedBy(6.dp)
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

    /*
     * ========================================================
     * STANDARD TRANSFORMER
     * ========================================================
     */

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
                "No standard transformer ratings are available."
            )

        } else {

            TransformerDropdown(
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
                        ?: "Select transformer",

                expanded =
                    standardMenuExpanded,

                onExpandedChange = {

                    standardMenuExpanded =
                        !standardMenuExpanded
                }
            ) {

                standardTransformers
                    .forEach { transformer ->

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

            selectedStandardTransformer
                ?.let { transformer ->

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
                        "Transformer %Z is required " +
                                "from actual transformer data."
                    )
                }

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            OutlinedTextField(
                value =
                    nameplateImpedance,

                onValueChange = {
                    nameplateImpedance =
                        it
                },

                label = {
                    Text(
                        "Transformer %Z"
                    )
                },

                placeholder = {
                    Text(
                        "Example: 6.0"
                    )
                },

                singleLine = true,

                modifier =
                    Modifier.fillMaxWidth()
            )

            Text(
                text =
                    "Do not enter an assumed %Z. " +
                            "Use the actual transformer nameplate or verified technical data.",

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
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
                        transformer == null
                    ) {

                        resultText =
                            "DATA REQUIRED\n\n" +
                                    "Select a standard transformer rating."

                    } else if (
                        z == null
                    ) {

                        resultText =
                            "DATA REQUIRED\n\n" +
                                    "Enter the actual transformer %Z."

                    } else {

                        calculateShortCircuit(

                            kva =
                                transformer
                                    .ratedPowerKVA,

                            voltageV =
                                transformer
                                    .secondaryVoltageV,

                            impedancePercent =
                                z,

                            referenceMode =
                                true
                        )
                    }
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

    /*
     * ========================================================
     * NAMEPLATE
     * ========================================================
     */

    if (
        transformerSource ==
                TransformerSource.NAMEPLATE
    ) {

        Text(
            text =
                "Existing Transformer / Nameplate",

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        OutlinedTextField(
            value =
                nameplateKVA,

            onValueChange = {
                nameplateKVA =
                    it
            },

            label = {
                Text(
                    "Transformer Rating (kVA)"
                )
            },

            singleLine = true,

            modifier =
                Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value =
                nameplateVoltage,

            onValueChange = {
                nameplateVoltage =
                    it
            },

            label = {
                Text(
                    "LV Voltage (V)"
                )
            },

            singleLine = true,

            modifier =
                Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value =
                nameplateImpedance,

            onValueChange = {
                nameplateImpedance =
                    it
            },

            label = {
                Text(
                    "Transformer %Z"
                )
            },

            placeholder = {
                Text(
                    "Example: 6.0"
                )
            },

            singleLine = true,

            modifier =
                Modifier.fillMaxWidth()
        )

        Text(
            text =
                "All three values must come from the actual transformer data.",

            style =
                MaterialTheme
                    .typography
                    .bodySmall
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

                when {

                    kva == null -> {

                        resultText =
                            "DATA REQUIRED\n\n" +
                                    "Enter transformer rating."
                    }

                    voltage == null -> {

                        resultText =
                            "DATA REQUIRED\n\n" +
                                    "Enter LV voltage."
                    }

                    z == null -> {

                        resultText =
                            "DATA REQUIRED\n\n" +
                                    "Enter actual transformer %Z."
                    }

                    else -> {

                        calculateShortCircuit(

                            kva =
                                kva,

                            voltageV =
                                voltage,

                            impedancePercent =
                                z,

                            referenceMode =
                                false
                        )
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
    }

    /*
     * ========================================================
     * VERIFIED MANUFACTURER
     * ========================================================
     */

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
                "No verified manufacturer transformer records are available."
            )

        } else {

            TransformerDropdown(

                title =
                    selectedCatalogTransformer
                        ?.let { transformer ->

                            buildString {

                                append(
                                    "%.0f kVA"
                                        .format(
                                            transformer
                                                .ratedPowerKVA
                                        )
                                )

                                append(
                                    " - "
                                )

                                append(
                                    "%.0f V"
                                        .format(
                                            transformer
                                                .secondaryVoltageV
                                        )
                                )

                                append(
                                    " - "
                                )

                                append(
                                    transformer
                                        .manufacturerName
                                )
                            }
                        }
                        ?: "Select transformer",

                expanded =
                    catalogMenuExpanded,

                onExpandedChange = {

                    catalogMenuExpanded =
                        !catalogMenuExpanded
                }
            ) {

                verifiedTransformers
                    .forEach { transformer ->

                        DropdownMenuItem(
                            text = {

                                Text(
                                    buildString {

                                        append(
                                            "%.0f kVA"
                                                .format(
                                                    transformer
                                                        .ratedPowerKVA
                                                )
                                        )

                                        append(
                                            " - "
                                        )

                                        append(
                                            "%.0f V"
                                                .format(
                                                    transformer
                                                        .secondaryVoltageV
                                                )
                                        )

                                        append(
                                            " - "
                                        )

                                        append(
                                            transformer
                                                .manufacturerName
                                        )
                                    }
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

            selectedCatalogTransformer
                ?.let { transformer ->

                    Text(
                        "Manufacturer: " +
                                transformer
                                    .manufacturerName
                    )

                    Text(
                        "Product family: " +
                                transformer
                                    .productFamily
                    )

                    Text(
                        "Rating: " +
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
                        "Transformer %Z: " +
                                (
                                    transformer
                                        .impedancePercent
                                        ?.let {
                                            "%.2f %%"
                                                .format(
                                                    it
                                                )
                                        }
                                        ?: "NOT AVAILABLE"
                                )
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Button(
                        onClick = {

                            val z =
                                transformer
                                    .impedancePercent

                            if (
                                z == null
                            ) {

                                resultText =
                                    "DATA REQUIRED\n\n" +
                                            "The selected manufacturer transformer " +
                                            "does not contain verified impedance (%Z) data.\n\n" +
                                            "The software will not invent transformer impedance."

                            } else {

                                calculateShortCircuit(

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
                            }
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

    /*
     * ========================================================
     * RESULT
     * ========================================================
     */

    if (
        resultText.isNotBlank()
    ) {

        HorizontalDivider()

        Text(
            text =
                "RESULT",

            style =
                MaterialTheme
                    .typography
                    .titleMedium
        )

        Text(
            text =
                resultText
        )
    }
}

}

/*

* ================================================================
* TRANSFORMER DROPDOWN
* ================================================================
  */

@Composable
private fun TransformerDropdown(
title: String,
expanded: Boolean,
onExpandedChange: () -> Unit,
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
            text =
                title
        )
    }

    DropdownMenu(
        expanded =
            expanded,

        onDismissRequest =
            onExpandedChange
    ) {

        content()
    }
}

}
