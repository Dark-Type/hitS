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
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.hits.R
import com.example.hits.utility.CameraX
import kotlinx.coroutines.delay
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.hits.utility.NeuralNetwork
import com.example.hits.utility.PlayerLogic
import com.example.hits.utility.User
import com.example.hits.utility.UserForLeaderboard
import com.example.hits.utility.databaseRef
import com.example.hits.utility.endGame
import com.example.hits.utility.getUsersForCurrGameLeaderboard
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt


class ScreenForGame {
    private val shakeThreshold = 10f
    private val moderateSpeedThreshold = 30f
    private var shakeCount = 0
    private var shakeTime = 0
    private var job: Job? = null
    private lateinit var leaderboardData: SnapshotStateList<UserForLeaderboard>

    @Composable
    fun GameScreen(lobbyId: Int, userID: Int, currGameMode: String, navController: NavController) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val cameraX = remember { CameraX(context, lifecycleOwner) }

        leaderboardData = remember { getUsersForCurrGameLeaderboard(lobbyId) }

        CameraCompose(
            context = context,
            cameraX = cameraX,
            navController,
            lobbyId,
            userID,
            currGameMode
        )

        DisposableEffect(Unit) {

            val endGameListener = object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val value = dataSnapshot.getValue(Boolean::class.java)

                    if (value == false) {
                        navController.navigate("resultsScreen/$lobbyId/$userID/$currGameMode")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {

                }
            }

            val leaderboardListener = object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    leaderboardData.clear()
                    val users = mutableListOf<User>()

                    for (userSnapshot in dataSnapshot.children) {

                        users.add(userSnapshot.getValue(User::class.java)!!)
                    }

                    users.sortWith(compareByDescending<User> { it.kills }.thenBy { it.deaths })

                    var i = 0

                    for (user in users) {

                        leaderboardData.add(
                            UserForLeaderboard(
                                user.name,
                                0,
                                i + 1,
                                user.kills,
                                user.deaths,
                                user.assists
                            )
                        )

                        i++
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo").child("users")
                .addValueEventListener(leaderboardListener)

            databaseRef.child("rooms").child(lobbyId.toString()).child("isPlaying")
                .addValueEventListener(endGameListener)

            onDispose {
                databaseRef.child("rooms").child(lobbyId.toString()).child("isPlaying")
                    .removeEventListener(endGameListener)

                databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                    .child("users")
                    .removeEventListener(leaderboardListener)
            }
        }

        BackHandler {

        }
    }


    private fun getPhotoFromPath(path: String): Bitmap? {
        return BitmapFactory.decodeFile(path)
    }

    @Composable
    fun CameraCompose(
        context: Context,
        cameraX: CameraX,
        navController: NavController, lobbyId: Int, userID: Int, currGameMode: String
    ) {
        val player = PlayerLogic(if (currGameMode == "One Hit Elimination") 50 else 100)
        player.listenForChanges(lobbyId, userID)
        val neuralNetwork = NeuralNetwork.getInstance(context)

        var showDialog by remember { mutableStateOf(false) }
        var bitmapToShow by remember { mutableStateOf<Bitmap?>(null) }
        val requiredPermissions =
            mutableListOf(
                Manifest.permission.CAMERA,
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        var hasCamPermission by remember {
            mutableStateOf(
                requiredPermissions.all {
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
            if (elapsedTime == 10000L) {
                val playerID = userID //эт нейронкой мб?
                player.revive(lobbyId, playerID)
                Log.d("revive", "revive $playerID")
            }
            var showStatsDialog by remember { mutableStateOf(false) }

            IconButton(
                onClick = { showStatsDialog = true },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(50.dp)
                    .padding(top = 15.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.stats),
                    contentDescription = "Stats",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(50.dp)

                )
            }
            if (showStatsDialog) {
                Dialog(onDismissRequest = { showStatsDialog = false }) {
                    Surface(color = Color.White) {

                        LazyColumn(modifier = Modifier.padding(16.dp)) {
                            itemsIndexed(leaderboardData) { index, player ->
                                val cardColor = when (index) {
                                    0 -> Color(0xFFD4AF37)
                                    1 -> Color(0xFFC0C0C0)
                                    2 -> Color(0xFFCD7F32)
                                    else -> Color.Gray
                                }

                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 12.dp
                                    ),
                                    colors = CardColors(
                                        cardColor,
                                        Color.White,
                                        Color.Gray,
                                        Color.White
                                    ),
                                ) {
                                    Text(
                                        text = player.name,
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 20.sp
                                    )

                                    Text(
                                        text = player.kills.toString(),
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 20.sp
                                    )

                                    Text(
                                        text = player.deaths.toString(),
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 20.sp
                                    )

                                    Text(
                                        text = player.assists.toString(),
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    endGame(lobbyId)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 15.dp)
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
                val sensorManager =
                    context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

                val sensorListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        val acceleration =
                            sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH
                        if (acceleration > shakeThreshold) {
                            shakeCount++
                            if (acceleration > moderateSpeedThreshold) {
                                shakeTime += 2
                            } else {
                                shakeTime++
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                }

                IconButton(
                    onClick = {
                        shakeCount = 0
                        sensorManager.registerListener(
                            sensorListener,
                            accelerometer,
                            SensorManager.SENSOR_DELAY_NORMAL
                        )
                        job = CoroutineScope(Dispatchers.Main).launch {
                            while (isActive) {
                                delay(1000)
                                shakeTime++
                                if (shakeTime >= 10) {
                                    player.heal()
                                    Toast.makeText(
                                        context,
                                        "Shake time: $shakeTime",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    shakeTime = 0
                                    break
                                }
                            }
                            sensorManager.unregisterListener(sensorListener)
                        }
                    },
                    modifier = Modifier.size(100.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.heal),
                        contentDescription = "heal",
                        modifier = Modifier.size(100.dp)
                    )
                }
                IconButton(onClick = {
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
                            val playerId = neuralNetwork.predictIfHit(bitmap)
                            if (playerId != -1) {
                                player.doDamage(lobbyId, playerId)
                                Toast.makeText(context, "Hit", Toast.LENGTH_SHORT).show()
                                // display damage, id and animation
                            } else {
                                // display miss animation
                            }
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

                }) {


                    Image(
                        painter = painterResource(id = R.drawable.prop),
                        contentDescription = "Shoot",
                        modifier = Modifier
                            .size(120.dp)
                    )
                }

                IconButton(
                    onClick = { cameraX.startRealTimeTextRecognition() },
                    modifier = Modifier.size(100.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.interact),
                        contentDescription = "Interact",
                        modifier = Modifier.size(100.dp)
                    )
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