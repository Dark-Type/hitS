package com.example.hits.utility

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.hits.R
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class NeuralNetwork(private val context: Context) {
    private var image: Bitmap ? = null
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession

    fun detect() {
        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        ortSession = ortEnv.createSession(readModel(), sessionOptions)

        try {
            performObjectDetection(ortSession, readInputImage())
            Log.d("ObjectDetection", "Successful ObjectDetection")
        } catch (e: Exception) {
            Log.d("ObjectDetection", "Error in ObjectDetection ${Exception(e)}")
        }
    }

    private fun assetFilePath(context: Context, asset: String): String {
        val file = File(context.filesDir, asset)
        try {
            val inpStream: InputStream = context.assets.open(asset)
            try {
                val outStream = FileOutputStream(file, false)
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (true) {
                    read = inpStream.read(buffer)
                    if (read == -1) {
                        break
                    }
                    outStream.write(buffer, 0, read)
                }
                outStream.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /*
    fun encode(bitmap: Bitmap) {
        val imageStream = context.assets.open("test_image.jpg")
        val bitmap = BitmapFactory.decodeStream(imageStream)
        val module = Module.load(assetFilePath(context, "model.ptl"))

        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        val output = module.forward(IValue.from(inputTensor)).toTensor().dataAsFloatArray
        println(output[0])
    }
    */

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