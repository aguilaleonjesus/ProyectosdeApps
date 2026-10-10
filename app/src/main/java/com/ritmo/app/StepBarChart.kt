package com.ritmo.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun StepBarChart(days: List<DaySteps>, labels: List<String>, compact: Boolean = false) {
    val scale = maxOf(10000, days.maxOfOrNull { it.steps } ?: 0)
    Column(Modifier.fillMaxWidth().semantics { contentDescription = days.joinToString { "${it.date}: ${it.steps} pasos" } }) {
        Row(Modifier.height(if (compact) 130.dp else 210.dp)) {
            Canvas(Modifier.weight(1f).fillMaxHeight()) {
                val slot = size.width / days.size
                val goalY = size.height * (1 - StepSimulationEngine.GOAL.toFloat() / scale)
                drawLine(DividerColor, Offset(0f, goalY), Offset(size.width, goalY), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
                days.forEachIndexed { i, day ->
                    val h = size.height * day.steps / scale
                    drawRect(Blue, Offset(slot * i + slot * .25f, size.height - h), Size(slot * .5f, h))
                }
            }
            if (!compact) BoxWithConstraints(Modifier.width(48.dp).fillMaxHeight()) {
                Text(number(scale), color = Muted, modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd))
                Text("7,500", color = Muted, modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd).offset(y = maxHeight * (1 - 7500f / scale) - 8.dp))
                Text("0", color = Muted, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd))
            }
        }
        Row(Modifier.fillMaxWidth().padding(end = if (compact) 0.dp else 48.dp)) {
            labels.forEach { Text(it, Modifier.weight(1f), color = Muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified) }
        }
    }
}

@Composable
fun MonthlyMiniChart(days: List<DaySteps>) = StepBarChart(days, days.map { if (it.date.dayOfMonth in listOf(1, 10, 20, days.size)) it.date.dayOfMonth.toString() else "" }, true)

@Composable
fun DailyProgressChart(steps: Int) {
    Box(Modifier.size(244.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            drawArc(DividerColor, -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(16.dp.toPx()))
            drawArc(Blue, -90f, 360f * (steps / 7500f).coerceIn(0f, 1f), false, style = androidx.compose.ui.graphics.drawscope.Stroke(16.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(number(steps), style = androidx.compose.material3.MaterialTheme.typography.displayMedium)
            Text("pasos simulados", color = Muted)
        }
    }
}
