package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Category
import com.example.ui.viewmodel.FinanceViewModel
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
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    // File selection launcher for backup importing
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBackupFile(context, uri) { success ->
                if (success) {
                    Toast.makeText(context, "Data Imported Successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to import backup! Ensure format is valid.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Settings & Backups", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup and Exports Section
            item {
                SectionTitle(text = "Data Management (completely offline)")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column {
                        SettingsRow(
                            title = "Export JSON Backup",
                            subtitle = "Save a secure local backup of all cash transactions and settings.",
                            icon = Icons.Default.CloudUpload,
                            color = Color(0xFF3B82F6),
                            onClick = {
                                coroutineScope.launch {
                                    val backupJson = viewModel.exportToJsonString()
                                    val file = viewModel.createBackupFile(context, backupJson)
                                    if (file != null) {
                                        shareLocalFile(context, file, "application/json")
                                    } else {
                                        Toast.makeText(context, "Failed to create backup file.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Import JSON Backup",
                            subtitle = "Restore all your categories and cash logs from an exported backup file.",
                            icon = Icons.Default.CloudDownload,
                            color = Color(0xFF10B981),
                            onClick = {
                                importLauncher.launch("application/json")
                            }
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Export to CSV Excel",
                            subtitle = "Generate a spreadsheet of cash movements for viewing on computer/Excel.",
                            icon = Icons.Default.GridOn,
                            color = Color(0xFFF59E0B),
                            onClick = {
                                coroutineScope.launch {
                                    val csvContent = viewModel.exportToCsvString()
                                    val file = viewModel.createCsvFile(context, csvContent)
                                    if (file != null) {
                                        shareLocalFile(context, file, "text/csv")
                                    } else {
                                        Toast.makeText(context, "Failed to create CSV file.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Categories Management
            item {
                SectionTitle(text = "Categories & Spends")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    SettingsRow(
                        title = "Manage Categories",
                        subtitle = "Customize, add, or delete cash expense categories.",
                        icon = Icons.Default.Category,
                        color = Color(0xFF8B5CF6),
                        onClick = { showCategoryDialog = true }
                    )
                }
            }

            // PIN Lock Passcode Security
            item {
                SectionTitle(text = "App Privacy & Protection")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    val hasPin = viewModel.getAppPIN() != null
                    SettingsRow(
                        title = if (hasPin) "Change or Remove PIN Code" else "Enable App PIN Lock",
                        subtitle = if (hasPin) "Secure passcode protection is active. Tap to modify." else "Add a 4-digit PIN gate to protect physical cash records from nosy eyes.",
                        icon = if (hasPin) Icons.Default.LockOpen else Icons.Default.Lock,
                        color = Color(0xFFEF4444),
                        onClick = { showPinDialog = true }
                    )
                }
            }

            // Reset Database Option
            item {
                SectionTitle(text = "System Actions")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    SettingsRow(
                        title = "Wipe and Reset All Data",
                        subtitle = "Delete all transactions and custom categories, reverting to empty defaults. Be careful!",
                        icon = Icons.Default.DeleteForever,
                        color = Color(0xFFDC2626),
                        onClick = { showResetConfirmation = true }
                    )
                }
            }
        }
    }

    // Custom Categories Management Dialog
    if (showCategoryDialog) {
        CategoryManageDialog(
            categoriesList = categories,
            onDismiss = { showCategoryDialog = false },
            onAddCategory = { viewModel.addCategory(it) },
            onDeleteCategory = { viewModel.deleteCategory(it) }
        )
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        PinSetupDialog(
            currentPin = viewModel.getAppPIN(),
            onDismiss = { showPinDialog = false },
            onSave = { newPin ->
                viewModel.setAppPIN(newPin)
                showPinDialog = false
                Toast.makeText(context, if (newPin == null) "PIN Protection Removed" else "PIN Lock Code Configured!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Danger Zone: Factory Reset") },
            text = { Text("Are you absolutely sure you want to completely erase all transaction logs and custom categories? This is completely permanent and cannot be reversed!") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showResetConfirmation = false
                        Toast.makeText(context, "Application Data Erased Successfully", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.testTag("reset_confirm_button")
                ) {
                    Text("Erase Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp, top = 8.dp)
    )
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

// Share local exported files offline using FileProvider
fun shareLocalFile(context: Context, file: File, mimeType: String) {
    try {
        val authority = "com.aistudio.cashtrack.mvkpzl.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Export File"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not trigger sharing: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun CategoryManageDialog(
    categoriesList: List<Category>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Categories", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("New category name...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_category_input"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCategory(newCategoryName)
                                newCategoryName = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_category_button")
                    ) {
                        Text("Add")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoriesList, key = { it.id }) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = cat.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (!cat.isSystem) {
                                IconButton(onClick = { onDeleteCategory(cat) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Text(text = "System Default", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun PinSetupDialog(
    currentPin: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    val hasPin = currentPin != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasPin) "PIN Settings" else "Setup PIN Lock") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (hasPin) "A secure PIN is already active. Enter a new 4-digit PIN code to update it, or choose Remove PIN." else "Please type a 4-digit security code to secure your app records on opening.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pinText,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinText = it },
                    label = { Text("4-digit PIN") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_code_field"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasPin) {
                    TextButton(onClick = { onSave(null) }) {
                        Text("Remove PIN", color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = { if (pinText.length == 4) onSave(pinText) },
                    enabled = pinText.length == 4,
                    modifier = Modifier.testTag("save_pin_button")
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
