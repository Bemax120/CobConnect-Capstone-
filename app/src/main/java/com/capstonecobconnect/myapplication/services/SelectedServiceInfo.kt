package com.capstonecobconnect.myapplication.services

import java.io.Serializable

data class SelectedServiceInfo(
    val services: List<ServiceItem>,
    var quantity: Int = 1
) : Serializable
