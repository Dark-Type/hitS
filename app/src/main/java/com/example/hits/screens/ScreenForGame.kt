package com.example.hits.screens


import android.Manifest
import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.hits.GAMEMODE_CS_GO
import com.example.hits.GAMEMODE_FFA
import com.example.hits.GAMEMODE_HNS
import com.example.hits.GAMEMODE_ONE_HIT_ELIMINATION
import com.example.hits.GAMEMODE_ONE_VS_ALL
import com.example.hits.GAMEMODE_TDM
import com.example.hits.SharedPrefHelper
import com.example.hits.ui.theme.LightTurquoise
import com.example.hits.ui.theme.Turquoise
import com.example.hits.ui.theme.Typography
import com.example.hits.utility.NeuralNetwork
import com.example.hits.utility.PlayerLogic
import com.example.hits.utility.TEAM_BLUE
import com.example.hits.utility.TEAM_RED
import com.example.hits.utility.TeamAndHealth
import com.example.hits.utility.User
import com.example.hits.utility.UserForLeaderboard
import com.example.hits.utility.copyStatsToGlobal
import com.example.hits.utility.databaseRef
import com.example.hits.utility.endGame
import com.example.hits.utility.explodeBomb
import com.example.hits.utility.getEmbeddings
import com.example.hits.utility.getUsersForCurrGameLeaderboard
import com.example.hits.utility.leaveFromOngoingGame
import com.example.hits.utility.voteForEnd
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule
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
    private var bombPlanted = false
    private var explosion = false
    private val HNSTimer = Timer()
    lateinit var HNSTimerSchedule : TimerTask


    @Suppress("DEPRECATION")
    @Composable
    fun DetectHitChangeAndAct(gotHit: MutableState<Boolean>) {
        val context = LocalContext.current
        val previousGotHit = remember { mutableStateOf(false) }

        LaunchedEffect(gotHit.value) {

            if (gotHit.value && gotHit.value != previousGotHit.value) {
                val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                    (context.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                }
                if (Build.VERSION.SDK_INT in 29..30) {
                    vibrator.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    )
                } else if (Build.VERSION.SDK_INT >= 31) {
                    vibrator.vibrate(
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    )
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                500L,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    }
                }
                gotHit.value = false
            }

            previousGotHit.value = gotHit.value
        }
    }

    @Composable
    fun GameScreen(
        lobbyId: Int,
        userID: Int,
        currGameMode: String,
        chosenTeam: Int,
        navController: NavController,
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val cameraX = remember { CameraX(context, lifecycleOwner) }
        val currentHealthStateForHealthBar = remember { mutableStateOf<Int?>(10) }
        val gotHit = remember { mutableStateOf(false) }
        var isPlayerHider = false


        Log.d("PlayerTeam", chosenTeam.toString())

        if (currGameMode == GAMEMODE_HNS) {

            HNSTimerSchedule = HNSTimer.schedule(300000) {
                databaseRef.child("rooms").child(lobbyId.toString())
                    .child("isPlaying")
                    .setValue(false)
            }
        }

        player = PlayerLogic(
            if (currGameMode == GAMEMODE_ONE_VS_ALL && (chosenTeam == TEAM_BLUE)) 1000 else 100,
            if (currGameMode == GAMEMODE_ONE_VS_ALL && (chosenTeam == TEAM_BLUE)) 30 else if (currGameMode == GAMEMODE_ONE_HIT_ELIMINATION || (currGameMode == GAMEMODE_HNS && chosenTeam== TEAM_BLUE)) 100 else 10
        )
        if (currGameMode == GAMEMODE_HNS) {
            if (chosenTeam != TEAM_BLUE) {
                isPlayerHider = true
            }
        }

        leaderboardData = remember { getUsersForCurrGameLeaderboard(lobbyId) }

        CameraCompose(
            context = context,
            cameraX = cameraX,
            lobbyId,
            userID,
            currGameMode,
            navController,
            currentHealthStateForHealthBar,
            gotHit,
            isPlayerHider
        )


        DisposableEffect(Unit) {

            val endGameListener = object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val value = dataSnapshot.getValue(Boolean::class.java)

                    if (value == false) {
                        endGame(lobbyId)
                        copyStatsToGlobal(lobbyId, userID)
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
                        gotHit.value = newHealth < player.getHealth()
                        player.changeHP(lobbyId, userID, newHealth, context)


                        currentHealthStateForHealthBar.value =
                            if (player.getHealthThreshold() == 100) newHealth / 10 else newHealth / 100
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Log the error
                }
            }

            val bombListener = object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val newBombValue = dataSnapshot.getValue(Boolean::class.java)

                    if (newBombValue == true) {

                        Toast.makeText(context, "Bomb Planted", Toast.LENGTH_SHORT)
                            .show()

                        Timer().schedule(60000) {
                            explodeBomb(lobbyId)
                        }

                        bombPlanted = true
                    }

                    if (newBombValue == false && bombPlanted) {

                        Toast.makeText(context, "Bomb Defused", Toast.LENGTH_SHORT)
                            .show()

                        Timer().schedule(2000) {
                            databaseRef.child("rooms").child(lobbyId.toString()).child("isPlaying")
                                .setValue(false)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Log the error
                }
            }

            val explosionListener = object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val newExplosion = dataSnapshot.getValue(Boolean::class.java)

                    if (newExplosion == true) {

                        explosion = newExplosion

                        Timer().schedule(2000) {
                            databaseRef.child("rooms").child(lobbyId.toString()).child("isPlaying")
                                .setValue(false)
                        }

                        Toast.makeText(context, "Bomb exploded", Toast.LENGTH_SHORT)
                            .show()

                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Log the error
                }
            }

            val aliveUsersListener = object : ChildEventListener {

                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

                    databaseRef.child("rooms").child(lobbyId.toString())
                        .child("gameInfo").child("users")
                        .addListenerForSingleValueEvent(object : ValueEventListener {

                            override fun onDataChange(snapshot: DataSnapshot) {

                                var redAlive = 0
                                var blueAlive = 0

                                for (userSnapshot in snapshot.children) {

                                    val userTeamAndHealth =
                                        userSnapshot.getValue(TeamAndHealth::class.java)

                                    if (userTeamAndHealth != null) {

                                        if (userTeamAndHealth.health > 0) {

                                            if (userTeamAndHealth.team == TEAM_RED) redAlive++
                                            if (userTeamAndHealth.team == TEAM_BLUE) blueAlive++
                                        }
                                    }
                                }

                                when (currGameMode) {

                                    GAMEMODE_FFA, GAMEMODE_ONE_HIT_ELIMINATION -> {

                                        if (redAlive <= 1) {

                                            Timer().schedule(2000) {
                                                databaseRef.child("rooms").child(lobbyId.toString())
                                                    .child("isPlaying")
                                                    .setValue(false)
                                            }

                                            Toast.makeText(
                                                context,
                                                "1 player alive!",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        }
                                    }

                                    GAMEMODE_TDM, GAMEMODE_ONE_VS_ALL -> {

                                        if (blueAlive <= 0 && redAlive <= 0) {

                                            Timer().schedule(2000) {
                                                databaseRef.child("rooms").child(lobbyId.toString())
                                                    .child("isPlaying")
                                                    .setValue(false)
                                            }

                                            Toast.makeText(
                                                context,
                                                "Draw! All players dead!",
                                                Toast.LENGTH_SHORT
                                            )
                                                .show()
                                        } else if (blueAlive <= 0) {

                                            Timer().schedule(2000) {
                                                databaseRef.child("rooms").child(lobbyId.toString())
                                                    .child("isPlaying")
                                                    .setValue(false)
                                            }

                                            Toast.makeText(context, "Red won!", Toast.LENGTH_SHORT)
                                                .show()
                                        } else if (redAlive <= 0) {

                                            Timer().schedule(2000) {
                                                databaseRef.child("rooms").child(lobbyId.toString())
                                                    .child("isPlaying")
                                                    .setValue(false)
                                            }

                                            Toast.makeText(context, "Red won!", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    }

                                    GAMEMODE_CS_GO -> {

                                        if (redAlive <= 0 && !bombPlanted) {

                                            Timer().schedule(2000) {
                                                databaseRef.child("rooms").child(lobbyId.toString())
                                                    .child("isPlaying")
                                                    .setValue(false)
                                            }

                                            Toast.makeText(context, "CT won!", Toast.LENGTH_SHORT)
                                                .show()
                                        }

                                        else if (blueAlive <= 0 && !explosion) {

                                            Timer().schedule(2000) {
                                                databaseRef.child("rooms").child(lobbyId.toString())
                                                    .child("isPlaying")
                                                    .setValue(false)
                                            }

                                            Toast.makeText(context, "T won!", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    }

                                    GAMEMODE_HNS -> {

                                        if (redAlive <= 0) {

                                            HNSTimerSchedule.cancel()
                                            HNSTimer.cancel()

                                            Timer().schedule(2000) {
                                                databaseRef.child("rooms").child(lobbyId.toString())
                                                    .child("isPlaying")
                                                    .setValue(false)
                                            }

                                            Toast.makeText(context, "Seekers won!", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {}
                        })
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            }

            databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo").child("users")
                .addValueEventListener(leaderboardListener)

            databaseRef.child("rooms").child(lobbyId.toString()).child("isPlaying")
                .addValueEventListener(endGameListener)

            databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo").child("users")
                .child(userID.toString()).child("health").addValueEventListener(healthListener)

            databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                .child("bombPlanted").addValueEventListener(bombListener)

            databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                .child("explosion").addValueEventListener(explosionListener)

            databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                .child("users").addChildEventListener(aliveUsersListener)

            onDispose {
                databaseRef.child("rooms").child(lobbyId.toString()).child("isPlaying")
                    .removeEventListener(endGameListener)

                databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                    .child("users")
                    .removeEventListener(leaderboardListener)

                databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                    .child("users")
                    .child("health").removeEventListener(healthListener)

                databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                    .child("bombPlanted").removeEventListener(bombListener)

                databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                    .child("explosion").removeEventListener(explosionListener)

                databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo")
                    .child("users").removeEventListener(aliveUsersListener)
            }
        }

        BackHandler {

        }
    }


    @Composable
    fun CameraCompose(
        context: Context,
        cameraX: CameraX,
        lobbyId: Int,
        userID: Int,
        currGameMode: String,
        navController: NavController,
        currentHealthStateForHealthBar: MutableState<Int?>,
        gotHit: MutableState<Boolean>,
        isPlayerHider: Boolean
    ) {

        val textAndTimeState = remember { mutableStateOf<Pair<String, Long>?>(null) }
        cameraX.textAndTime.observe(LocalLifecycleOwner.current) { pair ->
            textAndTimeState.value = pair
        }
        val dialogScale = remember { mutableFloatStateOf(0f) }


        LaunchedEffect(Unit) {
            dialogScale.floatValue = 1f
        }

        val animatedScale by animateFloatAsState(
            targetValue = dialogScale.floatValue,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            ), label = "smooth animations"
        )
        val hasNotInteracted = remember { mutableStateOf(true) }
        val lastObservedValue = remember { mutableStateOf<String?>(null) }
        fun triggerEvent(modeType: String) {
            hasNotInteracted.value = false
            if (modeType == "Plant") player.interactWithPlant(lobbyId, bombPlanted)
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

        DetectHitChangeAndAct(gotHit = gotHit)

        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        val thisPlayerID = sharedPrefHelper.getID()!!.toInt()
        val requiredPermissions =
            mutableListOf(
                Manifest.permission.CAMERA
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
        var showStatsDialog by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }

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
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            showStatsDialog = true
                            showSettingsDialog = false
                        },
                        modifier = Modifier
                            .size(50.dp)

                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.stats),
                            contentDescription = "Stats",
                            modifier = Modifier
                                .size(50.dp),
                        )
                    }
                    Image(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .align(Alignment.CenterVertically),
                        painter = when (currentHealthStateForHealthBar.value) {
                            0 -> painterResource(id = R.drawable.health_bar_0)
                            1 -> painterResource(id = R.drawable.health_bar_1)
                            2 -> painterResource(id = R.drawable.health_bar_2)
                            3 -> painterResource(id = R.drawable.health_bar_3)
                            4 -> painterResource(id = R.drawable.health_bar_4)
                            5 -> painterResource(id = R.drawable.health_bar_5)
                            6 -> painterResource(id = R.drawable.health_bar_6)
                            7 -> painterResource(id = R.drawable.health_bar_7)
                            8 -> painterResource(id = R.drawable.health_bar_8)
                            9 -> painterResource(id = R.drawable.health_bar_9)
                            10 -> painterResource(id = R.drawable.health_bar_10)
                            else -> {
                                painterResource(id = R.drawable.health_bar_10)
                            }
                        }, contentDescription = "Health bar"
                    )

                    IconButton(
                        onClick = {
                            showStatsDialog = false
                            showSettingsDialog = true
                        },
                        modifier = Modifier

                            .size(50.dp)


                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.settings),
                            contentDescription = "Settings",
                            modifier = Modifier
                                .size(50.dp)
                        )
                    }
                }

                textAndTimeState.value?.let { pair ->
                    if (pair.first != "0" && pair.first != "-1" && pair.first != " " && pair.second != 0L && hasNotInteracted.value) {
                        Text(
                            text = "Interacting with ${pair.first},\nTime remaining: ${pair.second} seconds",
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 32.dp),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            var showWaterImage by remember { mutableStateOf(false) }
            val imageScale by animateFloatAsState(if (showWaterImage) 1f else 0f)
            val imageAlpha by animateFloatAsState(if (showWaterImage) 1f else 0f)
            var waterOffset by remember { mutableStateOf(0.dp) }

            var gunOffset by remember { mutableStateOf(0.dp) }
            var gunClicked by remember { mutableStateOf(false) }
            var showAimHit by remember { mutableStateOf(false) }
            var isAnimationPlaying by remember { mutableStateOf(false) }


            LaunchedEffect(showAimHit) {
                if (showAimHit) {
                    delay(500)
                    showAimHit = false
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                if (!isPlayerHider) {
                    IconButton(
                        enabled = !isAnimationPlaying,
                        onClick = {
                            Log.d("CameraX", "button clicked")
                            if (currGameMode == GAMEMODE_HNS) {
                                CoroutineScope(Dispatchers.Default).launch {
                                    cameraX.toggleFlashLight(true)
                                    delay(500)
                                    cameraX.toggleFlashLight(false)
                                }
                            }

                            if (player.isAlive()) {

                                    cameraX.capturePhoto { bitmap ->
                                        Log.d("CameraX", "bitmap captured")

                                        //player.doDamage(lobbyId, 3, userID)
                                        CoroutineScope(Dispatchers.Main).launch {
                                            Log.d("CameraX", "coroutine launched")
                                            val playerTakenDamageId =
                                                neuralNetwork.predictIfHit(bitmap)
                                            Log.d(
                                                "CameraX",
                                                "playerId received: $playerTakenDamageId"
                                            )
                                            if (playerTakenDamageId != null) {
                                                showAimHit = true
                                                player.doDamage(
                                                    lobbyId,
                                                    playerTakenDamageId,
                                                    userID
                                                )
                                                Log.d("CameraX", "got damage")
                                            }
                                        }

                                        if (currGameMode!= GAMEMODE_HNS) {
                                            gunClicked = !gunClicked
                                            gunOffset = if (gunOffset == 0.dp) 10.dp else 0.dp
                                            waterOffset = if (waterOffset == 0.dp) 10.dp else 0.dp

                                            isAnimationPlaying = true
                                            showWaterImage = !showWaterImage
                                            CoroutineScope(Dispatchers.Main).launch {
                                                showWaterImage = !showWaterImage
                                                isAnimationPlaying = false
                                            }
                                            CoroutineScope(Dispatchers.Main).launch {
                                                gunOffset = 0.dp
                                                gunClicked = !gunClicked
                                                waterOffset = 0.dp

                                            }
                                        }


                                }
                            }

                        }, modifier = Modifier
                            .padding(bottom = 32.dp)
                            .size(400.dp)
                            .zIndex(1f)
                            .align(Alignment.BottomCenter)

                    )

                    {
                        val animatedOffset by animateDpAsState(
                            targetValue = gunOffset,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing

                            )
                        )

                        Image(
                            painter = painterResource(id = if (currGameMode!= GAMEMODE_HNS)R.drawable.main_gun else R.drawable.flashlight),
                            contentDescription = "Shoot",
                            modifier = Modifier
                                .size(400.dp)
                                .offset(y = animatedOffset)
                        )
                    }

                    if (showWaterImage) {
                        val animatedWaterOffset by animateDpAsState(
                            targetValue = waterOffset,
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )

                        Image(
                            painter = painterResource(id = R.drawable.water),
                            contentDescription = "Water",
                            modifier = Modifier
                                .size(450.dp * imageScale)
                                .align(Alignment.Center)
                                .alpha(imageAlpha)
                                .offset(y = animatedWaterOffset)
                        )
                    }
                }
            }

            Image(
                painter = painterResource(id = if (showAimHit) R.drawable.aim_hit else R.drawable.aim),
                contentDescription = "aim",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(20.dp)
            )

            if (showSettingsDialog) {
                Dialog(onDismissRequest = { showSettingsDialog = false }) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(animatedScale)
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
                                        copyStatsToGlobal(lobbyId, userID)
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
                        modifier = Modifier
                            .fillMaxHeight(0.6f)
                            .scale(animatedScale)
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
                    Box(modifier = Modifier.fillMaxWidth()) {

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
                                .align(Alignment.BottomStart)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.heal),
                                contentDescription = "heal",
                                modifier = Modifier
                                    .zIndex(2f)
                                    .size(100.dp),
                            )
                        }


                        var isAnalysisRunning = false
                        if (currGameMode == GAMEMODE_CS_GO) {
                            IconButton(
                                onClick = {
                                    if (!isAnalysisRunning) {

                                        cameraX.startAnalysis()
                                        isAnalysisRunning = true
                                    } else {
                                        cameraX.manuallyStopAnalysis()
                                        isAnalysisRunning = false
                                    }
                                },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.BottomEnd)
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


    }
}