package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

import com.example.data.WeeklySchedule

@Composable
fun SmartClockPanel(weeklySchedule: List<WeeklySchedule>) {
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
    
    val todayActivity = weeklySchedule.find { it.dayName.equals(dayName, ignoreCase = true) }?.activity ?: "No Schedule"
    
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = icon, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d  %s - %s", hour, minute, dayName, todayActivity),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = advice,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun HeaderPanel(
    isLoggedIn: Boolean,
    userName: String,
    userAvatar: String,
    onLoginClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
            Column {
                Text(
                    text = if (isLoggedIn) "Halo, $userName!" else "Selamat Datang!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Pantau progres latihan Anda hari ini.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

        if (isLoggedIn) {
            Surface(
                modifier = Modifier
                    .size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = GymPrimary,
                onClick = onProfileClick
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = userAvatar, fontSize = 20.sp)
                }
            }
        } else {
            IconButton(
                onClick = onLoginClick,
                modifier = Modifier
                    .background(GymPrimary, RoundedCornerShape(12.dp))
                    .size(44.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
            }
        }
    }
}

enum class WorkoutTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Skor", Icons.Default.Home),
    PPL_MENU("Latihan", Icons.Default.List),
    AI_COACH("Coach AI", Icons.Default.Face),
    PROTEIN("Nutrisi", Icons.Default.Info),
    STATISTICS("Statistik", Icons.Default.DateRange),
    PROFILE("Profil", Icons.Default.Person)
}

@Composable
fun TabNavigationRow(
    selectedTab: WorkoutTab,
    onTabSelected: (WorkoutTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.height(72.dp)
    ) {
        WorkoutTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            color = if (selectedTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )
        }
    }
}
