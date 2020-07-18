package com.helow.messenger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.edit
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {
    private val ref = Firebase.database.getReference("users/${Firebase.auth.uid}/token")
    private lateinit var preferences: SharedPreferences

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        preferences = getSharedPreferences("notifications", Context.MODE_PRIVATE)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel("messages", "Messages", NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(mChannel)
        }

        val args = Bundle()
        args.putString("uid", message.data["sender"])

        val pendingIntent = NavDeepLinkBuilder(applicationContext)
            .setGraph(R.navigation.my_nav)
            .setDestination(R.id.chatFragment)
            .setArguments(args)
            .createPendingIntent()

        val notification = NotificationCompat.Builder(this@MessagingService, "messages").apply {
            setSmallIcon(R.drawable.baseline_message_24)
            color = Color.GREEN
            setContentTitle(message.data["title"])
            setContentText(message.data["content"])
            setContentIntent(pendingIntent)
            setAutoCancel(true)
        }

        val notificationId = preferences.getInt("notificationId", 1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val remoteInput = RemoteInput.Builder("key_text_reply")
                .setLabel("Type here...")
                .build()
            args.putInt("notificationId", notificationId)

            val intent = Intent(this@MessagingService, DirectReplyReceiver::class.java)
            intent.putExtras(args)

            val replyPendingIntent = PendingIntent.getBroadcast(this@MessagingService, 0, intent, PendingIntent.FLAG_ONE_SHOT)

            val action = NotificationCompat.Action.Builder(R.drawable.baseline_forward_24, "Reply", replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build()

            notification.addAction(action)
        }

        notificationManager.notify("message", notificationId, notification.build())
        preferences.edit(commit = true) {
            putInt("notificationId", notificationId + 1)
        }
    }

    override fun onNewToken(token: String) {
        ref.setValue(token)
    }
}