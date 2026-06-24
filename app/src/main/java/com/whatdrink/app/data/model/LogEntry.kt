package com.whatdrink.app.data.model

data class LogEntry(
    val id: String = "",
    val userId: String = "",
    val drinkId: String = "",
    val drink: Drink? = null,
    val timestamp: Long = 0L,
)
