package com.helow.messenger

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.ktx.storage

class MainActivityViewModel : ViewModel() {
    val auth = Firebase.auth
    val db = Firebase.database
    val storage = Firebase.storage.getReference("images")
    val instanceId = FirebaseInstanceId.getInstance()
    val messaging = FirebaseMessaging.getInstance()
    val contacts = MutableLiveData<ArrayList<UserItem>>()
    private var listenersInitialized = false

    fun initContactsListeners() {
        if (listenersInitialized)
            return
        db.getReference("users/${auth.uid}/contacts").addChildEventListener(object : MyChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key
                db.getReference("users/${snapshot.getValue<String>()!!}").addListenerForSingleValueEvent(object : MyValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        contacts += UserItem(snapshot.getValue<UserRec>()!!, key)
                    }
                })
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val key = snapshot.key
                db.getReference("users/${snapshot.getValue<String>()!!}").addListenerForSingleValueEvent(object : MyValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        contacts -= UserItem(snapshot.getValue<UserRec>()!!, key)
                    }
                })
            }
        })
        listenersInitialized = true
    }
}