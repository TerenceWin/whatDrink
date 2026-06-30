package com.whatdrink.app.data.model

data class DrinkStats(
    val id: String = "",
    val averageRating: Double = 0.0,
    val views: Int = 0,
    val ranking: Int = 0,
    val commentCount: Int = 0
)
