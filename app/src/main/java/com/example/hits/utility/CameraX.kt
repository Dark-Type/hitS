package com.example.hits.utility

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class CameraX(
    private var context: Context,
    private var owner: LifecycleOwner,
) {
    var photoPath = mutableStateOf("")
    val timerLiveData = MutableLiveData<Long>()

    private var imageCapture: ImageCapture? = null

    @OptIn(ExperimentalGetImage::class)
    fun startRealTimeTextRecognition() {

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        var startTime: Long? = null

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->

                        val text = visionText.text
                        if (text.contains("hitsIsBomb")) {
                            if (startTime == null) {

                                startTime = SystemClock.elapsedRealtime()
                            } else if (SystemClock.elapsedRealtime() - startTime!! >= 10000) {
                                timerLiveData.postValue(SystemClock.elapsedRealtime() - startTime!!)
                                startTime = null
                            }
                        } else {

                            startTime = null
                        }
                    }
                    .addOnFailureListener {
                        Log.e("CameraX", "Text Recognition Error", it)

                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }
    }


    fun startCameraPreviewView(): PreviewView {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val previewView = PreviewView(context)
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setJpegQuality(85)
            .setTargetRotation(Surface.ROTATION_0)
            .build()

        val camSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.bindToLifecycle(owner, camSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraX", "Error binding camera preview", e)
            }
        }, ContextCompat.getMainExecutor(context))

        return previewView
    }

    fun capturePhoto(onCaptureFinished: (String) -> Unit) = owner.lifecycleScope.launch {
        val imageCapture = imageCapture ?: return@launch

        imageCapture.takePicture(ContextCompat.getMainExecutor(context), object :
            ImageCapture.OnImageCapturedCallback(), ImageCapture.OnImageSavedCallback {
            override fun onCaptureSuccess(image: ImageProxy) {
                super.onCaptureSuccess(image)
                owner.lifecycleScope.launch(Dispatchers.Default) {
                    val fileName = StringBuilder().apply {
                        append("IMG_")
                        append(System.currentTimeMillis())
                    }.toString()
                    photoPath.value = saveMediaToCache(
                        imageProxyToBitmap(image),
                        fileName
                    )
                    withContext(Dispatchers.Main) {
                        onCaptureFinished(photoPath.value)
                    }
                }
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Toast.makeText(context, "Image Saved to ${photoPath.value}", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraX", "Image capture error", exception)
                Toast.makeText(context, "Image Error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private var byteArray: ByteArray? = null
    private suspend fun imageProxyToBitmap(image: ImageProxy): Bitmap =
        withContext(owner.lifecycleScope.coroutineContext) {
            val planeProxy = image.planes[0]
            val buffer: ByteBuffer = planeProxy.buffer
            val remaining = buffer.remaining()

            if (byteArray == null || byteArray!!.size != remaining) {
                byteArray = ByteArray(remaining)
            }

            buffer.get(byteArray!!)
            BitmapFactory.decodeByteArray(byteArray, 0, remaining)
        }

    private suspend fun saveMediaToCache(bitmap: Bitmap, name: String): String =
        withContext(IO) {
            val filename = "$name.jpg"
            val imageFile = File(context.cacheDir, filename)
            val fos = FileOutputStream(imageFile)

            fos.use {
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
                if (success) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Saved Successfully to ${imageFile.absolutePath}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@withContext imageFile.absolutePath
                }
            }
            return@withContext ""
        }
}