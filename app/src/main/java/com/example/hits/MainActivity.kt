package com.example.hits

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.rive.runtime.kotlin.core.Rive
import com.example.hits.fragments.ScreenForGame
import com.example.hits.fragments.JoinLobbyFragment
import com.example.hits.fragments.LobbyFragment
import com.example.hits.fragments.ScreenForAR
import com.example.hits.fragments.ScreenForResults
import com.example.hits.fragments.SettingsFragment
import com.example.hits.ui.theme.HitSTheme
import com.example.hits.ui.theme.LightTurquoise
import com.example.hits.ui.theme.Typography
import com.example.hits.utility.NeuralNetwork
import com.example.hits.utility.createUser
import com.example.hits.utility.getNewID
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var sharedPrefHelper: SharedPrefHelper
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Rive.init(this)
        sharedPrefHelper = SharedPrefHelper(this)

        val neuralNetwork = NeuralNetwork.getInstance(this)

        if (sharedPrefHelper.getDamage() == null) sharedPrefHelper.saveDamage("0")
        if (sharedPrefHelper.getKills() == null) sharedPrefHelper.saveKills("0")
        if (sharedPrefHelper.getDeaths() == null) sharedPrefHelper.saveDeaths("0")
        if (sharedPrefHelper.getAssists() == null) sharedPrefHelper.saveAssists("0")
        if (sharedPrefHelper.getPoints() == null) sharedPrefHelper.savePoints("0")

        setContent {
            val navController = rememberNavController()

            val nickname = sharedPrefHelper.getNickname()
            val id = sharedPrefHelper.getID()

            NavHost(
                navController,
                startDestination = if (nickname.isNullOrEmpty() || id == "-1") "initUI" else "joinLobbyScreen",
            ) {
                composable("initUI") { InitUI(navController) }
                composable("joinLobbyScreen") { JoinLobbyFragment().JoinLobbyScreen(navController) }
                composable("settingsScreen/{lobbyId}") { backStackEntry ->
                    val lobbyId = backStackEntry.arguments?.getString("lobbyId")

                    if (lobbyId != null)
                        SettingsFragment().SettingsScreen(navController, lobbyId.toInt())
                }
                composable("resultsScreen/{lobbyId}/{userID}/{gamemodePlayed}") { backStackEntry ->
                    val lobbyId = backStackEntry.arguments?.getString("lobbyId")
                    val userID = backStackEntry.arguments?.getString("userID")
                    val gamemodePlayed = backStackEntry.arguments?.getString("gamemodePlayed")
                    if (lobbyId != null && userID != null && gamemodePlayed != null) {
                        ScreenForResults().ResultsScreen(
                            lobbyId.toInt(),
                            userID.toInt(),
                            gamemodePlayed,
                            navController
                        )
                    }
                }
                composable("arScreen") { ScreenForAR().ArScreen(navController) }
                composable("lobbyScreen/{lobbyId}") { backStackEntry ->
                    val lobbyId = backStackEntry.arguments?.getString("lobbyId")
                    if (lobbyId != null) {
                        LobbyFragment().LobbyScreen(lobbyId.toInt(), navController)
                    }
                }
                composable("gameScreen/{lobbyId}/{userID}/{currGamemode}") { backStackEntry ->
                    val lobbyId = backStackEntry.arguments?.getString("lobbyId")
                    val userID = backStackEntry.arguments?.getString("userID")
                    val currGamemode = backStackEntry.arguments?.getString("currGamemode")

                    if (lobbyId != null && userID != null && currGamemode != null) {

                        ScreenForGame().GameScreen(
                            lobbyId.toInt(),
                            userID.toInt(),
                            currGamemode,
                            navController
                        )
                    }
                }
            }
            LaunchedEffect(nickname) {
                if (!nickname.isNullOrEmpty()) {
                    navController.navigate("joinLobbyScreen")
                }
            }

        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    @Composable
    fun RequestPermissionOnStart() {
        val context = LocalContext.current
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(
                    context,
                    "You need to allow app to use camera!\nYou can set it in settings",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(context, "Thank you!", Toast.LENGTH_SHORT).show()
            }
        }

        LaunchedEffect(Unit) {
            val permission = Manifest.permission.CAMERA
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Toast.makeText(context, "Thank you", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    requestPermissionLauncher.launch(permission)
                }
            }
        }
    }

    @Composable
    fun InitUI(navController: NavController) {
        RequestPermissionOnStart()

        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        val nickname = sharedPrefHelper.getNickname() ?: ""
        val snackbarHostState = remember { SnackbarHostState() }
        val toast =
            Toast.makeText(LocalContext.current, "Creating your account...", Toast.LENGTH_SHORT)

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.lobby_background),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Image(
                painter = painterResource(id = R.drawable.lobby_gun0),
                contentDescription = "Gun",
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .fillMaxHeight(0.8f)
                    .align(Alignment.Center)
            )


            HitSTheme {

                Box(modifier = Modifier.fillMaxSize()) {
                    val scope = rememberCoroutineScope()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        val textState = remember { mutableStateOf(nickname) }

                        OutlinedTextField(
                            value = textState.value,
                            onValueChange = { textState.value = it },
                            placeholder = {
                                Text(
                                    "Enter your nickname",
                                    style = Typography.bodyMedium,
                                    color = Color(0xFF828282),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            },
                            modifier = Modifier
                                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp)
                                .fillMaxWidth()
                                .shadow(3.dp, RoundedCornerShape(50)),
                            shape = RoundedCornerShape(35),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )

                        @Composable
                        fun Modifier.animatedBorder(
                            borderColors: List<Color>,
                            backgroundColor: Color,
                            shape: Shape = RoundedCornerShape(50),
                            borderWidth: Dp = 1.dp,
                            animationDurationInMillis: Int = 1000,
                            easing: Easing = LinearEasing
                        ): Modifier {
                            val brush = Brush.sweepGradient(borderColors)
                            val infiniteTransition =
                                rememberInfiniteTransition(label = "animatedBorder")
                            val angle by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = animationDurationInMillis,
                                        easing = easing
                                    ),
                                    repeatMode = RepeatMode.Restart
                                ), label = "angleAnimation"
                            )

                            return this
                                .clip(shape)
                                .padding(borderWidth)
                                .drawWithContent {
                                    rotate(angle) {
                                        drawCircle(
                                            brush = brush,
                                            radius = size.width,
                                            blendMode = BlendMode.SrcIn,
                                        )
                                    }
                                    drawContent()
                                }
                                .background(color = backgroundColor, shape = shape)
                        }

                        var shouldDisplayAnimation by remember { mutableStateOf(false) }
                        Button(
                            onClick = {

                                if (textState.value.isBlank()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please enter a nickname")
                                    }
                                } else {
                                    shouldDisplayAnimation = true
                                    sharedPrefHelper.saveNickname(textState.value)

                                    if (sharedPrefHelper.getNickname() != null) {


                                        toast.show()

                                        getNewID().thenAccept { newID ->

                                            sharedPrefHelper.createID(newID)
                                            sharedPrefHelper.saveDamage("0")
                                            sharedPrefHelper.saveKills("0")
                                            sharedPrefHelper.saveDeaths("0")
                                            sharedPrefHelper.saveAssists("0")
                                            sharedPrefHelper.savePoints("0")

                                            createUser(
                                                sharedPrefHelper.getID()!!.toInt(),
                                                sharedPrefHelper.getNickname()!!
                                            )

                                            navController.navigate("joinLobbyScreen")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 100.dp, start = 32.dp, end = 32.dp)
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 3.dp,
                                    shape = MaterialTheme.shapes.extraLarge,
                                    clip = true
                                )
                                .let {
                                    if (shouldDisplayAnimation) it.animatedBorder(
                                        borderColors = listOf(
                                            LightTurquoise,
                                            Color.White,
                                            Color.White,
                                            LightTurquoise,
                                            LightTurquoise,
                                            LightTurquoise,
                                            LightTurquoise
                                        ), backgroundColor = Color.White
                                    ) else it
                                },
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.buttonColors(LightTurquoise),

                            ) {
                            Text(
                                "Register",
                                style = Typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 32.dp)
                    )
                }
            }
        }

    }


}
