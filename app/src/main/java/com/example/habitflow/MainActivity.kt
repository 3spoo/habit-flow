package com.example.habitflow

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.habitflow.alarm.AlarmManagerHelper
import com.example.habitflow.data.*
import com.example.habitflow.ui.PASTELS
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent{ HabitFlowAppUI(application as HabitFlowApp) }
    }

    @Composable
    fun RoutineScreen(app:HabitFlowApp){
        val routines by app.database.routines().observe().collectAsState(emptyList())
        val scope=rememberCoroutineScope()
        var add by remember{mutableStateOf(false)}
        var name by remember{mutableStateOf("")}
        var hour by remember{mutableIntStateOf(8)}
        var minute by remember{mutableIntStateOf(0)}
        var color by remember{mutableIntStateOf(0)}
        var selectedDays by remember{mutableStateOf(setOf(0,1,2,3,4,5,6))}
        val context=LocalContext.current
        Column(Modifier.fillMaxSize().padding(12.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("Routine",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                FilledTonalButton(onClick={add=true}){Icon(Icons.Default.Add,null);Text("Nuova")}
            }
            LazyColumn{
                items(routines){routine->
                    var expanded by remember{mutableStateOf(false)}
                    Card(Modifier.fillMaxWidth().padding(vertical=5.dp),
                        shape=RoundedCornerShape(16.dp),
                        border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){
                        Column(Modifier.clickable{expanded=!expanded}.padding(12.dp)){
                            Box(Modifier.fillMaxWidth().background(PASTELS[routine.colorIndex].copy(alpha=.55f),RoundedCornerShape(12.dp)).padding(10.dp),
                                contentAlignment=Alignment.Center){Text(routine.name,fontWeight=FontWeight.Bold)}
                            Spacer(Modifier.height(8.dp))
                            if(expanded){
                                val actions by app.database.actions().observe(routine.id).collectAsState(emptyList())
                                var actionTitle by remember{mutableStateOf("")}
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    OutlinedTextField(actionTitle,{actionTitle=it},label={Text("Subazione")},modifier=Modifier.weight(1f),
                                        shape=RoundedCornerShape(10.dp))
                                    IconButton(onClick={scope.launch{
                                        app.database.actions().insert(RoutineAction(routineId=routine.id,title=actionTitle,
                                            position=actions.size)); actionTitle=""
                                    }}){Icon(Icons.Default.Add,null)}
                                }
                                actions.forEachIndexed{index,action->
                                    Row(Modifier.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
                                        OutlinedButton(onClick={scope.launch{
                                            app.database.actions().update(action.copy(completedDate=if(action.completedDate==null)
                                                LocalDate.now().toString() else null))
                                        }},shape=RoundedCornerShape(10.dp),
                                            colors=ButtonDefaults.outlinedButtonColors(containerColor=
                                                if(action.completedDate!=null)PASTELS[routine.colorIndex]else Color.Transparent)){
                                            Text("${index+1}")
                                        }
                                        Spacer(Modifier.width(10.dp));Text(action.title)
                                    }
                                }
                            }else Text("%02d:%02d".format(routine.hour,routine.minute),
                                modifier=Modifier.padding(top=4.dp),style=MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
                                TextButton(onClick={scope.launch{
                                    AlarmManagerHelper.cancel(context,routine.id)
                                    app.database.routines().delete(routine)
                                }}){Text("Elimina")}
                            }
                        }
                    }
                }
            }
        }
        if(add) AlertDialog(onDismissRequest={add=false},title={Text("Nuova routine")},
            text={
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedTextField(name,{name=it},label={Text("Nome")})
                    Text("Giorni")
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        val names=listOf("Lun","Mar","Mer","Gio","Ven","Sab","Dom")
                        items(names.indices.toList()){index->
                            FilterChip(selected=index in selectedDays,onClick={
                                selectedDays=if(index in selectedDays)selectedDays-index else selectedDays+index
                            },label={Text(names[index])})
                        }
                    }
                    Row{TextButton(onClick={
                        TimePickerDialog(context,{_,h,m->hour=h;minute=m},hour,minute,true).show()
                    }){Text("Orario %02d:%02d".format(hour,minute))}}
                    Text("Colore")
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(PASTELS.indices.toList()){index->
                        Box(Modifier.size(34.dp).clickable{color=index}.background(PASTELS[index],
                            RoundedCornerShape(10.dp)).padding(2.dp))
                    }}
                }
            },
            confirmButton={TextButton(onClick={
                scope.launch{
                    var mask=0
                    selectedDays.forEach{mask=mask or (1 shl it)}
                    val routine=Routine(name=name,colorIndex=color,daysMask=mask,hour=hour,minute=minute)
                    val id=app.database.routines().insert(routine)
                    AlarmManagerHelper.schedule(context,routine.copy(id=id))
                }
                add=false
            }){Text("Salva")}},
            dismissButton={TextButton(onClick={add=false}){Text("Annulla")}})
    }

    @Composable
    fun TrackingScreen(app:HabitFlowApp,filterCategory:Category?){
        val activities by app.database.tracking().observe().collectAsState(emptyList())
        val categories by app.database.categories().observe().collectAsState(emptyList())
        val scope=rememberCoroutineScope()
        var add by remember{mutableStateOf(false)}
        var name by remember{mutableStateOf("")}
        var target by remember{mutableIntStateOf(1)}
        var frequency by remember{mutableStateOf("DAILY")}
        var color by remember{mutableIntStateOf(0)}
        val month=YearMonth.now()
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)){
            item{
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text(if(filterCategory==null)"Tracking" else filterCategory.name,
                        style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                    FilledTonalButton(onClick={add=true}){Icon(Icons.Default.Add,null);Text("Attività")}
                }
            }
            items(activities.filter{filterCategory==null||it.categoryId==filterCategory.id}){activity->
                var entries by remember{mutableStateOf<List<TrackingEntry>>(emptyList())}
                LaunchedEffect(activity.id){app.database.tracking().entries(activity.id).collect{entries=it}}
                Card(Modifier.fillMaxWidth().padding(vertical=6.dp),shape=RoundedCornerShape(16.dp),
                    border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.Top){
                        Box(Modifier.size(46.dp).background(PASTELS[activity.colorIndex],RoundedCornerShape(10.dp)),
                            contentAlignment=Alignment.Center){Icon(Icons.Default.Check,null)}
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)){
                            Row(verticalAlignment=Alignment.CenterVertically){
                                Text(activity.name,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                                TextButton(onClick={scope.launch{app.database.tracking().delete(activity)}}){Text("Elimina")}
                            }
                            val entryCounts=entries.associate{it.date to it.count}
                            Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                                (1..month.lengthOfMonth()).chunked(7).forEach{week->
                                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                        week.forEach{day->
                                            val date=month.atDay(day).toString()
                                            val count=entryCounts[date]?:0
                                            val done=count>=activity.target
                                            Box(Modifier.size(20.dp).background(
                                                if(done)PASTELS[activity.colorIndex]else Color.Transparent,
                                                RoundedCornerShape(5.dp)))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick={scope.launch{
                                val date=LocalDate.now().toString()
                                val old=app.database.tracking().entry(activity.id,date)
                                app.database.tracking().saveEntry(TrackingEntry(activityId=activity.id,date=date,
                                    count=(old?.count?:0)+1))
                            }}){Text("+1")}
                        }
                    }
                }
            }
        }
        if(add) AlertDialog(onDismissRequest={add=false},title={Text("Nuova attività")},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedTextField(name,{name=it},label={Text("Nome")})
                OutlinedTextField(target.toString(),{target=it.toIntOrNull()?:1},
                    label={Text("Obiettivo")})
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    listOf("DAILY","MONTHLY","WEEKLY","TIMED").forEach{mode->
                        FilterChip(selected=frequency==mode,onClick={frequency=mode},
                            label={Text(mode)})
                    }
                }
                LazyRow{items(PASTELS.indices.toList()){index->
                    Box(Modifier.padding(end=6.dp).size(34.dp).clickable{color=index}
                        .background(PASTELS[index],RoundedCornerShape(10.dp)))
                }}
            }},
            confirmButton={TextButton(onClick={scope.launch{
                app.database.tracking().insert(TrackingActivity(name=name,target=target,
                    frequencyType=frequency,colorIndex=color,categoryId=filterCategory?.id))
                add=false
            }){Text("Salva")}},
            dismissButton={TextButton(onClick={add=false}){Text("Annulla")}})
    }

    @Composable
    fun StudentScreen(app:HabitFlowApp){
        val exams by app.database.exams().observeExams().collectAsState(emptyList())
        val scope=rememberCoroutineScope()
        var add by remember{mutableStateOf(false)}
        var name by remember{mutableStateOf("")}
        var cfu by remember{mutableIntStateOf(6)}
        var studyMinutes by remember{mutableIntStateOf(25)}
        var remaining by remember{mutableIntStateOf(studyMinutes*60)}
        var running by remember{mutableStateOf(false)}
        LaunchedEffect(running,remaining){if(running&&remaining>0){
            kotlinx.coroutines.delay(1000); remaining--
        }else if(remaining==0)running=false}
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)){
            item{
                PermissionBanner(app)
                Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){
                    Column(Modifier.padding(12.dp)){
                        Text("Timer Pomodoro",fontWeight=FontWeight.Bold)
                        Text("%02d:%02d".format(remaining/60,remaining%60),
                            style=MaterialTheme.typography.headlineMedium)
                        Row{
                            Button(onClick={running=!running}){Text(if(running)"Pausa" else "Avvia")}
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick={running=false;remaining=studyMinutes*60}){Text("Reset")}
                        }
                        OutlinedTextField(studyMinutes.toString(),{
                            studyMinutes=it.toIntOrNull()?:25; if(!running)remaining=studyMinutes*60
                        },label={Text("Minuti")})
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical=8.dp),
                    verticalAlignment=Alignment.CenterVertically){
                    Text("Esami",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                    FilledTonalButton(onClick={add=true}){Icon(Icons.Default.Add,null);Text("Esame")}
                }
            }
            items(exams){exam->
                var expanded by remember{mutableStateOf(false)}
                var dateText by remember{mutableStateOf("")}
                var topicText by remember{mutableStateOf("")}
                Card(Modifier.fillMaxWidth().padding(vertical=5.dp),shape=RoundedCornerShape(16.dp),
                    border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){
                    Column(Modifier.clickable{expanded=!expanded}.padding(12.dp)){
                        Text(exam.name,fontWeight=FontWeight.Bold)
                        Text("${exam.cfu} CFU",style=MaterialTheme.typography.bodySmall)
                        if(expanded){
                            OutlinedTextField(exam.notes,{scope.launch{app.database.exams().updateExam(exam.copy(notes=it))}},
                                label={Text("Note esame")},modifier=Modifier.fillMaxWidth())
                            val dates by app.database.exams().observeDates(exam.id).collectAsState(emptyList())
                            dates.forEach{date->
                                val days=(try{java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(),
                                    LocalDate.parse(date.date)).toInt()}catch(_:Exception){0})
                                Text("${date.date}: $days giorni",style=MaterialTheme.typography.bodySmall)
                            }
                            Row{OutlinedTextField(dateText,{dateText=it},label={Text("Data YYYY-MM-DD")},
                                modifier=Modifier.weight(1f));IconButton(onClick={scope.launch{
                                app.database.exams().insertDate(ExamDate(examId=exam.id,date=dateText))
                            }}){Icon(Icons.Default.Add,null)}}
                            Text("Argomenti",fontWeight=FontWeight.Bold)
                            val topics by app.database.exams().observeTopics(exam.id).collectAsState(emptyList())
                            Row{OutlinedTextField(topicText,{topicText=it},modifier=Modifier.weight(1f))
                                IconButton(onClick={scope.launch{
                                    app.database.exams().insertTopic(ExamTopic(examId=exam.id,title=topicText,
                                        position=topics.size));topicText=""
                                }}){Icon(Icons.Default.Add,null)}}
                            topics.forEachIndexed{index,topic->
                                Row(verticalAlignment=Alignment.CenterVertically){
                                    OutlinedButton(onClick={scope.launch{
                                        app.database.exams().updateTopic(topic.copy(completed=!topic.completed))
                                    }},shape=RoundedCornerShape(10.dp)){Text("${index+1}")}
                                    Spacer(Modifier.width(8.dp));Text(topic.title)
                                }
                            }
                            val done=topics.count{it.completed}
                            Text("$done/${topics.size} argomenti completati")
                        }
                    }
                }
            }
        }
        if(add) AlertDialog(onDismissRequest={add=false},title={Text("Nuovo esame")},
            text={Column{
                OutlinedTextField(name,{name=it},label={Text("Esame")})
                OutlinedTextField(cfu.toString(),{cfu=it.toIntOrNull()?:6},label={Text("CFU")})
            }},confirmButton={TextButton(onClick={scope.launch{
                app.database.exams().insertExam(Exam(name=name,cfu=cfu));add=false
            }}){Text("Salva")}},dismissButton={TextButton(onClick={add=false}){Text("Annulla")}})
    }

    @Composable
    fun SettingsScreen(app:HabitFlowApp,dark:Boolean,student:Boolean){
        val scope=rememberCoroutineScope()
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)){
            item{
                Text("Impostazioni",style=MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Card(shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){
                    Column(Modifier.padding(12.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Text("Tema scuro",modifier=Modifier.weight(1f))
                            Switch(checked=dark,onCheckedChange={scope.launch{app.preferences.setDarkTheme(it)}})
                        }
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Text("Tema chiaro",modifier=Modifier.weight(1f))
                            Switch(checked=!dark,onCheckedChange={scope.launch{app.preferences.setDarkTheme(!it)}})
                        }
                        Text("Modalità studente: ${if(student)"Attiva" else "Disattiva"}")
                    }
                }
                Spacer(Modifier.height(12.dp))
                PermissionBanner(app)
                Text("Palette",modifier=Modifier.padding(top=12.dp,bottom=8.dp),
                    style=MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    items(PASTELS.indices.toList()){index->
                        Card(Modifier.size(48.dp).clickable{
                            scope.launch{app.preferences.setPaletteIndex(index)}
                        },shape=RoundedCornerShape(10.dp),
                            colors=CardDefaults.cardColors(containerColor=PASTELS[index])){}
                    }
                }
            }
        }
    }

    @Composable
    fun CategoriesScreen(app:HabitFlowApp){
        val categories by app.database.categories().observe().collectAsState(emptyList())
        val scope=rememberCoroutineScope()
        var add by remember{mutableStateOf(false)}
        var name by remember{mutableStateOf("")}
        var color by remember{mutableIntStateOf(0)}
        Column(Modifier.fillMaxSize().padding(12.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text("Categorie",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                FilledTonalButton(onClick={add=true}){Icon(Icons.Default.Add,null);Text("Nuova")}
            }
            LazyColumn{
                items(categories){category->
                    Card(Modifier.fillMaxWidth().padding(vertical=5.dp),shape=RoundedCornerShape(16.dp)){
                        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                            Box(Modifier.size(32.dp).background(PASTELS[category.colorIndex],RoundedCornerShape(10.dp)))
                            Spacer(Modifier.width(12.dp));Text(category.name,modifier=Modifier.weight(1f))
                            TextButton(onClick={scope.launch{app.database.categories().delete(category)}}){Text("Elimina")}
                        }
                    }
                }
            }
        }
        if(add) AlertDialog(onDismissRequest={add=false},title={Text("Nuova categoria")},
            text={Column{
                OutlinedTextField(name,{name=it},label={Text("Nome")})
                LazyRow{items(PASTELS.indices.toList()){index->
                    Box(Modifier.padding(end=6.dp).size(34.dp).clickable{color=index}
                        .background(PASTELS[index],RoundedCornerShape(10.dp)))
                }}
            }},
            confirmButton={TextButton(onClick={scope.launch{
                app.database.categories().insert(Category(name=name,colorIndex=color));add=false
            }}){Text("Salva")}},
            dismissButton={TextButton(onClick={add=false}){Text("Annulla")}})
    }

    @Composable
    fun CreateSheet(app:HabitFlowApp,onRoutine:()->Unit,onTracking:()->Unit){
        val context=LocalContext.current
        var sheet by remember{mutableStateOf(true)}
        if(sheet) AlertDialog(onDismissRequest={sheet=false},title={Text("Aggiungi")},
            text={Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                Card(Modifier.weight(1f).height(100.dp).clickable{
                    sheet=false;onRoutine()
                },shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=PASTELS[0])){
                    Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.Center){Icon(Icons.Default.FormatListBulleted,null);Text("Routine")}
                }
                Card(Modifier.weight(1f).height(100.dp).clickable{
                    sheet=false;onTracking()
                },shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=PASTELS[1])){
                    Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.Center){Icon(Icons.Default.GridView,null);Text("Tracking")}
                }
            }},
            confirmButton={},dismissButton={})
    }

    @Composable
    fun StatisticsScreen(app:HabitFlowApp,studentMode:Boolean){
        val categories by app.database.categories().observe().collectAsState(emptyList())
        val activities by app.database.tracking().observe().collectAsState(emptyList())
        var filter by remember{mutableStateOf("Tutte")}
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)){
            item{
                LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    item{FilterChip(selected=filter=="Tutte",onClick={filter="Tutte"},label={Text("Tutte")})}
                    items(categories){category->
                        FilterChip(selected=filter==category.name,onClick={filter=category.name},label={Text(category.name)})
                    }
                    if(studentMode)item{
                        FilterChip(selected=filter=="Studente",onClick={filter="Studente"},label={Text("Studente")})
                    }
                }
                Spacer(Modifier.height(12.dp))
                val visible=if(filter=="Tutte")activities else activities.filter{activity->
                    categories.firstOrNull{it.id==activity.categoryId}?.name==filter
                }
                Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){
                    Column(Modifier.padding(16.dp)){
                        Text("Completamento",style=MaterialTheme.typography.titleMedium)
                        Text("${visible.size} attività monitorate",style=MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        visible.forEach{activity->
                            Row(Modifier.padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
                                Box(Modifier.size(12.dp).background(PASTELS[activity.colorIndex],RoundedCornerShape(4.dp)))
                                Spacer(Modifier.width(8.dp));Text(activity.name)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth().height(150.dp),shape=RoundedCornerShape(16.dp),
                    colors=CardDefaults.cardColors(containerColor=PASTELS[1].copy(alpha=.25f))){
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Bottom){
                            listOf(35,55,42,72,61,84,68).forEachIndexed{index,height->
                                Box(Modifier.width(18.dp).height(height.dp).background(PASTELS[1],
                                    RoundedCornerShape(topStart=6.dp,topEnd=6.dp)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFlowAppUI(app:HabitFlowApp){
    val dark by app.preferences.darkTheme.collectAsState(false)
    val studentMode by app.preferences.studentMode.collectAsState(false)
    val palette by app.preferences.paletteIndex.collectAsState(0)
    val scope=rememberCoroutineScope()
    var tab by remember{mutableIntStateOf(0)}
    var drawerCategory by remember{mutableStateOf<Category?>(null)}
    val drawerState=rememberDrawerState(DrawerValue.Closed)
    val categories by app.database.categories().observe().collectAsState(emptyList())

    MaterialTheme(colorScheme=if(dark) darkColorScheme(
        primary=PASTELS[palette], background=Color(0xFF1E1E1E),
        surface=Color(0xFF252525), onBackground=Color.White,
        onSurface=Color.White
    )else lightColorScheme(
        primary=PASTELS[palette], background=Color(0xFFFAF7F2),
        surface=Color(0xFFFAF7F2), onBackground=Color(0xFF27251F),
        onSurface=Color(0xFF27251F)
    )) {
        ModalNavigationDrawer(drawerState=drawerState,drawerContent={
            ModalDrawerSheet(drawerContainerColor=MaterialTheme.colorScheme.background){
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                    Text("Menu",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                    IconButton(onClick={scope.launch{drawerState.close()}}){
                        Icon(Icons.Default.Close,"Chiudi")
                    }
                }
                NavigationDrawerItem(label={Text("Impostazioni")},selected=false,onClick={
                    tab=6; scope.launch{drawerState.close()}
                })
                Text("Categorie",modifier=Modifier.padding(16.dp,12.dp,16.dp,6.dp),
                    style=MaterialTheme.typography.labelLarge)
                categories.forEach{category->
                    NavigationDrawerItem(label={Row(verticalAlignment=Alignment.CenterVertically){
                        Box(Modifier.size(12.dp).background(PASTELS[category.colorIndex],RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(10.dp)); Text(category.name)
                    }},selected=drawerCategory?.id==category.id,onClick={
                        drawerCategory=category; tab=7; scope.launch{drawerState.close()}
                    })
                }
                TextButton(onClick={tab=8; scope.launch{drawerState.close()}},
                    modifier=Modifier.padding(start=8.dp)){Icon(Icons.Default.Add,null);Text("Gestisci categorie")}
                Spacer(Modifier.weight(1f))
                var studentDialog by remember{mutableStateOf(false)}
                NavigationDrawerItem(label={Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(Icons.Default.School,null); Spacer(Modifier.width(10.dp)); Text("Studente")
                }},selected=tab==5,onClick={
                    if(studentMode){tab=5}else studentDialog=true
                    scope.launch{drawerState.close()}
                })
                if(studentDialog) AlertDialog(onDismissRequest={studentDialog=false},
                    title={Text("Vuoi attivare la modalità studente?")},
                    confirmButton={TextButton(onClick={
                        scope.launch{app.preferences.setStudentMode(true)}
                        tab=5; studentDialog=false
                    }){Text("Sì")}},
                    dismissButton={TextButton(onClick={studentDialog=false}){Text("No")}})
            }
        }) {
            Scaffold(topBar={TopAppBar(title={Text("Habit Flow")},
                navigationIcon={IconButton(onClick={scope.launch{drawerState.open()}}){
                    Icon(Icons.Default.Menu,"Menu")
                }},
                actions={
                    if(tab==5) IconButton(onClick={scope.launch{app.preferences.setStudentMode(false)}}){
                        Icon(Icons.Default.MoreVert,"Disattiva")
                    }
                })},
                bottomBar={
                    NavigationBar{
                        NavigationBarItem(selected=tab==0,onClick={tab=0},icon={Icon(Icons.Default.CalendarMonth,null)},label={Text("Giorno")})
                        NavigationBarItem(selected=tab==1,onClick={tab=1},icon={Icon(Icons.Default.FormatListBulleted,null)},label={Text("Routine")})
                        NavigationBarItem(selected=false,onClick={tab=9},icon={Icon(Icons.Default.Add,null)},label={Text("Aggiungi")})
                        NavigationBarItem(selected=tab==2,onClick={tab=2},icon={Icon(Icons.Default.GridView,null)},label={Text("Tracking")})
                        NavigationBarItem(selected=tab==3,onClick={tab=3},icon={Icon(Icons.Default.ShowChart,null)},label={Text("Statistiche")})
                    }
                }){padding->
                Surface(Modifier.padding(padding).fillMaxSize()){
                    when(tab){
                        0->DayScreen(app)
                        1->RoutineScreen(app)
                        2->TrackingScreen(app,null)
                        3->StatisticsScreen(app,studentMode)
                        5->StudentScreen(app)
                        6->SettingsScreen(app,dark,studentMode)
                        7->TrackingScreen(app,drawerCategory)
                        8->CategoriesScreen(app)
                        9->CreateSheet(app,{tab=1},{tab=2})
                    }
                }
            }
        }
    }
}

@Composable fun PermissionBanner(app:HabitFlowApp){
    val context=LocalContext.current
    val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
    Card(Modifier.fillMaxWidth().padding(12.dp),colors=CardDefaults.cardColors(containerColor=PASTELS[3].copy(alpha=.45f))){
        Column(Modifier.padding(12.dp)){
            Text("Permessi essenziali",fontWeight=FontWeight.Bold)
            Text("Notifiche, sveglia full-screen e ottimizzazione batteria.")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Button(onClick={
                    if(Build.VERSION.SDK_INT>=33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }){Text("Notifiche")}
                OutlinedButton(onClick={
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:${context.packageName}")))
                }){Text("Sveglia")}
                OutlinedButton(onClick={
                    context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}")))
                }){Text("Batteria")}
            }
        }
    }
}

@Composable fun DayScreen(app:HabitFlowApp){
    val routines by app.database.routines().observe().collectAsState(emptyList())
    val tracking by app.database.tracking().observe().collectAsState(emptyList())
    val dates by remember{mutableStateOf(LocalDate.now())}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=12.dp)){
        item{
            Text(dates.month.getDisplayName(TextStyle.FULL,Locale.ITALIAN).replaceFirstChar{it.titlecase()},
                style=MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                val month=YearMonth.of(dates.year,dates.month)
                items((1..month.lengthOfMonth()).toList()){day->
                    val selected=day==dates.dayOfMonth
                    Card(Modifier.size(58.dp).clickable{},
                        shape=RoundedCornerShape(16.dp),
                        border=BorderStroke(1.dp,if(selected)PASTELS[0]else MaterialTheme.colorScheme.outline),
                        colors=CardDefaults.cardColors(containerColor=if(selected)PASTELS[0].copy(alpha=.35f)else MaterialTheme.colorScheme.surface)){
                        Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
                            Text("$day",fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        items(routines){routine->
            Card(Modifier.fillMaxWidth().padding(vertical=5.dp),shape=RoundedCornerShape(16.dp),
                border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){
                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                    Text("%02d:%02d".format(routine.hour,routine.minute),fontWeight=FontWeight.Bold)
                    Spacer(Modifier.width(12.dp)); Box(Modifier.size(36.dp).background(PASTELS[routine.colorIndex],RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(12.dp)); Text(routine.name)
                }
            }
        }
        items(tracking){activity->
            Card(Modifier.fillMaxWidth().padding(vertical=5.dp),shape=RoundedCornerShape(16.dp)){
                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                    Box(Modifier.size(36.dp).background(PASTELS[activity.colorIndex],RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(12.dp)); Text(activity.name); Spacer(Modifier.weight(1f))
                    val scope=rememberCoroutineScope()
                    TextButton(onClick={scope.launch{
                        val date=LocalDate.now().toString()
                        val old=app.database.tracking().entry(activity.id,date)
                        app.database.tracking().saveEntry(TrackingEntry(activityId=activity.id,date=date,
                            count=(old?.count?:0)+1))
                    }}){Text("+1")}
                }
            }
        }
    }
}
