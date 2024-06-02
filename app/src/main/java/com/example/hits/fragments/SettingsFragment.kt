package com.example.hits.fragments

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign

class SettingsFragment {
    @Composable
    fun SettingsScreen(navController: NavController) {
        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        var showDialog by remember { mutableStateOf(false) }
        var showToDoDialog by remember { mutableStateOf(false) }
        var newNickname by remember { mutableStateOf("") }
        val damage = sharedPrefHelper.getDamage() ?: "N/A"
        val kills = sharedPrefHelper.getKills() ?: "N/A"
        val deaths = sharedPrefHelper.getDeaths() ?: "N/A"
        val assists = sharedPrefHelper.getAssists() ?: "N/A"

        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                painter = painterResource(id = R.drawable.go_back),
                contentDescription = "Go back"
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(60.dp)
            ) {

                Button(
                    onClick = { showToDoDialog = true },
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text("Change Skins")
                }
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text("Change Nickname")
                }

                Text(
                    text = "KDA: $kills/$deaths/$assists",
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Damage: $damage",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Nickname to change") },
                text = {
                    OutlinedTextField(
                        value = newNickname,
                        onValueChange = { newNickname = it },
                        label = { Text("Enter new nickname") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            sharedPrefHelper.saveNickname(newNickname)
                            showDialog = false
                        })
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        sharedPrefHelper.saveNickname(newNickname)
                        showDialog = false
                    }) {
                        Text("Confirm")
                    }
                }
            )
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