package com.snapyr.flappybird

import com.badlogic.gdx.math.Rectangle

class SnapyrRectangle: Rectangle {
    var collisionsEnabled = true

    constructor(collisionsEnabled: Boolean): super() {
        this.collisionsEnabled = collisionsEnabled
    }

    constructor(x: Float, y: Float, width: Float, height: Float, collisionsEnabled: Boolean): super(x, y, width, height) {
        this.collisionsEnabled = collisionsEnabled
    }
}