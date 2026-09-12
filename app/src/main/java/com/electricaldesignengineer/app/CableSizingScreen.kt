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
import androidx.compose.material3.RadioButton
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

    var currentType by remember {
        mutableStateOf(
            if (project.isThreePhase) {
                "Alternating three-phase"
            } else {
                "Alternating single-phase"
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

    var load by remember {
        mutableStateOf("")
    }

    var powerFactor by remember {
        mutableStateOf(
            if (project.powerFactor > 0.0) {
                "%.2f".format(project.powerFactor)
            } else {
                "0.90"
            }
        )
    }

    var designCurrent by remember {
        mutableStateOf(
            if (project.designCurrentA > 0.0) {
                "%.2f".format(project.designCurrentA)
            } else {
                ""
            }
        )
    }

    var lineLength by remember {
        mutableStateOf(
            if (project.cableLengthM > 0.0) {
                "%.0f".format(project.cableLengthM)
            } else {
                "60"
            }
        )
    }

    var installationMethod by remember {
        mutableStateOf(
            InstallationMethod.CABLE_TRAY
        )
    }

    var ambientTemperature by remember {
        mutableStateOf(30)
    }

    var material by remember {
        mutableStateOf(
            CableMaterial.COPPER
        )
    }

    var insulation by remember {
        mutableStateOf(
            InsulationType.PVC
        )
    }

    var circuitsInConduit by remember {
        mutableStateOf(1)
    }

    var maximumVoltageDrop by remember {
        mutableStateOf("4")
    }

    var automaticSelection by remember {
        mutableStateOf(true)
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

    fun parseNumber(
        value: String
    ): Double? {

        return value
            .replace(',', '.')
            .trim()
            .toDoubleOrNull()
    }

    fun calculateCable() {

        resultText = ""
        errorText = ""

        val systemVoltage =
            parseNumber(voltage)

        val enteredLoad =
            parseNumber(load)

        val enteredCurrent =
            parseNumber(designCurrent)

        val enteredPF =
            parseNumber(powerFactor)

        val cableLength =
            parseNumber(lineLength)

        val maximumDrop =
            parseNumber(maximumVoltageDrop)

        if (
            systemVoltage == null ||
            systemVoltage <= 0.0
        ) {

            errorText =
                "Voltage must be greater than zero."

            return
        }

        if (
            cableLength == null ||
            cableLength <= 0.0
        ) {

            errorText =
                "Line length must be greater than zero."

            return
        }

        if (
            enteredPF == null ||
            enteredPF !in 0.01..1.0
        ) {

            errorText =
                "Power factor must be between 0.01 and 1.00."

            return
        }

        if (
            maximumDrop == null ||
            maximumDrop <= 0.0
        ) {

            errorText =
                "Maximum voltage drop must be greater than zero."

            return
        }

        val threePhase =
            currentType ==
                "Alternating three-phase"

        ProjectManager.updateSystem(
            voltageV = systemVoltage,
            powerFactor = enteredPF,
            isThreePhase = threePhase
        )

        /*
         * ========================================================
         * IMPORTANT ARCHITECTURE RULE
         * ========================================================
         *
         * CableSizingScreen DOES NOT calculate engineering values.
         *
         * If Design Current is entered, it is passed directly
         * to ProfessionalEngineeringCore.
         *
         * If Load is entered instead, the current is calculated
         * ONLY by ProfessionalEngineeringCore.calculateLoads().
         *
         * No electrical formula exists in this UI.
         * ========================================================
         */

        val current =
            if (
                enteredCurrent != null &&
                enteredCurrent > 0.0
            ) {

                enteredCurrent

            } else {

                if (
                    enteredLoad == null ||
                    enteredLoad <= 0.0
                ) {

                    errorText =
                        "Enter Design Current or Load."

                    return
                }

                val phaseSystem =
                    if (threePhase) {
                        ProfessionalEngineeringCore.PhaseSystem.THREE_PHASE
                    } else {
                        ProfessionalEngineeringCore.PhaseSystem.SINGLE_PHASE
                    }

                val system =
                    ProfessionalEngineeringCore.SystemInput(
                        voltageV = systemVoltage,
                        frequencyHz = 50.0,
                        phaseSystem = phaseSystem,
                        powerFactor = enteredPF
                    )

                val loadInput =
                    ProfessionalEngineeringCore.LoadInput(
                        name = "Cable Design Load",
                        quantity = 1.0,
                        unitPowerKW = enteredLoad,
                        demandFactor = 1.0,
                        powerFactor = enteredPF
                    )

                val loadResult =
                    ProfessionalEngineeringCore.calculateLoads(
                        loads = listOf(loadInput),
                        system = system
                    )

                if (
                    loadResult.trace.status ==
                        EngineeringStatus.FAIL ||
                    loadResult.currentA <= 0.0
                ) {

                    errorText =
                        "Professional Engineering Core could not calculate the design current."

                    return
                }

                loadResult.currentA
            }

        if (
            availableCables.isEmpty()
        ) {

            errorText =
                "No verified cable data is available for this configuration."

            return
        }

        if (
            !automaticSelection &&
            selectedManualSize == null
        ) {

            errorText =
                "Select a verified cable size."

            return
        }

        val phaseSystem =
            if (threePhase) {
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

                    if (automaticSelection) {
                        return catalog
                    }

                    val manualSize =
                        selectedManualSize
                            ?: return emptyList()

                    return catalog.filter {

                        abs(
                            it.sizeMm2 -
                                manualSize
                        ) < 0.000001
                    }
                }
            }

        /*
         * The UI does NOT calculate correction factors.
         *
         * The current ProfessionalEngineeringCore API requires
         * groupingFactor as an input. Until that API is extended
         * to accept circuitsInConduit directly and calculate the
         * engineering factor internally, the UI deliberately
         * does NOT contain a correction-factor table.
         *
         * The Core remains the only place where engineering
         * calculations are permitted.
         */

        val cableInput =
            ProfessionalEngineeringCore.CableDesignInput(

                designCurrentA =
                    current,

                lengthM =
                    cableLength,

                voltageV =
                    systemVoltage,

                powerFactor =
                    enteredPF,

                phaseSystem =
                    phaseSystem,

                conductorMaterial =
                    material,

                insulation =
                    insulation,

                installationMethod =
                    installationMethod,

                numberOfLoadedConductors =
                    if (threePhase) {
                        3
                    } else {
                        2
                    },

                ambientTemperatureC =
                    ambientTemperature.toDouble(),

                groupingFactor =
                    1.0,

                thermalInsulationFactor =
                    1.0,

                soilCorrectionFactor =
                    1.0,

                maximumVoltageDropPercent =
                    maximumDrop,

                maximumParallelRuns =
                    8
            )

        val calculation =
            ProfessionalEngineeringCore.designCable(
                input = cableInput,
                provider = provider
            )

        val selected =
            calculation.selectedCable

        if (selected == null) {

            errorText =
                when (calculation.status) {

                    EngineeringStatus.DATA_REQUIRED ->
                        "DATA REQUIRED: verified engineering data is incomplete."

                    EngineeringStatus.FAIL ->
                        "No cable configuration satisfies the requirements."

                    EngineeringStatus.WARNING ->
                        "WARNING: Professional Engineering Core returned a warning."

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
                    if (automaticSelection) {
                        "AUTOMATIC CABLE SELECTION"
                    } else {
                        "ENGINEER OVERRIDE — VALIDATED"
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
                    "Installation : ${installationMethod.displayName()}"
                )

                appendLine(
                    "Ambient : $ambientTemperature °C"
                )

                appendLine(
                    "Circuits : $circuitsInConduit"
                )

                appendLine()

                appendLine(
                    "Status : ${calculation.status}"
                )

                appendLine(
                    "Source : ${selected.source}"
                )

                appendLine(
                    "Revision : ${selected.revision}"
                )
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            TopEngineeringBar(
                onBack = onBack
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
            ) {

                SideEngineeringMenu()

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(
                                rememberScrollState()
                            )
                            .padding(
                                horizontal = 28.dp,
                                vertical = 16.dp
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    StandardTabs()

                    Text(
                        text =
                            "Conductor sizing and protective device coordination",
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Medium,
                        modifier =
                            Modifier.padding(
                                bottom = 8.dp
                            )
                    )

                    EngineeringDropdown(
                        label = "Current type:",
                        value = currentType,
                        options =
                            listOf(
                                "Direct current",
                                "Alternating single-phase",
                                "Alternating two-phase",
                                "Alternating three-phase"
                            ),
                        onSelected = {

                            currentType = it

                            if (
                                it ==
                                    "Alternating three-phase"
                            ) {
                                ProjectManager.updateSystem(
                                    isThreePhase = true
                                )
                            } else if (
                                it ==
                                    "Alternating single-phase"
                            ) {
                                ProjectManager.updateSystem(
                                    isThreePhase = false
                                )
                            }
                        }
                    )

                    EngineeringInputLine(
                        label = "Voltage:",
                        value = voltage,
                        unit = "V",
                        onValueChange = {
                            voltage = it
                        }
                    )

                    EngineeringInputLine(
                        label = "Load:",
                        value = load,
                        unit = "W",
                        onValueChange = {
                            load = it
                        }
                    )

                    EngineeringInputLine(
                        label = "Design current:",
                        value = designCurrent,
                        unit = "A",
                        onValueChange = {
                            designCurrent = it
                        }
                    )

                    EngineeringInputLine(
                        label = "Power factor:",
                        value = powerFactor,
                        unit = "",
                        onValueChange = {
                            powerFactor = it
                        }
                    )

                    EngineeringInputLine(
                        label = "Line length:",
                        value = lineLength,
                        unit = "m",
                        onValueChange = {
                            lineLength = it
                        }
                    )

                    EngineeringDropdown(
                        label = "Method of installation:",
                        value =
                            installationMethod.displayName(),
                        options =
                            InstallationMethod
                                .values()
                                .map {
                                    it.displayName()
                                },
                        onSelected = { selected ->

                            installationMethod =
                                InstallationMethod
                                    .values()
                                    .first {
                                        it.displayName() ==
                                            selected
                                    }

                            resultText = ""
                            errorText = ""
                            selectedManualSize = null
                        }
                    )

                    EngineeringDropdown(
                        label = "Ambient temperature:",
                        value =
                            "$ambientTemperature °C",
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
                        onSelected = {

                            ambientTemperature =
                                it
                                    .removeSuffix(
                                        " °C"
                                    )
                                    .toInt()

                            resultText = ""
                            errorText = ""
                        }
                    )

                    EngineeringDropdown(
                        label = "Conductor:",
                        value = material.displayName(),
                        options =
                            CableMaterial
                                .values()
                                .map {
                                    it.displayName()
                                },
                        onSelected = { selected ->

                            material =
                                CableMaterial
                                    .values()
                                    .first {
                                        it.displayName() ==
                                            selected
                                    }

                            resultText = ""
                            errorText = ""
                            selectedManualSize = null
                        }
                    )

                    EngineeringDropdown(
                        label = "Insulation:",
                        value = insulation.name,
                        options =
                            InsulationType
                                .values()
                                .map {
                                    it.name
                                },
                        onSelected = {

                            insulation =
                                InsulationType
                                    .valueOf(it)

                            resultText = ""
                            errorText = ""
                            selectedManualSize = null
                        }
                    )

                    EngineeringDropdown(
                        label =
                            "Circuits in the same conduit:",
                        value =
                            circuitsInConduit.toString(),
                        options =
                            (1..8).map {
                                it.toString()
                            },
                        onSelected = {

                            circuitsInConduit =
                                it.toInt()

                            resultText = ""
                            errorText = ""
                        }
                    )

                    EngineeringInputLine(
                        label = "Max voltage drop:",
                        value = maximumVoltageDrop,
                        unit = "%",
                        onValueChange = {
                            maximumVoltageDrop = it
                        }
                    )

                    Text(
                        text = "Cable selection:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier =
                            Modifier.padding(
                                top = 8.dp
                            )
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .clickable {
                                        automaticSelection =
                                            true
                                        selectedManualSize =
                                            null
                                    }
                                    .padding(
                                        end = 24.dp
                                    ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            RadioButton(
                                selected =
                                    automaticSelection,
                                onClick = {
                                    automaticSelection =
                                        true
                                    selectedManualSize =
                                        null
                                }
                            )

                            Text(
                                text = "Automatic"
                            )
                        }

                        Row(
                            modifier =
                                Modifier
                                    .clickable {
                                        automaticSelection =
                                            false
                                    },
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            RadioButton(
                                selected =
                                    !automaticSelection,
                                onClick = {
                                    automaticSelection =
                                        false
                                }
                            )

                            Text(
                                text = "Engineer"
                            )
                        }
                    }

                    if (!automaticSelection) {

                        EngineeringDropdown(
                            label =
                                "Engineer cable size:",
                            value =
                                selectedManualSize
                                    ?.let {
                                        "%.1f mm²"
                                            .format(it)
                                    }
                                    ?: "Select cable size",
                            options =
                                availableCables.map {
                                    "%.1f mm²"
                                        .format(
                                            it.sizeMm2
                                        )
                                },
                            onSelected = { selected ->

                                selectedManualSize =
                                    selected
                                        .removeSuffix(
                                            " mm²"
                                        )
                                        .toDoubleOrNull()

                                resultText = ""
                                errorText = ""
                            }
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Button(
                        onClick = {
                            calculateCable()
                        },
                        modifier =
                            Modifier
                                .align(
                                    Alignment.End
                                )
                                .width(240.dp)
                    ) {

                        Text(
                            text =
                                "CALCULATE",
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    if (
                        errorText.isNotBlank()
                    ) {

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme
                                            .colorScheme
                                            .errorContainer
                                )
                        ) {

                            Text(
                                text =
                                    errorText,
                                modifier =
                                    Modifier.padding(
                                        14.dp
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onErrorContainer,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    if (
                        resultText.isNotBlank()
                    ) {

                        Card(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        16.dp
                                    )
                            ) {

                                Text(
                                    text =
                                        "RESULT",
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            8.dp
                                        )
                                )

                                Text(
                                    text =
                                        resultText
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}


/* ============================================================
   TOP BAR
   ============================================================ */

@Composable
private fun TopEngineeringBar(
    onBack: () -> Unit
) {

    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(
                        horizontal = 16.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "☰",
                fontSize = 25.sp,
                modifier =
                    Modifier.padding(
                        end = 20.dp
                    )
            )

            Text(
                text =
                    "Conductor sizing and protective device coordination",
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Medium,
                modifier =
                    Modifier.weight(1f)
            )

            Text(
                text = "⌕",
                fontSize = 27.sp,
                modifier =
                    Modifier.padding(
                        horizontal = 12.dp
                    )
            )

            Text(
                text = "f(x)",
                fontSize = 17.sp,
                fontWeight =
                    FontWeight.Medium,
                modifier =
                    Modifier.padding(
                        horizontal = 12.dp
                    )
            )

            Text(
                text = "ⓘ",
                fontSize = 20.sp,
                modifier =
                    Modifier.padding(
                        horizontal = 12.dp
                    )
            )

            Text(
                text = "⋮",
                fontSize = 24.sp,
                modifier =
                    Modifier
                        .clickable {
                            onBack()
                        }
                        .padding(
                            start = 12.dp
                        )
            )
        }
    }
}


/* ============================================================
   SIDE MENU
   ============================================================ */

@Composable
private fun SideEngineeringMenu() {

    Column(
        modifier =
            Modifier
                .width(235.dp)
                .fillMaxHeight()
                .background(
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                )
                .verticalScroll(
                    rememberScrollState()
                )
    ) {

        SideMenuItem(
            icon = "⌂",
            title = "Main",
            selected = true
        )

        SideMenuItem(
            icon = "▣",
            title = "Motor",
            selected = false
        )

        SideMenuItem(
            icon = "↔",
            title = "Conversion",
            selected = false
        )

        SideMenuItem(
            icon = "□",
            title = "Resources",
            selected = false
        )

        SideMenuItem(
            icon = "⇆",
            title = "Pinout",
            selected = false
        )

        SideMenuItem(
            icon = "▤",
            title = "Formulas",
            selected = false
        )

        HorizontalDivider()

        SideMenuItem(
            icon = "◉",
            title = "Conductor sizing",
            selected = false
        )

        SideMenuItem(
            icon = "▦",
            title =
                "Conductor sizing and protective device coordination",
            selected = true
        )

        SideMenuItem(
            icon = "ΔV",
            title = "Calculation of voltage drop",
            selected = false
        )

        SideMenuItem(
            icon = "A",
            title = "Calculation of current",
            selected = false
        )

        SideMenuItem(
            icon = "V",
            title = "Calculation of voltage",
            selected = false
        )

        SideMenuItem(
            icon = "W",
            title = "Calculation of active power",
            selected = false
        )

        SideMenuItem(
            icon = "VA",
            title = "Calculation of apparent power",
            selected = false
        )

        SideMenuItem(
            icon = "var",
            title = "Calculation of reactive power",
            selected = false
        )

        SideMenuItem(
            icon = "cosφ",
            title = "Calculation of power factor",
            selected = false
        )

        SideMenuItem(
            icon = "Ω",
            title = "Calculation of resistance",
            selected = false
        )

        SideMenuItem(
            icon = "Z",
            title = "Calculation of impedance",
            selected = false
        )
    }
}


@Composable
private fun SideMenuItem(
    icon: String,
    title: String,
    selected: Boolean
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    if (selected) {
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    }
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = icon,
            modifier =
                Modifier.width(38.dp),
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text = title,
            fontSize = 14.sp,
            modifier =
                Modifier.weight(1f)
        )
    }
}


/* ============================================================
   STANDARD TABS
   ============================================================ */

@Composable
private fun StandardTabs() {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 8.dp
                ),
        horizontalArrangement =
            Arrangement.SpaceEvenly
    ) {

        listOf(
            "IEC",
            "CEI",
            "NEC",
            "CEC"
        ).forEachIndexed { index, standard ->

            Text(
                text = standard,
                fontSize = 15.sp,
                fontWeight =
                    if (index == 0) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                modifier =
                    Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 8.dp
                    )
            )
        }
    }

    HorizontalDivider()
}


/* ============================================================
   ENGINEERING INPUT
   ============================================================ */

@Composable
private fun EngineeringInputLine(
    label: String,
    value: String,
    unit: String,
    onValueChange: (String) -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = label,
            modifier =
                Modifier.width(250.dp),
            fontSize = 14.sp
        )

        OutlinedTextField(
            value = value,
            onValueChange =
                onValueChange,
            singleLine = true,
            modifier =
                Modifier.weight(1f),
            shape =
                RoundedCornerShape(0.dp)
        )

        if (unit.isNotBlank()) {

            Text(
                text = unit,
                modifier =
                    Modifier
                        .width(55.dp)
                        .padding(
                            start = 10.dp
                        )
            )
        }
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

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = label,
            modifier =
                Modifier.width(250.dp),
            fontSize = 14.sp
        )

        Box(
            modifier =
                Modifier.weight(1f)
        ) {

            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                0.dp
                            )
                        )
                        .clickable {
                            expanded = !expanded
                        },
                tonalElevation = 0.dp
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 12.dp,
                                vertical = 12.dp
                            ),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = value,
                        fontSize = 14.sp
                    )

                    Text(
                        text =
                            if (expanded) {
                                "▲"
                            } else {
                                "▼"
                            },
                        fontSize = 11.sp
                    )
                }
            }

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
   DISPLAY HELPERS
   ============================================================ */

private fun CableMaterial.displayName(): String {

    return when (this) {

        CableMaterial.COPPER ->
            "Copper"

        CableMaterial.ALUMINIUM ->
            "Aluminium"
    }
}


private fun InstallationMethod.displayName(): String {

    return when (this) {

        InstallationMethod.CONDUIT ->
            "1 - A1 / Conduit"

        InstallationMethod.TRUNKING ->
            "Trunking"

        InstallationMethod.CABLE_TRAY ->
            "Cable tray"

        InstallationMethod.CABLE_LADDER ->
            "Cable ladder"

        InstallationMethod.FREE_AIR ->
            "Free air"

        InstallationMethod.DIRECT_BURIED ->
            "Direct buried"

        InstallationMethod.DUCT ->
            "Duct"
    }
}
