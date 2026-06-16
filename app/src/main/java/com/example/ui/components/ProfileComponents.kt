package com.example.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ProfileTabContent(
    isLoggedIn: Boolean,
    userName: String,
    userEmail: String,
    userAvatar: String,
    totalSessions: Int,
    syncStatus: String,
    themePreference: String,
    onThemeChange: (String) -> Unit,
    onTriggerGoogleLogin: () -> Unit,
    onLoginLocal: (String, String, String) -> Unit,
    onLogout: () -> Unit,
    onUpdateProfile: (String, String, String) -> Unit,
    onSyncCloud: () -> Unit = {}
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👋", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Masuk Akun Latihan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dapatkan rekap pola latihan khusus, nutrisi, alarm luring, dan sinkronisasi instan aman.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
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
            Text(
                text = "Kartu Anggota Latihan Digital",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
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
                                "FITNESS TRACKER MEMBER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                "Platinum Fitness Tier",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary
                            )
                        }
                        Text(text = userAvatar, fontSize = 36.sp)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = userName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("STATUS LATIHAN", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f))
                            Text("KONSISTEN 🔥", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TOTAL SESI LATIHAN", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f))
                            Text("$totalSessions Sesi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }

            // Theme Switcher Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tampilan Aplikasi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "Sistem", "LIGHT" to "Terang", "DARK" to "Gelap").forEach { (value, label) ->
                            val isSelected = themePreference == value
                            FilterChip(
                                selected = isSelected,
                                onClick = { onThemeChange(value) },
                                label = { Text(label, fontSize = 11.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Cloud Sync Status Card
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSyncCloud() },
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
                            color = MaterialTheme.colorScheme.onSurface
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
                    Text(text = "Ubah Detail Profil", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

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
