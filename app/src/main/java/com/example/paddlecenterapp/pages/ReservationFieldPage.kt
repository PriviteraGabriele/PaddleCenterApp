package com.example.paddlecenterapp.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.BottomNavigationBar
import com.example.paddlecenterapp.models.Field
import com.example.paddlecenterapp.models.Slot
import com.example.paddlecenterapp.models.User
import com.example.paddlecenterapp.services.getUserIdByUserObject
import com.example.paddlecenterapp.services.searchUsers
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ReservationFieldPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var selectedItem by remember { mutableIntStateOf(1) }
    var fields by remember { mutableStateOf<List<Field>>(emptyList()) }
    var selectedField by remember { mutableStateOf<Field?>(null) }
    var selectedSlot by remember { mutableStateOf<Slot?>(null) }
    var participants by remember { mutableStateOf<List<Pair<String, String?>>>(List(4) { "" to null }) } // Lista di nome -> ID
    var searchQuery by remember { mutableStateOf("") }
    var foundUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Funzione per cercare utenti
    fun performUserSearch(query: String) {
        coroutineScope.launch {
            val users = searchUsers(query, authViewModel, flag = true)
            foundUsers = users
        }
    }

    // Ottieni l'utente autenticato
    val currentUser = authViewModel.getCurrentUser() // supponiamo che tu abbia una proprietà `currentUser` nel view model
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            authViewModel.getUserDataFromRealtimeDatabase(currentUser.uid) { user ->
                if (user != null) {
                    val userFullName = "${user.firstName} ${user.lastName}"
                    participants = participants.toMutableList().apply {
                        this[0] = userFullName to currentUser.uid
                    }
                }
            }
        }
    }

    // Fetch fields data from Firebase
    LaunchedEffect(Unit) {
        database.child("fields").get().addOnSuccessListener {
            val fieldMap = it.getValue<Map<String, Map<String, Any>>>()
            if (fieldMap != null) {
                val fieldList = fieldMap.map { (key, value) ->
                    val availability = (value["availability"] as Map<String, Map<String, Any>>).mapValues { entry ->
                        Slot(entry.value["date"] as String, entry.value["status"] as Boolean)
                    }
                    Field(key, value["name"] as String, availability)
                }
                fields = fieldList
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) } // Configurazione notifiche
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Booking Field", fontSize = 32.sp)

            Spacer(modifier = Modifier.height(22.dp))

            if (selectedField == null) {
                Text("Select Field")
                LazyColumn {
                    items(fields) { field ->
                        Button(
                            onClick = { selectedField = field },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Text(field.name)
                        }
                    }
                }
            }

            selectedField?.let { field ->
                // Filtra gli slot disponibili
                val availableSlots = field.availability.values.filter { it.status }

                // Mostra gli slot disponibili
                Text("Select Slot for ${field.name}")

                if (availableSlots.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Nessun slot disponibile
                    Text(
                        text = "No available slots for ${field.name}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    LazyColumn {
                        items(availableSlots) { slot ->
                            Button(
                                onClick = { selectedSlot = slot },
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            ) {
                                Text(slot.date)
                            }
                        }
                    }
                }

                selectedSlot?.let {
                    Text("${field.name} (${selectedSlot!!.date})")
                    Spacer(modifier = Modifier.height(16.dp))

                    // Ricerca utenti
                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            performUserSearch(it)
                        },
                        label = { Text("Search for Users") },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )

                    LazyColumn {
                        items(foundUsers) { user ->
                            Button(
                                onClick = {
                                    val userFullName = "${user.firstName} ${user.lastName}"
                                    getUserIdByUserObject(user) { userId ->
                                        if (userId != null) {
                                            // Controlla se l'ID dell'utente è già presente nella lista dei partecipanti
                                            if (participants.any { it.second == userId }) {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("This user is already a participant.")
                                                }
                                            } else {
                                                val nextEmptyIndex = participants.indexOfFirst { it.first.isEmpty() }
                                                if (nextEmptyIndex != -1) {
                                                    participants = participants.toMutableList().apply {
                                                        this[nextEmptyIndex] = userFullName to userId
                                                    }
                                                } else {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("All participant slots are filled!")
                                                    }
                                                }
                                            }
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Failed to fetch user ID.")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            ) {
                                Text("${user.firstName} ${user.lastName}")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mostra partecipanti (nomi)
                    Text("Participants")
                    participants.forEachIndexed { index, participant ->
                        TextField(
                            value = participant.first,
                            onValueChange = { updatedValue ->
                                participants = participants.toMutableList().apply {
                                    this[index] = updatedValue to this[index].second
                                }
                            },
                            label = {
                                Text(
                                    if (index == 0) "Participant ${index + 1} (Me)"
                                    else "Participant ${index + 1}"
                                )
                            },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            selectedField?.let { field ->
                                saveReservation(
                                    field.id,
                                    selectedSlot!!.date,
                                    participants,
                                    database,
                                    snackbarHostState,
                                    coroutineScope
                                )
                            }
                        },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Confirm Reservation")
                    }
                }
            }
        }
    }
}

// Funzione per salvare la prenotazione e aggiornare lo stato dello slot
fun saveReservation(
    fieldId: String,
    slotDate: String,
    participants: List<Pair<String, String?>>, // Lista di nome -> ID
    database: DatabaseReference,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    if (participants.any { it.first.isEmpty() || it.second == null }) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("All participant slots must be filled with valid users!")
        }
        return
    }

    val reservationData = mapOf(
        "participants" to participants.map { it.second }, // Salva solo gli ID
        "fieldId" to fieldId,
        "slotDate" to slotDate
    )

    // Recupera tutti gli slot per il campo
    database.child("fields").child(fieldId).child("availability").get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Trova lo slot con la data corrispondente
                val slotEntry = snapshot.children.firstOrNull { slotSnapshot ->
                    val date = slotSnapshot.child("date").getValue(String::class.java)
                    date == slotDate
                }

                val slotKey = slotEntry?.key
                if (slotKey != null) {
                    // Aggiorna lo stato dello slot a `false` (non disponibile)
                    database.child("fields").child(fieldId).child("availability").child(slotKey)
                        .child("status").setValue(false).addOnSuccessListener {
                            // Dopo aver aggiornato lo stato dello slot, salva la prenotazione
                            val reservationId = database.child("reservations").child("fields").push().key
                            if (reservationId != null) {
                                database.child("reservations").child("fields").child(reservationId).setValue(reservationData)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Reservation confirmed!")
                                }
                            }
                        }
                }
            }
        }
}
