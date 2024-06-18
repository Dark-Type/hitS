package com.example.hits.utility

import android.graphics.Bitmap
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtEnvironment
import android.content.Context
import android.util.Log
import com.example.hits.R
import java.io.InputStream

class NeuralNetwork(private val context: Context) {
    private var image: Bitmap ? = null
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession

    fun detect() {
        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        ortSession = ortEnv.createSession(readModel(), sessionOptions)


            performObjectDetection(ortSession, readInputImage())
            Log.d("ObjectDetection", "Successful ObjectDetection")
    }

    private fun performObjectDetection(
        ortSession: OrtSession,
        inputStream: InputStream
    ) {
        var objDetector = ObjectDetector()
        var imageStream = inputStream
        imageStream.reset()
        var result = objDetector.detect(imageStream, ortEnv, ortSession)

        // Вывод боксов в консоль для проверки
        val boxit = result.iterator()
        while(boxit.hasNext()) {
            var box_info = boxit.next()
            Log.d("ObjectDetection", (box_info[0] - box_info[2] / 2).toString())
            Log.d("ObjectDetection", (box_info[1] - box_info[3] / 2).toString())
        }
    }

    private fun readModel(): ByteArray {
        val modelID = R.raw.ssd_onnx
        return context.resources.openRawResource(modelID).readBytes()
    }

    private fun readInputImage(): InputStream {
        return context.assets.open("test_image.jpg")
    }

    fun putImage(image: Bitmap) {
        this.image = image
    }

    fun predictIfHit(): Boolean {
        return true
    }
}