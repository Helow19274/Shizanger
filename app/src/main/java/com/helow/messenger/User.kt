package com.helow.messenger

import androidx.annotation.Keep

@Keep
data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val token: String = "",
    val contacts: HashMap<String, String> = hashMapOf(),
    val online: Boolean = true,
    val lastSeen: String? = null,
    val inChatWith: String? = null
)