package com.snapyr.flappybird

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_global_leaderboard.*

class GlobalLeaderBoardActivityKt : DebugActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_leaderboard)

        action_track_a_leaderboard.setOnClickListener {
            onButtonAClicked(it)
        }
    }

    private fun onButtonAClicked(v: View) {
        safeTrack("birdsPushTestLeaderboard")
    }
}
