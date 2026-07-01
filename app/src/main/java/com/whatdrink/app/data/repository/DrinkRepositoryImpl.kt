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
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject

class DrinkRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : DrinkRepository {

    private val drinkDetails = firestore.collection("drinkDetails")
    private val reviews = firestore.collection("reviews")
    private val logs = firestore.collection("drinkLog")

    private val YAHOO_APP_ID = "dmVyPTIwMjUwNyZpZD1BNEsyblFOQk11Jmhhc2g9WkRJeFlXSTVaRGswTTJZd1ptVmlZdw"

    override suspend fun getDrinkByBarcode(barcode: String): Drink? {
        // 1. 先查 Firestore
        val snapshot = drinkDetails
            .whereEqualTo("barcode", barcode)
            .get()
            .await()
        if (!snapshot.isEmpty) {
            return snapshot.documents.first().toDrink()
        }

        // 2. 没有 → 查 Yahoo API
        val drink = fetchFromYahoo(barcode)
        if (drink != null) {
            drinkDetails.document(barcode).set(drinkToMap(drink)).await()
            return drink
        }

        // 3. 还是没有 → 返回 null
        return null
    }

    private suspend fun fetchFromYahoo(barcode: String): Drink? {
        return try {
            val url = "https://shopping.yahooapis.jp/ShoppingWebService/V3/itemSearch?appid=$YAHOO_APP_ID&jan_code=$barcode&results=1"
            val response = URL(url).readText()
            val json = JSONObject(response)
            val hits = json.getJSONArray("hits")
            if (hits.length() == 0) return null

            val item = hits.getJSONObject(0)
            val name = item.getString("name")
            val brand = if (item.has("brand")) item.getJSONObject("brand").getString("name") else ""
            val category = if (item.has("genreCategory")) item.getJSONObject("genreCategory").getString("name") else ""
            val imageUrl = if (item.has("image")) item.getJSONObject("image").getString("medium") else ""
            val description = if (item.has("description")) item.getString("description") else ""

            Drink(
                id = barcode,
                barcode = barcode,
                name = mapOf("ja" to name, "en" to name),
                brand = brand,
                imageUrl = imageUrl,
                category = category,
                description = mapOf("ja" to description, "en" to description),
                averageRating = 0.0,
                views = 0,
                commentCount = 0,
                ranking = 0
            )
        } catch (e: Exception) {
            Log.e("DrinkRepo", "Yahoo API error: ${e.message}")
            null
        }
    }

    private fun drinkToMap(drink: Drink): Map<String, Any> {
        return mapOf(
            "barcode" to drink.barcode,
            "name" to drink.name,
            "brand" to drink.brand,
            "imageUrl" to drink.imageUrl,
            "category" to drink.category,
            "description" to drink.description,
            "averageRating" to drink.averageRating,
            "views" to drink.views,
            "commentCount" to drink.commentCount,
            "ranking" to drink.ranking
        )
    }

    override suspend fun getDrinkById(id: String): Drink? {
        return drinkDetails.document(id).get().await().toDrink()
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

    override suspend fun getImageUrl(barcodeOrGsUrl: String): String? {
        return try {
            val ref = if (barcodeOrGsUrl.startsWith("gs://"))
                storage.getReferenceFromUrl(barcodeOrGsUrl)
            else
                storage.reference.child("drinks/$barcodeOrGsUrl.webp")
            // Build the public HTTPS URL directly — avoids needing an auth token
            val bucket = storage.app.options.storageBucket ?: return null
            val path = ref.path.trimStart('/').let {
                java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }
            val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$path?alt=media"
            Log.d("DrinkRepo", "Built image URL: $url")
            url
        } catch (e: Exception) {
            Log.w("DrinkRepo", "Failed to build image URL for $barcodeOrGsUrl: ${e.message}")
            null
        }
    }

    override fun getTrendingDrinks(): Flow<List<Drink>> = callbackFlow {
        Log.d("DrinkRepo", "Firebase project: ${firestore.app.options.projectId}")
        val listener = firestore.collection("drinks")
            .whereGreaterThan("ranking", 0)
            .orderBy("ranking", Query.Direction.ASCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DrinkRepo", "Firestore error: ${error.code} — ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot?.metadata?.isFromCache == true && snapshot.isEmpty) {
                    return@addSnapshotListener
                }
                val drinks = snapshot?.documents?.mapNotNull { doc ->
                    try { doc.toDrink() } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(drinks.sortedBy { it.ranking })
            }
        awaitClose { listener.remove() }
    }

    override fun getNewReleaseDrinks(): Flow<List<Drink>> = callbackFlow {
        val listener = drinkDetails
            .orderBy("ranking", Query.Direction.ASCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.documents?.mapNotNull { it.toDrink() } ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun searchDrinks(query: String): List<Drink> {
        val snapshot = drinkDetails.get().await()
        val q = query.trim().lowercase()
        return snapshot.documents.mapNotNull { it.toDrink() }.filter { drink ->
            drink.name.values.any { it.lowercase().contains(q) } ||
                drink.brand.lowercase().contains(q)
        }
    }

    override fun getReviewsForDrink(drinkId: String): Flow<List<Review>> = callbackFlow {
        val listener = reviews
            .whereEqualTo("drinkId", drinkId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(Review::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun submitReview(review: Review) {
        reviews.add(review).await()
        val drinkRef = drinkDetails.document(review.drinkId)
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
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(LogEntry::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addToLog(userId: String, drinkId: String) {
        val log = hashMapOf(
            "userId" to userId,
            "drinkId" to drinkId,
            "timestamp" to System.currentTimeMillis()
        )
        logs.add(log).await()
        drinkDetails.document(drinkId)
            .update("views", com.google.firebase.firestore.FieldValue.increment(1)).await()
    }

    override suspend fun removeFromLog(logEntryId: String) {
        logs.document(logEntryId).delete().await()
    }

    override suspend fun saveUser(userId: String, displayName: String, email: String) {
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
