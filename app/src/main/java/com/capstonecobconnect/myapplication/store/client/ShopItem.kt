package com.capstonecobconnect.myapplication.store.client

import java.io.Serializable

data class ShopItem(
    val productImage: String,
    val name: String,
    val price: String,
    val ratings: Float,
    val description: String,
    val sizes: List<Size>,
    val documentId: String,
    val shopID: String,
    val shopName: String,
    val cobblerLogo: String
) : Serializable
