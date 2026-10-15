package com.ritmo.app

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val Background = Color(0xFF202124)
val SurfaceColor = Color(0xFF25262A)
val Blue = Color(0xFF669DF6)
val Muted = Color(0xFFA9AAAD)
val DividerColor = Color(0xFF3C4043)

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        // Native date/time widgets also consult the process locale for their labels.
        java.util.Locale.setDefault(Spanish)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(Spanish)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Background.toArgb()), navigationBarStyle = SystemBarStyle.dark(Background.toArgb()))
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, secondary = Blue, background = Background, surface = Background, surfaceVariant = SurfaceColor, outline = DividerColor, onSurface = Color(0xFFF1F3F4), onBackground = Color(0xFFF1F3F4))) {
                RitmoApp()
            }
        }
    }
}

private fun showDatePicker(context: android.content.Context, selected: LocalDate, today: LocalDate, onDate: (LocalDate) -> Unit) {
    val zone = ZoneId.systemDefault()
    DatePickerDialog(context, { _, year, month, day -> onDate(LocalDate.of(year, month + 1, day)) }, selected.year, selected.monthValue - 1, selected.dayOfMonth).apply {
        datePicker.minDate = minOf(StepSimulationEngine.HISTORY_START, today).atStartOfDay(zone).toInstant().toEpochMilli()
        datePicker.maxDate = today.atStartOfDay(zone).toInstant().toEpochMilli()
    }.show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RitmoApp(vm: MainViewModel = viewModel()) {
    val now by vm.now.collectAsState()
    val today = now.toLocalDate()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { vm.refresh(); delay(60_000) }
        }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    var followToday by rememberSaveable { mutableStateOf(true) }
    val selected = if (followToday) today else LocalDate.parse(selectedText).coerceIn(minOf(StepSimulationEngine.HISTORY_START, today), today)
    val onDate: (LocalDate) -> Unit = { date ->
        val bounded = date.coerceIn(minOf(StepSimulationEngine.HISTORY_START, today), today)
        selectedText = bounded.toString()
        followToday = bounded == today
    }
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }
    var debug by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }
    var returnTab by rememberSaveable { mutableIntStateOf(-1) }
    var returnHistory by rememberSaveable { mutableStateOf(false) }
    var returnDate by rememberSaveable { mutableStateOf(today.toString()) }
    var returnFollowsToday by rememberSaveable { mutableStateOf(true) }
    val openDay: (LocalDate) -> Unit = {
        returnTab = tab
        returnHistory = history
        returnDate = selected.toString()
        returnFollowsToday = followToday
        onDate(it); history = false; tab = 0
    }
    val openCalendar = { showDatePicker(context, selected, today, onDate) }
    val goBack: () -> Unit = {
        when {
            debug -> debug = false
            history -> history = false
            returnTab >= 0 -> {
                tab = returnTab; history = returnHistory
                selectedText = returnDate; followToday = returnFollowsToday
                returnTab = -1
            }
            tab != 0 || selected != today -> { tab = 0; onDate(today) }
            else -> (context as? Activity)?.finish()
        }
    }
    BackHandler(debug || history || tab != 0 || selected != today || returnTab >= 0, goBack)
    Scaffold(containerColor = Background, topBar = {
        Column {
            TopAppBar(
                title = {
                    Text(when { debug -> "Laboratorio"; history -> "Historial"; else -> "Pasos" }, fontWeight = FontWeight.Normal, modifier = Modifier.clickable {
                        val time = android.os.SystemClock.elapsedRealtime()
                        taps = if (time - lastTap > 2500) 1 else taps + 1
                        lastTap = time
                        if (taps >= 7) { debug = true; taps = 0 }
                    })
                },
                navigationIcon = { IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    if (!debug && !history) IconButton(onClick = openCalendar) { Icon(Icons.Default.Add, "Consultar una fecha") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Más opciones") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = SurfaceColor) {
                            DropdownMenuItem(text = { Text("Ir a hoy") }, onClick = { menu = false; history = false; debug = false; onDate(today); tab = 0; returnTab = -1 })
                            DropdownMenuItem(text = { Text("Historial completo") }, onClick = { menu = false; debug = false; history = true })
                            DropdownMenuItem(text = { Text("Acerca de Ritmo") }, onClick = { menu = false; info = true })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
            if (!debug && !history) {
                TabRow(selectedTabIndex = tab, containerColor = Background, contentColor = Blue, divider = { HorizontalDivider(color = DividerColor, thickness = .5.dp) }) {
                    listOf("Día", "Semana", "Mes").forEachIndexed { index, label ->
                        Tab(selected = tab == index, onClick = { tab = index; returnTab = -1 }, selectedContentColor = Blue, unselectedContentColor = Muted, text = { Text(label, style = MaterialTheme.typography.titleSmall) })
                    }
                }
            }
        }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                debug -> DebugScreen(vm.repository.engine)
                history -> HistoryScreen(now, vm.repository, openDay)
                tab == 0 -> DayScreen(now, selected, vm.repository, onDate, { tab = 1 }, { history = true }, openCalendar)
                tab == 1 -> WeekScreen(now, selected, vm.repository, onDate, openDay, { history = true }, openCalendar)
                else -> MonthScreen(now, selected, vm.repository, onDate, openDay, { history = true }, openCalendar)
            }
        }
    }
    if (info) AlertDialog(
        onDismissRequest = { info = false },
        title = { Text("Ritmo") },
        text = { Text(modifier = Modifier.verticalScroll(rememberScrollState()), text = "Un lugar para consultar tu actividad diaria, semanal y mensual.\n\nOrigen de los datos: los pasos de esta aplicación se generan mediante una simulación determinista según la fecha y la hora local. No proceden de sensores ni representan mediciones reales.\n\nEl historial comienza el 1 de septiembre de 2026. Los días terminados muestran su total; el día actual avanza durante la jornada. La meta diaria es de 7,500 pasos.\n\nTodo se guarda en este dispositivo, sin cuentas ni conexión a Internet.") },
        confirmButton = { TextButton(onClick = { info = false }) { Text("Entendido") } }
    )
}

@Composable
fun DebugScreen(engine: StepSimulationEngine) {
    val context = LocalContext.current
    var fakeText by rememberSaveable { mutableStateOf(LocalDateTime.now().withSecond(0).withNano(0).toString()) }
    val fake = LocalDateTime.parse(fakeText)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Laboratorio de simulación", style = MaterialTheme.typography.headlineSmall)
        Text("Consulta cualquier fecha y hora sin modificar el progreso del panel.", color = Muted)
        OutlinedButton(onClick = {
            DatePickerDialog(context, { _, y, m, d -> fakeText = LocalDateTime.of(LocalDate.of(y, m + 1, d), fake.toLocalTime()).toString() }, fake.year, fake.monthValue - 1, fake.dayOfMonth).show()
        }, shape = RoundedCornerShape(16.dp)) { Text("Fecha: ${fake.toLocalDate()}") }
        OutlinedButton(onClick = {
            TimePickerDialog(context, { _, h, m -> fakeText = fake.withHour(h).withMinute(m).toString() }, fake.hour, fake.minute, true).show()
        }, shape = RoundedCornerShape(16.dp)) { Text("Hora: ${fake.format(DateTimeFormatter.ofPattern("HH:mm"))}") }
        Summary("Objetivo del día", "${number(engine.dailyTarget(fake.toLocalDate()))} pasos")
        Summary("Pasos a esta hora", "${number(engine.steps(fake.toLocalDate(), fake))} pasos")
        Summary("Total del día anterior", "${number(engine.steps(fake.toLocalDate().minusDays(1), fake))} pasos")
        Text("Lunes a viernes: 7,800–9,800 pasos al terminar el día.\nSábado y domingo: 7,000–8,200 pasos.\nEl historial comienza el 1 de septiembre de 2026.", color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}
