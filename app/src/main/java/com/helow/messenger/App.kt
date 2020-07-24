package com.helow.messenger

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@Suppress("UNUSED")
class App : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(wrapContextWithLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overrideLocale(this)
    }

    override fun onCreate() {
        super.onCreate()
        Firebase.database.setPersistenceEnabled(true)
    }
}