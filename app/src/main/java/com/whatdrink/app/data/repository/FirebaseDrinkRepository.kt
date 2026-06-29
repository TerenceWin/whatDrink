package com.whatdrink.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.whatdrink.app.data.model.Drink
import com.whatdrink.app.data.model.LogEntry
import com.whatdrink.app.data.model.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseDrinkRepository : DrinkRepository {

    private val db = FirebaseFirestore.getInstance()
    private val drinks = db.collection("drinkDetails")
    private val reviews = db.collection("reviews")
    private val logs = db.collection("drinkLog")

    override suspend fun getDrinkByBarcode(barcode: String): Drink? {
        val result = drinks.whereEqualTo("barcode", barcode).limit(1).get().await()
        return result.documents.firstOrNull()?.toObject(Drink::class.java)
    }

    override suspend fun getDrinkById(id: String): Drink? {
        return drinks.document(id).get().await().toObject(Drink::class.java)
    }

    override suspend fun searchDrinks(query: String): List<Drink> {
        val result = drinks
            .orderBy("name")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .get().await()
        return result.toObjects(Drink::class.java)
    }

    override fun getTrendingDrinks(): Flow<List<Drink>> = callbackFlow {
        val listener = drinks
            .orderBy("likeCount", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(Drink::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getNewReleaseDrinks(): Flow<List<Drink>> = callbackFlow {
        val listener = drinks
            .orderBy("firstScannedAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(Drink::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
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
        val drinkRef = drinks.document(review.drinkId)
        db.runTransaction { transaction ->
            val drink = transaction.get(drinkRef).toObject(Drink::class.java)!!
            val newCount = drink.reviewCount + 1
            val newAvg = (drink.averageRating * drink.reviewCount + review.rating) / newCount
            transaction.update(drinkRef, mapOf(
                "averageRating" to newAvg,
                "reviewCount" to newCount
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
        drinks.document(drinkId).update("likeCount",
            com.google.firebase.firestore.FieldValue.increment(1)).await()
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
    db.collection("users").document(userId).set(user).await()
}
}