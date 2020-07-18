package com.helow.messenger

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DirectReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val data = RemoteInput.getResultsFromIntent(intent)
        if (data != null) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val auth = Firebase.auth
            val toUid = intent.getStringExtra("uid")!!
            val chatId = if (auth.uid!! < toUid) "${auth.uid}-${toUid}" else "${toUid}-${auth.uid}"
            Firebase.database.getReference("messages/$chatId").push().setValue(Message(auth.uid!!, toUid, data.getCharSequence("key_text_reply").toString())).addOnSuccessListener {
                notificationManager.cancel("message", intent.getIntExtra("notificationId", 0))
            }
        }
    }
}