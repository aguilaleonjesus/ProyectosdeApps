package com.ritmo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

val Spanish: Locale = Locale.forLanguageTag("es-MX")
fun number(value: Int): String = String.format(Locale.US, "%,d", value)
fun fullDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Spanish)).replaceFirstChar { it.titlecase(Spanish) }
fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Spanish))
fun monthTitle(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Spanish)).replaceFirstChar { it.titlecase(Spanish) }
fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
fun range(start: LocalDate): String {
    val end = start.plusDays(6)
    return if (start.month == end.month && start.year == end.year) "${start.dayOfMonth}–${shortDate(end)}" else "${shortDate(start)} – ${shortDate(end)}"
}

/** Fit numerical totals to their column without dropping digits at large font sizes. */
@Composable
fun AdaptiveNumber(value: String, style: TextStyle, centered: Boolean = false) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val width = measurer.measure(value, style).size.width.coerceAtLeast(1)
        val available = with(LocalDensity.current) { maxWidth.toPx() }
        val fitted = style.copy(fontSize = style.fontSize * (available / width).coerceAtMost(1f))
        Text(value, Modifier.fillMaxWidth(), style = fitted, maxLines = 1, softWrap = false, textAlign = if (centered) TextAlign.Center else TextAlign.Start)
    }
}

@Composable
fun Summary(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelLarge)
        AdaptiveNumber(value, MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium))
    }
}

@Composable
fun PeriodNavigation(label: String, previous: Boolean, next: Boolean, onPrevious: () -> Unit, onNext: () -> Unit, onCalendar: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, enabled = previous) { Icon(Icons.Default.ChevronLeft, "Periodo anterior") }
        TextButton(onClick = onCalendar, modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        IconButton(onClick = onNext, enabled = next) { Icon(Icons.Default.ChevronRight, "Periodo siguiente") }
    }
}

@Composable
fun GoalBadge(modifier: Modifier = Modifier) {
    Box(modifier.size(36.dp).background(Blue.copy(alpha = .10f), CircleShape).semantics { contentDescription = "Meta de 7,500 pasos alcanzada" }, contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Check, null, tint = Blue, modifier = Modifier.size(21.dp))
    }
}

@Composable
fun DayRow(day: DaySteps, today: LocalDate, onSelect: (LocalDate) -> Unit, showYear: Boolean = false) {
    Column {
        Row(Modifier.fillMaxWidth().clickable { onSelect(day.date) }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(fullDate(day.date) + if (showYear) " de ${day.date.year}" else "", style = MaterialTheme.typography.bodyLarge)
                Text("${number(day.steps)} pasos" + if (day.date == today) " · En curso" else "", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            if (day.steps >= StepSimulationEngine.GOAL) GoalBadge()
            else Icon(Icons.Default.ChevronRight, "Ver día", tint = Muted, modifier = Modifier.size(20.dp))
        }
        HorizontalDivider(color = DividerColor.copy(alpha = .65f), thickness = .5.dp)
    }
}

@Composable
fun HistoryShortcut(onHistory: () -> Unit) {
    OutlinedCard(onClick = onHistory, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor), colors = CardDefaults.outlinedCardColors(containerColor = Background)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, tint = Blue, modifier = Modifier.padding(end = 14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Todo tu historial", style = MaterialTheme.typography.titleSmall)
                Text("Desde el 1 de septiembre de 2026", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, "Abrir historial", tint = Muted)
        }
    }
}

@Composable
fun DayScreen(now: LocalDateTime, date: LocalDate, repository: StepRepository, onDate: (LocalDate) -> Unit, onWeek: () -> Unit, onHistory: () -> Unit, onCalendar: () -> Unit) {
    val today = now.toLocalDate()
    val steps = repository.steps(date, now)
    val days = repository.days(weekStart(date), 7, now)
    val counted = days.filter { it.date >= StepSimulationEngine.HISTORY_START && it.date <= today }
    val total = counted.sumOf { it.steps }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            PeriodNavigation(if (date == today) "Hoy, ${shortDate(date)}" else fullDate(date), date > StepSimulationEngine.HISTORY_START, date < today, { onDate(date.minusDays(1)) }, { onDate(date.plusDays(1)) }, onCalendar)
        }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                DailyProgressChart(steps)
                Surface(shape = CircleShape, color = Blue.copy(alpha = .10f)) {
                    Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (steps >= StepSimulationEngine.GOAL) Icon(Icons.Default.Check, null, tint = Blue, modifier = Modifier.size(17.dp))
                        Text("Meta: 7,500 pasos", color = Blue, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Text(if (steps >= StepSimulationEngine.GOAL) "Meta diaria alcanzada" else "${number((StepSimulationEngine.GOAL - steps).coerceAtLeast(0))} pasos para tu meta", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            HorizontalDivider(color = DividerColor, thickness = .5.dp)
            Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Summary("Total de la semana", number(total), Modifier.weight(1f))
                Summary("Promedio diario", number(if (counted.isEmpty()) 0 else total / counted.size), Modifier.weight(1f))
            }
        }
        item {
            Card(onClick = onWeek, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceColor)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("Tu semana", style = MaterialTheme.typography.titleMedium); Text(range(weekStart(date)), color = Muted, style = MaterialTheme.typography.bodySmall) }
                        Icon(Icons.Default.ChevronRight, "Ver actividad semanal", tint = Blue)
                    }
                    StepBarChart(days, listOf("D", "L", "M", "M", "J", "V", "S"), compact = true, selectedDate = date, onDaySelected = { onWeek() })
                }
            }
        }
        item { HistoryShortcut(onHistory) }
    }
}

