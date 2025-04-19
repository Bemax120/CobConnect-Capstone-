package com.capstonecobconnect.myapplication.cobblers.HomeService

data class ServiceClass(
    val name: String,
    val address: String,
    val problem: String,
    val dateShed: String,
    val phone: String,
    val customerDocumentId: String,
    val documentId: String,
    val status: String,
    val geoCoding: String,
    val latitude: Double?,
    val longitude: Double?,
    var services: List<String> = listOf()

)

