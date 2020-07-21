package com.helow.messenger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.edit
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val preferences = getSharedPreferences("notifications", Context.MODE_PRIVATE)

        val notificationManager = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationManager.createNotificationChannel(NotificationChannel("messages", "Messages", NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
            })

        val notificationId = preferences.getInt("notificationId", 1)
        val args = Bundle()
        val sender = Person.Builder().setName("Me").build()
        val receiver = Person.Builder().setName(message.data["title"]).build()
        args.putString("uid", message.data["sender"])
        args.putInt("notificationId", notificationId)

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val group = NotificationCompat.Builder(this, "messages").apply {
                setSmallIcon(R.drawable.baseline_message_24)
                color = Color.GREEN
                setContentInfo(message.data["title"])
                setGroup(message.data["sender"])
                setGroupSummary(true)
            }
            notificationManager.notify(message.data["title"].hashCode(), group.build())
            notification.setGroup(message.data["sender"])

            val remoteInput = RemoteInput.Builder("key_text_reply")
                .setLabel("Type here...")
                .build()

            val intent = Intent(this, DirectReplyReceiver::class.java).apply {
                putExtras(args)
            }

            val replyPendingIntent = PendingIntent.getBroadcast(this, notificationId, intent, 0)

            val action = NotificationCompat.Action.Builder(R.drawable.baseline_forward_24, "Reply", replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build()

            val style = NotificationCompat.MessagingStyle(sender)
                .addMessage(message.data["content"], System.currentTimeMillis(), receiver)

            notification.addAction(action)
            notification.setStyle(style)
        }

        notificationManager.notify(notificationId, notification.build())
        preferences.edit(commit = true) {
            putInt("notificationId", notificationId + 1)
        }
    }

    override fun onNewToken(token: String) {
        Firebase.database.getReference("users/${Firebase.auth.uid}/token").setValue(token)
    }
}