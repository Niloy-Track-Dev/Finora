package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    var selectedGraphPeriod by remember { mutableStateOf("Last 7 Days") } // "Last 7 Days", "Weekly", "Monthly", "Yearly"
    var selectedCategoryTrend by remember { mutableStateOf("Groceries") }

    // Core computations
    val totalReceived = transactions.filter { it.type == "RECEIVED" }.sumOf { it.amount }
    val totalWithdrawn = transactions.filter { it.type == "WITHDRAWAL" }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val remainingCash = totalWithdrawn - totalExpense

    val expenses = transactions.filter { it.type == "EXPENSE" }

    // Average daily expense
    val activeDays = expenses.map {
        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
        "" + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH)
    }.distinct().size.coerceAtLeast(1)
    val avgDailyExpense = totalExpense / activeDays

    val highestExpense = expenses.maxOfOrNull { it.amount } ?: 0.0

    // Highest spending day computation
    val daySums = expenses.groupBy {
        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(cal.time)
    }.mapValues { it.value.sumOf { tx -> tx.amount } }
    val highestSpendingDayEntry = daySums.maxByOrNull { it.value }
    val highestSpendingDay = highestSpendingDayEntry?.key ?: "N/A"
    val highestSpendingDayAmount = highestSpendingDayEntry?.value ?: 0.0

    // Month-over-Month calculation
    val cal = Calendar.getInstance()
    val thisMonth = cal.get(Calendar.MONTH)
    val thisYear = cal.get(Calendar.YEAR)
    
    val currentMonthSpend = expenses.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.date }
        c.get(Calendar.MONTH) == thisMonth && c.get(Calendar.YEAR) == thisYear
    }.sumOf { it.amount }

    cal.add(Calendar.MONTH, -1)
    val lastMonth = cal.get(Calendar.MONTH)
    val lastMonthYear = cal.get(Calendar.YEAR)
    val lastMonthSpend = expenses.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.date }
        c.get(Calendar.MONTH) == lastMonth && c.get(Calendar.YEAR) == lastMonthYear
    }.sumOf { it.amount }

    val momChange = if (lastMonthSpend > 0) {
        ((currentMonthSpend - lastMonthSpend) / lastMonthSpend) * 100
    } else {
        0.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Analytics & Reports", 
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
            // Money Flow Visualization Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Money Flow Flowchart",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FlowSegment(title = "Bank Rcvd", amount = totalReceived, color = Color(0xFF059669))
                            Icon(Icons.Default.TrendingFlat, contentDescription = null, tint = Color.LightGray)
                            FlowSegment(title = "Cash Withdr", amount = totalWithdrawn, color = Color(0xFF2563EB))
                            Icon(Icons.Default.TrendingFlat, contentDescription = null, tint = Color.LightGray)
                            FlowSegment(title = "Expenses", amount = totalExpense, color = Color(0xFFDC2626))
                            Icon(Icons.Default.TrendingFlat, contentDescription = null, tint = Color.LightGray)
                            FlowSegment(title = "Cash Hand", amount = remainingCash, color = Color(0xFFD97706))
                        }
                    }
                }
            }

            // Trend Graph period selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spending Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Last 7 Days", "Weekly", "Monthly", "Yearly").forEach { period ->
                            val selected = selectedGraphPeriod == period
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.White)
                                    .clickable { selectedGraphPeriod = period }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (period == "Last 7 Days") "7D" else if (period == "Weekly") "W" else if (period == "Monthly") "M" else "Y",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Trend Chart Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "$selectedGraphPeriod Spending",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val chartData = getChartData(expenses, selectedGraphPeriod)
                        if (chartData.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No expenses recorded for this period", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            BarChart(data = chartData)
                        }
                    }
                }
            }

            // Category Breakdown Donut Pie Chart Card
            item {
                Text(
                    text = "Category Share",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val catShares = expenses
                            .groupBy { it.category }
                            .mapValues { it.value.sumOf { tx -> tx.amount } }
                            .toList()
                            .sortedByDescending { it.second }

                        if (catShares.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Add some expenses to see category share", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .weight(1.2f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DonutChart(shares = catShares)
                                }
                                
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    catShares.take(4).forEachIndexed { idx, pair ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(getChartColor(idx))
                                            )
                                            Text(
                                                text = "${pair.first}: ${formatTaka(pair.second)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Spending Heatmap Card (Last 30 Days)
            item {
                Text(
                    text = "Spending Heatmap (Last 30 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Daily Spending Intensity",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        SpendingHeatmap(expenses = expenses)
                    }
                }
            }

            // Month-over-Month Comparison Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Month-over-Month Change", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val up = momChange > 0
                                Icon(
                                    imageVector = if (up) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (up) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f%%", Math.abs(momChange)),
                                    fontWeight = FontWeight.Bold,
                                    color = if (up) Color(0xFFEF4444) else Color(0xFF10B981)
                                )
                                Text(
                                    text = if (up) " more than last month" else " less than last month",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Analytics Metrics Cards Section
            item {
                Text(
                    text = "Historical Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricMiniCard(title = "Transactions", value = "${transactions.size}", label = "Total Entries", modifier = Modifier.weight(1f))
                    MetricMiniCard(title = "Highest Day", value = formatTaka(highestSpendingDayAmount), label = highestSpendingDay, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun FlowSegment(
    title: String,
    amount: Double,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = formatTaka(amount), fontSize = 11.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
fun MetricMiniCard(
    title: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun BarChart(data: List<Pair<String, Double>>) {
    val maxValue = (data.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { pair ->
            val barHeightFraction = (pair.second / maxValue).toFloat()
            val animatedHeight by animateFloatAsState(
                targetValue = barHeightFraction,
                animationSpec = tween(durationMillis = 500)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (pair.second > 0) formatShortTaka(pair.second) else "",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .fillMaxWidth(0.6f)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (pair.second > 0) MaterialTheme.colorScheme.primary else Color.LightGray.copy(
                                alpha = 0.2f
                            )
                        )
                        .fillMaxHeight(animatedHeight)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pair.first,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DonutChart(shares: List<Pair<String, Double>>) {
    val total = shares.sumOf { it.second }.coerceAtLeast(1.0)
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        var startAngle = -90f
        shares.forEachIndexed { index, pair ->
            val sweepAngle = ((pair.second / total) * 360f).toFloat()
            drawArc(
                color = getChartColor(index),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 30f, cap = StrokeCap.Round),
                size = Size(size.width - 30f, size.height - 30f),
                topLeft = Offset(15f, 15f)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun SpendingHeatmap(expenses: List<Transaction>) {
    // Grid of last 28 days (4 weeks x 7 days)
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -27) // start 4 weeks ago

    val daySumMap = expenses.groupBy {
        val c = Calendar.getInstance().apply { timeInMillis = it.date }
        "" + c.get(Calendar.YEAR) + "-" + c.get(Calendar.MONTH) + "-" + c.get(Calendar.DAY_OF_MONTH)
    }.mapValues { it.value.sumOf { tx -> tx.amount } }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Draw a neat calendar grid representing spending intensities
            for (week in 0 until 4) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (day in 0 until 7) {
                        val currentKey = "" + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH)
                        val spend = daySumMap[currentKey] ?: 0.0

                        val intensityColor = when {
                            spend <= 0 -> Color(0xFFF1F5F9) // No spend
                            spend < 500 -> Color(0xFFFEE2E2) // Low
                            spend < 2000 -> Color(0xFFFCA5A5) // Medium
                            else -> Color(0xFFEF4444) // High
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(intensityColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (spend > 0) {
                                Text(
                                    text = "" + cal.get(Calendar.DAY_OF_MONTH),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (spend > 2000) Color.White else Color.DarkGray
                                )
                            }
                        }
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", fontSize = 10.sp, color = Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFF1F5F9)))
            Spacer(modifier = Modifier.width(2.dp))
            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFEE2E2)))
            Spacer(modifier = Modifier.width(2.dp))
            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFCA5A5)))
            Spacer(modifier = Modifier.width(2.dp))
            Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFEF4444)))
            Spacer(modifier = Modifier.width(4.dp))
            Text("More", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

// Helpers for Trend Chart Data extraction
fun getChartData(expenses: List<Transaction>, period: String): List<Pair<String, Double>> {
    val cal = Calendar.getInstance()
    return when (period) {
        "Last 7 Days" -> {
            val list = mutableListOf<Pair<String, Double>>()
            for (i in 0 until 7) {
                val start = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = start + 24 * 60 * 60 * 1000L
                val spend = expenses.filter { it.date in start until end }.sumOf { it.amount }
                
                val sdf = SimpleDateFormat("E", Locale.getDefault())
                val label = sdf.format(Date(start))
                list.add(Pair(label, spend))
            }
            list.reversed()
        }
        "Weekly" -> {
            // Last 4 weeks
            val list = mutableListOf<Pair<String, Double>>()
            for (i in 0 until 4) {
                val start = Calendar.getInstance().apply {
                    add(Calendar.WEEK_OF_YEAR, -i)
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = start + 7 * 24 * 60 * 60 * 1000L
                val spend = expenses.filter { it.date in start until end }.sumOf { it.amount }
                list.add(Pair("W${4-i}", spend))
            }
            list.reversed()
        }
        "Monthly" -> {
            // Last 6 months
            val list = mutableListOf<Pair<String, Double>>()
            for (i in 0 until 6) {
                val start = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -i)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val cEnd = Calendar.getInstance().apply {
                    timeInMillis = start
                    add(Calendar.MONTH, 1)
                }
                val end = cEnd.timeInMillis
                val spend = expenses.filter { it.date in start until end }.sumOf { it.amount }
                
                val sdf = SimpleDateFormat("MMM", Locale.getDefault())
                val label = sdf.format(Date(start))
                list.add(Pair(label, spend))
            }
            list.reversed()
        }
        "Yearly" -> {
            // Last 3 Years
            val list = mutableListOf<Pair<String, Double>>()
            for (i in 0 until 3) {
                val start = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -i)
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                val cEnd = Calendar.getInstance().apply {
                    timeInMillis = start
                    add(Calendar.YEAR, 1)
                }
                val end = cEnd.timeInMillis
                val spend = expenses.filter { it.date in start until end }.sumOf { it.amount }
                
                val sdf = SimpleDateFormat("yyyy", Locale.getDefault())
                val label = sdf.format(Date(start))
                list.add(Pair(label, spend))
            }
            list.reversed()
        }
        else -> emptyList()
    }
}

fun getChartColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF0F172A), // Slate
        Color(0xFF10B981), // Emerald
        Color(0xFF3B82F6), // Blue
        Color(0xFFF59E0B), // Amber
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF14B8A6)  // Teal
    )
    return colors[index % colors.size]
}

fun formatShortTaka(amount: Double): String {
    return if (amount >= 1000) {
        String.format("৳%.1fk", amount / 1000)
    } else {
        "৳${amount.toInt()}"
    }
}
