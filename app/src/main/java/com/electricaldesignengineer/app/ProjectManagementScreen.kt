package com.electricaldesignengineer.app

import android.app.Application
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
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.room.*
import com.electricaldesignengineer.app.data.AppDatabase
import com.electricaldesignengineer.app.data.ProjectDao
import com.electricaldesignengineer.app.data.ProjectEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectManagementViewModel(application: Application) :
    AndroidViewModel(application) {

    private val dao: ProjectDao =
        AppDatabase.getDatabase(application).projectDao()

    val projects = dao.getAllProjects()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun addProject(
        name: String,
        client: String,
        location: String,
        engineer: String
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {

            val project = ProjectEntity(
                name = name,
                clientName = client,
                projectLocation = location,
                engineerName = engineer,
                createdDate = SimpleDateFormat(
                    "yyyy-MM-dd HH:mm",
                    Locale.getDefault()
                ).format(Date()),

                connectedKW = ElectricalCalculator.connectedKW,
                demandKW = ElectricalCalculator.demandKW,
                totalKVA = ElectricalCalculator.totalKVA,
                designCurrentA = ElectricalCalculator.designCurrentA,
                voltageV = ElectricalCalculator.voltageV,
                powerFactor = ElectricalCalculator.powerFactor,
                cableSizeMm2 = ElectricalCalculator.cableSizeMm2,
                voltageDropPercent =
                    ElectricalCalculator.voltageDropPercent,
                shortCircuitKA =
                    ElectricalCalculator.shortCircuitKA,
                breakerRatingA =
                    ElectricalCalculator.breakerRatingA,
                breakerIcuKA =
                    ElectricalCalculator.breakerIcuKA,
                transformerKVA =
                    ElectricalCalculator.transformerKVA,
                generatorKVA =
                    ElectricalCalculator.generatorKVA,
                capacitorKVAR =
                    ElectricalCalculator.capacitorKVAR
            )

            dao.insertProject(project)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            dao.deleteProject(project)
        }
    }
}

@Composable
fun ProjectManagementScreen(
    onBack: () -> Unit,
    viewModel: ProjectManagementViewModel = viewModel()
) {

    val projects by viewModel.projects.collectAsState()

    var name by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var engineer by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Project Management",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Project Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = client,
            onValueChange = { client = it },
            label = { Text("Client Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Project Location") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = engineer,
            onValueChange = { engineer = it },
            label = { Text("Engineer Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = {
                    viewModel.addProject(
                        name,
                        client,
                        location,
                        engineer
                    )

                    name = ""
                    client = ""
                    location = ""
                    engineer = ""
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Project")
            }

            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Divider()

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Saved Projects",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {

            items(
                items = projects,
                key = { it.id }
            ) { project ->

                ProjectCard(
                    project = project,
                    onDelete = {
                        viewModel.deleteProject(project)
                    }
                )
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: ProjectEntity,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Text(
                text = project.name,
                style = MaterialTheme.typography.titleMedium
            )

            Text("Client: ${project.clientName}")
            Text("Location: ${project.projectLocation}")
            Text("Engineer: ${project.engineerName}")
            Text("Created: ${project.createdDate}")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Design Results",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                "Connected Load: %.2f kW"
                    .format(project.connectedKW)
            )

            Text(
                "Demand Load: %.2f kW"
                    .format(project.demandKW)
            )

            Text(
                "Total: %.2f kVA"
                    .format(project.totalKVA)
            )

            Text(
                "Design Current: %.2f A"
                    .format(project.designCurrentA)
            )

            Text(
                "Cable: %.1f mm²"
                    .format(project.cableSizeMm2)
            )

            Text(
                "Voltage Drop: %.2f %%"
                    .format(project.voltageDropPercent)
            )

            Text(
                "Short Circuit: %.2f kA"
                    .format(project.shortCircuitKA)
            )

            Text(
                "Breaker: ${project.breakerRatingA} A"
            )

            Text(
                "Breaker Icu: %.1f kA"
                    .format(project.breakerIcuKA)
            )

            Text(
                "Transformer: %.0f kVA"
                    .format(project.transformerKVA)
            )

            Text(
                "Generator: %.0f kVA"
                    .format(project.generatorKVA)
            )

            Text(
                "Capacitor Bank: %.1f kVAR"
                    .format(project.capacitorKVAR)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDelete
            ) {
                Text("Delete")
            }
        }
    }
}
