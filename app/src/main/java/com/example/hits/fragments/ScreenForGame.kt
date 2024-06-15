package com.example.hits.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.hits.NeuralNetwork
import com.example.hits.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors


class ScreenForGame() {
    private val neuralNetwork = NeuralNetwork()
    private val imageCapture = ImageCapture.Builder().build()
    @Composable
    fun GameScreen(navController: NavController) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraView(
                modifier = Modifier.fillMaxSize()
            )

            BottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                onLeftButtonClick = {  }
            ) { }

            Button(onClick = { },
                modifier = Modifier.align(Alignment.TopStart)) {
                Text("Settings")
            }

            Button(onClick = { navController.navigate("resultsScreen") },
                modifier = Modifier.align(Alignment.TopEnd)) {
                Text("To Results")
            }
        }
    }

    suspend fun takePicture(context: Context): Bitmap? {
        return withContext(Dispatchers.IO) {
            val imageFile = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

            imageCapture.takePicture(
                outputOptions,
                Executors.newSingleThreadExecutor(),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    }

                    override fun onError(exception: ImageCaptureException) {

                    }
                }
            )
            BitmapFactory.decodeFile(imageFile.absolutePath)

        }
    }
    @Composable
    fun BottomBar(
        modifier: Modifier = Modifier,
        onLeftButtonClick: () -> Unit,
        onRightButtonClick: () -> Unit
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onLeftButtonClick() }) {
                Text("Heal")
            }

            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            Image(
                painter = painterResource(id = R.drawable.prop),
                contentDescription = "Image",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        coroutineScope.launch {
                            val bitmap = takePicture(context)
                            if (bitmap != null) {
                                neuralNetwork.putImage(bitmap)
                            }
                        }
                    }
            )

            Button(onClick = { onRightButtonClick() }) {
                Text("Interact")
            }
        }
    }
    @Composable
    fun CameraView(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build()

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)

        AndroidView(factory = { ctx ->
            PreviewView(ctx).apply {
                preview.setSurfaceProvider(surfaceProvider)
            }
        }, modifier = modifier.fillMaxSize())
    }
}