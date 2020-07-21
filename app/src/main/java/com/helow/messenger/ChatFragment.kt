package com.helow.messenger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.mikepenz.fastadapter.scroll.EndlessRecyclerOnScrollListener
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
    private var editId = ""
    private var beforeEditText = ""
    private var lastKey: String? = null
    private var loaded = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_chat, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = (requireActivity() as MainActivity)
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        chatRef = model.db.getReference("messages/${if (model.auth.uid!! < args.uid) "${model.auth.uid}-${args.uid}" else "${args.uid}-${model.auth.uid}"}")

        view.recycler_view.adapter = fastAdapter

        fastAdapter.onClickListener = { _, _, item, _ ->
            val items = if (item.sent)
                arrayOf(getString(R.string.edit), getString(R.string.remove))
            else
                arrayOf(getString(R.string.remove))
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.choose_action)
                .setItems(items) { _, which ->
                    when (which) {
                        0 -> {
                            if (items.size == 2) {
                                editId = item.messageId
                                beforeEditText = view.message.text.toString()
                                view.message.apply {
                                    text?.clear()
                                    append(item.message.text)
                                    requestFocus()
                                    postDelayed({
                                        imm.showSoftInput(view.message, InputMethodManager.SHOW_IMPLICIT)
                                    }, 100)
                                }
                                view.cancel_edit_button.visibility = View.VISIBLE
                            } else
                                chatRef.child(item.messageId).removeValue()
                        }
                        1 -> chatRef.child(item.messageId).removeValue()
                    }
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
            false
        }

        view.recycler_view.addOnScrollListener(object : EndlessRecyclerOnScrollListener(view.recycler_view.layoutManager!!) {
            override fun onLoadMore(currentPage: Int) {
                if (loaded < 50 && currentPage >= 2)
                    return
                loaded = 0
                loadMoreMessages()
            }
        })

        model.db.getReference("users/${args.uid}").addValueEventListener(viewLifecycleOwner, object :
            ValueEventListener {
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

        chatRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) { }

            override fun onDataChange(snapshot: DataSnapshot) {
                lastKey = snapshot.children.firstOrNull()?.key
                addNewMessagesListener()
            }
        })

        view.send_button.setOnClickListener {
            val chunks = view.message.text!!.chunked(2048)
            lifecycleScope.launch {
                for (chunk in chunks)
                    sendMessage(args.uid, chunk)
                view.message.text?.clear()
                view.message.append(beforeEditText)
                editId = ""
                beforeEditText = ""
            }
        }

        view.message.addTextChangedListener {
            view.send_button.isEnabled = !it.isNullOrBlank()
        }

        view.cancel_edit_button.setOnClickListener {
            view.message.text?.clear()
            editId = ""
            view.cancel_edit_button.visibility = View.GONE
            view.message.text?.clear()
            view.message.append(beforeEditText)
            beforeEditText = ""
        }

        if (args.messageFromShare != null) {
            view.message.append(args.messageFromShare)
            activity.intent.removeExtra(Intent.EXTRA_TEXT)
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

    private suspend fun sendMessage(to: String, text: String) {
        val messageText = text.trim()
        if (editId == "")
            chatRef.push().setValue(Message(model.auth.uid!!, to, messageText)).await()
        else {
            chatRef.child(editId).setValue(MessageRec(model.auth.uid!!, to, messageText, messages[editId]!!.message.timestamp)).await()
            requireView().cancel_edit_button.visibility = View.GONE
        }
    }

    private fun addNewMessagesListener() {
        var keySet = false
        chatRef.orderByKey().startAt(lastKey ?: "").addChildEventListener(viewLifecycleOwner, object : ChildEventListener {
            override fun onCancelled(error: DatabaseError) { }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!keySet) {
                    lastKey = snapshot.key
                    keySet = true
                }
                val value = snapshot.getValue<MessageRec>()!!
                messages[snapshot.key!!] = MessageItem(value, value.from == model.auth.uid, snapshot.key!!)
                adapter.add(0, messages[snapshot.key!!]!!)
                requireView().recycler_view.scrollToPosition(0)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                messages.remove(snapshot.key!!)
                adapter.set(messages.toSortedMap().values.reversed())
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue<MessageRec>()!!
                if (value.text == messages[snapshot.key!!]!!.message.text)
                    return
                val mesageExists = messages[snapshot.key!!]
                messages[snapshot.key!!] = MessageItem(value, value.from == model.auth.uid, snapshot.key!!)
                adapter.set(messages.toSortedMap().values.reversed())
                if (mesageExists == null)
                    requireView().recycler_view.scrollToPosition(0)
            }
        })
    }

    private fun loadMoreMessages() {
        val position = adapter.adapterItemCount
        if (lastKey == null)
            return
        var keySet = false
        chatRef.orderByKey().endAt(lastKey).limitToLast(51).addChildEventListener(viewLifecycleOwner, object : ChildEventListener {
            override fun onCancelled(error: DatabaseError) { }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }

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
                messages[snapshot.key!!] = MessageItem(value, value.from == model.auth.uid, snapshot.key!!)
                adapter.add(position, messages[snapshot.key!!]!!)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                messages.remove(snapshot.key!!)
                adapter.set(messages.toSortedMap().values.reversed())
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val value = snapshot.getValue<MessageRec>()!!
                messages[snapshot.key!!] = MessageItem(value, value.from == model.auth.uid, snapshot.key!!)
                adapter.set(messages.toSortedMap().values.reversed())
            }
        })
    }
}