package com.electricaldesignengineer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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

        "Project Management" -> {
            ProjectManagementScreen {
                selectedModule = null
            }
        }

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

        "Power Factor" -> {
            PowerFactorCorrectionScreen {
                selectedModule = null
            }
        }

        "Earthing" -> {
            EarthingScreen {
                selectedModule = null
            }
        }

        "Load Schedule" -> {
            LoadScheduleScreen {
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
