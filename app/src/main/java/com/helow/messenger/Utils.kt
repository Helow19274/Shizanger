package com.helow.messenger

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.lifecycle.*
import androidx.navigation.NavDeepLinkBuilder
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import java.util.*

operator fun <T> MutableLiveData<ArrayList<T>>.plusAssign(values: T) {
    val value = this.value ?: arrayListOf()
    value.add(values)
    this.value = value
}

operator fun <T> MutableLiveData<ArrayList<T>>.minusAssign(values: T) {
    val uid = (values as ContactItem).user.uid
    val value = this.value?.filterNot { (it as ContactItem).user.uid == uid } ?: listOf()
    this.value = ArrayList(value.toMutableList())
}

fun DatabaseReference.addValueEventListener(lifecycleOwner: LifecycleOwner, listener: ValueEventListener) {
    addValueEventListener(listener)
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            removeEventListener(listener)
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}

fun DatabaseReference.addChildEventListener(lifecycleOwner: LifecycleOwner, listener: ChildEventListener) {
    addChildEventListener(listener)
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            removeEventListener(listener)
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}

fun Query.addChildEventListener(lifecycleOwner: LifecycleOwner, listener: ChildEventListener) {
    addChildEventListener(listener)
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            removeEventListener(listener)
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}

val mePerson = Person.Builder()
    .setName("Me")
    .build()

val availableLocales = sortedMapOf(
    "en" to "English",
    "ru" to "Русский",
    "tt" to "Русскій(дореволюціонный)"
)

@RequiresApi(Build.VERSION_CODES.M)
fun findActiveNotification(context: Context, notificationId: Int) =
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .activeNotifications.find { it.id == notificationId }?.notification

@RequiresApi(Build.VERSION_CODES.N)
fun addReply(context: Context, message: CharSequence, notificationId: Int, sender: Person, args: Bundle) {
    val activeNotification = findActiveNotification(context, notificationId) ?: return
    val activeStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(activeNotification)!!
    val newStyle = NotificationCompat.MessagingStyle(mePerson)
    activeStyle.messages.forEach {
        newStyle.addMessage(NotificationCompat.MessagingStyle.Message(it.text, it.timestamp, it.person))
    }
    newStyle.addMessage(message, System.currentTimeMillis(), sender)

    val pendingIntent = NavDeepLinkBuilder(context)
        .setGraph(R.navigation.my_nav)
        .setDestination(R.id.chatFragment)
        .setArguments(args)
        .createPendingIntent()

    val remoteInput = RemoteInput.Builder("key_text_reply")
        .setLabel("Type here...")
        .build()

    val intent = Intent(context, DirectReplyReceiver::class.java)
    intent.putExtras(args)

    val replyPendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, 0)

    val action = NotificationCompat.Action.Builder(R.drawable.baseline_forward_24, "Reply", replyPendingIntent)
        .addRemoteInput(remoteInput)
        .build()

    val notification = NotificationCompat.Builder(context, "messages").apply {
        setSmallIcon(R.drawable.baseline_message_24)
        color = Color.GREEN
        setContentIntent(pendingIntent)
        setAutoCancel(true)
        addAction(action)
        setStyle(newStyle)
        setGroup(args.getString("uid"))
    }
    NotificationManagerCompat.from(context).notify(notificationId, notification.build())
}

fun wrapContextWithLocale(context: Context): Context {
    val locale = Locale(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("locale", "ru")!!)
    Locale.setDefault(locale)
    val config = Configuration()
    config.setLocale(locale)
    return context.createConfigurationContext(config)
}

@Suppress("DEPRECATION")
fun overrideLocale(context: Context) {
    val locale = Locale(context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("locale", "ru")!!)
    Locale.setDefault(locale)
    val config = Configuration()
    config.setLocale(locale)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
    if (context != context.applicationContext) {
        context.applicationContext.resources.run { updateConfiguration(config, displayMetrics) }
    }
}