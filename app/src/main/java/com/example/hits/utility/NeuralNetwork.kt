package com.example.hits.utility

import android.graphics.Bitmap

class NeuralNetwork {
    private var image:Bitmap? = null
    fun putImage(image: Bitmap) {
        this.image = image
    }
    fun predictIfHit(): Boolean {
        return true
    }
}