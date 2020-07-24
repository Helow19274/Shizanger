package com.helow.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.getValue
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
import kotlinx.android.synthetic.main.fragment_chat.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.schedule

class ChatFragment : Fragment() {
    private val model: MainActivityViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()
    private val adapter = ItemAdapter<MessageItem>()
    private val fastAdapter = FastAdapter.with(adapter)
    private val messages = mutableMapOf<String, MessageItem>()
    private lateinit var chatRef: DatabaseReference
    private var editId: String? = null
    private var beforeEditText: String? = null
    private var beforeEditImageUri: Uri? = null
    private var lastKey: String? = null
    private var loaded = 0
    private var imageUri: Uri? = null
    private var typing = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = (requireActivity() as MainActivity)

        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        chatRef = model.db.getReference("messages/${if (model.auth.uid!! < args.uid) "${model.auth.uid}-${args.uid}" else "${args.uid}-${model.auth.uid}"}")

        recycler_view.adapter = fastAdapter

        fastAdapter.onClickListener = { _, _, item, _ ->
            val dialog = MaterialAlertDialogBuilder(context).apply {
                setTitle(R.string.choose_action)
                setNegativeButton(R.string.cancel) { _, _ -> }
            }
            if (item.sent)
                dialog.setItems(arrayOf(getString(R.string.edit), getString(R.string.remove))) {_, which ->
                    when (which) {
                        0 -> {
                            editId = item.messageId
                            beforeEditText = message.text.toString()
                            beforeEditImageUri = imageUri
                            imageUri = null
                            if (item.message.imageUrl != null)
                                imageUri = item.message.imageUrl.toUri()
                            if (imageUri != null)
                                attach_button.setIconResource(R.drawable.file_attached)
                            message.apply {
                                text?.clear()
                                if (item.message.text != null)
                                    append(item.message.text)
                                requestFocus()
                                postDelayed({
                                    imm.showSoftInput(message, InputMethodManager.SHOW_IMPLICIT)
                                }, 100)
                            }
                            cancel_edit_button.visibility = View.VISIBLE
                        }
                        1 -> chatRef.child(item.messageId).removeValue()
                    }
                }
            else
                dialog.setItems(arrayOf(getString(R.string.remove))) {_, _ ->
                    chatRef.child(item.messageId).removeValue()
                }
            dialog.show()
            false
        }

        recycler_view.addOnScrollListener(object : EndlessRecyclerOnScrollListener(recycler_view.layoutManager!!) {
            override fun onLoadMore(currentPage: Int) {
                if (loaded < 50 && currentPage >= 2)
                    return
                loaded = 0
                loadMoreMessages()
            }
        })

