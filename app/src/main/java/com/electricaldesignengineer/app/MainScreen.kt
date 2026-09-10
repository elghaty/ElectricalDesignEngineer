package com.electricaldesignengineer.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DesignModule(
    val title: String,
    val subtitle: String,
    val category: String
)

private val designModules = listOf(
    DesignModule(
        "Project Management",
        "Create and manage engineering projects",
        "PROJECT"
    ),
    DesignModule(
        "Single Line Diagram",
        "Build and review electrical SLD",
        "DESIGN"
    ),
    DesignModule(
        "Load Calculation",
        "Calculate connected and demand loads",
        "LOADS"
    ),
    DesignModule(
        "Load Schedule",
        "Prepare professional load schedules",
        "LOADS"
    ),
    DesignModule(
        "Cable Sizing",
        "Select cable by current, voltage drop and fault duty",
        "DESIGN"
    ),
    DesignModule(
        "Voltage Drop",
        "Verify voltage-drop compliance",
        "VERIFICATION"
    ),
    DesignModule(
        "Short Circuit",
        "Calculate prospective fault current",
        "PROTECTION"
    ),
    DesignModule(
        "Breaker Selection",
        "Select protective devices and ratings",
        "PROTECTION"
    ),
    DesignModule(
        "Transformer",
        "Transformer rating and engineering checks",
        "POWER"
    ),
    DesignModule(
        "Generator",
        "Generator sizing and loading",
        "POWER"
    ),
    DesignModule(
        "Power Factor",
        "Calculate capacitor-bank requirements",
        "POWER QUALITY"
    ),
    DesignModule(
        "Earthing",
        "Earthing and protective bonding calculations",
        "SAFETY"
    )
)

@Composable
fun MainScreen(
    onModuleSelected: (DesignModule) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Header()

            Spacer(modifier = Modifier.height(18.dp))

            HeroCard(
                onStartDesign = {
                    onModuleSelected(
                        designModules.first {
                            it.title == "Project Management"
                        }
                    )
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            SectionTitle(
                title = "Engineering Design",
                subtitle = "Professional electrical design tools"
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 2.dp,
                    bottom = 24.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = designModules,
                    key = { it.title }
                ) { module ->
                    DesignModuleCard(
                        module = module,
                        onClick = {
                            onModuleSelected(module)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ED",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "ELECTRICAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Design Engineer",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(
                    horizontal = 12.dp,
                    vertical = 7.dp
                )
        ) {
            Text(
                text = "IEC",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun HeroCard(
    onStartDesign: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "PROFESSIONAL WORKSPACE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Electrical Design",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Design, calculate and verify your electrical system from one engineering workspace.",
                modifier = Modifier.padding(top = 6.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip("AUTO DESIGN")
                StatusChip("IEC")
                StatusChip("ENGINEERING CORE")
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onStartDesign,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "START NEW DESIGN",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            )
            .padding(
                horizontal = 9.dp,
                vertical = 6.dp
            )
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(
                    alpha = 0.65f
                )
            )
        }

        Text(
            text = "${designModules.size} TOOLS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DesignModuleCard(
    module: DesignModule,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.12f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = module.title
                            .split(" ")
                            .take(2)
                            .joinToString("") {
                                it.firstOrNull()?.uppercase() ?: ""
                            },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = module.category,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = module.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = module.subtitle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "OPEN  →",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
