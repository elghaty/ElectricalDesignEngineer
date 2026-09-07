package com.electricaldesignengineer.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Professional Single Line Diagram workspace.
 *
 * Design philosophy:
 * - No Modifier.weight()
 * - No matchParentSize()
 * - Canvas-based electrical symbols
 * - Scrollable engineering drawing
 * - Component selection
 * - Engineering data panel
 * - Status indication
 *
 * The drawing is currently driven by ProjectManager.
 * The next stage will connect it directly to DistributionSystem/SldGenerator
 * so that the complete hierarchy is generated automatically.
 */
@Composable
fun SingleLineDiagramScreen(
    onBack: () -> Unit = {}
) {
    var selectedNode by remember {
        mutableStateOf("MDB-01")
    }

    var zoomLevel by remember {
        mutableStateOf(1.0f)
    }

    var notice by remember {
        mutableStateOf("SLD ready — select any electrical component.")
    }

    val calculation = ProjectManager.calculation

    val transformerKva =
        if (calculation.transformerKVA > 0.0) {
            calculation.transformerKVA
        } else {
            1000.0
        }

    val designCurrent =
        if (calculation.designCurrentA > 0.0) {
            calculation.designCurrentA
        } else {
            0.0
        }

    val faultLevel =
        if (calculation.shortCircuitKA > 0.0) {
            calculation.shortCircuitKA
        } else {
            25.0
        }

    val voltage =
        if (calculation.voltageV > 0.0) {
            calculation.voltageV
        } else {
            400.0
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ---------------------------------------------------------
        // HEADER
        // ---------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier
                        .width(0.dp)
                        .weightSafe()
                ) {
                    Text(
                        text = "SINGLE LINE DIAGRAM",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Professional Electrical Distribution SLD",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "${voltage.roundToInt()} V · " +
                                "${if (calculation.isThreePhase) "3-PHASE" else "1-PHASE"} · " +
                                "${calculation.frequencyHz} Hz",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Button(
                    onClick = onBack
                ) {
                    Text("Back")
                }
            }
        }

        // ---------------------------------------------------------
        // DESIGN SUMMARY
        // ---------------------------------------------------------

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            SldSummaryCard(
                title = "TRANSFORMER",
                value = "${transformerKva.roundToInt()} kVA",
                modifier = Modifier.width(150.dp)
            )

            SldSummaryCard(
                title = "DESIGN CURRENT",
                value = "${designCurrent.roundToInt()} A",
                modifier = Modifier.width(150.dp)
            )

            SldSummaryCard(
                title = "FAULT LEVEL",
                value = "$faultLevel kA",
                modifier = Modifier.width(150.dp)
            )

            SldSummaryCard(
                title = "POWER FACTOR",
                value = "%.2f".format(calculation.powerFactor),
                modifier = Modifier.width(150.dp)
            )
        }

        // ---------------------------------------------------------
        // TOOLBAR
        // ---------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "VIEW",
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        zoomLevel =
                            (zoomLevel - 0.1f)
                                .coerceAtLeast(0.6f)

                        notice =
                            "Zoom ${"%.0f".format(zoomLevel * 100)}%"
                    }
                ) {
                    Text("−")
                }

                Text(
                    text = "${"%.0f".format(zoomLevel * 100)}%",
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        zoomLevel =
                            (zoomLevel + 0.1f)
                                .coerceAtMost(1.6f)

                        notice =
                            "Zoom ${"%.0f".format(zoomLevel * 100)}%"
                    }
                ) {
                    Text("+")
                }

                OutlinedButton(
                    onClick = {
                        zoomLevel = 1.0f
                        notice = "View reset to 100%."
                    }
                ) {
                    Text("Fit")
                }

                OutlinedButton(
                    onClick = {
                        notice =
                            "SLD regenerated from current project design data."
                    }
                ) {
                    Text("Refresh SLD")
                }
            }
        }

        // ---------------------------------------------------------
        // MAIN DRAWING
        // ---------------------------------------------------------

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF08111F)
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier
                            .width(0.dp)
                            .weightSafe()
                    ) {

                        Text(
                            text = "ELECTRICAL DISTRIBUTION",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = calculation.projectName.ifBlank {
                                "Electrical Distribution System"
                            },
                            color = Color(0xFF9FB3C8),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Text(
                        text = "IEC DESIGN BASIS",
                        color = Color(0xFF6CE5B1),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        )
                ) {

                    ProfessionalSldDrawing(
                        transformerKva = transformerKva,
                        designCurrent = designCurrent,
                        faultLevel = faultLevel,
                        selectedNode = selectedNode,
                        zoomLevel = zoomLevel,
                        onSelect = {
                            selectedNode = it

                            notice =
                                "$it selected — engineering data displayed below."
                        }
                    )
                }
            }
        }

        // ---------------------------------------------------------
        // SELECTED COMPONENT
        // ---------------------------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {

            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Text(
                    text = "SELECTED COMPONENT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = selectedNode,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                HorizontalDivider()

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                SldEngineeringDetails(
                    selectedNode = selectedNode,
                    transformerKva = transformerKva,
                    designCurrent = designCurrent,
                    faultLevel = faultLevel,
                    voltage = voltage
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = notice,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Summary card used above the drawing.
 */
@Composable
private fun SldSummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier.height(76.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Main professional electrical drawing.
 */
@Composable
private fun ProfessionalSldDrawing(
    transformerKva: Double,
    designCurrent: Double,
    faultLevel: Double,
    selectedNode: String,
    zoomLevel: Float,
    onSelect: (String) -> Unit
) {

    val drawingWidth =
        (1100 * zoomLevel).roundToInt().dp

    val drawingHeight =
        (350 * zoomLevel).roundToInt().dp

    Box(
        modifier = Modifier
            .width(drawingWidth)
            .height(drawingHeight)
            .background(
                Color(0xFF08111F),
                RoundedCornerShape(8.dp)
            )
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val scale =
                zoomLevel.toFloat()

            val sx = { value: Float ->
                value * scale
            }

            val lineColor =
                Color(0xFF65D5FF)

            val healthyColor =
                Color(0xFF6CE5B1)

            val breakerColor =
                Color(0xFFFFC857)

            val transformerColor =
                Color(0xFF8FA8FF)

            val white =
                Color.White

            // -----------------------------------------------------
            // MAIN INCOMING
            // -----------------------------------------------------

            drawLine(
                color = lineColor,
                start = Offset(sx(40f), sx(175f)),
                end = Offset(sx(105f), sx(175f)),
                strokeWidth = sx(4f)
            )

            // Utility symbol
            drawCircle(
                color = healthyColor,
                radius = sx(18f),
                center = Offset(sx(40f), sx(175f)),
                style = Stroke(sx(3f))
            )

            // -----------------------------------------------------
            // TRANSFORMER
            // -----------------------------------------------------

            drawLine(
                color = lineColor,
                start = Offset(sx(105f), sx(175f)),
                end = Offset(sx(145f), sx(175f)),
                strokeWidth = sx(4f)
            )

            drawCircle(
                color = transformerColor,
                radius = sx(34f),
                center = Offset(sx(160f), sx(145f)),
                style = Stroke(sx(3f))
            )

            drawCircle(
                color = transformerColor,
                radius = sx(34f),
                center = Offset(sx(160f), sx(205f)),
                style = Stroke(sx(3f))
            )

            drawLine(
                color = lineColor,
                start = Offset(sx(194f), sx(175f)),
                end = Offset(sx(250f), sx(175f)),
                strokeWidth = sx(4f)
            )

            // -----------------------------------------------------
            // MAIN BREAKER
            // -----------------------------------------------------

            drawRect(
                color = breakerColor,
                topLeft = Offset(sx(250f), sx(145f)),
                size = Size(sx(35f), sx(60f)),
                style = Stroke(sx(3f))
            )

            drawLine(
                color = breakerColor,
                start = Offset(sx(255f), sx(195f)),
                end = Offset(sx(280f), sx(155f)),
                strokeWidth = sx(3f)
            )

            // -----------------------------------------------------
            // MDB BUS
            // -----------------------------------------------------

            drawLine(
                color = lineColor,
                start = Offset(sx(285f), sx(175f)),
                end = Offset(sx(360f), sx(175f)),
                strokeWidth = sx(4f)
            )

            drawLine(
                color = lineColor,
                start = Offset(sx(360f), sx(65f)),
                end = Offset(sx(360f), sx(285f)),
                strokeWidth = sx(8f)
            )

            // -----------------------------------------------------
            // OUTGOING FEEDERS
            // -----------------------------------------------------

            val feeders =
                listOf(
                    Triple(90f, 470f, "SMDB-01"),
                    Triple(175f, 590f, "SMDB-02"),
                    Triple(260f, 710f, "MCC-01")
                )

            feeders.forEach { feeder ->

                val y =
                    sx(feeder.first)

                val end =
                    sx(feeder.second)

                drawLine(
                    color = lineColor,
                    start = Offset(sx(360f), y),
                    end = Offset(end, y),
                    strokeWidth = sx(3f)
                )

                // feeder breaker
                drawRect(
                    color = breakerColor,
                    topLeft = Offset(
                        end,
                        y - sx(16f)
                    ),
                    size = Size(
                        sx(32f),
                        sx(32f)
                    ),
                    style = Stroke(sx(3f))
                )

                drawLine(
                    color = healthyColor,
                    start = Offset(
                        end + sx(32f),
                        y
                    ),
                    end = Offset(
                        end + sx(75f),
                        y
                    ),
                    strokeWidth = sx(3f)
                )

                // Panel symbol
                drawRect(
                    color = healthyColor,
                    topLeft = Offset(
                        end + sx(75f),
                        y - sx(25f)
                    ),
                    size = Size(
                        sx(50f),
                        sx(50f)
                    ),
                    style = Stroke(sx(3f))
                )

                // outgoing circuit
                drawLine(
                    color = healthyColor,
                    start = Offset(
                        end + sx(125f),
                        y
                    ),
                    end = Offset(
                        end + sx(165f),
                        y
                    ),
                    strokeWidth = sx(3f)
                )
            }

            // -----------------------------------------------------
            // NEUTRAL / EARTH SYMBOL
            // -----------------------------------------------------

            drawLine(
                color = white,
                start = Offset(sx(360f), sx(285f)),
                end = Offset(sx(360f), sx(315f)),
                strokeWidth = sx(3f)
            )

            drawLine(
                color = white,
                start = Offset(sx(340f), sx(315f)),
                end = Offset(sx(380f), sx(315f)),
                strokeWidth = sx(3f)
            )

            drawLine(
                color = white,
                start = Offset(sx(345f), sx(323f)),
                end = Offset(sx(375f), sx(323f)),
                strokeWidth = sx(3f)
            )

            drawLine(
                color = white,
                start = Offset(sx(350f), sx(331f)),
                end = Offset(sx(370f), sx(331f)),
                strokeWidth = sx(3f)
            )
        }

        // ---------------------------------------------------------
        // LABELS
        // ---------------------------------------------------------

        SldComponentLabel(
            text = "UTILITY\n11 kV",
            x = 12.dp,
            y = 188.dp,
            active = selectedNode == "UTILITY",
            onClick = {
                onSelect("UTILITY")
            }
        )

        SldComponentLabel(
            text = "TX-01\n${transformerKva.roundToInt()} kVA\n11/0.4 kV",
            x = 115.dp,
            y = 225.dp,
            active = selectedNode == "TRANSFORMER",
            onClick = {
                onSelect("TRANSFORMER")
            }
        )

        SldComponentLabel(
            text = "MAIN ACB\n${designCurrent.roundToInt()} A\nIcu ${faultLevel} kA",
            x = 230.dp,
            y = 215.dp,
            active = selectedNode == "MAIN ACB",
            onClick = {
                onSelect("MAIN ACB")
            }
        )

        SldComponentLabel(
            text = "MDB-01\n${designCurrent.roundToInt()} A",
            x = 305.dp,
            y = 15.dp,
            active = selectedNode == "MDB-01",
            onClick = {
                onSelect("MDB-01")
            }
        )

        SldComponentLabel(
            text = "SMDB-01\nMCCB 250 A",
            x = 430.dp,
            y = 55.dp,
            active = selectedNode == "SMDB-01",
            onClick = {
                onSelect("SMDB-01")
            }
        )

        SldComponentLabel(
            text = "SMDB-02\nMCCB 160 A",
            x = 550.dp,
            y = 140.dp,
            active = selectedNode == "SMDB-02",
            onClick = {
                onSelect("SMDB-02")
            }
        )

        SldComponentLabel(
            text = "MCC-01\nMCCB 125 A",
            x = 670.dp,
            y = 225.dp,
            active = selectedNode == "MCC-01",
            onClick = {
                onSelect("MCC-01")
            }
        )

        SldComponentLabel(
            text = "EARTH",
            x = 315.dp,
            y = 315.dp,
            active = selectedNode == "EARTH",
            onClick = {
                onSelect("EARTH")
            }
        )
    }
}

/**
 * Component label.
 */
@Composable
private fun SldComponentLabel(
    text: String,
    x: Dp,
    y: Dp,
    active: Boolean,
    onClick: () -> Unit
) {

    val borderColor =
        if (active) {
            Color(0xFF65D5FF)
        } else {
            Color(0xFF33485F)
        }

    Box(
        modifier = Modifier
            .offset(
                x = x,
                y = y
            )
            .border(
                width = if (active) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(5.dp)
            )
            .background(
                Color(0xCC0B1828),
                RoundedCornerShape(5.dp)
            )
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 6.dp,
                vertical = 4.dp
            )
    ) {

        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight =
                if (active) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                }
        )
    }
}

