package com.helow.messenger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.android.synthetic.main.fragment_register.view.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterFragment : Fragment() {
    private val args: RegisterFragmentArgs by navArgs()
    private val model: MainActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.email.setText(args.email)
        view.password.setText(args.password)

        view.button_register.setOnClickListener {
            if (view.username.text.isNullOrBlank())
                view.username_view.error = getString(R.string.empty_field)
            if (view.email.text.isNullOrBlank())
                view.email_view.error = getString(R.string.empty_field)
            if (view.password.text.isNullOrBlank())
                view.password_view.error = getString(R.string.empty_field)
            if (!view.username.text.isNullOrBlank() && !view.email.text.isNullOrBlank() && !view.password.text.isNullOrBlank()) {
                view.button_register.isEnabled = false
                signUpUser(view.username.text.toString(), view.email.text.toString(), view.password.text.toString())
            }
        }

        view.username.addTextChangedListener {
            if (it.isNullOrBlank())
                view.username_view.error = getString(R.string.empty_field)
            else
                view.username_view.error = null
        }

        view.email.addTextChangedListener {
            if (it.isNullOrBlank())
                view.email_view.error = getString(R.string.empty_field)
            else
                view.email_view.error = null
        }

        view.password.addTextChangedListener {
            if (it.isNullOrBlank())
                view.password_view.error = getString(R.string.empty_field)
            else
                view.password_view.error = null
        }

        view.button_already_registered.setOnClickListener {
            findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToLoginFragment(
                view.email.text.toString(), view.password.text.toString()
            ))
        }
    }

    private fun signUpUser(username: String, email: String, password: String) {
        lifecycleScope.launch {
            try {
                model.auth.createUserWithEmailAndPassword(email, password).await()
                val uid = model.auth.uid!!
                model.db.getReference("users/$uid").setValue(User(uid, username, email)).await()
                model.messaging.isAutoInitEnabled = true
                findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToContactsFragment())
            } catch (e: Exception) {
                val view = requireView()
                view.button_register.isEnabled = true
                when (e) {
                    is FirebaseAuthUserCollisionException -> {
                        view.email_view.error = getString(R.string.user_already_exists)
                        view.password_view.error = null
                    }
                    is FirebaseAuthWeakPasswordException -> {
                        view.email_view.error = null
                        view.password_view.error = getString(R.string.too_weak_password)
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        view.email_view.error = getString(R.string.bad_email_format)
                        view.password_view.error = null
                    }
                    else -> Snackbar.make(view, "Unexpected error occurred: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}