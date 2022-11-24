package com.snapyr.flappybird

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log

class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()

        Log.e("SnapyrFlappy", "APPLICATION onCreate")

        MyApp.appContext = applicationContext

        var snapyrHelper = SnapyrComponent.build(this.applicationContext)
        // Reset SDK (clear user id / batch queue) if env or write key were changed prior to this run
        if (SnapyrData.instance.needsReset) {
            snapyrHelper.onDoReset()
            SnapyrData.instance.needsReset = false
        }
    }

    companion object {
        lateinit var appContext: Context
    }
}