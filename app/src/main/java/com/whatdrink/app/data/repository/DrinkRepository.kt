package com.whatdrink.app.data.repository

import com.whatdrink.app.data.model.Drink
import com.whatdrink.app.data.model.DrinkDetail
import com.whatdrink.app.data.model.DrinkStats
import com.whatdrink.app.data.model.Comment
import com.whatdrink.app.data.model.Review
import com.whatdrink.app.data.model.LogEntry
import com.whatdrink.app.data.model.User
import kotlinx.coroutines.flow.Flow

interface DrinkRepository {
    suspend fun getDrinkByBarcode(barcode: String): Drink?
    suspend fun getDrinkById(id: String): Drink?
    suspend fun getDrinkDetail(id: String): DrinkDetail?
    suspend fun getDrinkStats(id: String): DrinkStats?
    suspend fun incrementViews(id: String)
    suspend fun getImageUrl(barcodeOrGsUrl: String): String?
    fun getTrendingDrinks(): Flow<List<Drink>>
    fun getNewReleaseDrinks(): Flow<List<Drink>>
    suspend fun searchDrinks(query: String): List<Drink>

    fun getReviewsForDrink(drinkId: String): Flow<List<Review>>
    suspend fun submitReview(review: Review)

    fun getLogForUser(userId: String): Flow<List<LogEntry>>
    suspend fun addToLog(userId: String, drinkId: String)
    suspend fun removeFromLog(logEntryId: String)
    suspend fun saveUser(userId: String, username: String, email: String)
    suspend fun getUser(userId: String): User?
    suspend fun updateUsername(userId: String, newUsername: String)
    fun getComments(drinkId: String): Flow<List<Comment>>
    suspend fun addComment(drinkId: String, userId: String, username: String, context: String)
    suspend fun getProfileImages(): List<String>
    suspend fun updateProfileImage(userId: String, gsUrl: String)
    fun getCommentsByUser(userId: String): Flow<List<Comment>>
    suspend fun getRatingsByUser(userId: String): List<Review>
}

