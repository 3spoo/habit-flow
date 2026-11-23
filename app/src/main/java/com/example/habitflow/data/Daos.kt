package com.example.habitflow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name") fun observe():Flow<List<Category>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insert(v:Category):Long
    @Update suspend fun update(v:Category)
    @Delete suspend fun delete(v:Category)
}
@Dao interface RoutineDao {
    @Query("SELECT * FROM routines WHERE enabled=1 ORDER BY hour,minute") fun observe():Flow<List<Routine>>
    @Query("SELECT * FROM routines WHERE enabled=1") suspend fun getEnabled():List<Routine>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insert(v:Routine):Long
    @Update suspend fun update(v:Routine)
    @Delete suspend fun delete(v:Routine)
}
@Dao interface ActionDao {
    @Query("SELECT * FROM routine_actions WHERE routineId=:id ORDER BY position") fun observe(id:Long):Flow<List<RoutineAction>>
    @Query("SELECT * FROM routine_actions WHERE routineId=:id ORDER BY position") suspend fun get(id:Long):List<RoutineAction>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insert(v:RoutineAction):Long
    @Update suspend fun update(v:RoutineAction)
    @Delete suspend fun delete(v:RoutineAction)
}
@Dao interface TrackingDao {
    @Query("SELECT * FROM tracking ORDER BY name") fun observe():Flow<List<TrackingActivity>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insert(v:TrackingActivity):Long
    @Update suspend fun update(v:TrackingActivity)
    @Delete suspend fun delete(v:TrackingActivity)
    @Query("SELECT * FROM tracking_entries WHERE activityId=:aid AND date=:date") suspend fun entry(aid:Long,date:String):TrackingEntry?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun saveEntry(v:TrackingEntry)
    @Query("SELECT * FROM tracking_entries WHERE activityId=:aid") fun entries(aid:Long):Flow<List<TrackingEntry>>
    @Query("SELECT * FROM pause_periods WHERE activityId=:aid") suspend fun pauses(aid:Long):List<PausePeriod>
    @Insert suspend fun insertPause(v:PausePeriod):Long
    @Delete suspend fun deletePause(v:PausePeriod)
}
@Dao interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY name") fun observeExams():Flow<List<Exam>>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insertExam(v:Exam):Long
    @Update suspend fun updateExam(v:Exam)
    @Delete suspend fun deleteExam(v:Exam)
    @Query("SELECT * FROM exam_dates WHERE examId=:id") fun observeDates(id:Long):Flow<List<ExamDate>>
    @Insert suspend fun insertDate(v:ExamDate):Long
    @Delete suspend fun deleteDate(v:ExamDate)
    @Query("SELECT * FROM exam_topics WHERE examId=:id ORDER BY position") fun observeTopics(id:Long):Flow<List<ExamTopic>>
    @Insert suspend fun insertTopic(v:ExamTopic):Long
    @Update suspend fun updateTopic(v:ExamTopic)
    @Delete suspend fun deleteTopic(v:ExamTopic)
}
