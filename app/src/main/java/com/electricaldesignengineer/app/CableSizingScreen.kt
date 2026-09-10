package com.electricaldesignengineer.app

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
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

@Composable
fun CableSizingScreen(
onBack: () -> Unit = {}
) {

val project = ProjectManager.calculation

var designCurrent by remember {
    mutableStateOf(
        if (project.designCurrentA > 0.0) {
            "%.2f".format(project.designCurrentA)
        } else {
            ""
        }
    )
}

var length by remember {
    mutableStateOf(
        if (project.cableLengthM > 0.0) {
            "%.2f".format(project.cableLengthM)
        } else {
            ""
        }
    )
}

var voltage by remember {
    mutableStateOf(
        if (project.voltageV > 0.0) {
            "%.2f".format(project.voltageV)
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
            ""
        }
    )
}

var maximumVoltageDrop by remember {
    mutableStateOf("5.0")
}

var isThreePhase by remember {
    mutableStateOf(project.isThreePhase)
}

var material by remember {
    mutableStateOf(CableMaterial.COPPER)
}

var insulation by remember {
    mutableStateOf(InsulationType.XLPE)
}

var installationMethod by remember {
    mutableStateOf(InstallationMethod.CABLE_TRAY)
}

var engineerOverride by remember {
    mutableStateOf(false)
}

var selectedManualSize by remember {
    mutableStateOf<Double?>(null)
}

var resultText by remember {
    mutableStateOf("")
}

var errorText by remember {
    mutableStateOf("")
}

val availableCables = remember(
    material,
    insulation,
    installationMethod
) {

    EngineeringCatalogRepository
        .getCableData(
            material = material,
            insulation = insulation,
            installationMethod = installationMethod
        )
        .filter {
            it.sizeMm2 > 0.0 &&
                it.baseAmpacityA > 0.0 &&
                it.resistanceOhmPerKm > 0.0 &&
                it.source.isNotBlank() &&
                it.revision.isNotBlank()
        }
        .sortedBy {
            it.sizeMm2
        }
}

fun calculateCable() {

    resultText = ""
    errorText = ""

    val current =
        designCurrent
            .replace(',', '.')
            .toDoubleOrNull()

    val cableLength =
        length
            .replace(',', '.')
            .toDoubleOrNull()

    val systemVoltage =
        voltage
            .replace(',', '.')
            .toDoubleOrNull()

    val pf =
        powerFactor
            .replace(',', '.')
            .toDoubleOrNull()

    val maxDrop =
        maximumVoltageDrop
            .replace(',', '.')
            .toDoubleOrNull()

    if (current == null || current <= 0.0) {
        errorText =
            "Design current must be greater than zero."
        return
    }

    if (cableLength == null || cableLength <= 0.0) {
        errorText =
            "Cable length must be greater than zero."
        return
    }

    if (systemVoltage == null || systemVoltage <= 0.0) {
        errorText =
            "System voltage must be greater than zero."
        return
    }

    if (pf == null || pf !in 0.01..1.0) {
        errorText =
            "Power factor must be between 0.01 and 1.00."
        return
    }

    if (maxDrop == null || maxDrop <= 0.0) {
        errorText =
            "Maximum voltage drop must be greater than zero."
        return
    }

    if (availableCables.isEmpty()) {
        errorText =
            "No verified cable data is available for the selected configuration."
        return
    }

    if (
        engineerOverride &&
        selectedManualSize == null
    ) {
        errorText =
            "Select a verified cable size for Engineer Override."
        return
    }

    ProjectManager.updateSystem(
        voltageV = systemVoltage,
        powerFactor = pf,
        isThreePhase = isThreePhase
    )

    val phaseSystem =
        if (isThreePhase) {
            ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE
        } else {
            ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
        }

    val provider =
        object :
            ProfessionalEngineeringCore.CableDataProvider {

            override fun availableCables(
                material: CableMaterial,
                insulation: InsulationType,
                installationMethod: InstallationMethod
            ):
                List<ProfessionalEngineeringCore.CableData> {

                val catalog =
                    EngineeringCatalogRepository
                        .getCableData(
                            material = material,
                            insulation = insulation,
                            installationMethod =
                                installationMethod
                        )
                        .filter {
                            it.sizeMm2 > 0.0 &&
                                it.baseAmpacityA > 0.0 &&
                                it.resistanceOhmPerKm > 0.0 &&
                                it.source.isNotBlank() &&
                                it.revision.isNotBlank()
                        }
                        .sortedBy {
                            it.sizeMm2
                        }

                if (!engineerOverride) {
                    return catalog
                }

                val manualSize =
                    selectedManualSize
                        ?: return emptyList()

                return catalog.filter {
                    kotlin.math.abs(
                        it.sizeMm2 - manualSize
                    ) < 0.000001
                }
            }
        }

    val input =
        ProfessionalEngineeringCore.CableDesignInput(

            designCurrentA =
                current,

            lengthM =
                cableLength,

            voltageV =
                systemVoltage,

            powerFactor =
                pf,

            phaseSystem =
                phaseSystem,

            conductorMaterial =
                material,

            insulation =
                insulation,

            installationMethod =
                installationMethod,

            numberOfLoadedConductors =
                if (isThreePhase) 3 else 2,

            ambientTemperatureC =
                30.0,

            groupingFactor =
                1.0,

            thermalInsulationFactor =
                1.0,

            soilCorrectionFactor =
                1.0,

            maximumVoltageDropPercent =
                maxDrop,

            maximumParallelRuns =
                8
        )

    val calculation =
        ProfessionalEngineeringCore.designCable(
            input = input,
            provider = provider
        )

    val selected =
        calculation.selectedCable

    if (selected == null) {

        errorText =
            when (calculation.status) {

                EngineeringStatus.DATA_REQUIRED ->
                    "DATA REQUIRED: verified cable engineering data is incomplete."

                EngineeringStatus.FAIL ->
                    "No cable configuration satisfies the design requirements."

                EngineeringStatus.WARNING ->
                    "WARNING: the calculation did not produce a valid cable."

                else ->
                    "No suitable cable was found."
            }

        return
    }

    ProjectManager.setCableResult(
        cableSizeMm2 =
            selected.sizeMm2,

        cableAmpacityA =
            calculation.totalAmpacityA,

        cableLengthM =
            cableLength,

        voltageDropV =
            calculation.voltageDropV,

        voltageDropPercent =
            calculation.voltageDropPercent
    )

    resultText =
        buildString {

            appendLine(
                if (engineerOverride) {
                    "ENGINEER OVERRIDE — VALIDATED"
                } else {
                    "AUTOMATIC CABLE SELECTION"
                }
            )

            appendLine()

            appendLine(
                "Cable Size : %.1f mm²"
                    .format(selected.sizeMm2)
            )

            appendLine(
                "Parallel Runs : %d"
                    .format(calculation.parallelRuns)
            )

            appendLine(
                "Ampacity / Run : %.2f A"
                    .format(
                        calculation.correctedAmpacityPerRunA
                    )
            )

            appendLine(
                "Total Ampacity Iz : %.2f A"
                    .format(
                        calculation.totalAmpacityA
                    )
            )

            appendLine()

            appendLine(
                "Design Current Ib : %.2f A"
                    .format(current)
            )

            appendLine(
                "Voltage Drop : %.2f V"
                    .format(
                        calculation.voltageDropV
                    )
            )

            appendLine(
                "Voltage Drop : %.2f %%"
                    .format(
                        calculation.voltageDropPercent
                    )
            )

            appendLine()

            appendLine(
                "Material : ${material.name}"
            )

            appendLine(
                "Insulation : ${insulation.name}"
            )

            appendLine(
                "Installation : ${installationMethod.name}"
            )

            appendLine()

            appendLine(
                "Status : ${calculation.status}"
            )

            appendLine()

            appendLine(
                "Data Source : ${selected.source}"
            )

            appendLine(
                "Data Revision : ${selected.revision}"
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
        text = "Cable Sizing",
        style =
            MaterialTheme.typography.headlineSmall
    )

    Text(
        text = "Professional Cable Selection",
        style =
            MaterialTheme.typography.bodyMedium
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

    Text(
        text = "Design Inputs",
        style =
            MaterialTheme.typography.titleMedium
    )

    OutlinedTextField(
        value = designCurrent,
        onValueChange = {
            designCurrent = it
        },
        label = {
            Text("Design Current Ib (A)")
        },
        singleLine = true,
        modifier =
            Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = length,
        onValueChange = {
            length = it
        },
        label = {
            Text("Cable Length (m)")
        },
        singleLine = true,
        modifier =
            Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = voltage,
        onValueChange = {
            voltage = it
        },
        label = {
            Text("System Voltage (V)")
        },
        singleLine = true,
        modifier =
            Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = powerFactor,
        onValueChange = {
            powerFactor = it
        },
        label = {
            Text("Power Factor")
        },
        singleLine = true,
        modifier =
            Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = maximumVoltageDrop,
        onValueChange = {
            maximumVoltageDrop = it
        },
        label = {
            Text("Maximum Voltage Drop (%)")
        },
        singleLine = true,
        modifier =
            Modifier.fillMaxWidth()
    )

    Text(
        text = "Phase System",
        style =
            MaterialTheme.typography.titleSmall
    )

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        Button(
            onClick = {
                isThreePhase = true
            },
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                if (isThreePhase) {
                    "✓ 3 Phase"
                } else {
                    "3 Phase"
                }
            )
        }

        OutlinedButton(
            onClick = {
                isThreePhase = false
            },
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                if (!isThreePhase) {
                    "✓ 1 Phase"
                } else {
                    "1 Phase"
                }
            )
        }
    }

    HorizontalDivider()

    Text(
        text = "Cable Data",
        style =
            MaterialTheme.typography.titleMedium
    )

    EngineeringDropdown(
        label = "Conductor Material",
        value = material.name,
        options =
            CableMaterial
                .values()
                .map {
                    it.name
                },
        onSelected = { value ->

            material =
                CableMaterial.valueOf(value)

            selectedManualSize = null
            resultText = ""
            errorText = ""
        }
    )

    EngineeringDropdown(
        label = "Insulation",
        value = insulation.name,
        options =
            InsulationType
                .values()
                .map {
                    it.name
                },
        onSelected = { value ->

            insulation =
                InsulationType.valueOf(value)

            selectedManualSize = null
            resultText = ""
            errorText = ""
        }
    )

    EngineeringDropdown(
        label = "Installation Method",
        value =
            installationMethod.name,
        options =
            InstallationMethod
                .values()
                .map {
                    it.name
                },
        onSelected = { value ->

            installationMethod =
                InstallationMethod.valueOf(value)

            selectedManualSize = null
            resultText = ""
            errorText = ""
        }
    )

    Text(
        text =
            "Verified catalog cables available: ${availableCables.size}",
        style =
            MaterialTheme.typography.bodySmall
    )

    HorizontalDivider()

    Text(
        text = "Cable Selection Mode",
        style =
            MaterialTheme.typography.titleMedium
    )

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        Button(
            onClick = {

                engineerOverride = false
                selectedManualSize = null
                resultText = ""
                errorText = ""

            },
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                if (!engineerOverride) {
                    "✓ Automatic"
                } else {
                    "Automatic"
                }
            )
        }

        OutlinedButton(
            onClick = {

                engineerOverride = true
                resultText = ""
                errorText = ""

            },
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                if (engineerOverride) {
                    "✓ Engineer"
                } else {
                    "Engineer"
                }
            )
        }
    }

    if (engineerOverride) {

        Text(
            text =
                "Engineer Override: select a verified catalog cable. The engineering core will validate it.",
            style =
                MaterialTheme.typography.bodySmall
        )

        EngineeringDropdown(
            label = "Engineer Cable Size",
            value =
                selectedManualSize
                    ?.let {
                        "%.1f mm²".format(it)
                    }
                    ?: "Select cable size",

            options =
                availableCables.map {
                    "%.1f mm²"
                        .format(it.sizeMm2)
                },

            onSelected = { value ->

                selectedManualSize =
                    value
                        .removeSuffix(" mm²")
                        .toDoubleOrNull()

                resultText = ""
                errorText = ""
            }
        )
    }

    Button(
        onClick = {
            calculateCable()
        },
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text(
            if (engineerOverride) {
                "VALIDATE ENGINEER CABLE"
            } else {
                "AUTO SELECT CABLE"
            }
        )
    }

    if (errorText.isNotBlank()) {

        HorizontalDivider()

        Text(
            text = errorText,
            color =
                MaterialTheme.colorScheme.error,
            style =
                MaterialTheme.typography.bodyMedium
        )
    }

    if (resultText.isNotBlank()) {

        HorizontalDivider()

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = resultText,
                modifier =
                    Modifier.padding(14.dp),
                style =
                    MaterialTheme.typography.bodyMedium
            )
        }
    }

    HorizontalDivider()

    Text(
        text = "Current Project Cable",
        style =
            MaterialTheme.typography.titleMedium
    )

    val stored =
        ProjectManager.calculation

    Text(
        text =
            "Cable Size: %.1f mm²"
                .format(stored.cableSizeMm2)
    )

    Text(
        text =
            "Ampacity Iz: %.2f A"
                .format(stored.cableAmpacityA)
    )

    Text(
        text =
            "Length: %.2f m"
                .format(stored.cableLengthM)
    )

    Text(
        text =
            "Voltage Drop: %.2f V"
                .format(stored.voltageDropV)
    )

    Text(
        text =
            "Voltage Drop: %.2f %%"
                .format(stored.voltageDropPercent)
    )

    Text(
        text =
            "Design Status: ${stored.designStatus}"
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
        Text("Back")
    }
}

}

@Composable
private fun EngineeringDropdown(
label: String,
value: String,
options: List<String>,
onSelected: (String) -> Unit
) {

var expanded by remember {
    mutableStateOf(false)
}

Column(
    modifier =
        Modifier.fillMaxWidth()
) {

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = {
            Text(label)
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    expanded = true
                }
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        }
    ) {

        options.forEach { option ->

            DropdownMenuItem(
                text = {
                    Text(option)
                },
                onClick = {

                    expanded = false
                    onSelected(option)
                }
            )
        }
    }
}

}
