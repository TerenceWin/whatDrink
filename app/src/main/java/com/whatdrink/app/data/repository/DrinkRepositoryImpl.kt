package com.whatdrink.app.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.whatdrink.app.data.model.Drink
import com.whatdrink.app.data.model.LogEntry
import com.whatdrink.app.data.model.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DrinkRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DrinkRepository {

    override suspend fun getDrinkByBarcode(barcode: String): Drink? {
        val snapshot = firestore
            .collection("drinks")
            .whereEqualTo("barcode", barcode)
            .get()
            .await()

        if (snapshot.isEmpty) return null
        return snapshot.documents.first().toDrink()
    }

    override suspend fun getDrinkById(id: String): Drink? {
        TODO("Not yet implemented")
    }

    override fun getTrendingDrinks(): Flow<List<Drink>> = callbackFlow {
        Log.d("DrinkRepo", "Firebase project: ${firestore.app.options.projectId}")
        Log.d("DrinkRepo", "Setting up Firestore listener")
        val listener = firestore.collection("drinks")
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DrinkRepo", "Firestore error: ${error.code} — ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                Log.d("DrinkRepo", "Snapshot: ${snapshot?.size()} docs, fromCache=${snapshot?.metadata?.isFromCache}")
                if (snapshot?.metadata?.isFromCache == true && snapshot.isEmpty) {
                    Log.w("DrinkRepo", "Cache empty — waiting for server response")
                    return@addSnapshotListener
                }
                val drinks = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toDrink().also { Log.d("DrinkRepo", "Parsed: ${it.id} name=${it.name}") }
                    } catch (e: Exception) {
                        Log.e("DrinkRepo", "Failed to parse doc ${doc.id}: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                Log.d("DrinkRepo", "Sending ${drinks.size} drinks to flow")
                trySend(drinks.sortedBy { it.ranking })
            }
        awaitClose { listener.remove() }
    }

    override fun getNewReleaseDrinks(): Flow<List<Drink>> {
        TODO("Not yet implemented")
    }

    override suspend fun searchDrinks(query: String): List<Drink> {
        TODO("Not yet implemented")
    }

    override fun getReviewsForDrink(drinkId: String): Flow<List<Review>> {
        TODO("Not yet implemented")
    }

    override suspend fun submitReview(review: Review) {
        TODO("Not yet implemented")
    }

    override fun getLogForUser(userId: String): Flow<List<LogEntry>> {
        TODO("Not yet implemented")
    }

    override suspend fun addToLog(userId: String, drinkId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun removeFromLog(logEntryId: String) {
        TODO("Not yet implemented")
    }
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toDrink(): Drink {
    return Drink(
        id = id,
        barcode = getString("barcode") ?: "",
        name = get("name") as? Map<String, String> ?: emptyMap(),
        brand = getString("brand") ?: "",
        imageUrl = getString("imageUrl") ?: "",
        category = getString("category") ?: "",
        description = get("description") as? Map<String, String> ?: emptyMap(),
        averageRating = (get("averageRating") as? Number)?.toDouble() ?: 0.0,
        views = getLong("views")?.toInt() ?: 0,
        commentCount = getLong("commentCount")?.toInt() ?: 0,
        ranking = getLong("ranking")?.toInt() ?: 0
    )
}
