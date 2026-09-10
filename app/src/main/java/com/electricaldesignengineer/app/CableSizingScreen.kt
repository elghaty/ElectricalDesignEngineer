package com.electricaldesignengineer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

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

    var loadKW by remember {
        mutableStateOf("")
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
                "%.0f".format(project.voltageV)
            } else {
                "400"
            }
        )
    }

    var powerFactor by remember {
        mutableStateOf(
            if (project.powerFactor > 0.0) {
                "%.3f".format(project.powerFactor)
            } else {
                "0.90"
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

    var ambientTemperature by remember {
        mutableStateOf(30)
    }

    var circuitsInConduit by remember {
        mutableStateOf(1)
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

    fun calculateDesignCurrentFromLoad(
        load: Double,
        voltageValue: Double,
        pf: Double
    ): Double? {

        if (load <= 0.0) {
            return null
        }

        if (voltageValue <= 0.0) {
            return null
        }

        if (pf !in 0.01..1.0) {
            return null
        }

        /*
         * IMPORTANT:
         *
         * No electrical formula is duplicated here.
         *
         * The calculation is intentionally routed through
         * ProfessionalEngineeringCore.calculateLoads().
         */

        val phaseSystem =
            if (isThreePhase) {
                ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE
            } else {
                ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
            }

        val system =
            ProfessionalEngineeringCore.SystemInput(
                voltageV = voltageValue,
                frequencyHz = 50.0,
                phaseSystem = phaseSystem,
                powerFactor = pf
            )

        val loadInput =
            ProfessionalEngineeringCore.LoadInput(
                name = "Cable Design Load",
                quantity = 1.0,
                unitPowerKW = load,
                demandFactor = 1.0,
                powerFactor = pf
            )

        val result =
            ProfessionalEngineeringCore.calculateLoads(
                loads = listOf(loadInput),
                system = system
            )

        if (
            result.trace.status == EngineeringStatus.FAIL ||
            result.currentA <= 0.0
        ) {
            return null
        }

        return result.currentA
    }

    fun calculateCable() {

        resultText = ""
        errorText = ""

        val enteredCurrent =
            designCurrent
                .replace(',', '.')
                .toDoubleOrNull()

        val enteredLoad =
            loadKW
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

        /*
         * Design current priority:
         *
         * 1. Engineer entered Design Current
         * 2. Otherwise calculate it automatically from Load
         */

        val current =
            if (
                enteredCurrent != null &&
                enteredCurrent > 0.0
            ) {

                enteredCurrent

            } else if (
                enteredLoad != null &&
                enteredLoad > 0.0
            ) {

                calculateDesignCurrentFromLoad(
                    load = enteredLoad,
                    voltageValue = systemVoltage,
                    pf = pf
                )

            } else {

                null
            }

        if (current == null || current <= 0.0) {

            errorText =
                "Enter Design Current or enter Load (kW) to calculate the current automatically."

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

                        abs(
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
                    if (isThreePhase) {
                        3
                    } else {
                        2
                    },

                ambientTemperatureC =
                    ambientTemperature.toDouble(),

                groupingFactor =
                    groupingFactorForCircuits(
                        circuitsInConduit
                    ),

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

        val currentSource =
            if (
                enteredCurrent != null &&
                enteredCurrent > 0.0
            ) {
                "Engineer Design Current"
            } else {
                "Automatically calculated from Load"
            }

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
                        .format(
                            selected.sizeMm2
                        )
                )

                appendLine(
                    "Parallel Runs : %d"
                        .format(
                            calculation.parallelRuns
                        )
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
                    "Current Source : $currentSource"
                )

                appendLine()

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
                    "Conductor : ${material.name}"
                )

                appendLine(
                    "Insulation : ${insulation.name}"
                )

                appendLine(
                    "Installation : ${installationMethod.name}"
                )

                appendLine(
                    "Ambient Temperature : ${ambientTemperature} °C"
                )

                appendLine(
                    "Circuits in Conduit : $circuitsInConduit"
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

    val wideLayout =
        androidx.compose.ui.platform.LocalConfiguration
            .current
            .screenWidthDp >= 700

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 18.dp,
                        vertical = 14.dp
                    ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            HeaderSection(
                projectName =
                    project.projectName.ifBlank {
                        "Current Project"
                    }
            )

            if (wideLayout) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(16.dp),
                    verticalAlignment =
                        Alignment.Top
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        DesignInputCard(
                            designCurrent = designCurrent,
                            onDesignCurrentChange = {
                                designCurrent = it
                            },
                            loadKW = loadKW,
                            onLoadChange = {
                                loadKW = it
                            },
                            length = length,
                            onLengthChange = {
                                length = it
                            },
                            voltage = voltage,
                            onVoltageChange = {
                                voltage = it
                            },
                            powerFactor = powerFactor,
                            onPowerFactorChange = {
                                powerFactor = it
                            },
                            maximumVoltageDrop =
                                maximumVoltageDrop,
                            onMaximumVoltageDropChange = {
                                maximumVoltageDrop = it
                            },
                            isThreePhase =
                                isThreePhase,
                            onThreePhaseChange = {
                                isThreePhase = it
                            }
                        )

                        CableDataCard(
                            material = material,
                            onMaterialChange = {
                                material = it
                                selectedManualSize = null
                                resultText = ""
                                errorText = ""
                            },
                            insulation = insulation,
                            onInsulationChange = {
                                insulation = it
                                selectedManualSize = null
                                resultText = ""
                                errorText = ""
                            },
                            installationMethod =
                                installationMethod,
                            onInstallationChange = {
                                installationMethod = it
                                selectedManualSize = null
                                resultText = ""
                                errorText = ""
                            },
                            ambientTemperature =
                                ambientTemperature,
                            onAmbientTemperatureChange = {
                                ambientTemperature = it
                                resultText = ""
                                errorText = ""
                            },
                            circuitsInConduit =
                                circuitsInConduit,
                            onCircuitsChange = {
                                circuitsInConduit = it
                                resultText = ""
                                errorText = ""
                            }
                        )
                    }

                    Column(
                        modifier =
                            Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        SelectionModeCard(
                            engineerOverride =
                                engineerOverride,
                            onAutomatic = {

                                engineerOverride = false
                                selectedManualSize = null
                                resultText = ""
                                errorText = ""
                            },
                            onEngineer = {

                                engineerOverride = true
                                resultText = ""
                                errorText = ""
                            },
                            selectedManualSize =
                                selectedManualSize,
                            availableCables =
                                availableCables,
                            onManualSizeChange = {
                                selectedManualSize = it
                                resultText = ""
                                errorText = ""
                            }
                        )

                        CalculateCard(
                            onCalculate = {
                                calculateCable()
                            }
                        )

                        ResultCard(
                            resultText = resultText,
                            errorText = errorText
                        )

                        CurrentProjectCableCard()
                    }
                }

            } else {

                DesignInputCard(
                    designCurrent = designCurrent,
                    onDesignCurrentChange = {
                        designCurrent = it
                    },
                    loadKW = loadKW,
                    onLoadChange = {
                        loadKW = it
                    },
                    length = length,
                    onLengthChange = {
                        length = it
                    },
                    voltage = voltage,
                    onVoltageChange = {
                        voltage = it
                    },
                    powerFactor = powerFactor,
                    onPowerFactorChange = {
                        powerFactor = it
                    },
                    maximumVoltageDrop =
                        maximumVoltageDrop,
                    onMaximumVoltageDropChange = {
                        maximumVoltageDrop = it
                    },
                    isThreePhase =
                        isThreePhase,
                    onThreePhaseChange = {
                        isThreePhase = it
                    }
                )

                CableDataCard(
                    material = material,
                    onMaterialChange = {
                        material = it
                        selectedManualSize = null
                        resultText = ""
                        errorText = ""
                    },
                    insulation = insulation,
                    onInsulationChange = {
                        insulation = it
                        selectedManualSize = null
                        resultText = ""
                        errorText = ""
                    },
                    installationMethod =
                        installationMethod,
                    onInstallationChange = {
                        installationMethod = it
                        selectedManualSize = null
                        resultText = ""
                        errorText = ""
                    },
                    ambientTemperature =
                        ambientTemperature,
                    onAmbientTemperatureChange = {
                        ambientTemperature = it
                        resultText = ""
                        errorText = ""
                    },
                    circuitsInConduit =
                        circuitsInConduit,
                    onCircuitsChange = {
                        circuitsInConduit = it
                        resultText = ""
                        errorText = ""
                    }
                )

                SelectionModeCard(
                    engineerOverride =
                        engineerOverride,
                    onAutomatic = {

                        engineerOverride = false
                        selectedManualSize = null
                        resultText = ""
                        errorText = ""
                    },
                    onEngineer = {

                        engineerOverride = true
                        resultText = ""
                        errorText = ""
                    },
                    selectedManualSize =
                        selectedManualSize,
                    availableCables =
                        availableCables,
                    onManualSizeChange = {
                        selectedManualSize = it
                        resultText = ""
                        errorText = ""
                    }
                )

                CalculateCard(
                    onCalculate = {
                        calculateCable()
                    }
                )

                ResultCard(
                    resultText = resultText,
                    errorText = errorText
                )

                CurrentProjectCableCard()
            }

            Button(
                onClick = onBack,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )
        }
    }
}


