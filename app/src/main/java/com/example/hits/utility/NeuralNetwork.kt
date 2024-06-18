package com.example.hits.utility

import android.graphics.Bitmap
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtEnvironment
import android.util.Log
import java.io.InputStream

class NeuralNetwork {
    private var image: Bitmap ? = null
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession

    fun detect(
        model: ByteArray, // Считать методом readModel() в activity
        inputStream: InputStream
    ) {
        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        ortSession = ortEnv.createSession(model, sessionOptions)

        try {
            performObjectDetection(ortSession, inputStream)
            Log.d("ObjectDetection", "Successful ObjectDetection")
        } catch (e: Exception) {
            Log.d("ObjectDetection", "Error in ObjectDetection")
        }
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
        var boxit = result.outputBox.iterator()
        while(boxit.hasNext()) {
            var box_info = boxit.next()
            Log.d("ObjectDetection", (box_info[0]-box_info[2] / 2).toString())
            Log.d("ObjectDetection", (box_info[1]-box_info[3] / 2).toString())
        }
    }

//    Заюзать внутри activity для загрузки модели
//    private fun readModel(): ByteArray {
//        val modelID = R.raw.ssd_onnx
//        return resources.openRawResource(modelID).readBytes()
//    }

//    Заюзать внутри activity для получения фото из ассета
//    private fun readInputImage(): InputStream {
//        return assets.open("test_image.jpg")
//    }

    fun putImage(image: Bitmap) {
        this.image = image
    }

    fun predictIfHit(): Boolean {
        return true
    }
}