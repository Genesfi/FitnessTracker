package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProteinIntake
import com.example.ui.theme.GymPrimary
import com.example.ui.theme.GymSecondary
import com.example.ui.theme.GymTertiary

@Composable
fun ProteinIntakeTabContent(
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
                color = MaterialTheme.colorScheme.onBackground
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Target protein harian Anda disesuaikan dengan intensitas aktivitas hari ini.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun ProteinItemCard(
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Target: $target $unit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onDecrement) {
                    Text("—", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$count", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    if (count >= target) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                    }
                }

                IconButton(onClick = onIncrement) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
