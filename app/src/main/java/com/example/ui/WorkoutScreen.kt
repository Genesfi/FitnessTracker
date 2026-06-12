package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.example.data.WorkoutCount
import com.example.data.WorkoutHistoryLog
import com.example.data.WorkoutReminder
import com.example.data.WeeklySchedule
import com.example.data.ExerciseChecklistItem
import com.example.data.ProteinIntake
import com.example.data.WorkoutSession
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private enum class WorkoutTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Star),
    PPL_MENU("List Gerakan", Icons.Filled.List),
    AI_COACH("AI Coach", Icons.Filled.Face),
    PROTEIN("Asupan Protein", Icons.Filled.ShoppingCart),
    STATISTICS("Rekap & Stats", Icons.Filled.Notifications),
    PROFILE("Akun & Login", Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State flows
    val counts by viewModel.workoutCounts.collectAsStateWithLifecycle()
    val logs by viewModel.historyLogs.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val lockState by viewModel.lockState.collectAsStateWithLifecycle()
    val advice by viewModel.geminiAdvice.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val isKeepSyncing by viewModel.isKeepSyncing.collectAsStateWithLifecycle()

    // New State Flows
    val exerciseChecklist by viewModel.exerciseChecklist.collectAsStateWithLifecycle()
    val proteinIntake by viewModel.proteinIntake.collectAsStateWithLifecycle()
    val workoutSessions by viewModel.workoutSessions.collectAsStateWithLifecycle()
    val isWorkoutDay by viewModel.isWorkoutDay.collectAsStateWithLifecycle()
    val encouragementMessage by viewModel.encouragementMessage.collectAsStateWithLifecycle()

    // Local UI State
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { WorkoutTab.values().size })

    LaunchedEffect(encouragementMessage) {
        encouragementMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearEncouragement()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        // Optional: Do something when page changes
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { token ->
                viewModel.signInWithFirebaseWithGoogle(token) { success, message ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            } ?: run {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Gagal mendapatkan ID Token dari Google.")
                }
            }
        } catch (e: ApiException) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Kesalahan Sign-In Google: ${e.statusCode}")
            }
        }
    }

    fun showBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewModel.toggleLockState("1234") { success, message -> // Bypass check logic in VM for demo/simplicity or use a special unlock method
                    coroutineScope.launch { snackbarHostState.showSnackbar("Fingerprint Verified: $message") }
                }
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                coroutineScope.launch { snackbarHostState.showSnackbar("Biometric Error: $errString") }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Kunci Papan Skor")
            .setSubtitle("Gunakan sidik jari atau kunci layar Anda")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Simulated local login state and set tracking
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()
    val exerciseSets by viewModel.exerciseSets.collectAsStateWithLifecycle()

    // Dialog flags
    var showPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showDirectEditCountDialog by remember { mutableStateOf<WorkoutCount?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf<String?>(null) } // Category ID
    var newLogText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Brand Panel
            HeaderPanel(
                isLocked = lockState?.isLocked ?: true,
                userName = userName,
                userAvatar = userAvatar,
                onLockClick = {
                    val currentlyLocked = lockState?.isLocked ?: true
                    if (currentlyLocked) {
                        showPinDialog = true
                    } else {
                        viewModel.toggleLockState("") { success, message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    }
                },
                onChangePinClick = {
                    if (lockState?.isLocked == false) {
                        showChangePinDialog = true
                    }
                },
                onBiometricUnlock = { showBiometricPrompt() }
            )

            SmartClockPanel()

            // Dynamic Tab selector pills
            TabNavigationRow(
                currentTab = WorkoutTab.values()[pagerState.currentPage],
                onTabSelected = { tab ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(tab.ordinal)
                    }
                }
            )

            // Content matching selected tab
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                pageSpacing = 16.dp,
                verticalAlignment = Alignment.Top
            ) { page ->
                val tab = WorkoutTab.values()[page]
                when (tab) {
                    WorkoutTab.DASHBOARD -> {
                        val refreshCooldown by viewModel.refreshCooldown.collectAsStateWithLifecycle()
                        DashboardTabContent(
                            counts = counts,
                            workoutSessions = workoutSessions,
                            isLocked = lockState?.isLocked ?: true,
                            advice = advice,
                            onAnalyze = { viewModel.analyzeWorkoutHabits(forceRefresh = false) },
                            onRefreshAdvice = { viewModel.analyzeWorkoutHabits(forceRefresh = true) },
                            refreshCooldown = refreshCooldown,
                            onIncrement = { viewModel.incrementCount(it.dayType) },
                            onDecrement = { viewModel.decrementCount(it.dayType) },
                            onDirectEdit = { showDirectEditCountDialog = it },
                            onLockedActionAttempt = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Papan Skor terkunci aman! Tekan tombol gembok atau sidik jari untuk mengedit.")
                                }
                            }
                        )
                    }
                    WorkoutTab.PPL_MENU -> {
                        PPLMenuTabContent(
                            checklist = exerciseChecklist,
                            onToggle = { viewModel.updateExerciseChecklist(it) },
                            onReset = { viewModel.resetExerciseChecklist() },
                            onAddExercise = { category -> showAddExerciseDialog = category },
                            onDeleteExercise = { viewModel.deleteExercise(it) }
                        )
                    }
                    WorkoutTab.AI_COACH -> {
                        val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
                        val isChatAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
                        AICoachChatTabContent(
                            messages = chatMessages,
                            isAnalyzing = isChatAnalyzing,
                            onSendMessage = { viewModel.sendMessage(it) }
                        )
                    }
                    WorkoutTab.PROTEIN -> {
                        ProteinIntakeTabContent(
                            intake = proteinIntake,
                            isWorkoutDay = isWorkoutDay,
                            onUpdateIntake = { type, delta -> viewModel.updateProteinIntake(type, delta) },
                            onToggleWorkoutDay = { viewModel.toggleWorkoutDayOverride() }
                        )
                    }
                    WorkoutTab.STATISTICS -> {
                        val weeklySchedule by viewModel.weeklySchedule.collectAsStateWithLifecycle()
                        RecapsTabContent(
                            counts = counts,
                            reminders = reminders,
                            weeklySchedule = weeklySchedule,
                            onUpdateWeeklyActivity = { day, activity -> viewModel.updateWeeklyActivity(day, activity) },
                            onAddReminderClick = { showAddReminderDialog = true },
                            onDeleteReminder = { viewModel.deleteReminder(it) },
                            onToggleReminder = { id, active -> viewModel.toggleReminderStatus(id, active) }
                        )
                    }
                    WorkoutTab.PROFILE -> {
                        val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
                        ProfileTabContent(
                            isLoggedIn = isLoggedIn,
                            userName = userName,
                            userEmail = userEmail,
                            userAvatar = userAvatar,
                            totalSessions = counts.sumOf { it.count },
                            syncStatus = syncStatus,
                            onTriggerGoogleLogin = {
                                googleSignInLauncher.launch(viewModel.getGoogleSignInClient(context).signInIntent)
                            },
                            onLoginLocal = { email, name, avatar ->
                                viewModel.loginSimulatedLocal(email, name, avatar)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Masuk via Akun Lokal berhasil!")
                                }
                            },
                            onLogout = {
                                viewModel.logoutUser(context)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Keluar dari akun.")
                                }
                            },
                            onUpdateProfile = { name, email, avatar ->
                                viewModel.updateProfile(name, email, avatar)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Profil berhasil diperbarui!")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Exercise Dialog
    showAddExerciseDialog?.let { category ->
        AddExerciseDialog(
            category = category,
            onDismiss = { showAddExerciseDialog = null },
            onSubmit = { name, note ->
                viewModel.addExercise(name, category, note)
                showAddExerciseDialog = null
            }
        )
    }

    // PIN Authentication Dialog for unlocking training stats
    if (showPinDialog) {
        PinDialog(
            title = "Buka Kunci Papan Skor",
            description = "Masukkan PIN 4 digit untuk mengubah papan skor akumulasi latihan Anda (PIN default: 1234).",
            onDismiss = { showPinDialog = false },
            onSubmit = { code ->
                viewModel.toggleLockState(code) { success, message ->
                    if (success) {
                        showPinDialog = false
                    }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        )
    }

    // Change Secure PIN Dialog
    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onSubmit = { oldPin, newPin ->
                viewModel.changePasscode(oldPin, newPin) { success, message ->
                    if (success) {
                        showChangePinDialog = false
                    }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        )
    }


    // Direct Edit Counts Dialog
    showDirectEditCountDialog?.let { workoutCount ->
        DirectEditCountDialog(
            workoutCount = workoutCount,
            onDismiss = { showDirectEditCountDialog = null },
            onSubmit = { countValue ->
                viewModel.updateCountDirectly(workoutCount.dayType, countValue)
                showDirectEditCountDialog = null
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Kuantitas ${workoutCount.name} disesuaikan ke ${countValue}x.")
                }
            }
        )
    }

    // Add Reminder Dialog
    if (showAddReminderDialog) {
        AddReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onSubmit = { time, label, days ->
                viewModel.addReminder(time, label, days)
                showAddReminderDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Pengingat \"$label\" dijadwalkan!")
                }
            }
        )
    }
}

