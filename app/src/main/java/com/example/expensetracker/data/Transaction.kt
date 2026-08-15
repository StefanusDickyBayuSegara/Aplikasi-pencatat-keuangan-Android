package com.example.expensetracker.data

data class Transaction(
    val id: Int = 0,
    val amount: Double,
    val category: String,
    val type: String, // "income" atau "expense"
    val date: Long,   // disimpan sebagai timestamp (System.currentTimeMillis())
    val note: String? = null
)
