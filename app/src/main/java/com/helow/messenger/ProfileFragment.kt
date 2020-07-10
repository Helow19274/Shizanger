package com.helow.messenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.android.synthetic.main.fragment_profile.view.*

class ProfileFragment : Fragment() {
    private val model: MainActivityViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val username = view.username
        val usernameView = view.username_view
        var name = ""

        model.db.getReference("users/${model.auth.currentUser!!.uid}/username").addValueEventListener(viewLifecycleOwner, object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) { }

            override fun onDataChange(snapshot: DataSnapshot) {
                name = snapshot.getValue<String>()!!
                username.setText(name)
            }
        })

        view.update_button.setOnClickListener {
            model.db.getReference("users/${model.auth.uid}/username").setValue(view.username.text.toString())
        }

        username.addTextChangedListener {
            when {
                it.isNullOrBlank() -> usernameView.error = "Field is empty"
                it.toString() == name -> view.update_button.isEnabled = false
                else -> {
                    usernameView.error = null
                    view.update_button.isEnabled = true
                }
            }
        }

        return view
    }
}