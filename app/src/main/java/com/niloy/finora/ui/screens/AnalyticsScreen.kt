package com.niloy.finora.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.finora.ui.theme.*
import com.niloy.finora.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedPeriod by remember { mutableStateOf("MONTHLY") } // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"

    // Filter transactions based on selection period
    val expenses = remember(transactions, selectedPeriod) {
        val now = Calendar.getInstance()
        val limitMs = when (selectedPeriod) {
            "DAILY" -> {
                now.set(Calendar.HOUR_OF_DAY, 0)
                now.set(Calendar.MINUTE, 0)
                now.set(Calendar.SECOND, 0)
                now.set(Calendar.MILLISECOND, 0)
                now.timeInMillis
            }
            "WEEKLY" -> {
                now.add(Calendar.DAY_OF_YEAR, -7)
                now.timeInMillis
            }
            "MONTHLY" -> {
                now.set(Calendar.DAY_OF_MONTH, 1)
                now.set(Calendar.HOUR_OF_DAY, 0)
                now.set(Calendar.MINUTE, 0)
                now.timeInMillis
            }
            else -> {
                now.set(Calendar.DAY_OF_YEAR, 1)
                now.timeInMillis
            }
        }
        transactions.filter { it.type == "EXPENSE" && it.date >= limitMs }
    }

    val totalExpense = remember(expenses) {
        expenses.sumOf { it.amount }
    }

    val transactionCount = expenses.size

    val dailyAverage = remember(totalExpense, selectedPeriod, expenses) {
        if (expenses.isEmpty()) 0.0 else {
            val days = when (selectedPeriod) {
                "DAILY" -> 1
                "WEEKLY" -> 7
                "MONTHLY" -> 30
                else -> 365
            }
            totalExpense / days
        }
    }

    val categoryBreakdown = remember(expenses) {
        expenses
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val segmentColors = remember {
        listOf(
            PrimaryTeal,
            AccentBlue,
            AccentPurple,
            IncomeGreen,
            Color(0xFFF59E0B), // Warm Amber
            Color(0xFFEC4899), // Premium Pink
            Color(0xFF06B6D4), // Cyan Indigo
            Color(0xFF84CC16)  // Elegant Lime
        )
    }

    // Identify Highest Spending Category and Day
    val topCategoryName = categoryBreakdown.firstOrNull()?.first ?: "None"
    val highestExpenseDayName = remember(expenses) {
        if (expenses.isEmpty()) "None" else {
            val sdf = SimpleDateFormat("MMMM dd", Locale.getDefault())
            val maxTx = expenses.maxByOrNull { it.amount }
            if (maxTx != null) sdf.format(Date(maxTx.date)) else "None"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // HEADER
        item {
            Column {
                Text(
                    text = "Spending Insights",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Financial Reports",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }
        }

        // PERIOD SELECTOR
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val periods = listOf(
                    "DAILY" to "Today",
                    "WEEKLY" to "Weekly",
                    "MONTHLY" to "Monthly",
                    "YEARLY" to "Yearly"
                )
                items(periods) { (key, label) ->
                    val isSelected = selectedPeriod == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) PrimaryTeal else Color.White)
                            .border(BorderStroke(0.5.dp, if (isSelected) Color.Transparent else BorderSubtle), RoundedCornerShape(10.dp))
                            .clickable { selectedPeriod = key }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        // HIERARCHY METRICS ROW (Total Expense, Average Daily, Transactions count)
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
                // Total Expense
                Column(modifier = Modifier.weight(1.2f)) {
                    Text("TOTAL OUTFLOW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCurrency(totalExpense),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderSubtle))

                // Average Daily
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("DAILY AVG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatCurrency(dailyAverage),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderSubtle))

                // Total Transactions
                Column(modifier = Modifier.weight(0.8f).padding(start = 12.dp)) {
                    Text("ENTRIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$transactionCount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        // POLISHED DONUT CHART
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category Breakdown",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.DonutLarge,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (categoryBreakdown.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No spending data for this period.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val sizeMin = size.minDimension - strokeWidth
                            val centerOffset = (size.width - sizeMin) / 2f

                            var startAngle = -90f
                            categoryBreakdown.forEachIndexed { index, (_, amount) ->
                                val sweepAngle = (amount / totalExpense).toFloat() * 360f
                                val color = segmentColors[index % segmentColors.size]

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = androidx.compose.ui.geometry.Offset(centerOffset, centerOffset),
                                    size = Size(sizeMin, sizeMin),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        // Center Text inside Donut Hole
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "OUTFLOW",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = formatCurrency(totalExpense),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // INTELLIGENT COMPANION ADVISORY INSIGHTS
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFEF2F2)) // Soft warm outline
                        .border(BorderStroke(0.5.dp, Color(0xFFFEE2E2)), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = "Insight",
                                tint = ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Household Advice",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "• Your highest spending category is $topCategoryName.",
                            fontSize = 12.sp,
                            color = Color(0xFF7F1D1D)
                        )
                        Text(
                            text = "• Highest spending spike was recorded on $highestExpenseDayName.",
                            fontSize = 12.sp,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }
            }
        }

        // CATEGORY RANKINGS
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Text(
                    text = "Spending Breakdown",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            itemsIndexed(categoryBreakdown) { index, (category, amount) ->
                val percentage = if (totalExpense > 0) (amount / totalExpense).toFloat() else 0f
                val color = segmentColors[index % segmentColors.size]

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
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            
                            // Sleek progress bar
                            LinearProgressIndicator(
                                progress = { percentage },
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = color,
                                trackColor = BorderSubtle
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(amount),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Text(
                            text = "${(percentage * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
