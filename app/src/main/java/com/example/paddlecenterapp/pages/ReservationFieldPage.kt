package com.example.paddlecenterapp.pages

import android.util.Log
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
import com.example.paddlecenterapp.services.AddAvailabilityButton
import com.example.paddlecenterapp.services.AddEntityButton
import com.example.paddlecenterapp.services.getUserIdByUserObject
import com.example.paddlecenterapp.services.searchUsers
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ReservationFieldPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var selectedItem by remember { mutableIntStateOf(1) }
    var isAdmin by remember { mutableStateOf(false) }
    var fields by remember { mutableStateOf<List<Field>>(emptyList()) }
    var selectedField by remember { mutableStateOf<Field?>(null) }
    var selectedSlot by remember { mutableStateOf<Slot?>(null) }
    var participants by remember { mutableStateOf<List<Pair<String, String?>>>(List(4) { "" to null }) }
    var searchQuery by remember { mutableStateOf("") }
    var foundUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun performUserSearch(query: String) {
        coroutineScope.launch {
            val users = searchUsers(query, authViewModel, flag = true)
            foundUsers = users
        }
    }

    val currentUser = authViewModel.getCurrentUser()
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            authViewModel.getUserDataFromRealtimeDatabase(currentUser.uid) { user ->
                if (user != null) {
                    val userFullName = "${user.firstName} ${user.lastName}"
                    participants = participants.toMutableList().apply {
                        this[0] = userFullName to currentUser.uid
                    }
                }

                if (user != null) {
                    isAdmin = user.admin
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        database.child("fields").get().addOnSuccessListener {
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

            val fieldMap = it.getValue<Map<String, Map<String, Any>>>()
            if (fieldMap == null) {
                Log.e("ReservationFieldPage", "Failed to load fields: Data is null.")
                return@addOnSuccessListener
            }

            val fieldList = fieldMap.map { (key, value) ->
                val availability = (value["availability"] as? Map<String, Map<String, Any>>)?.mapNotNull { entry ->
                    val date = entry.value["date"] as? String
                    val status = entry.value["status"] as? Boolean ?: false
                    if (date != null && status) {
                        val slotDate = LocalDateTime.parse(date, formatter)
                        if (slotDate.isAfter(now)) {
                            entry.key to Slot(date, true)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }?.toMap()?.toList()?.sortedBy { LocalDateTime.parse(it.second.date, formatter) }?.toMap() ?: emptyMap()

                Field(key, value["name"] as? String ?: "Unknown", availability)
            }

            fields = fieldList
        }.addOnFailureListener { exception ->
            Log.e("ReservationFieldPage", "Failed to load fields: ${exception.message}")
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
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Booking Field", fontSize = 32.sp)

            Spacer(modifier = Modifier.height(22.dp))

            if (selectedField == null) {
                Text("Select Field")

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(fields) { field ->
                        OutlinedButton(
                            onClick = { selectedField = field },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Text(field.name)
                        }
                    }
                }

                if (isAdmin) {
                    AddEntityButton("field")
                }
            }

            selectedField?.let { field ->
                val availableSlots = field.availability.values.filter { it.status }

                if (selectedSlot == null) {
                    Text("Select Slot for ${field.name}")
                }
                if (availableSlots.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No available slots for ${field.name}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    if (selectedSlot == null) {
                        LazyColumn {
                            items(availableSlots) { slot ->
                                OutlinedButton(
                                    onClick = { selectedSlot = slot },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                ) {
                                    Text(slot.date)
                                }
                            }
                        }
                    }
                }

                if (selectedSlot == null && isAdmin) {
                    AddAvailabilityButton(field.id, "field")
                }

                selectedSlot?.let {
                    Text("${field.name} (${selectedSlot!!.date})")
                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            performUserSearch(it)
                        },
                        label = { Text("Search for Users") },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(foundUsers) { user ->
                            OutlinedButton(
                                onClick = {
                                    val userFullName = "${user.firstName} ${user.lastName}"
                                    getUserIdByUserObject(user) { userId ->
                                        if (userId != null) {
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
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            enabled = false
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
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
                        modifier = Modifier.padding(8.dp).fillMaxWidth()
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
