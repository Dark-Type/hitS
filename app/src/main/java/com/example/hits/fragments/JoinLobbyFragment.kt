package com.example.hits.fragments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.hits.R
import com.example.hits.SharedPrefHelper
import com.example.hits.ui.theme.Blue
import com.example.hits.ui.theme.HitSTheme
import com.example.hits.ui.theme.LightTurquoise
import com.example.hits.ui.theme.StrokeBlue
import com.example.hits.ui.theme.Turquoise
import com.example.hits.utility.User
import com.example.hits.utility.addUserToRoom
import com.example.hits.utility.createRoom
import com.example.hits.utility.getRandomID

class JoinLobbyFragment {

    @Composable
    fun Greeting(modifier: Modifier = Modifier, preferences: SharedPrefHelper) {
        Box(
            modifier = modifier.padding(top = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "Hello, ${preferences.getNickname()}!",
            )
        }
    }

    @Composable
    fun NewsItem(
        news: String,
        showDialog: MutableState<Boolean>,
        selectedNews: MutableState<String?>
    ) {
        val newsTitle = news.split(":").firstOrNull() ?: news

        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            ),
            colors = CardColors(Blue, Color.White, StrokeBlue, Color.Gray),
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = newsTitle,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.bodySmall
                )
                IconButton(
                    onClick = {
                        selectedNews.value = news
                        showDialog.value = true
                    },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings_icon),
                        contentDescription = "info",
                        modifier = Modifier
                            .size(16.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun NewsDialog(news: String, showDialog: MutableState<Boolean>) {

        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp
        Dialog(onDismissRequest = { showDialog.value = false }) {
            ElevatedCard(
                modifier = Modifier.size(screenWidth * 2 / 3, screenHeight / 2)
            ) {

                Text(text = news, modifier = Modifier.padding(16.dp))
            }
        }
    }

    data class Player(
        val name: String,
        val score: Int,
        val rank: Int,
        val kills: Int,
        val deaths: Int,
        val assists: Int
    )

    @Composable
    fun LeaderboardItem(
        player: Player,
        index: Int,
        showDialog: MutableState<Boolean>,
        selectedPlayer: MutableState<Player?>
    ) {
        val backgroundColor = when (index) {
            0 -> Color(0xFFD4AF37)
            1 -> Color(0xFFC0C0C0)
            2 -> Color(0xFFCD7F32)
            else -> Blue
        }
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            colors = CardColors(backgroundColor, Color.White, StrokeBlue, Color.Gray),
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${player.rank}. ${player.name}",
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Score: ${player.score}",
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    style = MaterialTheme.typography.bodySmall
                )

                IconButton(
                    onClick = {
                        selectedPlayer.value = player
                        showDialog.value = true
                    },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings_icon),
                        contentDescription = "info",
                        modifier = Modifier
                            .size(16.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun PlayerStatsDialog(player: Player, showDialog: MutableState<Boolean>) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp
        Dialog(onDismissRequest = { showDialog.value = false }) {
            ElevatedCard(
                modifier = Modifier.size(screenWidth * 2 / 3, screenHeight / 2)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.SpaceEvenly,

                    ) {
                    Text(text = "Name: ${player.name}", modifier = Modifier.padding(16.dp))
                    Text(text = "Score: ${player.score}", modifier = Modifier.padding(16.dp))
                    Text(text = "Rank: ${player.rank}", modifier = Modifier.padding(16.dp))
                    Text(text = "Kills: ${player.kills}", modifier = Modifier.padding(16.dp))
                    Text(text = "Deaths: ${player.deaths}", modifier = Modifier.padding(16.dp))
                    Text(text = "Assists: ${player.assists}", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }


    @Composable
    fun JoinLobbyScreen(navController: NavController) {
        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        val lobbyCode = remember { mutableStateOf("") }
        val generatedId = remember { mutableStateOf("") }
        val showLeaderBoardsDialog = remember { mutableStateOf(false) }
        val selectedPlayer = remember { mutableStateOf<Player?>(null) }
        val showNewsDialog = remember { mutableStateOf(false) }
        val selectedNews = remember { mutableStateOf<String?>(null) }
        HitSTheme {
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
                IconButton(onClick = { navController.navigate("settingsScreen") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings_button),
                        contentDescription = "Settings",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .fillMaxSize(0.9f)
                ) {

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
                            val showNews = remember { mutableStateOf(true) }
                            val newsList = listOf(
                                "News 1: This is the first news item.",
                                "News 2: This is the second news item.",
                                "News 3: This is the third news item.",
                                "News 2: This is the second news item.",
                                "News 3: This is the third news item.",
                            )
                            val leaderboardList = listOf(
                                Player("Player", 100, 1, 50, 10, 40),
                                Player("Player", 90, 2, 45, 15, 35),
                                Player("Player", 80, 3, 40, 20, 30),

                                )

                            Button(
                                onClick = { showNews.value = !showNews.value },
                                colors = ButtonDefaults.buttonColors(LightTurquoise),
                                modifier = Modifier.padding(bottom = 16.dp),
                                border = BorderStroke(width = 1.dp, color = Turquoise)
                            ) {
                                Text(if (showNews.value) "To Leaderboards" else "To News")
                            }
                            if (showLeaderBoardsDialog.value) {
                                selectedPlayer.value?.let {
                                    PlayerStatsDialog(
                                        it,
                                        showLeaderBoardsDialog
                                    )
                                }
                            }
                            if (showNewsDialog.value) {
                                selectedNews.value?.let { NewsDialog(it, showNewsDialog) }
                            }

                            if (showNews.value) {
                                LazyColumn(modifier = Modifier.fillMaxHeight(0.5f)) {
                                    items(newsList) { news ->
                                        NewsItem(news, showNewsDialog, selectedNews)

                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxHeight(0.5f)) {
                                    itemsIndexed(leaderboardList) { index, player ->
                                        LeaderboardItem(
                                            player,
                                            index,
                                            showLeaderBoardsDialog,
                                            selectedPlayer
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(32.dp))

                                Button(
                                    onClick = {
                                        generatedId.value = getRandomID().toString()

                                        val firstUser = User(
                                            sharedPrefHelper.getID()!!.toInt(),
                                            sharedPrefHelper.getNickname()!!
                                        )

                                        createRoom(generatedId.value.toInt())
                                        addUserToRoom(generatedId.value.toInt(), firstUser)

                                        navController.navigate("lobbyScreen/${generatedId.value}")
                                    },
                                    colors = ButtonDefaults.buttonColors(LightTurquoise),
                                    border = BorderStroke(width = 1.dp, color = Turquoise)
                                ) {
                                    Text("Create Lobby")
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        color = Color.Gray
                                    )
                                    Text(text = "or", textAlign = TextAlign.Center)
                                    HorizontalDivider(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp),
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                OutlinedTextField(
                                    value = lobbyCode.value,
                                    onValueChange = { lobbyCode.value = it },
                                    label = { Text("Input lobby code to join") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (lobbyCode.value.isNotBlank()) {
                                            val lobbyId = lobbyCode.value

                                            val newUser = User(
                                                sharedPrefHelper.getID()!!.toInt(),
                                                sharedPrefHelper.getNickname()!!
                                            )
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
    }
}

