@file:OptIn(ExperimentalMaterial3Api::class)
package com.niloy.finora.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niloy.finora.data.model.Category
import com.niloy.finora.data.model.Transaction
import com.niloy.finora.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedFilterCategory.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedFilterType.collectAsStateWithLifecycle()
    val amountRange by viewModel.amountRange.collectAsStateWithLifecycle()
    val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()

    var showFilterDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Transactions", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                actions = {
                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.testTag("filter_dialog_button")
                    ) {
                        val hasActiveFilters = selectedCategory != null || selectedType != null || 
                                amountRange.first != null || amountRange.second != null || 
                                dateRange.first != null || dateRange.second != null
                        BadgedBox(
                            badge = { 
                                if (hasActiveFilters) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Advanced Filters"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by title, sender, note...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    singleLine = true
                )
            }

            // Quick Filter Info Bar
            if (selectedCategory != null || selectedType != null || sortOrder != "Newest") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Filters:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    
                    selectedType?.let { type ->
                        SuggestionChip(
                            onClick = { viewModel.setFilterType(null) },
                            label = { Text(type, fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(10.dp)) }
                        )
                    }

                    selectedCategory?.let { cat ->
                        SuggestionChip(
                            onClick = { viewModel.setFilterCategory(null) },
                            label = { Text(cat, fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(10.dp)) }
                        )
                    }

                    if (sortOrder != "Newest") {
                        SuggestionChip(
                            onClick = { viewModel.setSortOrder("Newest") },
                            label = { Text(sortOrder, fontSize = 11.sp) },
                            icon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(10.dp)) }
                        )
                    }
                }
            }

            // List of Transactions
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Matches Found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting your filters or search keywords.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.resetFilters() }) {
                            Text("Clear All Filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        SwipeableTransactionCard(
                            transaction = tx,
                            isBalanceHidden = isBalanceHidden,
                            onDeleteClick = { transactionToDelete = tx }
                        )
                    }
                }
            }
        }
    }

    // Advanced Filters Dialog
    if (showFilterDialog) {
        AdvancedFilterDialog(
            currentType = selectedType,
            currentCategory = selectedCategory,
            currentSort = sortOrder,
            currentAmountRange = amountRange,
            currentDateRange = dateRange,
            categoriesList = categories,
            onDismiss = { showFilterDialog = false },
            onApply = { type, category, sort, amt, dates ->
                viewModel.setFilterType(type)
                viewModel.setFilterCategory(category)
                viewModel.setSortOrder(sort)
                viewModel.setAmountRange(amt.first, amt.second)
                viewModel.setDateRange(dates.first, dates.second)
                showFilterDialog = false
            },
            onReset = {
                viewModel.resetFilters()
                showFilterDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to permanently delete this transaction? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        transactionToDelete = null
                    },
                    modifier = Modifier.testTag("delete_confirm_button")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SwipeableTransactionCard(
    transaction: Transaction,
    isBalanceHidden: Boolean,
    onDeleteClick: () -> Unit
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

    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(transaction.date))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = transaction.category,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isWithdrawal) "Cash Withdrawal" else transaction.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isExpense) "${transaction.category} • $dateStr" else if (transaction.bankName != null) "${transaction.bankName} • $dateStr" else dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
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
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expanded details if any optional fields exist
            if (transaction.note.isNotBlank() || !transaction.location.isNullOrBlank() || 
                !transaction.reason.isNullOrBlank() || !transaction.sender.isNullOrBlank()) {
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (transaction.type == "RECEIVED" && !transaction.sender.isNullOrBlank()) {
                        DetailBadge(label = "Sender", value = transaction.sender, icon = Icons.Default.Person)
                    }
                    if (transaction.type != "EXPENSE" && !transaction.bankName.isNullOrBlank()) {
                        DetailBadge(label = "Bank", value = transaction.bankName, icon = Icons.Default.AccountBalance)
                    }
                    if (!transaction.location.isNullOrBlank()) {
                        DetailBadge(label = "Location", value = transaction.location, icon = Icons.Default.Place)
                    }
                    if (!transaction.reason.isNullOrBlank()) {
                        DetailBadge(label = "Reason", value = transaction.reason, icon = Icons.Default.QuestionMark)
                    }
                }

                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: ${transaction.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun DetailBadge(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
        Text(text = "$label: $value", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
    }
}

@Composable
fun AdvancedFilterDialog(
    currentType: String?,
    currentCategory: String?,
    currentSort: String,
    currentAmountRange: Pair<Double?, Double?>,
    currentDateRange: Pair<Long?, Long?>,
    categoriesList: List<Category>,
    onDismiss: () -> Unit,
    onApply: (type: String?, category: String?, sort: String, amt: Pair<Double?, Double?>, dates: Pair<Long?, Long?>) -> Unit,
    onReset: () -> Unit
) {
    var type by remember { mutableStateOf(currentType) }
    var category by remember { mutableStateOf(currentCategory) }
    var sort by remember { mutableStateOf(currentSort) }
    
    var minAmount by remember { mutableStateOf(currentAmountRange.first?.toString() ?: "") }
    var maxAmount by remember { mutableStateOf(currentAmountRange.second?.toString() ?: "") }

    var showDatePickerRange by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(currentDateRange.first) }
    var endDate by remember { mutableStateOf(currentDateRange.second) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter & Sort",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sort Order
                Text(text = "Sort By", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                val sortOptions = listOf("Newest", "Oldest", "Highest Amount", "Lowest Amount")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortOptions.take(2).forEach { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { sort = option },
                            label = { Text(option, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sortOptions.drop(2).forEach { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { sort = option },
                            label = { Text(option, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Transaction Type
                Text(text = "Transaction Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EXPENSE", "RECEIVED", "WITHDRAWAL").forEach { txType ->
                        FilterChip(
                            selected = type == txType,
                            onClick = { type = if (type == txType) null else txType },
                            label = { Text(txType, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount range
                Text(text = "Amount Range (৳)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = minAmount,
                        onValueChange = { minAmount = it },
                        placeholder = { Text("Min", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxAmount,
                        onValueChange = { maxAmount = it },
                        placeholder = { Text("Max", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date Picker trigger
                Text(text = "Date Filter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val rangeText = if (startDate != null && endDate != null) {
                    "${sdf.format(Date(startDate!!))} - ${sdf.format(Date(endDate!!))}"
                } else {
                    "All Time"
                }

                OutlinedCard(
                    onClick = { showDatePickerRange = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = rangeText, style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Default.DateRange, contentDescription = "Choose Dates", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset")
                    }
                    Button(
                        onClick = {
                            val minVal = minAmount.toDoubleOrNull()
                            val maxVal = maxAmount.toDoubleOrNull()
                            onApply(type, category, sort, Pair(minVal, maxVal), Pair(startDate, endDate))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }

    if (showDatePickerRange) {
        // Date range picker standard in Material 3
        val state = rememberDateRangePickerState()
        AlertDialog(
            onDismissRequest = { showDatePickerRange = false },
            title = { Text("Select Date Range") },
            text = {
                Box(modifier = Modifier.height(400.dp)) {
                    DateRangePicker(state = state)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDate = state.selectedStartDateMillis
                        endDate = state.selectedEndDateMillis
                        showDatePickerRange = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerRange = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
