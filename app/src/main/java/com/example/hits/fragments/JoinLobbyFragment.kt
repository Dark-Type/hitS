package com.example.hits.fragments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hits.R
import com.example.hits.SharedPrefHelper
import com.example.hits.utility.User
import com.example.hits.utility.addUserToRoom
import com.example.hits.utility.createRoom
import com.example.hits.utility.getRandomID

class JoinLobbyFragment {

    @Composable
    fun Greeting(modifier: Modifier = Modifier, preferences: SharedPrefHelper) {
        Box(modifier = modifier.padding(top = 32.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                text = "Hello, ${preferences.getNickname()}!",
            )
        }
    }

    @Composable
    fun JoinLobbyScreen(navController: NavController) {
        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        val lobbyCode = remember { mutableStateOf("") }
        val generatedId = remember { mutableStateOf("") }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(onClick = { navController.navigate("settingsScreen") }) {
                Icon(
                    painter = painterResource(id = R.drawable.settings_button),
                    contentDescription = "Settings"
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Greeting(preferences = sharedPrefHelper)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Button(onClick = {
                            generatedId.value = getRandomID().toString()

                            val firstUser = User(sharedPrefHelper.getID()!!.toInt(), sharedPrefHelper.getNickname()!!)

                            createRoom(generatedId.value.toInt())
                            addUserToRoom(generatedId.value.toInt(), firstUser)

                            navController.navigate("lobbyScreen/${generatedId.value}")
                        }) {
                            Text("Create Lobby")
                        }

                        Spacer(modifier = Modifier.height(50.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                            Text(text = "or", textAlign = TextAlign.Center)
                            HorizontalDivider(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                color = androidx.compose.ui.graphics.Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(64.dp))

                        OutlinedTextField(
                            value = lobbyCode.value,
                            onValueChange = { lobbyCode.value = it },
                            label = { Text("Input lobby code to join") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 64.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (lobbyCode.value.isNotBlank()) {
                                    val lobbyId = lobbyCode.value

                                    val newUser = User(sharedPrefHelper.getID()!!.toInt(), sharedPrefHelper.getNickname()!!)
                                    addUserToRoom(lobbyId.toInt(), newUser)

                                    navController.navigate("lobbyScreen/$lobbyId")
                                }
                            })
                        )
                    }
                }
            }
        }
    }
}
