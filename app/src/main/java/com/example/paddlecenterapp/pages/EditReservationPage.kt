package com.example.paddlecenterapp.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
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
fun EditReservationPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    reservationId: String
) {
    var selectedItem by remember { mutableIntStateOf(0) }
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
            }
        }
    }

    LaunchedEffect(Unit) {
        database.child("fields").get().addOnSuccessListener { snapshot ->
            val fieldMap = snapshot.getValue<Map<String, Map<String, Any>>>()
            if (fieldMap != null) {
                val fieldList = fieldMap.map { (key, value) ->
                    val availability = (value["availability"] as? Map<String, Map<String, Any>>)?.mapValues { entry ->
                        Slot(
                            date = entry.value["date"] as? String ?: "",
                            status = entry.value["status"] as? Boolean ?: false
                        )
                    } ?: emptyMap()
                    Field(id = key, name = value["name"] as? String ?: "Unknown", availability = availability)
                }
                fields = fieldList
            } else {
                // Gestione del caso in cui il campo è nullo
                fields = emptyList()
            }
        }

        database.child("reservations").child("fields").child(reservationId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val reservationData = snapshot.getValue<Map<String, Any>>()
                if (reservationData != null) {
                    val participantsList = reservationData["participants"] as? List<String>
                    participantsList?.let { participantIds ->
                        participantIds.forEachIndexed { index, userId ->
                            database.child("users").child(userId).get().addOnSuccessListener { userSnapshot ->
                                val userName = "${userSnapshot.child("firstName").value as? String ?: "Unknown"} ${userSnapshot.child("lastName").value as? String ?: "User"}"
                                participants = participants.toMutableList().apply {
                                    this[index] = userName to userId
                                }
                            }
                        }
                    }
                    val fieldId = reservationData["fieldId"] as? String
                    val slotDate = reservationData["slotDate"] as? String
                    selectedField = fields.find { it.id == fieldId }
                    selectedSlot = selectedField?.availability?.values?.find { it.date == slotDate }
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
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Edit Reservation", fontSize = 32.sp)

            Spacer(modifier = Modifier.height(22.dp))

            selectedField?.let { field ->
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

                    LazyColumn {
                        items(foundUsers) { user ->
                            Button(
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
                        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextField(
                                value = participant.first,
                                onValueChange = { updatedValue ->
                                    participants = participants.toMutableList().apply {
                                        this[index] = updatedValue to this[index].second
                                    }
                                },
                                label = { Text("Participant ${index + 1}") },
                                modifier = Modifier.weight(1f),
                                enabled = false // Disabilita la modifica manuale
                            )
                            IconButton(
                                onClick = {
                                    participants = participants.toMutableList().apply {
                                        this[index] = "" to null
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Remove Participant")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (participants.any { it.first.isEmpty() }) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("All 4 participants must be selected.")
                                }
                            } else {
                                selectedField?.let { field ->
                                    saveReservation(
                                        field.id,
                                        selectedSlot?.date ?: "",
                                        participants.map { it.second ?: "" },
                                        reservationId,
                                        database,
                                        snackbarHostState,
                                        coroutineScope,
                                        navController
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

fun saveReservation(
    fieldId: String,
    slotDate: String,
    participants: List<String>,
    reservationId: String,
    database: DatabaseReference,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    navController: NavController
) {
    val reservationUpdates = mapOf(
        "fieldId" to fieldId,
        "slotDate" to slotDate,
        "participants" to participants
    )

    database.child("reservations").child("fields").child(reservationId).updateChildren(reservationUpdates)
        .addOnSuccessListener {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Reservation updated successfully!")
                navController.popBackStack() // Torna alla home dopo il salvataggio
            }
        }
        .addOnFailureListener {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Failed to update reservation.")
            }
        }
}
