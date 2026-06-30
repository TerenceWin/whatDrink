package com.whatdrink.app.data.model

data class DrinkDetail(
    val id: String = "",
    val name: Map<String, String> = emptyMap(),
    val nutrition: Map<String, Any> = emptyMap()
)
