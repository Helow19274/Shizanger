package com.helow.messenger

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import kotlinx.android.synthetic.main.fragment_profile.view.*

class ProfileFragment : Fragment() {
    private val model: MainActivityViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val preferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        var name = ""

        model.db.getReference("users/${model.auth.uid}/username").addValueEventListener(viewLifecycleOwner, object :
            ValueEventListener {
            override fun onCancelled(error: DatabaseError) { }

            override fun onDataChange(snapshot: DataSnapshot) {
                name = snapshot.getValue<String>()!!
                view.username.setText(name)
            }
        })

        view.update_button.setOnClickListener {
            model.db.getReference("users/${model.auth.uid}/username").setValue(view.username.text.toString())
        }

        view.change_locale_button.setOnClickListener {
            val locale = preferences.getString("locale", "ru")
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.change_locale)
                .setNegativeButton(R.string.cancel) {_, _ ->
                    preferences.edit(commit = true) {
                        putString("locale", locale)
                    }
                }
                .setPositiveButton("OK") {_, _ ->
                    requireActivity().recreate()
                }
                .setSingleChoiceItems(availableLocales.values.toTypedArray(), availableLocales.keys.indexOf(locale)) {_, which ->
                    preferences.edit(commit = true) {
                        putString("locale", availableLocales.keys.toList()[which])
                    }
                }
                .show()
        }

        view.username.addTextChangedListener {
            when {
                it.isNullOrBlank() -> view.username_view.error = "Field is empty"
                it.toString() == name -> view.update_button.isEnabled = false
                else -> {
                    view.username_view.error = null
                    view.update_button.isEnabled = true
                }
            }
        }
    }
}