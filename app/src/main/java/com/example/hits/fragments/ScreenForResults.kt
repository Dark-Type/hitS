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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.util.Random

class ScreenForResults {
    data class ScoreData(val id: Int, val score: Int)
    @Composable
    fun ResultsScreen(navController: NavController) {
        val scores = remember { mutableStateOf(generateRandomScores()) }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn {
                    itemsIndexed(scores.value) { index, scoreData ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${index + 1}.")
                            Text(text = "ID: ${scoreData.id}")
                            Text(text = "Score: ${scoreData.score}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { navController.navigate("joinLobbyFragment") }) {
                    Text("Go back")
                }


            }
        }
    }

    fun generateRandomScores(): List<ScoreData> {
        val random = Random()
        return List(10) { ScoreData(random.nextInt(100), random.nextInt(100)) }
            .sortedByDescending { it.score }
    }
}