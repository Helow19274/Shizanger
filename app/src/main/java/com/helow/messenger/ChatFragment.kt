package com.helow.messenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
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
    private lateinit var activity: MainActivity
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        model.db.getReference("users/${model.auth.currentUser?.uid}/inChatWith").setValue(args.uid)
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        val sendButton = view.send_button
        recyclerView = view.recycler_view
        activity = (requireActivity() as MainActivity)

        recyclerView.adapter = fastAdapter

        recyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            if (adapter.adapterItemCount > 0)
                recyclerView.scrollToPosition(adapter.adapterItemCount - 1)
        }

        fastAdapter.onLongClickListener = { _, _, item, _ ->
            MaterialAlertDialogBuilder(context)
                .setTitle("Are you sure?")
                .setMessage("Remove message?")
                .setNegativeButton("No") { _, _ -> }
                .setPositiveButton("Yes") { _, _ ->
                    lifecycleScope.launch {
                        model.firestore.document(item.messageId).delete().await()
                    }
                }
                .show()
            false
        }

        model.db.getReference("users/${args.uid}").addValueEventListener(viewLifecycleOwner, object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) { }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue<User>()!!
                activity.setActionBarSubTitle(if (user.online) "Online" else "Last seen ${DateTimeFormatter
                    .ofPattern("d.MM 'at' HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(user.lastSeen))}")
            }
        })

        model.db.getReference("users/${args.uid}").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) { }

            override fun onDataChange(snapshot: DataSnapshot) {
                activity.setActionBarTitle(snapshot.getValue<User>()!!.username)
            }
        })

        lifecycleScope.launch {
            val messages = model.firestore.whereEqualTo("from", model.auth.currentUser?.uid).whereEqualTo("to", args.uid).get().await() + model.firestore.whereEqualTo("to", model.auth.currentUser?.uid).whereEqualTo("from", args.uid).get().await()
            for (message in messages.sortedBy { it.getTimestamp("timestamp") }) {
                val obj = message.toObject<Message>()
                adapter.add(MessageItem(obj, obj.from == model.auth.currentUser?.uid, message.id))
            }
            if (adapter.adapterItemCount > 0)
                recyclerView.scrollToPosition(adapter.adapterItemCount - 1)

            var firstSent = false
            var firstReceived = false

            model.firestore.whereEqualTo("from", model.auth.currentUser?.uid).whereEqualTo("to", args.uid)
                .addSnapshotListener(viewLifecycleOwner) { querySnapshot, _ ->
                    if (firstSent) {
                        if (!querySnapshot!!.metadata.hasPendingWrites())
                            addFromSnapshot(querySnapshot, true)
                    }
                    else
                        firstSent = true
                }

            model.firestore.whereEqualTo("to", model.auth.currentUser?.uid).whereEqualTo("from", args.uid)
                .addSnapshotListener(viewLifecycleOwner) { querySnapshot, _ ->
                    if (firstReceived) {
                        if (!querySnapshot!!.metadata.hasPendingWrites())
                            addFromSnapshot(querySnapshot, false)
                    }
                    else
                        firstReceived = true
                }

            if (args.messageFromShare != null)
                sendMessage(args.uid, args.messageFromShare!!)
        }

        sendButton.setOnClickListener {
            val chunks = view.message.text!!.chunked(2048)
            lifecycleScope.launch {
                for (chunk in chunks)
                    sendMessage(args.uid, chunk)
            }
            view.message.text = null
        }

        view.message.addTextChangedListener {
            sendButton.isEnabled = !it.isNullOrBlank()
        }

        return view
    }

    private fun addFromSnapshot(querySnapshot: QuerySnapshot, sent: Boolean) {
        for (change in querySnapshot.documentChanges) {
            if (change.type != DocumentChange.Type.REMOVED) {
                adapter.add(MessageItem(change.document.toObject(), sent, change.document.id))
                recyclerView.scrollToPosition(adapter.adapterItemCount - 1)
            } else
                adapter.set(adapter.itemList.items.filterNot { it.messageId == change.document.id })
        }
    }

    private suspend fun sendMessage(to: String, text: String) {
        model.firestore.add(Message(model.auth.currentUser!!.uid, to, text)).await()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        model.db.getReference("users/${model.auth.currentUser?.uid}/inChatWith").setValue(null)
    }
}