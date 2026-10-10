package com.ritmo.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val Background = Color(0xFF202124)
val SurfaceColor = Color(0xFF25262A)
val Blue = Color(0xFF669DF6)
val Muted = Color(0xFFA9AAAD)
val DividerColor = Color(0xFF3C4043)
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(Spanish)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Blue, background = Background, surface = Background, onSurface = Color(0xFFF1F3F4), onBackground = Color(0xFFF1F3F4))) { RitmoApp() }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RitmoApp(vm: MainViewModel = viewModel()) {
    val now by vm.now.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { vm.refresh(); delay(60_000) }
        }
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var taps by remember { mutableIntStateOf(0) }
    var debug by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }
    BackHandler(debug || tab != 0) { if (debug) debug = false else tab = 0 }
    Scaffold(containerColor = Background, topBar = {
        Column {
            TopAppBar(title = { Text(if (debug) "Simulación" else "Pasos", Modifier.clickable { taps++; if (taps == 7) { debug = true; taps = 0 } }) },
                navigationIcon = { IconButton(onClick = { if (debug) debug = false else tab = 0 }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    IconButton(onClick = { info = true }) { Icon(Icons.Default.Add, "Información de la simulación") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Más opciones") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Acerca de Ritmo") }, onClick = { menu = false; info = true })
                        }
                    }
                }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Background))
            if (!debug) TabRow(selectedTabIndex = tab, containerColor = Background, contentColor = Blue) {
                listOf("Día", "Semana", "Mes").forEachIndexed { index, label -> Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) }) }
            }
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            if (debug) DebugScreen(vm.repository.engine)
            else when (tab) { 0 -> DayScreen(now, vm.repository) { tab = 1 }; 1 -> WeekScreen(now, vm.repository); else -> MonthScreen(now, vm.repository) }
        }
    }
    if (info) AlertDialog(onDismissRequest = { info = false }, title = { Text("Ritmo") }, text = { Text("Los pasos son simulados y se calculan según la fecha y la hora local. No se registran pasos reales ni se necesitan permisos. La meta diaria es de 7,500 pasos.") }, confirmButton = { TextButton(onClick = { info = false }) { Text("Entendido") } })
}
@Composable
fun DebugScreen(engine: StepSimulationEngine) {
    val context = LocalContext.current
    var fakeText by rememberSaveable { mutableStateOf(LocalDateTime.now().withSecond(0).withNano(0).toString()) }
    val fake = LocalDateTime.parse(fakeText)
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Laboratorio de simulación", style = MaterialTheme.typography.headlineSmall)
        Text("Esta vista no modifica el progreso guardado.", color = Muted)
        OutlinedButton(onClick = {
            DatePickerDialog(context, { _, y, m, d -> fakeText = fake.withYear(y).withMonth(1).withDayOfMonth(1).withMonth(m + 1).withDayOfMonth(d).toString() }, fake.year, fake.monthValue - 1, fake.dayOfMonth).show()
        }) { Text("Fecha: ${fake.toLocalDate()}") }
        OutlinedButton(onClick = {
            TimePickerDialog(context, { _, h, m -> fakeText = fake.withHour(h).withMinute(m).toString() }, fake.hour, fake.minute, true).show()
        }) { Text("Hora: ${fake.format(DateTimeFormatter.ofPattern("HH:mm"))}") }
        Summary("Objetivo generado", "${number(engine.dailyTarget(fake.toLocalDate()))} pasos")
        Summary("Pasos calculados", "${number(engine.steps(fake.toLocalDate(), fake))} pasos")
    }
}
