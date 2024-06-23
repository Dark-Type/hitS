package com.example.hits.utility

import android.graphics.Bitmap

class NeuralNetwork {
    private var image:Bitmap? = null
    fun putImage(image: Bitmap) {
        this.image = image
    }
    fun predictIfHit(image: Bitmap): Int {
        val hit = true
        val playerId =10
        return if (hit)playerId else -1
    }
}