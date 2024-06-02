package com.example.hits.fragments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp


class LobbyFragment {
    data class User(val id: Int, val name: String)

    @Composable
    fun LobbyScreen(lobbyId: Int) {
        val users = listOf(
            User(id = 1, name = "User 1"),
            User(id = 2, name = "User 2"),
            User(id = 3, name = "User 3"),
            User(id = 4, name = "User 4"),
            User(id = 5, name = "User 5"),
            User(id = 6, name = "User 6"),
            User(id = 7, name = "User 7"),
            User(id = 8, name = "User 8"),
            User(id = 9, name = "User 9"),
            User(id = 10, name = "User 10"),
            User(id = 11, name = "User 11"),
            User(id = 12, name = "User 12")
        )
        val showDialog = remember { mutableStateOf(false) }
        val selectedMode = remember { mutableIntStateOf(0) }
        val modes = listOf(
            "Solo Battle Royale",
            "Duo Battle Royale",
            "Trio Battle Royale",
            "Squads Battle Royale",
            "Planters vs Defusers",
            "Capture the flag",
            "BedWars",
            "Sniper Royale"
        )
        val votes = remember { mutableStateOf(List(modes.size) { 0 }) }
        val hasVoted = remember { mutableStateOf(false) }

        Text(text = "Lobby $lobbyId", modifier = Modifier.padding(16.dp))

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier
                .weight(0.5f)
                .padding(top = 64.dp)) {
                UserList(users)
            }

            DisplayModePercentages(selectedMode.intValue, modes, votes.value)


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { showDialog.value = true }) {
                    Text(text = "Choose mode")
                }

                Button(onClick = { /* Game starting logic */ }) {
                    Text(text = "Start session")
                }
            }
        }
        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                title = { Text(text = "Choose a mode") },
                text = {
                    LazyColumn {
                        itemsIndexed(modes) { index, mode ->
                            Text(
                                text = mode,
                                modifier = Modifier.clickable {
                                    if (!hasVoted.value) {
                                        selectedMode.intValue = index
                                        votes.value =
                                            votes.value.toMutableList().also { it[index]++ }
                                        hasVoted.value = true
                                        showDialog.value = false
                                    } else {
                                        showDialog.value = false

                                    }
                                }
                            )
                        }
                    }
                },
                confirmButton = { }
            )
        }
    }



@Composable
fun DisplayModePercentages(selectedMode: Int, modes: List<String>, votes: List<Int>) {
    val sortedModes = modes.zip(votes).sortedByDescending { it.second }
    LazyRow {
        itemsIndexed(sortedModes) { index, pair ->
            val mode = pair.first
            val vote = pair.second
            val displayPercentage = if (index == selectedMode) 100.0 / modes.size else 0.0
            Text(
                text = "$mode: ${displayPercentage.toInt()}% (Votes: $vote)",
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
fun UserList(users: List<User>) {
    LazyColumn {
        items(users) { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = user.name)
                Button(onClick = { println("Button clicked for user: ${user.name}") }) {
                    Text(text = "Button")
                }
            }
        }
    }
}
}