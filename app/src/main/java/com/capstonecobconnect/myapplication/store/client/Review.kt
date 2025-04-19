package com.capstonecobconnect.myapplication.store.client

import com.google.firebase.Timestamp

data class Review(
    val comments: String,
    val date: Timestamp,
    val fname: String,
    val lname: String,
    val profileImage: String,
    val ratings: Int
)
