package com.helow.messenger

import android.content.Context
import android.os.Build
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
import com.google.firebase.database.ktx.getValue
import kotlinx.android.synthetic.main.fragment_profile.*

class ProfileFragment : Fragment() {
    private val model: MainActivityViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val preferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        var name: String? = null

        model.db.getReference("users/${model.auth.uid}/username").addValueEventListener(viewLifecycleOwner, object : MyValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                name = snapshot.getValue<String>()!!
                username.setText(name)
            }
        })

        update_button.setOnClickListener {
            model.db.getReference("users/${model.auth.uid}/username").setValue(username.text.toString())
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            change_locale_button.visibility = View.GONE
        else
            change_locale_button.setOnClickListener {
                val locale = preferences.getString("locale", "ru")
                MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.change_locale)
                    .setNeutralButton(R.string.cancel) {_, _ ->
                        preferences.edit(commit = true) {
                            putString("locale", locale)
                        }
                    }
                    .setPositiveButton(R.string.ok) { _, _ ->
                        requireActivity().recreate()
                    }
                    .setSingleChoiceItems(availableLocales.values.toTypedArray(), availableLocales.keys.indexOf(locale)) {_, which ->
                        preferences.edit(commit = true) {
                            putString("locale", availableLocales.keys.toList()[which])
                        }
                    }
                    .show()
            }

        username.addTextChangedListener {
            when {
                it.toString() == name -> update_button.isEnabled = false
                it.isNullOrBlank() -> username_view.error = getString(R.string.empty_field)
                else -> {
                    username_view.error = null
                    update_button.isEnabled = true
                }
            }
        }
    }
}