package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Category
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        amount: Double,
        title: String,
        category: String,
        date: Long,
        sender: String?,
        bankName: String?,
        note: String,
        location: String?,
        reason: String?,
        subCategory: String?
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Expense, 1: Received, 2: Withdrawal

    // Fields
    var amountText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Others") }
    var noteText by remember { mutableStateOf("") }

    // Optional Fields
    var showOptionalFields by remember { mutableStateOf(false) }
    var senderText by remember { mutableStateOf("") }
    var bankNameText by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var subCategoryText by remember { mutableStateOf("") }

    // Date
    val calendar = remember { Calendar.getInstance() }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Category dropdown expansion state
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    text = "Add Money Record",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Received", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Withdrawal", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Amount Field (Always Required)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amountText = it },
                    label = { Text("Amount (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Tab Specific Core Fields
                when (selectedTab) {
                    0 -> { // Expense
                        OutlinedTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            label = { Text("Expense Title (e.g., CNG Fare)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("title_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = categoryDropdownExpanded,
                                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .testTag("category_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false }
                                ) {
                                    categories.forEach { categoryItem ->
                                        DropdownMenuItem(
                                            text = { Text(categoryItem.name) },
                                            onClick = {
                                                selectedCategory = categoryItem.name
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Received
                        OutlinedTextField(
                            value = senderText,
                            onValueChange = { senderText = it },
                            label = { Text("Sender (e.g., Father, Son)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sender_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = bankNameText,
                            onValueChange = { bankNameText = it },
                            label = { Text("Bank Name (e.g., Sonali Bank)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bank_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                    2 -> { // Withdrawal
                        OutlinedTextField(
                            value = bankNameText,
                            onValueChange = { bankNameText = it },
                            label = { Text("Withdrawn From Bank (e.g., Sonali Bank)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bank_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Date Selection Trigger
                OutlinedCard(
                    onClick = {
                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                selectedDateMillis = calendar.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        datePickerDialog.show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("date_trigger_card"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date: ${sdf.format(Date(selectedDateMillis))}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Select Date", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Expandable Optional Fields accordion
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOptionalFields = !showOptionalFields }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Optional Fields (Note, Location, etc.)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Icon(
                        imageVector = if (showOptionalFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Optional Fields"
                    )
                }

                if (showOptionalFields) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note / Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (selectedTab == 0) { // Expense only optional details
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = locationText,
                            onValueChange = { locationText = it },
                            label = { Text("Location (e.g., Kacha Bazar)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reasonText,
                            onValueChange = { reasonText = it },
                            label = { Text("Reason (e.g., Household emergency)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = subCategoryText,
                            onValueChange = { subCategoryText = it },
                            label = { Text("Sub-category") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (amount > 0.0) {
                                val txType = when (selectedTab) {
                                    0 -> "EXPENSE"
                                    1 -> "RECEIVED"
                                    else -> "WITHDRAWAL"
                                }
                                onSave(
                                    txType,
                                    amount,
                                    titleText,
                                    selectedCategory,
                                    selectedDateMillis,
                                    senderText.ifBlank { null },
                                    bankNameText.ifBlank { null },
                                    noteText,
                                    locationText.ifBlank { null },
                                    reasonText.ifBlank { null },
                                    subCategoryText.ifBlank { null }
                                )
                            }
                        },
                        enabled = amountText.isNotEmpty() && (selectedTab != 0 || titleText.isNotEmpty()),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_transaction_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
