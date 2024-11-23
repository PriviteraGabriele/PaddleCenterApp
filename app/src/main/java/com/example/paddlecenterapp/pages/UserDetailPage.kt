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
import androidx.compose.ui.platform.LocalContext
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
import com.example.paddlecenterapp.services.getUserIdByUserObject
import com.example.paddlecenterapp.services.removeFriendFromBothUsers
import kotlinx.coroutines.launch
import androidx.compose.runtime.*

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun UserDetailsPage(modifier: Modifier = Modifier, userId: String, navController: NavController, authViewModel: AuthViewModel) {
    var selectedItem by remember { mutableIntStateOf(2) }
    val context = LocalContext.current
    val currentUser = authViewModel.getCurrentUser()

    // Stato per il testo del bottone (aggiungi o rimuovi amico)
    var buttonText by remember { mutableStateOf("Add Friend") }
    var showDialogFriend by remember { mutableStateOf(false) }
    var showDialogReport by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf<User?>(null) }

    // Snackbar per messaggi di conferma
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Ottieni i dati dell'utente tramite l'ID passato
    LaunchedEffect(userId) {
        // Assumiamo che `authViewModel.getUserDataFromRealtimeDatabase` ritorni i dati dell'utente per il `userId` fornito
        authViewModel.getUserDataFromRealtimeDatabase(userId) { retrievedUser ->
            if (retrievedUser != null) {
                user = retrievedUser
            } else {
                Log.e("UserDetailsPage", "Error retrieving user data")
            }
        }
    }

    // Funzione per mostrare la Snackbar
    fun showSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
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
        Column( modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(enabled = true, state = rememberScrollState()) // Scroll abilita
            .padding(end = 4.dp), // Aggiungi padding per la scrollbar
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "User Details Page",
                fontSize = 32.sp,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp) // Aumentata la dimensione per un look più prominente
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
                    .padding(6.dp) // Maggiore spaziatura per l'immagine
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_picture), // Usa l'immagine caricata
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(110.dp) // Aggiusta la dimensione dell'immagine
                        .clip(CircleShape), // Ritaglia l'immagine a forma di cerchio
                    contentScale = ContentScale.Crop // Adatta l'immagine al contenitore
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "${user?.firstName} ${user?.lastName}", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            var bio = user?.bio ?: ""
            var ranking = user?.ranking ?: ""

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
            Button(onClick = {
                if (buttonText == "Remove Friend") {
                    // Mostra il dialog per rimuovere l'amico
                    showDialogFriend = true
                } else {
                    // Aggiungi l'amico
                    getUserIdByUserObject(user!!) { userId ->
                        if (userId != null) {
                            addFriendToBothUsers(userId) { success ->
                                showSnackbar(
                                    if (success) "Friend added successfully!" else "Error adding friend"
                                )
                                buttonText = "Remove Friend"  // Cambia il testo del bottone per indicare che sono amici
                            }
                        }
                    }
                }
            }) {
                Text(buttonText)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottone per segnalare l'utente
            Button(
                onClick = { showDialogReport = true },
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
            ) {
                Text("Report")
            }

            // Mostra il dialog per il report
            if (showDialogReport) {
                ReportDialog(
                    onDismiss = { showDialogReport = false },
                    onReport = { reason ->
                        // Gestisci la segnalazione
                        getUserIdByUserObject(user!!) { reportedUserId ->
                            val reportedById = currentUser?.uid ?: ""
                            if (reportedUserId != null && reportedById.isNotEmpty()) {
                                addReport(
                                    context = context,
                                    reportedUserId = reportedUserId,
                                    reportedById = reportedById,
                                    reason = reason
                                )
                                showSnackbar("Report submitted successfully!")
                            }
                        }
                    }
                )
            }

            // Dialog per confermare la rimozione dell'amico
            if (showDialogFriend) {
                AlertDialog(
                    onDismissRequest = { showDialogFriend = false },
                    title = { Text("Remove Friend") },
                    text = { Text("Are you sure you want to remove this friend?") },
                    confirmButton = {
                        Button(onClick = {
                            getUserIdByUserObject(user!!) { userId ->
                                if (userId != null) {
                                    removeFriendFromBothUsers(userId) { success ->
                                        showSnackbar(
                                            if (success) "Friend removed successfully!" else "Error removing friend"
                                        )
                                        buttonText = "Add Friend"  // Cambia il testo del bottone per indicare che non sono più amici
                                    }
                                }
                            }
                            showDialogFriend = false
                        }) {
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
        }
    }
}
