package com.capstonecobconnect.myapplication.services

import java.io.Serializable

data class ServiceItem(
    val category: String,
    val service: String,
    val price: Double
) : Serializable