@Composable
private fun SmartClockPanel() {
    var time by remember { mutableStateOf(Calendar.getInstance()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            time = Calendar.getInstance()
            kotlinx.coroutines.delay(1000)
        }
    }

    val hour = time.get(Calendar.HOUR_OF_DAY)
    val minute = time.get(Calendar.MINUTE)
    val dayName = SimpleDateFormat("EEEE", Locale("id", "ID")).format(time.time)
    
    val (icon, advice) = when (hour) {
        in 0..4 -> "😴" to "Waktunya Tidur & Recovery Otot"
        in 5..7 -> "☀️" to "Waktunya Berjemur & Sinar Matahari"
        in 8..10 -> "🍳" to "Jam Sarapan Protein Tinggi"
        in 16..18 -> "🏋️" to "Jam Latihan Efektif (Puncak Energi)"
        in 19..21 -> "🥗" to "Waktunya Makan Malam Nutrisi"
        else -> "💪" to "Tetap Aktif & Jaga Hidrasi"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GymTertiary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%02d:%02d - %s", hour, minute, dayName),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = GymSecondary
                )
                Text(
                    text = advice,
                    fontSize = 11.sp,
                    color = GymSecondary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun HeaderPanel(
    isLocked: Boolean,
    userName: String,
    userAvatar: String,
    onLockClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onBiometricUnlock: () -> Unit
) {
    val context = LocalContext.current
    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circular initial avatar emoji with shadow and light blue background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GymTertiary) // Light blue: #D1E4FF
                    .shadow(1.dp, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userAvatar,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymSecondary // Deep navy: #001D36
                )
            }

            Column {
                Text(
                    text = "Progres $userName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bright green status bullet
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Text(
                        text = "Sinkron Keep & Gemini luring",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }


        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Biometric button
            if (isLocked && canAuthenticate) {
                IconButton(
                    onClick = onBiometricUnlock,
                    modifier = Modifier
                        .size(40.dp)
                        .background(GymTertiary, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Unlock with Fingerprint",
                        tint = GymSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Settings button for passcode
            if (!isLocked) {
                IconButton(
                    onClick = onChangePinClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .testTag("change_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ganti PIN",
                        tint = GymSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Secure Lock Toggle Button
            IconButton(
                onClick = onLockClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isLocked) GymTertiary else GymPrimary.copy(alpha = 0.15f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isLocked) GymSecondary.copy(alpha = 0.15f) else GymPrimary.copy(alpha = 0.2f),
                        CircleShape
                    )
                    .testTag("lock_toggle_button")
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.CheckCircle,
                    contentDescription = "Buka / Kunci Papan Skor",
                    tint = if (isLocked) GymSecondary else GymPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TabNavigationRow(
    currentTab: WorkoutTab,
    onTabSelected: (WorkoutTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = currentTab.ordinal,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp,
        divider = {},
        indicator = {}
    ) {
        WorkoutTab.values().forEach { tab ->
            val isSelected = currentTab == tab
            // Active pill uses #D1E4FF (GymTertiary) in Professional Polish
            val bgPillColor = if (isSelected) GymTertiary else Color.Transparent
            val textPillColor = if (isSelected) GymSecondary else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp, horizontal = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgPillColor)
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("tab_" + tab.name.lowercase())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = textPillColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.title,
                        color = textPillColor,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTabContent(
    counts: List<WorkoutCount>,
    workoutSessions: List<WorkoutSession>,
    isLocked: Boolean,
    advice: String,
    onAnalyze: () -> Unit,
    onRefreshAdvice: () -> Unit,
    refreshCooldown: Int,
    onIncrement: (WorkoutCount) -> Unit,
    onDecrement: (WorkoutCount) -> Unit,
    onDirectEdit: (WorkoutCount) -> Unit,
    onLockedActionAttempt: () -> Unit
) {
    val totalSessions = remember(counts) { counts.sumOf { it.count } }

    LaunchedEffect(Unit) {
        onAnalyze()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = GymTertiary),
            border = BorderStroke(1.dp, Color(0xFFBAC7DB))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "PAPAN SKOR AKUMULASI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GymSecondary.copy(alpha = 0.7f),
                        letterSpacing = 1.2.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isLocked) GymPrimary else Color(0xFF22C55E))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isLocked) "DIKUNCI AMAN" else "SUNTIK DATA AKTIF",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Sesi Latihan", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GymSecondary)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$totalSessions",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = GymSecondary,
                        lineHeight = 54.sp
                    )
                    Text(
                        text = "Total Sesi Latihan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GymSecondary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // List workout counts
        counts.forEach { item ->
            WorkoutItemCard(
                item = item,
                isLocked = isLocked,
                onIncrement = { onIncrement(item) },
                onDecrement = { onDecrement(item) },
                onDirectEdit = { onDirectEdit(item) },
                onLockedActionAttempt = onLockedActionAttempt
            )
        }

        // Recap Section
        Text(
            text = "Recap & Visual Progress",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = GymSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFDEE3EB))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Konsistensi Bulanan",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Simple horizontal bars for last 3 months
                val months = listOf("April", "Mei", "Juni")
                val sessionCounts = listOf(18, 24, totalSessions) // Simulated historical data
                
                months.forEachIndexed { index, month ->
                    val count = sessionCounts[index]
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = month, fontSize = 11.sp, color = Color(0xFF191C1E))
                            Text(text = "$count Sesi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GymPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val progress = if (count > 0) (count / 30f).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GymPrimary,
                            trackColor = Color(0xFFF3F3FA)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Prediksi Tahunan: ${totalSessions * 2} Sesi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymSecondary
                )
            }
        }

        // Gemini Advice
        val displaysAdvice = if (advice.isBlank()) {
            "Gunakan Coach AI untuk mendapatkan saran porsi latihan yang personal!"
        } else {
            // Clean up formatting symbols
            advice.replace("**", "").replace("##", "").replace("*", "•")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F7)),
            border = BorderStroke(1.dp, Color(0xFFC4C6D0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "✨", fontSize = 20.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Coach AI Advisor:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF191C1E)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displaysAdvice,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF44474E)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(
                            onClick = onRefreshAdvice,
                            enabled = refreshCooldown == 0,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = if (refreshCooldown == 0) GymPrimary else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (refreshCooldown > 0) {
                            Text(
                                text = "${refreshCooldown}s",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutItemCard(
    item: WorkoutCount,
    isLocked: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDirectEdit: () -> Unit,
    onLockedActionAttempt: () -> Unit
) {
    val subtext = when (item.dayType.uppercase()) {
        "LEGS" -> "Fokus: Quad, Hamstring"
        "PUSH" -> "Dada, Bahu, Trisep"
        "PULL" -> "Punggung, Bisep"
        else -> "Latihan Pelengkap & Terapi"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .testTag("workout_card_" + item.dayType.lowercase()),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFDEE3EB))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Emoji box with #F3F3FA container background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F3FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = item.emoji, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1E)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtext,
                        fontSize = 12.sp,
                        color = Color(0xFF74777F)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Decrement Button "-" (Only visible when UNLOCKED)
                if (!isLocked) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onDecrement() }
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                            .testTag("decrement_" + item.dayType.lowercase()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "—",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Styled count text (Clickable for direct edit ONLY when UNLOCKED)
                Text(
                    text = "${item.count}x",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = GymPrimary,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(enabled = !isLocked) { onDirectEdit() }
                )

                // Increment Button "+" (ALWAYS visible and clickable for instant recording)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GymPrimary)
                        .clickable { onIncrement() }
                        .testTag("increment_" + item.dayType.lowercase()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Sesi",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PPLMenuTabContent(
    checklist: List<ExerciseChecklistItem>,
    onToggle: (ExerciseChecklistItem) -> Unit,
    onReset: () -> Unit,
    onAddExercise: (String) -> Unit,
    onDeleteExercise: (ExerciseChecklistItem) -> Unit
) {
    val categories = listOf(
        "LEG" to "🦵 Leg Day (Kaki)",
        "PUSH" to "💪 Push Day (Dorong)",
        "PULL" to "🦾 Pull Day (Tarik)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PPL Menu & Checklist",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GymSecondary
            )
            
            TextButton(onClick = onReset) {
                Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }

        categories.forEach { (catId, catName) ->
            val items = checklist.filter { it.category == catId }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFDEE3EB))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = catName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GymPrimary
                        )
                        IconButton(onClick = { onAddExercise(catId) }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = GymPrimary, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (items.isEmpty()) {
                        Text(
                            "Belum ada gerakan.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isCompleted,
                                onCheckedChange = { onToggle(item.copy(isCompleted = it)) },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f).clickable { onToggle(item.copy(isCompleted = !item.isCompleted)) }) {
                                Text(
                                    text = item.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (item.isCompleted) Color.Gray else Color.Black
                                )
                                if (item.note.isNotBlank()) {
                                    Text(
                                        text = item.note,
                                        fontSize = 10.sp,
                                        color = GymPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteExercise(item) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AICoachChatTabContent(
    messages: List<WorkoutViewModel.ChatMessage>,
    isAnalyzing: Boolean,
    onSendMessage: (String) -> Unit
) {
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            if (isAnalyzing) {
                item {
                    Text(
                        "Coach sedang mengetik...",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tanya Coach...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        onSendMessage(textState)
                        textState = ""
                    }
                },
                enabled = !isAnalyzing,
                modifier = Modifier
                    .background(GymPrimary, CircleShape)
                    .size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun ChatBubble(message: WorkoutViewModel.ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) GymPrimary else Color(0xFFF3F3FA)
    val textColor = if (message.isUser) Color.White else Color.Black
    val shape = if (message.isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Simple Markdown-like formatting for bold text
                val formattedText = message.text.replace("**", "")
                Text(
                    text = formattedText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun ProteinIntakeTabContent(
    intake: List<ProteinIntake>,
    isWorkoutDay: Boolean,
    onUpdateIntake: (String, Int) -> Unit,
    onToggleWorkoutDay: () -> Unit
) {
    val eggIntake = intake.find { it.proteinType == "EGGS" }?.count ?: 0
    val fishIntake = intake.find { it.proteinType == "FISH" }?.count ?: 0
    val peaIntake = intake.find { it.proteinType == "PEA" }?.count ?: 0

    val eggTarget = if (isWorkoutDay) 4 else 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Asupan Protein Harian",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GymSecondary
            )
            
            FilterChip(
                selected = isWorkoutDay,
                onClick = onToggleWorkoutDay,
                label = { Text(if (isWorkoutDay) "Hari Latihan" else "Hari Istirahat") },
                leadingIcon = if (isWorkoutDay) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }

        // Egg Tracker
        ProteinItemCard(
            title = "Telur Rebus/Dadar",
            emoji = "🥚",
            count = eggIntake,
            target = eggTarget,
            unit = "Butir",
            onIncrement = { onUpdateIntake("EGGS", 1) },
            onDecrement = { onUpdateIntake("EGGS", -1) }
        )

        // Baby Fish Tracker
        ProteinItemCard(
            title = "Baby Fish Crispy",
            emoji = "🐟",
            count = fishIntake,
            target = 5,
            unit = "Sdm",
            onIncrement = { onUpdateIntake("FISH", 1) },
            onDecrement = { onUpdateIntake("FISH", -1) }
        )

        // Pea Protein Tracker
        ProteinItemCard(
            title = "Pea Protein Isolate",
            emoji = "🥛",
            count = peaIntake,
            target = 1,
            unit = "Scoop",
            onIncrement = { onUpdateIntake("PEA", 1) },
            onDecrement = { onUpdateIntake("PEA", -1) }
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GymTertiary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Target protein harian Anda disesuaikan dengan intensitas aktivitas hari ini.",
                    fontSize = 12.sp,
                    color = GymSecondary
                )
            }
        }
    }
}

@Composable
private fun ProteinItemCard(
    title: String,
    emoji: String,
    count: Int,
    target: Int,
    unit: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDEE3EB))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F3FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Target: $target $unit", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDecrement) {
                    Text("—", fontWeight = FontWeight.Bold)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$count", fontSize = 20.sp, fontWeight = FontWeight.Black, color = GymPrimary)
                    if (count >= target) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                    }
                }

                IconButton(onClick = onIncrement) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = GymPrimary)
                }
            }
        }
    }
}

@Composable
private fun RecapsTabContent(
    counts: List<WorkoutCount>,
    reminders: List<WorkoutReminder>,
    weeklySchedule: List<WeeklySchedule>,
    onUpdateWeeklyActivity: (String, String) -> Unit,
    onAddReminderClick: () -> Unit,
    onDeleteReminder: (Int) -> Unit,
    onToggleReminder: (Int, Boolean) -> Unit
) {
    val totalSessions = remember(counts) { counts.sumOf { it.count } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Rekap & Statistik Latihan",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = GymSecondary
        )

        // Weekly Schedule Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFDEE3EB))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Jadwal Latihan Mingguan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                weeklySchedule.forEach { item ->
                    var isEditing by remember { mutableStateOf(false) }
                    var tempActivity by remember(item.activity) { mutableStateOf(item.activity) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.dayName}:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(60.dp)
                        )
                        
                        if (isEditing) {
                            OutlinedTextField(
                                value = tempActivity,
                                onValueChange = { tempActivity = it },
                                modifier = Modifier.weight(1f).height(48.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        onUpdateWeeklyActivity(item.dayName, tempActivity)
                                        isEditing = false
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = item.activity,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f).clickable { isEditing = true }
                            )
                        }
                    }
                }
            }
        }

        // Monthly breakdown progress bars
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFDEE3EB))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Konsistensi Bulanan (2026)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val months = listOf(
                    "Januari" to 12,
                    "Februari" to 15,
                    "Maret" to 20,
                    "April" to 18,
                    "Mei" to 24,
                    "Juni (Aktif)" to totalSessions
                )

                months.forEach { (mName, mCount) ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = mName, fontSize = 12.sp, color = Color(0xFF191C1E), fontWeight = FontWeight.Medium)
                            Text(text = "$mCount Sesi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GymPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val progress = if (mCount > 0) java.lang.Math.min(1.0f, mCount / 24.0f) else 0f
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = GymPrimary,
                            trackColor = Color(0xFFF3F3FA)
                        )
                    }
                }
            }
        }

        // Annual evaluation trophy card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = GymTertiary),
            border = BorderStroke(1.dp, Color(0xFFBAC7DB))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Rekap Tahunan Atlet Gusti 🏆",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Akumulasi total sesi leg/push/pull Anda adalah sebesar $totalSessions sesi latihan harian. Rasio pemulihan prima & progres fisik terpantau luring secara konsisten!",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = GymSecondary.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Alarms section nested here
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alarm Pengingat Latihan",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GymSecondary
            )

            Button(
                onClick = onAddReminderClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GymPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_reminder_trigger")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pasang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum memasang pengingat alarm latihan.", fontSize = 12.sp, color = Color(0xFF74777F))
            }
        } else {
            reminders.forEach { reminder ->
                ReminderCardItem(
                    reminder = reminder,
                    onDelete = { onDeleteReminder(reminder.id) },
                    onToggle = { active -> onToggleReminder(reminder.id, active) }
                )
            }
        }
    }
}

