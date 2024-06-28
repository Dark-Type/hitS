package com.example.hits.fragments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.hits.R
import com.example.hits.ui.theme.Bronze
import com.example.hits.ui.theme.Gold
import com.example.hits.ui.theme.LightTurquoise
import com.example.hits.ui.theme.Silver
import com.example.hits.ui.theme.Typography
import com.example.hits.utility.databaseRef
import com.example.hits.utility.getUsersForResultsScreen
import com.example.hits.utility.removeUserFromRoom

class ScreenForResults {

    @Composable
    fun ResultsScreen(
        lobbyId: Int,
        userID: Int,
        gameModePlayed: String,
        navController: NavController
    ) {

        val scores = remember { getUsersForResultsScreen(lobbyId, gameModePlayed) }
        val showDialog = remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxSize()) {

            Image(
                painter = painterResource(id = R.drawable.main_background),
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Results",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Color.White,
                        fontSize = 52.sp,
                        style = Typography.labelLarge
                    )
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxHeight(0.7f).padding(start = 16.dp, end = 16.dp),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {


                            LazyColumn(modifier = Modifier.fillMaxHeight(0.6f).padding(16.dp)) {
                                itemsIndexed(scores) { index, scoreData ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = 12.dp,
                                        ),
                                        shape = RoundedCornerShape(15.dp),
                                        colors = CardColors(
                                            when (index) {
                                                0 -> Gold
                                                1 -> Silver
                                                2 -> Bronze
                                                else -> Color.White
                                            }, if (index > 2) {
                                                Color.Black
                                            } else Color.White, Color.Gray, Color.White
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${index + 1}. ",

                                                )
                                            Text(text = scoreData.name)
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(text = "${scoreData.score}")
                                        }
                                    }
                                }
                            }
                            Card(
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                                    .fillMaxWidth(0.9f)
                                    .shadow(4.dp, RoundedCornerShape(50.dp))
                                    .align(Alignment.CenterHorizontally)
                                    .clickable { showDialog.value = true },
                                shape = RoundedCornerShape(50.dp),
                                colors = CardColors(
                                    containerColor = LightTurquoise,
                                    contentColor = Color.White,
                                    Color.White,
                                    Color.White
                                )
                            ) {
                                Text(
                                    "Overall stats",
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.CenterHorizontally),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                                    .fillMaxWidth(0.9f)
                                    .shadow(4.dp, RoundedCornerShape(50.dp))
                                    .align(Alignment.CenterHorizontally)
                                    .clickable {
                                        databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo").setValue("")
                                        removeUserFromRoom(lobbyId, userID)
                                        navController.navigate("joinLobbyScreen")
                                    },
                                shape = RoundedCornerShape(50.dp),
                                colors = CardColors(
                                    containerColor = LightTurquoise,
                                    contentColor = Color.White,
                                    Color.White,
                                    Color.White
                                )
                            ) {
                                Text(
                                    "Return to Main menu",
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.CenterHorizontally),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .shadow(4.dp, RoundedCornerShape(50.dp))
                                    .fillMaxWidth(0.9f)
                                    .align(Alignment.CenterHorizontally)
                                    .clickable {
                                        println(scores.size)
                                        databaseRef.child("rooms").child(lobbyId.toString()).child("gameInfo").setValue("")
                                        navController.navigate("lobbyScreen/$lobbyId")
                                    },
                                shape = RoundedCornerShape(50.dp),
                                colors = CardColors(
                                    containerColor = LightTurquoise,
                                    contentColor = Color.White,
                                    Color.White,
                                    Color.White
                                )
                            ) {
                                Text(
                                    "Return to Lobby",
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.CenterHorizontally),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                        }
                    }
                }

                BackHandler {

                }
            }
        }

        if (showDialog.value) {
            Dialog(onDismissRequest = { showDialog.value = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()

                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 12.dp
                    )
                ) {
                    Card (Modifier.fillMaxWidth(), colors = CardColors(Color.White, Color.Black, Color.Black, Color.White)) {

                        Text(text = "Kills ${(scores.find { it.id == userID })?.kills}", modifier = Modifier.padding(16.dp), style = Typography.bodyMedium)
                        Text(text = "Deaths ${(scores.find { it.id == userID })?.deaths}", modifier = Modifier.padding(16.dp), style = Typography.bodyMedium)
                        Text(text = "Assists ${(scores.find { it.id == userID })?.assists}", modifier = Modifier.padding(16.dp), style = Typography.bodyMedium)
                    }
                }

            }
        }
    }
}