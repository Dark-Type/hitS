package com.example.hits.utility

import ai.onnxruntime.OrtSession
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import java.io.InputStream
import java.nio.FloatBuffer
import java.util.*

internal class ObjectDetector {
    fun detect(
        inputStream: InputStream,
        ortEnv: OrtEnvironment,
        ortSession: OrtSession
    ): Array<FloatArray> {
        // Step 1: convert image into byte array (raw image bytes)
        val rawImageBytes = inputStream.readBytes()

        // Step 2: get the shape of the byte array and make ort tensor
        val shape = longArrayOf(1, 3, 500, 500)

        // Normalize the byte values to float values
        val floatImageBytes = rawImageBytes.map { it.toFloat() / 255.0f }.toFloatArray()

        val inputTensor = OnnxTensor.createTensor(
            ortEnv,
            FloatBuffer.wrap(floatImageBytes),
            shape
        )

        inputTensor.use {
            // Step 3: call ort inferenceSession run
            val output = ortSession.run(Collections.singletonMap("input", inputTensor),
                setOf("output")
            )

            // Step 4: output analysis
            output.use {
                // Step 5: set output result
                return (output.get(0)?.value) as Array<FloatArray>
            }
        }
    }
}