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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        val username = view.username
        val usernameView = view.username_view
        val email = view.email
        val emailView = view.email_view
        val password = view.password
        val passwordView = view.password_view

        email.setText(args.email)
        password.setText(args.password)

        view.button_register.setOnClickListener {
            if (username.text.isNullOrBlank())
                usernameView.error = getString(R.string.empty_field)
            if (email.text.isNullOrBlank())
                emailView.error = getString(R.string.empty_field)
            if (password.text.isNullOrBlank())
                passwordView.error = getString(R.string.empty_field)
            if (!username.text.isNullOrBlank() && !email.text.isNullOrBlank() && !password.text.isNullOrBlank())
                signUpUser(username.text.toString(), email.text.toString(), password.text.toString())
        }

        username.addTextChangedListener {
            if (it.isNullOrBlank())
                usernameView.error = getString(R.string.empty_field)
            else
                usernameView.error = null
        }

        email.addTextChangedListener {
            if (it.isNullOrBlank())
                emailView.error = getString(R.string.empty_field)
            else
                emailView.error = null
        }

        password.addTextChangedListener {
            if (it.isNullOrBlank())
                passwordView.error = getString(R.string.empty_field)
            else
                passwordView.error = null
        }

        view.button_already_registered.setOnClickListener {
            findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToLoginFragment(
                view.email.text.toString(), view.password.text.toString()
            ))
        }
        return view
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
                when (e) {
                    is FirebaseAuthUserCollisionException -> {
                        view.email_view.error = getString(R.string.user_already_exists)
                        view.password_view.error = null
                    }
                    is FirebaseAuthWeakPasswordException -> {
                        view.email_view.error = null
                        view.password_view.error = getString(R.string.too_weak_pasword)
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