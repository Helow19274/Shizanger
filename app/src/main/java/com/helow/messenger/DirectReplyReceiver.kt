package com.helow.messenger

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class DirectReplyReceiver : BroadcastReceiver() {
    private val auth = Firebase.auth
    private val db = Firebase.database.getReference("messages")

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onReceive(context: Context, intent: Intent) {
        val data = RemoteInput.getResultsFromIntent(intent)
        if (data != null) {
            val result = goAsync()
            val toUid = intent.getStringExtra("uid")!!
            val chatId = if (auth.uid!! < toUid) "${auth.uid}-${toUid}" else "${toUid}-${auth.uid}"
            db.child(chatId).push().setValue(Message(auth.uid!!, toUid, data.getCharSequence("key_text_reply").toString())).addOnSuccessListener {
                addReply(context, data.getCharSequence("key_text_reply").toString(), intent.getIntExtra("notificationId", 0), mePerson, intent.extras!!)
                result.finish()
            }
        }
    }
}