/* ============================================================
   HEADER
   ============================================================ */

@Composable
private fun HeaderSection(
    projectName: String
) {

    Column(
        modifier =
            Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {

        Text(
            text = "CABLE SIZING",
            style =
                MaterialTheme.typography.headlineSmall,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text = "Professional Cable Selection",
            style =
                MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Project: $projectName",
            style =
                MaterialTheme.typography.bodySmall
        )

        HorizontalDivider(
            modifier =
                Modifier.padding(
                    top = 6.dp
                )
        )
    }
}


/* ============================================================
   DESIGN INPUT CARD
   ============================================================ */

@Composable
private fun DesignInputCard(
    designCurrent: String,
    onDesignCurrentChange: (String) -> Unit,
    loadKW: String,
    onLoadChange: (String) -> Unit,
    length: String,
    onLengthChange: (String) -> Unit,
    voltage: String,
    onVoltageChange: (String) -> Unit,
    powerFactor: String,
    onPowerFactorChange: (String) -> Unit,
    maximumVoltageDrop: String,
    onMaximumVoltageDropChange: (String) -> Unit,
    isThreePhase: Boolean,
    onThreePhaseChange: (Boolean) -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            SectionTitle(
                title = "DESIGN INPUTS"
            )

            OutlinedTextField(
                value = designCurrent,
                onValueChange =
                    onDesignCurrentChange,
                label = {
                    Text(
                        "Design Current Ib (A)"
                    )
                },
                placeholder = {
                    Text(
                        "Optional - calculated from Load"
                    )
                },
                singleLine = true,
                modifier =
                    Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = loadKW,
                onValueChange =
                    onLoadChange,
                label = {
                    Text("Load (kW)")
                },
                placeholder = {
                    Text(
                        "Used when Design Current is empty"
                    )
                },
                singleLine = true,
                modifier =
                    Modifier.fillMaxWidth()
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(
                    value = length,
                    onValueChange =
                        onLengthChange,
                    label = {
                        Text("Length (m)")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = voltage,
                    onValueChange =
                        onVoltageChange,
                    label = {
                        Text("Voltage (V)")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.weight(1f)
                )
            }

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                OutlinedTextField(
                    value = powerFactor,
                    onValueChange =
                        onPowerFactorChange,
                    label = {
                        Text("Power Factor")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = maximumVoltageDrop,
                    onValueChange =
                        onMaximumVoltageDropChange,
                    label = {
                        Text("Max V-Drop (%)")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.weight(1f)
                )
            }

            Text(
                text = "CURRENT TYPE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                if (isThreePhase) {

                    Button(
                        onClick = {
                            onThreePhaseChange(true)
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("✓ 3 PHASE")
                    }

                    OutlinedButton(
                        onClick = {
                            onThreePhaseChange(false)
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("1 PHASE")
                    }

                } else {

                    OutlinedButton(
                        onClick = {
                            onThreePhaseChange(true)
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("3 PHASE")
                    }

                    Button(
                        onClick = {
                            onThreePhaseChange(false)
                        },
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("✓ 1 PHASE")
                    }
                }
            }
        }
    }
}


/* ============================================================
   CABLE DATA CARD
   ============================================================ */

@Composable
private fun CableDataCard(
    material: CableMaterial,
    onMaterialChange: (CableMaterial) -> Unit,
    insulation: InsulationType,
    onInsulationChange: (InsulationType) -> Unit,
    installationMethod: InstallationMethod,
    onInstallationChange: (InstallationMethod) -> Unit,
    ambientTemperature: Int,
    onAmbientTemperatureChange: (Int) -> Unit,
    circuitsInConduit: Int,
    onCircuitsChange: (Int) -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            SectionTitle(
                title = "CABLE DATA"
            )

            EngineeringDropdown(
                label = "Conductor",
                value = material.name,
                options =
                    CableMaterial
                        .values()
                        .map {
                            it.name
                        },
                onSelected = {
                    onMaterialChange(
                        CableMaterial.valueOf(it)
                    )
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
                onSelected = {
                    onInsulationChange(
                        InsulationType.valueOf(it)
                    )
                }
            )

            EngineeringDropdown(
                label = "Method of installation",
                value =
                    installationMethod.displayName(),
                options =
                    InstallationMethod
                        .values()
                        .map {
                            it.displayName()
                        },
                onSelected = { selected ->

                    val enumValue =
                        InstallationMethod
                            .values()
                            .first {
                                it.displayName() == selected
                            }

                    onInstallationChange(
                        enumValue
                    )
                }
            )

            EngineeringDropdown(
                label = "Ambient temperature",
                value = "$ambientTemperature °C",
                options =
                    listOf(
                        20,
                        25,
                        30,
                        35,
                        40,
                        45,
                        50,
                        55,
                        60
                    ).map {
                        "$it °C"
                    },
                onSelected = { selected ->

                    onAmbientTemperatureChange(
                        selected
                            .removeSuffix(" °C")
                            .toInt()
                    )
                }
            )

            EngineeringDropdown(
                label = "Circuits in same conduit",
                value =
                    circuitsInConduit.toString(),
                options =
                    (1..8).map {
                        it.toString()
                    },
                onSelected = {
                    onCircuitsChange(
                        it.toInt()
                    )
                }
            )
        }
    }
}


/* ============================================================
   SELECTION MODE CARD
   ============================================================ */

@Composable
private fun SelectionModeCard(
    engineerOverride: Boolean,
    onAutomatic: () -> Unit,
    onEngineer: () -> Unit,
    selectedManualSize: Double?,
    availableCables:
        List<ProfessionalEngineeringCore.CableData>,
    onManualSizeChange: (Double) -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            SectionTitle(
                title = "CABLE SELECTION"
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                if (!engineerOverride) {

                    Button(
                        onClick = onAutomatic,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("✓ AUTOMATIC")
                    }

                    OutlinedButton(
                        onClick = onEngineer,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("ENGINEER")
                    }

                } else {

                    OutlinedButton(
                        onClick = onAutomatic,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("AUTOMATIC")
                    }

                    Button(
                        onClick = onEngineer,
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text("✓ ENGINEER")
                    }
                }
            }

            if (engineerOverride) {

                Text(
                    text =
                        "Engineer Override: select a verified catalog cable. The Professional Engineering Core will validate the selection.",
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
                                .format(
                                    it.sizeMm2
                                )
                        },
                    onSelected = { value ->

                        val size =
                            value
                                .removeSuffix(
                                    " mm²"
                                )
                                .toDoubleOrNull()

                        if (size != null) {
                            onManualSizeChange(size)
                        }
                    }
                )
            }

            Text(
                text =
                    "Verified catalog cables available: ${availableCables.size}",
                style =
                    MaterialTheme.typography.bodySmall
            )
        }
    }
}


