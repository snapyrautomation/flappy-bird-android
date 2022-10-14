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
import android.content.Context
import android.util.Log
import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Rectangle
import com.snapyr.sdk.inapp.InAppMessage
import java.util.*

enum class RenderState { RUNNING, PAUSED }

class FlappyBird(private val context: Context, private var collisionsEnabled: Boolean = false, private var score: Int = 0) : ApplicationAdapter() {

    private var initialCollisionsEnabled = collisionsEnabled

    private lateinit var batch: SpriteBatch
    private lateinit var background: Texture
    private lateinit var gameOver: Texture
    private lateinit var birds: Array<Texture>
    private lateinit var topTubeRectangles: Array<Rectangle?>
    private lateinit var bottomTubeRectangles: Array<Rectangle?>
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

    fun onInAppMessage(message: InAppMessage) {
        var jsonContent = message.Content.jsonContent
        if (jsonContent != null) {
            var hiScore = jsonContent["hiScore"] as Double
            var alertMsg = jsonContent["message"] as String
            if (hiScore >= 0) {
                // We want to pause the game, show an alert saying "we're going to enable
                // collisions", then resume the game after user accepts. But I couldn't figure out
                // how to show an alert in front of the game in time, so just enabling collisions
                collisionsEnabled = true

//                renderState = RenderState.PAUSED
//                Gdx.graphics.isContinuousRendering = false
//                Gdx.graphics.requestRendering()
//                AlertDialog.Builder(context)
//                        .setTitle("Hi Score: $hiScore")
//                        .setMessage("$alertMsg\n\nNow see how far you get when pipes actually matter!")
//                        .setPositiveButton("I'll... try I guess...") { _, _ ->
//                            collisionsEnabled = true
//                            // resume game
//                            renderState = RenderState.RUNNING
//                            Gdx.graphics.isContinuousRendering = true
//                            Gdx.graphics.requestRendering()
//                        }
//                        .setNegativeButton("No way, I admit I suck") { _, _ ->
//                            // resume game
//                            renderState = RenderState.RUNNING
//                            Gdx.graphics.isContinuousRendering = true
//                            Gdx.graphics.requestRendering()
//                        }
//                        .show()
            }

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

                topTubeRectangles[i] = Rectangle(tubeX[i],
                        gdxHeight / 2f + GAP / 2 + tubeOffset[i],
                        topTubeWidth.toFloat(),
                        topTubeHeight.toFloat())

                bottomTubeRectangles[i] = Rectangle(tubeX[i],
                        gdxHeight / 2f - GAP / 2 - bottomTubeHeight.toFloat() + tubeOffset[i],
                        bottomTubeWidth.toFloat(),
                        bottomTubeHeight.toFloat())
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
            if (Intersector.overlaps(birdCircle, topTubeRectangles[i])
                    || Intersector.overlaps(birdCircle, bottomTubeRectangles[i])) {
                gameState = if (collisionsEnabled) 2 else 1
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
            topTubeRectangles[i] = Rectangle()
            bottomTubeRectangles[i] = Rectangle()
        }
    }

    companion object {
        private const val GRAVITY = 2f
        private const val TUBE_VELOCITY = 4f
        private const val GAP = 800f
    }
}
