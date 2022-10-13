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

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.github.kostasdrakonakis.androidnavigator.IntentNavigator
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : Activity() {
    private var snapyrInitialized = false

    private lateinit var identifyUserId: EditText
    private lateinit var identifyKey: EditText
    private lateinit var identifyEmail: EditText
    private lateinit var identifyName: EditText
    private lateinit var identifyPhone: EditText

    fun initializeSnapyr() {
        if (snapyrInitialized) {
            Toast.makeText(this, "Snapyr already initialized; restart app to initialize again", Toast.LENGTH_LONG).show()
            return
        }
        var snapyrData: SnapyrData = SnapyrData.instance
        snapyrData.identifyUserId=identifyUserId.text.toString()
        snapyrData.identifyEmail=identifyEmail.text.toString()
        snapyrData.identifyKey=identifyKey.text.toString()
        snapyrData.identifyName=identifyName.text.toString()
        snapyrData.identifyPhone=identifyPhone.text.toString()

        var snapyr = SnapyrComponent.build(this.applicationContext)
        snapyr.onDoReset()
        snapyr.onDoIdentify()

        snapyrInitialized = true

        // disable inputs now that we've initialized
        identifyUserId.isEnabled = false
        identifyKey.isEnabled = false
        identifyEmail.isEnabled = false
        identifyName.isEnabled = false
        identifyPhone.isEnabled = false
        env.isEnabled = false
        initSnapyrButton.isEnabled = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        identifyUserId = findViewById<EditText>(R.id.identify_userid);
        //        identifyUserId.setText("alina3")
        identifyKey = findViewById<EditText>(R.id.identify_key);
        identifyKey.setText("HheJr6JJGowjvMvJGq9FqunE0h8EKAIG")
        identifyEmail = findViewById<EditText>(R.id.identify_email);
        //        identifyEmail.setText("alina@snapyr.com")
        identifyName = findViewById<EditText>(R.id.identify_name);
        //        identifyName.setText("alina3")
        identifyPhone = findViewById<EditText>(R.id.identify_phone);

        var snapyrData: SnapyrData = SnapyrData.instance;
        env.text = "Env: " + snapyrData.env

        initSnapyrButton.setOnClickListener {
            initializeSnapyr()
        }

        playButton.setOnClickListener {
            if (!snapyrInitialized) {
                initializeSnapyr()
            }
            IntentNavigator.startMainActivity(this)
        }

        env.setOnClickListener{
            val builder = AlertDialog.Builder(this)
                .setTitle("Choose Env!")
            builder.setPositiveButton("dev") { dialog, which ->
                snapyrData.env = "dev"
                identifyKey.setText("38bT1SbGJ0A12CJqk8DFRzypJnIylRmg")
                env.text = "Env: dev"

            }
            builder.setNeutralButton("prod") { dialog, which ->
                snapyrData.env = "prod"
                identifyKey.setText("HheJr6JJGowjvMvJGq9FqunE0h8EKAIG")
                env.text = "Env: prod"
            }
            builder.setNegativeButton("stg") { dialog, which ->
                snapyrData.env = "stg"
                identifyKey.setText("kuxCvTgQdcXAgNjrhrMP2U46VIhUi6Wz")
                env.text = "Env: stg"
            }
            builder.show()
        }
    }
}