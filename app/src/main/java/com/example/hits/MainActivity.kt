package com.example.hits

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.rive.runtime.kotlin.core.Rive
import com.example.hits.fragments.ScreenForGame
import com.example.hits.fragments.JoinLobbyFragment
import com.example.hits.fragments.LobbyFragment
import com.example.hits.fragments.ScreenForResults
import com.example.hits.fragments.SettingsFragment
import com.example.hits.ui.theme.HitSTheme
import com.example.hits.ui.theme.LightTurquoise
import com.example.hits.ui.theme.Turquoise
import com.example.hits.utility.NeuralNetwork
import com.example.hits.utility.createUser
import com.example.hits.utility.getNewID
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var sharedPrefHelper: SharedPrefHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Rive.init(this)
        sharedPrefHelper = SharedPrefHelper(this)
        val neuralNetwork = NeuralNetwork(this)
        neuralNetwork.detect(neuralNetwork.readModel(), neuralNetwork.readInputImage())



        setContent {
            val navController = rememberNavController()

            val nickname = sharedPrefHelper.getNickname()

            NavHost(
                navController,
                startDestination = if (nickname.isNullOrEmpty()) "initUI" else "joinLobbyScreen"
            ) {
                composable("initUI") { InitUI(navController) }
                composable("joinLobbyScreen") { JoinLobbyFragment().JoinLobbyScreen(navController) }
                composable("settingsScreen") { SettingsFragment().SettingsScreen(navController) }
                composable("resultsScreen/{lobbyId}") { backStackEntry ->
                    val lobbyId = backStackEntry.arguments?.getString("lobbyId")
                    if (lobbyId != null) {
                        ScreenForResults().ResultsScreen(lobbyId.toInt(), navController)
                    }
                }
                composable("lobbyScreen/{lobbyId}") { backStackEntry ->
                    val lobbyId = backStackEntry.arguments?.getString("lobbyId")
                    if (lobbyId != null) {
                        LobbyFragment().LobbyScreen(lobbyId.toInt(), navController)
                    }
                }
                composable("gameScreen/{lobbyId}") { backStackEntry ->
                    val lobbyId = backStackEntry.arguments?.getString("lobbyId")
                    if (lobbyId != null) {
                        ScreenForGame().GameScreen(lobbyId.toInt(), navController)
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


    @Composable
    fun InitUI(navController: NavController) {
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {

            } else {


            }
        }
        LaunchedEffect(Unit) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }


        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        val nickname = sharedPrefHelper.getNickname() ?: ""
        val snackbarHostState = remember { SnackbarHostState() }
        val toast =
            Toast.makeText(LocalContext.current, "Creating your account...", Toast.LENGTH_SHORT)



        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.register_background),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            HitSTheme {

                Box(modifier = Modifier.fillMaxSize()) {
                    val scope = rememberCoroutineScope()
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        val textState = remember { mutableStateOf(nickname) }

                        TextField(
                            value = textState.value,
                            onValueChange = { textState.value = it },
                            placeholder = { Text("Enter your nickname") },
                            modifier = Modifier.padding(16.dp),
                            shape = MaterialTheme.shapes.small
                        )

                        Button(
                            onClick = {

                                if (textState.value.isBlank()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please enter a nickname")
                                    }
                                } else {
                                    sharedPrefHelper.saveNickname(textState.value)

                                    if (sharedPrefHelper.getNickname() != null) {

                                        toast.show()

                                        getNewID().thenAccept { newID ->

                                            sharedPrefHelper.createID(newID)

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
                                .padding(32.dp)
                                .fillMaxWidth(0.6f),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.buttonColors(LightTurquoise),
                            border = BorderStroke(1.dp, Turquoise)
                        ) {
                            Text("Save Nickname")
                        }
                    }
                    SnackbarHost(hostState = snackbarHostState)
                }
            }
        }

    }

}
