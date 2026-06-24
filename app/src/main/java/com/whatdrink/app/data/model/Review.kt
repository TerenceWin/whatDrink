package com.whatdrink.app.data.model

data class Review(
    val id: String = "",
    val drinkId: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val userPhotoUrl: String = "",
    val rating: Int = 0, // 1–5
    val comment: String = "",
    val timestamp: Long = 0L,
)
