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
import com.whatdrink.app.data.model.Comment
import com.whatdrink.app.data.model.Review
import com.whatdrink.app.data.model.User
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
        val doc = drinkDetails.document(id).get().await()
        if (!doc.exists()) return null
        return doc.toDrink()
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

    override suspend fun dReview(review: Review) {
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
        }.await
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

    override suspend fun getUser(userId: String): User? {
        val doc = firestore.collection("users").document(userId).get().await()
        if (!doc.exists()) return null
        return User(
            id = doc.getString("id") ?: userId,
            username = doc.getString("username") ?: "",
            email = doc.getString("email") ?: "",
            memberSince = doc.getTimestamp("memberSince"),
            reviewsCount = doc.getLong("reviewsCount")?.toInt() ?: 0,
            ratingCount = doc.getLong("ratingCount")?.toInt() ?: 0,
            profileImage = doc.getString("profileImage") ?: ""
        )
    }

    override suspend fun getProfileImages(): List<String> {
        val bucket = storage.app.options.storageBucket ?: return emptyList()
        val result = storage.reference.child("profile").listAll().await()
        return result.items.map { item -> "gs://$bucket${item.path}" }
    }

    override suspend fun updateProfileImage(userId: String, gsUrl: String) {
        firestore.collection("users").document(userId)
            .update("profileImage", gsUrl)
            .await()
    }

    override fun getComments(drinkId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection("drinkDetails").document(drinkId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val comments = snapshot?.documents?.mapNotNull { doc ->
                    Comment(
                        commentId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        username = doc.getString("username") ?: "",
                        context = doc.getString("context") ?: "",
                        createdAt = doc.getTimestamp("createdAt")
                    )
                } ?: emptyList()
                trySend(comments)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addComment(drinkId: String, userId: String, username: String, context: String) {
        val timestamp = com.google.firebase.Timestamp.now()
        val commentId = firestore.collection("allComments").document().id

        val drinkComment = mapOf(
            "commentId" to commentId,
            "userId" to userId,
            "username" to username,
            "context" to context,
            "createdAt" to timestamp
        )
        val globalComment = mapOf(
            "commentId" to commentId,
            "userId" to userId,
            "username" to username,
            "context" to context,
            "createdAt" to timestamp,
            "drinkId" to drinkId
        )

        firestore.collection("drinkDetails").document(drinkId)
            .collection("comments").document(commentId)
            .set(drinkComment).await()

        firestore.collection("allComments").document(commentId)
            .set(globalComment).await()
    }

    override suspend fun updateUsername(userId: String, newUsername: String) {
        firestore.collection("users").document(userId)
            .update("username", newUsername)
            .await()
        Log.d("DrinkRepo", "updateUsername: users doc updated")

        val userComments = firestore.collection("allComments")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        Log.d("DrinkRepo", "updateUsername: found ${userComments.size()} comments to update")
        if (userComments.isEmpty) return

        val usernameMap = mapOf("username" to newUsername)
        val mergeOptions = com.google.firebase.firestore.SetOptions.merge()

        val batch = firestore.batch()
        userComments.documents.forEach { doc ->
            val commentId = doc.getString("commentId") ?: doc.id
            val drinkId   = doc.getString("drinkId")   ?: return@forEach

            Log.d("DrinkRepo", "updateUsername: batching commentId=$commentId drinkId=$drinkId")
            batch.set(
                firestore.collection("allComments").document(commentId),
                usernameMap,
                mergeOptions
            )
            batch.set(
                firestore.collection("drinkDetails").document(drinkId)
                    .collection("comments").document(commentId),
                usernameMap,
                mergeOptions
            )
        }
        try {
            batch.commit().await()
            Log.d("DrinkRepo", "updateUsername: batch committed")
        } catch (e: Exception) {
            Log.e("DrinkRepo", "updateUsername: batch FAILED: ${e.message}")
            throw e
        }
    }

    override suspend fun saveUser(userId: String, username: String, email: String) {
        val user = hashMapOf(
            "id" to userId,
            "username" to username,
            "email" to email,
            "memberSince" to com.google.firebase.Timestamp.now(),
            "reviewsCount" to 0,
            "ratingCount" to 0,
            "profileImage" to "gs://whatdrinkdb.firebasestorage.app/profile/monster1.jpeg"
        )
        firestore.collection("users").document(userId).set(user).await()
    }
    override suspend fun getDrinkDetail(id: String): DrinkDetail? {
    val doc = firestore.collection("drinkDetails").document(id).get().await()
    if (!doc.exists()) return null
    return DrinkDetail(
        id = doc.id,
        name = doc.get("name") as? Map<String, String> ?: emptyMap(),
        nutrition = doc.get("nutrition") as? Map<String, Any> ?: emptyMap()
    )
}

override suspend fun getDrinkStats(id: String): DrinkStats? {
    val doc = firestore.collection("drinkStats").document(id).get().await()
    if (!doc.exists()) return null
    return DrinkStats(
        id = doc.id,
        averageRating = (doc.get("averageRating") as? Number)?.toDouble() ?: 0.0,
        views = doc.getLong("views")?.toInt() ?: 0,
        ranking = doc.getLong("ranking")?.toInt() ?: 0,
        commentCount = doc.getLong("commentCount")?.toInt() ?: 0
    )
}

override suspend fun incrementViews(id: String) {
    firestore.collection("drinkStats").document(id)
        .update("views", com.google.firebase.firestore.FieldValue.increment(1)).await()
}

override suspend fun getImageUrl(barcodeOrGsUrl: String): String? {
    return try {
        val ref = storage.getReferenceFromUrl(barcodeOrGsUrl)
        ref.downloadUrl.await().toString()
    } catch (e: Exception) {
        null
    }
}

override suspend fun getUser(userId: String): User? {
    val doc = firestore.collection("users").document(userId).get().await()
    if (!doc.exists()) return null
    return User(
        id = doc.id,
        username = doc.getString("username") ?: "",
        email = doc.getString("email") ?: "",
        memberSince = doc.getTimestamp("memberSince"),
        reviewsCount = doc.getLong("reviewsCount")?.toInt() ?: 0,
        ratingCount = doc.getLong("ratingCount")?.toInt() ?: 0,
        profileImage = doc.getString("profileImage") ?: ""
    )
}

override suspend fun updateUsername(userId: String, newUsername: String) {
    firestore.collection("users").document(userId)
        .update("username", newUsername).await()
}

override fun getComments(drinkId: String): Flow<List<Comment>> = callbackFlow {
    val listener = firestore.collection("comments")
        .whereEqualTo("drinkId", drinkId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, _ ->
            val comments = snapshot?.documents?.mapNotNull { doc ->
                Comment(
                    commentId = doc.id,
                    userId = doc.getString("userId") ?: "",
                    username = doc.getString("username") ?: "",
                    context = doc.getString("context") ?: "",
                    createdAt = doc.getTimestamp("createdAt")
                )
            } ?: emptyList()
            trySend(comments)
        }
    awaitClose { listener.remove() }
}

override suspend fun addComment(drinkId: String, userId: String, username: String, context: String) {
    val comment = hashMapOf(
        "drinkId" to drinkId,
        "userId" to userId,
        "username" to username,
        "context" to context,
        "createdAt" to com.google.firebase.Timestamp.now()
    )
    firestore.collection("comments").add(comment).await()
    firestore.collection("drinkStats").document(drinkId)
        .update("commentCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
}

override suspend fun getProfileImages(): List<String> {
    return try {
        val listResult = storage.reference.child("profile_images").listAll().await()
        listResult.items.map { it.downloadUrl.await().toString() }
    } catch (e: Exception) {
        emptyList()
    }
}

override suspend fun updateProfileImage(userId: String, gsUrl: String) {
    firestore.collection("users").document(userId)
        .update("profileImage", gsUrl).await()
}
    suspend fun getReviewsByUser(userId: String): List<Review> {
    return reviews
        .whereEqualTo("userId", userId)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .get().await()
        .toObjects(Review::class.java)
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
