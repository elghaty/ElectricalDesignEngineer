package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProjectManagementScreen(
    onBack: () -> Unit = {}
) {

    var projectName by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var projectLocation by remember { mutableStateOf("") }
    var engineerName by remember { mutableStateOf("") }

    var projects by remember {
        mutableStateOf(listOf<ElectricalProject>())
    }

    var message by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Text("Project Management")

        OutlinedTextField(
            value = projectName,
            onValueChange = { projectName = it },
            label = { Text("Project Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = clientName,
            onValueChange = { clientName = it },
            label = { Text("Client Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = projectLocation,
            onValueChange = { projectLocation = it },
            label = { Text("Project Location") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = engineerName,
            onValueChange = { engineerName = it },
            label = { Text("Engineer Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                if (
                    projectName.isBlank() ||
                    clientName.isBlank()
                ) {
                    message = "Project name and client name are required."
                    return@Button
                }

                val newProject = ElectricalProject(
                    id = System.currentTimeMillis(),
                    name = projectName,
                    clientName = clientName,
                    projectLocation = projectLocation,
                    engineerName = engineerName,
                    createdDate = System.currentTimeMillis().toString()
                )

                projects = projects + newProject

                projectName = ""
                clientName = ""
                projectLocation = ""
                engineerName = ""

                message = "Project created successfully."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Project")
        }

        if (message.isNotEmpty()) {
            Text(message)
        }

        Text(
            text = "Projects: ${projects.size}"
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(
                items = projects,
                key = { it.id }
            ) { project ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {

                    Text(
                        text = project.name
                    )

                    Text(
                        text = "Client: ${project.clientName}"
                    )

                    if (project.projectLocation.isNotBlank()) {
                        Text(
                            text = "Location: ${project.projectLocation}"
                        )
                    }

                    if (project.engineerName.isNotBlank()) {
                        Text(
                            text = "Engineer: ${project.engineerName}"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Button(
                            onClick = {
                                message =
                                    "Project selected: ${project.name}"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open")
                        }

                        Button(
                            onClick = {
                                projects =
                                    projects.filter {
                                        it.id != project.id
                                    }

                                message = "Project deleted."
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}
