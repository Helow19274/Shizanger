package com.helow.messenger

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class DirectReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val data = RemoteInput.getResultsFromIntent(intent)
        if (data != null) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            Firebase.firestore.collection("messages").add(Message(Firebase.auth.currentUser!!.uid, intent.getStringExtra("uid")!!, data.getCharSequence("key_text_reply").toString())).addOnSuccessListener {
                notificationManager.cancel("message", intent.getIntExtra("notificationId", 0))
            }
        }
    }
}