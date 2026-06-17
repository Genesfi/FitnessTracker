package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class WorkoutRepository(private val dao: WorkoutDao) {

    private val firestore: FirebaseFirestore? by lazy { 
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null } 
    }
    private val auth: FirebaseAuth? by lazy { 
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null } 
    }

    private val _syncStatus = MutableStateFlow("Siap Sinkron")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    val allCounts: Flow<List<WorkoutCount>> = dao.getAllCounts()
    val allHistoryLogs: Flow<List<WorkoutHistoryLog>> = dao.getAllHistoryLogs()
    val allReminders: Flow<List<WorkoutReminder>> = dao.getAllReminders()
    val secretLockFlow: Flow<AppSecretLock?> = dao.getSecretLockFlow()
    val allExerciseChecklist: Flow<List<ExerciseChecklistItem>> = dao.getAllExerciseChecklist()
    val allWorkoutSessions: Flow<List<WorkoutSession>> = dao.getAllWorkoutSessions()
    val weeklySchedule: Flow<List<WeeklySchedule>> = dao.getWeeklySchedule()
    val allChatHistory: Flow<List<ChatHistory>> = dao.getAllChatHistory()

    fun getProteinIntakeForDate(date: String): Flow<List<ProteinIntake>> = dao.getProteinIntakeForDate(date)

    // Seeds the database if it is empty
    suspend fun seedDatabase() {
        // Seed workout counts - For NEW users, start with 0. 
        // Existing data from cloud will overwrite this during sync.
        val existingCounts = dao.getAllCounts().firstOrNull()
        if (existingCounts.isNullOrEmpty()) {
            dao.insertOrUpdateCount(WorkoutCount("LEG", "Leg Day (Kaki)", "🦵", 0))
            dao.insertOrUpdateCount(WorkoutCount("PUSH", "Push Day (Dada/Bahu/Trisep)", "💪", 0))
            dao.insertOrUpdateCount(WorkoutCount("PULL", "Pull Day (Punggung/Bisep)", "🦾", 0))
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
                // MINGGU PERTAMA (Week 1 / Odd)
                // Push Day (Week 1)
                ExerciseChecklistItem("Push Up di Atas Kursi (Incline)", "PUSH", "3 Set", weekType = 1),
                ExerciseChecklistItem("Dumbbell Shoulder Press", "PUSH", "3 Set", weekType = 1),
                ExerciseChecklistItem("Overhead Triceps Extension", "PUSH", "3 Set", weekType = 1),
                ExerciseChecklistItem("Lying Leg Raises", "PUSH", "3 Set", weekType = 1),
                
                // Pull Day (Week 1)
                ExerciseChecklistItem("Dumbbell Row", "PULL", "4 Set", weekType = 1),
                ExerciseChecklistItem("Reverse Flys", "PULL", "3 Set", weekType = 1),
                ExerciseChecklistItem("Hammer Curls", "PULL", "3 Set", weekType = 1),
                ExerciseChecklistItem("Plank", "PULL", "3 Set", weekType = 1),

                // Leg Day (Week 1)
                ExerciseChecklistItem("Goblet Squats", "LEG", "3 Set", weekType = 1),
                ExerciseChecklistItem("Alternating Lunges", "LEG", "3 Set", weekType = 1),
                ExerciseChecklistItem("Standing Calf Raises", "LEG", "3 Set", weekType = 1),

                // MINGGU KEDUA (Week 2 / Even)
                // Push Day (Week 2)
                ExerciseChecklistItem("Close Grip / Knee Push Up", "PUSH", "3 Set", weekType = 2),
                ExerciseChecklistItem("Flys to Hexpress", "PUSH", "3 Set", weekType = 2),
                ExerciseChecklistItem("Dumbbell Side Raises", "PUSH", "3 Set", weekType = 2),
                ExerciseChecklistItem("Lying Leg Raises", "PUSH", "3 Set", weekType = 2),

                // Pull Day (Week 2)
                ExerciseChecklistItem("Dumbbell Pull Over", "PULL", "4 Set", weekType = 2),
                ExerciseChecklistItem("Seated Curls", "PULL", "3 Set", weekType = 2),
                ExerciseChecklistItem("21s (Bicep)", "PULL", "2 Set", weekType = 2),
                ExerciseChecklistItem("Plank", "PULL", "3 Set", weekType = 2),

                // Leg Day (Week 2)
                ExerciseChecklistItem("Front Squats", "LEG", "3 Set", weekType = 2),
                ExerciseChecklistItem("Stiff Legged Deadlift", "LEG", "3 Set", weekType = 2),
                ExerciseChecklistItem("Lying Leg Curls", "LEG", "3 Set", weekType = 2)
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
        dao.insertWorkoutSession(WorkoutSession(dayType = dayType, date = System.currentTimeMillis()))
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

    suspend fun clearDuplicateProteinIntake() {
        // This is a helper to clean up if needed, but the unique index should handle it now
        // For existing duplicates, we might need a more complex query or just wipe and let sync restore
    }

    suspend fun repairData(targetLeg: Int, targetPush: Int, targetPull: Int) {
        // 1. Update counts
        dao.insertOrUpdateCount(WorkoutCount("LEG", "Leg Day (Kaki)", "🦵", targetLeg))
        dao.insertOrUpdateCount(WorkoutCount("PUSH", "Push Day (Dada/Bahu/Trisep)", "💪", targetPush))
        dao.insertOrUpdateCount(WorkoutCount("PULL", "Pull Day (Punggung/Bisep)", "🦾", targetPull))
        
        // 2. Clear ALL existing sessions to reset history to a clean state
        dao.deleteAllSessions()
        
        // 3. Add exactly 1 session for June to keep it "Active"
        dao.insertWorkoutSession(WorkoutSession(dayType = "PULL", date = System.currentTimeMillis()))
        
        // 4. Add history log
        addHistoryLog("Repair: Data sesi total diperbaiki ke L:$targetLeg, P:$targetPush, PL:$targetPull. Riwayat sesi detail di-reset.")

        syncToCloud()
    }

    suspend fun deleteProteinIntake(id: Int) {
        dao.deleteProteinIntakeById(id)
        syncToCloud()
    }

    suspend fun decrementCount(dayType: String) {
        val current = dao.getCountByDayType(dayType) ?: return
        if (current.count > 0) {
            dao.insertOrUpdateCount(current.copy(count = current.count - 1))
            
            // Cheat prevention: Remove the last session recorded today
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
            
            val lastSession = dao.getLastSessionToday(dayType, startOfDay, endOfDay)
            if (lastSession != null) {
                dao.deleteSessionById(lastSession.id)
            }
            
            syncToCloud()
        }
    }

    suspend fun updateCount(dayType: String, newCount: Int) {
        val current = dao.getCountByDayType(dayType) ?: return
        val oldCount = current.count
        val diff = newCount - oldCount
        
        dao.insertOrUpdateCount(current.copy(count = newCount))
        
        if (diff > 0) {
            // Add sessions to current month/year (Safety limit: max 100 at once)
            val toAdd = diff.coerceAtMost(100)
            repeat(toAdd) {
                dao.insertWorkoutSession(WorkoutSession(dayType = dayType, date = System.currentTimeMillis()))
            }
        } else if (diff < 0) {
            // Remove sessions (newest first)
            val toRemove = (-diff).coerceAtMost(100)
            val sessions = dao.getSessionsByDayType(dayType, toRemove)
            sessions.forEach { dao.deleteSessionById(it.id) }
        }

        syncToCloud()
    }

    suspend fun syncFromCloud(onDataRestored: (name: String, avatar: String, theme: String?) -> Unit = { _, _, _ -> }) {
        val currentAuth = auth ?: return
        val currentFirestore = firestore ?: return
        
        val userId = currentAuth.currentUser?.uid ?: return
        _syncStatus.value = "Menghubungkan ke Cloud..."
        try {
            val snapshot = currentFirestore.collection("users").document(userId).collection("data").document("full_backup").get().await()
            if (snapshot.exists()) {
                val data = snapshot.data ?: return
                
                // 1. Restore Profile & Preferences
                val profileData = data["profile"] as? Map<String, String>
                val restoredName = profileData?.get("name")
                val restoredAvatar = profileData?.get("avatar")
                
                val preferences = data["preferences"] as? Map<String, String>
                val restoredTheme = preferences?.get("theme")

                if (restoredName != null && restoredAvatar != null) {
                    onDataRestored(restoredName, restoredAvatar, restoredTheme)
                }

                // 2. Restore Workout Counts
                val countsData = data["counts"] as? Map<String, Long>
                countsData?.forEach { (dayType, count) ->
                    val name = when(dayType) {
                        "LEG" -> "Leg Day (Kaki)"
                        "PUSH" -> "Push Day (Dada/Bahu/Trisep)"
                        "PULL" -> "Pull Day (Punggung/Bisep)"
                        else -> "Workout"
                    }
                    val emoji = when(dayType) {
                        "LEG" -> "🦵"
                        "PUSH" -> "💪"
                        "PULL" -> "🦾"
                        else -> "🏋️"
                    }
                    dao.insertOrUpdateCount(WorkoutCount(dayType, name, emoji, count.toInt()))
                }

                // 3. Restore Exercise Checklist
                val checklistData = data["checklist"] as? List<Map<String, Any>>
                checklistData?.map { 
                    ExerciseChecklistItem(
                        name = it["name"] as String,
                        category = it["category"] as String,
                        note = it["note"] as String,
                        isCompleted = it["isCompleted"] as Boolean,
                        weekType = (it["weekType"] as? Long)?.toInt() ?: 0
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
                        emoji = it["emoji"] as? String ?: "🥚",
                        count = (it["count"] as Long).toInt(),
                        target = (it["target"] as? Long)?.toInt() ?: 0,
                        unit = it["unit"] as? String ?: "Butir",
                        date = it["date"] as String
                    ))
                }

                // 7. Restore Workout Sessions (if available)
                val sessionsData = data["sessions"] as? List<Map<String, Any>>
                sessionsData?.forEach {
                    dao.insertWorkoutSession(WorkoutSession(
                        dayType = it["dayType"] as String,
                        date = it["date"] as Long
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

    suspend fun syncToCloud(profile: Map<String, String>? = null, preferences: Map<String, String>? = null) {
        val currentAuth = auth ?: return
        val currentFirestore = firestore ?: return
        
        val userId = currentAuth.currentUser?.uid ?: return
        _syncStatus.value = "Menyimpan ke Cloud..."
        try {
            val counts = dao.getAllCounts().firstOrNull() ?: emptyList()
            val checklist = dao.getAllExerciseChecklist().firstOrNull() ?: emptyList()
            val reminders = dao.getAllReminders().firstOrNull() ?: emptyList()
            val schedule = dao.getWeeklySchedule().firstOrNull() ?: emptyList()
            val proteinIntakes = dao.getAllProteinIntake()
            val sessions = dao.getAllWorkoutSessions().firstOrNull() ?: emptyList()

            // Construct backup map
            val backupMap = mutableMapOf<String, Any>(
                "counts" to counts.associate { it.dayType to it.count },
                "checklist" to checklist.map { mapOf("name" to it.name, "category" to it.category, "note" to it.note, "isCompleted" to it.isCompleted, "weekType" to it.weekType) },
                "reminders" to reminders.map { mapOf("time" to it.time, "label" to it.label, "days" to it.days, "isActive" to it.isActive) },
                "schedule" to schedule.map { mapOf("dayName" to it.dayName, "activity" to it.activity, "order" to it.order) },
                "protein" to proteinIntakes.map { 
                    mapOf(
                        "proteinType" to it.proteinType,
                        "emoji" to it.emoji,
                        "count" to it.count,
                        "target" to it.target,
                        "unit" to it.unit,
                        "date" to it.date
                    )
                },
                "sessions" to sessions.map { mapOf("dayType" to it.dayType, "date" to it.date) },
                "last_updated" to System.currentTimeMillis()
            )

            // Include profile if provided
            profile?.let { backupMap["profile"] = it }
            // Include preferences if provided
            preferences?.let { backupMap["preferences"] = it }

            currentFirestore.collection("users").document(userId).collection("data").document("full_backup")
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

    suspend fun addChatHistory(message: ChatHistory) {
        dao.insertChatHistory(message)
    }

    suspend fun clearChatHistory() {
        dao.deleteAllChatHistory()
    }

    suspend fun getSecretLockDirect(): AppSecretLock? {
        return dao.getSecretLockDirect()
    }

    suspend fun updateSecretLock(lock: AppSecretLock) {
        dao.insertSecretLock(lock)
    }
}
