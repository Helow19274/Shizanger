package com.helow.messenger

import android.app.Application
import android.content.Context
import android.content.res.Configuration

@Suppress("UNUSED")
class App : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(wrapContextWithLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overrideLocale(this)
    }
}