package com.niloy.finora.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE"

    val filteredTransactions = remember(transactions, searchQuery, selectedFilter) {
        transactions.filter { tx ->
            val matchesFilter = when (selectedFilter) {
                "INCOME" -> tx.type == "RECEIVED"
                "EXPENSE" -> tx.type == "EXPENSE"
                else -> true
            }
            val matchesQuery = searchQuery.isEmpty() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    tx.note.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesQuery
        }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            getRelativeDateHeader(tx.date)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // HEADER
        Column {
            Text(
                text = "Account Ledger",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Transaction History",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }

        // SEARCH BAR (MINIMAL, ELEGANT WHITE FIELD)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search title, category, notes...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = BorderSubtle,
                focusedLabelColor = PrimaryTeal
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_transactions_input")
        )

        // FILTER CHIPS (HIGHLY BALANCED AND SPACIOUS)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // All Chip
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All Logs (${transactions.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryTeal,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = BorderSubtle,
                    selectedBorderColor = Color.Transparent,
                    enabled = true,
                    selected = selectedFilter == "ALL"
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Income Chip
            FilterChip(
                selected = selectedFilter == "INCOME",
                onClick = { selectedFilter = "INCOME" },
                label = { Text("Income", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (selectedFilter == "INCOME") Color.White else IncomeGreen
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IncomeGreen,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = BorderSubtle,
                    selectedBorderColor = Color.Transparent,
                    enabled = true,
                    selected = selectedFilter == "INCOME"
                ),
                shape = RoundedCornerShape(10.dp)
            )

            // Expense Chip
            FilterChip(
                selected = selectedFilter == "EXPENSE",
                onClick = { selectedFilter = "EXPENSE" },
                label = { Text("Expense", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (selectedFilter == "EXPENSE") Color.White else ExpenseRed
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ExpenseRed,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = BorderSubtle,
                    selectedBorderColor = Color.Transparent,
                    enabled = true,
                    selected = selectedFilter == "EXPENSE"
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // TRANSACTION LIST
        if (groupedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(BorderStroke(0.5.dp, BorderSubtle), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SearchOff, contentDescription = "No Results", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "No matching transactions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "Try searching with other words.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedTransactions.forEach { (dateHeader, items) ->
                    // INTELLECTUAL DATE GROUP SECTION (Minimal & crisp text header)
                    item {
                        Text(
                            text = dateHeader,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }

                    items(items, key = { it.id }) { tx ->
                        TransactionRowItem(
                            transaction = tx,
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == "RECEIVED"
    val isWithdrawal = transaction.type == "WITHDRAWAL"

    val timeString = remember(transaction.date) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.date))
    }

    val categoryIcon = remember(transaction.category, transaction.type) {
        when {
            isIncome -> Icons.Default.CallReceived
            isWithdrawal -> Icons.Default.AccountBalanceWallet
            else -> {
                val catLower = transaction.category.lowercase()
                when {
                    catLower.contains("grocery") || catLower.contains("bazar") || catLower.contains("food") -> Icons.Default.Restaurant
                    catLower.contains("medicine") || catLower.contains("doctor") || catLower.contains("health") -> Icons.Default.LocalHospital
                    catLower.contains("bill") || catLower.contains("utilities") || catLower.contains("rent") -> Icons.Default.Receipt
                    catLower.contains("cloth") || catLower.contains("shopping") -> Icons.Default.Checkroom
                    catLower.contains("gift") -> Icons.Default.Redeem
                    else -> Icons.Default.Category
                }
            }
        }
    }

    val iconContainerColor = remember(transaction.type) {
        when {
            isIncome -> IncomeBg
            isWithdrawal -> Color(0xFFEFF6FF)
            else -> ExpenseBg
        }
    }

    val iconTintColor = remember(transaction.type) {
        when {
            isIncome -> IncomeGreen
            isWithdrawal -> AccentBlue
            else -> ExpenseRed
        }
    }

    // Modern Swipeable / Clickable Actions or simple row with delete icon
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(16.dp))
            .clickable { showDeleteConfirm = true }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = transaction.title.ifEmpty {
                        if (isIncome) "Income" else if (isWithdrawal) "Cash Withdrawal" else "Expense"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val sign = when {
                isIncome -> "+"
                isWithdrawal -> "⇄"
                else -> "-"
            }
            val amountColor = when {
                isIncome -> IncomeGreen
                isWithdrawal -> AccentBlue
                else -> ExpenseRed
            }
            Text(
                text = "$sign ${formatCurrency(transaction.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = amountColor
            )

            Text(
                text = transaction.paymentMethod,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Would you like to delete this cash movement row from the ledger database permanently?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun getRelativeDateHeader(dateMs: Long): String {
    val calTx = Calendar.getInstance().apply { timeInMillis = dateMs }
    val calToday = Calendar.getInstance()
    val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        calTx.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                calTx.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR) -> "TODAY"
        calTx.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                calTx.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR) -> "YESTERDAY"
        else -> SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(dateMs)).uppercase()
    }
}
