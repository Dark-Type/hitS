package com.example.hits.utility

import android.content.Context
import android.graphics.Bitmap
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class CameraX(
    private var context: Context,
    private var owner: LifecycleOwner,
) {

    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    val textAndTime = MutableLiveData<Pair<String, Long>>()
    var analysisRunning = false
    private var countdownTimer: CountDownTimer? = null
    var manuallyStopping = false
    var eventType = MutableLiveData<String>()

    fun startAnalysis() {
        if (!analysisRunning) {
            Log.d("TextRecognizer", "Setting up image analysis")
            imageAnalyzer?.setAnalyzer(ContextCompat.getMainExecutor(context), TextAnalysis(this))
            manuallyStopping = false
        } else {
            Log.d("TextRecognizer", "Analysis is already running")
        }
    }

    fun startTimer(seconds: Int, textType: String) {
        val duration = seconds * 1000L
        countdownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                textAndTime.postValue(Pair(textType, millisUntilFinished / 1000))
            }

            override fun onFinish() {
                textAndTime.postValue(Pair("-1", -1))
                eventType.postValue(textType)
                stopAnalysis()
                manuallyStopping = true
                countdownTimer = null
            }
        }
        countdownTimer?.start()
    }

    fun resetTimer() {
        countdownTimer?.cancel()
        textAndTime.postValue(Pair("", 0))
    }

    fun manuallyStopAnalysis() {
        stopAnalysis()
        manuallyStopping = true
    }

    fun stopAnalysis() {
        analysisRunning = false
        Log.d("TextRecognizer", "Stopping image analysis")
        imageAnalyzer?.clearAnalyzer()
    }


    private class TextAnalysis(private val cameraX: CameraX) : ImageAnalysis.Analyzer {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)


        private var firstRecognizedTime: Long = System.currentTimeMillis()

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            if (cameraX.manuallyStopping) {
                imageProxy.close()
                return
            }
            val mediaImage = imageProxy.image
            if (mediaImage != null && !cameraX.manuallyStopping) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val patternForPlantA = "(?i)plant".toRegex()
                        var triggeredByFlag = false

                        for (j in 1..6) {
                            val patternForFlag = "(?i)flag$j".toRegex()
                            if (patternForFlag.containsMatchIn(visionText.text)) {
                                triggeredByFlag = true
                                if (!cameraX.analysisRunning) {
                                    cameraX.analysisRunning = true
                                    cameraX.startTimer(8, "Flag $j")
                                }
                            }
                        }
                        if (patternForPlantA.containsMatchIn(visionText.text)) {
                            if (!cameraX.analysisRunning) {
                                cameraX.analysisRunning = true
                                cameraX.startTimer(10, "Plant")
                            }
                        } else {
                            if (!triggeredByFlag) {
                                cameraX.resetTimer()
                                cameraX.analysisRunning = false
                                firstRecognizedTime = System.currentTimeMillis()
                            }
                        }

                    }
                    .addOnFailureListener { e ->

                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }
    }

    val timerLiveData = MutableLiveData<Long>()

    private var imageCapture: ImageCapture? = null
    private var previewView: PreviewView? = null


    fun startCameraPreviewView(): PreviewView {
        if (cameraProvider == null) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()
                bindCamera()
            }, ContextCompat.getMainExecutor(context))
        } else {
            bindCamera()
        }
        return previewView ?: PreviewView(context).also { previewView = it }
    }

    private fun bindCamera() {

        if (previewView == null) {
            previewView = PreviewView(context)
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView!!.surfaceProvider)
        }

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageCapture = ImageCapture.Builder()
            .setJpegQuality(85)
            .setTargetRotation(previewView!!.display.rotation)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        try {
            cameraProvider?.unbindAll() // Unbind any existing use cases
            camera = cameraProvider?.bindToLifecycle(
                owner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
        } catch (e: Exception) {
            Log.e("CameraX", "Binding failed", e)
        }
    }
    fun toggleFlashLight(status: Boolean) {
        if (camera != null) {
            camera?.cameraControl?.enableTorch(status)
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()
                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        owner, cameraSelector
                    )
                    camera?.cameraControl?.enableTorch(status)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }


    fun capturePhoto(onCaptureFinished: (Bitmap) -> Unit) {
        Log.d("CameraX", "Capturing photo")

        val bitmap: Bitmap? = previewView?.bitmap
        if (bitmap != null) {
            Log.d("CameraX", "Photo captured")
            onCaptureFinished(bitmap)
        }

    }
}


