package com.niloy.finora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.niloy.finora.data.model.Category
import com.niloy.finora.ui.theme.*

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
    // Three states: "EXPENSE", "WITHDRAWAL", "RECEIVED"
    var selectedType by remember { mutableStateOf("EXPENSE") }
    var amountText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Others") }
    
    // For RECEIVED
    var senderName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    
    // For WITHDRAWAL
    var sourceBankName by remember { mutableStateOf("") }
    
    var note by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    val typeColor = remember(selectedType) {
        when (selectedType) {
            "RECEIVED" -> IncomeGreen
            "WITHDRAWAL" -> AccentBlue
            else -> ExpenseRed
        }
    }

    val typeBg = remember(selectedType) {
        when (selectedType) {
            "RECEIVED" -> IncomeBg
            "WITHDRAWAL" -> Color(0xFFEFF6FF)
            else -> ExpenseBg
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BorderSubtle),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // DIALOG HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Record Cashflow",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                        Text(
                            text = "New Ledger Entry",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BackgroundLight)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                // 3-WAY SEGMENTED CONTROL (EXPENSE | WITHDRAWAL | RECEIVED)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BackgroundLight)
                        .padding(4.dp)
                ) {
                    // EXPENSE TAB
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedType == "EXPENSE") ExpenseRed else Color.Transparent)
                            .clickable { selectedType = "EXPENSE" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == "EXPENSE") Color.White else TextMuted
                        )
                    }

                    // WITHDRAWAL TAB
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedType == "WITHDRAWAL") AccentBlue else Color.Transparent)
                            .clickable { selectedType = "WITHDRAWAL" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Withdrawal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == "WITHDRAWAL") Color.White else TextMuted
                        )
                    }

                    // RECEIVED TAB
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedType == "RECEIVED") IncomeGreen else Color.Transparent)
                            .clickable { selectedType = "RECEIVED" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Received",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == "RECEIVED") Color.White else TextMuted
                        )
                    }
                }

                // AMOUNT INPUT CARD
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(typeBg)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (selectedType) {
                            "RECEIVED" -> "MONEY RECEIVED INTO"
                            "WITHDRAWAL" -> "PHYSICAL CASH WITHDRAWAL"
                            else -> "DAILY HOUSEHOLD EXPENSE"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = typeColor,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' } },
                        placeholder = { Text("0.00", fontSize = 28.sp, color = typeColor.copy(alpha = 0.5f)) },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            color = typeColor
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = typeColor,
                            unfocusedBorderColor = typeColor.copy(alpha = 0.2f)
                        )
                    )
                }

                // CONDITIONAL DYNAMIC INPUT FIELDS
                when (selectedType) {
                    "RECEIVED" -> {
                        // SENDER FIELD
                        OutlinedTextField(
                            value = senderName,
                            onValueChange = { senderName = it },
                            label = { Text("Sender (e.g. Husband, Son)", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("Who sent the money?") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // BANK NAME FIELD
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Destination (e.g. Bank Account, Cash)", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("e.g. Sonali Bank (Leave empty for direct cash)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "WITHDRAWAL" -> {
                        // FROM BANK FIELD
                        OutlinedTextField(
                            value = sourceBankName,
                            onValueChange = { sourceBankName = it },
                            label = { Text("Source Bank Name", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("e.g. Sonali Bank") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    else -> {
                        // EXPENSE FIELD
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title / Purpose of Expense", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("e.g. Grocery Bazar, Electricity Bill") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_title_input")
                        )

                        // CATEGORY SELECTION DECK
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Choose Category",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(categories) { cat ->
                                    val isSelected = cat.name == selectedCategory
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategory = cat.name },
                                        label = { Text(cat.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PrimaryTeal,
                                            selectedLabelColor = Color.White,
                                            containerColor = BackgroundLight,
                                            labelColor = TextSecondary
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // COMMON NOTE FIELD
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Remarks / Extra Note (Optional)", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("Write extra details here...") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // SAVE ACTION TRIGGER
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val savedTitle = when (selectedType) {
                                "WITHDRAWAL" -> "Cash Withdrawal"
                                "RECEIVED" -> "Received from ${senderName.ifEmpty { "Family Member" }}"
                                else -> title.ifEmpty { "Daily Expense" }
                            }

                            val savedCategory = when (selectedType) {
                                "WITHDRAWAL" -> "Cash Withdrawal"
                                "RECEIVED" -> "Received Income"
                                else -> selectedCategory
                            }

                            onSave(
                                selectedType,
                                amount,
                                savedTitle,
                                savedCategory,
                                System.currentTimeMillis(),
                                senderName.takeIf { it.isNotBlank() },
                                if (selectedType == "WITHDRAWAL") sourceBankName.takeIf { it.isNotBlank() } else bankName.takeIf { it.isNotBlank() },
                                note,
                                null,
                                null,
                                null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("dialog_save_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = typeColor
                    )
                ) {
                    Text(
                        text = "Save Entry",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}
