package com.example.habitflow.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.habitFlowDataStore by preferencesDataStore("habit_flow_settings")

class PreferencesRepository(context:Context){
    private val store=context.habitFlowDataStore
    val darkTheme:Flow<Boolean> = store.data.map { it[dark] ?: false }
    val studentMode:Flow<Boolean> = store.data.map { it[student] ?: false }
    val paletteIndex:Flow<Int> = store.data.map { it[palette] ?: 0 }
    suspend fun setDarkTheme(v:Boolean)=store.edit{it[dark]=v}
    suspend fun setStudentMode(v:Boolean)=store.edit{it[student]=v}
    suspend fun setPaletteIndex(v:Int)=store.edit{it[palette]=v}
    suspend fun setMorningSent(date:String)=store.edit{it[morningDate]=date}
    suspend fun wasMorningSent(date:String)=store.data.first()[morningDate]==date
    companion object {
        private val dark=booleanPreferencesKey("dark_theme")
        private val student=booleanPreferencesKey("student_mode")
        private val palette=intPreferencesKey("palette_index")
        private val morningDate=stringPreferencesKey("morning_date")
    }
}
