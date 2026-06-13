package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProteinIntake
import com.example.ui.theme.GymPrimary

@Composable
fun ProteinIntakeTabContent(
    intake: List<ProteinIntake>,
    isWorkoutDay: Boolean,
    onUpdateIntake: (String, Int) -> Unit,
    onAddIntakeItem: (String, String, Int, String) -> Unit,
    onDeleteIntakeItem: (Int) -> Unit,
    onToggleWorkoutDay: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Asupan Nutrisi Harian",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isWorkoutDay,
                    onClick = onToggleWorkoutDay,
                    label = { Text(if (isWorkoutDay) "Latihan" else "Istirahat", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GymPrimary.copy(alpha = 0.1f),
                        selectedLabelColor = GymPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.LightGray.copy(alpha = 0.5f),
                        selectedBorderColor = GymPrimary.copy(alpha = 0.5f),
                        enabled = true,
                        selected = isWorkoutDay
                    ),
                    leadingIcon = if (isWorkoutDay) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GymPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Nutrisi", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (intake.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada data nutrisi hari ini.", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        intake.forEach { item ->
            val eggTarget = if (item.proteinType == "EGGS") (if (isWorkoutDay) 4 else 2) else item.target
            
            ProteinItemCard(
                item = item,
                displayTarget = eggTarget,
                onIncrement = { onUpdateIntake(item.proteinType, 1) },
                onDecrement = { onUpdateIntake(item.proteinType, -1) },
                onDelete = { onDeleteIntakeItem(item.id) }
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("💡", fontSize = 20.sp)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Saran Coach: Tambah asupan protein di hari latihan untuk pemulihan otot yang lebih cepat!",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

    if (showAddDialog) {
        AddNutritionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, emoji, target, unit ->
                onAddIntakeItem(name, emoji, target, unit)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddNutritionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🍱") }
    var target by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Porsi") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Tambah Menu Nutrisi", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Nama Makanan (misal: Whey)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = emoji, 
                        onValueChange = { emoji = it }, 
                        label = { Text("Emoji") }, 
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = unit, 
                        onValueChange = { unit = it }, 
                        label = { Text("Satuan") }, 
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                OutlinedTextField(
                    value = target, 
                    onValueChange = { target = it }, 
                    label = { Text("Target Harian") }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank()) {
                        onConfirm(name, emoji, target.toIntOrNull() ?: 1, unit)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GymPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Text("Simpan Nutrisi", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { 
                Text("Batal", color = Color.Gray) 
            }
        }
    )
}

@Composable
fun ProteinItemCard(
    item: ProteinIntake,
    displayTarget: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteIcon by remember { mutableStateOf(false) }
    
    val displayName = remember(item.proteinType) {
        when(item.proteinType) {
            "EGGS" -> "Telur Rebus/Dadar"
            "FISH" -> "Baby Fish Crispy"
            "PEA" -> "Pea Protein Isolate"
            else -> item.proteinType
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDeleteIcon = !showDeleteIcon },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon section
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.emoji, fontSize = 26.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Label section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "Target: $displayTarget ${item.unit}", 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }

            // Controls section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(
                    visible = showDeleteIcon,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.padding(end = 8.dp).size(32.dp).background(Color.Red.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = onDecrement, 
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text("—", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.widthIn(min = 36.dp)
                    ) {
                        Text(
                            text = "${item.count}", 
                            fontSize = 18.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        if (item.count >= displayTarget && displayTarget > 0) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                            }
                        }
                    }

                    IconButton(
                        onClick = onIncrement, 
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
