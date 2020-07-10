package com.helow.messenger

import androidx.lifecycle.*
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

operator fun <T> MutableLiveData<ArrayList<T>>.plusAssign(values: List<T>) {
    val value = this.value ?: arrayListOf()
    value.addAll(values)
    this.value = value
}

operator fun <T> MutableLiveData<ArrayList<T>>.plusAssign(values: T) {
    val value = this.value ?: arrayListOf()
    value.add(values)
    this.value = value
}

operator fun <T> MutableLiveData<ArrayList<T>>.minusAssign(values: T) {
    val uid = (values as ContactItem).user.uid

    val value = this.value?.filterNot { (it as ContactItem).user.uid == uid } ?: listOf()
    this.value = ArrayList(value.toMutableList())
}

fun Query.addSnapshotListener(lifecycleOwner: LifecycleOwner, listener: (QuerySnapshot?, FirebaseFirestoreException?) -> Unit) {
    val registration = addSnapshotListener(listener)
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            registration.remove()
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}

fun DatabaseReference.addValueEventListener(lifecycleOwner: LifecycleOwner, listener: ValueEventListener) {
    addValueEventListener(listener)
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            removeEventListener(listener)
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}

fun DatabaseReference.addChildEventListener(lifecycleOwner: LifecycleOwner, listener: ChildEventListener) {
    addChildEventListener(listener)
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            removeEventListener(listener)
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}