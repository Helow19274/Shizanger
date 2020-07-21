package com.helow.messenger

import androidx.annotation.Keep

@Keep
data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val token: String? = null,
    val contacts: HashMap<String, String> = hashMapOf(),
    val online: Boolean = true,
    val lastSeen: Map<String, String>? = null,
    val inChatWith: String? = null
)

@Keep
data class UserRec(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val token: String? = null,
    val contacts: HashMap<String, String> = hashMapOf(),
    val online: Boolean = true,
    val lastSeen: Long = 0,
    val inChatWith: String? = null
)