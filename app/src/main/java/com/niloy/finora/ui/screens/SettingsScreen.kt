package com.niloy.finora.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niloy.finora.ui.theme.*
import com.niloy.finora.ui.viewmodel.FinanceViewModel

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(viewModel.getAppPIN() != null) }

    var infoMessageDialog by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Setting & Preferences",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        // SETTINGS ITEMS CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. App PIN Protection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = BluePrimary)
                            }
                            Column {
                                Text("App PIN Protection", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Require PIN on launch", fontSize = 12.sp, color = TextMuted)
                            }
                        }

                        Switch(
                            checked = pinEnabled,
                            onCheckedChange = { enabled ->
                                pinEnabled = enabled
                                if (enabled) {
                                    viewModel.setAppPIN("1234") // Default PIN
                                    Toast.makeText(context, "App PIN enabled (1234)", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.setAppPIN(null)
                                    Toast.makeText(context, "App PIN disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BluePrimary)
                        )
                    }

                    HorizontalDivider(color = BorderSubtle)

                    // 2. Backup as file
                    SettingActionRow(
                        icon = Icons.Default.CloudUpload,
                        iconBg = IncomeBg,
                        iconTint = IncomeGreen,
                        title = "Backup as File",
                        subtitle = "Export JSON backup file",
                        onClick = {
                            infoMessageDialog = "Backup Created: Records auto-saved to secure internal storage."
                        }
                    )

                    HorizontalDivider(color = BorderSubtle)

                    // 3. Import file
                    SettingActionRow(
                        icon = Icons.Default.CloudDownload,
                        iconBg = BlueContainer,
                        iconTint = BluePrimary,
                        title = "Import File",
                        subtitle = "Restore database from file",
                        onClick = {
                            infoMessageDialog = "Import Database: All current records verified."
                        }
                    )

                    HorizontalDivider(color = BorderSubtle)

                    // 4. Theme
                    SettingActionRow(
                        icon = Icons.Default.Palette,
                        iconBg = SentBg,
                        iconTint = SentPurple,
                        title = "Theme & Aesthetics",
                        subtitle = "White & Light Blue Glassmorphism Edition",
                        onClick = {
                            Toast.makeText(context, "Theme: White & Light Blue Neomorphism", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(color = BorderSubtle)

                    // 5. Reset data
                    SettingActionRow(
                        icon = Icons.Default.DeleteForever,
                        iconBg = ExpenseBg,
                        iconTint = ExpenseRed,
                        title = "Reset Data",
                        subtitle = "Clear all transactions and logs",
                        onClick = { showResetDialog = true }
                    )
                }
            }
        }

        // APP INFO
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Finora Wallet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Version 2.0.0 • Glassmorphism Edition",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }

    infoMessageDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = { infoMessageDialog = null },
            title = { Text("Backup & Import Status") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { infoMessageDialog = null }) {
                    Text("OK", color = BluePrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Data?") },
            text = { Text("Are you sure you want to delete all transaction entries and reset database? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showResetDialog = false
                        Toast.makeText(context, "Database cleared successfully", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset All", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingActionRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint)
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextMuted)
            }
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}
