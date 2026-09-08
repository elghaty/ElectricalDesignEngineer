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
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
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
            project.cableLengthM.toString()
        } else {
            "30"
        }
    )
}

var voltage by remember {
    mutableStateOf(
        if (project.voltageV > 0.0) {
            project.voltageV.toString()
        } else {
            "400"
        }
    )
}

var powerFactor by remember {
    mutableStateOf(
        if (project.powerFactor > 0.0) {
            project.powerFactor.toString()
        } else {
            "0.90"
        }
    )
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

var maximumVoltageDrop by remember {
    mutableStateOf("5.0")
}

var selectedCableSize by remember {
    mutableStateOf<Double?>(null)
}

var selectedParallelRuns by remember {
    mutableStateOf(1)
}

var manualMode by remember {
    mutableStateOf(false)
}

var result by remember {
    mutableStateOf("")
}

var errorMessage by remember {
    mutableStateOf("")
}

val availableCables = remember(
    material,
    insulation,
    installationMethod
) {
    EngineeringCatalogRepository.getCableData(
        material = material,
        insulation = insulation,
        installationMethod = installationMethod
    )
}

Column(
    modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
) {

    Text(
        text = "Cable Sizing",
        style = MaterialTheme.typography.headlineSmall
    )

    Text(
        text = "Professional Cable Selection",
        style = MaterialTheme.typography.bodyMedium
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
        text = "Design Inputs",
        style = MaterialTheme.typography.titleMedium
    )

    OutlinedTextField(
        value = designCurrent,
        onValueChange = {
            designCurrent = it
        },
        label = {
            Text("Design Current (A)")
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
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
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = voltage,
        onValueChange = {
            voltage = it
        },
        label = {
            Text("Voltage (V)")
        },
        singleLine = true,
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
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Text(
        text = "Phase System",
        style = MaterialTheme.typography.titleSmall
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Button(
            onClick = {
                isThreePhase = true
            },
            modifier = Modifier.weight(1f)
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
            modifier = Modifier.weight(1f)
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
        style = MaterialTheme.typography.titleMedium
    )

    EngineeringDropdown(
        label = "Conductor Material",
        value = material.name,
        options = CableMaterial.entries.map { it.name },
        onSelected = { value ->
            material = CableMaterial.valueOf(value)
            selectedCableSize = null
        }
    )

    EngineeringDropdown(
        label = "Insulation",
        value = insulation.name,
        options = InsulationType.entries.map { it.name },
        onSelected = { value ->
            insulation = InsulationType.valueOf(value)
            selectedCableSize = null
        }
    )

    EngineeringDropdown(
        label = "Installation Method",
        value = installationMethod.name,
        options = InstallationMethod.entries.map { it.name },
        onSelected = { value ->
            installationMethod = InstallationMethod.valueOf(value)
            selectedCableSize = null
        }
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
        modifier = Modifier.fillMaxWidth()
    )

    HorizontalDivider()

    Text(
        text = "Cable Selection",
        style = MaterialTheme.typography.titleMedium
    )

    Text(
        text = if (manualMode) {
            "Manual selection mode"
        } else {
            "Automatic selection mode"
        },
        style = MaterialTheme.typography.bodyMedium
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Button(
            onClick = {
                manualMode = false
                selectedCableSize = null
                selectedParallelRuns = 1
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                if (!manualMode) {
                    "✓ Automatic"
                } else {
                    "Automatic"
                }
            )
        }

        OutlinedButton(
            onClick = {
                manualMode = true
                selectedCableSize = null
                selectedParallelRuns = 1
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                if (manualMode) {
                    "✓ Manual"
                } else {
                    "Manual"
                }
            )
        }
    }

    if (manualMode) {

        if (availableCables.isEmpty()) {

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "No verified cable data available",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = "The catalog does not currently contain verified cable records for the selected material, insulation and installation method."
                    )
                }
            }

        } else {

            EngineeringDropdown(
                label = "Select Cable Size",
                value = selectedCableSize?.let {
                    "%.1f mm²".format(it)
                } ?: "Select cable",
                options = availableCables.map {
                    "%.1f mm²".format(it.sizeMm2)
                },
                onSelected = { value ->

                    val sizeText =
                        value.removeSuffix(" mm²")

                    selectedCableSize =
                        sizeText.toDoubleOrNull()
                }
            )
        }
    }

    if (selectedCableSize != null) {

        Text(
            text = "Selected Cable: %.1f mm²".format(
                selectedCableSize
            ),
            style = MaterialTheme.typography.titleSmall
        )
    }

    Button(
        onClick = {

            errorMessage = ""
            result = ""

            val current =
                designCurrent.toDoubleOrNull()

            val cableLength =
                length.toDoubleOrNull()

            val v =
                voltage.toDoubleOrNull()

            val pf =
                powerFactor
                    .toDoubleOrNull()
                    ?.coerceIn(0.01, 1.0)

            val maxDrop =
                maximumVoltageDrop
                    .toDoubleOrNull()

            if (
                current == null ||
                current <= 0.0
            ) {
                errorMessage =
                    "Design current must be greater than zero."
                return@Button
            }

            if (
                cableLength == null ||
                cableLength <= 0.0
            ) {
                errorMessage =
                    "Cable length must be greater than zero."
                return@Button
            }

            if (
                v == null ||
                v <= 0.0
            ) {
                errorMessage =
                    "Voltage must be greater than zero."
                return@Button
            }

            if (pf == null) {
                errorMessage =
                    "Enter a valid power factor."
                return@Button
            }

            if (
                maxDrop == null ||
                maxDrop <= 0.0
            ) {
                errorMessage =
                    "Maximum voltage drop must be greater than zero."
                return@Button
            }

            if (manualMode && selectedCableSize == null) {
                errorMessage =
                    "Select a cable size before manual calculation."
                return@Button
            }

            if (availableCables.isEmpty()) {
                errorMessage =
                    "No verified cable data is available in the engineering catalog."
                return@Button
            }

            ProjectManager.updateSystem(
                voltageV = v,
                powerFactor = pf,
                isThreePhase = isThreePhase
            )

            val phaseSystem =
                if (isThreePhase) {
                    ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE
                } else {
                    ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
                }

            val designInput =
                ProfessionalEngineeringCore.CableDesignInput(
                    designCurrentA = current,
                    lengthM = cableLength,
                    voltageV = v,
                    powerFactor = pf,
                    phaseSystem = phaseSystem,
                    conductorMaterial = material,
                    insulation = insulation,
                    installationMethod = installationMethod,
                    numberOfLoadedConductors =
                        if (isThreePhase) 3 else 2,
                    ambientTemperatureC = 30.0,
                    groupingFactor = 1.0,
                    thermalInsulationFactor = 1.0,
                    soilCorrectionFactor = 1.0,
                    maximumVoltageDropPercent = maxDrop,
                    maximumParallelRuns = 8
                )

            val provider =
                object :
                    ProfessionalEngineeringCore.CableDataProvider {

                    override fun availableCables(
                        material:
                            CableMaterial,
                        insulation:
                            InsulationType,
                        installationMethod:
                            InstallationMethod
                    ): List<ProfessionalEngineeringCore.CableData> {

                        val source =
                            EngineeringCatalogRepository
                                .getCableData(
                                    material = material,
                                    insulation = insulation,
                                    installationMethod =
                                        installationMethod
                                )

                        if (!manualMode) {
                            return source
                        }

                        val selected =
                            selectedCableSize
                                ?: return emptyList()

                        return source.filter {
                            kotlin.math.abs(
                                it.sizeMm2 - selected
                            ) < 0.0001
                        }
                    }
                }

            val calculation =
                ProfessionalEngineeringCore.designCable(
                    input = designInput,
                    provider = provider
                )

            val selected =
                calculation.selectedCable

            if (selected == null) {

                errorMessage =
                    when (calculation.status) {
                        EngineeringStatus.DATA_REQUIRED ->
                            "Cable calculation requires verified catalog data."

                        EngineeringStatus.FAIL ->
                            "No cable configuration satisfies the design requirements."

                        else ->
                            "No suitable cable was found."
                    }

                return@Button
            }

            selectedCableSize =
                selected.sizeMm2

            selectedParallelRuns =
                calculation.parallelRuns

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

            result =
                buildString {

                    appendLine(
                        if (manualMode) {
                            "MANUALLY SELECTED CABLE"
                        } else {
                            "AUTOMATICALLY SELECTED CABLE"
                        }
                    )

                    appendLine()

                    appendLine(
                        "Cable Size: %.1f mm²"
                            .format(selected.sizeMm2)
                    )

                    appendLine(
                        "Parallel Runs: %d"
                            .format(calculation.parallelRuns)
                    )

                    appendLine(
                        "Ampacity / Run: %.2f A"
                            .format(
                                calculation
                                    .correctedAmpacityPerRunA
                            )
                    )

                    appendLine(
                        "Total Ampacity: %.2f A"
                            .format(
                                calculation.totalAmpacityA
                            )
                    )

                    appendLine()

                    appendLine(
                        "Voltage Drop: %.2f V"
                            .format(
                                calculation.voltageDropV
                            )
                    )

                    appendLine(
                        "Voltage Drop: %.2f %%"
                            .format(
                                calculation.voltageDropPercent
                            )
                    )

                    appendLine()

                    appendLine(
                        "Design Current: %.2f A"
                            .format(current)
                    )

                    appendLine(
                        "Status: ${calculation.status}"
                    )

                    appendLine()

                    appendLine(
                        "Source: ${selected.source}"
                    )

                    appendLine(
                        "Revision: ${selected.revision}"
                    )
                }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (manualMode) {
                "Validate & Save Selected Cable"
            } else {
                "Automatically Select & Save Cable"
            }
        )
    }

    if (errorMessage.isNotBlank()) {

        HorizontalDivider()

        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }

    if (result.isNotBlank()) {

        HorizontalDivider()

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = result,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    HorizontalDivider()

    Text(
        text = "Current Project",
        style = MaterialTheme.typography.titleMedium
    )

    Text(
        text = "Design Current: %.2f A".format(
            ProjectManager.calculation.designCurrentA
        )
    )

    Text(
        text = "Cable: %.1f mm²".format(
            ProjectManager.calculation.cableSizeMm2
        )
    )

    Text(
        text = "Ampacity: %.1f A".format(
            ProjectManager.calculation.cableAmpacityA
        )
    )

    Text(
        text = "Cable Length: %.1f m".format(
            ProjectManager.calculation.cableLengthM
        )
    )

    Text(
        text = "Voltage Drop: %.2f %%".format(
            ProjectManager.calculation.voltageDropPercent
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

@OptIn(ExperimentalMaterial3Api::class)
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

ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = {
        expanded = !expanded
    },
    modifier = Modifier.fillMaxWidth()
) {

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = {
            Text(label)
        },
        trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(
                expanded = expanded
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor()
    )

    ExposedDropdownMenu(
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

                    onSelected(option)

                    expanded = false
                }
            )
        }
    }
}

}
