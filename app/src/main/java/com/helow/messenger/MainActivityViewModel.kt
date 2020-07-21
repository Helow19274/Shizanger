package com.helow.messenger

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivityViewModel : ViewModel() {
    val auth = Firebase.auth
    val db = Firebase.database
    val instanceId = FirebaseInstanceId.getInstance()
    val messaging = FirebaseMessaging.getInstance()
    val contacts = MutableLiveData<ArrayList<ContactItem>>()
    private var listenersInitialized = false

    fun initContactsListeners() {
        if (listenersInitialized)
            return
        db.getReference("users/${auth.uid}/contacts").addChildEventListener(object : ChildEventListener {
            override fun onCancelled(error: DatabaseError) { }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onChildAdded(snapshot2: DataSnapshot, previousChildName: String?) {
                val data = snapshot2.getValue<String>()!!
                db.getReference("users/$data").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) { }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        contacts += ContactItem(snapshot.getValue<UserRec>()!!, snapshot2.key)
                    }
                })
            }

            override fun onChildRemoved(snapshot2: DataSnapshot) {
                val data = snapshot2.getValue<String>()!!
                db.getReference("users/$data").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) { }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        contacts -= ContactItem(snapshot.getValue<UserRec>()!!, snapshot2.key)
                    }
                })
            }
        })
        listenersInitialized = true
    }
}