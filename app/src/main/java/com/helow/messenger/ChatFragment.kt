package com.helow.messenger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.*
import com.google.firebase.database.ktx.getValue
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.android.synthetic.main.fragment_chat.view.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChatFragment : Fragment() {
    private val model: MainActivityViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()
    private val adapter = ItemAdapter<MessageItem>()
    private val fastAdapter = FastAdapter.with(adapter)
    private val messages = mutableMapOf<String, MessageItem>()
    private lateinit var chatRef: DatabaseReference
    private lateinit var cancelButton: Button
    private var editId = ""
    private var beforeEditText = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val activity = (requireActivity() as MainActivity)
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        cancelButton = view.cancel_edit_button
        val recyclerView = view.recycler_view
        val sendButton = view.send_button
        val message = view.message
        chatRef = model.db.getReference("messages/${if (model.auth.uid!! < args.uid) "${model.auth.uid}-${args.uid}" else "${args.uid}-${model.auth.uid}"}")

        recyclerView.adapter = fastAdapter

        fastAdapter.onClickListener = { _, _, item, _ ->
            val items = if (item.sent)
                arrayOf("Edit", "Remove")
            else
                arrayOf("Remove")
            MaterialAlertDialogBuilder(context)
                .setTitle("Choose action?")
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> {
                            if (items.size == 2) {
                                editId = item.messageId
                                beforeEditText = message.text.toString()
                                message.apply {
                                    text?.clear()
                                    append(item.message.text)
                                    requestFocus()
                                    postDelayed({
                                        imm.showSoftInput(message, InputMethodManager.SHOW_IMPLICIT)
                                    }, 100)
                                }
                                cancelButton.visibility = View.VISIBLE
                            } else
                                chatRef.child(item.messageId).removeValue()
                        }
                        1 -> chatRef.child(item.messageId).removeValue()
                    }
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
            false
        }

        model.db.getReference("users/${args.uid}").addValueEventListener(viewLifecycleOwner, object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue<UserRec>()!!
                activity.setActionBarTitle(user.username)
                activity.setActionBarSubTitle(
                    if (user.online) "Online" else "Last seen ${DateTimeFormatter
                        .ofPattern("d.MM 'at' HH:mm")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(user.lastSeen))}"
                )
            }
        })

        chatRef.orderByChild("timestamp").addChildEventListener(viewLifecycleOwner, object : ChildEventListener {
            override fun onCancelled(error: DatabaseError) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue<MessageRec>()!!
                messages[snapshot.key!!] = MessageItem(value, value.from == model.auth.uid, snapshot.key!!)
                adapter.set(messages.values.toList())
                recyclerView.scrollToPosition(adapter.adapterItemCount - 1)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                messages.remove(snapshot.key!!)
                adapter.set(messages.values.toList())
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue<MessageRec>()!!
                if (value.text == messages[snapshot.key!!]!!.message.text)
                    return
                val scroll = messages[snapshot.key!!]
                messages[snapshot.key!!] = MessageItem(value, value.from == model.auth.uid, snapshot.key!!)
                adapter.set(messages.values.toList())
                if (scroll == null)
                    recyclerView.scrollToPosition(adapter.adapterItemCount - 1)
            }
        })

        sendButton.setOnClickListener {
            val chunks = view.message.text!!.chunked(2048)
            lifecycleScope.launch {
                for (chunk in chunks)
                    sendMessage(args.uid, chunk)
                message.text?.clear()
                message.append(beforeEditText)
                editId = ""
                beforeEditText = ""
            }
        }

        message.addTextChangedListener {
            sendButton.isEnabled = !it.isNullOrBlank()
        }

        cancelButton.setOnClickListener {
            view.message.text?.clear()
            editId = ""
            cancelButton.visibility = View.GONE
            message.text?.clear()
            message.append(beforeEditText)
            beforeEditText = ""
        }

        if (args.messageFromShare != null) {
            message.append(args.messageFromShare)
            activity.intent.removeExtra(Intent.EXTRA_TEXT)
        }

        return view
    }

    private suspend fun sendMessage(to: String, text: String) {
        if (editId == "")
            chatRef.push().setValue(Message(model.auth.uid!!, to, text)).await()
        else {
            chatRef.child(editId).setValue(MessageRec(model.auth.uid!!, to, text, messages[editId]!!.message.timestamp)).await()
            cancelButton.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        model.db.getReference("users/${model.auth.uid}/inChatWith").setValue(args.uid)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        model.db.getReference("users/${model.auth.uid}/inChatWith").setValue(null)
    }
}