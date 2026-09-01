package com.niloy.finora.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.niloy.finora.ui.theme.*
import com.niloy.finora.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var showPINManageDialog by remember { mutableStateOf(false) }

    // Backup import launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importBackupFile(context, it) { success ->
                if (success) {
                    Toast.makeText(context, "✅ Backup Restored Successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "❌ Failed to read backup file.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // HEADER
        item {
            Column {
                Text(
                    text = "Preferences",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Application Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }
        }

        // 1. BRAND PROFILE CARD (Delicate, Minimalist White Surface)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryTealContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "Wallet Icon",
                        tint = PrimaryTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Finora Premium",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("LOCAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                    Text(
                        text = "100% Secure Household Ledger v2.1",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. APP SECURITY SECTION
        item {
            SettingsGroupCard(title = "App Security & Passcode") {
                val pinSet = viewModel.getAppPIN() != null
                SettingsItem(
                    icon = if (pinSet) Icons.Default.LockOpen else Icons.Default.Lock,
                    iconBg = if (pinSet) Color(0xFFECFDF5) else PrimaryTealContainer,
                    iconTint = if (pinSet) IncomeGreen else PrimaryTeal,
                    title = if (pinSet) "Update Passcode PIN" else "Configure PIN Lock",
                    subtitle = if (pinSet) "Secure active login passcode" else "Lock ledger to restrict unauthorized eyes",
                    onClick = { showPINManageDialog = true }
                )
            }
        }

        // 3. OFFLINE STORAGE & DATA UTILITIES
        item {
            SettingsGroupCard(title = "Backup & Data Exporter") {
                // Export JSON Backup Share Action
                SettingsItem(
                    icon = Icons.Default.CloudUpload,
                    iconBg = Color(0xFFEFF6FF),
                    iconTint = AccentBlue,
                    title = "Backup Database Ledger",
                    subtitle = "Generate dynamic JSON file to share & save",
                    onClick = {
                        coroutineScope.launch {
                            val backupJson = viewModel.exportToJsonString()
                            val backupFile = viewModel.createBackupFile(context, backupJson)
                            if (backupFile != null) {
                                shareExportFile(context, backupFile, "application/json")
                            } else {
                                Toast.makeText(context, "Failed to create backup file.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(BorderSubtle))

                // Export CSV Share Action
                SettingsItem(
                    icon = Icons.Default.Description,
                    iconBg = Color(0xFFECFDF5),
                    iconTint = IncomeGreen,
                    title = "Export to Excel/CSV",
                    subtitle = "Format transactions to view inside spreadsheets",
                    onClick = {
                        coroutineScope.launch {
                            val csvData = viewModel.exportToCsvString()
                            val csvFile = viewModel.createCsvFile(context, csvData)
                            if (csvFile != null) {
                                shareExportFile(context, csvFile, "text/csv")
                            } else {
                                Toast.makeText(context, "Failed to create CSV file.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(BorderSubtle))

                // Import JSON File Picker
                SettingsItem(
                    icon = Icons.Default.FolderOpen,
                    iconBg = Color(0xFFFAF5FF),
                    iconTint = AccentPurple,
                    title = "Restore Backup",
                    subtitle = "Restore previously exported Finora JSON backups",
                    onClick = { filePickerLauncher.launch("application/json") }
                )
            }
        }

        // 4. DATA PURGE & MANAGEMENT SECTION (Danger Zone)
        item {
            SettingsGroupCard(title = "Danger Zone") {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    iconBg = ExpenseBg,
                    iconTint = ExpenseRed,
                    title = "Reset All Database Records",
                    subtitle = "Irreversibly delete all household transactions",
                    onClick = { showClearDialog = true }
                )
            }
        }

        // 5. SECURE PRIVACY STATEMENT FOOTER
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "100% Offline • 100% Secure • Private",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
                Text(
                    text = "Finora operates strictly on-device. No data ever leaves your hand.",
                    fontSize = 10.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // PIN PASSCODE DIALOG
    if (showPINManageDialog) {
        var newPinText by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        val currentPin = viewModel.getAppPIN()

        AlertDialog(
            onDismissRequest = { showPINManageDialog = false },
            title = {
                Text(
                    text = if (currentPin != null) "Passcode Protection Active" else "Set Security Passcode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (currentPin != null) {
                            "Type in a new 4-digit numeric code below to update, or click disable lock to remove completely."
                        } else {
                            "Configure a secure 4-digit code to guard your ledger on application start."
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = newPinText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                newPinText = input
                                errorMsg = null
                            }
                        },
                        placeholder = { Text("e.g. 1234", color = TextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMsg != null) {
                        Text(text = errorMsg ?: "", color = ExpenseRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinText.length == 4) {
                            viewModel.setAppPIN(newPinText)
                            Toast.makeText(context, "✅ PIN Passcode updated successfully!", Toast.LENGTH_SHORT).show()
                            showPINManageDialog = false
                        } else {
                            errorMsg = "PIN must be exactly 4 digits."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Passcode", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showPINManageDialog = false }) {
                        Text("Cancel")
                    }
                    if (currentPin != null) {
                        TextButton(
                            onClick = {
                                viewModel.setAppPIN(null)
                                Toast.makeText(context, "🔓 PIN Passcode Disabled", Toast.LENGTH_SHORT).show()
                                showPINManageDialog = false
                            }
                        ) {
                            Text("Disable Lock", color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        )
    }

    // CLEAR ALL DATA DIALOG
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = "Purge Ledger Database?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This action is completely permanent and irreversible. All your transactions and category modifications will be permanently deleted from this device.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        Toast.makeText(context, "✅ Database cleared completely.", Toast.LENGTH_SHORT).show()
                        showClearDialog = false
                    }
                ) {
                    Text("Yes, Clear All Data", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
fun SettingsItem(
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
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Go",
            tint = BorderSubtle,
            modifier = Modifier.size(18.dp)
        )
    }
}

fun shareExportFile(context: Context, file: File, mimeType: String) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val fileUri = FileProvider.getUriForFile(context, authority, file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Save / Share Ledger Data"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
