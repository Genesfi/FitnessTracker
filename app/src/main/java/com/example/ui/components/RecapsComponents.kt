package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WeeklySchedule
import com.example.data.WorkoutCount
import com.example.data.WorkoutReminder
import com.example.data.WorkoutSession
import com.example.ui.theme.GymPrimary
import com.example.ui.theme.GymSecondary
import java.util.Calendar

@Composable
fun RecapsTabContent(
    counts: List<WorkoutCount>,
    reminders: List<WorkoutReminder>,
    workoutSessions: List<WorkoutSession>,
    weeklySchedule: List<WeeklySchedule>,
    yearlyWrapped: Map<Int, String>,
    isAnalyzing: Boolean,
    onUpdateWeeklyActivity: (String, String) -> Unit,
    onAddReminderClick: () -> Unit,
    onDeleteReminder: (Int) -> Unit,
    onToggleReminder: (Int, Boolean) -> Unit,
    onGenerateWrapped: (Int) -> Unit,
) {
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    
    val availableYears = remember(workoutSessions) {
        val years = workoutSessions.map {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            cal.get(Calendar.YEAR)
        }.distinct().toMutableList()
        if (!years.contains(2026)) years.add(2026) 
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (!years.contains(currentYear)) years.add(currentYear)
        years.sortedDescending()
    }

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
                text = "Rekap & Statistik Latihan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(availableYears) { year ->
                    FilterChip(
                        selected = selectedYear == year,
                        onClick = { selectedYear = year },
                        label = { Text(year.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        WrappedAchievementCard(
            year = selectedYear,
            sessions = workoutSessions.filter {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.date
                cal.get(Calendar.YEAR) == selectedYear
            },
            totalLegacyCount = if (selectedYear == 2026) {
                (counts.sumOf { it.count } - workoutSessions.size).coerceIn(0, 31)
            } else 0,
            wrappedText = yearlyWrapped[selectedYear],
            isAnalyzing = isAnalyzing,
            onGenerateRequest = { onGenerateWrapped(selectedYear) }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Jadwal Latihan Mingguan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
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
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(60.dp)
                        )
                        
                        if (isEditing) {
                            OutlinedTextField(
                                value = tempActivity,
                                onValueChange = { tempActivity = it },
                                modifier = Modifier.weight(1f).height(48.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface),
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
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f).clickable { isEditing = true }
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Konsistensi Bulanan ($selectedYear)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val monthNames = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                val currentMonthIndex = if (selectedYear == currentYear) calendar.get(Calendar.MONTH) else 11
                
                val currentSessionsCount = workoutSessions.size
                val totalLegacySessions = if (selectedYear == 2026) {
                    (counts.sumOf { it.count } - currentSessionsCount).coerceIn(0, 31)
                } else 0

                for (mIndex in 0..currentMonthIndex) {
                    val mName = monthNames[mIndex]
                    var monthSessionsCount = workoutSessions.count { session ->
                        val sessionCal = Calendar.getInstance()
                        sessionCal.timeInMillis = session.date
                        val sMonth = sessionCal.get(Calendar.MONTH)
                        val sYear = sessionCal.get(Calendar.YEAR)
                        (sMonth == mIndex && sYear == selectedYear)
                    }
                    
                    if (selectedYear == 2026 && totalLegacySessions > 0) {
                        if (mIndex == 3) monthSessionsCount += 16
                        if (mIndex == 4) monthSessionsCount += 10
                        if (mIndex == 5) monthSessionsCount += (totalLegacySessions - 26).coerceAtLeast(0)
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = mName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            Text(text = "$monthSessionsCount Sesi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val progress = if (monthSessionsCount > 0) kotlin.math.min(1.0f, monthSessionsCount / 24.0f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Alarm Pengingat Latihan",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = onAddReminderClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GymPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pasang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (reminders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Text("Belum memasang pengingat alarm latihan.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun WrappedAchievementCard(
    year: Int,
    sessions: List<WorkoutSession>,
    totalLegacyCount: Int,
    wrappedText: String?,
    isAnalyzing: Boolean,
    onGenerateRequest: () -> Unit
) {
    val leg = sessions.count { it.dayType == "LEG" } + (if (year == 2026 && totalLegacyCount > 0) 16 else 0)
    val push = sessions.count { it.dayType == "PUSH" } + (if (year == 2026 && totalLegacyCount > 0) 8 else 0)
    val pull = sessions.count { it.dayType == "PULL" } + (if (year == 2026 && totalLegacyCount > 0) (totalLegacyCount - 24).coerceAtLeast(0) else 0)
    val total = sessions.size + totalLegacyCount

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GymPrimary, GymSecondary)
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FITNESS WRAPPED $year",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = 2.sp
                    )
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tahun Luar Biasa!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = total.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = " Total Sesi Latihan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AchievementStatItem("Leg", leg, "🦵")
                    AchievementStatItem("Push", push, "💪")
                    AchievementStatItem("Pull", pull, "🦾")
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (wrappedText != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "✨ AI Achievement Summary:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = wrappedText,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onGenerateRequest,
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GymPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = GymPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Wrapped Summary", color = GymPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementStatItem(label: String, count: Int, emoji: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 24.sp)
        Text(text = count.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
fun ReminderCardItem(
    reminder: WorkoutReminder,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (reminder.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (reminder.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = reminder.time, fontSize = 18.sp, fontWeight = FontWeight.Black, color = if (reminder.isActive) MaterialTheme.colorScheme.onSurface else Color.Gray)
                    Text(text = reminder.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (reminder.isActive) MaterialTheme.colorScheme.primary else Color.Gray)
                    Text(text = reminder.days, fontSize = 9.sp, color = Color.Gray)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
