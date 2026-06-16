package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import com.example.data.WorkoutReminder
import com.example.data.WeeklySchedule
import com.example.data.ExerciseChecklistItem
import com.example.data.ProteinIntake
import com.example.data.WorkoutSession
import com.example.ui.theme.*
import com.example.ui.components.*
import kotlinx.coroutines.launch
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineSet()
    
    // Core states from ViewModel
    val counts by viewModel.workoutCounts.collectAsStateWithLifecycle()
    val logs by viewModel.historyLogs.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val lockState by viewModel.lockState.collectAsStateWithLifecycle()
    val checklist by viewModel.exerciseChecklist.collectAsStateWithLifecycle()
    val protein by viewModel.proteinIntake.collectAsStateWithLifecycle()
    val sessions by viewModel.workoutSessions.collectAsStateWithLifecycle()
    val weeklySchedule by viewModel.weeklySchedule.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isWorkoutDay by viewModel.isWorkoutDay.collectAsStateWithLifecycle()
    
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()
    val themePreference by viewModel.theme.collectAsStateWithLifecycle()
    
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val advice by viewModel.geminiAdvice.collectAsStateWithLifecycle()
    val yearlyWrapped by viewModel.yearlyWrappedSummary.collectAsStateWithLifecycle()
    val refreshCooldown by viewModel.refreshCooldown.collectAsStateWithLifecycle()
    val encouragement by viewModel.encouragementMessage.collectAsStateWithLifecycle()

    // UI States
    val tabs = WorkoutTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    
    var showPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf<String?>(null) }
    var showDirectEditDialog by remember { mutableStateOf<WorkoutCount?>(null) }
    var showAddReminderDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Sync Pager with Bottom Nav
    LaunchedEffect(pagerState.currentPage) {
        // Pager change triggers UI state if needed (optional)
    }

    // Biometric Launcher
    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)

    fun showBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.toggleLockState("1234") { success, msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    scope.launch { snackbarHostState.showSnackbar("Biometrik Gagal: $errString") }
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Kunci Papan Skor")
            .setSubtitle("Gunakan sidik jari atau wajah untuk akses cepat")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    // Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                viewModel.signInWithFirebaseWithGoogle(token) { success, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        } catch (e: ApiException) {
            scope.launch { snackbarHostState.showSnackbar("Google Sign-In Gagal: ${e.message}") }
        }
    }

    // Encouragement effect
    LaunchedEffect(encouragement) {
        encouragement?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearEncouragement()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            TabNavigationRow(
                selectedTab = tabs[pagerState.currentPage],
                onTabSelected = { tab ->
                    scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            HeaderPanel(
                isLoggedIn = isLoggedIn,
                userName = userName,
                userAvatar = userAvatar,
                onLoginClick = { scope.launch { pagerState.animateScrollToPage(WorkoutTab.PROFILE.ordinal) } },
                onProfileClick = { scope.launch { pagerState.animateScrollToPage(WorkoutTab.PROFILE.ordinal) } }
            )

            SmartClockPanel(weeklySchedule = weeklySchedule)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { page ->
                when (tabs[page]) {
                    WorkoutTab.DASHBOARD -> DashboardTabContent(
                        counts = counts,
                        workoutSessions = sessions,
                        isLocked = lockState?.isLocked ?: true,
                        advice = advice,
                        onAnalyze = { 
                            // Only auto-analyze if we don't have advice yet
                            if (advice.isBlank() || advice.contains("Coach AI untuk mendapatkan saran")) {
                                viewModel.analyzeWorkoutHabits() 
                            }
                        },
                        onRefreshAdvice = { viewModel.analyzeWorkoutHabits(true) },
                        refreshCooldown = refreshCooldown,
                        onIncrement = { viewModel.incrementCount(it.dayType) },
                        onDecrement = { viewModel.decrementCount(it.dayType) },
                        onDirectEdit = { showDirectEditDialog = it },
                        onLockedActionAttempt = {
                            if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
                                showBiometricPrompt()
                            } else {
                                showPinDialog = true
                            }
                        }
                    )
                    WorkoutTab.PPL_MENU -> PPLMenuTabContent(
                        checklist = checklist,
                        onToggle = { viewModel.updateExerciseChecklist(it) },
                        onReset = { viewModel.resetExerciseChecklist() },
                        onAddExercise = { showAddExerciseDialog = it },
                        onDeleteExercise = { viewModel.deleteExercise(it) }
                    )
                    WorkoutTab.AI_COACH -> AICoachChatTabContent(
                        messages = messages,
                        isAnalyzing = isAnalyzing,
                        onSendMessage = { viewModel.sendMessage(it) }
                    )
                    WorkoutTab.PROTEIN -> ProteinIntakeTabContent(
                        intake = protein,
                        isWorkoutDay = isWorkoutDay,
                        onUpdateIntake = { type, delta -> viewModel.updateProteinIntake(type, delta) },
                        onAddIntakeItem = { name, emoji, target, unit -> viewModel.addProteinIntakeItem(name, emoji, target, unit) },
                        onDeleteIntakeItem = { id -> viewModel.deleteProteinIntakeItem(id) },
                        onToggleWorkoutDay = { viewModel.toggleWorkoutDayOverride() }
                    )
                    WorkoutTab.STATISTICS -> RecapsTabContent(
                        counts = counts,
                        reminders = reminders,
                        workoutSessions = sessions,
                        weeklySchedule = weeklySchedule,
                        yearlyWrapped = yearlyWrapped,
                        isAnalyzing = isAnalyzing,
                        onUpdateWeeklyActivity = { day, act -> viewModel.updateWeeklyActivity(day, act) },
                        onAddReminderClick = { showAddReminderDialog = true },
                        onDeleteReminder = { viewModel.deleteReminder(it) },
                        onToggleReminder = { id, active -> viewModel.toggleReminderStatus(id, active) },
                        onGenerateWrapped = { year: Int -> viewModel.generateYearlyWrapped(year) }
                    )
                    WorkoutTab.PROFILE -> ProfileTabContent(
                        isLoggedIn = isLoggedIn,
                        userName = userName,
                        userEmail = userEmail,
                        userAvatar = userAvatar,
                        totalSessions = counts.sumOf { it.count },
                        syncStatus = syncStatus,
                        themePreference = themePreference,
                        onThemeChange = { viewModel.setTheme(it) },
                        onTriggerGoogleLogin = { 
                            googleSignInLauncher.launch(viewModel.getGoogleSignInClient(context).signInIntent)
                        },
                        onLoginLocal = { email, name, avatar -> viewModel.loginSimulatedLocal(email, name, avatar) },
                        onLogout = { viewModel.logoutUser(context) },
                        onUpdateProfile = { n, e, a -> viewModel.updateProfile(n, e, a) },
                        onSyncCloud = { viewModel.syncData() }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showPinDialog) {
        PinDialog(
            currentPin = lockState?.passcode ?: "1234",
            onDismiss = { showPinDialog = false },
            onUnlock = { pin ->
                viewModel.toggleLockState(pin) { success, msg ->
                    if (success) showPinDialog = false
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            }
        )
    }

    if (showDirectEditDialog != null) {
        DirectEditCountDialog(
            item = showDirectEditDialog!!,
            onDismiss = { showDirectEditDialog = null },
            onConfirm = { count ->
                viewModel.updateCountDirectly(showDirectEditDialog!!.dayType, count)
                showDirectEditDialog = null
            }
        )
    }

    if (showAddExerciseDialog != null) {
        AddExerciseDialog(
            category = showAddExerciseDialog!!,
            onDismiss = { showAddExerciseDialog = null },
            onConfirm = { name, note ->
                viewModel.addExercise(name, showAddExerciseDialog!!, note)
                showAddExerciseDialog = null
            }
        )
    }

    if (showAddReminderDialog) {
        AddReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onConfirm = { time, label, days ->
                viewModel.addReminder(time, label, days)
                showAddReminderDialog = false
            }
        )
    }
}

// Reusable Coroutine Scope helper for Compose
@Composable
fun rememberCoroutineSet() = rememberCoroutineScope()

@Composable
fun PinDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onUnlock: (String) -> Unit
) {
    var pinEntry by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = GymPrimary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Akses Terkunci", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Masukkan 4 digit PIN keamanan Anda untuk mengubah papan skor.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(4) { index ->
                        val char = pinEntry.getOrNull(index)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (char != null) GymPrimary else Color(0xFFF3F3FA)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (char != null) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                            }
                        }
                    }
                }

                // Simplified Keypad
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0 until 4) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (j in 0 until 3) {
                                val num = keys[i * 3 + j]
                                Button(
                                    onClick = {
                                        when (num) {
                                            "C" -> if (pinEntry.isNotEmpty()) pinEntry = pinEntry.dropLast(1)
                                            "OK" -> onUnlock(pinEntry)
                                            else -> if (pinEntry.length < 4) pinEntry += num
                                        }
                                    },
                                    modifier = Modifier.size(64.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (num == "OK") GymPrimary.copy(alpha = 0.15f) else Color(0xFFF3F3FA),
                                        contentColor = if (num == "OK") GymPrimary else MaterialTheme.colorScheme.onBackground
                                    )
                                ) {
                                    Text(num, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddExerciseDialog(
    category: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Gerakan Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Gerakan") },
                    placeholder = { Text("cth: Barbell Squat") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Beban/Tips (Opsional)") },
                    placeholder = { Text("cth: Fokus tempo lambat") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, note) },
                colors = ButtonDefaults.buttonColors(containerColor = GymPrimary)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun DirectEditCountDialog(
    item: WorkoutCount,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(item.count.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Skor ${item.name}") },
        text = {
            Column {
                Text("Masukkan jumlah akumulasi sesi latihan secara manual:", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { if (it.all { char -> char.isDigit() }) textValue = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text("Sesi") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(textValue.toIntOrNull() ?: item.count) },
                colors = ButtonDefaults.buttonColors(containerColor = GymPrimary)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>) -> Unit
) {
    var hour by remember { mutableStateOf("07") }
    var minute by remember { mutableStateOf("00") }
    var label by remember { mutableStateOf("Waktunya Latihan!") }
    val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
    val selectedDays = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pasang Pengingat Latihan") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hour = it },
                        modifier = Modifier.width(64.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(" : ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minute = it },
                        modifier = Modifier.width(64.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label Pengingat") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pilih Hari:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    days.forEach { day ->
                        FilterChip(
                            selected = selectedDays.contains(day),
                            onClick = {
                                if (selectedDays.contains(day)) selectedDays.remove(day)
                                else selectedDays.add(day)
                            },
                            label = { Text(day, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val time = "${hour.padStart(2, '0')}:${minute.padStart(2, '0')}"
                    onConfirm(time, label, selectedDays.toList()) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = GymPrimary)
            ) {
                Text("Pasang")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
