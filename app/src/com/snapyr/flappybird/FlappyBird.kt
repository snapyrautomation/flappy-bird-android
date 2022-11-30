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
import androidx.core.text.HtmlCompat
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.snapyr.sdk.Snapyr
import com.snapyr.sdk.inapp.InAppActionType
import com.snapyr.sdk.inapp.InAppCallback
import com.snapyr.sdk.inapp.InAppMessage
import com.snapyr.sdk.inapp.InAppPayloadType
import java.util.*

enum class RenderState { RUNNING, PAUSED }

class FlappyBird(private val context: Activity, private var collisionsEnabled: Boolean = false, private var score: Int = 0) : ApplicationAdapter(),
    InAppCallback {

    private var initialCollisionsEnabled = collisionsEnabled

    private lateinit var batch: SpriteBatch
    private lateinit var background: Texture
    private lateinit var gameOver: Texture
    private lateinit var birds: Array<Texture>
    private lateinit var topTubeRectangles: Array<SnapyrRectangle?>
    private lateinit var bottomTubeRectangles: Array<SnapyrRectangle?>
    private lateinit var birdCircle: Circle
    private lateinit var font: BitmapFont
    private lateinit var topTube: Texture
    private lateinit var bottomTube: Texture
    private lateinit var random: Random

    private var renderState = RenderState.RUNNING
    private var flapState = 0
    private var birdY: Float = 0f
    private var velocity: Float = 0f
    private var scoringTube: Int = 0
    private var gameState: Int = 0
    private val numberOfTubes: Int = 4
    private var gdxHeight: Int = 0
    private var gdxWidth: Int = 0
    private var topTubeWidth: Int = 0
    private var bottomTubeWidth: Int = 0
    private var topTubeHeight: Int = 0
    private var bottomTubeHeight: Int = 0

    private val tubeX = FloatArray(numberOfTubes)
    private val tubeOffset = FloatArray(numberOfTubes)
    private var distanceBetweenTubes: Float = 0.toFloat()
    private var snapyr = try { SnapyrComponent.instance } catch (e: Exception) { SnapyrComponent.build(context) }

    override fun onAction(message: InAppMessage) {
        if (message.ActionType != InAppActionType.ACTION_TYPE_CUSTOM || message.Content.type != InAppPayloadType.PAYLOAD_TYPE_JSON) {
            return
        }
        var jsonContent = message.Content.jsonPayload
        if (jsonContent != null) {
            var hiScoreRaw = jsonContent["hiScore"]
            var hiScore = 0.0
            var alertMsgRaw = jsonContent["message"]
            var alertMsg: String = ""

            try {
                alertMsg = alertMsgRaw as String
                if (hiScoreRaw is String) {
                    hiScore = hiScoreRaw.toDoubleOrNull() ?: 0.0
                } else {
                    hiScore = hiScoreRaw as Double
                }
            } catch (e: Exception) {
                // Invalid JSON message - warn and stop
                pauseGame()
                showInvalidJsonDialog()
                Snapyr.with(context).trackInAppMessageImpression(message.ActionToken)
                return
            }

            if (hiScore >= 0) {
                pauseGame()
                val alert = AlertDialog.Builder(context)
                    .setTitle("Hi Score: $hiScore")
                    .setMessage("$alertMsg\n\nNow see how far you get when pipes actually matter!")
                    .setPositiveButton("I'll... try I guess...") { _, _ ->
                        Snapyr.with(context).trackInAppMessageClick(message.ActionToken)
                        collisionsEnabled = true
                        resumeGame()
                    }
                    .setNegativeButton("No way, I admit I suck") { _, _ ->
                        Snapyr.with(context).trackInAppMessageDismiss(message.ActionToken)
                        resumeGame()
                    }
                    .setCancelable(false)

                context.runOnUiThread {
                    alert.show()
                    Snapyr.with(context).trackInAppMessageImpression(message.ActionToken)
                }
            }

        }
    }

    private fun pauseGame() {
        renderState = RenderState.PAUSED
        Gdx.graphics.isContinuousRendering = false
        Gdx.graphics.requestRendering()
    }

    private fun resumeGame() {
        renderState = RenderState.RUNNING
        Gdx.graphics.isContinuousRendering = true
        Gdx.graphics.requestRendering()
    }

    private fun showInvalidJsonDialog() {
        context.runOnUiThread {
            AlertDialog.Builder(context)
                .setTitle("Invalid message")
                .setMessage(
                    HtmlCompat.fromHtml(
                        """
                                Received a custom JSON in-app message from Snapyr, but it did not have 
                                the expected format. Expecting a message like: 
                                <br /><br />
                                <font face="monospace" size="-2">{
                                <br/>&nbsp;&nbsp;"message":[string],
                                <br/>&nbsp;&nbsp;"hiScore":[number]
                                <br />}
                                </font>
                            """.trimIndent(), HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                )
                .setNegativeButton("OK") { _, _ ->
                    resumeGame()
                }
                .show()
        }
    }

    override fun create() {
        batch = SpriteBatch()
        background = Texture("bg.png")
        gameOver = Texture("gameover.png")
        birdCircle = Circle()
        font = BitmapFont()
        font.color = Color.WHITE
        font.data.setScale(10f)

        birds = arrayOf(Texture("bird.png"), Texture("bird2.png"))

        gdxHeight = Gdx.graphics.height
        gdxWidth = Gdx.graphics.width

        topTube = Texture("toptube.png")
        bottomTube = Texture("bottomtube.png")
        random = Random()
        distanceBetweenTubes = gdxWidth * 3f / 4f
        topTubeRectangles = arrayOfNulls(numberOfTubes)
        bottomTubeRectangles = arrayOfNulls(numberOfTubes)

        topTubeWidth = topTube.width
        topTubeHeight = topTube.height
        bottomTubeWidth = bottomTube.width
        bottomTubeHeight = bottomTube.height

        snapyr.registerInAppListener("mainGame", this)

        startGame()
    }

    override fun render() {
        if (renderState == RenderState.PAUSED) {
            return
        }

        batch.begin()
        batch.draw(background, 0f, 0f, gdxWidth.toFloat(), gdxHeight.toFloat())

        if (gameState == 1) {
            if (tubeX[scoringTube] < gdxWidth / 2) {
                score++
                snapyr.yourScore(score)
                if (scoringTube < numberOfTubes - 1) {
                    scoringTube++
                } else {
                    scoringTube = 0
                }
            }

            if (Gdx.input.justTouched()) {
                velocity = -30f
            }

            for (i in 0 until numberOfTubes) {

                if (tubeX[i] < -topTubeWidth) {
                    tubeX[i] += numberOfTubes * distanceBetweenTubes
                    tubeOffset[i] = (random.nextFloat() - 0.5f) * (gdxHeight.toFloat() - GAP - 200f)
                } else {
                    tubeX[i] = tubeX[i] - TUBE_VELOCITY
                }

                batch.draw(topTube, tubeX[i], gdxHeight / 2f + GAP / 2 + tubeOffset[i])
                batch.draw(bottomTube,
                        tubeX[i],
                        gdxHeight / 2f - GAP / 2 - bottomTubeHeight.toFloat() + tubeOffset[i])

                topTubeRectangles[i] = SnapyrRectangle(tubeX[i],
                        gdxHeight / 2f + GAP / 2 + tubeOffset[i],
                        topTubeWidth.toFloat(),
                        topTubeHeight.toFloat(),
                        collisionsEnabled)

                bottomTubeRectangles[i] = SnapyrRectangle(tubeX[i],
                        gdxHeight / 2f - GAP / 2 - bottomTubeHeight.toFloat() + tubeOffset[i],
                        bottomTubeWidth.toFloat(),
                        bottomTubeHeight.toFloat(),
                        collisionsEnabled)
            }

            if (birdY >= 0) {
                velocity += GRAVITY
                birdY -= velocity
            } else {
                if (collisionsEnabled) {
                    gameState = 2
                } else {
                    birdY = 0F
                    velocity = 0F
                }
            }

        } else if (gameState == 0) {
            if (Gdx.input.justTouched()) {
                gameState = 1
            }
        } else if (gameState == 2) {
            batch.draw(gameOver,
                    gdxWidth / 2f - gameOver.width / 2f,
                    gdxHeight / 2f - gameOver.height / 2f)

            if (Gdx.input.justTouched()) {
                collisionsEnabled = initialCollisionsEnabled
                gameState = 1
                startGame()
                score = 0
                scoringTube = 0
                velocity = 0f
            }
        }

        flapState = if (flapState == 0) 1 else 0
        if(score == 100) gameState = 2

        batch.draw(birds[flapState], gdxWidth / 2f - birds[flapState].width / 2f, birdY)
        font.draw(batch, score.toString(), 100f, 200f)
        birdCircle.set(gdxWidth / 2f,
                birdY + birds[flapState].height / 2f,
                birds[flapState].width / 2f)

        for (i in 0 until numberOfTubes) {
            if (Intersector.overlaps(birdCircle, topTubeRectangles[i]) && topTubeRectangles[i]?.collisionsEnabled == true) {
                gameState = 2
            } else if (Intersector.overlaps(birdCircle, topTubeRectangles[i]) && topTubeRectangles[i]?.collisionsEnabled == true) {
                gameState = 2
            }
        }

        batch.end()
    }

    private fun startGame() {
        snapyr.onDoTrack()

        birdY = gdxHeight / 2f - birds[0].height / 2f

        for (i in 0 until numberOfTubes) {
            tubeOffset[i] = (random.nextFloat() - 0.5f) * (gdxHeight.toFloat() - GAP - 200f)
            tubeX[i] = gdxWidth / 2f - topTubeWidth / 2f + gdxWidth.toFloat() + i * distanceBetweenTubes
            topTubeRectangles[i] = SnapyrRectangle(collisionsEnabled)
            bottomTubeRectangles[i] = SnapyrRectangle(collisionsEnabled)
        }
    }

    companion object {
        private const val GRAVITY = 2f
        private const val TUBE_VELOCITY = 4f
        private const val GAP = 800f
    }
}
