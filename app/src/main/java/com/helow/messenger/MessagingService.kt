package com.helow.messenger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.edit
import androidx.core.graphics.drawable.IconCompat
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.File
import java.util.*
import kotlin.concurrent.schedule

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

        var text = if (message.data["content"] != null)
            message.data["content"]!!
        else
            wrapContextWithLocale(this).getString(R.string.picture)

        if (message.data["imageUrl"] != null)
            text = "\uD83D\uDDBC$text"

        val imageUrl = if (message.data["content"] == null && message.data["imageUrl"] != null)
            message.data["imageUrl"]
        else
            null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sender = Person.Builder()
                .setName(message.data["title"])

            if (message.data["avatarUrl"] != null) {
                val icon = GlideApp
                    .with(applicationContext)
                    .asBitmap()
                    .load(message.data["avatarUrl"])
                    .submit()
                    .get()
                sender.setIcon(IconCompat.createWithBitmap(icon))
            }
            if (imageUrl != null) {
                val fileName = UUID.randomUUID().toString()
                val image = GlideApp
                    .with(applicationContext)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get()
                applicationContext.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                    image.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                args.putString("imageUrl", applicationContext.filesDir.listFiles { _, name -> name == fileName }!![0].absolutePath)
            }
            addReply(this, text, message.data["sender"].hashCode(), sender.build(), args)

            if (args["imageUrl"] != null)
                Timer().schedule(500) {
                    File(args.getString("imageUrl")!!).delete()
                }
        }

        else {
            val preferences = getSharedPreferences("notifications", Context.MODE_PRIVATE)
            val notificationId = preferences.getInt("notificationId", 1)
            val pendingIntent = NavDeepLinkBuilder(this).run {
                setGraph(R.navigation.my_nav)
                setDestination(R.id.chatFragment)
                setArguments(args)
                createPendingIntent()
            }

            val notification = NotificationCompat.Builder(this, "messages").run {
                setSmallIcon(R.drawable.message)
                color = Color.GREEN
                setContentTitle(message.data["title"])
                setContentText(text)
                setContentIntent(pendingIntent)
                setAutoCancel(true)
                build()
            }
            notificationManager.notify(notificationId, notification)
            preferences.edit(commit = true) {
                putInt("notificationId", notificationId + 1)
            }
        }
    }

    override fun onNewToken(token: String) {
        Firebase.database.getReference("users/${Firebase.auth.uid}/token").setValue(token)
    }
}