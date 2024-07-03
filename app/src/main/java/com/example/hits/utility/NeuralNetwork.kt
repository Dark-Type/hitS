package com.example.hits.utility

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.Result
import ai.onnxruntime.extensions.OrtxPackage
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import com.example.hits.R
import com.example.hits.SharedPrefHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.Collections

class NeuralNetwork private constructor(context: Context) {
    private val applicationContext = context.applicationContext
    private var ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var detectorOrtSession: OrtSession
    private var autoencoderOrtSession: OrtSession
    private var embeddings: Array<Pair<FloatArray, Int>> = emptyArray()
    val sharedPrefHelper = SharedPrefHelper(applicationContext)

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
    suspend fun rememberPerson(roomID: Int, userToScanID: Int, image: Bitmap): Boolean {
        val boxes = detect(image)
        if (boxes.size == 1) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    applicationContext,
                    "Photo is recognized successfully!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            val croppedImage = cropBitmap(
                image,
                boxes[0][0],
                boxes[0][1],
                boxes[0][2],
                boxes[0][3]
            )

            val embedding = encode(croppedImage)
            addEmbeddingToDatabase(roomID, userToScanID, embedding)
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    applicationContext,
                    "Photo is not recognized, objects on photo detected ${boxes.size}!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return boxes.size == 1
    }

    fun embeddingsSetter(passedEmbeddings: Array<Pair<FloatArray, Int>>) {
        embeddings = passedEmbeddings
    }

    // Распознать человека по фото
    private suspend fun recognizePerson(image: Bitmap): Int {
        Log.d("recognizePerson", "recognizePerson launched")
        val embedding = encode(image)
        Log.d("recognizePerson", "encoded image")
        var result = embeddings[0].second
        var maxSimilarity = 0.0f

        for (embeddingPair in embeddings) {
            if (embeddingPair.second != sharedPrefHelper.getID()!!.toInt()) {
                val similarity = getSimilarity(embedding, embeddingPair.first)
                if (similarity > maxSimilarity) {
                    maxSimilarity = similarity
                    result = embeddingPair.second
                }
            }
        }

        Log.d("recognizePerson", "recognized person")

        return result
    }

    // Compare two embeddings with cosine similarity
    private fun getSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        var dotProduct = 0f
        var magnitude1 = 0f
        var magnitude2 = 0f

        for ((v1, v2) in vector1.zip(vector2)) {
            dotProduct += v1 * v2
            magnitude1 += v1 * v1
            magnitude2 += v2 * v2
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

    // To (xmin, ymin, xmax, ymax) format
    private fun getBoxCoordinates(
        xCenter: Int,
        yCenter: Int,
        height: Int,
        width: Int
    ): Array<Int> {
        return arrayOf(
            xCenter - width / 2,
            yCenter - height / 2,
            xCenter + width / 2,
            yCenter + height / 2
        )
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val result = stream.toByteArray()
        stream.reset()
        return result
    }

    // Найти людей на фото
    private suspend fun detect(bitmap: Bitmap): ArrayList<Array<Int>> =
        withContext(Dispatchers.Default) {

            val input = bitmapToByteArray(bitmap)

            val shape = longArrayOf(input.size.toLong())

            val inputTensor = OnnxTensor.createTensor(
                ortEnvironment,
                ByteBuffer.wrap(input),
                shape,
                OnnxJavaType.UINT8
            )
            Log.d("detect", "created tensor")

            val output = detectorOrtSession.run(
                Collections.singletonMap("image", inputTensor),
                setOf("image_out", "scaled_box_out_next")
            ) as Result
            Log.d("detect", "ran session")

            // bounding boxes in (xcenter, ycenter, height, width) format
            val result = (output.get(1)?.value) as Array<FloatArray>

            val boxIt = result.iterator()
            val boundingBoxes = ArrayList<Array<Int>>()

            while (boxIt.hasNext()) {
                val box = boxIt.next()
                for (element in box) {
                    Log.d("detect", element.toString())
                }
                // Is person detected
                if (box[5].toInt() == 0) {
                    Log.d("detect", "person detected")

                    val boxCoordinates = getBoxCoordinates(
                        box[0].toInt(),
                        box[1].toInt(),
                        box[2].toInt(),
                        box[3].toInt()
                    )

                    boundingBoxes.add(boxCoordinates)
                }
            }

            return@withContext boundingBoxes
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

    private fun readYolo(): ByteArray {
        val modelID = R.raw.yolov8n_with_pre_post_processing
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
                println("hit")
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
        val sessionOptions: OrtSession.SessionOptions = OrtSession.SessionOptions()
        sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
        sessionOptions.setIntraOpNumThreads(3)
        detectorOrtSession = ortEnvironment.createSession(
            readYolo(),
            sessionOptions
        )
        autoencoderOrtSession = ortEnvironment.createSession(
            readAutoencoder(),
            OrtSession.SessionOptions()
        )
    }
}