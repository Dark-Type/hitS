package com.example.hits.fragments


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.hits.GAMEMODE_ONE_HIT_ELIMINATION
import com.example.hits.SharedPrefHelper
import com.example.hits.ui.theme.LightTurquoise
import com.example.hits.ui.theme.Turquoise
import com.example.hits.ui.theme.Typography
import com.example.hits.utility.NeuralNetwork
import com.example.hits.utility.PlayerLogic
import com.example.hits.utility.User
import com.example.hits.utility.UserForLeaderboard
import com.example.hits.utility.databaseRef
import com.example.hits.utility.getEmbeddings
import com.example.hits.utility.getUsersForCurrGameLeaderboard
import com.example.hits.utility.leaveFromOngoingGame
import com.example.hits.utility.voteForEnd
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
    private lateinit var player: PlayerLogic
    private var isVoted = false


    @Composable
    fun HealthToast() {
        val context = LocalContext.current
        val health = remember { mutableIntStateOf(player.getHealth()) }

        LaunchedEffect(health.intValue) {
            Toast.makeText(context, "Current health: ${health.intValue}", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    fun GameScreen(lobbyId: Int, userID: Int, currGameMode: String, navController: NavController) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val cameraX = remember { CameraX(context, lifecycleOwner) }

        player = PlayerLogic(if (currGameMode == GAMEMODE_ONE_HIT_ELIMINATION) 50 else 100)

        leaderboardData = remember { getUsersForCurrGameLeaderboard(lobbyId) }

        CameraCompose(
            context = context,
            cameraX = cameraX,
            lobbyId,
            userID,
            currGameMode,
            navController
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

                    for ((i, user) in users.withIndex()) {

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

                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            }

            val healthListener = object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val newHealth = dataSnapshot.getValue(Int::class.java)

                    if (newHealth != null) {
                        player.changeHP(lobbyId, userID, newHealth, context)

                        Toast.makeText(context, "Health changed: $newHealth", Toast.LENGTH_SHORT)
                            .show()

                        Log.d("TAKED DMG", newHealth.toString() + " curr hp: ${player.getHealth()}")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Log the error
                }
            }

            databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo").child("users")
                .addValueEventListener(leaderboardListener)

            databaseRef.child("rooms").child(lobbyId.toString()).child("isPlaying")
                .addValueEventListener(endGameListener)

            databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo").child("users")
                .child(userID.toString()).child("health").addValueEventListener(healthListener)

            onDispose {
                databaseRef.child("rooms").child(lobbyId.toString()).child("isPlaying")
                    .removeEventListener(endGameListener)

                databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                    .child("users")
                    .removeEventListener(leaderboardListener)

                databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                    .child("users")
                    .child("health").removeEventListener(healthListener)
            }
        }

        BackHandler {

        }
    }


    @Composable
    fun CameraCompose(
        context: Context,
        cameraX: CameraX,
        lobbyId: Int, userID: Int, currGameMode: String, navController: NavController
    ) {
        val textAndTimeState = remember { mutableStateOf<Pair<String, Long>?>(null) }
        cameraX.textAndTime.observe(LocalLifecycleOwner.current) { pair ->
            textAndTimeState.value = pair
        }
        val lastObservedValue = remember { mutableStateOf<String?>(null) }
        fun triggerEvent(modeType: String) {

            Toast.makeText(context, "You interacted with $modeType", Toast.LENGTH_SHORT).show()
        }
        cameraX.eventType.observe(LocalLifecycleOwner.current) { modeType ->

            if (modeType != lastObservedValue.value) {
                triggerEvent(modeType)

                lastObservedValue.value = modeType
            }

        }
        val neuralNetwork = NeuralNetwork.getInstance(context)
        LaunchedEffect(lobbyId) {
            CoroutineScope(Dispatchers.Default).launch {
                val embeddings: Array<Pair<FloatArray, Int>> by lazy { getEmbeddings(lobbyId) }
                neuralNetwork.embeddingsSetter(embeddings)
            }
        }

        HealthToast()
        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        val thisPlayerID = sharedPrefHelper.getID()!!.toInt()
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
        var showDialog by remember { mutableStateOf(false) }
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
            textAndTimeState.value?.let { pair ->
                if (pair.first != "0" && pair.first != "-1" && pair.first != " " && pair.second != 0L) {
                    Text(
                        text = "Interacting with ${pair.first},\nTime remaining: ${pair.second} seconds",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 32.dp),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        Log.d("CameraX", "button clicked")

                        if (player.isAlive())
                            cameraX.capturePhoto() { bitmap ->
                                Log.d("CameraX", "bitmap captured")

                                //player.doDamage(lobbyId, 3, userID)
                                CoroutineScope(Dispatchers.Main).launch {
                                    Log.d("CameraX", "coroutine launched")
                                    val playerTakenDamageId = neuralNetwork.predictIfHit(bitmap)
                                    Log.d("CameraX", "playerId received: $playerTakenDamageId")
                                    if (playerTakenDamageId != null) {

                                        player.doDamage(lobbyId, playerTakenDamageId, userID)
                                        Log.d("CameraX", "got damage")

                                        Toast.makeText(
                                            context,
                                            "You hit player with id $playerTakenDamageId",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // display damage, id and animation
                                    } else {
                                        //display miss animation
                                    }
                                    Toast
                                        .makeText(
                                            context,
                                            "DONE",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                    showDialog = true
                                }
                            }

                    }, enabled = true, modifier = Modifier
                        .padding(bottom = 32.dp)
                        .size(400.dp)
                        .zIndex(1f)
                        .align(Alignment.BottomCenter)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.main_gun),
                        contentDescription = "Shoot",
                        modifier = Modifier
                            .size(400.dp)
                    )
                }
            }
            Image(
                painter = painterResource(id = R.drawable.aim),
                contentDescription = "aim",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp)
            )
            var showStatsDialog by remember { mutableStateOf(false) }
            var showSettingsDialog by remember { mutableStateOf(false) }


            IconButton(
                onClick = {
                    showStatsDialog = true
                    showSettingsDialog = false
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 32.dp, start = 32.dp)
                    .size(50.dp)

            ) {
                Image(
                    painter = painterResource(id = R.drawable.stats),
                    contentDescription = "Stats",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(50.dp),
                )
            }
            IconButton(
                onClick = {
                    showStatsDialog = false
                    showSettingsDialog = true
                },
                modifier = Modifier
                    .padding(top = 32.dp, end = 32.dp)
                    .size(50.dp)
                    .align(Alignment.TopEnd)

            ) {
                Image(
                    painter = painterResource(id = R.drawable.settings),
                    contentDescription = "Settings",
                    modifier = Modifier
                        .size(50.dp)
                )
            }
            if (showSettingsDialog) {
                Dialog(onDismissRequest = { showSettingsDialog = false }) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {

                            ElevatedCard(
                                modifier = Modifier
                                    .padding(
                                        top = 16.dp,
                                        start = 16.dp,
                                        end = 8.dp,
                                    )
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        if (!isVoted) {
                                            voteForEnd(lobbyId)
                                            isVoted = true
                                        }
                                    },
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 12.dp
                                ),
                                colors = CardColors(
                                    LightTurquoise,
                                    Color.White,
                                    Color.Gray,
                                    Color.White
                                ),
                            ) {
                                Text(
                                    text = "Vote for Ending Game",
                                    modifier = Modifier.padding(16.dp),
                                    style = Typography.bodySmall
                                )
                            }
                            ElevatedCard(
                                modifier = Modifier
                                    .padding(
                                        top = 8.dp,
                                        start = 16.dp,
                                        end = 16.dp,
                                        bottom = 16.dp
                                    )
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        leaveFromOngoingGame(lobbyId, userID)
                                        navController.navigate("resultsScreen/$lobbyId/$userID/$currGameMode")

                                        //endGame(lobbyId)
                                    },
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 12.dp
                                ),
                                colors = CardColors(
                                    Turquoise,
                                    Color.White,
                                    Color.Gray,
                                    Color.White
                                ),
                            ) {
                                Text(
                                    text = "Forfeit and leave",
                                    modifier = Modifier.padding(16.dp),
                                    style = Typography.bodySmall
                                )
                            }

                        }
                    }
                }
            }
            if (showStatsDialog) {
                Dialog(onDismissRequest = { showStatsDialog = false }) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxHeight(0.6f)
                    ) {
                        Column(modifier = Modifier.fillMaxHeight()) {


                            ElevatedCard(
                                modifier = Modifier
                                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 12.dp
                                ),
                                colors = CardColors(
                                    Color.White,
                                    Color.Black,
                                    Color.Gray,
                                    Color.White
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Place",
                                        modifier = Modifier.padding(
                                            bottom = 8.dp,
                                            top = 8.dp,
                                            start = 16.dp
                                        ),
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = "Name",
                                        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
                                        fontSize = 20.sp
                                    )

                                    Text(
                                        text = "K",
                                        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
                                        fontSize = 20.sp
                                    )

                                    Text(
                                        text = "D",
                                        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp),
                                        fontSize = 20.sp
                                    )

                                    Text(
                                        text = "A",
                                        modifier = Modifier.padding(
                                            bottom = 8.dp,
                                            top = 8.dp,
                                            end = 16.dp
                                        ),
                                        fontSize = 20.sp
                                    )
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxHeight()
                            ) {
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
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = player.rank.toString(),
                                                modifier = Modifier.padding(
                                                    bottom = 8.dp,
                                                    top = 8.dp,
                                                    start = 16.dp
                                                ),
                                                fontSize = 20.sp
                                            )
                                            Text(
                                                text = player.name,
                                                modifier = Modifier.padding(
                                                    bottom = 8.dp,
                                                    top = 8.dp
                                                ),
                                                fontSize = 20.sp
                                            )

                                            Text(
                                                text = player.kills.toString(),
                                                modifier = Modifier.padding(
                                                    bottom = 8.dp,
                                                    top = 8.dp
                                                ),
                                                fontSize = 20.sp
                                            )

                                            Text(
                                                text = player.deaths.toString(),
                                                modifier = Modifier.padding(
                                                    bottom = 8.dp,
                                                    top = 8.dp
                                                ),
                                                fontSize = 20.sp
                                            )

                                            Text(
                                                text = player.assists.toString(),
                                                modifier = Modifier.padding(
                                                    bottom = 8.dp,
                                                    top = 8.dp,
                                                    end = 16.dp
                                                ),
                                                fontSize = 20.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 45.dp), Arrangement.Bottom, Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .zIndex(2f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sensorManager =
                        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                    val accelerometer =
                        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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

                            if (player.isAlive()) {

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
                                            player.heal(lobbyId, thisPlayerID)

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
                            }
                        },
                        modifier = Modifier
                            .padding(16.dp)
                            .size(100.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.heal),
                            contentDescription = "heal",
                            modifier = Modifier
                                .zIndex(2f)
                                .size(100.dp),
                        )
                    }
                    Spacer(modifier = Modifier.fillMaxWidth(0.65f))

                    var isAnalysisRunning = false

                    IconButton(
                        onClick = {
                            if (!isAnalysisRunning) {
                                Log.d("TextRecognition", "Interact button clicked")
                                cameraX.startAnalysis()
                                isAnalysisRunning = true
                            } else {
                                Log.d(
                                    "TextRecognition",
                                    "Interact button clicked again, stopping analysis"
                                )
                                cameraX.manuallyStopAnalysis()
                                isAnalysisRunning = false
                            }
                        },
                        modifier = Modifier
                            .size(100.dp)
                            .zIndex(2f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.interact),
                            contentDescription = "Interact",
                            modifier = Modifier
                                .size(100.dp)
                        )
                    }
                }
            }

        }


    }
}