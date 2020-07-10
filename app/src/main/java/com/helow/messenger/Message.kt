package com.helow.messenger

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

@Keep
data class Message(
    val from: String = "",
    val to: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null
)