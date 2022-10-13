package com.snapyr.flappybird

import android.content.Context

class SnapyrData private constructor(val context: Context) {

    var preferences = context.applicationContext.getSharedPreferences("snapyrConfig", Context.MODE_PRIVATE)
    var prefEditor = preferences.edit()

    fun destroy() {
        ourInstance = null
    }

    companion object {
        private var ourInstance: SnapyrData? = null

        val instance: SnapyrData
            get() {
                if (ourInstance == null)
                    ourInstance = SnapyrData(MyApp.appContext)
                return ourInstance!!
            }
    }
        var env: String = preferences.getString("env", "")!!
            set(value) {
                field = value
                prefEditor.putString("env", value)
                prefEditor.apply()
            }
        var identifyKey: String = preferences.getString("identifyKey", "")!!
            set(value) {
                field = value
                prefEditor.putString("identifyKey", value)
                prefEditor.apply()
            }
        var identifyUserId: String = preferences.getString("identifyUserId", "")!!
            set(value) {
                field = value
                prefEditor.putString("identifyUserId", value)
                prefEditor.apply()
            }
        var identifyName: String = preferences.getString("identifyName", "")!!
            set(value) {
                field = value
                prefEditor.putString("identifyName", value)
                prefEditor.apply()
            }
        var identifyEmail: String = preferences.getString("identifyEmail", "")!!
            set(value) {
                field = value
                prefEditor.putString("identifyEmail", value)
                prefEditor.apply()
            }
        var identifyPhone: String = preferences.getString("identifyPhone", "")!!
            set(value) {
                field = value
                prefEditor.putString("identifyPhone", value)
                prefEditor.apply()
            }
}
