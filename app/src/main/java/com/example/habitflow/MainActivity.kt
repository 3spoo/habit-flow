package com.example.habitflow

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.habitflow.alarm.AlarmManagerHelper
import com.example.habitflow.data.*
import com.example.habitflow.ui.PASTELS
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HabitFlowAppUI(application as HabitFlowApp) }
    }
}

private val pagePadding = 28.dp
private val cardShape = RoundedCornerShape(24.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFlowAppUI(app: HabitFlowApp) {
    val dark by app.preferences.darkTheme.collectAsState(false)
    val studentMode by app.preferences.studentMode.collectAsState(false)
    val palette by app.preferences.paletteIndex.collectAsState(0)
    val categories by app.database.categories().observe().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var tab by remember { mutableIntStateOf(0) }
    var drawerCategory by remember { mutableStateOf<Category?>(null) }

    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = PASTELS[palette],
            background = Color(0xFF1E1E1E),
            surface = Color(0xFF252525),
            onBackground = Color.White,
            onSurface = Color.White
        ) else lightColorScheme(
            primary = PASTELS[palette],
            background = Color(0xFFFDFBF8),
            surface = Color(0xFFFFFFFF),
            onBackground = Color(0xFF342F29),
            onSurface = Color(0xFF342F29)
        )
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.background,
                    modifier = Modifier.width(310.dp)
                ) {
                    Text(
                        "Menu",
                        Modifier.padding(start = 24.dp, top = 18.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Categorie",
                        Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f)
                    )
                    Spacer(Modifier.height(8.dp))
                    categories.forEach { category ->
                        NavigationDrawerItem(
                            label = { Text(category.name) },
                            selected = drawerCategory?.id == category.id,
                            onClick = {
                                drawerCategory = category
                                tab = 7
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = { tab = 8; scope.launch { drawerState.close() } },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("+ Nuova categoria") }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Impostazioni",
                        Modifier.padding(horizontal = 24.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Tema", Modifier.padding(horizontal = 24.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { scope.launch { app.preferences.setDarkTheme(false) } },
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!dark) PASTELS[0].copy(alpha = .30f) else Color.Transparent
                            )
                        ) { Text("Chiaro", fontWeight = if (!dark) FontWeight.Bold else FontWeight.Normal) }
                        OutlinedButton(
                            onClick = { scope.launch { app.preferences.setDarkTheme(true) } },
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (dark) PASTELS[0].copy(alpha = .30f) else Color.Transparent
                            )
                        ) { Text("Scuro", fontWeight = if (dark) FontWeight.Bold else FontWeight.Normal) }
                    }
                    var studentDialog by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = {
                            if (studentMode) tab = 5 else studentDialog = true
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (studentMode) "Modalità studente: attiva" else "Apri sezione Studente")
                    }
                    if (studentDialog) AlertDialog(
                        onDismissRequest = { studentDialog = false },
                        title = { Text("Vuoi attivare la modalità studente?") },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch { app.preferences.setStudentMode(true) }
                                tab = 5
                                studentDialog = false
                            }) { Text("Si") }
                        },
                        dismissButton = { TextButton(onClick = { studentDialog = false }) { Text("No") } }
                    )
                }
            }
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Text("Habit Flow",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold)
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.ArrowBack, "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = {}) { Icon(Icons.Default.Chat, null) }
                            IconButton(onClick = {}) { Icon(Icons.Default.Share, null) }
                            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        NavigationBarItem(tab == 0, { tab = 0 },
                            icon = { Icon(Icons.Default.CalendarMonth, null) }, label = null)
                        NavigationBarItem(tab == 1, { tab = 1 },
                            icon = { Icon(Icons.Default.Menu, null) }, label = null)
                        NavigationBarItem(tab == 9, { tab = 9 }, icon = {
                            Surface(Modifier.size(64.dp), RoundedCornerShape(22.dp),
                                color = PASTELS[5].copy(alpha = .82f)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, null, Modifier.size(30.dp))
                                }
                            }
                        }, label = null)
                        NavigationBarItem(tab == 2, { tab = 2 },
                            icon = { Icon(Icons.Default.GridView, null) }, label = null)
                        NavigationBarItem(tab == 3, { tab = 3 },
                            icon = { Icon(Icons.Default.ShowChart, null) }, label = null)
                    }
                }
            ) { padding ->
                Surface(Modifier.padding(padding).fillMaxSize()) {
                    when (tab) {
                        0 -> DayScreen(app)
                        1 -> RoutineScreen(app)
                        2 -> TrackingScreen(app, null)
                        3 -> StatisticsScreen(app)
                        5 -> StudentScreen(app)
                        6 -> SettingsScreen(app, dark)
                        7 -> TrackingScreen(app, drawerCategory)
                        8 -> CategoriesScreen(app)
                        9 -> CreateSheet(app, { tab = 1 }, { tab = 2 })
                    }
                }
            }
        }
    }
}

