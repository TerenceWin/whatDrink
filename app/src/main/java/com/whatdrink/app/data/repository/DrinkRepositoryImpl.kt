package com.whatdrink.app.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

    private val reviews = firestore.collection("reviews")
    private val logs = firestore.collection("drinkLog")

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
        return firestore.collection("drinks").document(id).get().await().toDrink()
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

    override fun getNewReleaseDrinks(): Flow<List<Drink>> = callbackFlow {
        val listener = firestore.collection("drinks")
            .orderBy("ranking", Query.Direction.ASCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.documents?.mapNotNull { it.toDrink() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun searchDrinks(query: String): List<Drink> {
        val snapshot = firestore.collection("drinks").get().await()
        val q = query.trim().lowercase()
        return snapshot.documents.mapNotNull { it.toDrink() }.filter { drink ->
            drink.name.values.any { it.lowercase().contains(q) } ||
                drink.brand.lowercase().contains(q)
        }
    }

    override fun getReviewsForDrink(drinkId: String): Flow<List<Review>> = callbackFlow {
        val listener = reviews
            .whereEqualTo("drinkId", drinkId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(Review::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun submitReview(review: Review) {
        reviews.add(review).await()
        val drinkRef = firestore.collection("drinks").document(review.drinkId)
        firestore.runTransaction { transaction ->
            val drink = transaction.get(drinkRef).toDrink()
            val newCount = drink.commentCount + 1
            val newAvg = (drink.averageRating * drink.commentCount + review.rating) / newCount
            transaction.update(drinkRef, mapOf(
                "averageRating" to newAvg,
                "commentCount" to newCount
            ))
        }.await()
    }

    override fun getLogForUser(userId: String): Flow<List<LogEntry>> = callbackFlow {
        val listener = logs
            .whereEqualTo("userId", userId)
            .orderBy("scannedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(LogEntry::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addToLog(userId: String, drinkId: String) {
        val log = hashMapOf(
            "userId" to userId,
            "drinkId" to drinkId,
            "scannedAt" to com.google.firebase.Timestamp.now()
        )
        logs.add(log).await()
        firestore.collection("drinks").document(drinkId)
            .update("views", com.google.firebase.firestore.FieldValue.increment(1)).await()
    }

    override suspend fun removeFromLog(logEntryId: String) {
        logs.document(logEntryId).delete().await()
    }

    suspend fun saveUser(userId: String, displayName: String, email: String) {
        val user = hashMapOf(
            "userId" to userId,
            "displayName" to displayName,
            "email" to email,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        firestore.collection("users").document(userId).set(user).await()
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