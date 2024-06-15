package com.example.hits

import android.graphics.Bitmap

class NeuralNetwork {
    private var image:Bitmap? = null
    fun putImage(image: Bitmap) {
        this.image = image
    }
}