/* ============================================================
   CALCULATE CARD
   ============================================================ */

@Composable
private fun CalculateCard(
    onCalculate: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
        ) {

            Button(
                onClick = onCalculate,
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text =
                        "CALCULATE & SELECT CABLE",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


/* ============================================================
   RESULT CARD
   ============================================================ */

@Composable
private fun ResultCard(
    resultText: String,
    errorText: String
) {

    if (
        resultText.isBlank() &&
        errorText.isBlank()
    ) {
        return
    }

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            SectionTitle(
                title = "ENGINEERING RESULT"
            )

            if (errorText.isNotBlank()) {

                Text(
                    text = errorText,
                    color =
                        MaterialTheme
                            .colorScheme
                            .error,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            if (resultText.isNotBlank()) {

                Text(
                    text = resultText,
                    style =
                        MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


/* ============================================================
   CURRENT PROJECT CABLE
   ============================================================ */

@Composable
private fun CurrentProjectCableCard() {

    val stored =
        ProjectManager.calculation

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            SectionTitle(
                title = "CURRENT PROJECT CABLE"
            )

            ResultLine(
                label = "Cable Size",
                value =
                    "%.1f mm²"
                        .format(
                            stored.cableSizeMm2
                        )
            )

            ResultLine(
                label = "Ampacity Iz",
                value =
                    "%.2f A"
                        .format(
                            stored.cableAmpacityA
                        )
            )

            ResultLine(
                label = "Length",
                value =
                    "%.2f m"
                        .format(
                            stored.cableLengthM
                        )
            )

            ResultLine(
                label = "Voltage Drop",
                value =
                    "%.2f V"
                        .format(
                            stored.voltageDropV
                        )
            )

            ResultLine(
                label = "Voltage Drop %",
                value =
                    "%.2f %%"
                        .format(
                            stored.voltageDropPercent
                        )
            )

            ResultLine(
                label = "Design Status",
                value =
                    stored.designStatus
            )
        }
    }
}


/* ============================================================
   GENERIC SECTION TITLE
   ============================================================ */

@Composable
private fun SectionTitle(
    title: String
) {

    Text(
        text = title,
        style =
            MaterialTheme.typography.titleMedium,
        fontWeight =
            FontWeight.Bold
    )
}


/* ============================================================
   RESULT LINE
   ============================================================ */

@Composable
private fun ResultLine(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            fontWeight =
                FontWeight.Medium
        )

        Text(
            text = value
        )
    }
}


/* ============================================================
   PROFESSIONAL DROPDOWN
   ============================================================ */

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

        Text(
            text = label,
            style =
                MaterialTheme.typography.bodySmall,
            fontWeight =
                FontWeight.Medium
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Box(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                8.dp
                            )
                        )
                        .clickable {

                            expanded =
                                !expanded
                        },
                tonalElevation = 1.dp,
                shape =
                    RoundedCornerShape(
                        8.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 14.dp,
                                vertical = 15.dp
                            ),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = value,
                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge
                    )

                    Text(
                        text =
                            if (expanded) {
                                "▲"
                            } else {
                                "▼"
                            },
                        fontSize = 13.sp
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = {
                            Text(option)
                        },
                        onClick = {

                            expanded = false

                            onSelected(
                                option
                            )
                        }
                    )
                }
            }
        }
    }
}


/* ============================================================
   INSTALLATION DISPLAY NAMES
   ============================================================ */

private fun InstallationMethod.displayName():
    String {

    return when (this) {

        InstallationMethod.CONDUIT ->
            "Conduit"

        InstallationMethod.TRUNKING ->
            "Trunking"

        InstallationMethod.CABLE_TRAY ->
            "Cable Tray"

        InstallationMethod.CABLE_LADDER ->
            "Cable Ladder"

        InstallationMethod.FREE_AIR ->
            "Free Air"

        InstallationMethod.DIRECT_BURIED ->
            "Direct Buried"

        InstallationMethod.DUCT ->
            "Duct"
    }
}


/* ============================================================
   GROUPING FACTOR
   ============================================================ */

private fun groupingFactorForCircuits(
    circuits: Int
): Double {

    return when {

        circuits <= 1 ->
            1.00

        circuits == 2 ->
            0.80

        circuits == 3 ->
            0.70

        circuits == 4 ->
            0.65

        circuits == 5 ->
            0.60

        circuits == 6 ->
            0.57

        circuits == 7 ->
            0.54

        else ->
            0.52
    }
}
