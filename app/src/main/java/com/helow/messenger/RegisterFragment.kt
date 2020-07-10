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
                usernameView.error = "Field is empty"
            if (email.text.isNullOrBlank())
                emailView.error = "Field is empty"
            if (password.text.isNullOrBlank())
                passwordView.error = "Field is empty"
            if (!username.text.isNullOrBlank() && !email.text.isNullOrBlank() && !password.text.isNullOrBlank())
                signUpUser(username.text.toString(), email.text.toString(), password.text.toString())
        }

        username.addTextChangedListener {
            if (it.isNullOrBlank())
                usernameView.error = "Field is empty"
            else
                usernameView.error = null
        }

        email.addTextChangedListener {
            if (it.isNullOrBlank())
                emailView.error = "Field is empty"
            else
                emailView.error = null
        }

        password.addTextChangedListener {
            if (it.isNullOrBlank())
                passwordView.error = "Field is empty"
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
                val uid = model.auth.currentUser!!.uid
                model.db.getReference("users/$uid").setValue(User(uid, username, email)).await()
                model.messaging.isAutoInitEnabled = true
                findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToContactsFragment())
            } catch (e: Exception) {
                val view = requireView()
                when (e) {
                    is FirebaseAuthUserCollisionException -> {
                        view.email_view.error = "User already exists"
                        view.password_view.error = null
                    }
                    is FirebaseAuthWeakPasswordException -> {
                        view.email_view.error = null
                        view.password_view.error = "Password is too weak"
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        view.email_view.error = "Bad email format"
                        view.password_view.error = null
                    }
                    else -> Snackbar.make(view, "Unexpected error occurred: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}