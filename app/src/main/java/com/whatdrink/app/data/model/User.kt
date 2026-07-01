package com.whatdrink.app.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val memberSince: Timestamp? = null,
    val reviewsCount: Int = 0,
    val ratingCount: Int = 0,
    val profileImage: String = ""
)
