package com.example.hits.fragments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.hits.R
import com.example.hits.SharedPrefHelper
import com.example.hits.ui.theme.HitSTheme
import com.example.hits.ui.theme.LightTurquoise
import com.example.hits.ui.theme.StrokeBlue
import com.example.hits.ui.theme.Turquoise
import com.example.hits.ui.theme.Typography
import com.example.hits.utility.User
import com.example.hits.utility.UserForLeaderboard
import com.example.hits.utility.addUserToRoom
import com.example.hits.utility.createRoom
import com.example.hits.utility.getNews
import com.example.hits.utility.getRandomID
import com.example.hits.utility.getUsersForLeaderboard

class JoinLobbyFragment {

    @Composable
    fun Greeting(modifier: Modifier = Modifier, preferences: SharedPrefHelper) {
        Box(
            modifier = modifier.padding(top = 4.dp, bottom = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                        append("Hello, ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("${preferences.getNickname()}")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                        append("!")
                    }
                },
                fontSize = 28.sp
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
            colors = CardColors(Color(0xFFDADDE2), Color.Black, StrokeBlue, Color.Gray),
            modifier = Modifier
                .fillMaxSize()
                .height(80.dp)
                .padding(4.dp)
                .clickable {
                    selectedNews.value = news
                    showDialog.value = true
                },
            shape = RoundedCornerShape(15.dp)

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
                    style = MaterialTheme.typography.labelMedium
                )


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
                modifier = Modifier.size(screenWidth * 2 / 3, screenHeight / 2),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 12.dp
                )
            ) {

                Text(text = news, modifier = Modifier.padding(16.dp))
            }
        }
    }

    @Composable
    fun LeaderboardItem(
        user: UserForLeaderboard,
        index: Int,
        showDialog: MutableState<Boolean>,
        selectedPlayer: MutableState<UserForLeaderboard?>
    ) {
        val backgroundColor = when (index) {
            0 -> Color(0xFFD4AF37)
            1 -> Color(0xFFC0C0C0)
            2 -> Color(0xFFCD7F32)
            else -> Color(0xFFDADDE2)
        }
        val fontColor = if (index > 2) Color.Black else Color.White
        val fontType = if (index > 2) Typography.bodyMedium else Typography.bodySmall
        val cardElevation = if (index < 3) 12.dp else 6.dp
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = cardElevation
            ),
            colors = CardColors(backgroundColor, fontColor, StrokeBlue, Color.Gray),
            modifier = Modifier
                .fillMaxSize()
                .height(64.dp)
                .padding(4.dp),
            shape = RoundedCornerShape(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${user.rank + 1}. ${user.name}",
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    style = fontType,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
                Text(
                    text = "Score: ${user.points}",
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    style = Typography.bodyMedium
                )

                IconButton(
                    onClick = {
                        selectedPlayer.value = user
                        showDialog.value = true
                    },
                    modifier = Modifier.padding(4.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings_icon),
                        contentDescription = "info",
                        modifier = Modifier
                            .size(16.dp),
                        tint = fontColor
                    )
                }
            }
        }
    }

    @Composable
    fun PlayerStatsDialog(user: UserForLeaderboard, showDialog: MutableState<Boolean>) {
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
                    Text(text = "Name: ${user.name}", modifier = Modifier.padding(16.dp))
                    Text(text = "Score: ${user.points}", modifier = Modifier.padding(16.dp))
                    Text(text = "Rank: ${user.rank+1}", modifier = Modifier.padding(16.dp))
                    Text(text = "Kills: ${user.kills}", modifier = Modifier.padding(16.dp))
                    Text(text = "Deaths: ${user.deaths}", modifier = Modifier.padding(16.dp))
                    Text(text = "Assists: ${user.assists}", modifier = Modifier.padding(16.dp))
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
        val selectedPlayer = remember { mutableStateOf<UserForLeaderboard?>(null) }
        val showNewsDialog = remember { mutableStateOf(false) }
        val selectedNews = remember { mutableStateOf<String?>(null) }
        val isSurfaceVisible = remember { mutableStateOf(false) }
        val newsList = remember { getNews() }

        HitSTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                Image(
                    painter = painterResource(id = R.drawable.lobby_background),
                    contentDescription = "Background",
                    modifier = Modifier
                        .fillMaxSize()
                        .let {
                            if (isSurfaceVisible.value) it.clickable(onClick = {
                                isSurfaceVisible.value = false
                            }) else it
                        },
                    contentScale = ContentScale.FillBounds
                )
                Button(
                    onClick = { navController.navigate("arScreen") },
                    colors = ButtonDefaults.buttonColors(LightTurquoise),
                    border = BorderStroke(width = 1.dp, color = Turquoise)
                ) {
                    Text("Open AR Screen")
                }

                IconButton(
                    onClick = { isSurfaceVisible.value = true },
                    modifier = Modifier
                        .fillMaxSize(0.8f)
                        .fillMaxHeight(0.8f)
                        .align(Alignment.Center)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.lobby_gun0),
                        contentDescription = "to lobby",
                    )
                }
                IconButton(
                    onClick = {  if(!isSurfaceVisible.value)navController.navigate("settingsScreen/-1") else isSurfaceVisible.value = false },
                    modifier = Modifier
                        .fillMaxSize(0.4f)
                        .align(Alignment.BottomStart)
                        .padding(16.dp),

                ) {
                    Image(
                        painter = painterResource(id = R.drawable.lobby_go_to_settings),
                        contentDescription = "Settings",
                        modifier = Modifier


                    )
                }
                if (isSurfaceVisible.value) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 25.dp, bottom = 60.dp, start = 16.dp, end = 16.dp)
                            .fillMaxSize(0.9f)
                    ) {

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Greeting(preferences = sharedPrefHelper)
                                val showNews = remember { mutableStateOf(true) }

                                val leaderboardList = remember { getUsersForLeaderboard() }

                                Button(
                                    onClick = { showNews.value = !showNews.value },
                                    colors = ButtonDefaults.buttonColors(LightTurquoise),
                                    modifier = Modifier.shadow(3.dp, shape = RoundedCornerShape(50)).fillMaxWidth(1f),

                                ) {
                                    Text(if (showNews.value) "To Leaderboards" else "To News", modifier = Modifier.padding(4.dp))
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
                                        itemsIndexed(leaderboardList) { index, user ->
                                            LeaderboardItem(
                                                user,
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
                                    Spacer(modifier = Modifier.height(64.dp))

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
                                        modifier = Modifier.fillMaxWidth(1f).shadow(3.dp, shape = RoundedCornerShape(50))
                                    ) {
                                        Text("Create Lobby", modifier = Modifier.padding(4.dp))
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

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

                                    Spacer(modifier = Modifier.height(12.dp))

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

        BackHandler {

        }
    }
}

