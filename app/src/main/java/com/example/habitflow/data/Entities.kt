package com.example.habitflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(@PrimaryKey(autoGenerate=true) val id:Long=0, val name:String, val colorIndex:Int=0)

@Entity(tableName = "routines")
data class Routine(@PrimaryKey(autoGenerate=true) val id:Long=0, val name:String,
    val categoryId:Long?=null, val colorIndex:Int=0, val daysMask:Int=127,
    val hour:Int=8, val minute:Int=0, val enabled:Boolean=true)

@Entity(tableName = "routine_actions")
data class RoutineAction(@PrimaryKey(autoGenerate=true) val id:Long=0, val routineId:Long,
    val title:String, val position:Int, val completedDate:String?=null)

@Entity(tableName = "tracking")
data class TrackingActivity(@PrimaryKey(autoGenerate=true) val id:Long=0, val name:String,
    val categoryId:Long?=null, val colorIndex:Int=0, val iconName:String="check",
    val daysMask:Int=127, val frequencyType:String="DAILY", val target:Int=1,
    val durationSeconds:Int=0, val remainingSeconds:Int=0)

@Entity(tableName = "tracking_entries")
data class TrackingEntry(@PrimaryKey(autoGenerate=true) val id:Long=0, val activityId:Long,
    val date:String, val count:Int=0, val remainingSeconds:Int?=null)

@Entity(tableName = "pause_periods")
data class PausePeriod(@PrimaryKey(autoGenerate=true) val id:Long=0, val activityId:Long,
    val startDate:String, val endDate:String)

@Entity(tableName = "exams")
data class Exam(@PrimaryKey(autoGenerate=true) val id:Long=0, val name:String,
    val cfu:Int=6, val notes:String="")

@Entity(tableName = "exam_dates")
data class ExamDate(@PrimaryKey(autoGenerate=true) val id:Long=0, val examId:Long, val date:String)

@Entity(tableName = "exam_topics")
data class ExamTopic(@PrimaryKey(autoGenerate=true) val id:Long=0, val examId:Long,
    val title:String, val position:Int, val completed:Boolean=false)
