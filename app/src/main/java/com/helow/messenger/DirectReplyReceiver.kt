package com.helow.messenger

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.helow.messenger.model.Message
import kotlin.concurrent.thread

class DirectReplyReceiver : BroadcastReceiver() {
    private val auth = Firebase.auth
    private val db = Firebase.database.getReference("messages")

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onReceive(context: Context, intent: Intent) {
        val data = RemoteInput.getResultsFromIntent(intent)
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (data != null) {
            val person = Person.Builder()
                .setName(context.getString(R.string.me))
            val result = goAsync()
            thread {
                val imageUrl = preferences.getString("imageUrl", null)
                if (imageUrl != null) {
                    val icon = GlideApp
                        .with(context)
                        .asBitmap()
                        .load(imageUrl)
                        .submit()
                        .get()
                    person.setIcon(IconCompat.createWithBitmap(icon))
                }
                val toUid = intent.getStringExtra("uid")!!
                val chatId = if (auth.uid!! < toUid) "${auth.uid}-${toUid}" else "${toUid}-${auth.uid}"
                db.child(chatId).push().setValue(Message(auth.uid!!, toUid, data.getCharSequence("key_text_reply").toString())).addOnSuccessListener {
                    addReply(context, data.getCharSequence("key_text_reply").toString(), toUid.hashCode(), person.build(), intent.extras!!)
                    result.finish()
                }
            }
        }
    }
}