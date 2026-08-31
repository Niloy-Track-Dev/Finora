package com.niloy.finora.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
                "EXPENSE" -> tx.type != "RECEIVED"
                else -> true
            }
            val matchesQuery = searchQuery.isEmpty() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    tx.category.contains(searchQuery, ignoreCase = true) ||
                    (tx.note.contains(searchQuery, ignoreCase = true))

            matchesFilter && matchesQuery
        }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(tx.date))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // HEADER
        Text(
            text = "Transaction Records",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // SEARCH BAR
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by title, category, or note...", color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceLight,
                unfocusedContainerColor = SurfaceLight,
                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = BorderSubtle
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_transactions_input")
        )

        // FILTER CHIPS
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All (${transactions.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryTeal,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedFilter == "INCOME",
                onClick = { selectedFilter = "INCOME" },
                label = { Text("Income") },
                leadingIcon = {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = IncomeGreen,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedFilter == "EXPENSE",
                onClick = { selectedFilter = "EXPENSE" },
                label = { Text("Expense") },
                leadingIcon = {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ExpenseRed,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
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
                Text(
                    text = "No matching records found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                groupedTransactions.forEach { (dateHeader, items) ->
                    item {
                        Text(
                            text = dateHeader,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(items, key = { it.id }) { tx ->
                        TransactionCardItem(
                            transaction = tx,
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            }
        }
    }
}
