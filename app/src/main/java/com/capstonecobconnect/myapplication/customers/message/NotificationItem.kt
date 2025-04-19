package com.capstonecobconnect.myapplication.customers.message

data class NotificationItem(
    val title: String = "",
    val body: String = "",
    val receiverId: String = "",
    val timestamp: Long = 0L
)

