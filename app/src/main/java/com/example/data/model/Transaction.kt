package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "RECEIVED", "WITHDRAWAL", "EXPENSE"
    val date: Long, // timestamp in ms
    val amount: Double,
    val title: String, // for EXPENSE (title), for RECEIVED (can represent note or sender), for WITHDRAWAL ("Cash Withdrawal")
    val category: String = "Others", // for EXPENSE
    val subCategory: String? = null,
    val location: String? = null,
    val reason: String? = null,
    val paymentMethod: String = "Cash",
    val note: String = "",
    val receiptUri: String? = null,
    val sender: String? = null, // for RECEIVED
    val bankName: String? = null // for RECEIVED and WITHDRAWAL
)
