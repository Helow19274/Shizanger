package com.helow.messenger

import android.view.View
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import kotlinx.android.synthetic.main.user_item.view.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

open class UserItem(val user: UserRec, val contactKey: String?=null) : AbstractItem<UserItem.ViewHolder>() {
    val ref = Firebase.database.getReference("users/${user.uid}")

    override val type: Int
        get() = R.id.recycler_view

    override val layoutRes: Int
        get() = R.layout.user_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<UserItem>(view) {
        private val username = view.username
        private val email = view.email
        private val lastSeen = view.last_seen
        private val listener = object : MyValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue<UserRec>()!!
                lastSeen.text = if (user.online)
                    username.context.getString(R.string.online)
                else
                    username.context.getString(R.string.last_seen, DateTimeFormatter
                        .ofPattern(username.context.getString(R.string.last_seen_pattern))
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(user.lastSeen)))
                username.text = user.username
            }
        }

        override fun bindView(item: UserItem, payloads: List<Any>) {
            username.text = item.user.username
            email.text = item.user.email
            item.ref.addValueEventListener(listener)
        }

        override fun unbindView(item: UserItem) {
            username.text = null
            email.text = null
            lastSeen.text = null
            item.ref.removeEventListener(listener)
        }
    }
}