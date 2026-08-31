package com.niloy.finora.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.niloy.finora.data.model.Category
import com.niloy.finora.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Transaction::class, Category::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cash_tracker_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val categoryDao = database.categoryDao()
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
        }
    }
}
