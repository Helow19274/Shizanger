package com.helow.messenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.android.synthetic.main.fragment_add_contacts.view.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddContactsFragment : Fragment() {

    private val adapter = ItemAdapter<ContactItem>()
    private val fastAdapter = FastAdapter.with(adapter)
    private val model: MainActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_add_contacts, container, false)
        view.recycler_view.adapter = fastAdapter

        fastAdapter.onClickListener = { _, _, item, position ->
            val ref = model.db.getReference("users/${model.auth.uid}/contacts")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) { }

                override fun onDataChange(snapshot: DataSnapshot) {
                    lifecycleScope.launch {
                        ref.push().setValue(item.user.uid).await()
                        adapter.remove(position)
                    }
                }
            })
            false
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val uids = model.contacts.value.orEmpty().map { it.user.uid } + listOf(model.auth.uid)

        model.db.getReference("users").addChildEventListener(viewLifecycleOwner, object : ChildEventListener {
            override fun onCancelled(error: DatabaseError) { }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.key !in uids)
                    adapter.add(ContactItem(snapshot.getValue<UserRec>()!!))
            }

            override fun onChildRemoved(snapshot: DataSnapshot) { }

        })
    }
}