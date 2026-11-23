package com.example.habitflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.habitflow.data.AppDatabase
import com.example.habitflow.data.PreferencesRepository

class HabitFlowApp:Application(){
    val database by lazy { AppDatabase.build(this) }
    val preferences by lazy { PreferencesRepository(this) }
    override fun onCreate(){
        super.onCreate()
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            "routine_fullscreen","Routine",NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description="Avvisi urgenti delle routine"
            enableVibration(true); vibrationPattern=longArrayOf(0,180); setSound(null,null)
        })
        manager.createNotificationChannel(NotificationChannel(
            "morning_summary","Riepilogo mattutino",NotificationManager.IMPORTANCE_DEFAULT
        ))
    }
}
