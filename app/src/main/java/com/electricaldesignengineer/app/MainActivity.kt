package com.electricaldesignengineer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ElectricalDesignTheme {
                Surface {
                    ElectricalDesignApp()
                }
            }
        }
    }
}

@Composable
private fun ElectricalDesignTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF55D6FF),
            onPrimary = Color(0xFF002B38),
            secondary = Color(0xFF68E6B2),
            onSecondary = Color(0xFF00382A),
            tertiary = Color(0xFFFFC857),
            onTertiary = Color(0xFF392900),
            background = Color(0xFF07111F),
            onBackground = Color(0xFFE8F1F7),
            surface = Color(0xFF0D1B2A),
            onSurface = Color(0xFFE8F1F7),
            surfaceVariant = Color(0xFF14263A),
            onSurfaceVariant = Color(0xFFB6C6D4),
            outline = Color(0xFF30475A)
        ),
        content = content
    )
}

@Composable
fun ElectricalDesignApp() {

    var selectedModule by remember {
        mutableStateOf<DesignModule?>(null)
    }

    when (selectedModule?.title) {

        "Project Management" -> {
            ProjectManagementScreen(
                onBack = {
                    selectedModule = null
                }
            )
        }

        "Single Line Diagram" -> {
            SingleLineDiagramScreen {
                selectedModule = null
            }
        }

        "Load Calculation" -> {
            LoadCalculationScreen {
                selectedModule = null
            }
        }

        "Load Schedule" -> {
            LoadScheduleScreen {
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

        else -> {
            MainScreen { module ->
                selectedModule = module
            }
        }
    }
}
