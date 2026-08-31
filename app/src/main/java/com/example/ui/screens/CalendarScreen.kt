package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()

    var currentCalendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // Group transactions by date for fast indicator checks
    val dateGroups = remember(transactions) {
        transactions.groupBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            "" + cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH)
        }
    }

    // Selected Day Calculations
    val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    val selectedKey = "" + selectedCal.get(Calendar.YEAR) + "-" + selectedCal.get(Calendar.MONTH) + "-" + selectedCal.get(Calendar.DAY_OF_MONTH)
    val dayTransactions = dateGroups[selectedKey] ?: emptyList()

    val dayExpenses = dayTransactions.filter { it.type == "EXPENSE" }
    val dayReceived = dayTransactions.filter { it.type == "RECEIVED" }
    val dayWithdrawals = dayTransactions.filter { it.type == "WITHDRAWAL" }

    val totalDayExpense = dayExpenses.sumOf { it.amount }
    val totalDayReceived = dayReceived.sumOf { it.amount }
    val totalDayWithdrawal = dayWithdrawals.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Financial Calendar", 
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
            // Calendar month selector and Grid Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header with left/right arrows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = currentCalendarMonth.timeInMillis
                                        add(Calendar.MONTH, -1)
                                    }
                                    currentCalendarMonth = newCal
                                }
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                            }

                            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                            Text(
                                text = monthFormat.format(currentCalendarMonth.time),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = {
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = currentCalendarMonth.timeInMillis
                                        add(Calendar.MONTH, 1)
                                    }
                                    currentCalendarMonth = newCal
                                }
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Week Days Labels Row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days Grid
                        val daysGrid = getDaysForMonth(currentCalendarMonth)
                        daysGrid.forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                week.forEach { date ->
                                    if (date == null) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    } else {
                                        val isSelected = isSameDay(date.timeInMillis, selectedDateMillis)
                                        val dateKey = "" + date.get(Calendar.YEAR) + "-" + date.get(Calendar.MONTH) + "-" + date.get(Calendar.DAY_OF_MONTH)
                                        val dayTxs = dateGroups[dateKey] ?: emptyList()
                                        val hasExpenses = dayTxs.any { it.type == "EXPENSE" }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                                )
                                                .clickable { selectedDateMillis = date.timeInMillis }
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "" + date.get(Calendar.DAY_OF_MONTH),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                                                )
                                                if (hasExpenses) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else Color(0xFFEF4444))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Day Summary Details Panel
            item {
                val daySdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                Text(
                    text = "Summary: ${daySdf.format(Date(selectedDateMillis))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Quick Info stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniDayStat(title = "Spent", value = formatTaka(totalDayExpense), color = Color(0xFFEF4444), modifier = Modifier.weight(1f))
                    MiniDayStat(title = "Received", value = formatTaka(totalDayReceived), color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                    MiniDayStat(title = "Withdrawn", value = formatTaka(totalDayWithdrawal), color = Color(0xFF3B82F6), modifier = Modifier.weight(1f))
                }
            }

            // Day Transactions list
            if (dayTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No transactions on this day.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(dayTransactions, key = { it.id }) { tx ->
                    TransactionItem(transaction = tx, isBalanceHidden = isBalanceHidden)
                }
            }
        }
    }
}

@Composable
fun MiniDayStat(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

fun getDaysForMonth(calendar: Calendar): List<List<Calendar?>> {
    val grid = mutableListOf<List<Calendar?>>()
    
    val tempCal = Calendar.getInstance().apply {
        timeInMillis = calendar.timeInMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
    val totalDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    var currentDay = 1
    // Loop through weeks (max 6 weeks)
    for (w in 0 until 6) {
        val weekList = mutableListOf<Calendar?>()
        for (d in 1..7) {
            if (w == 0 && d < firstDayOfWeek) {
                weekList.add(null)
            } else if (currentDay > totalDays) {
                weekList.add(null)
            } else {
                val dayCal = Calendar.getInstance().apply {
                    timeInMillis = tempCal.timeInMillis
                    set(Calendar.DAY_OF_MONTH, currentDay)
                }
                weekList.add(dayCal)
                currentDay++
            }
        }
        grid.add(weekList)
        if (currentDay > totalDays) break
    }

    return grid
}

fun isSameDay(millis1: Long, millis2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
            cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
}
