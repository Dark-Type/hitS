package com.example.hits

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hits.fragments.JoinLobbyFragment
import com.example.hits.fragments.LobbyFragment
import com.example.hits.ui.theme.HitSTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var sharedPrefHelper: SharedPrefHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefHelper = SharedPrefHelper(this)

        setContent {
            val navController = rememberNavController()

            val nickname = sharedPrefHelper.getNickname()

            NavHost(navController, startDestination = if (nickname.isNullOrEmpty()) "initUI" else "joinLobbyFragment") {
                composable("initUI") { InitUI(navController) }
                composable("joinLobbyFragment") { JoinLobbyFragment().JoinLobbyScreen(navController) }
                composable("lobbyFragment/{lobbyId}") { backStackEntry ->
                    val lobbyId = backStackEntry.arguments?.getString("lobbyId")
                    if (lobbyId != null) {
                        LobbyFragment().LobbyScreen(lobbyId.toInt())
                    }
                }
            }

            LaunchedEffect(nickname) {
                if (!nickname.isNullOrEmpty()) {
                    navController.navigate("joinLobbyFragment")
                }
            }
        }
    }
}
@Composable
fun InitUI(navController: NavController) {
    val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
    val nickname = sharedPrefHelper.getNickname() ?: ""
    val snackbarHostState = remember { SnackbarHostState() }

    HitSTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
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
                        modifier = Modifier.padding(16.dp)
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
                                    navController.navigate("joinLobbyFragment")
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Save Nickname")
                    }
                }
                SnackbarHost(hostState = snackbarHostState)
            }
        }
    }
}

