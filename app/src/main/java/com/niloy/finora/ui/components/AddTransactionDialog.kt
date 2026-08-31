package com.niloy.finora.ui.components

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
    var isExpense by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Others") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var note by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = SurfaceLight,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // DIALOG HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Entry",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                // EXPENSE / INCOME SEGMENTED CONTROL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BackgroundLight)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isExpense) ExpenseRed else Color.Transparent)
                            .clickable { isExpense = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) Color.White else TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isExpense) IncomeGreen else Color.Transparent)
                            .clickable { isExpense = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            fontWeight = FontWeight.Bold,
                            color = if (!isExpense) Color.White else TextMuted
                        )
                    }
                }

                // AMOUNT DISPLAY INPUT
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amount (৳)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { char -> char.isDigit() || char == '.' } },
                        placeholder = { Text("0.00", fontSize = 28.sp, color = TextMuted) },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = if (isExpense) ExpenseRed else IncomeGreen
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                }

                // TITLE / DESCRIPTION
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Purpose") },
                    placeholder = { Text("e.g. Grocery Shopping, Salary") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_title_input")
                )

                // CATEGORY SELECTION
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = cat.name == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.name },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryTeal,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // NOTE
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // SAVE BUTTON
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            onSave(
                                if (isExpense) "EXPENSE" else "RECEIVED",
                                amount,
                                title.ifEmpty { if (isExpense) "Expense" else "Income" },
                                selectedCategory,
                                System.currentTimeMillis(),
                                null,
                                null,
                                note,
                                null,
                                null,
                                null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("dialog_save_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExpense) ExpenseRed else IncomeGreen
                    )
                ) {
                    Text(
                        text = "Save Entry",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
