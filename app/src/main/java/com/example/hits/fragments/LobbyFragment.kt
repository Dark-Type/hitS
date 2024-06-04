package com.example.hits.fragments

import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.hits.R
import com.example.hits.SharedPrefHelper
import com.example.hits.utility.User
import com.example.hits.utility.databaseRef
import com.example.hits.utility.getLobbyUsers
import com.example.hits.utility.removeUserFromRoom
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError


class LobbyFragment {

    var users : SnapshotStateList<User> = mutableStateListOf()
    var lobbyIdToCheck = 0

    @Composable
    fun LobbyScreen(lobbyId: Int, navController: NavController) {

        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        val lifecycleOwner = LocalLifecycleOwner.current

        lobbyIdToCheck = lobbyId

        users = remember { getLobbyUsers(lobbyId) }
        listenForChanges(lobbyId)

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
        }
        Text(text = "Lobby $lobbyId", modifier = Modifier.padding(16.dp))


        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(top = 64.dp)
            ) {

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

        BackHandler {
            // Get the current user
            val currentUser = users.find { it.id == sharedPrefHelper.getID()?.toInt() }

            // Remove the current user from the room
            if (currentUser != null) {
                removeUserFromRoom(lobbyId, currentUser)
            }

            // Navigate back
            navController.popBackStack()
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

    private fun listenForChanges(lobbyId: Int) {
        val childEventListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val newUser = dataSnapshot.getValue(User::class.java)
                val existingUser = users.find { it.id == newUser?.id }

                // Only add the new user if it doesn't already exist in the list
                if (existingUser == null && newUser != null) {
                    users.add(newUser)
                }
                // Call a function to update your UI here
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                // A user has changed, find it and update
                val newUser = dataSnapshot.getValue(User::class.java)
                val oldUser = users.find { it.id == newUser?.id }
                oldUser?.let {
                    users[users.indexOf(it)] = newUser!!
                }
                // Call a function to update your UI here
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                users.remove(user)
                // Call a function to update your UI here
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        }

        databaseRef.child("rooms").child(lobbyId.toString()).child("users").addChildEventListener(childEventListener)
    }
}