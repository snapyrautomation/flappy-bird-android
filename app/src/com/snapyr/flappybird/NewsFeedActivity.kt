package com.snapyr.flappybird

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_newsfeed.*

class NewsFeedActivity : DebugActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_newsfeed)


        action_track_a_newsfeed.setOnClickListener {
            onButtonAClicked(it)
        }
    }

    private fun onButtonAClicked(v: View) {
        trackAndLog("birdsPushTestHomescreen")
    }
}
