package com.helow.messenger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(wrapContextWithLocale(newBase))
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notificationManager = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationManager.createNotificationChannel(NotificationChannel("messages", "Messages", NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
            })

        val args = bundleOf("uid" to message.data["sender"])
        val sender = Person.Builder()
            .setName(message.data["title"])
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            addReply(this, message.data["content"]!!, message.data["sender"].hashCode(), sender, args)
        else {
            val preferences = getSharedPreferences("notifications", Context.MODE_PRIVATE)
            val notificationId = preferences.getInt("notificationId", 1)
            val pendingIntent = NavDeepLinkBuilder(applicationContext)
                .setGraph(R.navigation.my_nav)
                .setDestination(R.id.chatFragment)
                .setArguments(args)
                .createPendingIntent()

            val notification = NotificationCompat.Builder(this, "messages").apply {
                setSmallIcon(R.drawable.baseline_message_24)
                color = Color.GREEN
                setContentIntent(pendingIntent)
                setAutoCancel(true)
            }
            notificationManager.notify(notificationId, notification.build())
            preferences.edit(commit = true) {
                putInt("notificationId", notificationId + 1)
            }
        }
    }

    override fun onNewToken(token: String) {
        Firebase.database.getReference("users/${Firebase.auth.uid}/token").setValue(token)
    }
}