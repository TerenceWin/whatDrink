package com.whatdrink.app.data.model

data class Drink(
    val id: String = "",
    val barcode: String = "",
    val name: Map<String, String> = emptyMap(), // { "en": "Peach Tea", "ja": "ピーチティー" }
    val brand: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val description: Map<String, String> = emptyMap(),
    val averageRating: Double = 0.0,
    val views: Int = 0,
    val ranking: Int = 0,
    val commentCount: Int = 0
)
