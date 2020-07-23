package com.helow.messenger

import androidx.annotation.Keep
import com.google.firebase.database.ServerValue

@Keep
data class Message(
    val from: String = "",
    val to: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val timestamp: Map<String, String> = ServerValue.TIMESTAMP,
    val seen: Boolean = false
)

@Keep
data class MessageRec(
    val from: String = "",
    val to: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val timestamp: Long = 0,
    val seen: Boolean = false
)