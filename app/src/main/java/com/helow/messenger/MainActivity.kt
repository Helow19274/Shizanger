package com.helow.messenger

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private val model: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(materialToolbar)

        navController = findNavController(R.id.fragment)

        val appBarConf = AppBarConfiguration(setOf(R.id.loginFragment, R.id.contactsFragment))
        materialToolbar.setupWithNavController(navController, appBarConf)

        val navHostFragment = fragment as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.my_nav)
        if (model.auth.currentUser != null)
            graph.startDestination = R.id.contactsFragment

        navHostFragment.navController.graph = graph

        navController.addOnDestinationChangedListener { _, destination, _ ->
            setActionBarTitle(destination.label)
            if (destination.id != R.id.chatFragment)
                setActionBarSubTitle(null)
        }
    }

    override fun onSupportNavigateUp() = super.onSupportNavigateUp() || navController.navigateUp()

    fun setActionBarTitle(title: CharSequence?) {
        supportActionBar!!.title = title
    }

    fun setActionBarSubTitle(title: CharSequence?) {
        supportActionBar!!.subtitle = title
    }

    override fun onStart() {
        super.onStart()
        if (model.auth.currentUser != null)
            model.db.getReference("users/${model.auth.uid}/online").setValue(true)
    }

    override fun onStop() {
        super.onStop()
        intent.removeExtra(Intent.EXTRA_TEXT)
        if (model.auth.currentUser != null)
            model.db.getReference("users/${model.auth.uid}/online").setValue(false)
    }
}