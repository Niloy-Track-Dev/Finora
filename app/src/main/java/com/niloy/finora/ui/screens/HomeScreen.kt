package com.niloy.finora.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.finora.data.model.Transaction
import com.niloy.finora.ui.theme.*
import com.niloy.finora.ui.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: FinanceViewModel,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle(initialValue = false)

    // Calculate Bank & Cash positions
    val bankIncome = remember(transactions) {
        transactions.filter { it.type == "RECEIVED" && (!it.bankName.isNullOrBlank() || it.paymentMethod == "Bank") }.sumOf { it.amount }
    }
    val withdrawnCash = remember(transactions) {
        transactions.filter { it.type == "WITHDRAWAL" }.sumOf { it.amount }
    }
    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "RECEIVED" }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val bankBalance = bankIncome - withdrawnCash

    val cashReceivedDirectly = remember(transactions) {
        transactions.filter { it.type == "RECEIVED" && (it.bankName.isNullOrBlank() && it.paymentMethod == "Cash") }.sumOf { it.amount }
    }
    val cashExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" && it.paymentMethod == "Cash" }.sumOf { it.amount }
    }
    val cashInHand = withdrawnCash + cashReceivedDirectly - cashExpense
    val netBalance = totalIncome - totalExpense

    // Today's Expense
    val todayExpense = remember(transactions) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        transactions.filter { it.type == "EXPENSE" && it.date >= startMs }.sumOf { it.amount }
    }

    // This Month's Expense
    val thisMonthExpense = remember(transactions) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        transactions.filter { it.type == "EXPENSE" && it.date >= startMs }.sumOf { it.amount }
    }

    val recentTransactions = remember(transactions) {
        transactions.take(5)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // TOP GREETING HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Assalamu Alaikum",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Mother's Ledger",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                // Invisible Toggle Button
                IconButton(
                    onClick = { viewModel.setBalanceHidden(!isBalanceHidden) },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Balance",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // PREMIUM INTELLECTUAL BALANCE STACK (No cards, high visual prominence)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Tracked Cash",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isBalanceHidden) "••••••" else formatCurrency(netBalance),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    modifier = Modifier.testTag("home_total_balance")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal)
                    )
                    Text(
                        text = "This Month",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = if (isBalanceHidden) "••••" else "${formatCurrency(thisMonthExpense)} spent",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ExpenseRed
                    )
                }
            }
        }

        // THREE-WAY BALANCES ROW (Bank, Cash In Hand, Today's Spent)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bank
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bank Account", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBalanceHidden) "••••" else formatCurrency(bankBalance),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentBlue
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(BorderSubtle)
                )

                // Cash in hand
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text("Cash in Hand", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBalanceHidden) "••••" else formatCurrency(cashInHand),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryTeal
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(BorderSubtle)
                )

                // Today spent
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text("Today's Spent", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isBalanceHidden) "••••" else formatCurrency(todayExpense),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = ExpenseRed
                    )
                }
            }
        }

        // SPENDING OVERVIEW (SMOOTH BEZIER SPARKLINE)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Spending Trend",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Household spent curve (Last 7 Days)",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryTealContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "WEEKLY FLOW",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Points for Trend Curve
                val points = remember(transactions) {
                    val last7Days = (0..6).map { dayOffset ->
                        val checkCal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -dayOffset)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val startMs = checkCal.timeInMillis
                        checkCal.set(Calendar.HOUR_OF_DAY, 23)
                        checkCal.set(Calendar.MINUTE, 59)
                        checkCal.set(Calendar.SECOND, 59)
                        val endMs = checkCal.timeInMillis

                        val dayTotal = transactions
                            .filter { it.type == "EXPENSE" && it.date in startMs..endMs }
                            .sumOf { it.amount }
                        dayTotal.toFloat()
                    }.reversed()
                    last7Days
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(vertical = 4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val maxVal = (points.maxOrNull() ?: 100f).coerceAtLeast(100f)

                        val path = Path()
                        val fillPath = Path()
                        val stepX = width / (points.size - 1).coerceAtLeast(1)

                        points.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = height - (value / maxVal * (height * 0.8f)) - (height * 0.05f)

                            if (index == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (index - 1) * stepX
                                val prevY = height - (points[index - 1] / maxVal * (height * 0.8f)) - (height * 0.05f)

                                val controlX1 = prevX + (stepX / 2f)
                                val controlY1 = prevY
                                val controlX2 = prevX + (stepX / 2f)
                                val controlY2 = y

                                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                            }

                            if (index == points.size - 1) {
                                fillPath.lineTo(x, height)
                                fillPath.close()
                            }
                        }

                        // Gradient Area Fill under path
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(PrimaryTealLight.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )

                        // Draw smooth line
                        drawPath(
                            path = path,
                            color = PrimaryTeal,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Highlight dots
                        points.forEachIndexed { index, value ->
                            val x = index * stepX
                            val y = height - (value / maxVal * (height * 0.8f)) - (height * 0.05f)

                            if (value > 0f) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = PrimaryTeal,
                                    radius = 2.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val sdf = SimpleDateFormat("EEE", Locale.getDefault())
                    val weekdays = (0..6).map { dayOffset ->
                        val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -dayOffset) }
                        sdf.format(c.time)
                    }.reversed()

                    weekdays.forEach { dayName ->
                        Text(
                            text = dayName,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // QUICK ADD SHORTCUT
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(16.dp))
                    .clickable { onAddTransactionClick() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Entry",
                    tint = PrimaryTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Record Cash Ledger Entry",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
            }
        }

        // RECENT TRANSACTIONS HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                
                Text(
                    text = "See Logs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal,
                    modifier = Modifier.clickable { /* Handled via bottom bar tab selection */ }
                )
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                EmptyStateCard(onAddClick = onAddTransactionClick)
            }
        } else {
            items(recentTransactions, key = { it.id }) { tx ->
                TransactionCardItem(
                    transaction = tx,
                    onDelete = { viewModel.deleteTransaction(tx) }
                )
            }
        }
    }
}

@Composable
fun TransactionCardItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == "RECEIVED"
    val isWithdrawal = transaction.type == "WITHDRAWAL"

    val dateString = remember(transaction.date) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transaction.date))
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

    // Borderless beautiful transaction row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(16.dp))
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
                        if (isIncome) "Income Received" else if (isWithdrawal) "Cash Withdrawal" else "Expense"
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
                        text = dateString,
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
}

@Composable
fun EmptyStateCard(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(20.dp))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(PrimaryTealContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = PrimaryTeal,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "No Cash Movements Yet",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Track your family spending and cash in hand easily.",
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.height(38.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Your First Entry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun formatCurrency(amount: Double): String {
    return "৳" + String.format(Locale.US, "%,.2f", amount)
}
