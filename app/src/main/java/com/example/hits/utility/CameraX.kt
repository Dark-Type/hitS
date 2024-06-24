package com.example.hits.utility

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraX(
    private var context: Context,
    private var owner: LifecycleOwner,
) {
    var photoPath = mutableStateOf("")
    val timerLiveData = MutableLiveData<Long>()

    private var imageCapture: ImageCapture? = null
    private var previewView: PreviewView? = null

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
        previewView = PreviewView(context)
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView!!.surfaceProvider)
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

        return previewView!!
    }

    fun capturePhoto(onCaptureFinished: (Bitmap) -> Unit) {
        owner.lifecycleScope.launch(Dispatchers.Default) {
            var bitmap: Bitmap?
            withContext(Dispatchers.Main) {
                bitmap = previewView?.bitmap
            }
            if (bitmap != null) {
                withContext(Dispatchers.Main) {
                    onCaptureFinished(bitmap!!)
                }
            }
        }
    }


}