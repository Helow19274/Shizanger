package com.helow.messenger

import android.view.View
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import kotlinx.android.synthetic.main.user_item.view.*

open class ContactItem(val user: User, val contactKey: String?=null) : AbstractItem<ContactItem.ViewHolder>() {
    val ref = Firebase.database.getReference("users/${user.uid}")

    override val type: Int
        get() = R.id.recycler_view

    override val layoutRes: Int
        get() = R.layout.user_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ContactItem>(view) {
        private lateinit var listener: ValueEventListener
        private val username = view.username
        private val email = view.email
        private val card = view.card

        override fun bindView(item: ContactItem, payloads: List<Any>) {
            listener = object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) { }

                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue<User>()!!
                    card.isChecked = user.online
                    username.text = user.username
                }
            }
            username.text = item.user.username
            email.text = item.user.email
            item.ref.addValueEventListener(listener)
        }

        override fun unbindView(item: ContactItem) {
            username.text = null
            email.text = null
            item.ref.removeEventListener(listener)
        }
    }
}