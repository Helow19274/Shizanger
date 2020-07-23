package com.helow.messenger

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MessageItem(val message: MessageRec, val sent: Boolean, val messageId: String) : AbstractItem<MessageItem.ViewHolder>() {
    override val type: Int
        get() = if (sent) 1 else 2

    override val layoutRes: Int
        get() = if (sent) R.layout.message_sent_item else R.layout.message_received_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<MessageItem>(view) {
        private val messageText = view.findViewById<TextView>(R.id.message_text)
        private val timestampText = view.findViewById<TextView>(R.id.timestamp)
        private val image = view.findViewById<ImageView>(R.id.image)

        override fun bindView(item: MessageItem, payloads: List<Any>) {
            messageText.text = item.message.text
            timestampText.text = DateTimeFormatter
                .ofPattern("d.MM HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(item.message.timestamp))
            if (item.message.imageUrl.isNotEmpty())
                Glide.with(image)
                    .load(item.message.imageUrl)
                    .into(image)
            else
                image.setImageDrawable(null)
        }

        override fun unbindView(item: MessageItem) {
            messageText.text = null
            timestampText.text = null
            image.setImageDrawable(null)
        }
    }
}