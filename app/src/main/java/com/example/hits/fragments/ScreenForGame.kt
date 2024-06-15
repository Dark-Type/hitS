package com.example.hits.fragments

import androidx.camera.core.CameraSelector
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.hits.R


class ScreenForGame() {
    @Composable
    fun GameScreen(navController: NavController) {
        Box(modifier = Modifier.fillMaxSize()) {
            CameraView(
                modifier = Modifier.fillMaxSize()
            )

            BottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                onLeftButtonClick = {  },
                onRightButtonClick = {  },
                onImageClick = {  }
            )

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
    @Composable
    fun BottomBar(modifier: Modifier = Modifier, onLeftButtonClick: () -> Unit, onRightButtonClick: () -> Unit, onImageClick: () -> Unit) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { onLeftButtonClick() }) {
                Text("Heal")
            }

            Image(
                painter = painterResource(id = R.drawable.prop),
                contentDescription = "Image",
                modifier = Modifier
                    .size(100.dp)
                    .clickable { onImageClick() }
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
