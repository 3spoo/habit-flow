package com.example.habitflow.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.habitflow.HabitFlowApp
import com.example.habitflow.alarm.AlarmManagerHelper
import com.example.habitflow.data.RoutineAction
import kotlinx.coroutines.launch

class FullScreenRoutineActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true); setTurnScreenOn(true)
        val id=intent.getLongExtra("routine_id",-1)
        val name=intent.getStringExtra("routine_name")?:"Routine"
        setContent{
            val app=application as HabitFlowApp
            val scope=rememberCoroutineScope()
            var actions by remember { mutableStateOf<List<RoutineAction>>(emptyList()) }
            LaunchedEffect(id){actions=app.database.actions().get(id)}
            Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
                Column(Modifier.fillMaxSize().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){
                    Spacer(Modifier.height(48.dp))
                    Text(name,style=MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(24.dp))
                    actions.forEachIndexed{index,action->
                        OutlinedCard(Modifier.fillMaxWidth().padding(vertical=6.dp),
                            shape=RoundedCornerShape(16.dp),
                            border=BorderStroke(1.dp,PASTELS[0])){
                            Row(Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment=Alignment.CenterVertically){
                                OutlinedButton(onClick={scope.launch{
                                    app.database.actions().update(action.copy(completedDate=java.time.LocalDate.now().toString()))
                                }},shape=RoundedCornerShape(10.dp)){Text("${index+1}") }
                                Spacer(Modifier.width(12.dp)); Text(action.title)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick={AlarmManagerHelper.postpone(this@FullScreenRoutineActivity,id,name);
                        finish()},Modifier.fillMaxWidth()){Text("Posticipa di 10 minuti")}
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick={finish()},Modifier.fillMaxWidth()){Text("Salta per oggi")}
                }
            }
        }
    }
}
