package com.example.paddlecenterapp.pages

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "${user.firstName} ${user.lastName}", fontSize = 18.sp)
            Text(text = user.email, fontSize = 14.sp)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Button(
                onClick = {
                    // Implementa la logica per aggiungere un amico
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
                            }
                        } else {
                            coroutineScope.launch {
                                snackbarMessage = "User not found."
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue.copy(alpha = 0.9f)),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                val currentUser = authViewModel.getCurrentUser()

                var friends by remember { mutableStateOf(mapOf<String, Boolean>()) }

                // Recupera i dati aggiuntivi da Firebase Realtime Database se l'utente è loggato
                LaunchedEffect(currentUser) {
                    if (currentUser != null) {
                        authViewModel.getUserDataFromRealtimeDatabase(currentUser.uid) { user ->
                            if (user != null) {
                                friends = user.friends!!
                            }
                        }
                    }
                }

                val textState = remember { mutableStateOf("") }

                val friendId = remember { mutableStateOf("") }
                val stringValue: String = friendId.value

                LaunchedEffect(user) {
                    getUserIdByUserObject(user) { userId ->
                        if (userId != null) {
                            friendId.value = userId
                        }
                    }
                }

                checkFriendship(stringValue, friends) { isFriend ->
                    // Aggiorna lo stato di textState
                    textState.value = if (isFriend) "✓" else "+"
                }

                // Usa textState per mostrare il testo all'esterno della callback
                Text(textState.value, fontSize = 20.sp)
            }

            var showDialog by remember { mutableStateOf(false) }

            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("R", fontSize = 20.sp)
            }

            if (showDialog) {
                ReportDialog(
                    onDismiss = { showDialog = false },
                    onReport = { reason ->
                        // Ottenere l'ID dell'utente che segnala e dell'utente segnalato
                        val currentUser = authViewModel.getCurrentUser()
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
