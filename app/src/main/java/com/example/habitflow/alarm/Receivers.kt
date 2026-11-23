package com.example.habitflow.alarm

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.habitflow.HabitFlowApp
import com.example.habitflow.MainActivity
import com.example.habitflow.ui.FullScreenRoutineActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class RoutineAlarmReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        val id=intent.getLongExtra("routine_id",-1)
        val name=intent.getStringExtra("routine_name")?:"Routine"
        val result=goAsync()
        CoroutineScope(Dispatchers.IO).launch{
            val app=context.applicationContext as HabitFlowApp
            app.database.routines().getEnabled().firstOrNull{it.id==id}?.let{
                AlarmManagerHelper.schedule(context,it)
            }
            result.finish()
        }
        val full=PendingIntent.getActivity(context,id.toInt(),
            Intent(context,FullScreenRoutineActivity::class.java).apply{
                putExtra("routine_id",id); putExtra("routine_name",name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val postpone=PendingIntent.getBroadcast(context,(id+100000).toInt(),
            Intent(context,RoutineActionReceiver::class.java).apply{
                action=RoutineActionReceiver.POSTPONE
                putExtra("routine_id",id); putExtra("routine_name",name)
            },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val skip=PendingIntent.getBroadcast(context,(id+200000).toInt(),
            Intent(context,RoutineActionReceiver::class.java).apply{
                action=RoutineActionReceiver.SKIP; putExtra("routine_id",id)
            },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open=PendingIntent.getActivity(context,(id+300000).toInt(),
            Intent(context,MainActivity::class.java).apply{putExtra("open_routine",id)},
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if(Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){
            val notification=NotificationCompat.Builder(context,"routine_fullscreen")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(name).setContentText("È ora della tua routine")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM).setAutoCancel(true)
                .setFullScreenIntent(full,true).setContentIntent(open)
                .addAction(0,"Posticipa 10 min",postpone)
                .addAction(0,"Salta per oggi",skip).build()
            NotificationManagerCompat.from(context).notify(id.toInt(),notification)
        }
    }
}

class RoutineActionReceiver:BroadcastReceiver(){
    companion object{const val POSTPONE="POSTPONE_ROUTINE"; const val SKIP="SKIP_ROUTINE"}
    override fun onReceive(context:Context,intent:Intent){
        val id=intent.getLongExtra("routine_id",-1)
        if(intent.action==POSTPONE) AlarmManagerHelper.postpone(context,id,
            intent.getStringExtra("routine_name")?:"Routine")
        else NotificationManagerCompat.from(context).cancel(id.toInt())
    }
}

class BootReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        if(intent.action==Intent.ACTION_BOOT_COMPLETED||intent.action==Intent.ACTION_MY_PACKAGE_REPLACED){
            val result=goAsync()
            CoroutineScope(Dispatchers.IO).launch{
                val app=context.applicationContext as HabitFlowApp
                app.database.routines().getEnabled().forEach{AlarmManagerHelper.schedule(context,it)}
                result.finish()
            }
        }
    }
}

class MorningReceiver:BroadcastReceiver(){
    override fun onReceive(context:Context,intent:Intent){
        if(intent.action!=Intent.ACTION_USER_PRESENT)return
        val result=goAsync()
        CoroutineScope(Dispatchers.IO).launch{
            val app=context.applicationContext as HabitFlowApp
            val today=LocalDate.now().toString()
            if(!app.preferences.wasMorningSent(today)){
                if(Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(context,
                        Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){
                    val routines=app.database.routines().getEnabled().size
                    val notification=NotificationCompat.Builder(context,"morning_summary")
                        .setSmallIcon(android.R.drawable.ic_menu_today)
                        .setContentTitle("Buongiorno")
                        .setContentText("$routines routine previste. Riepilogo della giornata pronto.")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
                    NotificationManagerCompat.from(context).notify(9001,notification)
                    app.preferences.setMorningSent(today)
                }
            }
            result.finish()
        }
    }
}
