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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.github.kostasdrakonakis.androidnavigator.IntentNavigator
import com.snapyr.sdk.Properties
import com.snapyr.sdk.Snapyr
import com.snapyr.sdk.Traits
import com.snapyr.sdk.inapp.InAppActionType
import com.snapyr.sdk.inapp.InAppCallback
import com.snapyr.sdk.inapp.InAppMessage
import com.snapyr.sdk.inapp.InAppPayloadType
import kotlinx.android.synthetic.main.activity_splash.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone


class SplashActivity : DebugActivityBase(), InAppCallback {
    private var snapyrInitialized = false
    var snapyrData: SnapyrData = SnapyrData.instance

    var currentInAppMessage: InAppMessage? = null
    var currentMessageInteracted: Boolean = false

    private lateinit var identifyUserId: EditText
    private lateinit var identifyKey: EditText
    private lateinit var identifyEmail: EditText
    private lateinit var identifyName: EditText
    private lateinit var identifyPhone: EditText

    private fun doIdentify() {
        if (identifyUserId.text.toString() != snapyrData.identifyUserId) {
            SnapyrComponent.instance.onDoReset()
        }

        snapyrData.identifyUserId = identifyUserId.text.toString()
        snapyrData.identifyEmail = identifyEmail.text.toString()
        snapyrData.identifyName = identifyName.text.toString()
        snapyrData.identifyPhone = identifyPhone.text.toString()

        this.identifyAndLog(
            snapyrData.identifyUserId, Traits()
                .putName(snapyrData.identifyName)
                .putEmail(snapyrData.identifyEmail)
                .putPhone(snapyrData.identifyPhone)
                .putValue("games_played", 0)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        SnapyrComponent.instance.registerInAppListener("splash", this)

        identifyKey = findViewById<EditText>(R.id.identify_key);
        identifyUserId = findViewById<EditText>(R.id.identify_userid);

        identifyKey.setText(snapyrData.sdkWriteKey)
        identifyUserId.setText(snapyrData.identifyUserId)

        identifyEmail = findViewById<EditText>(R.id.identify_email);
        identifyEmail.setText(snapyrData.identifyEmail)

        identifyName = findViewById<EditText>(R.id.identify_name);
        identifyName.setText(snapyrData.identifyName)

        identifyPhone = findViewById<EditText>(R.id.identify_phone);
        identifyPhone.setText(snapyrData.identifyPhone);

        current_env.setText("ENV: " + snapyrData.env)

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        formatter.timeZone = TimeZone.getTimeZone("Etc/UTC");

        var buildDateStr = "Unknown date"
        try {
            // hack - we want `com.snapyr.sdk.core.BuildConfig.BUILD_DATE` but it only exists on newer SDK builds. Try getting it by reflection; otherwise label will show "unknown"
            val buildDate =
                com.snapyr.sdk.core.BuildConfig::class.java.getDeclaredField("BUILD_DATE").get(null) as Date
            buildDateStr = formatter.format(buildDate)
        } catch (e: Exception) {}

        snapyr_version_label.text =
            "Snapyr SDK: ${com.snapyr.sdk.core.BuildConfig.VERSION_NAME} ($buildDateStr)"

        doIdentifyButton.setOnClickListener {
            this.hideKeyboard()
            this.doIdentify()
        }

        playButton.setOnClickListener {
            IntentNavigator.startMainActivity(this)
        }

        changeEnvOrKey.setOnClickListener{
            onChangeEnvOrKey()
        }

        playerStinksButton.setOnClickListener {
            onPlayerStinksClick(it)
        }

        reachedVipButton.setOnClickListener {
            onReachedVipClick(it)
        }

        pushtest_bad_url.setOnClickListener {
            onBadUrlClick()
        }

        pushtest_leaderboard.setOnClickListener {
            onLeaderboardClick()
        }

        pushtest_homescreen.setOnClickListener {
            onHomescreenClick()
        }

        pushtest_no_deeplink.setOnClickListener {
            onNoDeeplinkClick()
        }

        overlayTest.setOnClickListener {
            trackAndLog("overlayTest")
        }

        longTextNotif.setOnClickListener {
            trackAndLog("longTextNotif")
        }

        setupWebView()
    }

    private fun onChangeEnvOrKey() {
        val alertView = layoutInflater.inflate(R.layout.alert_writekey_env, null)
        // Multiple dialog opens reuse the same inflated view - it needs to be removed from its parent before subsequent display or it will crash
        if (alertView.parent != null) {
            (alertView.parent as ViewGroup).removeView(alertView)
        }

        val writeKeyInput = alertView.findViewById<EditText>(R.id.alert_sdk_writekey)
        val envLabel = alertView.findViewById<TextView>(R.id.alert_env)
        val devButton = alertView.findViewById<Button>(R.id.alert_env_dev)
        val stageButton = alertView.findViewById<Button>(R.id.alert_env_stage)
        val prodButton = alertView.findViewById<Button>(R.id.alert_env_prod)
        var selectedEnv = snapyrData.env

        writeKeyInput.setText(snapyrData.sdkWriteKey)
        envLabel.text = "Env: " + selectedEnv

        devButton.setOnClickListener {
            selectedEnv = "dev"
            writeKeyInput.setText(SnapyrData.DEFAULT_WRITE_KEY_DEV)
            envLabel.text = "Env: dev"
        }
        stageButton.setOnClickListener {
            selectedEnv = "stg"
            writeKeyInput.setText(SnapyrData.DEFAULT_WRITE_KEY_STAGE)
            envLabel.text = "Env: stg"
        }
        prodButton.setOnClickListener {
            selectedEnv = "prod"
            writeKeyInput.setText(SnapyrData.DEFAULT_WRITE_KEY_PROD)
            envLabel.text = "Env: prod"
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("SDK Write Key and Environment")
        builder.setView(alertView)

        builder.setPositiveButton(android.R.string.ok) { dialog, which ->
            val isChanged = (selectedEnv != snapyrData.env || writeKeyInput.text.toString() != snapyrData.sdkWriteKey)
            if (!isChanged) {
                return@setPositiveButton
            }
            snapyrData.needsReset = true
            snapyrData.env = selectedEnv
            changeEnvOrKey.text = "Env: " + selectedEnv
            snapyrData.sdkWriteKey = writeKeyInput.text.toString()
            identifyKey.setText(writeKeyInput.text.toString())
            showRestartRequiredAlert()
        }
        .setNegativeButton(android.R.string.cancel) { _, _ -> }

        val dialog = builder.create()
        // Make the dialog full screen width
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window!!.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.show()
        dialog.window!!.attributes = layoutParams
    }

    private fun showRestartRequiredAlert() {
        AlertDialog.Builder(this)
            .setTitle("Restart Snappy Bird to apply changes")
            .setMessage("Your settings will be applied the next time you run the app. Please force quit and restart Snappy Bird.")
            .setPositiveButton("Restart now") { _, _ ->
                triggerRestart()
            }
            .setOnDismissListener {
                triggerRestart()
            }
            .show()
    }

    fun triggerRestart() {
        // Force restart of the app, back to this activity
        val intent = Intent(applicationContext, this.javaClass)
        this.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupWebView() {
        val client = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.getUrl().toString()
                if (currentInAppMessage != null) {
                    Snapyr.with(this@SplashActivity).trackInAppMessageClick(currentInAppMessage!!.ActionToken, Properties().putValue("url", url))
                }

                // Overriding `shouldOverrideUrlLoading` lets us intercept the URL when clicked, but breaks deep links.
                // Following code makes them work again
                // https://stackoverflow.com/a/53059413
                return if (url == null || url.startsWith("http://") || url.startsWith("https://")) false else try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    view.context.startActivity(intent)
                    true
                } catch (e: java.lang.Exception) {
                    Log.e("SnapyrFlappy", "shouldOverrideUrlLoading Exception: $e")
                    true
                }

            }
        }
        topBanner.webViewClient = client
        topBanner.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Any tap on the webview (even if it didn't trigger a link)
                currentMessageInteracted = true
                if (currentInAppMessage != null) {
                    Snapyr.with(this@SplashActivity).trackInAppMessageClick(currentInAppMessage!!.ActionToken)
                }
            }
            false
        }
    }

    private fun dismissInAppWebview() {
        try {
            if (SnapyrComponent.hasInstance) {
                // Intention for deregistering here was to prevent in-app callbacks from running in this Activity after
                // user has switched to another Activity (the game). That doesn't seem to be necessary, as the OS suspends
                // this activity and the callback doesn't run. If the user goes back to this activity, the callbacks start
                // working again.
                //            snapyr.deregisterInAppListener("splash")
                // We don't give the user a direct way to dismiss custom HTML message in this app, but treat as dismiss if they close this
                // screen without interacting with the webview
                if (currentInAppMessage != null && !currentMessageInteracted) {
                    Snapyr.with(this).trackInAppMessageDismiss(currentInAppMessage!!.ActionToken)
                }
            }
        } catch (e: Exception) {
            Log.e("SnapyrFlappy", "Caught error:", e)
        } finally {
            currentInAppMessage = null
            currentMessageInteracted = false
            // effectively unloads/clears the webview
            topBanner.loadUrl("about:blank")
        }
    }

    override fun onStop() {
        super.onStop()
        dismissInAppWebview()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissInAppWebview()
    }

    private fun onBadUrlClick() {
        trackAndLog("birdsPushTestBadUrl")
    }

    private fun onLeaderboardClick() {
        trackAndLog("birdsPushTestLeaderboard")
    }

    private fun onHomescreenClick() {
        trackAndLog("birdsPushTestHomescreen")
    }

    private fun onNoDeeplinkClick() {
        trackAndLog("birdsPushTestNoDeeplink")
    }

    private fun onPlayerStinksClick(v: View) {
        trackAndLog("userStinks")
    }

    private fun onReachedVipClick(v: View) {
        trackAndLog("userRules")
    }

    override fun onAction(message: InAppMessage) {
        if (message.ActionType != InAppActionType.ACTION_TYPE_CUSTOM) {
            return
        }
        if (message.Content.type == InAppPayloadType.PAYLOAD_TYPE_HTML) {
            // keep track of it so we can read back properties like actionToken later
            currentInAppMessage = message
            currentMessageInteracted = false
            runOnUiThread {
                // Neither Brandon nor I know why this needs to be base64 but w/e
                val encodedHtml = Base64.encodeToString(message.Content.htmlPayload.toByteArray(), Base64.NO_PADDING)
                topBanner.loadData(encodedHtml, "text/html", "base64")
                Snapyr.with(this).trackInAppMessageImpression(message.ActionToken);
            }
        }

        Log.d("SnapyrFlappy", "onAction: " + message.asValueMap().toString());
    }
}