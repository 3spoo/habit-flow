package com.example.habitflow.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.habitflow.data.Routine
import java.util.Calendar

object AlarmManagerHelper{
    fun schedule(context:Context,routine:Routine){
        val calendar=Calendar.getInstance().apply{
            set(Calendar.HOUR_OF_DAY,routine.hour); set(Calendar.MINUTE,routine.minute)
            set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
            if(timeInMillis<=System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR,1)
        }
        repeat(8){
            val androidDay=calendar.get(Calendar.DAY_OF_WEEK)
            val maskIndex=when(androidDay){
                Calendar.MONDAY->0; Calendar.TUESDAY->1; Calendar.WEDNESDAY->2
                Calendar.THURSDAY->3; Calendar.FRIDAY->4; Calendar.SATURDAY->5
                else->6
            }
            if((routine.daysMask and (1 shl maskIndex))!=0) return@repeat
            calendar.add(Calendar.DAY_OF_YEAR,1)
        }
        val intent=Intent(context,RoutineAlarmReceiver::class.java).apply{
            putExtra("routine_id",routine.id); putExtra("routine_name",routine.name)
        }
        val pending=PendingIntent.getBroadcast(context,routine.id.toInt(),intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm=context.getSystemService(AlarmManager::class.java)
        try{
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,pending)
        }catch(_:SecurityException){
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,pending)
        }
    }
    fun postpone(context:Context,routineId:Long,name:String){
        val intent=Intent(context,RoutineAlarmReceiver::class.java).apply{
            putExtra("routine_id",routineId); putExtra("routine_name",name)
        }
        val pending=PendingIntent.getBroadcast(context,routineId.toInt(),intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+10*60*1000,pending)
    }
    fun cancel(context:Context,routineId:Long){
        val pending=PendingIntent.getBroadcast(context,routineId.toInt(),
            Intent(context,RoutineAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).cancel(pending)
    }
}
