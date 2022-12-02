package com.snapyr.flappybird

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.snapyr.sdk.Properties
import com.snapyr.sdk.Snapyr
import com.snapyr.sdk.Traits
import java.text.MessageFormat
import java.text.SimpleDateFormat
import java.util.*

abstract class DebugActivityBase: Activity() {
    var isNewActivity = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.w("SnapyrFlappy", MessageFormat.format("$localClassName: onCreate: {0}", intent))
    }

    // Wait til onPostCreate for first `addLog` call as it depends on the view being ready, which was probably set up during onCreate
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val intent = intent
        this.addLog("onPostCreate", "intent: $intent")
        intent?.let { handleOpenIntent(it) }
    }

    override fun onStart() {
        super.onStart()
        Log.w("SnapyrFlappy", MessageFormat.format("$localClassName: onStart: {0}", intent))
    }

    override fun onResume() {
        Log.w("SnapyrFlappy", "$localClassName: onResume")
        super.onResume()
        if (!isNewActivity) {
            this.addLog("Activity resumed")
        }
        isNewActivity = false
    }

    override fun onNewIntent(newIntent: Intent) {
        Log.w("SnapyrFlappy", "$localClassName: onNewIntent: $newIntent")
        addLog("onNewIntent", MessageFormat.format("intent: {0}", newIntent))
        handleOpenIntent(newIntent)
        // `onNewIntent` doesn't automatically update the intent on the activity. Do that explicitly
        // so any later activity calls to `getIntent()` get this new version
        this.intent = newIntent
        super.onNewIntent(newIntent)
    }

    fun handleOpenIntent(intent: Intent) {
        val action = intent.action
        val data = intent.data
        if (data == null) {
            Toast.makeText(this, "No deep link info provided", Toast.LENGTH_LONG).show()
            return
        }
        val paths = data.pathSegments
        if (paths.size > 1) {
            val response = paths[0]
            val text = paths[1]
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
            Log.e("SnapyrFlappy", "$localClassName open intent data: $data")
        }
    }

    protected fun identifyAndLog(userId: String, traits: Traits? = null) {
        try {
            Snapyr.with(this).identify(userId, traits, null)
            addLog("Identify sent", userId)
        } catch (e: Exception) {
            Toast.makeText(this, "Error running identify - did you forget to initialize Snapyr?", Toast.LENGTH_SHORT).show()
        }
    }

    protected fun trackAndLog(event: String, props: Properties? = null) {
        try {
            if (Snapyr.with(this).snapyrContext.traits().userId() == null) {
                throw Exception("Must identify first")
            }
            Snapyr.with(this).track(event, props)
            addLog("Track sent", event)
        } catch (e: Exception) {
            Toast.makeText(this, "Error running track - did you forget to identify?", Toast.LENGTH_SHORT).show()
        }
    }

    protected fun addLog(name: String) {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS")
        val date = Date()
        val newEntry =
            Html.fromHtml(String.format("<p>%s: <b>%s</b></p>", formatter.format(date), name))
        prependLogEntry(newEntry)
    }

    protected fun addLog(name: String, description: String) {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS")
        val date = Date()
        val newEntry = Html.fromHtml(
            String.format(
                "<p>%s: <b>%s</b>: %s</p>",
                formatter.format(date), name, description
            )
        )
        prependLogEntry(newEntry)
    }

    protected fun prependLogEntry(newEntry: Spanned) {
        val logView = findViewById<TextView>(R.id.event_log)
        Log.i("SnapyrFlappy", newEntry.toString())
        if (logView == null) {
            Log.e("SnapyrFlappy", "$localClassName: addLog: could not find event_log view")
            return
        }
        // Prepend so the latest entry shows up on top
        val editableText = logView.editableText
        editableText.insert(0, newEntry)
        logView.text = editableText
    }
}