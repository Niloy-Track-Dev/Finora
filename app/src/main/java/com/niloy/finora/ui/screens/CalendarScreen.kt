package com.niloy.finora.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun CalendarScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())

    val calendar = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    val monthName = remember(currentMonth, currentYear) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, currentMonth)
            set(Calendar.YEAR, currentYear)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val daysInMonth = remember(currentMonth, currentYear) {
        Calendar.getInstance().apply {
            set(Calendar.MONTH, currentMonth)
            set(Calendar.YEAR, currentYear)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Days with transactions in current month
    val daysWithTransactions = remember(transactions, currentMonth, currentYear) {
        transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }.map { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
            cal.get(Calendar.DAY_OF_MONTH)
        }.toSet()
    }

    val selectedDateTransactions = remember(transactions, selectedDay, currentMonth, currentYear) {
        transactions.filter { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
            cal.get(Calendar.DAY_OF_MONTH) == selectedDay &&
                    cal.get(Calendar.MONTH) == currentMonth &&
                    cal.get(Calendar.YEAR) == currentYear
        }
    }

    val dayIncome = remember(selectedDateTransactions) {
        selectedDateTransactions.filter { it.type == "RECEIVED" }.sumOf { it.amount }
    }
    val dayExpense = remember(selectedDateTransactions) {
        selectedDateTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // HEADER & NAVIGATION CONTROLS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Calendar Tracker",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = monthName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            // Elegant navigation arrows
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentMonth == 0) {
                            currentMonth = 11
                            currentYear--
                        } else {
                            currentMonth--
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(BorderStroke(0.5.dp, BorderSubtle), CircleShape)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = {
                        if (currentMonth == 11) {
                            currentMonth = 0
                            currentYear++
                        } else {
                            currentMonth++
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(BorderStroke(0.5.dp, BorderSubtle), CircleShape)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // CALENDAR CARD WITH EMBEDDED GRID
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            // Days of week header
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { dayName ->
                    Text(
                        text = dayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMuted,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // CALENDAR GRID
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(daysInMonth) { dayIdx ->
                    val day = dayIdx + 1
                    val isSelected = day == selectedDay
                    val hasTransaction = daysWithTransactions.contains(day)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .width(36.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(if (isSelected) PrimaryTeal else Color.Transparent)
                            .clickable { selectedDay = day }
                    ) {
                        Text(
                            text = "$day",
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimary
                        )

                        // Heat dot indicator
                        if (hasTransaction) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else PrimaryTeal)
                            )
                        }
                    }
                }
            }
        }

        // DAY METRICS SUMMARY PILL CARD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(BorderStroke(0.5.dp, BorderSubtle), RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryTealContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EventNote, contentDescription = "Day Stats", tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "Day $selectedDay Summary",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (dayIncome > 0) {
                    Text(
                        text = "+${formatCurrency(dayIncome)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = IncomeGreen
                    )
                }
                if (dayExpense > 0) {
                    Text(
                        text = "-${formatCurrency(dayExpense)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = ExpenseRed
                    )
                }
                if (dayIncome == 0.0 && dayExpense == 0.0) {
                    Text(
                        text = "No flows",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // SELECTED DAY TRANSACTIONS LIST
        if (selectedDateTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No cash records found for this date.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(selectedDateTransactions, key = { it.id }) { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        onDelete = { viewModel.deleteTransaction(tx) }
                    )
                }
            }
        }
    }
}
