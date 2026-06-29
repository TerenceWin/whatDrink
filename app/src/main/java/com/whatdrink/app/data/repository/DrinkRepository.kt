package com.whatdrink.app.data.repository

import com.whatdrink.app.data.model.Drink
import com.whatdrink.app.data.model.Review
import com.whatdrink.app.data.model.LogEntry
import kotlinx.coroutines.flow.Flow

interface DrinkRepository {
    suspend fun getDrinkByBarcode(barcode: String): Drink?
    suspend fun getDrinkById(id: String): Drink?
    fun getTrendingDrinks(): Flow<List<Drink>>
    fun getNewReleaseDrinks(): Flow<List<Drink>>
    suspend fun searchDrinks(query: String): List<Drink>

    fun getReviewsForDrink(drinkId: String): Flow<List<Review>>
    suspend fun submitReview(review: Review)

    fun getLogForUser(userId: String): Flow<List<LogEntry>>
    suspend fun addToLog(userId: String, drinkId: String)
    suspend fun removeFromLog(logEntryId: String)
}

