package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.electricaldesignengineer.app.data.AppDatabase
import com.electricaldesignengineer.app.data.ProjectEntity
import kotlinx.coroutines.launch

@Composable
fun ProjectManagementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val database = remember {
        AppDatabase.getDatabase(context)
    }

    val projectDao = database.projectDao()

    val projects by projectDao
        .getAllProjects()
        .collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    var projectName by remember {
        mutableStateOf("")
    }

    var clientName by remember {
        mutableStateOf("")
    }

    var projectLocation by remember {
        mutableStateOf("")
    }

    var engineerName by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Project Management"
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        OutlinedTextField(
            value = projectName,
            onValueChange = {
                projectName = it
            },
            label = {
                Text("Project Name")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = clientName,
            onValueChange = {
                clientName = it
            },
            label = {
                Text("Client Name")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = projectLocation,
            onValueChange = {
                projectLocation = it
            },
            label = {
                Text("Project Location")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = engineerName,
            onValueChange = {
                engineerName = it
            },
            label = {
                Text("Engineer Name")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = {

                if (projectName.isNotBlank()) {

                    val project = ProjectEntity(
                        name = projectName,
                        clientName = clientName,
                        projectLocation = projectLocation,
                        engineerName = engineerName,
                        createdDate = System.currentTimeMillis().toString()
                    )

                    scope.launch {
                        projectDao.insertProject(project)
                    }

                    projectName = ""
                    clientName = ""
                    projectLocation = ""
                    engineerName = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Project")
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Saved Projects"
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(
                items = projects,
                key = {
                    it.id
                }
            ) { project ->

                ProjectCard(
                    project = project,
                    onDelete = {

                        scope.launch {
                            projectDao.deleteProject(project)
                        }
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectEntity,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = project.name
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Client: ${project.clientName}"
            )

            Text(
                text = "Location: ${project.projectLocation}"
            )

            Text(
                text = "Engineer: ${project.engineerName}"
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                Button(
                    onClick = onDelete
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
