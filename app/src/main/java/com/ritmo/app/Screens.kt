package com.ritmo.app

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.util.Locale

val Spanish = Locale.forLanguageTag("es-MX")
fun number(value: Int): String = String.format(Locale.US, "%,d", value)
fun fullDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Spanish)).replaceFirstChar { it.titlecase(Spanish) }
fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
fun range(start: LocalDate): String {
    val end = start.plusDays(6)
    val format = DateTimeFormatter.ofPattern("d 'de' MMMM", Spanish)
    return if (start.month == end.month) "${start.dayOfMonth}–${end.format(format)}" else "${start.format(format)} – ${end.format(format)}"
}
@Composable
fun Summary(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) { Text(label, color = Muted, style = MaterialTheme.typography.bodyMedium); Text(value, style = MaterialTheme.typography.headlineSmall) }
}
@Composable
fun DayScreen(now: LocalDateTime, repository: StepRepository, onWeek: () -> Unit) {
    val today = now.toLocalDate()
    val steps = repository.steps(today, now)
    val week = repository.days(weekStart(today), 7, now)
    val elapsed = week.count { it.date <= today }
    Column(Modifier.fillMaxSize().verticalScrollCompat().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(fullDate(today), color = Muted)
        DailyProgressChart(steps)
        Text("Meta: 7,500 pasos", color = Blue)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Summary("Total semanal", number(week.sumOf { it.steps }))
            Summary("Promedio diario", number(week.sumOf { it.steps } / elapsed))
        }
        Card(onClick = onWeek, colors = CardDefaults.cardColors(containerColor = SurfaceColor)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Actividad semanal"); Text("Consulta tu ritmo de la semana", color = Muted) }
                Icon(Icons.Default.ChevronRight, "Ver semana", tint = Blue)
            }
        }
        Text("Actividad simulada · No mide pasos reales", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}
@Composable
fun WeekScreen(now: LocalDateTime, repository: StepRepository) {
    val days = repository.days(weekStart(now.toLocalDate()), 7, now)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(range(days.first().date), color = Muted)
                Text(number(days.sumOf { it.steps }), style = MaterialTheme.typography.displaySmall)
                Text("pasos esta semana", color = Muted)
            }
        }
        item { StepBarChart(days, listOf("dom", "lun", "mar", "mié", "jue", "vie", "sáb")) }
        item { Text("Los pasos son una medición de cuánto te mueves, y pueden ayudarte a identificar cambios en los niveles de actividad", color = Muted) }
        items(days) { day ->
            Column {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(fullDate(day.date)); Text("${number(day.steps)} pasos", color = Muted) }
                    if (day.steps >= 7500) Box(Modifier.size(40.dp).background(SurfaceColor, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, "Meta alcanzada", tint = Blue) }
                }
                HorizontalDivider(color = DividerColor)
            }
        }
    }
}
@Composable
fun MonthScreen(now: LocalDateTime, repository: StepRepository) {
    val today = now.toLocalDate()
    val days = repository.days(today.withDayOfMonth(1), today.lengthOfMonth(), now)
    val total = days.sumOf { it.steps }
    Column(Modifier.fillMaxSize().verticalScrollCompat().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Spanish)).replaceFirstChar { it.titlecase(Spanish) }, style = MaterialTheme.typography.titleLarge)
        Summary("Total mensual", "${number(total)} pasos")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Summary("Promedio diario", number(total / today.dayOfMonth))
            Summary("Días con meta", "${days.count { it.steps >= 7500 }}")
        }
        MonthlyMiniChart(days)
        Text("Meta diaria: 7,500 pasos", color = Blue)
        Text("El promedio incluye los días transcurridos del mes. Los días futuros muestran cero.", color = Muted)
    }
}
@Composable
fun Modifier.verticalScrollCompat(): Modifier = this.verticalScroll(androidx.compose.foundation.rememberScrollState())
