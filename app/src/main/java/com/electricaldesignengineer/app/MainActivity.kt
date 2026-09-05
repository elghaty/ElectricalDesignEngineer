package com.electricaldesignengineer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface {
                    ElectricalDesignApp()
                }
            }
        }
    }
}

@Composable
fun ElectricalDesignApp() {

    var selectedModule by remember {
        mutableStateOf<DesignModule?>(null)
    }

    when (selectedModule?.title) {

        "Load Calculation" -> {
            LoadCalculationScreen {
                selectedModule = null
            }
        }

        "Cable Sizing" -> {
            CableSizingScreen {
                selectedModule = null
            }
        }

        "Voltage Drop" -> {
            VoltageDropScreen {
                selectedModule = null
            }
        }

        "Short Circuit" -> {
            ShortCircuitScreen {
                selectedModule = null
            }
        }

        "Breaker Selection" -> {
            BreakerSelectionScreen {
                selectedModule = null
            }
        }

        "Transformer" -> {
            TransformerSizingScreen {
                selectedModule = null
            }
        }

        "Generator" -> {
            GeneratorSizingScreen {
                selectedModule = null
            }
        }

        else -> {
            MainScreen { module ->
                selectedModule = module
            }
        }
    }
}
        MainScreen { module ->
            selectedModule = module
        }
    }
}
