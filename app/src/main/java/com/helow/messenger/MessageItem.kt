package com.helow.messenger

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.google.firebase.database.DatabaseReference
import com.helow.messenger.model.MessageRec
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.message_sent_item.view.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MessageItem(val message: MessageRec, val sent: Boolean, val messageId: String, private val charRef: DatabaseReference) : AbstractItem<MessageItem.ViewHolder>() {
    override val type: Int
        get() = if (sent) 1 else 2

    override val layoutRes: Int
        get() = if (sent) R.layout.message_sent_item else R.layout.message_received_item

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(view: View) : FastAdapter.ViewHolder<MessageItem>(view) {
        private val messageText = view.findViewById<TextView>(R.id.message_text)
        private val timestampText = view.findViewById<TextView>(R.id.timestamp)
        private val image = view.findViewById<ImageView>(R.id.image)
        private val seen = view.seen_status

        override fun bindView(item: MessageItem, payloads: List<Any>) {
            if (item.message.text != null)
                messageText.text = item.message.text
            else
                messageText.visibility = View.GONE

            if (item.sent) {
                if (item.message.seen)
                    seen.setImageResource(R.drawable.seen)
                else
                    seen.setImageResource(R.drawable.sent)
            }
            timestampText.text = DateTimeFormatter
                .ofPattern("d.MM HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(item.message.timestamp))
            if (item.message.imageUrl != null) {
                GlideApp.with(image)
                    .load(item.message.imageUrl)
                    .placeholder(R.drawable.image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .into(image)

                val viewer = StfalconImageViewer.Builder(image.context, listOf(1)) { image2, _ ->
                    image2.setImageDrawable(image.drawable)
                }.withTransitionFrom(image)
                image.setOnClickListener {
                    viewer.show()
                }
            }
            else
                image.setImageDrawable(null)

            if (!item.sent)
                item.charRef.child("${item.messageId}/seen").setValue(true)
        }

        override fun unbindView(item: MessageItem) {
            messageText.text = null
            messageText.visibility = View.VISIBLE
            timestampText.text = null
            if (item.sent)
                seen.setImageDrawable(null)
            image.setImageDrawable(null)
        }
    }
}