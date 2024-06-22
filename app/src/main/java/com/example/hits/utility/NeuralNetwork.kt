package com.example.hits.utility

import ai.onnxruntime.OnnxTensor
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.hits.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.Result
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.Collections

class NeuralNetwork(private val context: Context) {
    private var image: Bitmap ? = null
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession

    fun detect() {
        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        ortSession = ortEnv.createSession(readSsd(), sessionOptions)

        try {
            performObjectDetection(ortSession, readInputImage())
            Log.d("ObjectDetection", "Successful ObjectDetection")
        } catch (e: Exception) {
            Log.d("ObjectDetection", "Error in ObjectDetection ${Exception(e)}")
        }
    }

    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    // Resize to 256 x 256 + bitmap to bytebuffer
    private fun preprocessImage(bitmap: Bitmap): ByteArray {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        val byteBuffer = ByteBuffer.allocate(3 * resizedBitmap.width * resizedBitmap.height)

        for (y in 0 until resizedBitmap.height) {
            for (x in 0 until resizedBitmap.width) {
                val pixel = resizedBitmap.getPixel(x, y)
                byteBuffer.put((pixel shr 16 and 0xFF).toByte()) // Red
                byteBuffer.put((pixel shr 8 and 0xFF).toByte()) // Green
                byteBuffer.put((pixel and 0xFF).toByte()) // Blue
            }
        }

        return byteBuffer.array()
    }

    fun encode(inputBitmap: Bitmap): Array<FloatArray> {
        val environment = OrtEnvironment.getEnvironment()
        val session = environment.createSession(
            readAutoencoder(),
            OrtSession.SessionOptions()
        )

        // Предобработка изображения
        val input = preprocessImage(inputBitmap)

        val floatImageBytes = input.map { it.toFloat() / 255.0f }.toFloatArray()
        val shape = longArrayOf(1, 3, 256, 256)

        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            FloatBuffer.wrap(floatImageBytes),
            shape
        )

        val output = session.run(
            Collections.singletonMap("input_image", inputTensor),
            setOf("reconstructed", "latent_code")
        ) as Result

        val latent_code = ((output.get(1)?.value) as Array<FloatArray>)

        return latent_code
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

    private fun readSsd(): ByteArray {
        val modelID = R.raw.ssd_onnx
        return context.resources.openRawResource(modelID).readBytes()
    }

    private fun readAutoencoder(): ByteArray {
        val modelID = R.raw.autoencoder_quant
        return context.resources.openRawResource(modelID).readBytes()
    }

    fun readInputImage(): InputStream {
        return context.assets.open("test_image.jpg")
    }

    fun putImage(image: Bitmap) {
        this.image = image
    }

    fun predictIfHit(): Int {
        return 0
    }
}