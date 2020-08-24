package com.helow.messenger

import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.helow.messenger.model.UserRec
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import kotlinx.android.synthetic.main.user_item.view.*

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
        private val online = view.online
        private val profileImage = view.profile_image
        private val listener = object : MyValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue<UserRec>()!!
                online.isVisible = user.online
                username.text = user.username
                if (user.imageUrl != null)
                    GlideApp
                        .with(view)
                        .load(user.imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(profileImage)
                else
                    profileImage.setImageResource(R.drawable.default_profile)
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
            online.visibility = View.GONE
            profileImage.setImageDrawable(null)
            item.ref.removeEventListener(listener)
        }
    }
}