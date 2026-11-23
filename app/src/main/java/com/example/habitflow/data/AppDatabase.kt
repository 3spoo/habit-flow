package com.example.habitflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities=[Category::class,Routine::class,RoutineAction::class,TrackingActivity::class,
        TrackingEntry::class,PausePeriod::class,Exam::class,ExamDate::class,ExamTopic::class],
    version=1, exportSchema=false
)
abstract class AppDatabase:RoomDatabase(){
    abstract fun categories():CategoryDao
    abstract fun routines():RoutineDao
    abstract fun actions():ActionDao
    abstract fun tracking():TrackingDao
    abstract fun exams():ExamDao
    companion object{
        fun build(context:Context)=Room.databaseBuilder(context,AppDatabase::class.java,"habit-flow.db").build()
    }
}
