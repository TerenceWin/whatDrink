package com.whatdrink.app.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.whatdrink.app.data.model.Drink
import com.whatdrink.app.data.model.DrinkDetail
import com.whatdrink.app.data.model.DrinkStats
import com.whatdrink.app.data.model.LogEntry
import com.whatdrink.app.data.model.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DrinkRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : DrinkRepository {

    private val reviews = firestore.collection("reviews")
    private val logs = firestore.collection("drinkLog")

    override suspend fun getDrinkByBarcode(barcode: String): Drink? {
    val detailDoc = firestore.collection("drinkDetails").document(barcode).get().await()
    if (detailDoc.exists()) {
        return detailDoc.toDrink()
    }

  
    val snapshot = firestore
        .collection("drinks")
        .whereEqualTo("barcode", barcode)
        .get()
        .await()
    if (!snapshot.isEmpty) {
        return snapshot.documents.first().toDrink()
    }

  
    val yahooResult = try {
        com.whatdrink.app.data.remote.YahooShoppingApi.fetchByBarcode(barcode)
    } catch (e: Exception) {
        Log.w("DrinkRepo", "Yahoo API lookup failed for $barcode: ${e.message}")
        null
    } ?: return null

   
    val drinkData = hashMapOf(
        "barcode" to yahooResult.barcode,
        "name" to mapOf("en" to yahooResult.nameEn, "ja" to yahooResult.nameJa),
        "brand" to yahooResult.brand,
        "category" to yahooResult.category,
        "imageUrl" to yahooResult.imageUrl,
        "source" to "yahoo",
        "description" to mapOf("en" to yahooResult.descriptionEn, "ja" to yahooResult.descriptionJa),
        "nutrition" to mapOf(
            "calories" to 0, "protein" to 0.0, "fat" to 0.0,
            "carbohydrates" to 0.0, "sugar" to 0.0,
            "sodium" to 0.0, "caffeine" to 0.0
        )
    )
    firestore.collection("drinkDetails").document(barcode).set(drinkData).await()

    val statsData = hashMapOf(
        "barcode" to barcode,
        "views" to 0,
        "averageRating" to 0.0,
        "commentCount" to 0
    )
    firestore.collection("drinkStats").document(barcode).set(statsData).await()

    return Drink(
    id = barcode,
    barcode = barcode,
    name = mapOf("en" to yahooResult.nameEn, "ja" to yahooResult.nameJa),
    brand = yahooResult.brand,
    imageUrl = yahooResult.imageUrl,
    category = yahooResult.category,
    description = mapOf("en" to yahooResult.descriptionEn, "ja" to yahooResult.descriptionJa)
)
}

    override suspend fun getDrinkById(id: String): Drink? {
        return firestore.collection("drinks").document(id).get().await().toDrink()
    }

    override suspend fun getDrinkDetail(id: String): DrinkDetail? {
        val doc = firestore.collection("drinkDetails").document(id).get().await()
        if (!doc.exists()) return null
        return doc.toDrinkDetail()
    }

    override suspend fun getDrinkStats(id: String): DrinkStats? {
        val doc = firestore.collection("drinkStats").document(id).get().await()
        if (!doc.exists()) return null
        return doc.toDrinkStats()
    }

    override suspend fun incrementViews(id: String) {
        firestore.collection("drinkStats").document(id)
            .update("views", FieldValue.increment(1))
            .await()
    }

    override suspend fun getImageUrl(barcode: String): String? {
        return try {
            storage.reference.child("drinks/$barcode.webp").downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.w("DrinkRepo", "Storage image not found for $barcode: ${e.message}")
            null
        }
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
            .update("views", FieldValue.increment(1)).await()
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
private fun DocumentSnapshot.toDrinkStats(): DrinkStats {
    return DrinkStats(
        id = id,
        averageRating = (get("averageRating") as? Number)?.toDouble() ?: 0.0,
        views = getLong("views")?.toInt() ?: 0,
        ranking = getLong("ranking")?.toInt() ?: 0,
        commentCount = getLong("commentCount")?.toInt() ?: 0
    )
}

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toDrinkDetail(): DrinkDetail {
    return DrinkDetail(
        id = id,
        name = get("name") as? Map<String, String> ?: emptyMap(),
        nutrition = get("nutrition") as? Map<String, Any> ?: emptyMap()
    )
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
