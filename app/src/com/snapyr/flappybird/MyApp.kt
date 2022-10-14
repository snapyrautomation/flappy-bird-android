package com.snapyr.flappybird

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager

class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs1 = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = prefs1.edit()
        editor.clear()
        editor.commit()

        MyApp.appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
    }
}