/*
 * Copyright 2018 Konstantinos Drakonakis.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.snapyr.flappybird

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.snapyr.sdk.Properties
import com.snapyr.sdk.Snapyr
import com.snapyr.sdk.Traits
import com.snapyr.sdk.inapp.InAppCallback
import com.snapyr.sdk.inapp.InAppConfig
import com.snapyr.sdk.inapp.InAppMessage


class SnapyrComponent private constructor(private val context: Context) {

    var snapyrData: SnapyrData = SnapyrData.instance;
    var preferences = context.applicationContext.getSharedPreferences("snapyrConfig", Context.MODE_PRIVATE)
    var inappListeners = mutableMapOf<String, InAppCallback>()

    companion object {
        private var ourInstance: SnapyrComponent? = null

        val hasInstance: Boolean
            get() = ourInstance != null

        val instance: SnapyrComponent
            get() {
                synchronized(SnapyrComponent) {
                    if (ourInstance == null) {
                        throw Exception("Run `build()` before accessing instance")
                    }
                    return ourInstance!!
                }
            }

        internal fun build(context: Context): SnapyrComponent {
            synchronized(SnapyrComponent) {
                if (ourInstance == null) {
                    ourInstance = SnapyrComponent(context)
                }
                var snapyr = Snapyr.Builder(context, SnapyrData.instance.sdkWriteKey)
                Log.d("SnapyrFlappy", "singleton.env: " + ourInstance!!.snapyrData.env);
                if (ourInstance!!.snapyrData.env == "dev")
                    snapyr.enableDevEnvironment()
                if (ourInstance!!.snapyrData.env == "stg")
                    snapyr.enableStageEnvironment()
                snapyr.enableSnapyrPushHandling()
                    .logLevel(Snapyr.LogLevel.VERBOSE)
                    .trackApplicationLifecycleEvents() // Enable this to record certain application events automatically
                    .recordScreenViews() // Enable this to record screen views automatically
                    .flushQueueSize(1)
                    .configureInAppHandling(
                        InAppConfig()
                            .setPollingRate(30000)
                            .setActionCallback { inAppMessage: InAppMessage? ->
                                if (inAppMessage != null) {
                                    ourInstance!!.userInAppCallback(
                                        inAppMessage
                                    )
                                }
                            })
                ;
                if (!Snapyr.Valid()) {
                    Snapyr.setSingletonInstance(snapyr.build());
                    Log.d("SnapyrFlappy", "Snapyr SDK initialized.")
                }

                return ourInstance!!
            }
        }
    }

    private fun userInAppCallback(message: InAppMessage) {
        Log.println(
                Log.INFO,
                "SnapyrFlappy",
                """inapp cb triggered: 
	${message.Timestamp}
	${message.ActionType}
	        ${message.UserId}
	${message.ActionToken}
	${message.Content}
	""")
        inappListeners.forEach { entry ->
            entry.value.onAction(message)
        }
//        flappyBird.onInAppMessage(message)
    }

    fun registerInAppListener(key: String, listener: InAppCallback) {
        inappListeners.set(key, listener)
    }

    fun deregisterInAppListener(key: String) {
        inappListeners.remove(key);
    }

    internal fun onDoReset() {
        Snapyr.with(context).reset()
    }


    internal fun onDoTrack() {
        Log.d("SnapyrFlappy", "onDoTrack: Track tapped")
        val prefs1 = PreferenceManager.getDefaultSharedPreferences(context)
        if (!prefs1.getBoolean("register", false)) {
            Snapyr.with(context).track("register1")
            val editor = prefs1.edit()
            editor.putBoolean("register", true)
            editor.commit()
        }
    }

    internal fun yourScore(scoreNumber: Int) {
        Log.d("SnapyrFlappy", "onDoTrack: Track tapped")
        Snapyr.with(context).track("score", Properties().putValue("total", scoreNumber));
    }

    internal fun onDoFlush() {
        Log.d("SnapyrFlappy", "onDoFlush: Flush tapped")
        Snapyr.with(context).flush()
    }

}
