package com.example.hits.utility

import ai.onnxruntime.OnnxTensor
import android.content.Context
import android.graphics.Bitmap
import com.example.hits.R
import java.io.InputStream
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.Result
import java.nio.FloatBuffer
import java.util.Collections

class NeuralNetwork private constructor(context: Context) {

    private val applicationContext = context.applicationContext
    private var ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var detectorOrtSession: OrtSession
    private var autoencoderOrtSession: OrtSession

    companion object {
        @Volatile
        private var INSTANCE: NeuralNetwork? = null

        fun getInstance(context: Context): NeuralNetwork =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: NeuralNetwork(context).also {
                    INSTANCE = it
                }
            }
    }


    fun rememberPerson(roomID: Int, userToScanID: Int, image: Bitmap) {

        val embedding = encode(image)

        addEmbeddingToDatabase(roomID, userToScanID, embedding)

        /*
        тут нужно положить id и соответствующий ей эмбеддинг в бд
        может быть несколько, но не меньше 4 эмбеддингов для одного id
        */
    }

    fun recognizePerson(roomID: Int, image: Bitmap): Int {
        val embedding = encode(image)

        // Логика получения из бд массива пар <Эмбеддинг, id>
        val embeddings: Array<Pair<FloatArray, Int>> = getEmbeddings(roomID)

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

    private fun scaleBoundingBox(
        width: Int,
        height: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): Array<Int> {
        return arrayOf(
            left * width / 500,
            top * height / 500,
            right * width / 500,
            bottom * height / 500
        )
    }

    fun detect(bitmap: Bitmap) {
        val resizedBitmap = bitmap.let { Bitmap.createScaledBitmap(it, 500, 500, false) }
        val input = preprocessImage(resizedBitmap, 500, 500)

        val shape = longArrayOf(1, 3, 500, 500)

        val inputTensor = OnnxTensor.createTensor(
            ortEnvironment,
            input,
            shape
        )

        val output = detectorOrtSession.run(
            Collections.singletonMap("input", inputTensor),
            setOf("boxes", "scores", "labels")
        ) as Result

        val result = (output.get(0)?.value) as Array<FloatArray>

        // Вывод боксов в консоль для проверки
        val boxit = result.iterator()

        println("Кол-во боксов: ")
        println(result.size)

        // bb in (xmin, ymin, xmax, ymax) format

        while(boxit.hasNext()) {
            var box_info = boxit.next()
            println("ща будут боксы детектора: ")

            val scaledBoundingBoxes = scaleBoundingBox(
                bitmap.width,
                bitmap.height,
                box_info[0].toInt(),
                box_info[1].toInt(),
                box_info[2].toInt(),
                box_info[3].toInt(),
            )

            println(scaledBoundingBoxes[0])
            println(scaledBoundingBoxes[1])
            println(scaledBoundingBoxes[2])
            println(scaledBoundingBoxes[3])
        }
    }

    // Resize to width x height + bitmap to floatArray
    fun preprocessImage(
        bitmap: Bitmap,
        width: Int,
        height: Int
    ): FloatBuffer {
        val imgData = FloatBuffer.allocate(1 * 3 * width * height)
        imgData.rewind()
        val stride = width * height
        val bmpData = IntArray(stride)
        bitmap.getPixels(bmpData, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in 0..<width) {
            for (j in 0..<height) {
                val idx = height * i + j
                val pixelValue = bmpData[idx]
                imgData.put(idx, (((pixelValue shr 16 and 0xFF) / 255f - 0.485f) / 0.229f))
                imgData.put(idx + stride, (((pixelValue shr 8 and 0xFF) / 255f - 0.456f) / 0.224f))
                imgData.put(idx + stride * 2, (((pixelValue and 0xFF) / 255f - 0.406f) / 0.225f))
            }
        }

        imgData.rewind()
        return imgData
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

    fun encode(bitmap: Bitmap): FloatArray {
        /*
        Переводит картинку в эмбеддинг
        */
        // Предобработка изображения
        val resizedBitmap = bitmap.let { Bitmap.createScaledBitmap(it, 256, 256, false) }
        val input = preprocessImage(resizedBitmap, 256, 256)

        val shape = longArrayOf(1, 3, 256, 256)

        val inputTensor = OnnxTensor.createTensor(
            ortEnvironment,
            input,
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
        return applicationContext.resources.openRawResource(modelID).readBytes()
    }

    private fun readAutoencoder(): ByteArray {
        val modelID = R.raw.autoencoder_quant
        return applicationContext.resources.openRawResource(modelID).readBytes()
    }

    fun readInputImage(): InputStream {
        return applicationContext.assets.open("test_image.jpg")
    }

    fun predictIfHit(image: Bitmap): Int {
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