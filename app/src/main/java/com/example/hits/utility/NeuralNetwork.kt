package com.example.hits.utility

import ai.onnxruntime.*
import com.example.hits.R
import android.graphics.Bitmap
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtEnvironment
import android.graphics.BitmapFactory

class NeuralNetwork {
    private var image: Bitmap ? = null
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession

    private fun performObjectDetection(ortSession: OrtSession) {
        var objDetector = ObjectDetector()
        var imagestream = readInputImage()
        image.setImageBitmap(
            BitmapFactory.decodeStream(imagestream)
        );
        imagestream.reset()
        var result = objDetector.detect(imagestream, ortEnv, ortSession)
    }

    private fun readModel(): ByteArray {
        val modelID = R.raw.ssd_onnx
        return resources.openRawResource(modelID).readBytes()
    }

    fun putImage(image: Bitmap) {
        this.image = image
    }

    fun predictIfHit(): Boolean {
        return true
    }
}