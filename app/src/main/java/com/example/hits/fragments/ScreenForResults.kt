package com.example.hits.fragments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.hits.R
import com.example.hits.ui.theme.Bronze
import com.example.hits.ui.theme.Gold
import com.example.hits.ui.theme.Silver
import com.example.hits.utility.getUsersForResultsScreen
import com.example.hits.utility.removeUserFromRoom
import com.example.hits.utility.ScoreData
import java.util.Random

class ScreenForResults {

    @Composable
    fun ResultsScreen(lobbyId: Int, userID: Int, gamemodePlayed: String, navController: NavController) {

        val scores = remember { getUsersForResultsScreen(lobbyId, gamemodePlayed) }
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "Results",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Color.White,
                        fontSize = 52.sp
                    )
                    LazyColumn(modifier = Modifier.fillMaxHeight(0.6f)) {
                        itemsIndexed(scores) { index, scoreData ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 12.dp,
                                ),
                                shape = RoundedCornerShape(4.dp),
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
                                        text = "${index + 1}.",

                                        )
                                    Text(text = "ID: ${scoreData.id}")
                                    Text(text = "Score: ${scoreData.score}")
                                }
                            }
                        }
                    }
                    val whiteColor = Color(0xFFE0E0E0)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showDialog.value = true },
                        modifier = Modifier.padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(whiteColor)
                    ) {
                        Text("Overall stats", color = Color.Black)
                    }

                    Button(
                        onClick = {
                            removeUserFromRoom(lobbyId, userID)
                            navController.navigate("joinLobbyScreen")
                                  },
                        modifier = Modifier.padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(whiteColor)
                    ) {
                        Text("Return to Main menu", color = Color.Black)
                    }

                    Button(
                        onClick = {
                            println(scores.size)
                            navController.navigate("lobbyScreen/$lobbyId") },
                        modifier = Modifier.padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(whiteColor)
                    ) {
                        Text("Return to Lobby", color = Color.Black)
                    }

                }
            }

            BackHandler {

            }
        }

        if (showDialog.value) {
            Dialog(onDismissRequest = { showDialog.value = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 12.dp
                    )
                ) {
                    Column {
                        Text("Prop data")
                    }
                }

            }
        }
    }
}