package com.whatdrink.app.data.model

import com.google.firebase.Timestamp

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val username: String = "",
    val context: String = "",
    val createdAt: Timestamp? = null
)
