package com.ritmo.app

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/** Shared week/month plot. Values and goal always use the same, integer-valued axis. */
@Composable
fun StepBarChart(
    days: List<DaySteps>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    selectedDate: LocalDate? = null,
    onDaySelected: (LocalDate) -> Unit = {},
) {
    if (days.isEmpty()) return
    val axisWidth = if (compact) 0.dp else 44.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.65f)
    // A week fits a normal phone in full. Only exceptionally narrow viewports scroll.
    val minimumPlotWidth = if (compact) (days.size * 8).dp else 224.dp
    val scale = maxOf(10_000, (((days.maxOfOrNull { it.steps } ?: 0) + 2_499) / 2_500) * 2_500)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val chartWidth = maxOf(maxWidth, minimumPlotWidth + axisWidth)
        Column(Modifier.horizontalScroll(rememberScrollState()).width(chartWidth)) {
            Box(Modifier.fillMaxWidth().height(if (compact) 128.dp else 222.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val axis = axisWidth.toPx()
                    val plotWidth = size.width - axis
                    val inset = if (compact) 8.dp.toPx() else 10.sp.toPx()
                    val plotHeight = size.height - inset * 2
                    val slot = plotWidth / days.size
                    fun y(value: Int) = inset + plotHeight * (1f - value.toFloat() / scale)
                    val baseline = y(0)
                    val goalY = y(StepSimulationEngine.GOAL)
                    val lineWidth = 1.dp.toPx()
                    if (!compact) {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Muted.toArgb()
                            textSize = 11.sp.toPx()
                            textAlign = Paint.Align.RIGHT
                            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                        }
                        val textOffset = -(paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2
                        for (value in listOf(scale, 5_000, 2_500, 0)) {
                            val lineY = y(value)
                            drawLine(DividerColor.copy(alpha = if (value == 0) .65f else .32f), Offset(0f, lineY), Offset(plotWidth, lineY), lineWidth)
                            drawContext.canvas.nativeCanvas.drawText(number(value), size.width, lineY + textOffset, paint)
                        }
                        paint.color = Blue.toArgb()
                        drawContext.canvas.nativeCanvas.drawText(number(StepSimulationEngine.GOAL), size.width, goalY + textOffset, paint)
                    } else {
                        drawLine(DividerColor.copy(alpha = .65f), Offset(0f, baseline), Offset(plotWidth, baseline), lineWidth)
                    }
                    days.forEachIndexed { index, day ->
                        val isSelected = day.date == selectedDate
                        val barWidth = (slot * if (compact) .58f else .38f).coerceAtLeast(2.dp.toPx())
                        val x = slot * index + (slot - barWidth) / 2
                        if (isSelected) {
                            drawRoundRect(Blue.copy(alpha = .08f), Offset(slot * index + slot * .07f, inset), Size(slot * .86f, plotHeight), CornerRadius(8.dp.toPx()))
                        }
                        val value = day.steps.coerceIn(0, scale)
                        if (value == 0) {
                            drawRoundRect(DividerColor, Offset(x, baseline - 2.dp.toPx()), Size(barWidth, 2.dp.toPx()), CornerRadius(1.dp.toPx()))
                        } else {
                            val top = y(value)
                            val radius = minOf(barWidth / 2, if (compact) 2.dp.toPx() else 4.dp.toPx(), (baseline - top) / 2)
                            val path = Path().apply {
                                moveTo(x, baseline)
                                lineTo(x, top + radius)
                                quadraticBezierTo(x, top, x + radius, top)
                                lineTo(x + barWidth - radius, top)
                                quadraticBezierTo(x + barWidth, top, x + barWidth, top + radius)
                                lineTo(x + barWidth, baseline)
                                close()
                            }
                            drawPath(path, Blue.copy(alpha = if (selectedDate == null || isSelected) 1f else .76f))
                        }
                    }
                    // Keep the daily goal visible even when bars cross it.
                    drawLine(Blue.copy(alpha = .52f), Offset(0f, goalY), Offset(plotWidth, goalY), lineWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
                }
                Row(Modifier.fillMaxSize().padding(end = axisWidth)) {
                    days.forEach { day ->
                        Box(Modifier.weight(1f).fillMaxHeight().semantics {
                            contentDescription = "${fullDate(day.date)}, ${number(day.steps)} pasos${if (day.steps >= StepSimulationEngine.GOAL) ", meta alcanzada" else ""}"
                            selected = day.date == selectedDate
                        }.clickable(role = Role.Button, onClickLabel = "Ver este día") { onDaySelected(day.date) })
                    }
                }
            }
            if (compact) {
                // Sparse labels get their own width instead of being clipped to a narrow daily bar.
                BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp).height(26.dp)) {
                    days.forEachIndexed { index, _ ->
                        val label = labels.getOrElse(index) { "" }
                        if (label.isNotEmpty()) {
                            val labelWidth = 40.dp
                            val x = (maxWidth * ((index + .5f) / days.size) - labelWidth / 2).coerceIn(0.dp, maxWidth - labelWidth)
                            Text(label, Modifier.offset(x = x).width(labelWidth).clearAndSetSemantics {}, color = Muted,
                                fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(end = axisWidth, top = 8.dp)) {
                    days.forEachIndexed { index, day ->
                        Text(
                            text = labels.getOrElse(index) { "" },
                            modifier = Modifier.weight(1f).clearAndSetSemantics {},
                            color = if (day.date == selectedDate) Blue else Muted,
                            fontSize = (12f / (LocalDensity.current.fontScale / 1.35f).coerceAtLeast(1f)).sp,
                            fontWeight = if (day.date == selectedDate) FontWeight.Medium else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyMiniChart(
    days: List<DaySteps>,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate? = null,
    onDaySelected: (LocalDate) -> Unit = {},
) = StepBarChart(
    days = days,
    labels = days.map { day ->
        if (day.date.dayOfMonth in listOf(1, 10, 20, days.lastOrNull()?.date?.dayOfMonth)) day.date.dayOfMonth.toString() else ""
    },
    compact = true,
    selectedDate = selectedDate,
    onDaySelected = onDaySelected,
    modifier = modifier,
)

@Composable
fun DailyProgressChart(steps: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, 272.dp)
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val numberStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Light)
        val textWidth = measurer.measure(number(steps), numberStyle).size.width
        val available = with(density) { (diameter * .73f).toPx() }
        val fittedStyle = numberStyle.copy(fontSize = numberStyle.fontSize * (available / textWidth.coerceAtLeast(1)).coerceAtMost(1f))
        Box(
            Modifier.size(diameter).semantics(mergeDescendants = true) {
                contentDescription = "${number(steps)} pasos de una meta de ${number(StepSimulationEngine.GOAL)}"
                progressBarRangeInfo = ProgressBarRangeInfo(steps.coerceIn(0, StepSimulationEngine.GOAL).toFloat(), 0f..StepSimulationEngine.GOAL.toFloat())
            },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                val width = 17.dp.toPx()
                drawArc(DividerColor.copy(alpha = .75f), -90f, 360f, false, style = Stroke(width))
                val progress = (steps.toFloat() / StepSimulationEngine.GOAL).coerceIn(0f, 1f)
                if (progress > 0f) {
                    drawArc(Blue, -90f, 360f * progress, false, style = Stroke(width, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics {}) {
                Text(number(steps), style = fittedStyle, color = Color(0xFFF1F3F4), maxLines = 1, softWrap = false)
                Text("pasos", style = MaterialTheme.typography.bodyLarge, color = Muted, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