        model.db.getReference("users/${args.uid}").addValueEventListener(viewLifecycleOwner, object : MyValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue<UserRec>()!!
                activity.setActionBarTitle(user.username)
                activity.setActionBarSubTitle(
                    when {
                        user.typing == model.auth.uid -> getString(R.string.typing)
                        user.online -> getString(R.string.online)
                        else -> getString(R.string.last_seen, DateTimeFormatter
                            .ofPattern(getString(R.string.last_seen_pattern))
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(user.lastSeen)))
                    }
                )
            }
        })

        send_button.setOnClickListener {
            val chunks = message.text.toString().chunked(2048)
            lifecycleScope.launch {
                if (chunks.isEmpty())
                    sendMessage(args.uid, null)
                else
                    for (chunk in chunks)
                        sendMessage(args.uid, chunk)
                message.text?.clear()
                if (beforeEditText != null)
                    message.append(beforeEditText)
                editId = null
                if (beforeEditImageUri != null)
                    attach_button.setIconResource(R.drawable.file_attached)
                else
                    attach_button.setIconResource(R.drawable.attach_file)
                imageUri = beforeEditImageUri
                beforeEditImageUri = null
                beforeEditText = null
            }
        }

        message.addTextChangedListener {
            send_button.isEnabled = !it.isNullOrBlank() || imageUri != null
            if (!typing) {
                typing = true
                model.db.getReference("users/${model.auth.uid}/typing").setValue(args.uid)
                Timer().schedule(2000) {
                    typing = false
                    model.db.getReference("users/${model.auth.uid}/typing").setValue(null)
                }
            }
        }

        cancel_edit_button.setOnClickListener {
            message.text?.clear()
            editId = null
            cancel_edit_button.visibility = View.GONE
            message.text?.clear()
            if (beforeEditText != null)
                message.append(beforeEditText)
            else
                message.isEnabled = false
            beforeEditText = null
            if (beforeEditImageUri != null)
                attach_button.setIconResource(R.drawable.file_attached)
            else {
                attach_button.setIconResource(R.drawable.attach_file)
                if (message.text.isNullOrBlank())
                    send_button.isEnabled = false
            }
            imageUri = beforeEditImageUri
            beforeEditImageUri = null
        }

        attach_button.setOnClickListener {
            if (imageUri == null) {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), 1)
            }
            else {
                imageUri = null
                attach_button.setIconResource(R.drawable.attach_file)
                if (message.text.isNullOrEmpty())
                    send_button.isEnabled = false
            }
        }

        chatRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : MyValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lastKey = snapshot.children.firstOrNull()?.key
                addNewMessagesListener()
            }
        })

        if (args.messageFromShare != null) {
            message.append(args.messageFromShare)
            activity.intent.removeExtra(Intent.EXTRA_TEXT)
        }
    }

    override fun onStart() {
        super.onStart()
        model.db.getReference("users/${model.auth.uid}/inChatWith").setValue(args.uid)
        NotificationManagerCompat.from(requireContext()).cancel(args.uid.hashCode())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        model.db.getReference("users/${model.auth.uid}/inChatWith").setValue(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && requestCode == 1 && resultCode == Activity.RESULT_OK) {
            imageUri = data.data!!
            attach_button.setIconResource(R.drawable.file_attached)
            send_button.isEnabled = true
        }
    }

    private suspend fun sendMessage(to: String, text: String?) {
        send_button.isEnabled = false
        var url: String? = null
        if (imageUri != null) {
            url = if (imageUri!!.host == "firebasestorage.googleapis.com")
                imageUri.toString()
            else {
                val stream = requireActivity().contentResolver.openInputStream(imageUri!!)!!
                val ref = model.storage.child(UUID.randomUUID().toString())
                ref.putStream(stream).await()
                ref.downloadUrl.await().toString()
            }
        }
        val messageText = text?.trim()
        if (editId == null) {
            chatRef.push().setValue(Message(model.auth.uid!!, to, messageText, url)).await()
            imageUri = null
            attach_button.setIconResource(R.drawable.attach_file)
        }
        else {
            chatRef.child(editId!!).setValue(MessageRec(model.auth.uid!!, to, messageText, url, messages[editId!!]!!.message.timestamp)).await()
            cancel_edit_button.visibility = View.GONE
            imageUri = null
            attach_button.setIconResource(R.drawable.attach_file)
        }
    }

    private fun addNewMessagesListener() {
        var keySet = false
        chatRef.orderByKey().startAt(lastKey ?: "").addChildEventListener(viewLifecycleOwner, object : MyChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!keySet) {
                    lastKey = snapshot.key
                    keySet = true
                }
                val value = snapshot.getValue<MessageRec>()!!
                messages[snapshot.key!!] = MessageItem(value, value.from == model.auth.uid, snapshot.key!!, chatRef)
                adapter.add(0, messages[snapshot.key!!]!!)
                recycler_view.scrollToPosition(0)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                messages.remove(snapshot.key!!)
                adapter.set(messages.toSortedMap().values.reversed())
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue<MessageRec>()!!
                val message = messages[snapshot.key!!]!!
                if ((message.sent && value.text == message.message.text && value.seen == message.message.seen && value.imageUrl == message.message.imageUrl) || (!message.sent && value.text == message.message.text && value.imageUrl == message.message.imageUrl))
                    return
                messages[snapshot.key!!] = MessageItem(value, value.from == model.auth.uid, snapshot.key!!, chatRef)
                adapter.set(messages.toSortedMap().values.reversed())
            }
        })
    }

    private fun loadMoreMessages() {
        val position = adapter.adapterItemCount
        if (lastKey == null)
            return
        var keySet = false
        chatRef.orderByKey().endAt(lastKey).limitToLast(51).addChildEventListener(viewLifecycleOwner, object : MyChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!keySet) {
                    if (lastKey == snapshot.key)
                        return
                    lastKey = snapshot.key
                    keySet = true
                }
                if (snapshot.key in messages)
                    return

                loaded += 1

                val value = snapshot.getValue<MessageRec>()!!
                messages[snapshot.key!!] = MessageItem(value, value.from == model.auth.uid, snapshot.key!!, chatRef)
                adapter.add(position, messages[snapshot.key!!]!!)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                messages.remove(snapshot.key!!)
                adapter.set(messages.toSortedMap().values.reversed())
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue<MessageRec>()!!
                val message = messages[snapshot.key!!]!!
                if ((message.sent && value.text == message.message.text && value.seen == message.message.seen && value.imageUrl == message.message.imageUrl) || (!message.sent && value.text == message.message.text && value.imageUrl == message.message.imageUrl))
                    return
                messages[snapshot.key!!] = MessageItem(value, value.from == model.auth.uid, snapshot.key!!, chatRef)
                adapter.set(messages.toSortedMap().values.reversed())
            }
        })
    }
}