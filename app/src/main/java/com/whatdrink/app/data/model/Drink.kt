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
    val reviewCount: Int = 0,
    val isNewRelease: Boolean = false,
    val isTrending: Boolean = false,
)
