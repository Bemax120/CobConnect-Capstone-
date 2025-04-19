package com.capstonecobconnect.myapplication.store.client

sealed class CartEntry {
    data class Header(val shopId: String, var shopName: String = "", var shopLogo: String = "") : CartEntry()
    data class Item(val cartItem: CartItem) : CartEntry()
}
