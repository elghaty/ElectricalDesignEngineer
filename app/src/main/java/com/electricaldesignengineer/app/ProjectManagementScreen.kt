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
import androidx.compose.material3.Card
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
fun ProjectManagementScreen(
    onBack: () -> Unit = {}
) {

    var projectName by remember {
        mutableStateOf(ProjectManager.calculation.projectName)
    }

    var clientName by remember {
        mutableStateOf(ProjectManager.calculation.clientName)
    }

    var projectLocation by remember {
        mutableStateOf(ProjectManager.calculation.projectLocation)
    }

    var engineerName by remember {
        mutableStateOf(ProjectManager.calculation.engineerName)
    }

    var message by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Project Management",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Create and manage the active electrical design project.",
            style = MaterialTheme.typography.bodyMedium
        )

        HorizontalDivider()

        Text(
            text = "Project Information",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = projectName,
            onValueChange = {
                projectName = it
            },
            label = {
                Text("Project Name")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = clientName,
            onValueChange = {
                clientName = it
            },
            label = {
                Text("Client Name")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = projectLocation,
            onValueChange = {
                projectLocation = it
            },
            label = {
                Text("Project Location")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = engineerName,
            onValueChange = {
                engineerName = it
            },
            label = {
                Text("Engineer Name")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Button(
            onClick = {

                if (projectName.isBlank()) {

                    message = "ERROR: Project Name is required."

                } else {

                    ProjectManager.updateProjectInfo(
                        projectName = projectName.trim(),
                        clientName = clientName.trim(),
                        projectLocation = projectLocation.trim(),
                        engineerName = engineerName.trim()
                    )

                    message = "Project information saved successfully."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Project Information")
        }

        OutlinedButton(
            onClick = {

                ProjectManager.startNewProject(
                    projectName = projectName.trim(),
                    clientName = clientName.trim(),
                    projectLocation = projectLocation.trim(),
                    engineerName = engineerName.trim()
                )

                message = "New project created successfully."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start New Project")
        }

        OutlinedButton(
            onClick = {

                ProjectManager.clearLoads()

                message = "All project loads have been cleared."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear All Loads")
        }

        if (message.isNotBlank()) {

            HorizontalDivider()

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        HorizontalDivider()

        Text(
            text = "Active Project",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Text(
                    text = ProjectManager.calculation.projectName.ifBlank {
                        "Unnamed Project"
                    },
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "Client: ${
                        ProjectManager.calculation.clientName.ifBlank {
                            "-"
                        }
                    }"
                )

                Text(
                    text = "Location: ${
                        ProjectManager.calculation.projectLocation.ifBlank {
                            "-"
                        }
                    }"
                )

                Text(
                    text = "Engineer: ${
                        ProjectManager.calculation.engineerName.ifBlank {
                            "-"
                        }
                    }"
                )

                HorizontalDivider()

                Text(
                    text = "Connected Load: %.2f kW".format(
                        ProjectManager.calculation.connectedKW
                    )
                )

                Text(
                    text = "Demand Load: %.2f kW".format(
                        ProjectManager.calculation.demandKW
                    )
                )

                Text(
                    text = "Total Load: %.2f kVA".format(
                        ProjectManager.calculation.totalKVA
                    )
                )

                Text(
                    text = "Design Current: %.2f A".format(
                        ProjectManager.calculation.designCurrentA
                    )
                )

                Text(
                    text = "Voltage: %.0f V".format(
                        ProjectManager.calculation.voltageV
                    )
                )

                Text(
                    text = "Power Factor: %.3f".format(
                        ProjectManager.calculation.powerFactor
                    )
                )

                Text(
                    text = "Phase System: ${
                        if (ProjectManager.calculation.isThreePhase) {
                            "3 Phase"
                        } else {
                            "1 Phase"
                        }
                    }"
                )

                Text(
                    text = "Number of Loads: ${
                        ProjectManager.loads.size
                    }"
                )

                Text(
                    text = "Status: ${
                        ProjectManager.calculation.designStatus
                    }"
                )
            }
        }

        HorizontalDivider()

        Text(
            text = "Project Loads",
            style = MaterialTheme.typography.titleMedium
        )

        if (ProjectManager.loads.isEmpty()) {

            Text(
                text = "No loads have been added to this project yet.",
                style = MaterialTheme.typography.bodyMedium
            )

        } else {

            ProjectManager.loads.forEachIndexed { index, load ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Text(
                            text = "${index + 1}. ${load.name}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "Quantity: ${load.quantity}"
                        )

                        Text(
                            text = "Power / Load: %.2f kW".format(
                                load.powerKW
                            )
                        )

                        Text(
                            text = "Demand Factor: %.2f".format(
                                load.demandFactor
                            )
                        )

                        Text(
                            text = "Power Factor: %.2f".format(
                                load.powerFactor
                            )
                        )

                        Text(
                            text = "Connected: %.2f kW".format(
                                load.quantity * load.powerKW
                            )
                        )

                        Text(
                            text = "Demand: %.2f kW".format(
                                load.quantity *
                                        load.powerKW *
                                        load.demandFactor
                            )
                        )

                        OutlinedButton(
                            onClick = {

                                ProjectManager.removeLoad(load)

                                ProjectManager.calculateFromLoads()

                                message =
                                    "Load removed and project recalculated."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Load")
                        }
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = {

                ProjectManager.calculateFromLoads()

                message =
                    "Project loads recalculated successfully."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Recalculate Project")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
