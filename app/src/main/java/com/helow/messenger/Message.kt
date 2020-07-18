package com.helow.messenger

import androidx.annotation.Keep
import com.google.firebase.database.ServerValue

@Keep
data class Message(
    val from: String = "",
    val to: String = "",
    val text: String = "",
    val timestamp: Map<String, String> = ServerValue.TIMESTAMP
)

@Keep
data class MessageRec(
    val from: String = "",
    val to: String = "",
    val text: String = "",
    val timestamp: Long = 0
)