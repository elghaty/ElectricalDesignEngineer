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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CableSizingScreen(
onBack: () -> Unit = {}
) {

val project = ProjectManager.calculation

// ============================================================
// INPUTS
// ============================================================

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
            ""
        }
    )
}

var voltage by remember {
    mutableStateOf(
        if (project.voltageV > 0.0) {
            project.voltageV.toString()
        } else {
            ""
        }
    )
}

var powerFactor by remember {
    mutableStateOf(
        if (project.powerFactor > 0.0) {
            project.powerFactor.toString()
        } else {
            ""
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

var manualMode by remember {
    mutableStateOf(false)
}

var result by remember {
    mutableStateOf("")
}

var errorMessage by remember {
    mutableStateOf("")
}

// ============================================================
// VERIFIED CABLE CATALOG
// ============================================================

val availableCables =
    remember(
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
                    it.source.isNotBlank() &&
                    it.revision.isNotBlank()
            }
            .sortedBy {
                it.sizeMm2
            }
    }

// ============================================================
// CABLE ENGINEERING CALCULATION
// ============================================================

fun calculateCable() {

    result = ""
    errorMessage = ""

    val current =
        designCurrent
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }

    val cableLength =
        length
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }

    val v =
        voltage
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }

    val pf =
        powerFactor
            .toDoubleOrNull()
            ?.takeIf {
                it > 0.0 && it <= 1.0
            }

    val maxDrop =
        maximumVoltageDrop
            .toDoubleOrNull()
            ?.takeIf { it > 0.0 }

    if (current == null) {
        errorMessage =
            "Design current must be greater than zero."
        return
    }

    if (cableLength == null) {
        errorMessage =
            "Cable length must be greater than zero."
        return
    }

    if (v == null) {
        errorMessage =
            "Voltage must be greater than zero."
        return
    }

    if (pf == null) {
        errorMessage =
            "Power factor must be between 0 and 1."
        return
    }

    if (maxDrop == null) {
        errorMessage =
            "Maximum voltage drop must be greater than zero."
        return
    }

    if (availableCables.isEmpty()) {
        errorMessage =
            "No verified cable data is available for the selected configuration."
        return
    }

    if (
        manualMode &&
        selectedCableSize == null
    ) {
        errorMessage =
            "Select a cable size for engineer override."
        return
    }

    // ========================================================
    // SAVE SYSTEM DATA
    // ========================================================

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

    // ========================================================
    // VERIFIED CATALOG PROVIDER
    // ========================================================

    val provider =
        object :
            ProfessionalEngineeringCore.CableDataProvider {

            override fun availableCables(
                material: CableMaterial,
                insulation: InsulationType,
                installationMethod: InstallationMethod
            ):
                List<
                    ProfessionalEngineeringCore.CableData
                > {

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
                                it.source.isNotBlank() &&
                                it.revision.isNotBlank()
                        }

                if (!manualMode) {
                    return catalog
                }

                val selected =
                    selectedCableSize
                        ?: return emptyList()

                return catalog.filter {
                    kotlin.math.abs(
                        it.sizeMm2 - selected
                    ) < 0.0001
                }
            }
        }

    // ========================================================
    // PROFESSIONAL ENGINEERING CORE
    // ========================================================

    val input =
        ProfessionalEngineeringCore.CableDesignInput(

            designCurrentA = current,

            lengthM = cableLength,

            voltageV = v,

            powerFactor = pf,

            phaseSystem = phaseSystem,

            conductorMaterial = material,

            insulation = insulation,

            installationMethod =
                installationMethod,

            numberOfLoadedConductors =
                if (isThreePhase) 3 else 2,

            ambientTemperatureC = 30.0,

            groupingFactor = 1.0,

            thermalInsulationFactor = 1.0,

            soilCorrectionFactor = 1.0,

            maximumVoltageDropPercent =
                maxDrop,

            maximumParallelRuns = 8
        )

    val calculation =
        ProfessionalEngineeringCore
            .designCable(
                input = input,
                provider = provider
            )

    // ========================================================
    // VALIDATE ENGINEERING RESULT
    // ========================================================

    val selected =
        calculation.selectedCable

    if (selected == null) {

        errorMessage =
            when (calculation.status) {

                EngineeringStatus.DATA_REQUIRED ->
                    "DATA REQUIRED: verified cable engineering data is missing."

                EngineeringStatus.FAIL ->
                    "No cable configuration satisfies the ampacity and voltage-drop requirements."

                EngineeringStatus.WARNING ->
                    "Cable calculation returned a warning without a valid selected cable."

                else ->
                    "No suitable cable was found."
            }

        return
    }

    // ========================================================
    // SAVE RESULT
    // ========================================================

    selectedCableSize =
        selected.sizeMm2

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

    // ========================================================
    // RESULT
    // ========================================================

    result =
        buildString {

            appendLine(
                if (manualMode) {
                    "ENGINEER OVERRIDE — CABLE VALIDATED"
                } else {
                    "AUTOMATIC CABLE SELECTION"
                }
            )

            appendLine()

            appendLine(
                "Cable Size: %.1f mm²"
                    .format(
                        selected.sizeMm2
                    )
            )

            appendLine(
                "Parallel Runs: %d"
                    .format(
                        calculation.parallelRuns
                    )
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
                "Design Current Ib: %.2f A"
                    .format(current)
            )

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
                "Material: ${material.name}"
            )

            appendLine(
                "Insulation: ${insulation.name}"
            )

            appendLine(
                "Installation: ${installationMethod.name}"
            )

            appendLine()

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
}

