package com.niloy.finora.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.finora.data.model.Transaction
import com.niloy.finora.ui.theme.*
import com.niloy.finora.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTimeframe by remember { mutableStateOf("TODAY") } // "TODAY", "WEEKLY", "MONTHLY"
    var showMenu by remember { mutableStateOf(false) }
    var activeSubView by remember { mutableStateOf<String?>(null) } // "WITHDRAW" or "SENT"

    var showPINConfirmDialog by remember { mutableStateOf<Transaction?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var showAddModal by remember { mutableStateOf<String?>(null) } // "WITHDRAW" or "SENT"

    val timeframeStart = remember(selectedTimeframe) {
        val cal = Calendar.getInstance()
        when (selectedTimeframe) {
            "TODAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            "WEEKLY" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
            }
            "MONTHLY" -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
            }
        }
        cal.timeInMillis
    }

    val filteredExpenses = remember(transactions, timeframeStart) {
        transactions.filter { it.date >= timeframeStart && it.type == "EXPENSE" }
    }

    val totalExpenseAmount = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    val categoryBreakdown = remember(filteredExpenses) {
        filteredExpenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val withdrawTransactions = remember(transactions) {
        transactions.filter { it.type == "WITHDRAWAL" }
    }

    val sentTransactions = remember(transactions) {
        transactions.filter { it.type == "SENT" || (it.type == "EXPENSE" && it.category.contains("Sent", true)) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // HEADER WITH 3-DOT MENU
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (activeSubView) {
                    "WITHDRAW" -> "Money Withdraw History"
                    "SENT" -> "Sent Money History"
                    else -> "Analytics & Reports"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu Options", tint = TextPrimary)
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Money Withdraw") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = WithdrawOrange) },
                        onClick = {
                            showMenu = false
                            activeSubView = "WITHDRAW"
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Sent Money") },
                        leadingIcon = { Icon(Icons.Default.Send, contentDescription = null, tint = SentPurple) },
                        onClick = {
                            showMenu = false
                            activeSubView = "SENT"
                        }
                    )

                    if (activeSubView != null) {
                        DropdownMenuItem(
                            text = { Text("Expense Analytics") },
                            leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null, tint = BluePrimary) },
                            onClick = {
                                showMenu = false
                                activeSubView = null
                            }
                        )
                    }
                }
            }
        }

        if (activeSubView == "WITHDRAW") {
            // MONEY WITHDRAW VIEW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${withdrawTransactions.size} Records",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Button(
                    onClick = { showAddModal = "WITHDRAW" },
                    colors = ButtonDefaults.buttonColors(containerColor = WithdrawOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Withdraw")
                }
            }

            if (withdrawTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Money Withdraw history found", color = TextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(withdrawTransactions, key = { it.id }) { item ->
                        HistoryCardItem(
                            title = item.title.ifEmpty { "Cash Withdrawal" },
                            person = item.sender ?: "Niloy",
                            bankName = item.bankName ?: "Bank / ATM",
                            amount = item.amount,
                            dateMs = item.date,
                            badgeColor = WithdrawOrange,
                            onDeleteClick = { showPINConfirmDialog = item }
                        )
                    }
                }
            }
        } else if (activeSubView == "SENT") {
            // SENT MONEY VIEW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${sentTransactions.size} Records",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Button(
                    onClick = { showAddModal = "SENT" },
                    colors = ButtonDefaults.buttonColors(containerColor = SentPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Sent Entry")
                }
            }

            if (sentTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Sent Money history found", color = TextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(sentTransactions, key = { it.id }) { item ->
                        HistoryCardItem(
                            title = item.title.ifEmpty { "Sent Money" },
                            person = item.sender ?: "Receiver",
                            bankName = item.bankName ?: "Bank / MFS",
                            amount = item.amount,
                            dateMs = item.date,
                            badgeColor = SentPurple,
                            onDeleteClick = { showPINConfirmDialog = item }
                        )
                    }
                }
            }
        } else {
            // MAIN ANALYTICS VIEW
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("TODAY" to "Today", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly").forEach { (code, label) ->
                    FilterChip(
                        selected = selectedTimeframe == code,
                        onClick = { selectedTimeframe = code },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BluePrimary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // TOTAL EXPENSE SUMMARY CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "$selectedTimeframe Expense Summary",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = formatCurrency(totalExpenseAmount),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }

            Text(
                text = "Category Wise Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (categoryBreakdown.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No expenses recorded for this timeframe", color = TextMuted)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(categoryBreakdown) { (catName, amount) ->
                        val percent = if (totalExpenseAmount > 0) (amount / totalExpenseAmount).toFloat() else 0f

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${formatCurrency(amount)} (${(percent * 100).toInt()}%)",
                                        fontWeight = FontWeight.Bold,
                                        color = BluePrimary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { percent },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = BluePrimary,
                                    trackColor = BorderSubtle
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // PIN CONFIRMATION DIALOG FOR DELETION
    showPINConfirmDialog?.let { target ->
        AlertDialog(
            onDismissRequest = {
                showPINConfirmDialog = null
                pinInput = ""
                pinError = null
            },
            title = { Text("Security Lock Verification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter App PIN to authorize deleting this record.")
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("PIN Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    pinError?.let { err ->
                        Text(err, color = ExpenseRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val appPin = viewModel.getAppPIN() ?: ""
                        if (pinInput == appPin || appPin.isEmpty()) {
                            viewModel.deleteTransaction(target)
                            showPINConfirmDialog = null
                            pinInput = ""
                            pinError = null
                        } else {
                            pinError = "Incorrect PIN code!"
                        }
                    }
                ) {
                    Text("Confirm Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPINConfirmDialog = null
                    pinInput = ""
                    pinError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ADD MODAL FOR WITHDRAW OR SENT
    showAddModal?.let { type ->
        AddSubEntryModal(
            type = type,
            onDismiss = { showAddModal = null },
            onSave = { name, bank, amount ->
                viewModel.addTransaction(
                    type = if (type == "WITHDRAW") "WITHDRAWAL" else "SENT",
                    amount = amount,
                    title = if (type == "WITHDRAW") "Money Withdraw" else "Sent Money",
                    category = if (type == "WITHDRAW") "Bank / ATM" else "Sent Money",
                    date = System.currentTimeMillis(),
                    sender = name,
                    bankName = bank,
                    note = "",
                    location = null,
                    reason = null,
                    subCategory = null
                )
                showAddModal = null
            }
        )
    }
}

@Composable
fun HistoryCardItem(
    title: String,
    person: String,
    bankName: String,
    amount: Double,
    dateMs: Long,
    badgeColor: Color,
    onDeleteClick: () -> Unit
) {
    val dateString = remember(dateMs) {
        SimpleDateFormat("EEEE, dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(dateMs))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = formatCurrency(amount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor
                )
            }

            Text(
                text = "Person: $person • Bank: $bankName",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddSubEntryModal(
    type: String, // "WITHDRAW" or "SENT"
    onDismiss: () -> Unit,
    onSave: (name: String, bank: String, amount: Double) -> Unit
) {
    var personName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (type == "WITHDRAW") "Add Money Withdraw" else "Add Sent Money") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text(if (type == "WITHDRAW") "Person / Who Withdrew" else "Receiver / Person") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name / MFS") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (৳)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onSave(personName.ifEmpty { "Niloy" }, bankName.ifEmpty { "Bank" }, amt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (type == "WITHDRAW") WithdrawOrange else SentPurple)
            ) {
                Text("Save Entry", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
