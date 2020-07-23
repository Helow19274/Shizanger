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
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginFragment : Fragment() {
    private val args: LoginFragmentArgs by navArgs()
    private val model: MainActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        email.setText(args.email)
        password.setText(args.password)

        button_login.setOnClickListener {
            if (email.text.isNullOrBlank())
                email_view.error = getString(R.string.empty_field)
            if (password.text.isNullOrBlank())
                password_view.error = getString(R.string.empty_field)
            if (!email.text.isNullOrBlank() && !password.text.isNullOrBlank()) {
                button_login.isEnabled = false
                signInUser(email.text.toString(), password.text.toString())
            }
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

        button_not_registered.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToRegisterFragment(
                email.text.toString(), password.text.toString()
            ))
        }
    }

    private fun signInUser(email: String, password: String) {
        lifecycleScope.launch {
            try {
                model.auth.signInWithEmailAndPassword(email, password).await()
                model.messaging.isAutoInitEnabled = true
                model.db.getReference("users/${model.auth.uid}/online").setValue(true)
                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToContactsFragment())
            } catch (e: Exception) {
                val view = requireView()
                button_login.isEnabled = true
                when (e) {
                    is FirebaseAuthInvalidUserException -> {
                        email_view.error = getString(R.string.user_not_exists)
                        password_view.error = null
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        email_view.error = null
                        password_view.error = getString(R.string.wrong_password)
                    }
                    else -> Snackbar.make(view, "Unexpected error occurred: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}