package com.electricaldesignengineer.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class DesignModule(
    val title: String,
    val subtitle: String
)

private val modules = listOf(
    DesignModule("Load Calculation", "Calculate electrical loads"),
    DesignModule("Cable Sizing", "Select cable size"),
    DesignModule("Voltage Drop", "Check voltage drop"),
    DesignModule("Short Circuit", "Calculate fault current"),
    DesignModule("Breaker Selection", "Select protection"),
    DesignModule("Transformer", "Transformer sizing"),
    DesignModule("Generator", "Generator sizing"),
    DesignModule("Power Factor", "Capacitor bank"),
    DesignModule("Earthing", "Earthing calculations"),
    DesignModule("Load Schedule", "Create load schedule")
)

@Composable
fun MainScreen(
    onModuleSelected: (DesignModule) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Electrical Design Engineer",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Electrical Design & Calculation",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(modules) { module ->

                Button(
                    onClick = {
                        onModuleSelected(module)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(2.dp)
                ) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(text = module.title)
                        Text(
                            text = module.subtitle,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