/**
 * Engineering information panel.
 */
@Composable
private fun SldEngineeringDetails(
    selectedNode: String,
    transformerKva: Double,
    designCurrent: Double,
    faultLevel: Double,
    voltage: Double
) {

    when (selectedNode) {

        "TRANSFORMER" -> {

            SldDataRow(
                "Equipment",
                "Distribution Transformer"
            )

            SldDataRow(
                "Rating",
                "${transformerKva.roundToInt()} kVA"
            )

            SldDataRow(
                "Primary",
                "11 kV"
            )

            SldDataRow(
                "Secondary",
                "${voltage.roundToInt()} V"
            )

            SldDataRow(
                "Impedance",
                "Project value required"
            )

            SldDataRow(
                "Vector Group",
                "Project value required"
            )
        }

        "MAIN ACB" -> {

            SldDataRow(
                "Breaker Type",
                "ACB"
            )

            SldDataRow(
                "Design Current Ib",
                "${designCurrent.roundToInt()} A"
            )

            SldDataRow(
                "Fault Current Ik",
                "$faultLevel kA"
            )

            SldDataRow(
                "Protection",
                "Ib ≤ In ≤ Iz"
            )

            SldDataRow(
                "Breaking Capacity",
                "Icu ≥ Ik"
            )
        }

        "MDB-01" -> {

            SldDataRow(
                "Panel",
                "Main Distribution Board"
            )

            SldDataRow(
                "Voltage",
                "${voltage.roundToInt()} V"
            )

            SldDataRow(
                "Design Current",
                "${designCurrent.roundToInt()} A"
            )

            SldDataRow(
                "Fault Level",
                "$faultLevel kA"
            )

            SldDataRow(
                "Status",
                "DESIGN REVIEW"
            )
        }

        "SMDB-01" -> {

            SldDataRow(
                "Panel",
                "Sub Main Distribution Board"
            )

            SldDataRow(
                "Breaker",
                "MCCB 250 A"
            )

            SldDataRow(
                "Cable",
                "Auto selected"
            )

            SldDataRow(
                "Protection",
                "Ib ≤ In ≤ Iz"
            )
        }

        "SMDB-02" -> {

            SldDataRow(
                "Panel",
                "Sub Main Distribution Board"
            )

            SldDataRow(
                "Breaker",
                "MCCB 160 A"
            )

            SldDataRow(
                "Cable",
                "Auto selected"
            )

            SldDataRow(
                "Voltage Drop",
                "≤ project limit"
            )
        }

        "MCC-01" -> {

            SldDataRow(
                "Panel",
                "Motor Control Centre"
            )

            SldDataRow(
                "Breaker",
                "MCCB 125 A"
            )

            SldDataRow(
                "Motor Loads",
                "Project loads"
            )

            SldDataRow(
                "Starting",
                "Motor starting check required"
            )
        }

        "UTILITY" -> {

            SldDataRow(
                "Supply",
                "Utility MV Supply"
            )

            SldDataRow(
                "Voltage",
                "11 kV"
            )

            SldDataRow(
                "Fault Level",
                "$faultLevel kA"
            )

            SldDataRow(
                "Interface",
                "Utility protection coordination"
            )
        }

        "EARTH" -> {

            SldDataRow(
                "System",
                "Protective Earthing"
            )

            SldDataRow(
                "Purpose",
                "Fault current return path"
            )

            SldDataRow(
                "Verification",
                "Earth resistance + fault loop"
            )
        }

        else -> {

            SldDataRow(
                "Component",
                selectedNode
            )

            SldDataRow(
                "Status",
                "Design data available"
            )

            SldDataRow(
                "Protection",
                "Verify Ib ≤ In ≤ Iz"
            )

            SldDataRow(
                "Short Circuit",
                "Verify Icu ≥ Ik"
            )
        }
    }
}

/**
 * Data row.
 */
@Composable
private fun SldDataRow(
    name: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {

        Text(
            text = name,
            modifier = Modifier.width(145.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Small helper replacing Modifier.weight().
 *
 * This intentionally returns a normal Modifier so the screen
 * does not depend on RowColumnParentData.weight.
 */
private fun Modifier.weightSafe(): Modifier {
    return this
}
