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

@Composable
fun VoltageDropScreen(
    onBack: () -> Unit = {}
) {
    val project = ProjectManager.calculation

    var current by remember {
        mutableStateOf(
            if (project.designCurrentA > 0.0)
                "%.2f".format(project.designCurrentA)
            else ""
        )
    }

    var length by remember {
        mutableStateOf(
            if (project.cableLengthM > 0.0)
                "%.1f".format(project.cableLengthM)
            else "30"
        )
    }

    var voltage by remember {
        mutableStateOf(
            project.voltageV.toString()
        )
    }

    var powerFactor by remember {
        mutableStateOf(
            project.powerFactor.toString()
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
        mutableStateOf(
            InstallationMethod.CABLE_TRAY
        )
    }

    var selectedCable by remember {
        mutableStateOf<
            ProfessionalEngineeringCore.CableData?
            >(null)
    }

    var showCableMenu by remember {
        mutableStateOf(false)
    }

    var result by remember {
        mutableStateOf("")
    }

    var hasCalculated by remember {
        mutableStateOf(false)
    }

    fun calculateCable(
        requestedSize: Double? = null
    ) {

        val i =
            current.toDoubleOrNull() ?: 0.0

        val l =
            length.toDoubleOrNull() ?: 0.0

        val v =
            voltage.toDoubleOrNull() ?: 0.0

        val pf =
            powerFactor
                .toDoubleOrNull()
                ?.coerceIn(0.01, 1.0)
                ?: 0.9

        if (
            i <= 0.0 ||
            l <= 0.0 ||
            v <= 0.0
        ) {
            result =
                "Please enter valid design values."

            hasCalculated = false
            return
        }

        ProjectManager.updateSystem(
            voltageV = v,
            powerFactor = pf,
            isThreePhase = isThreePhase
        )

        val catalogCables =
            EngineeringCatalogRepository
                .getCableData(
                    material = material,
                    insulation = insulation,
                    installationMethod =
                        installationMethod
                )

        if (catalogCables.isEmpty()) {

            selectedCable = null

            result =
                """
                CABLE DESIGN

                Status:
                DATA_REQUIRED

                No verified cable data is available
                for the selected material,
                insulation and installation method.
                """.trimIndent()

            hasCalculated = true
            return
        }

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
                ): List<
                        ProfessionalEngineeringCore.CableData
                        > {

                    return if (
                        requestedSize == null
                    ) {
                        catalogCables
                    } else {
                        catalogCables.filter {
                            kotlin.math.abs(
                                it.sizeMm2 -
                                        requestedSize
                            ) < 0.0001
                        }
                    }
                }
            }

        val design =
            ProfessionalEngineeringCore.designCable(
                input =
                    ProfessionalEngineeringCore.CableDesignInput(
                        designCurrentA = i,
                        lengthM = l,
                        voltageV = v,
                        powerFactor = pf,

                        phaseSystem =
                            if (isThreePhase) {
                                ProfessionalEngineeringCore
                                    .PhaseSystem
                                    .THREE_PHASE
                            } else {
                                ProfessionalEngineeringCore
                                    .PhaseSystem
                                    .SINGLE_PHASE
                            },

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
                            5.0,

                        maximumParallelRuns =
                            8
                    ),
                provider = provider
            )

        selectedCable =
            design.selectedCable

        if (
            design.status ==
                EngineeringStatus.PASS &&
            design.selectedCable != null
        ) {

            val cable =
                design.selectedCable!!

            ProjectManager.setCableResult(
                cableSizeMm2 =
                    cable.sizeMm2,

                cableAmpacityA =
                    design.totalAmpacityA,

                cableLengthM =
                    l,

                voltageDropV =
                    design.voltageDropV,

                voltageDropPercent =
                    design.voltageDropPercent
            )
        }

        result =
            buildString {

                appendLine(
                    if (requestedSize == null)
                        "AUTOMATIC CABLE SELECTION"
                    else
                        "MANUAL CABLE CHECK"
                )

                appendLine()

                appendLine(
                    "Design Current: %.2f A"
                        .format(i)
                )

                appendLine(
                    "Length: %.1f m"
                        .format(l)
                )

                appendLine(
                    "Voltage: %.0f V"
                        .format(v)
                )

                appendLine(
                    "Power Factor: %.2f"
                        .format(pf)
                )

                appendLine(
                    "System: ${
                        if (isThreePhase)
                            "3 Phase"
                        else
                            "1 Phase"
                    }"
                )

                appendLine()

                if (
                    design.selectedCable != null
                ) {

                    val cable =
                        design.selectedCable!!

                    appendLine(
                        "Selected Cable:"
                    )

                    appendLine(
                        "Size: %.1f mm²"
                            .format(
                                cable.sizeMm2
                            )
                    )

                    appendLine(
                        "Ampacity / Run: %.2f A"
                            .format(
                                design
                                    .correctedAmpacityPerRunA
                            )
                    )

                    appendLine(
                        "Parallel Runs: ${
                            design.parallelRuns
                        }"
                    )

                    appendLine(
                        "Total Ampacity: %.2f A"
                            .format(
                                design.totalAmpacityA
                            )
                    )

                    appendLine()

                    appendLine(
                        "Voltage Drop: %.2f V"
                            .format(
                                design.voltageDropV
                            )
                    )

                    appendLine(
                        "Voltage Drop: %.2f %%"
                            .format(
                                design.voltageDropPercent
                            )
                    )

                    appendLine()

                    appendLine(
                        "Source: ${
                            cable.source
                        }"
                    )

                    appendLine(
                        "Revision: ${
                            cable.revision
                        }"
                    )

                } else {

                    appendLine(
                        "No suitable cable found."
                    )
                }

                appendLine()

                appendLine(
                    "FINAL STATUS: ${
                        design.status
                    }"
                )

                design.checks.forEach { check ->

                    appendLine()

                    appendLine(
                        "${check.name}: ${
                            check.status
                        }"
                    )

                    appendLine(
                        check.message
                    )
                }
            }

        hasCalculated = true
    }

    val availableCables =
        remember(
            material,
            insulation,
            installationMethod
        ) {
            EngineeringCatalogRepository
                .getCableData(
                    material,
                    insulation,
                    installationMethod
                )
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
            text =
                "Voltage Drop & Cable Sizing",
            style =
                MaterialTheme.typography.headlineSmall
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

        OutlinedTextField(
            value = current,
            onValueChange = {
                current = it
            },
            label = {
                Text("Design Current (A)")
            },
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
            modifier =
                Modifier.fillMaxWidth()
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
                Text("3 Phase")
            }

            Button(
                onClick = {
                    isThreePhase = false
                },
                modifier =
                    Modifier.weight(1f)
            ) {
                Text("1 Phase")
            }
        }

        HorizontalDivider()

        Text(
            "Cable Engineering Data",
            style =
                MaterialTheme.typography.titleMedium
        )

        OutlinedButton(
            onClick = {
                material =
                    if (
                        material ==
                        CableMaterial.COPPER
                    ) {
                        CableMaterial.ALUMINIUM
                    } else {
                        CableMaterial.COPPER
                    }
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                "Material: $material"
            )
        }

        OutlinedButton(
            onClick = {
                insulation =
                    when (insulation) {
                        InsulationType.PVC ->
                            InsulationType.XLPE

                        InsulationType.XLPE ->
                            InsulationType.EPR

                        InsulationType.EPR ->
                            InsulationType.PVC
                    }
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                "Insulation: $insulation"
            )
        }

        OutlinedButton(
            onClick = {
                installationMethod =
                    when (installationMethod) {
                        InstallationMethod.CONDUIT ->
                            InstallationMethod.TRUNKING

                        InstallationMethod.TRUNKING ->
                            InstallationMethod.CABLE_TRAY

                        InstallationMethod.CABLE_TRAY ->
                            InstallationMethod.CABLE_LADDER

                        InstallationMethod.CABLE_LADDER ->
                            InstallationMethod.FREE_AIR

                        InstallationMethod.FREE_AIR ->
                            InstallationMethod.DIRECT_BURIED

                        InstallationMethod.DIRECT_BURIED ->
                            InstallationMethod.DUCT

                        InstallationMethod.DUCT ->
                            InstallationMethod.CONDUIT
                    }
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text(
                "Installation: $installationMethod"
            )
        }

        Button(
            onClick = {
                selectedCable = null
                calculateCable()
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Text("AUTO SELECT CABLE")
        }

        if (
            hasCalculated &&
            selectedCable != null
        ) {

            HorizontalDivider()

            Text(
                "Recommended Cable: %.1f mm²"
                    .format(
                        selectedCable!!.sizeMm2
                    ),
                style =
                    MaterialTheme.typography.titleMedium
            )

            OutlinedButton(
                onClick = {
                    showCableMenu =
                        !showCableMenu
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("CHANGE CABLE")
            }

            DropdownMenu(
                expanded =
                    showCableMenu,
                onDismissRequest = {
                    showCableMenu = false
                }
            ) {

                if (
                    availableCables.isEmpty()
                ) {

                    DropdownMenuItem(
                        text = {
                            Text(
                                "No verified cable data"
                            )
                        },
                        onClick = {
                            showCableMenu = false
                        }
                    )

                } else {

                    availableCables.forEach { cable ->

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "%.1f mm² | %.0f A"
                                        .format(
                                            cable.sizeMm2,
                                            cable.baseAmpacityA
                                        )
                                )
                            },
                            onClick = {

                                selectedCable =
                                    cable

                                showCableMenu =
                                    false

                                calculateCable(
                                    requestedSize =
                                        cable.sizeMm2
                                )
                            }
                        )
                    }
                }
            }
        }

        if (result.isNotEmpty()) {

            HorizontalDivider()

            Text(
                text = result,
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        HorizontalDivider()

        Text(
            "Current Project Result",
            style =
                MaterialTheme.typography.titleMedium
        )

        Text(
            "Design Current: %.2f A"
                .format(
                    ProjectManager.calculation
                        .designCurrentA
                )
        )

        Text(
            "Selected Cable: %.1f mm²"
                .format(
                    ProjectManager.calculation
                        .cableSizeMm2
                )
        )

        Text(
            "Cable Ampacity: %.2f A"
                .format(
                    ProjectManager.calculation
                        .cableAmpacityA
                )
        )

        Text(
            "Cable Length: %.1f m"
                .format(
                    ProjectManager.calculation
                        .cableLengthM
                )
        )

        Text(
            "Voltage Drop: %.2f V"
                .format(
                    ProjectManager.calculation
                        .voltageDropV
                )
        )

        Text(
            "Voltage Drop: %.2f %%"
                .format(
                    ProjectManager.calculation
                        .voltageDropPercent
                )
        )

        Text(
            "Design Status: ${
                ProjectManager.calculation
                    .designStatus
            }"
        )

        Spacer(
            modifier = Modifier.height(10.dp)
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
