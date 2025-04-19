package com.capstonecobconnect.myapplication.cobblers.HomeService

data class ServiceItem(
    val quantity: Int = 0,
    val totalPrice: Double = 0.0,
    val services: List<String> = listOf()
)
