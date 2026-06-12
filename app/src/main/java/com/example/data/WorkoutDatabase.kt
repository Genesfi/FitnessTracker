package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "workout_counts")
data class WorkoutCount(
    @PrimaryKey val dayType: String, // "LEG", "PUSH", "PULL"
    val name: String,
    val emoji: String,
    val count: Int
)

@Entity(tableName = "workout_history_logs")
data class WorkoutHistoryLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

@Entity(tableName = "workout_reminders")
data class WorkoutReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val time: String, // formatted as "HH:mm"
    val label: String,
    val days: String, // comma separated days of week e.g. "Senin, Rabu"
    val isActive: Boolean = true
)

@Entity(tableName = "app_secret_lock")
data class AppSecretLock(
    @PrimaryKey val key: String = "scoreboard_lock",
    val passcode: String = "1234",
    val isLocked: Boolean = true
)

@Entity(tableName = "exercise_checklist")
data class ExerciseChecklistItem(
    @PrimaryKey val name: String,
    val category: String, // "LEG", "PUSH", "PULL"
    val note: String = "",
    val isCompleted: Boolean = false
)

@Entity(tableName = "protein_intake")
data class ProteinIntake(
    @PrimaryKey val proteinType: String, // "EGGS", "FISH", "PEA"
    val count: Int = 0,
    val date: String // "yyyy-MM-dd"
)

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayType: String,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "weekly_schedule")
data class WeeklySchedule(
    @PrimaryKey val dayName: String, // "Senin", "Selasa", etc.
    val activity: String,
    val order: Int
)

@Dao
interface WorkoutDao {
    // Workout counts query and modifications
    @Query("SELECT * FROM workout_counts")
    fun getAllCounts(): Flow<List<WorkoutCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCount(workoutCount: WorkoutCount)

    @Query("SELECT * FROM workout_counts WHERE dayType = :dayType LIMIT 1")
    suspend fun getCountByDayType(dayType: String): WorkoutCount?

    // Workout history logs
    @Query("SELECT * FROM workout_history_logs ORDER BY timestamp DESC")
    fun getAllHistoryLogs(): Flow<List<WorkoutHistoryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryLog(log: WorkoutHistoryLog)

    @Query("DELETE FROM workout_history_logs WHERE id = :id")
    suspend fun deleteHistoryLog(id: Int)

    @Query("UPDATE workout_history_logs SET isSynced = :isSynced WHERE id = :id")
    suspend fun updateLogSyncStatus(id: Int, isSynced: Boolean)

    // Workout Reminders
    @Query("SELECT * FROM workout_reminders ORDER BY id DESC")
    fun getAllReminders(): Flow<List<WorkoutReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: WorkoutReminder)

    @Query("DELETE FROM workout_reminders WHERE id = :id")
    suspend fun deleteReminder(id: Int)

    @Query("UPDATE workout_reminders SET isActive = :isActive WHERE id = :id")
    suspend fun updateReminderStatus(id: Int, isActive: Boolean)

    // App Secret Lock
    @Query("SELECT * FROM app_secret_lock WHERE `key` = 'scoreboard_lock' LIMIT 1")
    fun getSecretLockFlow(): Flow<AppSecretLock?>

    @Query("SELECT * FROM app_secret_lock WHERE `key` = 'scoreboard_lock' LIMIT 1")
    suspend fun getSecretLockDirect(): AppSecretLock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecretLock(lock: AppSecretLock)

    // Exercise Checklist
    @Query("SELECT * FROM exercise_checklist")
    fun getAllExerciseChecklist(): Flow<List<ExerciseChecklistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateExerciseChecklist(item: ExerciseChecklistItem)

    @androidx.room.Delete
    suspend fun deleteExerciseChecklist(item: ExerciseChecklistItem)

    @Query("UPDATE exercise_checklist SET isCompleted = 0")
    suspend fun resetExerciseChecklist()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseChecklist(items: List<ExerciseChecklistItem>)

    // Protein Intake
    @Query("SELECT * FROM protein_intake WHERE date = :date")
    fun getProteinIntakeForDate(date: String): Flow<List<ProteinIntake>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProteinIntake(intake: ProteinIntake)

    // Workout Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions")
    fun getAllWorkoutSessions(): Flow<List<WorkoutSession>>

    // Weekly Schedule
    @Query("SELECT * FROM weekly_schedule ORDER BY `order` ASC")
    fun getWeeklySchedule(): Flow<List<WeeklySchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklySchedule(schedule: List<WeeklySchedule>)

    @Query("UPDATE weekly_schedule SET activity = :activity WHERE dayName = :dayName")
    suspend fun updateWeeklyActivity(dayName: String, activity: String)
}

@Database(
    entities = [
        WorkoutCount::class,
        WorkoutHistoryLog::class,
        WorkoutReminder::class,
        AppSecretLock::class,
        ExerciseChecklistItem::class,
        ProteinIntake::class,
        WorkoutSession::class,
        WeeklySchedule::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun dao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
