package com.helow.messenger

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import kotlinx.android.synthetic.main.fragment_contacts.view.*
import kotlin.concurrent.thread

class ContactsFragment : Fragment() {
    private val model: MainActivityViewModel by activityViewModels()
    private val adapter = ItemAdapter<ContactItem>()
    private val fastAdapter = FastAdapter.with(adapter)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_contacts, container, false)
        setHasOptionsMenu(true)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.recycler_view.adapter = fastAdapter
        model.initContactsListeners()

        model.contacts.observe(viewLifecycleOwner, Observer {
            adapter.set(it)
        })

        fastAdapter.onClickListener = { _, _, item, _ ->
            val activity = requireActivity() as MainActivity
            findNavController().navigate(ContactsFragmentDirections.actionContactsFragmentToChatFragment(item.user.uid, activity.intent.getStringExtra(Intent.EXTRA_TEXT)))
            false
        }

        fastAdapter.onLongClickListener = { _, _, item, _ ->
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.remove_contact)
                .setNegativeButton(R.string.no) { _, _ -> }
                .setPositiveButton(R.string.yes) { _, _ ->
                    model.db.getReference("users/${model.auth.uid}/contacts/${item.contactKey}").removeValue()
                }
                .show()
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.log_out -> {
                model.db.getReference("users/${model.auth.uid}/online").setValue(false)
                model.messaging.isAutoInitEnabled = false
                model.db.getReference("/users/${model.auth.uid}/token").setValue(null).addOnSuccessListener {
                    thread {
                        model.instanceId.deleteInstanceId()
                    }
                    model.auth.signOut()
                    model.contacts.value?.clear()
                    findNavController().navigate(ContactsFragmentDirections.actionChatsFragmentToLoginFragment())
                }
            }

            R.id.add_contacts -> findNavController().navigate(ContactsFragmentDirections.actionContactsFragmentToAddContactsFragment())

            R.id.profile -> findNavController().navigate(ContactsFragmentDirections.actionContactsFragmentToProfileFragment())
        }
        return super.onOptionsItemSelected(item)
    }
}