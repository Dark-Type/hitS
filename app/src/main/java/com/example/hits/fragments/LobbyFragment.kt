package com.example.hits.fragments


import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.hits.R
import com.example.hits.SharedPrefHelper
import com.example.hits.getGamemodes
import com.example.hits.ui.theme.LightTurquoise
import com.example.hits.ui.theme.StrokeBlue
import com.example.hits.ui.theme.Turquoise
import com.example.hits.utility.User
import com.example.hits.utility.addValue
import com.example.hits.utility.databaseRef
import com.example.hits.utility.getLobbyUsers
import com.example.hits.utility.removeUserFromRoom
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError


class LobbyFragment {

    var users: SnapshotStateList<User> = mutableStateListOf()
    var lobbyIdToCheck = 0
    var databaseVotesRef = databaseRef
    var didLocalDeviceInitiateChange = false

    @Composable
    fun LobbyScreen(lobbyId: Int, navController: NavController) {


        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        databaseVotesRef = databaseRef.child("rooms").child(lobbyId.toString()).child("gamemodeVotes")

        lobbyIdToCheck = lobbyId

        users = remember { getLobbyUsers(lobbyId) }

        val showDialog = remember { mutableStateOf(false) }
        val selectedMode = remember { mutableIntStateOf(-1) }
        val modes = getGamemodes()
        val votes = remember { mutableStateOf(List(modes.size) { 0 }) }

        listenForChanges(lobbyId, modes, votes)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopEnd
        ) {
            Image(
                painter = painterResource(id = R.drawable.main_background),
                contentDescription = "Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { navController.navigate("settingsScreen") },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.settings_button),
                        contentDescription = "Settings"

                    )
                }
            }
            Surface(
                color = Color.White, shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 25.dp, bottom = 60.dp, start = 16.dp, end = 16.dp)
                    .fillMaxSize(0.9f)
            ) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "Lobby: $lobbyId",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 32.sp
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .padding(top = 64.dp)
                    ) {

                        UserList(users)
                    }


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { showDialog.value = true },
                            colors = ButtonDefaults.buttonColors(LightTurquoise),
                            border = BorderStroke(width = 1.dp, color = Turquoise),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(text = "Choose mode")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { navController.navigate("gameScreen/$lobbyId") },
                            colors = ButtonDefaults.buttonColors(LightTurquoise),
                            border = BorderStroke(width = 1.dp, color = Turquoise),

                            ) {
                            Text(text = "Start session")
                        }
                    }
                }
                if (showDialog.value) {
                    Dialog(onDismissRequest = { showDialog.value = false }) {
                        Box(
                            modifier = Modifier
                                .background(Color.LightGray)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Choose a mode",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                LazyColumn {
                                    itemsIndexed(modes) { index, mode ->
                                        ElevatedCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 8.dp),
                                            elevation = CardDefaults.cardElevation(
                                                defaultElevation = 6.dp
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardColors(
                                                Color.White,
                                                Color.Black,
                                                StrokeBlue,
                                                Color.Gray
                                            )
                                        ) {
                                            Text(
                                                text = "$mode: ${votes.value[index]} votes",
                                                modifier = Modifier
                                                    .clickable {
                                                        if (selectedMode.intValue != index) {

                                                            if (selectedMode.intValue != -1) {
                                                                votes.value =
                                                                    votes.value
                                                                        .toMutableList()
                                                                        .also {
                                                                            it[selectedMode.intValue]--
                                                                            addValue(databaseVotesRef.child(modes[selectedMode.intValue]), -1)
                                                                        }
                                                            }
                                                            votes.value =
                                                                votes.value
                                                                    .toMutableList()
                                                                    .also {
                                                                        it[index]++
                                                                        addValue(databaseVotesRef.child(modes[index]), 1)
                                                                    }
                                                            selectedMode.intValue = index
                                                        } else {
                                                            votes.value =
                                                                votes.value
                                                                    .toMutableList()
                                                                    .also {
                                                                        it[index]--
                                                                        addValue(databaseVotesRef.child(modes[selectedMode.intValue]), -1)
                                                                    }

                                                            selectedMode.intValue = -1
                                                        }
                                                        showDialog.value = false
                                                    }
                                                    .padding(16.dp),
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                BackHandler {

                    val currentUser = users.find { it.id == sharedPrefHelper.getID()?.toInt() }

                    if (currentUser != null) {
                        removeUserFromRoom(lobbyId, currentUser)
                    }

                    if (selectedMode.intValue != -1) {
                        addValue(databaseVotesRef.child(modes[selectedMode.intValue]), -1)
                    }

                    navController.popBackStack()
                }
            }
        }
    }


    @Composable
    fun UserList(users: List<User>) {
        LazyColumn {
            items(users) { user ->
                ElevatedCard(
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    ),
                    colors = CardColors(Color.White, Color.Black, StrokeBlue, Color.Gray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Text(
                            text = user.name, modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(start = 8.dp)
                        )

                        Spacer(modifier = Modifier.width(30.dp))
                        Button(
                            onClick = { println("Button clicked for user: ${user.name}") },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                LightTurquoise
                            ),
                            modifier = Modifier
                                .size(71.dp, 30.dp)
                                .align(Alignment.CenterVertically),
                            content = {
                                Text(text = "Scan", fontSize = 10.sp)
                            }
                        )
                        Spacer(modifier = Modifier.width(60.dp))
                        IconButton(onClick = { }) {
                            Icon(
                                painter = painterResource(id = R.drawable.settings_icon),
                                contentDescription = "info",
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.CenterVertically)

                            )
                        }

                    }
                }
            }
        }
    }

    private fun listenForChanges(lobbyId: Int, modes: List<String>, votes: MutableState<List<Int>>) {

        val userJoinListener = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val newUser = dataSnapshot.getValue(User::class.java)
                val existingUser = users.find { it.id == newUser?.id }

                if (existingUser == null && newUser != null) {
                    users.add(newUser)
                }

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {

                val newUser = dataSnapshot.getValue(User::class.java)
                val oldUser = users.find { it.id == newUser?.id }
                oldUser?.let {
                    users[users.indexOf(it)] = newUser!!
                }

            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                users.remove(user)

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        }

        val voteChangesListener = object : ChildEventListener {

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {

                if (didLocalDeviceInitiateChange) {

                    didLocalDeviceInitiateChange = false
                }

                else {
                    val key = dataSnapshot.key
                    val value = dataSnapshot.getValue(Int::class.java)

                    val updatedVotes = votes.value.toMutableList()
                    updatedVotes[modes.indexOf(key)] = value ?: updatedVotes[modes.indexOf(key)]
                    votes.value = updatedVotes

                    Log.d("Firebase", "Vote for $key changed to $value")
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(databaseError: DatabaseError) {}
        }

        databaseRef.child("rooms").child(lobbyId.toString()).child("users")
            .addChildEventListener(userJoinListener)

        databaseVotesRef.addChildEventListener(voteChangesListener)
    }
}