package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await

class WorkoutRepository(private val dao: WorkoutDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _syncStatus = MutableStateFlow("Siap Sinkron")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    val allCounts: Flow<List<WorkoutCount>> = dao.getAllCounts()
    val allHistoryLogs: Flow<List<WorkoutHistoryLog>> = dao.getAllHistoryLogs()
    val allReminders: Flow<List<WorkoutReminder>> = dao.getAllReminders()
    val secretLockFlow: Flow<AppSecretLock?> = dao.getSecretLockFlow()
    val allExerciseChecklist: Flow<List<ExerciseChecklistItem>> = dao.getAllExerciseChecklist()
    val allWorkoutSessions: Flow<List<WorkoutSession>> = dao.getAllWorkoutSessions()
    val weeklySchedule: Flow<List<WeeklySchedule>> = dao.getWeeklySchedule()

    fun getProteinIntakeForDate(date: String): Flow<List<ProteinIntake>> = dao.getProteinIntakeForDate(date)

    // Seeds the database if it is empty
    suspend fun seedDatabase() {
        // Seed workout counts
        val existingCounts = dao.getAllCounts().firstOrNull()
        if (existingCounts.isNullOrEmpty()) {
            dao.insertOrUpdateCount(WorkoutCount("LEG", "Leg Day (Kaki)", "🦵", 16))
            dao.insertOrUpdateCount(WorkoutCount("PUSH", "Push Day (Dada/Bahu/Trisep)", "💪", 8))
            dao.insertOrUpdateCount(WorkoutCount("PULL", "Pull Day (Punggung/Bisep)", "🦾", 7))
        }

        // Seed weekly schedule
        val existingSchedule = dao.getWeeklySchedule().firstOrNull()
        if (existingSchedule.isNullOrEmpty()) {
            val defaultSchedule = listOf(
                WeeklySchedule("Senin", "Rest Day", 1),
                WeeklySchedule("Selasa", "Push Day", 2),
                WeeklySchedule("Rabu", "Rest Day", 3),
                WeeklySchedule("Kamis", "Leg Day 1", 4),
                WeeklySchedule("Jumat", "Pull Day", 5),
                WeeklySchedule("Sabtu", "Rest Day", 6),
                WeeklySchedule("Minggu", "Leg Day 2", 7)
            )
            dao.insertWeeklySchedule(defaultSchedule)
        }

        // Seed exercise checklist
        val existingChecklist = dao.getAllExerciseChecklist().firstOrNull()
        if (existingChecklist.isNullOrEmpty()) {
            val checklist = listOf(
                // Push Day
                ExerciseChecklistItem("Wide to Close Push Up", "PUSH", "Wall Push Up (Regressi)"),
                ExerciseChecklistItem("Push Up di Atas Kursi", "PUSH", "Incline Push Up"),
                ExerciseChecklistItem("Close Grip Push Up", "PUSH", "Knee Push Up (Regressi)"),
                ExerciseChecklistItem("Flys to Hexpress", "PUSH"),
                ExerciseChecklistItem("Dumbbell Shoulder Press", "PUSH"),
                ExerciseChecklistItem("Dumbbell Side Raises", "PUSH"),
                ExerciseChecklistItem("Overhead Triceps Extension", "PUSH"),
                ExerciseChecklistItem("Lying Leg Raises", "PUSH"),
                
                // Pull Day
                ExerciseChecklistItem("Dumbbell Row", "PULL"),
                ExerciseChecklistItem("Dumbbell Deadlift", "PULL"),
                ExerciseChecklistItem("Dumbbell Pull Over", "PULL"),
                ExerciseChecklistItem("Reverse Flys", "PULL"),
                ExerciseChecklistItem("21s", "PULL"),
                ExerciseChecklistItem("Hammer Curls", "PULL"),
                ExerciseChecklistItem("Seated Curls", "PULL"),
                ExerciseChecklistItem("Plank", "PULL"),

                // Leg Day
                ExerciseChecklistItem("Squats", "LEG"),
                ExerciseChecklistItem("Stiff Legged Deadlift", "LEG"),
                ExerciseChecklistItem("Goblet Squats", "LEG"),
                ExerciseChecklistItem("Alternating Lunges", "LEG"),
                ExerciseChecklistItem("Front Squats", "LEG"),
                ExerciseChecklistItem("Lying Leg Curls", "LEG"),
                ExerciseChecklistItem("Standing Calf Raises", "LEG")
            )
            dao.insertExerciseChecklist(checklist)
        }

        // Seed lock state if empty
        val existingLock = dao.getSecretLockDirect()
        if (existingLock == null) {
            dao.insertSecretLock(AppSecretLock("scoreboard_lock", "1234", isLocked = true))
        }

        // Seed initial log
        val existingLogs = dao.getAllHistoryLogs().firstOrNull()
        if (existingLogs.isNullOrEmpty()) {
            dao.insertHistoryLog(
                WorkoutHistoryLog(
                    note = "Memulai pelacak Gusti Workout Tracker. Akumulasi awal: Leg Day (16x), Push Day (8x), Pull Day (7x).",
                    timestamp = System.currentTimeMillis(),
                    isSynced = true
                )
            )
        }
    }

    suspend fun incrementCount(dayType: String) {
        val current = dao.getCountByDayType(dayType) ?: return
        dao.insertOrUpdateCount(current.copy(count = current.count + 1))
        dao.insertWorkoutSession(WorkoutSession(dayType = dayType))
        syncToCloud()
    }

    suspend fun updateExerciseChecklist(item: ExerciseChecklistItem) {
        dao.updateExerciseChecklist(item)
        syncToCloud()
    }

    suspend fun deleteExerciseChecklist(item: ExerciseChecklistItem) {
        dao.deleteExerciseChecklist(item)
        syncToCloud()
    }

    suspend fun addExerciseChecklist(item: ExerciseChecklistItem) {
        dao.updateExerciseChecklist(item)
        syncToCloud()
    }

    suspend fun resetExerciseChecklist() {
        dao.resetExerciseChecklist()
        syncToCloud()
    }

    suspend fun updateProteinIntake(intake: ProteinIntake) {
        dao.updateProteinIntake(intake)
        syncToCloud()
    }

    suspend fun decrementCount(dayType: String) {
        val current = dao.getCountByDayType(dayType) ?: return
        if (current.count > 0) {
            dao.insertOrUpdateCount(current.copy(count = current.count - 1))
            syncToCloud()
        }
    }

    suspend fun updateCount(dayType: String, newCount: Int) {
        val current = dao.getCountByDayType(dayType) ?: return
        dao.insertOrUpdateCount(current.copy(count = newCount))
        syncToCloud()
    }

    suspend fun syncFromCloud(onProfileRestored: (String, String) -> Unit = { _, _ -> }) {
        val userId = auth.currentUser?.uid ?: return
        _syncStatus.value = "Menghubungkan ke Cloud..."
        try {
            val snapshot = firestore.collection("users").document(userId).collection("data").document("full_backup").get().await()
            if (snapshot.exists()) {
                val data = snapshot.data ?: return
                
                // 1. Restore Profile (Custom Name & Avatar)
                val profileData = data["profile"] as? Map<String, String>
                val restoredName = profileData?.get("name")
                val restoredAvatar = profileData?.get("avatar")
                if (restoredName != null && restoredAvatar != null) {
                    onProfileRestored(restoredName, restoredAvatar)
                }

                // 2. Restore Workout Counts
                val countsData = data["counts"] as? Map<String, Long>
                countsData?.forEach { (dayType, count) ->
                    val current = dao.getCountByDayType(dayType)
                    if (current != null) dao.insertOrUpdateCount(current.copy(count = count.toInt()))
                }

                // 3. Restore Exercise Checklist
                val checklistData = data["checklist"] as? List<Map<String, Any>>
                checklistData?.map { 
                    ExerciseChecklistItem(
                        name = it["name"] as String,
                        category = it["category"] as String,
                        note = it["note"] as String,
                        isCompleted = it["isCompleted"] as Boolean
                    )
                }?.let { dao.insertExerciseChecklist(it) }

                // 4. Restore Reminders
                val remindersData = data["reminders"] as? List<Map<String, Any>>
                remindersData?.forEach {
                    dao.insertReminder(WorkoutReminder(
                        time = it["time"] as String,
                        label = it["label"] as String,
                        days = it["days"] as String,
                        isActive = it["isActive"] as Boolean
                    ))
                }

                // 5. Restore Weekly Schedule
                val scheduleData = data["schedule"] as? List<Map<String, Any>>
                scheduleData?.map {
                    WeeklySchedule(
                        dayName = it["dayName"] as String,
                        activity = it["activity"] as String,
                        order = (it["order"] as Long).toInt()
                    )
                }?.let { dao.insertWeeklySchedule(it) }

                // 6. Restore Protein Intake
                val proteinData = data["protein"] as? List<Map<String, Any>>
                proteinData?.forEach {
                    dao.updateProteinIntake(ProteinIntake(
                        proteinType = it["proteinType"] as String,
                        count = (it["count"] as Long).toInt(),
                        date = it["date"] as String
                    ))
                }

                _syncStatus.value = "Data Berhasil Dipulihkan"
                addHistoryLog("Sync: Berhasil mengunduh pencadangan penuh dari Cloud.")
            } else {
                _syncStatus.value = "Cloud Kosong (Data Baru)"
            }
        } catch (e: Exception) {
            _syncStatus.value = "Gagal Sinkron: ${e.localizedMessage}"
            addHistoryLog("Sync Error: ${e.localizedMessage}")
        }
    }

    suspend fun syncToCloud(profile: Map<String, String>? = null) {
        val userId = auth.currentUser?.uid ?: return
        _syncStatus.value = "Menyimpan ke Cloud..."
        try {
            val counts = dao.getAllCounts().firstOrNull() ?: emptyList()
            val checklist = dao.getAllExerciseChecklist().firstOrNull() ?: emptyList()
            val reminders = dao.getAllReminders().firstOrNull() ?: emptyList()
            val schedule = dao.getWeeklySchedule().firstOrNull() ?: emptyList()

            // Construct backup map
            val backupMap = mutableMapOf<String, Any>(
                "counts" to counts.associate { it.dayType to it.count },
                "checklist" to checklist.map { mapOf("name" to it.name, "category" to it.category, "note" to it.note, "isCompleted" to it.isCompleted) },
                "reminders" to reminders.map { mapOf("time" to it.time, "label" to it.label, "days" to it.days, "isActive" to it.isActive) },
                "schedule" to schedule.map { mapOf("dayName" to it.dayName, "activity" to it.activity, "order" to it.order) },
                "last_updated" to System.currentTimeMillis()
            )

            // Include profile if provided, otherwise keep existing or placeholder
            profile?.let { backupMap["profile"] = it }

            firestore.collection("users").document(userId).collection("data").document("full_backup")
                .set(backupMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { _syncStatus.value = "Tersimpan di Cloud" }
                .addOnFailureListener { _syncStatus.value = "Gagal Menyimpan" }
        } catch (e: Exception) {
            _syncStatus.value = "Gagal Backup: ${e.localizedMessage}"
        }
    }

    suspend fun addHistoryLog(note: String) {
        dao.insertHistoryLog(WorkoutHistoryLog(note = note))
    }

    suspend fun deleteHistoryLog(id: Int) {
        dao.deleteHistoryLog(id)
    }

    suspend fun updateLogSyncStatus(id: Int, isSynced: Boolean) {
        dao.updateLogSyncStatus(id, isSynced)
    }

    suspend fun addReminder(time: String, label: String, days: String) {
        dao.insertReminder(WorkoutReminder(time = time, label = label, days = days))
        syncToCloud()
    }

    suspend fun deleteReminder(id: Int) {
        dao.deleteReminder(id)
        syncToCloud()
    }

    suspend fun updateReminderStatus(id: Int, isActive: Boolean) {
        dao.updateReminderStatus(id, isActive)
        syncToCloud()
    }

    suspend fun updateWeeklyActivity(dayName: String, activity: String) {
        dao.updateWeeklyActivity(dayName, activity)
        syncToCloud()
    }

    suspend fun getSecretLockDirect(): AppSecretLock? {
        return dao.getSecretLockDirect()
    }

    suspend fun updateSecretLock(lock: AppSecretLock) {
        dao.insertSecretLock(lock)
    }
}
