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
import com.google.firebase.database.*
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

val availableLocales = sortedMapOf(
    "en" to "English",
    "ru" to "Русский",
    "tt" to "Русскій(дореволюціонный)"
)

@RequiresApi(Build.VERSION_CODES.N)
fun addReply(context: Context, message: CharSequence, notificationId: Int, sender: Person, args: Bundle) {
    val notificationManager = context.getSystemService(NotificationManager::class.java)
    val newStyle = NotificationCompat.MessagingStyle(sender)

    val activeNotification = notificationManager.activeNotifications.find { it.id == notificationId }?.notification
    if (activeNotification != null) {
        val activeStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(activeNotification)!!
        activeStyle.messages.forEach {
            newStyle.addMessage(NotificationCompat.MessagingStyle.Message(it.text, it.timestamp, it.person))
        }
    }

    newStyle.addMessage(message, System.currentTimeMillis(), sender)

    val pendingIntent = NavDeepLinkBuilder(context).run {
        setGraph(R.navigation.my_nav)
        setDestination(R.id.chatFragment)
        setArguments(args)
        createPendingIntent()
    }

    val remoteInput = RemoteInput.Builder("key_text_reply").run {
        setLabel(context.getString(R.string.type_here))
        build()
    }

    val intent = Intent(context, DirectReplyReceiver::class.java)
    intent.putExtras(args)

    val replyPendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, 0)

    val action = NotificationCompat.Action.Builder(R.drawable.forward, context.getString(R.string.reply), replyPendingIntent).run {
        addRemoteInput(remoteInput)
        build()
    }

    val notification = NotificationCompat.Builder(context, "messages").run {
        setSmallIcon(R.drawable.message)
        color = Color.GREEN
        setContentIntent(pendingIntent)
        setAutoCancel(true)
        addAction(action)
        setStyle(newStyle)
        build()
    }
    NotificationManagerCompat.from(context).notify(notificationId, notification)
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
    if (context != context.applicationContext)
        context.applicationContext.resources.run { updateConfiguration(config, displayMetrics) }
}

interface MyChildEventListener : ChildEventListener {
    override fun onCancelled(error: DatabaseError) { }
    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }
    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) { }
    override fun onChildRemoved(snapshot: DataSnapshot) { }
    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) { }
}

interface MyValueEventListener : ValueEventListener {
    override fun onCancelled(error: DatabaseError) { }
}