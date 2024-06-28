package com.example.hits.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hits.ui.theme.Turquoise
import com.example.hits.ui.theme.Typography
import com.example.hits.utility.databaseRef
import com.example.hits.utility.updateNicknameInDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ScreenForSettings {
    @Composable
    fun SettingsScreen(navController: NavController, lobbyId: Int) {
        val sharedPrefHelper = SharedPrefHelper(LocalContext.current)
        var showDialog by remember { mutableStateOf(false) }
        var showToDoDialog by remember { mutableStateOf(false) }
        var newNickname by remember { mutableStateOf("") }
        val kills = sharedPrefHelper.getKills() ?: "N/A"
        val deaths = sharedPrefHelper.getDeaths() ?: "N/A"
        val assists = sharedPrefHelper.getAssists() ?: "N/A"
        val context = LocalContext.current

        val requestPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(context, "Thank you!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permission is not granted", Toast.LENGTH_SHORT).show()
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.main_background),
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Text(
                text = "Settings",
                fontSize = 36.sp,
                style = Typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 36.dp),
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .align(Alignment.Center)
                ) {
                    Text(
                        text = "Stats",
                        fontSize = 28.sp,
                        style = Typography.labelLarge,
                        modifier = Modifier.align(Alignment.Start),
                        color = Color.White
                    )
                    LazyColumn(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.Start)
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .fillMaxWidth(),
                                colors = CardDefaults.cardColors(Color(0xFFD9D9D9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Kills:",
                                        textAlign = TextAlign.Start,
                                        color = Color(0xFF595959)
                                    )
                                    Spacer(
                                        modifier = Modifier.fillMaxWidth(0.95f)
                                    )
                                    Text(
                                        text = kills,
                                        textAlign = TextAlign.End,
                                        color = Color(0xFF595959)
                                    )
                                }
                            }
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(Color(0xFFD9D9D9)),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
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
                                        color = Color(0xFF595959)
                                    )
                                    Spacer(
                                        modifier = Modifier.fillMaxWidth(0.95f)
                                    )
                                    Text(
                                        text = deaths,
                                        textAlign = TextAlign.End,
                                        color = Color(0xFF595959)
                                    )
                                }
                            }
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(Color(0xFFD9D9D9)),
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .align(Alignment.Start)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Assists:",
                                        textAlign = TextAlign.Start,
                                        color = Color(0xFF595959)
                                    )
                                    Spacer(
                                        modifier = Modifier.fillMaxWidth(0.95f)
                                    )
                                    Text(
                                        text = assists,
                                        textAlign = TextAlign.End,
                                        color = Color(0xFF595959)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.fillMaxHeight(0.05f))

                    Card(
                        colors = CardDefaults.cardColors(Color(0xFFD9D9D9)),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .clickable { showToDoDialog = true },

                        ) {
                        Text(
                            "Change Skins",
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally),
                            color = Color(0xFF595959),
                            style = Typography.bodySmall
                        )
                    }
                    Card(
                        colors = CardDefaults.cardColors(Color(0xFFD9D9D9)),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .clickable { showDialog = true },
                    ) {
                        Text(
                            "Change Nickname",
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally),
                            color = Color(0xFF595959),
                            style = Typography.bodySmall
                        )
                    }
                    @Composable
                    fun ShowAlertDialog(
                        showDialog: Boolean,
                        onDismiss: () -> Unit,
                        onConfirm: () -> Unit
                    ) {
                        if (showDialog) {
                            AlertDialog(
                                onDismissRequest = onDismiss,
                                title = { Text("Camera permission required") },
                                text = { Text("Camera permission is required for this feature. Please go to the app settings to grant the permission.") },
                                confirmButton = {
                                    Button(onClick = onConfirm) {
                                        Text("Go to settings")
                                    }
                                },
                                dismissButton = {
                                    Button(onClick = onDismiss) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }

                    var showPermission by remember { mutableStateOf(false) }
                    Card(
                        colors = CardDefaults.cardColors(Color(0xFFD9D9D9)),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .align(Alignment.CenterHorizontally)
                            .fillMaxWidth()
                            .clickable {
                                val permission = Manifest.permission.CAMERA
                                when {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        permission
                                    ) == PackageManager.PERMISSION_GRANTED -> {
                                        Toast
                                            .makeText(
                                                context,
                                                "Permission is already granted\nThank you!",
                                                Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    }

                                    else -> {
                                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                                context as Activity,
                                                permission
                                            )
                                        ) {
                                            requestPermissionLauncher.launch(permission)
                                        } else {

                                            showPermission = true
                                        }
                                    }
                                }
                            },

                        ) {
                        Text(
                            "Request Camera Permission",
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.CenterHorizontally),
                            color = Color(0xFF595959),
                            style = Typography.bodySmall
                        )
                    }
                    Button(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(top = 72.dp)
                            .shadow(
                                elevation = 3.dp,
                                shape = MaterialTheme.shapes.extraLarge,
                                clip = true
                            )
                            .fillMaxWidth(0.6f)
                            .align(Alignment.CenterHorizontally),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(Turquoise)
                    ) {
                        Text(
                            "Exit",
                            modifier = Modifier.padding(8.dp),
                            color = Color.White,
                            style = Typography.bodySmall
                        )
                    }
                    if (showPermission) {
                        ShowAlertDialog(
                            showDialog = showPermission,
                            onDismiss = { showPermission = false },
                            onConfirm = {

                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                val uri = Uri.fromParts("package", context.packageName, null)
                                intent.data = uri
                                context.startActivity(intent)
                            }
                        )
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
                                    updateNicknameInDatabase(
                                        sharedPrefHelper.getID()!!.toInt(),
                                        newNickname
                                    )
                                    showDialog = false
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    sharedPrefHelper.saveNickname(newNickname)
                                    updateNicknameInDatabase(
                                        sharedPrefHelper.getID()!!.toInt(),
                                        newNickname
                                    )
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
                    text = { Text("You currently have no skins") },
                    confirmButton = {
                        Button(onClick = { showToDoDialog = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }


        DisposableEffect(Unit) {

            val nicknameChangeListener = object : ValueEventListener {

                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val newNick = dataSnapshot.getValue(String::class.java)

                    if (newNick != null) {
                        databaseRef.child("rooms").child(lobbyId.toString()).child("users")
                            .child(sharedPrefHelper.getID().toString()).child("name")
                            .setValue(newNick)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Log the error
                }
            }

            databaseRef.child("users").child(sharedPrefHelper.getID().toString()).child("name")
                .addValueEventListener(nicknameChangeListener)

            onDispose {

                databaseRef.child("users").child(sharedPrefHelper.getID().toString()).child("name")
                    .removeEventListener(nicknameChangeListener)

            }
        }
    }
}