@Composable
fun DayScreen(app: HabitFlowApp) {
    val routines by app.database.routines().observe().collectAsState(emptyList())
    val month = YearMonth.now()
    var selected by remember { mutableIntStateOf(LocalDate.now().dayOfMonth) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = pagePadding, vertical = 18.dp)) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((1..month.lengthOfMonth()).toList()) { day ->
                    Card(
                        Modifier.size(58.dp).clickable { selected = day },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp,
                            if (day == selected) PASTELS[0] else MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (day == selected) PASTELS[0].copy(alpha = .35f)
                                else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("$day", fontWeight = if (day == selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        items(routines) { routine ->
            Card(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = cardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("%02d:%02d".format(routine.hour, routine.minute), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(36.dp).background(PASTELS[routine.colorIndex], RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(12.dp))
                    Text(routine.name)
                }
            }
        }
    }
}

@Composable
fun RoutineScreen(app: HabitFlowApp) {
    val routines by app.database.routines().observe().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = pagePadding, vertical = 18.dp)) {
        item {
            Text("Routine", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(22.dp))
        }
        items(routines) { routine ->
            val actions by app.database.actions().observe(routine.id).collectAsState(emptyList())
            var expanded by remember(routine.id) { mutableStateOf(false) }
            var title by remember(routine.id) { mutableStateOf("") }
            Card(
                Modifier.fillMaxWidth().padding(bottom = 18.dp).clickable { expanded = !expanded },
                shape = cardShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    Box(
                        Modifier.fillMaxWidth().height(62.dp)
                            .background(PASTELS[routine.colorIndex].copy(alpha = .72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${routine.name}  %02d:%02d  • ${actions.count { it.completedDate != null }}/${actions.size}",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        actions.forEachIndexed { index, action ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(42.dp).background(
                                        if (action.completedDate != null) PASTELS[routine.colorIndex]
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                    contentAlignment = Alignment.Center
                                ) { Text("${index + 1}", fontWeight = FontWeight.Bold) }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    action.title,
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (action.completedDate != null)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
                                    else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (action.completedDate != null)
                                        TextDecoration.LineThrough else null
                                )
                            }
                        }
                        if (expanded) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(title, { title = it }, label = { Text("Subazione") },
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                                IconButton(onClick = {
                                    if (title.isNotBlank()) scope.launch {
                                        app.database.actions().insert(
                                            RoutineAction(routineId = routine.id, title = title, position = actions.size)
                                        )
                                        title = ""
                                    }
                                }) { Icon(Icons.Default.Add, null) }
                            }
                            TextButton(onClick = {
                                scope.launch {
                                    AlarmManagerHelper.cancel(context, routine.id)
                                    app.database.routines().delete(routine)
                                }
                            }) { Text("Elimina") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingScreen(app: HabitFlowApp, filterCategory: Category?) {
     val activities by app.database.tracking().observe().collectAsState(emptyList())
     val month = YearMonth.now()
     val scope = rememberCoroutineScope()
     LazyColumn(
         Modifier.fillMaxSize(),
         contentPadding = PaddingValues(horizontal = pagePadding - 6.dp, vertical = 18.dp)
     ) {
         item {
             Text(filterCategory?.name ?: "Tracking", Modifier.fillMaxWidth(),
                 textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineMedium,
                 fontWeight = FontWeight.ExtraBold)
             Spacer(Modifier.height(20.dp))
         }
         items(activities.filter { filterCategory == null || it.categoryId == filterCategory.id }) { activity ->
             var entries by remember(activity.id) { mutableStateOf<List<TrackingEntry>>(emptyList()) }
             LaunchedEffect(activity.id) {
                 app.database.tracking().entries(activity.id).collect { entries = it }
             }
             val counts = entries.associate { it.date to it.count }
             val icon = if (activity.name.contains("Legge", true)) Icons.Default.MenuBook
                 else if (activity.name.contains("Bevi", true)) Icons.Default.LocalDrink
                 else Icons.Default.Check
             val detail = if (activity.frequencyType == "WEEKLY") "volte a settimana"
                 else if (activity.frequencyType == "MONTHLY") "volte al mese"
                 else "questa settimana"

             Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.Top) {
                 Box(
                     Modifier.padding(top = 28.dp).size(82.dp)
                         .background(PASTELS[activity.colorIndex].copy(alpha = .50f), RoundedCornerShape(22.dp)),
                     contentAlignment = Alignment.Center
                 ) {
                     Icon(icon, null, Modifier.size(34.dp),
                         tint = PASTELS[(activity.colorIndex + 1) % PASTELS.size])
                 }
                 Spacer(Modifier.width(18.dp))
                 Column(Modifier.weight(1f)) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                         Text("${activity.name} · ${activity.target} $detail",
                             Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                         Box(
                             Modifier.size(38.dp)
                                 .background(PASTELS[6].copy(alpha = .35f), RoundedCornerShape(12.dp)),
                             contentAlignment = Alignment.Center
                         ) { Icon(Icons.Default.Pause, null, Modifier.size(22.dp), tint = Color(0xFFB87300)) }
                     }
                     Card(
                         Modifier.fillMaxWidth().padding(top = 8.dp),
                         shape = RoundedCornerShape(24.dp),
                         border = BorderStroke(2.dp, PASTELS[activity.colorIndex].copy(alpha = .95f)),
                         colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                     ) {
                         Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                             (1..35).chunked(7).forEach { week ->
                                 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                     week.forEach { day ->
                                         val date = if (day <= month.lengthOfMonth()) month.atDay(day).toString() else ""
                                         val done = (counts[date] ?: 0) >= activity.target
                                         Box(
                                             Modifier.weight(1f).aspectRatio(1f)
                                                 .border(2.dp, PASTELS[activity.colorIndex].copy(alpha = .75f),
                                                     RoundedCornerShape(10.dp))
                                                 .background(
                                                     if (done) PASTELS[activity.colorIndex] else Color.Transparent,
                                                     RoundedCornerShape(10.dp)
                                                 ),
                                             contentAlignment = Alignment.Center
                                         ) {
                                             if (done) Icon(Icons.Default.Check, null,
                                                 tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                                         }
                                     }
                                 }
                             }
                         }
                     }
                     Text("0/${activity.target}", Modifier.padding(top = 8.dp),
                         style = MaterialTheme.typography.titleMedium,
                         color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                     TextButton(onClick = {
                         scope.launch {
                             val date = LocalDate.now().toString()
                             val old = app.database.tracking().entry(activity.id, date)
                             app.database.tracking().saveEntry(
                                 TrackingEntry(activityId = activity.id, date = date,
                                     count = (old?.count ?: 0) + 1)
                             )
                         }
                     }) { Text("+1") }
                 }
             }
         }
     }
}

@Composable
fun StudentScreen(app: HabitFlowApp) {
     val exams by app.database.exams().observeExams().collectAsState(emptyList())
     val scope = rememberCoroutineScope()
     var showAdd by remember { mutableStateOf(false) }
     var minutes by remember { mutableIntStateOf(25) }
     var remaining by remember { mutableIntStateOf(minutes * 60) }
     var running by remember { mutableStateOf(false) }
     LaunchedEffect(running, remaining) {
         if (running && remaining > 0) {
             delay(1000)
             remaining--
         } else if (remaining == 0) running = false
     }
     LazyColumn(Modifier.fillMaxSize(),
         contentPadding = PaddingValues(horizontal = pagePadding, vertical = 18.dp)) {
         item {
             Text("Studente", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                 style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
             Spacer(Modifier.height(24.dp))
             Card(
                 Modifier.fillMaxWidth(),
                 shape = RoundedCornerShape(24.dp),
                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
             ) {
                 Column(Modifier.fillMaxWidth().padding(20.dp),
                     horizontalAlignment = Alignment.CenterHorizontally) {
                     Text("Studio · blocchi completati: 0",
                         style = MaterialTheme.typography.titleMedium,
                         color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                     Text("%02d:%02d".format(remaining / 60, remaining % 60),
                         Modifier.padding(vertical = 10.dp),
                         style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold)
                     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                         OutlinedButton({ running = !running }, Modifier.weight(1f),
                             shape = RoundedCornerShape(14.dp)) { Text(if (running) "Pausa" else "Avvia") }
                         OutlinedButton({ remaining = minutes * 60 }, Modifier.weight(1f),
                             shape = RoundedCornerShape(14.dp)) { Text("Salta fase") }
                         OutlinedButton({ running = false; remaining = minutes * 60 }, Modifier.weight(1f),
                             shape = RoundedCornerShape(14.dp)) { Text("Azzera") }
                     }
                     Spacer(Modifier.height(14.dp))
                     OutlinedTextField(
                         minutes.toString(),
                         {
                             minutes = it.toIntOrNull() ?: 25
                             if (!running) remaining = minutes * 60
                         },
                         label = { Text("Durate (minuti)") },
                         modifier = Modifier.fillMaxWidth(),
                         shape = RoundedCornerShape(14.dp)
                     )
                 }
             }
             Spacer(Modifier.height(16.dp))
             Card(
                 Modifier.fillMaxWidth(),
                 shape = RoundedCornerShape(24.dp),
                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
             ) {
                 Row(Modifier.padding(18.dp)) {
                     Column(Modifier.weight(1f)) {
                         Text("CFU", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                         Text("${exams.sumOf { it.cfu }}", style = MaterialTheme.typography.headlineMedium,
                             fontWeight = FontWeight.Bold)
                     }
                     Column(Modifier.weight(1f)) {
                         Text("Media", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                         Text("—", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                     }
                     Column(Modifier.weight(1f)) {
                         Text("Ponderata", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                         Text("—", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                     }
                 }
             }
             Spacer(Modifier.height(14.dp))
             Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                 OutlinedButton({ showAdd = true }, shape = RoundedCornerShape(14.dp)) {
                     Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Esame")
                 }
                 OutlinedButton({}, shape = RoundedCornerShape(14.dp)) { Text("Importa piano (PDF)") }
             }
         }
         items(exams) { exam ->
             Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = cardShape,
                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                 Column(Modifier.padding(12.dp)) {
                     Text(exam.name, fontWeight = FontWeight.Bold)
                     Text("${exam.cfu} CFU", style = MaterialTheme.typography.bodySmall)
                 }
             }
         }
     }
     if (showAdd) {
         var name by remember { mutableStateOf("") }
         var cfu by remember { mutableIntStateOf(6) }
         AlertDialog(
             onDismissRequest = { showAdd = false },
             title = { Text("Nuovo esame") },
             text = {
                 Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                     OutlinedTextField(name, { name = it }, label = { Text("Esame") })
                     OutlinedTextField(cfu.toString(), { cfu = it.toIntOrNull() ?: 6 }, label = { Text("CFU") })
                 }
             },
             confirmButton = {
                 TextButton(onClick = {
                     scope.launch { app.database.exams().insertExam(Exam(name = name, cfu = cfu)) }
                     showAdd = false
                 }) { Text("Salva") }
             },
             dismissButton = { TextButton({ showAdd = false }) { Text("Annulla") } }
         )
     }
}

@Composable
fun StatisticsScreen(app: HabitFlowApp) {
     val categories by app.database.categories().observe().collectAsState(emptyList())
     val activities by app.database.tracking().observe().collectAsState(emptyList())
     LazyColumn(Modifier.fillMaxSize(),
         contentPadding = PaddingValues(horizontal = pagePadding, vertical = 18.dp)) {
         item {
             Text("Statistiche", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                 style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
             Spacer(Modifier.height(24.dp))
             Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                 Column(Modifier.padding(22.dp)) {
                     Text("Completato questo mese",
                         color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                     Text("1%", style = MaterialTheme.typography.displayMedium,
                         fontWeight = FontWeight.ExtraBold)
                 }
             }
             Spacer(Modifier.height(16.dp))
             Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                 Row(Modifier.padding(20.dp)) {
                     Column(Modifier.weight(1f)) {
                         Text("Serie attuale",
                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                         Text("0", style = MaterialTheme.typography.displaySmall,
                             fontWeight = FontWeight.ExtraBold)
                         Text("giorni",
                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                     }
                     Column(Modifier.weight(1f)) {
                         Text("Record",
                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                         Text("0", style = MaterialTheme.typography.displaySmall,
                             fontWeight = FontWeight.ExtraBold)
                         Text("giorni",
                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                     }
                 }
             }
             Spacer(Modifier.height(10.dp))
             Text("Un giorno conta nella serie se completi almeno il 50%.",
                 Modifier.padding(vertical = 8.dp),
                 color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
             Card(Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(24.dp),
                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                     Box(Modifier.fillMaxWidth().padding(horizontal = 48.dp).height(5.dp)
                         .background(PASTELS[1], CircleShape))
                 }
             }
             Spacer(Modifier.height(16.dp))
             Card(Modifier.fillMaxWidth().height(150.dp), shape = RoundedCornerShape(24.dp),
                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                 Row(
                     Modifier.fillMaxSize().padding(horizontal = 38.dp, vertical = 18.dp),
                     verticalAlignment = Alignment.Bottom,
                     horizontalArrangement = Arrangement.SpaceBetween
                 ) {
                     listOf("Giu", "Lug", "Ago", "Set", "Ott", "Nov").forEach {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                             Box(Modifier.size(14.dp).background(PASTELS[1], CircleShape))
                             Spacer(Modifier.height(8.dp))
                             Text(it, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                         }
                     }
                 }
             }
         }
     }
}

@Composable
fun SettingsScreen(app: HabitFlowApp, dark: Boolean) {
     val scope = rememberCoroutineScope()
     LazyColumn(Modifier.fillMaxSize(),
         contentPadding = PaddingValues(horizontal = pagePadding, vertical = 18.dp)) {
         item {
             Text("Impostazioni", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                 style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
             Spacer(Modifier.height(24.dp))
             Card(Modifier.fillMaxWidth(), shape = cardShape,
                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                 Column(Modifier.padding(16.dp)) {
                     Text("Tema", style = MaterialTheme.typography.labelLarge)
                     Spacer(Modifier.height(8.dp))
                     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         OutlinedButton(
                             { scope.launch { app.preferences.setDarkTheme(false) } },
                             Modifier.weight(1f),
                             shape = RoundedCornerShape(14.dp)
                         ) { Text("Chiaro", fontWeight = if (!dark) FontWeight.Bold else FontWeight.Normal) }
                         OutlinedButton(
                             { scope.launch { app.preferences.setDarkTheme(true) } },
                             Modifier.weight(1f),
                             shape = RoundedCornerShape(14.dp)
                         ) { Text("Scuro", fontWeight = if (dark) FontWeight.Bold else FontWeight.Normal) }
                     }
                 }
             }
             Spacer(Modifier.height(12.dp))
             PermissionBanner(app)
         }
     }
}

@Composable
fun CategoriesScreen(app: HabitFlowApp) {
     val categories by app.database.categories().observe().collectAsState(emptyList())
     val scope = rememberCoroutineScope()
     var showAdd by remember { mutableStateOf(false) }
     var name by remember { mutableStateOf("") }
     var color by remember { mutableIntStateOf(0) }
     Column(Modifier.fillMaxSize().padding(horizontal = pagePadding, vertical = 18.dp)) {
         Text("Categorie", Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
             style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
         Spacer(Modifier.height(20.dp))
         LazyColumn(Modifier.weight(1f)) {
             items(categories) { category ->
                 Card(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                     shape = RoundedCornerShape(16.dp),
                     border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                     Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                         Box(Modifier.size(32.dp).background(PASTELS[category.colorIndex], RoundedCornerShape(10.dp)))
                         Spacer(Modifier.width(12.dp))
                         Text(category.name, Modifier.weight(1f))
                         TextButton(onClick = { scope.launch { app.database.categories().delete(category) } }) {
                             Text("Elimina")
                         }
                     }
                 }
             }
         }
         OutlinedButton({ showAdd = true }, Modifier.fillMaxWidth().height(48.dp),
             shape = RoundedCornerShape(14.dp)) { Text("+ Nuova categoria") }
     }
     if (showAdd) AlertDialog(
         onDismissRequest = { showAdd = false },
         title = { Text("Nuova categoria") },
         text = {
             Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                 OutlinedTextField(name, { name = it }, label = { Text("Nome") })
                 LazyRow {
                     items(PASTELS.indices.toList()) { index ->
                         Box(Modifier.padding(end = 6.dp).size(34.dp).clickable { color = index }
                             .background(PASTELS[index], RoundedCornerShape(10.dp)))
                     }
                 }
             }
         },
         confirmButton = {
             TextButton(onClick = {
                 scope.launch { app.database.categories().insert(Category(name = name, colorIndex = color)) }
                 showAdd = false
             }) { Text("Salva") }
         },
         dismissButton = { TextButton({ showAdd = false }) { Text("Annulla") } }
     )
}

@Composable
fun CreateSheet(app: HabitFlowApp, onRoutine: () -> Unit, onTracking: () -> Unit) {
     AlertDialog(
         onDismissRequest = onRoutine,
         title = { Text("Aggiungi") },
         text = {
             Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                 Card(Modifier.weight(1f).height(100.dp).clickable(onClick = onRoutine),
                     shape = RoundedCornerShape(16.dp),
                     colors = CardDefaults.cardColors(containerColor = PASTELS[0])) {
                     Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Routine") }
                 }
                 Card(Modifier.weight(1f).height(100.dp).clickable(onClick = onTracking),
                     shape = RoundedCornerShape(16.dp),
                     colors = CardDefaults.cardColors(containerColor = PASTELS[1])) {
                     Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tracking") }
                 }
             }
         },
         confirmButton = {},
         dismissButton = {}
     )
}

@Composable
fun PermissionBanner(app: HabitFlowApp) {
     val context = LocalContext.current
     val notifications = rememberLauncherForActivityResult(
         ActivityResultContracts.RequestPermission()
     ) {}
     Card(
         Modifier.fillMaxWidth(),
         colors = CardDefaults.cardColors(containerColor = PASTELS[3].copy(alpha = .45f)),
         shape = RoundedCornerShape(16.dp)
     ) {
         Column(Modifier.padding(12.dp)) {
             Text("Permessi essenziali", fontWeight = FontWeight.Bold)
             Text("Notifiche, sveglia full-screen e ottimizzazione batteria.")
             Spacer(Modifier.height(8.dp))
             Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                 Button(onClick = {
                     if (Build.VERSION.SDK_INT >= 33)
                         notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                 }) { Text("Notifiche") }
                 OutlinedButton(onClick = {
                     context.startActivity(Intent(
                         Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                         Uri.parse("package:${context.packageName}")
                     ))
                 }) { Text("Sveglia") }
                 OutlinedButton(onClick = {
                     context.startActivity(Intent(
                         Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                         Uri.parse("package:${context.packageName}")
                     ))
                 }) { Text("Batteria") }
             }
         }
     }
}
