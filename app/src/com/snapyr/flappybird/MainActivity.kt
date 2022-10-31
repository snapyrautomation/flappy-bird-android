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

import android.R
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.github.kostasdrakonakis.annotation.Intent


@Intent
class MainActivity : AndroidApplication() {
    var collisionsEnabled = false
    var startingScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if(intent != null) {
            val currentIntent: Uri? = intent.data;
            handleOpenIntent(currentIntent)
        }

        initialize(FlappyBird(this, collisionsEnabled, startingScore), AndroidApplicationConfiguration())
    }

    private fun handleOpenIntent(data: Uri?) {
        if (data == null) {
            return
        }
        val isCorrect = data.getQueryParameter("correct")
        Log.d("SnapyrFlappy", "isCorrect: " + isCorrect.toString())
        if (isCorrect != null && isCorrect != "") {
            if(isCorrect == "true")
                correct()
            else if (isCorrect == "false"){
                wrong()
            } else {
                issue()
            }
        }

        val collisionsEnabled = data.getQueryParameter("collisionsEnabled")
        if (collisionsEnabled != null && collisionsEnabled != "") {
            this.collisionsEnabled = collisionsEnabled.toBoolean()
            if (this.collisionsEnabled) {
                AlertDialog.Builder(context)
                        .setTitle("Watch Out!")
                        .setMessage("We're giving you 1,000 points as a reward!\n\nBut collisions are enabled now, so it'll be a lot harder!")
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton("Got it!", null)
                        .show()
            } else {
                AlertDialog.Builder(context)
                        .setTitle("Have a Hand!")
                        .setMessage("Looks like you could use some help, so we made this round a little easier!")
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton("Got it!", null)
                        .show()
            }
        }

        val startingScore = data.getQueryParameter("startingScore")
        if (startingScore != null && startingScore != "") {
            this.startingScore = startingScore.toInt()
        }
    }

    private fun correct() {
        AlertDialog.Builder(context)
            .setTitle("Congratulations!")
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton(
                R.string.yes, null)
            .setNegativeButton(R.string.no, null)
            .setView(com.snapyr.flappybird.R.layout.alert_view)
            .show()
    }

    private fun wrong() {
        AlertDialog.Builder(context)
            .setTitle("Sorry!")
            .setPositiveButton(
                R.string.yes, null)
            .setNegativeButton(R.string.no, null)
            .setView(com.snapyr.flappybird.R.layout.wrong_alert_view)
            .show()
    }


    private fun issue() {
        AlertDialog.Builder(context)
            .setTitle("Wrong deeplink!")
            .setPositiveButton(
                R.string.yes, null)
            .setNegativeButton(R.string.no, null)
            .setView(com.snapyr.flappybird.R.layout.wrong_alert_view)
            .show()
    }
}
