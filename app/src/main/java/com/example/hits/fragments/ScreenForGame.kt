package com.example.hits.fragments


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.hits.R
import com.example.hits.utility.CameraX
import kotlinx.coroutines.delay


class ScreenForGame() {


    @Composable
    fun GameScreen(navController: NavController) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val cameraX = remember { CameraX(context, lifecycleOwner) }
        CameraCompose(context = context, cameraX = cameraX, navController)
    }


    private fun getPhotoFromPath(path: String): Bitmap? {
        return BitmapFactory.decodeFile(path)
    }

    @Composable
    fun CameraCompose(
        context: Context,
        cameraX: CameraX,
        navController: NavController
    ) {

        var showDialog by remember { mutableStateOf(false) }
        var bitmapToShow by remember { mutableStateOf<Bitmap?>(null) }
        val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        var hasCamPermission by remember {
            mutableStateOf(
                REQUIRED_PERMISSIONS.all {
                    ContextCompat.checkSelfPermission(context, it) ==
                            PackageManager.PERMISSION_GRANTED
                })
        }
        var elapsedTime by remember { mutableLongStateOf(0L) }
        cameraX.timerLiveData.observe(LocalLifecycleOwner.current) { time ->
            elapsedTime = time
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { granted ->
                hasCamPermission = granted.size == 2
            }
        )
        LaunchedEffect(key1 = true) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCamPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { cameraX.startCameraPreviewView() }
                )
            }
            if (elapsedTime != 0L) {
                Text(
                    text = "Elapsed Time: ${elapsedTime / 1000} seconds",
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            Button(
                onClick = { /* Handle settings button click */ },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(text = "Settings")
            }

            Button(
                onClick = { navController.navigate("resultsScreen") },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(text = "Exit")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 45.dp), Arrangement.Bottom, Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(
                        start = 40.dp,
                        top = 12.dp,
                        end = 40.dp,
                        bottom = 12.dp
                    )
                ) {
                    Text(text = "Heal", fontSize = 16.sp)
                }

                Image(
                    painter = painterResource(id = R.drawable.prop),
                    contentDescription = "Shoot",
                    modifier = Modifier
                        .size(120.dp)
                        .clickable {

                            cameraX.capturePhoto { pathToPhoto ->
                                Toast
                                    .makeText(
                                        context,
                                        "Image Saved to $pathToPhoto",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                                val bitmap = getPhotoFromPath(pathToPhoto)
                                if (bitmap != null) {
                                    Toast
                                        .makeText(
                                            context,
                                            "BITMAP IS NOT NULL",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                    bitmapToShow = bitmap
                                    showDialog = true
                                }

                            }
                        }
                )

                Button(
                    onClick = { cameraX.startRealTimeTextRecognition() },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(
                        start = 40.dp,
                        top = 12.dp,
                        end = 40.dp,
                        bottom = 12.dp
                    )
                ) {
                    Text(text = "Interact", fontSize = 16.sp)
                }
            }
        }

        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Box(modifier = Modifier.size(200.dp)) {
                    bitmapToShow?.let { bitmap ->
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null)
                    }
                }
            }
        }
        LaunchedEffect(key1 = showDialog) {
            if (showDialog) {
                delay(2000)
                showDialog = false
            }
        }

    }

}