@Composable
fun WeekScreen(now: LocalDateTime, date: LocalDate, repository: StepRepository, onDate: (LocalDate) -> Unit, onDay: (LocalDate) -> Unit, onHistory: () -> Unit, onCalendar: () -> Unit) {
    val today = now.toLocalDate()
    val start = weekStart(date)
    val days = repository.days(start, 7, now)
    val visibleDays = days.filter { it.date >= StepSimulationEngine.HISTORY_START && it.date <= today }
    val total = days.sumOf { it.steps }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
        item { PeriodNavigation(range(start), start > weekStart(StepSimulationEngine.HISTORY_START), start.plusDays(6) < today, { onDate(date.minusWeeks(1).coerceAtLeast(StepSimulationEngine.HISTORY_START)) }, { onDate(date.plusWeeks(1).coerceAtMost(today)) }, onCalendar) }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AdaptiveNumber(number(total), MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Normal, letterSpacing = (-1).sp), centered = true)
                Text("pasos", color = Muted, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item { StepBarChart(days, listOf("dom", "lun", "mar", "mié", "jue", "vie", "sáb"), onDaySelected = { if (it in StepSimulationEngine.HISTORY_START..today) onDay(it) }) }
        item {
            Text("Meta diaria: 7,500 pasos", color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 14.dp, bottom = 26.dp))
            Text("Los pasos son una medición de cuánto te mueves, y pueden ayudarte a identificar cambios en los niveles de actividad", color = Muted, lineHeight = 23.sp, style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(Modifier.padding(top = 26.dp, bottom = 8.dp), color = DividerColor, thickness = .5.dp)
        }
        items(visibleDays, key = { it.date.toEpochDay() }) { DayRow(it, today, onDay) }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Summary("Promedio diario", number(if (visibleDays.isEmpty()) 0 else total / visibleDays.size), Modifier.weight(1f))
                Summary("Días con meta", "${visibleDays.count { it.steps >= StepSimulationEngine.GOAL }} de ${visibleDays.size}", Modifier.weight(1f))
            }
        }
        item { HistoryShortcut(onHistory) }
    }
}

@Composable
fun MonthScreen(now: LocalDateTime, date: LocalDate, repository: StepRepository, onDate: (LocalDate) -> Unit, onDay: (LocalDate) -> Unit, onHistory: () -> Unit, onCalendar: () -> Unit) {
    val today = now.toLocalDate()
    val start = date.withDayOfMonth(1)
    val days = repository.days(start, start.lengthOfMonth(), now)
    val counted = days.filter { it.date >= StepSimulationEngine.HISTORY_START && it.date <= today }
    val total = counted.sumOf { it.steps }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
        item { PeriodNavigation(monthTitle(date), start > StepSimulationEngine.HISTORY_START.withDayOfMonth(1), date.withDayOfMonth(date.lengthOfMonth()) < today, { onDate(date.minusMonths(1).coerceAtLeast(StepSimulationEngine.HISTORY_START)) }, { onDate(date.plusMonths(1).coerceAtMost(today)) }, onCalendar) }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AdaptiveNumber(number(total), MaterialTheme.typography.displayMedium.copy(letterSpacing = (-1).sp), centered = true)
                Text("pasos este mes", color = Muted)
            }
        }
        item { MonthlyMiniChart(days, onDaySelected = { if (it in StepSimulationEngine.HISTORY_START..today) onDay(it) }) }
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Summary("Promedio diario", number(if (counted.isEmpty()) 0 else total / counted.size), Modifier.weight(1f))
                Summary("Días con meta", "${counted.count { it.steps >= StepSimulationEngine.GOAL }} de ${counted.size}", Modifier.weight(1f))
            }
            Text("Meta diaria: 7,500 pasos", color = Muted, style = MaterialTheme.typography.labelLarge)
            HorizontalDivider(Modifier.padding(top = 24.dp, bottom = 12.dp), color = DividerColor, thickness = .5.dp)
            Text("Actividad diaria", style = MaterialTheme.typography.titleMedium)
        }
        items(counted.reversed(), key = { it.date.toEpochDay() }) { DayRow(it, today, onDay) }
        item { Spacer(Modifier.height(24.dp)); HistoryShortcut(onHistory) }
    }
}

@Composable
fun HistoryScreen(now: LocalDateTime, repository: StepRepository, onDay: (LocalDate) -> Unit) {
    val days = repository.history(now).reversed()
    val groups = days.groupBy { it.date.withDayOfMonth(1) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
        item {
            Text("Desde el 1 de septiembre de 2026", color = Muted, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SurfaceColor)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Summary("Pasos acumulados", number(days.sumOf { it.steps }), Modifier.weight(1f))
                    Summary("Días con meta", "${days.count { it.steps >= StepSimulationEngine.GOAL }}", Modifier.weight(1f))
                }
            }
        }
        groups.forEach { (month, entries) ->
            item(key = "month-$month") { Text(monthTitle(month), color = Blue, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp)) }
            items(entries, key = { it.date.toEpochDay() }) { DayRow(it, now.toLocalDate(), onDay, showYear = true) }
        }
        if (days.isEmpty()) item { Text("El historial comienza el 1 de septiembre de 2026.", color = Muted, modifier = Modifier.padding(vertical = 32.dp)) }
    }
}
