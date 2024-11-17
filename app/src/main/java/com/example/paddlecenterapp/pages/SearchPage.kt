package com.example.paddlecenterapp.pages

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.models.User
import com.example.paddlecenterapp.models.addFriendToCurrentUser
import com.example.paddlecenterapp.models.getUserIdByUserObject
import com.example.paddlecenterapp.BottomNavigationBar
import com.example.paddlecenterapp.models.addReport
import com.example.paddlecenterapp.models.checkFriendship
import kotlinx.coroutines.launch
import com.example.paddlecenterapp.services.searchUsers
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun SearchPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel
) {
    var selectedItem by remember { mutableIntStateOf(1) } // Ricerca è la seconda voce
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<User>>(emptyList()) }

    // SnackbarHostState per gestire i messaggi Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                navController = navController
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) } // Aggiungi il SnackbarHost
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Barra di ricerca e bottone nella stessa riga
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search users...") },
                    modifier = Modifier
                        .weight(1f)
                )

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                users = searchUsers(query, authViewModel)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Visualizzazione dei risultati
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { user ->
                    UserItem(user, snackbarHostState, authViewModel) // Passa il snackbarHostState alla funzione UserItem
                }
            }
        }
    }
}

@Composable
fun UserItem(user: User, snackbarHostState: SnackbarHostState, authViewModel: AuthViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Stato per controllare il messaggio del Snackbar
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Mostra Snackbar quando `snackbarMessage` viene impostato
    snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null // Reset del messaggio dopo la visualizzazione
        }
    }


// Stato per il testo del bottone (aggiungi o rimuovi amico)
    var buttonText by remember { mutableStateOf("+") }
    var showDialogFriend by remember { mutableStateOf(false) }

// Recupera i dati dell'utente corrente e controlla se l'utente cercato è già un amico
    val currentUser = authViewModel.getCurrentUser()

    val friends = remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    Log.d("FriendshipCheck", "UserSearched: $user")

    LaunchedEffect(currentUser, user) {
        if (currentUser != null) {
            Log.d("FriendshipCheck", "MainUser: ${currentUser.uid}")
            authViewModel.getUserDataFromRealtimeDatabase(currentUser.uid) { cUser ->
                if (cUser != null) {
                    cUser.friends?.let {
                        // Log per monitorare gli amici dell'utente
                        Log.d("FriendshipCheck", "MainUser friends: $it")
                        friends.value = it

                        // Recupera l'ID dell'utente cercato
                        getUserIdByUserObject(user) { userId ->
                            if (userId != null) {
                                // Log per monitorare l'ID dell'utente cercato
                                Log.d("FriendshipCheck", "User ID of searched user: $userId")

                                // Verifica se sono amici
                                checkFriendship(userId, it) { isFriend ->
                                    // Log per monitorare se gli utenti sono amici
                                    Log.d("FriendshipCheck", "Are they friends? $isFriend")

                                    buttonText = if (isFriend) "✓" else "+"
                                }
                            } else {
                                // Log per monitorare se non è stato trovato l'ID
                                Log.d("FriendshipCheck", "User ID not found for searched user.")

                                buttonText = "+"
                            }
                        }
                    }
                }
            }
        }
    }


    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = "${user.firstName} ${user.lastName}", fontSize = 18.sp)
            Text(text = user.email, fontSize = 14.sp)
        }

        // Bottone unico per aggiungere o rimuovere amici
        Button(
            onClick = {
                if (buttonText == "✓") {
                    // Mostra il dialog per rimuovere l'amico
                    showDialogFriend = true
                } else {
                    // Aggiungi l'amico
                    getUserIdByUserObject(user) { userId ->
                        if (userId != null) {
                            addFriendToCurrentUser(userId) { success ->
                                coroutineScope.launch {
                                    snackbarMessage = if (success) {
                                        "Friend added successfully!"
                                    } else {
                                        "Error in adding friend."
                                    }
                                }
                                buttonText = "✓"  // Cambia il testo del bottone per indicare che sono amici
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarMessage = "User not found."
                            }
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue.copy(alpha = 0.9f)),
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(buttonText, fontSize = 20.sp)
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
                            getUserIdByUserObject(user) { userId ->
                                if (userId != null) {
                                    removeFriendFromCurrentUser(userId) { success ->
                                        coroutineScope.launch {
                                            snackbarMessage = if (success) {
                                                "Friend removed successfully!"
                                            } else {
                                                "Error in removing friend."
                                            }
                                        }
                                        buttonText = "+"  // Cambia il testo del bottone per indicare che non sono più amici
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarMessage = "User not found."
                                    }
                                }
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

        Spacer(modifier = Modifier.width(12.dp)) // Aggiungi uno spazio tra i due pulsanti

        var showDialogReport by remember { mutableStateOf(false) }

        Button(
            onClick = { showDialogReport = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
            shape = CircleShape,
            modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("R", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(8.dp)) // Aggiungi uno spazio tra i due pulsanti

        if (showDialogReport) {
            ReportDialog(
                onDismiss = { showDialogReport = false },
                onReport = { reason ->
                    // Ottenere l'ID dell'utente che segnala e dell'utente segnalato
                    getUserIdByUserObject(user) { reportedUserId ->
                        val reportedById = currentUser?.uid ?: ""

                        if (reportedUserId != null && reportedById.isNotEmpty()) {
                            addReport(
                                context = context,
                                reportedUserId = reportedUserId,
                                reportedById = reportedById,
                                reason = reason
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    val reportOptions = listOf(
        "False claim of victory",
        "Unsportsmanlike behavior",
        "Offensive language"
    )

    var selectedReason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report user") },
        text = {
            Column {
                Text("Reason for reporting:")
                reportOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = option }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically // Assicura che i pallini siano allineati correttamente
                    ) {
                        RadioButton(
                            selected = selectedReason == option,
                            onClick = { selectedReason = option }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            option,
                            modifier = Modifier.align(Alignment.CenterVertically) // Centra solo il testo
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedReason.isNotEmpty()) {
                        onReport(selectedReason)
                        onDismiss()
                    }
                }
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun removeFriendFromCurrentUser(friendId: String, onResult: (Boolean) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser != null) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)

        // Rimuovi l'ID del nuovo amico dalla lista degli amici
        userRef.child("friends").child(friendId).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Rimuovi anche il riferimento inverso nella lista dell'amico
                FirebaseDatabase.getInstance().getReference("users").child(friendId)
                    .child("friends").child(currentUser.uid).removeValue().addOnCompleteListener { task2 ->
                        onResult(task2.isSuccessful)
                    }
            } else {
                onResult(false)
            }
        }
    } else {
        onResult(false)
    }
}