@Composable
private fun ProfileTabContent(
    isLoggedIn: Boolean,
    userName: String,
    userEmail: String,
    userAvatar: String,
    totalSessions: Int,
    syncStatus: String,
    onTriggerGoogleLogin: () -> Unit,
    onLoginLocal: (String, String, String) -> Unit,
    onLogout: () -> Unit,
    onUpdateProfile: (String, String, String) -> Unit
) {
    var localName by remember { mutableStateOf("") }
    var localEmail by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("💪") }

    val avatars = listOf("💪", "🥦", "🧗", "🏃", "🧘", "🥑")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isLoggedIn) {
            // Logged Out UI
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GymTertiary),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFBAC7DB))
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Masuk Akun Latihan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GymSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dapatkan rekap pola latihan khusus, nutrisi, alarm luring, dan sinkronisasi instan aman.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = GymSecondary.copy(alpha = 0.8f)
                    )
                }
            }

            // Google login button
            Button(
                onClick = onTriggerGoogleLogin,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("trigger_google_dialog"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GymPrimary)
            ) {
                Text("🔑 Masuk Instan via Google Account", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Text(
                text = "Atau Masuk Akun Manual Lokal",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF74777F),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Local login form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFDEE3EB))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = localName,
                        onValueChange = { localName = it },
                        label = { Text("Nama Lengkap") },
                        placeholder = { Text("cth: Gustisaputra Gym") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("local_login_name"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymPrimary)
                    )

                    OutlinedTextField(
                        value = localEmail,
                        onValueChange = { localEmail = it },
                        label = { Text("Alamat Email") },
                        placeholder = { Text("cth: gusti@gmail.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("local_login_email"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GymPrimary)
                    )

                    Text(text = "Pilih Avatar Latihan Anda:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        avatars.forEach { av ->
                            val isSel = selectedAvatar == av
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) GymPrimary else Color(0xFFF3F3FA))
                                    .border(2.dp, if (isSel) GymSecondary else Color.Transparent, CircleShape)
                                    .clickable { selectedAvatar = av }
                                    .testTag("avatar_select_" + av),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = av, fontSize = 20.sp)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (localName.isNotBlank() && localEmail.isNotBlank()) {
                                onLoginLocal(localEmail, localName, selectedAvatar)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("submit_local_login"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GymPrimary)
                    ) {
                        Text("Masuk via Profil Lokal", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Logged In members card
            Text(
                text = "Kartu Anggota Latihan Digital",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GymSecondary
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = GymSecondary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "GUSTI TRACKER MEMBER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                "Platinum Fitness Tier",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GymTertiary
                            )
                        }
                        Text(text = userAvatar, fontSize = 36.sp)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = userName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = Color.White.copy(alpha = 0.2f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("STATUS LATIHAN", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            Text("KONSISTEN 🔥", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TOTAL SESI LATIHAN", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            Text("$totalSessions Sesi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GymTertiary)
                        }
                    }
                }
            }

            // Cloud Sync Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (syncStatus.contains("Gagal")) Color.Red else Color(0xFF22C55E))
                    )
                    Column {
                        Text(
                            text = "Auto-Sync Google Cloud",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GymSecondary
                        )
                        Text(
                            text = syncStatus,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Profile editor card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFDEE3EB))
            ) {
                var editName by remember(userName) { mutableStateOf(userName) }
                var editEmail by remember(userEmail) { mutableStateOf(userEmail) }
                var editAvatar by remember(userAvatar) { mutableStateOf(userAvatar) }

                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Ubah Detail Profil", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GymPrimary)

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name")
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_email")
                    )

                    Text(text = "Pilih Avatar Baru:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        avatars.forEach { av ->
                            val isSel = editAvatar == av
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) GymPrimary else Color(0xFFF3F3FA))
                                    .clickable { editAvatar = av }
                                    .testTag("edit_avatar_select_" + av),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = av, fontSize = 16.sp)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (editName.isNotBlank() && editEmail.isNotBlank()) {
                                onUpdateProfile(editName, editEmail, editAvatar)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("submit_edit_profile"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GymPrimary)
                    ) {
                        Text("Simpan Perubahan")
                    }
                }
            }

            // Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("logout_button")
            ) {
                Text("Keluar dari Akun", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReminderCardItem(
    reminder: WorkoutReminder,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (reminder.isActive) GymPrimary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (reminder.isActive) GymPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reminder.time,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (reminder.isActive) MaterialTheme.colorScheme.onBackground
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = reminder.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (reminder.isActive) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = reminder.days,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GymPrimary,
                        checkedTrackColor = GymPrimary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .graphicsLayer(scaleX = 0.85f, scaleY = 0.85f)
                        .testTag("reminder_switch_" + reminder.id)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("delete_reminder_" + reminder.id)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus pengingat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// SUB DIALOGS HELPER CLASSES

@Composable
private fun PinDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    LaunchedEffect(code) {
        if (code.length == 4) {
            onSubmit(code)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GymSecondary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Passcode text output
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "• ".repeat(code.length) + "_ ".repeat(4 - code.length),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GymSecondary,
                        letterSpacing = 4.sp
                    )
                }

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Numeric Pad inside PIN dialog
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "OK")
                    )

                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { num ->
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (num == "OK") GymPrimary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            when (num) {
                                                "C" -> {
                                                    if (code.isNotEmpty()) code = code.dropLast(1)
                                                    errorMsg = ""
                                                }
                                                "OK" -> {
                                                    if (code.length == 4) {
                                                        onSubmit(code)
                                                    } else {
                                                        errorMsg = "PIN harus terdiri dari 4 digit!"
                                                    }
                                                }
                                                else -> {
                                                    if (code.length < 4) {
                                                        code += num
                                                    }
                                                    errorMsg = ""
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = num,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (num == "OK") GymPrimary else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss) {
                    Text("Batal", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = GymSecondary,
                    modifier = Modifier.size(36.dp)
                )
                Text("Ganti PIN Keamanan", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) oldPin = it },
                    label = { Text("PIN Lama") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                    label = { Text("PIN Baru (4 Digit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (errorMsg.isNotEmpty()) {
                    Text(text = errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            if (oldPin.length != 4 || newPin.length != 4) {
                                errorMsg = "Kedua sandi PIN harus berupa 4 digit angka!"
                            } else {
                                onSubmit(oldPin, newPin)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymPrimary)
                    ) {
                        Text("Simpan PIN", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseDialog(
    category: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Tambah Gerakan Baru", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Gerakan") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(onClick = { if (name.isNotBlank()) onSubmit(name, note) }) {
                        Text("Tambah")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectEditCountDialog(
    workoutCount: WorkoutCount,
    onDismiss: () -> Unit,
    onSubmit: (Int) -> Unit
) {
    var countText by remember { mutableStateOf(workoutCount.count.toString()) }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = workoutCount.emoji, fontSize = 36.sp)
                Text(
                    text = "Edit Langsung Jumlah Sesi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Konfigurasi total ${workoutCount.name} Gusti secara manual.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = countText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) countText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(0.6f).testTag("popup_direct_count_input")
                )

                if (errorMsg.isNotEmpty()) {
                    Text(text = errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val countVal = countText.toIntOrNull()
                            if (countVal == null || countVal < 0) {
                                errorMsg = "Jumlah sesi minimal adalah 0!"
                            } else {
                                onSubmit(countVal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymPrimary)
                    ) {
                        Text("Selesai")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, List<String>) -> Unit
) {
    var hours by remember { mutableStateOf("07") }
    var minutes by remember { mutableStateOf("30") }
    var label by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    val daysOfWeek = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    val selectedDays = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = GymPrimary,
                    modifier = Modifier.size(36.dp)
                )
                Text("Buat Pengingat", fontSize = 16.sp, fontWeight = FontWeight.Bold)

                // Time picker simple row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hours = it },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(" : ", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minutes = it },
                        modifier = Modifier.width(60.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("cth: Kaki (Leg Day) 🦵") },
                    label = { Text("Label Latihan") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Days selection header
                Text(
                    text = "Pilih Hari Latihan:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                // Grid checkboxes for days of week
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    daysOfWeek.forEach { day ->
                        val isChecked = selectedDays.contains(day)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    if (isChecked) selectedDays.remove(day) else selectedDays.add(day)
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked == true) selectedDays.add(day) else selectedDays.remove(day)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = day, fontSize = 13.sp)
                        }
                    }
                }

                if (errorMsg.isNotEmpty()) {
                    Text(text = errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val h = hours.toIntOrNull()
                            val m = minutes.toIntOrNull()
                            if (h == null || h !in 0..23 || m == null || m !in 0..59) {
                                errorMsg = "Format Jam (0-23) atau Menit (0-59) salah!"
                            } else if (label.isBlank()) {
                                errorMsg = "Harap masukkan label latihan!"
                            } else {
                                val hStr = h.toString().padStart(2, '0')
                                val mStr = m.toString().padStart(2, '0')
                                onSubmit("$hStr:$mStr", label, selectedDays.toList())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GymPrimary)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
