package com.capstonecobconnect.myapplication.store.client

data class CartItem(
    val cartItemId: String = "", // Firestore document ID
    var name: String = "",
    var imageUrl: String = "",
    var price: Double = 0.0,
    var size: String = "",
    var quantity: Int = 1,
    var storeName: String? = null,
    var shopID: String? = null,
    var documentId: String? = null,
    var cobblerLogo: String? = null,
    var isChecked: Boolean = false
)
