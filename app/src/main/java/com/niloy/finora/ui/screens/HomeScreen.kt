package com.niloy.finora.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.finora.data.model.Transaction
import com.niloy.finora.ui.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FinanceViewModel,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()

    // Calculations
    val totalReceived = transactions.filter { it.type == "RECEIVED" }.sumOf { it.amount }
    val totalWithdrawn = transactions.filter { it.type == "WITHDRAWAL" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    
    val remainingCash = totalWithdrawn - totalExpense
    val bankBalance = totalReceived - totalWithdrawn

    // Time-based calculations
    val now = Calendar.getInstance()
    
    // Today
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val todayExpenses = transactions.filter { it.type == "EXPENSE" && it.date >= startOfToday }.sumOf { it.amount }

    // This Week
    val startOfWeek = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val weekExpenses = transactions.filter { it.type == "EXPENSE" && it.date >= startOfWeek }.sumOf { it.amount }

    // This Month
    val startOfMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val monthExpenses = transactions.filter { it.type == "EXPENSE" && it.date >= startOfMonth }.sumOf { it.amount }

    // Metrics
    val activeDays = transactions.filter { it.type == "EXPENSE" }
        .map { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            "" + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH)
        }.distinct().size.coerceAtLeast(1)
    val avgDailyExpense = totalExpense / activeDays

    val highestExpenseTx = transactions.filter { it.type == "EXPENSE" }.maxByOrNull { it.amount }
    val highestExpense = highestExpenseTx?.amount ?: 0.0

    val categorySums = transactions.filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    val topCategory = categorySums.maxByOrNull { it.value }?.key ?: "N/A"

    val recentTransactions = transactions.take(5)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Cash Tracker", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setBalanceHidden(!isBalanceHidden) },
                        modifier = Modifier.testTag("toggle_balance_button")
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Balance"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC), // Beautiful white-slate background
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Cash Balance Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_balance_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "REMAINING CASH (IN HAND)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isBalanceHidden) "৳ ••••" else formatTaka(remainingCash),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Divider(color = Color.White.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Bank Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBalanceHidden) "৳ ••••" else formatTaka(bankBalance),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Spent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatTaka(totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Money Flow Visualization Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Money Flow Visualization",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FlowItem(
                                title = "Received",
                                amount = totalReceived,
                                color = Color(0xFF10B981),
                                icon = Icons.Default.CallReceived,
                                isHidden = isBalanceHidden
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "moves to",
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            FlowItem(
                                title = "Withdrawn",
                                amount = totalWithdrawn,
                                color = Color(0xFF3B82F6),
                                icon = Icons.Default.AccountBalanceWallet,
                                isHidden = isBalanceHidden
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "moves to",
                                tint = Color.Gray.copy(alpha = 0.5f)
                            )
                            FlowItem(
                                title = "Expenses",
                                amount = totalExpense,
                                color = Color(0xFFEF4444),
                                icon = Icons.Default.TrendingUp,
                                isHidden = false
                            )
                        }
                    }
                }
            }

            // Quick Stats Grid
            item {
                Text(
                    text = "Quick Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Today's Spent",
                        value = formatTaka(todayExpenses),
                        icon = Icons.Default.Today,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "This Week",
                        value = formatTaka(weekExpenses),
                        icon = Icons.Default.DateRange,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "This Month",
                        value = formatTaka(monthExpenses),
                        icon = Icons.Default.CalendarMonth,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Daily Average",
                        value = formatTaka(avgDailyExpense),
                        icon = Icons.Default.QueryStats,
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Highest Expense",
                        value = formatTaka(highestExpense),
                        icon = Icons.Default.ArrowUpward,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Top Category",
                        value = topCategory,
                        icon = Icons.Default.PieChart,
                        color = Color(0xFF0D9488),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Transactions Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    EmptyStateCard(onAddTransactionClick = onAddTransactionClick)
                }
            } else {
                items(recentTransactions, key = { it.id }) { transaction ->
                    TransactionItem(transaction = transaction, isBalanceHidden = isBalanceHidden)
                }
            }
        }
    }
}

@Composable
fun FlowItem(
    title: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    isHidden: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isHidden) "৳••" else formatTaka(amount),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == "EXPENSE"
    val isWithdrawal = transaction.type == "WITHDRAWAL"
    val color = when (transaction.type) {
        "RECEIVED" -> Color(0xFF10B981)
        "WITHDRAWAL" -> Color(0xFF3B82F6)
        else -> Color(0xFFEF4444)
    }
    val icon = when (transaction.type) {
        "RECEIVED" -> Icons.Default.CallReceived
        "WITHDRAWAL" -> Icons.Default.AccountBalanceWallet
        else -> getCategoryIcon(transaction.category)
    }

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateStr = sdf.format(Date(transaction.date))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.category,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isWithdrawal) "Cash Withdrawal" else transaction.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isExpense) "${transaction.category} • $dateStr" else if (transaction.bankName != null) "${transaction.bankName} • $dateStr" else dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val hideValue = isBalanceHidden && transaction.type != "EXPENSE"
                Text(
                    text = if (hideValue) "৳••••" else "${if (isExpense) "-" else "+"} ${formatTaka(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 80.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Transactions Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start tracking by adding physical cash, withdrawals, or household expenses.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAddTransactionClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add First Transaction")
            }
        }
    }
}

// Global Helpers for UI Formatting
fun formatTaka(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("bn", "BD"))
    val formatted = format.format(amount)
    // Clean up symbol to fit simple Bangladesh styling ৳
    return "৳" + formatted.replace(Regex("[^0-9,.]"), "")
}

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.ROOT)) {
        "groceries" -> Icons.Default.ShoppingCart
        "transport" -> Icons.Default.DirectionsTransit
        "household" -> Icons.Default.Home
        "health" -> Icons.Default.LocalHospital
        "food" -> Icons.Default.Restaurant
        "education" -> Icons.Default.School
        "personal" -> Icons.Default.Person
        "communication" -> Icons.Default.PhoneAndroid
        "religious" -> Icons.Default.Church
        "shopping" -> Icons.Default.ShoppingBag
        else -> Icons.Default.Category
    }
}
