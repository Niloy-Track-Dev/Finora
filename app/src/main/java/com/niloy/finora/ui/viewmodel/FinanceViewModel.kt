package com.niloy.finora.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niloy.finora.data.local.AppDatabase
import com.niloy.finora.data.model.Category
import com.niloy.finora.data.model.Transaction
import com.niloy.finora.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository
    val allTransactions: StateFlow<List<Transaction>>
    val allCategories: StateFlow<List<Category>>

    // Search and Filters State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilterCategory = MutableStateFlow<String?>(null)
    val selectedFilterCategory = _selectedFilterCategory.asStateFlow()

    private val _selectedFilterType = MutableStateFlow<String?>(null)
    val selectedFilterType = _selectedFilterType.asStateFlow()

    private val _amountRange = MutableStateFlow<Pair<Double?, Double?>>(Pair(null, null))
    val amountRange = _amountRange.asStateFlow()

    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(Pair(null, null))
    val dateRange = _dateRange.asStateFlow()

    private val _sortOrder = MutableStateFlow("Newest") // "Newest", "Oldest", "Highest Amount", "Lowest Amount"
    val sortOrder = _sortOrder.asStateFlow()

    // Calendar selected date (millis)
    private val _selectedCalendarDate = MutableStateFlow(System.currentTimeMillis())
    val selectedCalendarDate = _selectedCalendarDate.asStateFlow()

    // App PIN Lock State
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked = _isAppLocked.asStateFlow()

    private val _isBalanceHidden = MutableStateFlow(false)
    val isBalanceHidden = _isBalanceHidden.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("cash_tracker_prefs", Context.MODE_PRIVATE)

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = FinanceRepository(database.transactionDao(), database.categoryDao())
        
        allTransactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allCategories = repository.allCategories.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Read initial pin state
        val hasPin = sharedPrefs.getString("app_pin", null) != null
        _isAppLocked.value = hasPin
        _isBalanceHidden.value = sharedPrefs.getBoolean("hide_balance", false)
    }

    // Data class to capture filters type-safely
    data class TransactionFilters(
        val search: String = "",
        val category: String? = null,
        val type: String? = null,
        val minAmt: Double? = null,
        val maxAmt: Double? = null,
        val startDate: Long? = null,
        val endDate: Long? = null,
        val sort: String = "Newest"
    )

    private val filtersState = combine(
        _searchQuery,
        _selectedFilterCategory,
        _selectedFilterType,
        _amountRange,
        combine(_dateRange, _sortOrder) { dates, sort -> Pair(dates, sort) }
    ) { search, category, type, amtRange, datesAndSort ->
        val (minAmt, maxAmt) = amtRange
        val (dates, sort) = datesAndSort
        val (startDate, endDate) = dates
        TransactionFilters(
            search = search,
            category = category,
            type = type,
            minAmt = minAmt,
            maxAmt = maxAmt,
            startDate = startDate,
            endDate = endDate,
            sort = sort
        )
    }

    // Filtered Transactions Flow
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        filtersState
    ) { list, filters ->
        var result = list

        // Search Query filter
        if (filters.search.isNotBlank()) {
            result = result.filter {
                it.title.contains(filters.search, ignoreCase = true) ||
                it.note.contains(filters.search, ignoreCase = true) ||
                it.sender?.contains(filters.search, ignoreCase = true) == true ||
                it.bankName?.contains(filters.search, ignoreCase = true) == true ||
                it.category.contains(filters.search, ignoreCase = true)
            }
        }

        // Category filter
        if (filters.category != null) {
            result = result.filter { it.category.equals(filters.category, ignoreCase = true) }
        }

        // Transaction Type filter
        if (filters.type != null) {
            result = result.filter { it.type == filters.type }
        }

        // Amount Range filter
        if (filters.minAmt != null) {
            result = result.filter { it.amount >= filters.minAmt }
        }
        if (filters.maxAmt != null) {
            result = result.filter { it.amount <= filters.maxAmt }
        }

        // Date Range filter
        if (filters.startDate != null) {
            result = result.filter { it.date >= filters.startDate }
        }
        if (filters.endDate != null) {
            // End of the day offset to capture full day
            val endOfDay = Calendar.getInstance().apply {
                timeInMillis = filters.endDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            result = result.filter { it.date <= endOfDay }
        }

        // Sort Order
        when (filters.sort) {
            "Newest" -> result.sortedByDescending { it.date }
            "Oldest" -> result.sortedBy { it.date }
            "Highest Amount" -> result.sortedByDescending { it.amount }
            "Lowest Amount" -> result.sortedBy { it.amount }
            else -> result.sortedByDescending { it.date }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Setters for filters
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setFilterCategory(category: String?) { _selectedFilterCategory.value = category }
    fun setFilterType(type: String?) { _selectedFilterType.value = type }
    fun setAmountRange(min: Double?, max: Double?) { _amountRange.value = Pair(min, max) }
    fun setDateRange(start: Long?, end: Long?) { _dateRange.value = Pair(start, end) }
    fun setSortOrder(order: String) { _sortOrder.value = order }
    fun setSelectedCalendarDate(date: Long) { _selectedCalendarDate.value = date }

    fun resetFilters() {
        _searchQuery.value = ""
        _selectedFilterCategory.value = null
        _selectedFilterType.value = null
        _amountRange.value = Pair(null, null)
        _dateRange.value = Pair(null, null)
        _sortOrder.value = "Newest"
    }

    // CRUD Transactions
    fun addTransaction(
        type: String,
        amount: Double,
        title: String,
        category: String = "Others",
        date: Long = System.currentTimeMillis(),
        sender: String? = null,
        bankName: String? = null,
        note: String = "",
        location: String? = null,
        reason: String? = null,
        subCategory: String? = null,
        receiptUri: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val tx = Transaction(
                type = type,
                amount = amount,
                title = title.ifBlank { if (type == "WITHDRAWAL") "Cash Withdrawal" else if (type == "RECEIVED") "Received from $sender" else "Expense" },
                category = category,
                date = date,
                sender = sender,
                bankName = bankName,
                note = note,
                location = location,
                reason = reason,
                subCategory = subCategory,
                receiptUri = receiptUri
            )
            repository.insertTransaction(tx)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransactionById(id)
        }
    }

    // Categories
    fun addCategory(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isNotBlank()) {
                repository.insertCategory(Category(name = name, isSystem = false))
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCategory(category)
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
        }
    }

    // Security Features
    fun setAppPIN(pin: String?) {
        sharedPrefs.edit().apply {
            if (pin == null) {
                remove("app_pin")
                _isAppLocked.value = false
            } else {
                putString("app_pin", pin)
                _isAppLocked.value = false // unlocked immediately after setting
            }
            apply()
        }
    }

    fun getAppPIN(): String? {
        return sharedPrefs.getString("app_pin", null)
    }

    fun lockApp() {
        if (getAppPIN() != null) {
            _isAppLocked.value = true
        }
    }

    fun unlockApp(pin: String): Boolean {
        val savedPin = getAppPIN()
        return if (savedPin == pin) {
            _isAppLocked.value = false
            true
        } else {
            false
        }
    }

    fun setBalanceHidden(hidden: Boolean) {
        _isBalanceHidden.value = hidden
        sharedPrefs.edit().putBoolean("hide_balance", hidden).apply()
    }

    // Export JSON Backup
    suspend fun exportToJsonString(): String = withContext(Dispatchers.IO) {
        val transactionsList = allTransactions.value
        val categoriesList = allCategories.value

        val rootJson = JSONObject()
        
        val txArray = JSONArray()
        transactionsList.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("type", tx.type)
                put("date", tx.date)
                put("amount", tx.amount)
                put("title", tx.title)
                put("category", tx.category)
                put("subCategory", tx.subCategory ?: JSONObject.NULL)
                put("location", tx.location ?: JSONObject.NULL)
                put("reason", tx.reason ?: JSONObject.NULL)
                put("paymentMethod", tx.paymentMethod)
                put("note", tx.note)
                put("receiptUri", tx.receiptUri ?: JSONObject.NULL)
                put("sender", tx.sender ?: JSONObject.NULL)
                put("bankName", tx.bankName ?: JSONObject.NULL)
            }
            txArray.put(obj)
        }
        rootJson.put("transactions", txArray)

        val catArray = JSONArray()
        categoriesList.forEach { cat ->
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("isSystem", cat.isSystem)
            }
            catArray.put(obj)
        }
        rootJson.put("categories", catArray)
        rootJson.put("exportTime", System.currentTimeMillis())
        rootJson.put("version", 1)

        rootJson.toString(2)
    }

    // Import JSON Backup
    suspend fun importFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            
            val txArray = root.getJSONArray("transactions")
            val transactions = mutableListOf<Transaction>()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                transactions.add(
                    Transaction(
                        type = obj.getString("type"),
                        date = obj.getLong("date"),
                        amount = obj.getDouble("amount"),
                        title = obj.getString("title"),
                        category = obj.optString("category", "Others"),
                        subCategory = if (obj.isNull("subCategory")) null else obj.getString("subCategory"),
                        location = if (obj.isNull("location")) null else obj.getString("location"),
                        reason = if (obj.isNull("reason")) null else obj.getString("reason"),
                        paymentMethod = obj.optString("paymentMethod", "Cash"),
                        note = obj.optString("note", ""),
                        receiptUri = if (obj.isNull("receiptUri")) null else obj.getString("receiptUri"),
                        sender = if (obj.isNull("sender")) null else obj.getString("sender"),
                        bankName = if (obj.isNull("bankName")) null else obj.getString("bankName")
                    )
                )
            }

            val catArray = root.optJSONArray("categories")
            val categories = mutableListOf<Category>()
            if (catArray != null) {
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    categories.add(
                        Category(
                            name = obj.getString("name"),
                            isSystem = obj.optBoolean("isSystem", false)
                        )
                    )
                }
            }

            repository.importBackup(transactions, categories)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Export CSV representation
    suspend fun exportToCsvString(): String = withContext(Dispatchers.IO) {
        val transactionsList = allTransactions.value.sortedByDescending { it.date }
        val sb = java.lang.StringBuilder()
        sb.append("Date,Type,Amount (৳),Title,Category,Sender,Bank Name,Note,Location,Reason\n")
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        transactionsList.forEach { tx ->
            val dateStr = sdf.format(Date(tx.date))
            val titleEsc = tx.title.replace("\"", "\"\"")
            val senderEsc = (tx.sender ?: "").replace("\"", "\"\"")
            val bankEsc = (tx.bankName ?: "").replace("\"", "\"\"")
            val noteEsc = tx.note.replace("\"", "\"\"")
            val locEsc = (tx.location ?: "").replace("\"", "\"\"")
            val reasonEsc = (tx.reason ?: "").replace("\"", "\"\"")

            sb.append("\"$dateStr\",")
            sb.append("\"${tx.type}\",")
            sb.append("${tx.amount},")
            sb.append("\"$titleEsc\",")
            sb.append("\"${tx.category}\",")
            sb.append("\"$senderEsc\",")
            sb.append("\"$bankEsc\",")
            sb.append("\"$noteEsc\",")
            sb.append("\"$locEsc\",")
            sb.append("\"$reasonEsc\"\n")
        }
        sb.toString()
    }

    // Helper functions to import from a file URI
    fun importBackupFile(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            val sb = java.lang.StringBuilder()
                            var line: String? = reader.readLine()
                            while (line != null) {
                                sb.append(line)
                                line = reader.readLine()
                            }
                            importFromJson(sb.toString())
                        }
                    } ?: false
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
            onResult(result)
        }
    }

    // Trigger local share of a file
    fun createBackupFile(context: Context, jsonContent: String): File? {
        return try {
            val cacheDir = context.cacheDir
            val backupFile = File(cacheDir, "cashtrack_backup_${System.currentTimeMillis()}.json")
            backupFile.writeText(jsonContent)
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createCsvFile(context: Context, csvContent: String): File? {
        return try {
            val cacheDir = context.cacheDir
            val csvFile = File(cacheDir, "cashtrack_transactions_${System.currentTimeMillis()}.csv")
            csvFile.writeText(csvContent)
            csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
