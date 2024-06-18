package com.example.hits.fragments

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hits.R
import com.example.hits.SharedPrefHelper
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hits.ui.theme.Blue
import com.example.hits.ui.theme.StrokeBlue
import com.example.hits.utility.updateNicknameInDatabase

class SettingsFragment {
    @Composable
    fun SettingsScreen(navController: NavController) {
        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        var showDialog by remember { mutableStateOf(false) }
        var showToDoDialog by remember { mutableStateOf(false) }
        var newNickname by remember { mutableStateOf("") }
        val kills = sharedPrefHelper.getKills() ?: "N/A"
        val deaths = sharedPrefHelper.getDeaths() ?: "N/A"
        val assists = sharedPrefHelper.getAssists() ?: "N/A"

        val requestPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {

            } else {

            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.main_background),
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.go_back),
                    contentDescription = "Go back",
                )
            }
            Text(
                text = "Settings",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp),
                color = Color.White
            )


            Box(modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp)) {
                Column(
                    modifier = Modifier
                        .padding(60.dp)
                ) {
                    Text(
                        text = "Stats",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start),
                        color = Color.White
                    )
                    LazyColumn(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .align(Alignment.Start)
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .align(Alignment.Start)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Kills:",
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    )
                                    Text(
                                        text = kills,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .align(Alignment.Start)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Deaths:",
                                        textAlign = TextAlign.Start,
                                    )
                                    Spacer(
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    )
                                    Text(
                                        text = deaths,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                        item {
                            Card(
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .align(Alignment.Start)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Assists:",
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(
                                        modifier = Modifier.fillMaxWidth(0.8f)
                                    )
                                    Text(
                                        text = assists,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.fillMaxHeight(0.4f))

                    Button(
                        onClick = { showToDoDialog = true },
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                            .align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(Blue)
                    ) {
                        Text("Change Skins")
                    }
                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                            .align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(Blue)
                    ) {
                        Text("Change Nickname")
                    }
                    Button(
                        onClick = { requestPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .padding(bottom = 32.dp)
                            .align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(StrokeBlue)
                    ) {
                        Text("Request Camera Permission")
                    }
                }
            }

            if (showDialog) {
                Dialog(onDismissRequest = { showDialog = false }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color.White, shape = RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nickname to change",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            OutlinedTextField(
                                value = newNickname,
                                onValueChange = { newNickname = it },
                                label = { Text("Enter new nickname") },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    sharedPrefHelper.saveNickname(newNickname)
                                    updateNicknameInDatabase(sharedPrefHelper.getID()!!.toInt(), newNickname)
                                    showDialog = false
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    sharedPrefHelper.saveNickname(newNickname)
                                    updateNicknameInDatabase(sharedPrefHelper.getID()!!.toInt(), newNickname)
                                    showDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
            if (showToDoDialog) {
                AlertDialog(
                    onDismissRequest = { showToDoDialog = false },
                    title = { Text("Change Skins") },
                    text = { Text("TODO") },
                    confirmButton = {
                        Button(onClick = { showToDoDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}