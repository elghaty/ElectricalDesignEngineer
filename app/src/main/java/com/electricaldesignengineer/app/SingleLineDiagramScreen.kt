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
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A compact, editable single-line workspace.  It intentionally uses only
 * Compose Canvas primitives so it remains fast and works offline.
 */
@Composable
fun SingleLineDiagramScreen(onBack: () -> Unit = {}) {
    var transformerKva by remember { mutableStateOf(if (ProjectManager.calculation.transformerKVA > 0) ProjectManager.calculation.transformerKVA.toString() else "1000") }
    var faultLevel by remember { mutableStateOf(if (ProjectManager.calculation.shortCircuitKA > 0) ProjectManager.calculation.shortCircuitKA.toString() else "25") }
    var selected by remember { mutableStateOf("MDB") }
    var notice by remember { mutableStateOf("Ready — select an element to inspect its design data.") }
    val current = ProjectManager.calculation.designCurrentA
    val feederCurrent = if (current > 0) current else 630.0

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SINGLE LINE DIAGRAM", style = MaterialTheme.typography.headlineSmall)
                Text("LV distribution · live design checks", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onBack) { Text("Back") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = transformerKva, onValueChange = { transformerKva = it },
                label = { Text("Transformer kVA") }, modifier = Modifier.weight(1f), singleLine = true
            )
            OutlinedTextField(
                value = faultLevel, onValueChange = { faultLevel = it },
                label = { Text("Bus fault kA") }, modifier = Modifier.weight(1f), singleLine = true
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = {
                val kva = transformerKva.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                val ik = faultLevel.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                ProjectManager.setTransformer(kva)
                ProjectManager.setShortCircuit(ik)
                notice = "Design basis updated: ${kva.roundToInt()} kVA transformer, $ik kA fault level."
            }, modifier = Modifier.weight(1f)) { Text("Apply design basis") }
            Button(onClick = {
                val cable = EngineeringDesignEngine.autoSelectCable(
                    designCurrentA = feederCurrent, lengthM = 55.0, voltageV = 400.0,
                    powerFactor = 0.9, threePhase = true
                )
                notice = if (cable.success) "Feeder check PASS: ${cable.cable?.sizeMm2} mm², voltage drop ${"%.2f".format(cable.voltageDropPercent)}%." else cable.status
            }, modifier = Modifier.weight(1f)) { Text("Run feeder check") }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A)), modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.padding(12.dp)) {
                Text("Diagram workspace", style = MaterialTheme.typography.titleMedium)
                Text("Tap a component to view its selection state", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    SldCanvas(selected = selected, onSelected = { selected = it }, feederCurrent = feederCurrent)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Text("Selected: $selected", style = MaterialTheme.typography.titleMedium)
                Text(selectedDetails(selected, feederCurrent, faultLevel))
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(notice, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SldCanvas(selected: String, onSelected: (String) -> Unit, feederCurrent: Double) {
    val accent = Color(0xFF65D5FF)
    val safe = Color(0xFF6CE5B1)
    val warning = Color(0xFFFFC857)
    Box(Modifier.width(780.dp).height(330.dp).background(Color(0xFF08111F), RoundedCornerShape(8.dp))) {
        Canvas(Modifier.matchParentSize()) {
            val line = Stroke(width = 3f)
            // utility → transformer → MDB bus → three outgoing feeders
            drawLine(accent, androidx.compose.ui.geometry.Offset(50f, 165f), androidx.compose.ui.geometry.Offset(125f, 165f), strokeWidth = 4f)
            drawRect(safe, androidx.compose.ui.geometry.Offset(125f, 125f), androidx.compose.ui.geometry.Size(82f, 80f), style = line)
            drawLine(accent, androidx.compose.ui.geometry.Offset(207f, 165f), androidx.compose.ui.geometry.Offset(300f, 165f), strokeWidth = 4f)
            drawLine(accent, androidx.compose.ui.geometry.Offset(300f, 55f), androidx.compose.ui.geometry.Offset(300f, 275f), strokeWidth = 7f)
            listOf(75f to 430f, 165f to 540f, 255f to 650f).forEach { (y, end) ->
                drawLine(accent, androidx.compose.ui.geometry.Offset(300f, y), androidx.compose.ui.geometry.Offset(end, y), strokeWidth = 3f)
                drawRect(warning, androidx.compose.ui.geometry.Offset(end, y - 18f), androidx.compose.ui.geometry.Size(30f, 36f), style = line)
                drawLine(safe, androidx.compose.ui.geometry.Offset(end + 30f, y), androidx.compose.ui.geometry.Offset(end + 75f, y), strokeWidth = 3f)
                drawCircle(safe, 16f, androidx.compose.ui.geometry.Offset(end + 92f, y), style = line)
            }
        }
        SldTag("UTILITY\n11 kV", 20.dp, 185.dp, selected == "Utility") { onSelected("Utility") }
        SldTag("TX-01\n${ProjectManager.calculation.transformerKVA.takeIf { it > 0 } ?: 1000.0} kVA", 115.dp, 215.dp, selected == "Transformer") { onSelected("Transformer") }
        SldTag("MDB-01\n400 V · ${feederCurrent.roundToInt()} A", 250.dp, 285.dp, selected == "MDB") { onSelected("MDB") }
        SldTag("SMDB-L1\nMCCB 250 A", 405.dp, 95.dp, selected == "SMDB-L1") { onSelected("SMDB-L1") }
        SldTag("DB-OFFICE\nMCCB 160 A", 515.dp, 185.dp, selected == "DB-OFFICE") { onSelected("DB-OFFICE") }
        SldTag("MCC-PUMP\nMCCB 125 A", 625.dp, 275.dp, selected == "MCC-PUMP") { onSelected("MCC-PUMP") }
    }
}

@Composable
private fun SldTag(text: String, x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp, active: Boolean, onClick: () -> Unit) {
    Text(text, modifier = Modifier.padding(start = x, top = y).border(1.dp, if (active) Color(0xFF65D5FF) else Color.Transparent, RoundedCornerShape(4.dp)).clickable(onClick = onClick).padding(4.dp), style = MaterialTheme.typography.labelSmall)
}

private fun selectedDetails(selected: String, current: Double, fault: String): String = when (selected) {
    "Utility" -> "Incoming supply at 11 kV. Confirm utility fault level and protection interface."
    "Transformer" -> "11/0.4 kV transformer. Verify impedance, vector group, losses and earthing arrangement."
    "MDB" -> "Main distribution board. Design current: ${current.roundToInt()} A · available fault level: $fault kA."
    else -> "$selected outgoing feeder. Check Ib ≤ In ≤ Iz, Icu ≥ Ik, and voltage-drop limit before issue."
}
