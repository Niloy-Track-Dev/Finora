package com.niloy.finora.data.repository

import com.niloy.finora.data.local.CategoryDao
import com.niloy.finora.data.local.TransactionDao
import com.niloy.finora.data.model.Category
import com.niloy.finora.data.model.Transaction
import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteTransactionById(id)

    suspend fun insertCategory(category: Category): Long =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category)

    suspend fun importBackup(transactions: List<Transaction>, categories: List<Category>) {
        transactionDao.deleteAllTransactions()
        categoryDao.deleteAllCategories()
        
        transactionDao.insertTransactions(transactions)
        
        // Ensure default categories are always present if not in import
        val sysCategories = categories.filter { it.isSystem }
        val finalCategories = categories.toMutableList()
        val defaultNames = listOf(
            "Groceries", "Transport", "Household", "Health", "Food",
            "Education", "Personal", "Communication", "Religious", "Shopping", "Others"
        )
        defaultNames.forEach { name ->
            if (categories.none { it.name.equals(name, ignoreCase = true) }) {
                finalCategories.add(Category(name = name, isSystem = true))
            }
        }
        categoryDao.insertCategories(finalCategories)
    }

    suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        categoryDao.deleteAllCategories()
        
        // Reseed defaults
        val defaultCategories = listOf(
            Category(name = "Groceries", isSystem = true),
            Category(name = "Transport", isSystem = true),
            Category(name = "Household", isSystem = true),
            Category(name = "Health", isSystem = true),
            Category(name = "Food", isSystem = true),
            Category(name = "Education", isSystem = true),
            Category(name = "Personal", isSystem = true),
            Category(name = "Communication", isSystem = true),
            Category(name = "Religious", isSystem = true),
            Category(name = "Shopping", isSystem = true),
            Category(name = "Others", isSystem = true)
        )
        categoryDao.insertCategories(defaultCategories)
    }
}
