package com.example.hits.utility

import ai.onnxruntime.OnnxTensor
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.hits.R
import java.io.InputStream
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.Result
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.Collections

class NeuralNetwork(private val context: Context) {
    private var ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var detectorOrtSession: OrtSession
    private var autoencoderOrtSession: OrtSession

    fun rememberPerson(id: Int, image: Bitmap) {
        val embedding = encode(image)

        /*
        тут нужно положить id и соответствующий ей эмбеддинг в бд
        может быть несколько, но не меньше 4 эмбеддингов для одного id
        */
    }

    fun recognizePerson(image: Bitmap): Int {
        val embedding = encode(image)

        // Логика получения из бд массива пар <Эмбеддинг, id>
        val embeddings: Array<Pair<FloatArray, Int>> = arrayOf(Pair(floatArrayOf(), 0))

        var result = embeddings[0].second
        var maxSimilarity = 0.0f
        for (i in embeddings.indices) {
            val similarity = getSimilarity(embedding, embeddings[i].first)
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity
                result = embeddings[i].second
            }
        }

        return result
    }
    fun getSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        /*
        Выдает число в диапазоне [0;1], выражающее
        сходство между эмбеддингами двух картинок
        */
        var dotProduct = 0f
        var magnitude1 = 0f
        var magnitude2 = 0f

        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            magnitude1 += vector1[i] * vector1[i]
            magnitude2 += vector2[i] * vector2[i]
        }

        magnitude1 = kotlin.math.sqrt(magnitude1)
        magnitude2 = kotlin.math.sqrt(magnitude2)

        return dotProduct / (magnitude1 * magnitude2)
    }

    fun detect(bitmap: Bitmap) {
        val input = preprocessImage(bitmap, 500, 500)

        val floatImageBytes = input.map { it.toFloat() }.toFloatArray()

        val shape = longArrayOf(1, 3, 500, 500)

        val inputTensor = OnnxTensor.createTensor(
            ortEnvironment,
            FloatBuffer.wrap(floatImageBytes),
            shape
        )

        val output = detectorOrtSession.run(
            Collections.singletonMap("input", inputTensor),
            setOf("output")
        )

        val result =  (output.get(0)?.value) as Array<FloatArray>

        // Вывод боксов в консоль для проверки
        val boxit = result.iterator()

        println("Кол-во боксов: ")
        println(result.size)

        while(boxit.hasNext()) {
            var box_info = boxit.next()
            println("ща будут боксы детектора: ")
            println(box_info[0] - box_info[2] / 2)
            println(box_info[1] - box_info[3] / 2)
        }
    }

    // Resize to width x height + bitmap to bytearray
    private fun preprocessImage(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
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

    private fun flatten(array: Array<FloatArray>): FloatArray {
        val size = array.sumBy { it.size }
        val result = FloatArray(size)
        var index = 0

        array.forEach { floatArr ->
            floatArr.forEach { value ->
                result[index] = value
                index++
            }
        }

        return result
    }

    fun encode(inputBitmap: Bitmap): FloatArray {
        /*
        Переводит картинку в эмбеддинг
        */
        // Предобработка изображения
        val input = preprocessImage(inputBitmap, 256, 256)

        val floatImageBytes = input.map { it.toFloat() }.toFloatArray()
        val shape = longArrayOf(1, 3, 256, 256)

        val inputTensor = OnnxTensor.createTensor(
            ortEnvironment,
            FloatBuffer.wrap(floatImageBytes),
            shape
        )

        val output = autoencoderOrtSession.run(
            Collections.singletonMap("input_image", inputTensor),
            setOf("reconstructed", "latent_code")
        ) as Result

        val latent_code = ((output.get(1)?.value) as Array<FloatArray>)

        return flatten(latent_code)
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

    fun predictIfHit(): Int {
        return 0
    }

    init {
        detectorOrtSession = ortEnvironment.createSession(
            readSsd(),
            OrtSession.SessionOptions()
        )
        autoencoderOrtSession = ortEnvironment.createSession(
            readAutoencoder(),
            OrtSession.SessionOptions()
        )
    }
}