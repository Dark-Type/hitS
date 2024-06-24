package com.example.hits.utility

import ai.onnxruntime.OnnxTensor
import android.content.Context
import android.graphics.Bitmap
import com.example.hits.R
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.Result
import android.graphics.Canvas
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import java.util.Collections

class NeuralNetwork private constructor(context: Context) {
    private val applicationContext = context.applicationContext
    private var ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var detectorOrtSession: OrtSession
    private var autoencoderOrtSession: OrtSession
    private var embeddings: Array<Pair<FloatArray, Int>> = emptyArray()

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


    // Положить эмбеддинг человека в бд
    suspend fun rememberPerson(roomID: Int, userToScanID: Int, image: Bitmap) {
        val embedding = encode(image)
        addEmbeddingToDatabase(roomID, userToScanID, embedding)
    }

    fun embeddingsSetter(passedEmbeddings: Array<Pair<FloatArray, Int>>) {
        embeddings = passedEmbeddings
    }

    // Распознать человека по фото
    private suspend fun recognizePerson(image: Bitmap): Int {
        val embedding = encode(image)

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

    // Сравнить два эмбеддинга с помощью cosine similarity
    private fun getSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
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

    /*
    Отшкалировать bb из prediction модели для
    предобработанного изображения в формат изначального
    */
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

    // Найти людей на фото
    private suspend fun detect(bitmap: Bitmap): ArrayList<Array<Int>> {
        val input = preprocessImage(bitmap, 500, 500)
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

        val boxIt = result.iterator()

        // bounding boxes in (xmin, ymin, xmax, ymax) format
        val boundingBoxes = ArrayList<Array<Int>>()

        while (boxIt.hasNext()) {
            val box = boxIt.next()

            val scaledBoundingBoxes = scaleBoundingBox(
                bitmap.width,
                bitmap.height,
                box[0].toInt(),
                box[1].toInt(),
                box[2].toInt(),
                box[3].toInt()
            )

            boundingBoxes.add(scaledBoundingBoxes)
        }

        return boundingBoxes
    }

    private suspend fun preprocessImage(
        bitmap: Bitmap,
        width: Int,
        height: Int
    ): FloatBuffer = withContext(Dispatchers.Default) {
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            width,
            height,
            false
        )
        val imgData = FloatBuffer.allocate(3 * width * height)
        val stride = width * height
        val bmpData = IntArray(stride)

        resizedBitmap.getPixels(
            bmpData,
            0,
            resizedBitmap.width,
            0,
            0,
            resizedBitmap.width,
            resizedBitmap.height
        )

        val jobs = List(width) { i ->
            launch {
                for (j in 0 until height) {
                    val idx = height * i + j
                    val pixelValue = bmpData[idx]
                    synchronized(imgData) {
                        imgData.put(
                            idx,
                            (((pixelValue shr 16 and 0xFF) / 255f - 0.485f) / 0.229f)
                        )
                        imgData.put(
                            idx + stride,
                            (((pixelValue shr 8 and 0xFF) / 255f - 0.456f) / 0.224f)
                        )
                        imgData.put(
                            idx + stride * 2,
                            (((pixelValue and 0xFF) / 255f - 0.406f) / 0.225f)
                        )
                    }
                }
            }
        }

        jobs.joinAll()

        imgData.rewind()
        imgData
    }

    // "Выравнивание" тензора до размерности 1
    private fun flatten(array: Array<FloatArray>): FloatArray {
        val size = array.sumOf { it.size }
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

    /*
    Переводит картинку в эмбеддинг
    */
    private suspend fun encode(bitmap: Bitmap): FloatArray {
        val resizedBitmap = bitmap.let {
            Bitmap.createScaledBitmap(
                it,
                256,
                256,
                false
            )
        }
        val input = preprocessImage(
            resizedBitmap,
            256,
            256
        )

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

        val latentCode = (output.get(1)?.value) as Array<FloatArray>

        return flatten(latentCode)
    }

    private fun readSsd(): ByteArray {
        val modelID = R.raw.ssd_onnx
        return applicationContext.resources.openRawResource(modelID).readBytes()
    }

    private fun readAutoencoder(): ByteArray {
        val modelID = R.raw.autoencoder_quant
        return applicationContext.resources.openRawResource(modelID).readBytes()
    }


    private fun isPointInRectangle(
        pointX: Int,
        pointY: Int,
        xMin: Int,
        yMin: Int,
        xMax: Int,
        yMax: Int
    ): Boolean {
        return pointX in xMin..xMax && pointY in yMin..yMax
    }

    // Обрезка bitmap по координатам bb
    private fun cropBitmap(
        bitmap: Bitmap,
        xMin: Int,
        yMin: Int,
        xMax: Int,
        yMax: Int
    ): Bitmap {
        val width = xMax - xMin
        val height = yMax - yMin
        val croppedBitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(croppedBitmap)
        val srcRect = Rect(xMin, yMin, xMax, yMax)
        val destRect = Rect(0, 0, width, height)

        canvas.drawBitmap(bitmap, srcRect, destRect, null)

        return croppedBitmap
    }

    /*
    Возвращает id игрока, в которого попал игрок, или null,
    если попадания не было
     */
    suspend fun predictIfHit(image: Bitmap): Int? = withContext(Dispatchers.Default) {
        val aimCoordinates = arrayOf(
            image.width / 2,
            image.height / 2
        )

        val persons = detect(image)

        for (person in persons) {
            // Если прицел внутри bb
            if (
                isPointInRectangle(
                    aimCoordinates[0],
                    aimCoordinates[1],
                    person[0],
                    person[1],
                    person[2],
                    person[3]
                )
            ) {
                // Обрезка bitmap по координатам bb
                val croppedPersonImage = cropBitmap(
                    image,
                    person[0],
                    person[1],
                    person[2],
                    person[3]
                )

                return@withContext recognizePerson(croppedPersonImage)
            }
        }

        return@withContext null
    }

    init {
        // Загрузка моделей в ort сессии
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