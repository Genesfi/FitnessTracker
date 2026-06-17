package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkoutCount
import com.example.data.WorkoutSession
import com.example.ui.theme.*
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardTabContent(
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
    
    // Fixed sort order to prevent list items from jumping around
    val sortedCounts = remember(counts) {
        counts.sortedBy { 
            when(it.dayType) {
                "LEG" -> 1
                "PUSH" -> 2
                "PULL" -> 3
                else -> 4
            }
        }
    }

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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        letterSpacing = 1.2.sp
                    )

                    // Lock Toggle Button
                    Surface(
                        onClick = onLockedActionAttempt,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLocked) MaterialTheme.colorScheme.primary else Color(0xFF22C55E)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Refresh, // Fallback if LockOpen missing
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = if (isLocked) "DIKUNCI" else "BUKA KUNCI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sesi Latihan", 
                    fontSize = 22.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$totalSessions",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 54.sp
                    )
                    Text(
                        text = "Total Sesi Latihan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // List workout counts
        sortedCounts.forEach { item ->
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
                color = MaterialTheme.colorScheme.onBackground,
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Dynamic monthly statistics based on real workout sessions
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                
                // 31 is the initial legacy count (16 Leg + 8 Push + 7 Pull)
                // We calculate how many "real" sessions we have vs what's reported in total counts
                val currentSessionsCount = workoutSessions.size
                val totalLegacySessions = (totalSessions - currentSessionsCount).coerceAtLeast(0)

                for (i in 2 downTo 0) {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.MONTH, -i)
                    val mIndex = calendar.get(Calendar.MONTH)
                    val mYear = calendar.get(Calendar.YEAR)
                    val mName = monthNames[mIndex]
                    
                    var monthSessions = workoutSessions.count { session ->
                        val sessionCal = Calendar.getInstance()
                        sessionCal.timeInMillis = session.date
                        sessionCal.get(Calendar.MONTH) == mIndex && sessionCal.get(Calendar.YEAR) == mYear
                    }

                    // Correction logic: If this is April/May 2026 and we have "Legacy" data
                    if (mYear == 2026 && totalLegacySessions > 0) {
                        if (mIndex == 3) monthSessions += 16 // April: 16 sessions
                        if (mIndex == 4) monthSessions += 10 // May: 10 sessions
                        if (mIndex == 5) {
                            // June gets the remainder, but we cap it to make sure 
                            // the bar doesn't exceed the logical total session count
                            val juneLegacy = (totalLegacySessions - 26).coerceAtLeast(0)
                            monthSessions += juneLegacy
                        }
                    }
                    
                    // Final safety: A single month's bar shouldn't look like it has 
                    // more sessions than the total accumulated score unless 
                    // the user has actually logged that many sessions.
                    val displaySessions = if (mYear == 2026 && mIndex >= 3 && mIndex <= 5) {
                        monthSessions.coerceAtMost(totalSessions)
                    } else {
                        monthSessions
                    }
                    
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "$mName $mYear", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "$displaySessions Sesi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val progress = if (displaySessions > 0) (displaySessions / 24f).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Prediksi Tahunan: ${totalSessions * 2} Sesi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displaysAdvice,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun WorkoutItemCard(
    item: WorkoutCount,
    isLocked: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDirectEdit: () -> Unit,
    onLockedActionAttempt: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isLocked) onLockedActionAttempt() else onDirectEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = item.emoji, fontSize = 24.sp)
                    }
                }
                
                Column {
                    Text(
                        text = item.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sesi Latihan Terdata",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically, 
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { if (isLocked) onLockedActionAttempt() else onDecrement() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = "${item.count}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.widthIn(min = 32.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { if (isLocked) onLockedActionAttempt() else onIncrement() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
