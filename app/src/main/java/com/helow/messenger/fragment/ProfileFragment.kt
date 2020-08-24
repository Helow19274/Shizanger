package com.helow.messenger.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ktx.getValue
import com.helow.messenger.*
import com.helow.messenger.model.UserRec
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {
    private val model: MainActivityViewModel by viewModels()
    private lateinit var preferences: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE)
        var name: String? = null

        model.db.getReference("users/${model.auth.uid}").addValueEventListener(viewLifecycleOwner, object : MyValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue<UserRec>()!!
                name = user.username
                username.setText(name)
                if (user.imageUrl != null)
                    GlideApp
                        .with(view)
                        .load(user.imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(profile_image)
                else
                    profile_image.setImageResource(R.drawable.default_profile)
            }
        })

        update_username_button.setOnClickListener {
            model.db.getReference("users/${model.auth.uid}/username").setValue(username.text.toString())
        }

        profile_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), 1)
        }

        profile_image.setOnLongClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.remove_profile_photo)
                .setPositiveButton(R.string.yes) {_, _ ->
                    model.db.getReference("users/${model.auth.uid}/imageUrl").removeValue()
                }
                .setNegativeButton(R.string.cancel) {_, _ -> }
                .show()
            true
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            change_locale_button.visibility = View.GONE
        else
            change_locale_button.setOnClickListener {
                val locale = preferences.getString("locale", "ru")
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.change_locale)
                    .setNeutralButton(R.string.cancel) { _, _ ->
                        preferences.edit(commit = true) {
                            putString("locale", locale)
                        }
                    }
                    .setPositiveButton(R.string.ok) { _, _ ->
                        requireActivity().recreate()
                    }
                    .setSingleChoiceItems(availableLocales.values.toTypedArray(), availableLocales.keys.indexOf(locale)) { _, which ->
                        preferences.edit(commit = true) {
                            putString("locale", availableLocales.keys.toList()[which])
                        }
                    }
                    .show()
            }

        username.addTextChangedListener {
            when {
                it.toString() == name -> update_username_button.isEnabled = false
                it.isNullOrBlank() -> username_view.error = getString(R.string.empty_field)
                else -> {
                    username_view.error = null
                    update_username_button.isEnabled = true
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val exifStream = requireActivity().contentResolver.openInputStream(data.data!!)!!
            val exif = ExifInterface(exifStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            exifStream.close()

            val stream = requireActivity().contentResolver.openInputStream(data.data!!)!!
            val image = getCircledBitmap(BitmapFactory.decodeStream(stream), exif)
            val s = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.PNG, 100, s)
            val ref = model.profileStorage.child(model.auth.uid!!)
            lifecycleScope.launch {
                ref.putBytes(s.toByteArray()).await()
                val url = ref.downloadUrl.await().toString()
                model.db.getReference("users/${model.auth.uid}/imageUrl").setValue(url)
                preferences.edit {
                    putString("imageUrl", url)
                }
            }
        }
    }
}