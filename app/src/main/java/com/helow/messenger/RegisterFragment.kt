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
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterFragment : Fragment() {
    private val args: RegisterFragmentArgs by navArgs()
    private val model: MainActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email.setText(args.email)
        password.setText(args.password)

        button_register.setOnClickListener {
            if (username.text.isNullOrBlank())
                username_view.error = getString(R.string.empty_field)
            if (email.text.isNullOrBlank())
                email_view.error = getString(R.string.empty_field)
            if (password.text.isNullOrBlank())
                password_view.error = getString(R.string.empty_field)
            if (!username.text.isNullOrBlank() && !email.text.isNullOrBlank() && !password.text.isNullOrBlank()) {
                button_register.isEnabled = false
                signUpUser(username.text.toString(), email.text.toString(), password.text.toString())
            }
        }

        username.addTextChangedListener {
            if (it.isNullOrBlank())
                username_view.error = getString(R.string.empty_field)
            else
                username_view.error = null
        }

        email.addTextChangedListener {
            if (it.isNullOrBlank())
                email_view.error = getString(R.string.empty_field)
            else
                email_view.error = null
        }

        password.addTextChangedListener {
            if (it.isNullOrBlank())
                password_view.error = getString(R.string.empty_field)
            else
                password_view.error = null
        }

        button_already_registered.setOnClickListener {
            findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToLoginFragment(
                email.text.toString(), password.text.toString()
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
                button_register.isEnabled = true
                when (e) {
                    is FirebaseAuthUserCollisionException -> {
                        email_view.error = getString(R.string.user_already_exists)
                        password_view.error = null
                    }
                    is FirebaseAuthWeakPasswordException -> {
                        email_view.error = null
                        password_view.error = getString(R.string.too_weak_password)
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        email_view.error = getString(R.string.bad_email_format)
                        password_view.error = null
                    }
                    else -> Snackbar.make(view, "Unexpected error occurred: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}