// ============================================================
// UI
// ============================================================

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
            CableMaterial.entries.map {
                it.name
            },
        onSelected = { value ->

            material =
                CableMaterial.valueOf(value)

            selectedCableSize = null
        }
    )

    EngineeringDropdown(
        label = "Insulation",
        value = insulation.name,
        options =
            InsulationType.entries.map {
                it.name
            },
        onSelected = { value ->

            insulation =
                InsulationType.valueOf(value)

            selectedCableSize = null
        }
    )

    EngineeringDropdown(
        label = "Installation Method",
        value =
            installationMethod.name,
        options =
            InstallationMethod.entries.map {
                it.name
            },
        onSelected = { value ->

            installationMethod =
                InstallationMethod.valueOf(
                    value
                )

            selectedCableSize = null
        }
    )

    OutlinedTextField(
        value = maximumVoltageDrop,
        onValueChange = {
            maximumVoltageDrop = it
        },
        label = {
            Text(
                "Maximum Voltage Drop (%)"
            )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    HorizontalDivider()

    Text(
        text = "Cable Selection",
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

                manualMode = false
                selectedCableSize = null

            },
            modifier =
                Modifier.weight(1f)
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

            },
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                if (manualMode) {
                    "✓ Engineer Override"
                } else {
                    "Engineer Override"
                }
            )
        }
    }

    Text(
        text =
            if (manualMode) {
                "Engineer Override: select a catalog cable. The engineering core will validate it against the design current and voltage-drop requirement."
            } else {
                "Automatic: the program selects the smallest verified cable configuration that satisfies the engineering requirements."
            },
        style =
            MaterialTheme.typography.bodySmall
    )

    if (manualMode) {

        if (availableCables.isEmpty()) {

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(12.dp)
                ) {

                    Text(
                        text =
                            "No verified cable data available.",
                        style =
                            MaterialTheme
                                .typography
                                .titleSmall
                    )
                }
            }

        } else {

            EngineeringDropdown(
                label = "Engineer Cable Selection",
                value =
                    selectedCableSize
                        ?.let {
                            "%.1f mm²"
                                .format(it)
                        }
                        ?: "Select cable",

                options =
                    availableCables.map {
                        "%.1f mm²"
                            .format(
                                it.sizeMm2
                            )
                    },

                onSelected = { value ->

                    selectedCableSize =
                        value
                            .removeSuffix(
                                " mm²"
                            )
                            .toDoubleOrNull()
                }
            )
        }
    }

    if (selectedCableSize != null) {

        Text(
            text =
                "Selected Cable: %.1f mm²"
                    .format(
                        selectedCableSize
                    ),
            style =
                MaterialTheme.typography.titleSmall
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
            if (manualMode) {
                "VALIDATE & SAVE CABLE"
            } else {
                "AUTO SELECT & SAVE CABLE"
            }
        )
    }

    if (errorMessage.isNotBlank()) {

        HorizontalDivider()

        Text(
            text = errorMessage,
            color =
                MaterialTheme.colorScheme.error,
            style =
                MaterialTheme.typography.bodyMedium
        )
    }

    if (result.isNotBlank()) {

        HorizontalDivider()

        Text(
            text = result,
            style =
                MaterialTheme.typography.bodyMedium
        )
    }

    // ========================================================
    // CURRENT PROJECT RESULT
    // ========================================================

    HorizontalDivider()

    Text(
        text = "Current Project Cable",
        style =
            MaterialTheme.typography.titleMedium
    )

    Text(
        text =
            "Cable Size: %.1f mm²"
                .format(
                    ProjectManager
                        .calculation
                        .cableSizeMm2
                )
    )

    Text(
        text =
            "Cable Ampacity Iz: %.2f A"
                .format(
                    ProjectManager
                        .calculation
                        .cableAmpacityA
                )
    )

    Text(
        text =
            "Cable Length: %.2f m"
                .format(
                    ProjectManager
                        .calculation
                        .cableLengthM
                )
    )

    Text(
        text =
            "Voltage Drop: %.2f V"
                .format(
                    ProjectManager
                        .calculation
                        .voltageDropV
                )
    )

    Text(
        text =
            "Voltage Drop: %.2f %%"
                .format(
                    ProjectManager
                        .calculation
                        .voltageDropPercent
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

    Button(
        onClick = onBack,
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Text("Back")
    }
}

}
