package com.capstonecobconnect.myapplication.wallet

data class Transaction(
    val userId: String = "",
    val type: String = "",       // e.g., "Cash-In", "Cash-Out"
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val transactionType: String = ""
)
