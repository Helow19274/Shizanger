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
import kotlinx.android.synthetic.main.fragment_login.view.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginFragment : Fragment() {
    private val args: LoginFragmentArgs by navArgs()
    private val model: MainActivityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.email.setText(args.email)
        view.password.setText(args.password)

        view.button_login.setOnClickListener {
            if (view.email.text.isNullOrBlank())
                view.email_view.error = getString(R.string.empty_field)
            if (view.password.text.isNullOrBlank())
                view.password_view.error = getString(R.string.empty_field)
            if (!view.email.text.isNullOrBlank() && !view.password.text.isNullOrBlank()) {
                view.button_login.isEnabled = false
                signInUser(view.email.text.toString(), view.password.text.toString())
            }
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

        view.button_not_registered.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToRegisterFragment(
                view.email.text.toString(), view.password.text.toString()
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
                view.button_login.isEnabled = true
                when (e) {
                    is FirebaseAuthInvalidUserException -> {
                        view.email_view.error = getString(R.string.user_not_exists)
                        view.password_view.error = null
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        view.email_view.error = null
                        view.password_view.error = getString(R.string.wrong_password)
                    }
                    else -> Snackbar.make(view, "Unexpected error occurred: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }
}