package com.example.paddlecenterapp.pages

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.BottomNavigationBar
import com.example.paddlecenterapp.R
import com.example.paddlecenterapp.models.User
import com.example.paddlecenterapp.services.ReportDialog
import com.example.paddlecenterapp.services.addFriendToBothUsers
import com.example.paddlecenterapp.services.addReport
import com.example.paddlecenterapp.services.removeFriendFromBothUsers
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.paddlecenterapp.services.banUser
import com.example.paddlecenterapp.services.checkFriendship
import com.example.paddlecenterapp.services.unbanUser

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun UserDetailsPage(
    modifier: Modifier = Modifier,
    userId: String,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var selectedItem by remember { mutableIntStateOf(2) }
    val currentUser = authViewModel.getCurrentUser()

    // Stato per controllare se l'utente è admin
    var isAdmin by remember { mutableStateOf(false) }
    var buttonTextFriend by remember { mutableStateOf("Add Friend") }
    var buttonTextBan by remember { mutableStateOf("Ban User") }
    var showDialogFriend by remember { mutableStateOf(false) }
    var showDialogReport by remember { mutableStateOf(false) }
    var showDialogBan by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf<User?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val friends = remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    // Stato per controllare il messaggio del Snackbar
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Mostra Snackbar quando `snackbarMessage` viene impostato
    snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null // Reset del messaggio dopo la visualizzazione
        }
    }

    // Carica i dati dell'utente autenticato
    LaunchedEffect(currentUser) {
        currentUser?.let {
            authViewModel.getUserDataFromRealtimeDatabase(it.uid) { userData ->
                if (userData != null) {
                    isAdmin = userData.admin // Aggiorna lo stato correttamente
                }
            }
        }
    }

    // Ottieni i dati dell'utente tramite l'ID passato e controllo se l'user è bannato o meno
    LaunchedEffect(userId) {
        authViewModel.getUserDataFromRealtimeDatabase(userId) { retrievedUser ->
            if (retrievedUser != null) {
                user = retrievedUser

                // Verifica se l'utente è bannato
                val isBanned = retrievedUser.banned
                Log.d("Ban", "User: $retrievedUser")
                // Log per monitorare lo stato del ban
                Log.d("Ban", "User banned status: $isBanned")

                // Imposta il testo del bottone per il ban
                buttonTextBan = if (isBanned) "Unban User" else "Ban User"
            } else {
                Log.e("UserDetailsPage", "Error retrieving user data")
            }
        }
    }

    // controllo dell'amicizia tra i due utenti
    LaunchedEffect(currentUser, userId) {
        if (currentUser != null) {
            Log.d("FriendshipCheck", "MainUser: ${currentUser.uid}")
            authViewModel.getUserDataFromRealtimeDatabase(currentUser.uid) { cUser ->
                if (cUser != null) {
                    cUser.friends?.let {
                        // Log per monitorare gli amici dell'utente
                        Log.d("FriendshipCheck", "MainUser friends: $it")
                        friends.value = it

                        // Log per monitorare l'ID dell'utente cercato
                        Log.d("FriendshipCheck", "User ID of searched user: $userId")

                        // Verifica se sono amici
                        checkFriendship(userId, it) { isFriend ->
                            // Log per monitorare se gli utenti sono amici
                            Log.d("FriendshipCheck", "Are they friends? $isFriend")

                            // Imposta il testo del bottone per amici
                            buttonTextFriend = if (isFriend) "Remove Friend" else "Add Friend"
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                navController = navController
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(enabled = true, state = rememberScrollState())
                .padding(end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "User Details Page",
                fontSize = 32.sp,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Mostra i dettagli utente
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
                    .padding(6.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_picture),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "${user?.firstName} ${user?.lastName}", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            var bio = user?.bio ?: ""
            var ranking = user?.ranking ?: ""
            val banned = user?.banned

            if (isAdmin){
                if (banned == true){
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "[Banned]",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red // Imposta il colore del testo
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

            }

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                enabled = false,
                modifier = Modifier
                    .widthIn(min = 80.dp, max = 320.dp)
                    .heightIn(min = 56.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ranking,
                onValueChange = { ranking = it },
                label = { Text("Ranking") },
                enabled = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottone Aggiungi/Rimuovi Amico
            Button(
                onClick = {
                    if (buttonTextFriend == "Remove Friend") {
                        // Mostra il dialog per rimuovere l'amico
                        showDialogFriend = true
                    } else {
                        addFriendToBothUsers(userId) { success ->
                            coroutineScope.launch {
                                snackbarMessage = if (success) {
                                    "Friend added successfully!"
                                } else {
                                    "Error in adding friend."
                                }
                            }
                            buttonTextFriend =
                                "Remove Friend"  // Cambia il testo del bottone per indicare che sono amici
                        }
                    }
                }
            ) {
                Text(buttonTextFriend)
            }

            // Dialog per confermare la rimozione dell'amico
            if (showDialogFriend) {
                AlertDialog(
                    onDismissRequest = { showDialogFriend = false },
                    title = { Text("Remove Friend") },
                    text = { Text("Are you sure you want to remove this friend?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                removeFriendFromBothUsers(userId) { success ->
                                    coroutineScope.launch {
                                        snackbarMessage = if (success) {
                                            "Friend removed successfully!"
                                        } else {
                                            "Error in removing friend."
                                        }
                                    }
                                    buttonTextFriend =
                                        "Add Friend"  // Cambia il testo del bottone per indicare che non sono più amici
                                }
                                showDialogFriend = false
                            }
                        ) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDialogFriend = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showDialogReport = true },
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
            ) {
                Text("Report User")
            }

            Spacer(modifier = Modifier.width(8.dp)) // Aggiungi uno spazio tra i due pulsanti

            if (showDialogReport) {
                ReportDialog(
                    onDismiss = { showDialogReport = false },
                    onReport = { reason ->
                        val reportedById = currentUser?.uid ?: ""

                        if (reportedById.isNotEmpty()) {
                            addReport(
                                context = context,
                                reportedUserId = userId,
                                reportedById = reportedById,
                                reason = reason
                            )
                        }
                    }
                )
            }

            // Mostra pulsanti solo se l'utente è admin
            if (isAdmin) {
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = {
                    // Mostra i report dell'utente
                    //navController.navigate("UserReports/$userId")
                }) {
                    Text("View Reports")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (buttonTextBan == "Ban User") {
                            showDialogBan = true
                        } else {
                            unbanUser(userId) { success ->
                                coroutineScope.launch {
                                    snackbarMessage = if (success) {
                                        "User unbanned successfully!"
                                    } else {
                                        "Error unbanning user."
                                    }
                                }
                                buttonTextBan =
                                    "Uban User"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
                ) {
                    Text(buttonTextBan)
                }

                // Dialog per confermare la rimozione dell'amico
                if (showDialogBan) {
                    AlertDialog(
                        onDismissRequest = { showDialogBan = false },
                        title = { Text("Ban User") },
                        text = { Text("Are you sure you want to ban this user?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    banUser(userId) { success ->
                                        coroutineScope.launch {
                                            snackbarMessage = if (success) {
                                                "User banned successfully!"
                                            } else {
                                                "Error during user ban"
                                            }
                                        }
                                        buttonTextBan =
                                            "Unban User"  // Cambia il testo del bottone per indicare che non sono più amici
                                    }
                                    showDialogBan = false
                                }
                            ) {
                                Text("Yes")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDialogBan = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}