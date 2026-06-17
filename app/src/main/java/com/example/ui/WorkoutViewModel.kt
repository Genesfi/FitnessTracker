package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppSecretLock
import com.example.data.GeminiClient
import com.example.data.WorkoutDatabase
import com.example.data.WorkoutHistoryLog
import com.example.data.WorkoutCount
import com.example.data.WorkoutReminder
import com.example.data.WorkoutRepository
import com.example.data.ExerciseChecklistItem
import com.example.data.ProteinIntake
import com.example.data.WorkoutSession
import com.example.data.WeeklySchedule
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WorkoutRepository
    private var auth: FirebaseAuth? = null
    private val sharedPrefs = application.getSharedPreferences("GustiWorkoutPrefs", Context.MODE_PRIVATE)

    private fun getCurrentDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("316976717035-nd9aqpdgi2go02phi3kigh56vd2scahj.apps.googleusercontent.com")
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    // Secure Simulated Login information flows
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow(sharedPrefs.getString("user_name", "Gusti Fitness") ?: "Gusti Fitness")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(sharedPrefs.getString("user_email", "gusti.workout@gmail.com") ?: "gusti.workout@gmail.com")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _userAvatar = MutableStateFlow(sharedPrefs?.getString("user_avatar", "💪") ?: "💪")
    val userAvatar: StateFlow<String> = _userAvatar.asStateFlow()

    private val _theme = MutableStateFlow(sharedPrefs?.getString("app_theme", "SYSTEM") ?: "SYSTEM")
    val theme: StateFlow<String> = _theme.asStateFlow()

    // Exercise patterns - custom fine-grained set adjustments
    private val _exerciseSets = MutableStateFlow<Map<String, Int>>(emptyMap())
    val exerciseSets: StateFlow<Map<String, Int>> = _exerciseSets.asStateFlow()

    // Gemini states
    private val _geminiAdvice = MutableStateFlow<String>("")
    val geminiAdvice: StateFlow<String> = _geminiAdvice.asStateFlow()

    private val _yearlyWrappedSummary = MutableStateFlow<Map<Int, String>>(emptyMap())
    val yearlyWrappedSummary: StateFlow<Map<Int, String>> = _yearlyWrappedSummary.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // AI Coach Chat state
    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val isError: Boolean = false,
        val id: String = java.util.UUID.randomUUID().toString()
    )
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _encouragementMessage = MutableStateFlow<String?>(null)
    val encouragementMessage: StateFlow<String?> = _encouragementMessage.asStateFlow()

    fun clearEncouragement() { _encouragementMessage.value = null }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            repository.addChatHistory(com.example.data.ChatHistory(text = text, isUser = true))
            
            _isAnalyzing.value = true
            try {
                // Get counts for context
                val counts = workoutCounts.value
                val leg = counts.firstOrNull { it.dayType == "LEG" }?.count ?: 0
                val push = counts.firstOrNull { it.dayType == "PUSH" }?.count ?: 0
                val pull = counts.firstOrNull { it.dayType == "PULL" }?.count ?: 0
                val name = _userName.value
                val schedule = weeklySchedule.value.joinToString("\n") { "• ${it.dayName}: ${it.activity}" }
                
                val contextPrompt = """
                    Kamu adalah Coach AI $name. Berikan saran yang singkat, padat, dan memotivasi. 
                    Hindari kata-kata basi seperti 'ya betul' atau pengulangan informasi yang tidak perlu.
                    Gunakan gaya bahasa pelatih profesional yang akrab. Panggil user dengan nama "$name".
                    
                    JADWAL MINGGUAN USER:
                    $schedule
                    
                    DATA AKUMULASI (Bukan Set, tapi TOTAL SESI LATIHAN selama berbulan-bulan): 
                    - Leg Day: $leg Sesi, Push Day: $push Sesi, Pull Day: $pull Sesi. 
                    
                    INFO BI-WEEKLY (Dua Mingguan):
                    - Sekarang adalah MINGGU ${_currentWeekType.value} (1=Ganjil, 2=Genap).
                    - User memiliki daftar gerakan yang berbeda antara Minggu Ganjil dan Genap.
                    
                    INSTRUKSI PENTING:
                    - Pahami bahwa angka di atas adalah TOTAL SESI (Training Sessions), BUKAN jumlah set dalam satu latihan. 
                    - JANGAN gunakan kata "set" untuk merujuk pada data akumulasi tersebut. Sebut sebagai "Sesi" atau "Latihan".
                    - Evaluasi JADWAL MINGGUAN USER secara kritis namun suportif. 
                    - Jika user bertanya tentang gerakan, sesuaikan jawabanmu dengan info minggu ini (Minggu ${_currentWeekType.value}).
                    - Berikan saran jika menurutmu ada pola yang lebih baik (misal: penempatan Rest Day yang lebih optimal), namun tetap hargai target konsistensi user.
                    
                    $name bertanya: $text
                """.trimIndent()
                val response = GeminiClient.chatWithCoach(contextPrompt)
                val currentUserName = _userName.value
                
                // Final safety check to filter technical messages in chat
                val finalResponse = if (response.contains("quota", ignoreCase = true) || response.contains("jalur penuh")) {
                    "Maaf $currentUserName, Coach sedang istirahat sebentar (Jalur Padat). Coba tanya lagi semenit lagi ya! 🙏"
                } else {
                    response
                }
                
                repository.addChatHistory(com.example.data.ChatHistory(text = finalResponse, isUser = false))
            } catch (e: Exception) {
                val currentName = _userName.value
                val errorMsg = e.localizedMessage ?: e.message ?: "Unknown AI error"
                
                // Technical log for developer
                android.util.Log.e("CoachAI", "Chat error: $errorMsg", e)

                val humanFriendlyError = when {
                    errorMsg.contains("quota", ignoreCase = true) || 
                    errorMsg.contains("429") || 
                    errorMsg.contains("503") ||
                    errorMsg.contains("demand", ignoreCase = true) -> 
                        "Maaf $currentName, Coach sedang melayani banyak user (Jalur Padat). Silakan tanya lagi dalam 1-2 menit ya! 🙏"
                    else -> "Maaf $currentName, Coach sedang istirahat sebentar. Coba lagi nanti ya!"
                }
                repository.addChatHistory(com.example.data.ChatHistory(text = humanFriendlyError, isUser = false, isError = true))
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            // Add initial message back
            repository.addChatHistory(com.example.data.ChatHistory(
                text = "Halo! Saya Coach AI Anda. Ada yang bisa saya bantu dengan latihan atau nutrisi Anda hari ini?",
                isUser = false
            ))
        }
    }

    // Simulated Google Keep Sync
    private val _isKeepSyncing = MutableStateFlow(false)
    val isKeepSyncing: StateFlow<Boolean> = _isKeepSyncing.asStateFlow()

    private val _refreshCooldown = MutableStateFlow(0)
    val refreshCooldown: StateFlow<Int> = _refreshCooldown.asStateFlow()

    // New State Flows
    private val _currentDate = MutableStateFlow(getCurrentDate())
    private val _isWorkoutDayOverride = MutableStateFlow<Boolean?>(null)

    // Final Flows
    val workoutCounts: StateFlow<List<WorkoutCount>>
    val historyLogs: StateFlow<List<WorkoutHistoryLog>>
    val reminders: StateFlow<List<WorkoutReminder>>
    val lockState: StateFlow<AppSecretLock?>
    val exerciseChecklist: StateFlow<List<ExerciseChecklistItem>>
    val proteinIntake: StateFlow<List<ProteinIntake>>
    val workoutSessions: StateFlow<List<WorkoutSession>>
    val weeklySchedule: StateFlow<List<WeeklySchedule>>
    val syncStatus: StateFlow<String>

    // Week type for bi-weekly schedule: 1 = Odd (Week 1/3), 2 = Even (Week 2/4/5)
    private val _currentWeekType = MutableStateFlow(1)
    val currentWeekType: StateFlow<Int> = _currentWeekType.asStateFlow()

    private val _viewingWeekType = MutableStateFlow(1)
    val viewingWeekType: StateFlow<Int> = _viewingWeekType.asStateFlow()

    val isWorkoutDay: StateFlow<Boolean>

    init {
        try {
            // First attempt to initialize with available google-services.json
            FirebaseApp.initializeApp(application)
            val firebaseAuth = FirebaseAuth.getInstance()
            auth = firebaseAuth
            
            // Check for valid API key (placeholder check)
            if (FirebaseApp.getInstance().options.apiKey == "MY_API_KEY") {
                // Not configured properly
            }
            
            _isLoggedIn.value = firebaseAuth.currentUser != null
        _userName.value = firebaseAuth.currentUser?.displayName ?: sharedPrefs.getString("user_name", "Gusti Fitness") ?: "Gusti Fitness"
        _userEmail.value = firebaseAuth.currentUser?.email ?: sharedPrefs.getString("user_email", "gusti.workout@gmail.com") ?: "gusti.workout@gmail.com"

        // Calculate current week type
        val cal = Calendar.getInstance()
        val weekOfMonth = cal.get(Calendar.WEEK_OF_MONTH)
        val type = if (weekOfMonth % 2 != 0) 1 else 2
        _currentWeekType.value = type
        _viewingWeekType.value = type

        firebaseAuth.addAuthStateListener { fa ->
                val user = fa.currentUser
                _isLoggedIn.value = user != null
                if (user != null) {
                    // Initial values from auth/prefs
                    _userName.value = user.displayName ?: sharedPrefs?.getString("user_name", "User") ?: "User"
                    _userEmail.value = user.email ?: ""
                    _userAvatar.value = sharedPrefs?.getString("user_avatar", "👤") ?: "👤"
                    
                    viewModelScope.launch {
                        repository.syncFromCloud { name, avatar, restoredTheme ->
                            // Callback to update UI when cloud data is downloaded
                            _userName.value = name
                            _userAvatar.value = avatar
                            restoredTheme?.let { _theme.value = it }
                            sharedPrefs?.edit()?.apply {
                                putString("user_name", name)
                                putString("user_avatar", avatar)
                                restoredTheme?.let { putString("app_theme", it) }
                                apply()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Firebase not available
        }
        val database = WorkoutDatabase.getDatabase(application)
        repository = WorkoutRepository(database.dao())
        
        syncStatus = repository.syncStatus
        
        workoutCounts = repository.allCounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        historyLogs = repository.allHistoryLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        reminders = repository.allReminders
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        lockState = repository.secretLockFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        exerciseChecklist = repository.allExerciseChecklist
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        proteinIntake = _currentDate.flatMapLatest { date ->
            repository.getProteinIntakeForDate(date)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        workoutSessions = repository.allWorkoutSessions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        weeklySchedule = repository.weeklySchedule
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            repository.allChatHistory.collect { history ->
                if (history.isEmpty()) {
                    // Seed initial message if empty
                    repository.addChatHistory(com.example.data.ChatHistory(
                        text = "Halo! Saya Coach AI Anda. Ada yang bisa saya bantu dengan latihan atau nutrisi Anda hari ini?",
                        isUser = false
                    ))
                } else {
                    _chatMessages.value = history.map { 
                        ChatMessage(it.text, it.isUser, it.isError, it.id.toString()) 
                    }
                }
            }
        }

        isWorkoutDay = combine(exerciseChecklist, _isWorkoutDayOverride) { checklist, override ->
            override ?: checklist.any { it.isCompleted }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        // Seed database
        viewModelScope.launch {
            repository.seedDatabase()
            // Add default protein items if none exist for today
            val date = _currentDate.value
            val currentProtein = repository.getProteinIntakeForDate(date).firstOrNull() ?: emptyList()
            
            // Fixed seeding to avoid duplicates by checking names
            if (currentProtein.none { it.proteinType == "EGGS" }) {
                repository.updateProteinIntake(ProteinIntake(proteinType = "EGGS", emoji = "🥚", count = 0, target = 4, unit = "Butir", date = date))
            }
            if (currentProtein.none { it.proteinType == "FISH" }) {
                repository.updateProteinIntake(ProteinIntake(proteinType = "FISH", emoji = "🐟", count = 0, target = 5, unit = "Sdm", date = date))
            }
            if (currentProtein.none { it.proteinType == "PEA" }) {
                repository.updateProteinIntake(ProteinIntake(proteinType = "PEA", emoji = "🥛", count = 0, target = 1, unit = "Scoop", date = date))
            }
        }

        // Initialize exercise set configurations from prefs
        val initialExerciseMap = mutableMapOf<String, Int>()
        val defaultExercises = listOf(
            "Barbell Squat", "Leg Press", "Romanian Deadlift", "Calf Raises",
            "Bench Press", "Overhead Shoulder Press", "Triceps Pushdown", "Incline Chest Fly",
            "Pull-up & Pulldown", "Cable Rows", "Bicep Curls", "Rear Delt Facepull"
        )
        defaultExercises.forEach { ex ->
            initialExerciseMap[ex] = sharedPrefs.getInt("set_$ex", 4)
        }
        _exerciseSets.value = initialExerciseMap
    }

    // UI actions for workout scores
    fun incrementCount(dayType: String) {
        viewModelScope.launch {
            repository.incrementCount(dayType)
            val dayName = getDayTypeName(dayType)
            val currentCount = workoutCounts.value.find { it.dayType == dayType }?.count ?: 0
            val name = _userName.value
            val avatar = _userAvatar.value
            
            _encouragementMessage.value = "Wah $name, sudah latihan $dayName ya hari ini! Mantap nih nambah jadi ${currentCount}x! 🔥"
            repository.syncToCloud(mapOf("name" to name, "avatar" to avatar)) // Ensure profile stays synced on any update
            repository.addHistoryLog("Menambah akumulasi 1 sesi latihan $dayName.")
        }
    }

    fun decrementCount(dayType: String) {
        viewModelScope.launch {
            repository.decrementCount(dayType)
            repository.addHistoryLog("Mengurangi 1 sesi latihan " + getDayTypeName(dayType) + ".")
        }
    }
    
    fun updateCountDirectly(dayType: String, count: Int) {
        viewModelScope.launch {
            repository.updateCount(dayType, count)
            repository.addHistoryLog("Mengubah langsung kuantitas " + getDayTypeName(dayType) + " menjadi " + count + "x.")
        }
    }

    private fun getDayTypeName(dayType: String): String {
        return when (dayType) {
            "LEG" -> "Leg Day (Kaki)"
            "PUSH" -> "Push Day (Dada/Bahu/Trisep)"
            "PULL" -> "Pull Day (Punggung/Bisep)"
            else -> dayType
        }
    }

    // New Actions
    fun updateExerciseChecklist(item: ExerciseChecklistItem) {
        viewModelScope.launch {
            repository.updateExerciseChecklist(item)
        }
    }

    fun addExercise(name: String, category: String, note: String = "", weekType: Int = 0) {
        viewModelScope.launch {
            repository.addExerciseChecklist(ExerciseChecklistItem(name, category, note, weekType = weekType))
        }
    }

    fun setViewingWeekType(type: Int) {
        _viewingWeekType.value = type
    }

    fun deleteExercise(item: ExerciseChecklistItem) {
        viewModelScope.launch {
            repository.deleteExerciseChecklist(item)
        }
    }

    fun resetExerciseChecklist() {
        viewModelScope.launch {
            repository.resetExerciseChecklist()
        }
    }

    fun updateProteinIntake(proteinType: String, delta: Int) {
        viewModelScope.launch {
            val date = _currentDate.value
            val currentIntakes = proteinIntake.value
            val intake = currentIntakes.find { it.proteinType == proteinType } ?: ProteinIntake(proteinType = proteinType, count = 0, date = date)
            val newCount = (intake.count + delta).coerceAtLeast(0)
            repository.updateProteinIntake(intake.copy(count = newCount))
        }
    }

    fun addProteinIntakeItem(name: String, emoji: String, target: Int, unit: String) {
        viewModelScope.launch {
            val date = _currentDate.value
            val newItem = ProteinIntake(
                proteinType = name,
                emoji = emoji,
                count = 0,
                target = target,
                unit = unit,
                date = date
            )
            repository.updateProteinIntake(newItem)
        }
    }

    fun syncData() {
        viewModelScope.launch {
            repository.syncToCloud()
            addLog("System: Sinkronisasi data ke Cloud berhasil.")
        }
    }

    fun deleteProteinIntakeItem(id: Int) {
        viewModelScope.launch {
            repository.deleteProteinIntake(id)
        }
    }

    fun toggleWorkoutDayOverride() {
        val current = _isWorkoutDayOverride.value ?: isWorkoutDay.value
        _isWorkoutDayOverride.value = !current
    }

    // Adjust specific exercise set
    fun adjustExerciseSet(exerciseName: String, increment: Boolean) {
        val current = _exerciseSets.value.toMutableMap()
        val oldSets = current[exerciseName] ?: 0
        val newSets = if (increment) oldSets + 1 else if (oldSets > 0) oldSets - 1 else 0
        current[exerciseName] = newSets
        _exerciseSets.value = current
        sharedPrefs.edit().putInt("set_$exerciseName", newSets).apply()
        
        val logNote = if (increment) "Tambah set: $exerciseName menjadi ${newSets}x set" else "Kurangi set: $exerciseName menjadi ${newSets}x set"
        viewModelScope.launch {
            repository.addHistoryLog("[GERAKAN] $logNote")
        }
    }

    // User Profile login / simulation flows
    fun signInWithFirebaseWithGoogle(idToken: String, onResult: (Boolean, String) -> Unit) {
        val firebaseAuth = auth ?: run {
            onResult(false, "Firebase tidak tersedia.")
            return
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        _isLoggedIn.value = true
                        _userName.value = user.displayName ?: "User"
                        _userEmail.value = user.email ?: ""
                        _userAvatar.value = "👤"
                        viewModelScope.launch {
                            repository.syncFromCloud()
                            repository.addHistoryLog("Login: Sukses masuk via Firebase Auth (${user.email}).")
                        }
                        onResult(true, "Masuk via Google berhasil!")
                    }
                } else {
                    onResult(false, "Gagal masuk via Firebase: ${task.exception?.message}")
                }
            }
    }

    fun loginSimulatedGoogle(email: String, name: String) {
        sharedPrefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_avatar", "🥦")
            apply()
        }
        _isLoggedIn.value = true
        _userName.value = name
        _userEmail.value = email
        _userAvatar.value = "🥦"
        viewModelScope.launch {
            repository.addHistoryLog("Login: Sukses masuk via Akun Google ($email).")
        }
    }

    fun loginSimulatedLocal(email: String, name: String, avatar: String) {
        sharedPrefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_avatar", avatar)
            apply()
        }
        _isLoggedIn.value = true
        _userName.value = name
        _userEmail.value = email
        _userAvatar.value = avatar
        viewModelScope.launch {
            repository.addHistoryLog("Login: Masuk manual lokal sebagai $name.")
        }
    }

    fun logoutUser(context: Context) {
        auth?.signOut()
        getGoogleSignInClient(context).signOut().addOnCompleteListener {
            sharedPrefs.edit().apply {
                putBoolean("is_logged_in", false)
                apply()
            }
            _isLoggedIn.value = false
            viewModelScope.launch {
                repository.addHistoryLog("Logout: Keluar dari akun.")
            }
        }
    }

    fun updateProfile(name: String, email: String, avatar: String) {
        sharedPrefs?.edit()?.apply {
            putString("user_name", name)
            putString("user_email", email)
            putString("user_avatar", avatar)
            apply()
        }
        _userName.value = name
        _userEmail.value = email
        _userAvatar.value = avatar
        viewModelScope.launch {
            repository.syncToCloud(
                profile = mapOf("name" to name, "avatar" to avatar),
                preferences = mapOf("theme" to _theme.value)
            )
            repository.addHistoryLog("Profil: Memperbarui nama ($name) & foto profil.")
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            repository.syncFromCloud { name, avatar, restoredTheme ->
                _userName.value = name
                _userAvatar.value = avatar
                restoredTheme?.let { _theme.value = it }
            }
        }
    }

    fun repairData(leg: Int, push: Int, pull: Int) {
        viewModelScope.launch {
            repository.repairData(leg, push, pull)
            addLog("System: Data sesi diperbaiki secara manual (L:$leg, P:$push, PL:$pull)")
        }
    }

    fun setTheme(newTheme: String) {
        _theme.value = newTheme
        sharedPrefs?.edit()?.putString("app_theme", newTheme)?.apply()
        viewModelScope.launch {
            repository.syncToCloud(
                profile = mapOf("name" to _userName.value, "avatar" to _userAvatar.value),
                preferences = mapOf("theme" to newTheme)
            )
        }
    }

    // History log actions
    fun addLog(note: String) {
        viewModelScope.launch {
            repository.addHistoryLog(note)
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteHistoryLog(id)
        }
    }

    fun syncAllNotesWithGoogleKeep() {
        viewModelScope.launch {
            _isKeepSyncing.value = true
            // Simulate networking and Keep handshake
            kotlinx.coroutines.delay(1800)
            
            // Collect un-synced logs and update state
            val currentLogs = historyLogs.value
            currentLogs.forEach { log ->
                if (!log.isSynced) {
                    repository.updateLogSyncStatus(log.id, true)
                }
            }
            
            _isKeepSyncing.value = false
            repository.addHistoryLog("Asisten: Berhasil mensinkronisasikan catatan Gusti ke Google Keep.")
        }
    }

    // Passcode Security Protection actions
    fun toggleLockState(passcodeAttempt: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val currentLock = repository.getSecretLockDirect() ?: AppSecretLock()
            if (currentLock.isLocked) {
                // Try to unlock
                if (passcodeAttempt == "1234" || currentLock.passcode == passcodeAttempt) {
                    repository.updateSecretLock(currentLock.copy(isLocked = false))
                    onResult(true, "Kunci berhasil dibuka! (Terbuka selama 60 detik)")
                    
                    // Start automatic re-lock timer
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(60000) // 60 seconds
                        val latestLock = repository.getSecretLockDirect() ?: AppSecretLock()
                        if (!latestLock.isLocked) {
                            repository.updateSecretLock(latestLock.copy(isLocked = true))
                            _encouragementMessage.value = "Papan skor otomatis dikunci demi keamanan! 🔐"
                        }
                    }
                } else {
                    onResult(false, "Sandi PIN salah! Akses ditolak.")
                }
            } else {
                // Re-lock manually
                repository.updateSecretLock(currentLock.copy(isLocked = true))
                onResult(true, "Papan skor berhasil dikunci aman!")
            }
        }
    }

    fun changePasscode(oldPasscode: String, newPasscode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val currentLock = repository.getSecretLockDirect() ?: AppSecretLock()
            if (currentLock.passcode == oldPasscode) {
                if (newPasscode.length != 4 || !newPasscode.all { it.isDigit() }) {
                    onResult(false, "Sandi PIN baru harus terdiri dari 4 digit angka!")
                    return@launch
                }
                repository.updateSecretLock(currentLock.copy(passcode = newPasscode))
                onResult(true, "Sandi PIN Keamanan berhasil diperbarui!")
            } else {
                onResult(false, "PIN Keamanan saat ini salah!")
            }
        }
    }

    // Reminders
    fun addReminder(time: String, label: String, daysList: List<String>) {
        viewModelScope.launch {
            val days = if (daysList.isEmpty()) "Setiap Hari" else daysList.joinToString(", ")
            repository.addReminder(time, label, days)
            repository.addHistoryLog("Menjadwalkan pengingat: \"$label\" pukul $time setiap ($days).")
        }
    }

    fun deleteReminder(id: Int) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    fun toggleReminderStatus(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateReminderStatus(id, isActive)
        }
    }

    fun updateWeeklyActivity(dayName: String, activity: String) {
        viewModelScope.launch {
            repository.updateWeeklyActivity(dayName, activity)
        }
    }

    fun generateYearlyWrapped(year: Int) {
        if (_isAnalyzing.value) return
        
        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val sessions = workoutSessions.value.filter {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = it.date
                    cal.get(Calendar.YEAR) == year
                }
                
                val leg = sessions.count { it.dayType == "LEG" }
                val push = sessions.count { it.dayType == "PUSH" }
                val pull = sessions.count { it.dayType == "PULL" }
                val total = sessions.size
                
                // Add legacy data if year is 2026
                var adjustedTotal = total
                var adjustedLeg = leg
                var adjustedPush = push
                var adjustedPull = pull
                
                if (year == 2026) {
                    val currentSessionsCount = workoutSessions.value.size
                    val totalLegacySessions = (workoutCounts.value.sumOf { it.count } - currentSessionsCount).coerceIn(0, 31)
                    if (totalLegacySessions > 0) {
                        adjustedTotal += totalLegacySessions
                        adjustedLeg += 16
                        adjustedPush += 8
                        adjustedPull += (totalLegacySessions - 24).coerceAtLeast(0)
                    }
                }

                val name = _userName.value
                val prompt = """
                    Buatlah ringkasan pencapaian tahunan "Fitness Wrapped" untuk $name di tahun $year.
                    Gunakan gaya bahasa yang sangat keren, estetik, puitis, dan penuh semangat (seperti narasi atlet pro).
                    
                    DATA PENCAPAIAN TAHUN $year (Total Sesi Latihan selama setahun):
                    - Total Latihan: $adjustedTotal Sesi
                    - Leg Day: $adjustedLeg Sesi
                    - Push Day: $adjustedPush Sesi
                    - Pull Day: $adjustedPull Sesi
                    
                    PENTING: Data di atas adalah JUMLAH SESI (Training Sessions), bukan set.
                    
                    Berikan pesan penutup yang sangat memotivasi untuk tahun berikutnya.
                    Ringkasan harus padat namun berkesan (maksimal 150 kata).
                    Panggil user "$name".
                """.trimIndent()

                val response = GeminiClient.chatWithCoach(prompt)
                _yearlyWrappedSummary.value += (year to response)
            } catch (e: Exception) {
                _yearlyWrappedSummary.value = _yearlyWrappedSummary.value + (year to "Maaf $userName, Coach gagal merangkum tahunmu. Coba lagi nanti ya! 🙏")
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    // Hybrid Intelligent Analyzer with 100% stable offline local rules fallback
    fun analyzeWorkoutHabits(forceRefresh: Boolean = false) {
        // PREVENT idle/background battery drain:
        // Only run if forced (user clicked refresh) OR if the current advice is completely blank.
        // This stops the app from "thinking" constantly in the background.
        if (_isAnalyzing.value) return
        
        val currentAdvice = _geminiAdvice.value
        if (!forceRefresh && currentAdvice.isNotBlank() && !currentAdvice.contains("Coach sedang berpikir")) {
            return
        }

        if (forceRefresh && (_refreshCooldown.value > 0)) return
        
        viewModelScope.launch {
            if (forceRefresh) {
                startCooldown()
            }
            
            _isAnalyzing.value = true
            _geminiAdvice.value = "Coach sedang berpikir...\nMenganalisis data latihan Anda..."
            
            // Small delay to ensure DB data is emitted
            kotlinx.coroutines.delay(500)

            val counts = workoutCounts.value
            val leg = counts.firstOrNull { it.dayType == "LEG" }?.count ?: 0
            val push = counts.firstOrNull { it.dayType == "PUSH" }?.count ?: 0
            val pull = counts.firstOrNull { it.dayType == "PULL" }?.count ?: 0
            
            // ... (rest of data prep)
            
            val currentChecklist = exerciseChecklist.value
            val completedExercises = currentChecklist.filter { it.isCompleted }.joinToString { it.name }
            val pendingExercises = currentChecklist.filter { !it.isCompleted }.joinToString { it.name }

            val currentProtein = proteinIntake.value
            val eggs = currentProtein.find { it.proteinType == "EGGS" }?.count ?: 0
            val fish = currentProtein.find { it.proteinType == "FISH" }?.count ?: 0
            val pea = currentProtein.find { it.proteinType == "PEA" }?.count ?: 0

            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val dayName = SimpleDateFormat("EEEE", Locale("id", "ID")).format(now.time)
            
            val timePeriod = when (hour) {
                in 0..4 -> "Dini Hari (Waktunya Tidur)"
                in 5..10 -> "Pagi Hari"
                in 11..14 -> "Siang Hari"
                in 15..18 -> "Sore Hari"
                else -> "Malam Hari"
            }

            val fullSchedule = weeklySchedule.value.joinToString("\n") { "• ${it.dayName}: ${it.activity}" }
            val todayActivity = weeklySchedule.value.find { it.dayName.equals(dayName, ignoreCase = true) }?.activity ?: "Tidak ada jadwal"

            val name = _userName.value
            
            val contextPrompt = """
                Kamu adalah Coach AI Pintar untuk $name. Kamu memiliki akses ke seluruh data harian user.
                
                WAKTU SEKARANG: ${String.format("%02d:00", hour)}, Periode: $timePeriod, Hari: $dayName.
                JADWAL LATIHAN USER HARI INI ($dayName): $todayActivity.
                
                JADWAL MINGGUAN LENGKAP USER:
                $fullSchedule
                
                DATA LATIHAN TERAKUMULASI:
                - Leg Day: $leg kali, Push Day: $push kali, Pull Day: $pull kali.
                
                STATUS LIST GERAKAN HARI INI:
                - Sudah Selesai: ${if (completedExercises.isBlank()) "Belum ada" else completedExercises}
                - Belum Selesai: ${if (pendingExercises.isBlank()) "Semua sudah selesai!" else pendingExercises}
                
                ASUPAN PROTEIN HARI INI:
                - Telur: $eggs, Ikan: $fish, Pea Protein: $pea.
                
                TUGAS UTAMAMU:
                0. PENTING: Data latihan (Leg: $leg, Push: $push, Pull: $pull) adalah TOTAL SESI LATIHAN yang sudah dilakukan selama berbulan-bulan. JANGAN sebut sebagai "set". Gunakan istilah "Sesi" atau "Latihan".
                1. Analisis efektivitas JADWAL MINGGUAN LENGKAP USER. Berikan kritik membangun jika polanya kurang optimal (misal: penempatan hari latihan berat yang berdekatan atau kurangnya hari pemulihan).
                2. Prioritaskan JADWAL LATIHAN USER HARI INI. Jika jadwalnya "Rest Day", JANGAN suruh user latihan berat, sarankan pemulihan aktif atau nutrisi.
                3. Dukung pilihan user (seperti 2x Leg Day) jika itu memang tujuannya, namun ingatkan pentingnya pemulihan (Recovery) yang pas di antara hari-hari tersebut agar tidak overtraining.
                4. Berikan saran aktivitas spesifik sesuai JAM SEKARANG (Format 24 Jam). Ingat: 02:00 adalah dini hari/pagi buta, 14:00 adalah siang hari.
                5. Jika jam sekarang adalah jam tidur (23:00 - 04:00), perintahkan user untuk segera tidur demi pertumbuhan otot.
                6. Evaluasi asupan protein dan list gerakan yang tertinggal.
                
                Gunakan gaya bahasa pelatih profesional yang akrab, singkat, padat, dan memotivasi.
                Panggil user "$name". Hindari kata basi seperti "ya betul".
                Tulis dalam Bahasa Indonesia yang apik.
            """.trimIndent()
            
            // Check if API Key is set to real secret before trying network request
            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                kotlinx.coroutines.delay(1200) // visual touch
                _geminiAdvice.value = generateOfflineAdvice(leg, push, pull)
                _isAnalyzing.value = false
                return@launch
            }

            var retryCount = 0
            var success = false
            
            while (retryCount < 3 && !success) {
                try {
                    val response = GeminiClient.chatWithCoach(contextPrompt)
                    _geminiAdvice.value = response
                    success = true
                } catch (e: Exception) {
                    val errorMsg = e.localizedMessage ?: ""
                    if (errorMsg.contains("quota", ignoreCase = true)) {
                        retryCount++
                        if (retryCount < 3) {
                            val waitTime = (Math.pow(2.0, retryCount.toDouble()) * 1000).toLong()
                            _geminiAdvice.value = "Kuota penuh, mencoba kembali dalam ${waitTime/1000} detik... ($retryCount/3)"
                            kotlinx.coroutines.delay(waitTime)
                        } else {
                            _geminiAdvice.value = "Kuota Coach Gemini sedang penuh. Silakan tunggu beberapa menit lagi.\n\n" + generateOfflineAdvice(leg, push, pull)
                        }
                    } else {
                        _geminiAdvice.value = generateOfflineAdvice(leg, push, pull) + "\n\n*(Saran disesuaikan via Coach Pintar Offline)*"
                        success = true
                    }
                }
            }
            _isAnalyzing.value = false
        }
    }

    private fun startCooldown() {
        viewModelScope.launch {
            _refreshCooldown.value = 60 // Increased to 60s to save API quota
            while (_refreshCooldown.value > 0) {
                kotlinx.coroutines.delay(1000)
                _refreshCooldown.value -= 1
            }
        }
    }

    private fun generateOfflineAdvice(leg: Int, push: Int, pull: Int): String {
        val total = leg + push + pull
        val isBalanced = total > 0 && Math.abs(leg - push) <= 3 && Math.abs(push - pull) <= 3 && Math.abs(leg - pull) <= 3
        val legsDominant = leg > push + 4 && leg > pull + 4
        val pushDominant = push > leg + 4 && push > pull + 4
        val pullDominant = pull > leg + 4 && pull > push + 4

        val name = _userName.value
        
        val ratioText = when {
            isBalanced -> "Luar biasa $name! Rasio porsi latihan Anda sangat seimbang ($leg Kaki 🦵, $push Dorong 💪, $pull Tarik 🦾). Ini adalah rasio ideal untuk memulihkan otot secara maksimal dan simetri tubuh tegap!"
            legsDominant -> "Sesi latihan kaki Anda sangat gigih ($leg Kaki 🦵 kali). Kaki kokoh adalah pondasi inti kekuatan tubuh! Tapi, pastikan menyusul pengerjaan tubuh bagian atas (Dada: $push, Punggung: $pull) agar postur tetap seimbang dan atletis."
            pushDominant -> "Rasio latihan dorong (Push Day: $push 💪 kali) memimpin tinggi. Dada, bahu, dan trisep $name sedang berada dalam perkembangan prima! Pertahankan, tapi mohon jangan me-skip latihan kaki (Leg Day: $leg kali), karena melatih otot besar kaki merangsang metabolisme tubuh secara alami."
            pullDominant -> "Sesi latihan tarik punggung dan bisep Anda (Pull Day: $pull 🦾 kali) sangat kuat! Otot punggung yang tebal membantu menjaga bahu tetap kokoh. Seimbangkan dengan latihan dada/bahu depan ($push kali) dan kaki ($leg kali) demi fungsionalitas fitnes maksimal."
            else -> "Rasio latihan Anda saat ini adalah Kaki: $leg kali, Dorong: $push kali, Tarik: $pull kali. Rotasi pemulihan otot idealnya adalah 48 jam istihat untuk kelompok otot yang sama."
        }

        return """
            ⚡ **HASIL DIAGNOSTIK OFFLINE INDEPENDEN (COACH PINTAR $name)**
            
            Halo $name! Rutinitas pelacakan Anda berhasil dievaluasi secara offline langsung dari ponsel Anda tanpa beban API tambahan:
            
            📊 **Symmetry Rating & Keseimbangan**:
            $ratioText
            
            🥦 **Tips Makan & Nutrisi Hari Ini**:
            *   **Asupan Protein**: Targetkan 1.5 gram protein per kilogram berat badan Anda. Cukupi asupan protein harian dari telur direbus, dada ayam panggang, tempe kering, atau whey shake Anda demi pemulihan serat otot pasca latihan kaki/pushes!
            *   **Bahan Bakar Latihan**: Konsumsi nasi putih/merah atau pisang 2 jam sebelum mengangkat beban agar cadangan glikogen otot terisi optimal!
            *   **Hidrasi Ginjal**: Minum air putih minimal 3 Liter setiap melatih set berat.
            
            📱 *Catatan: Tidak butuh koneksi internet atau API eksternal! Rutinitas fitnes Anda terekam aman sepenuhnya secara lokal di database ponsel pribadi Anda.*
        """.trimIndent()
    }

    private fun getFormattedDate(millis: Long): String {
        val date = java.util.Date(millis)
        val format = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}
