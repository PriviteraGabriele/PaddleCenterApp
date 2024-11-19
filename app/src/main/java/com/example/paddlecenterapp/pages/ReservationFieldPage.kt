package com.example.paddlecenterapp.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paddlecenterapp.BottomNavigationBar
import com.example.paddlecenterapp.models.Field
import com.example.paddlecenterapp.models.Slot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import kotlinx.coroutines.launch

@Composable
fun ReservationFieldPage(modifier: Modifier = Modifier, navController: NavController) {
    var selectedItem by remember { mutableIntStateOf(0) }
    var fields by remember { mutableStateOf<List<Field>>(emptyList()) }
    var selectedField by remember { mutableStateOf<Field?>(null) }
    var selectedSlot by remember { mutableStateOf<Slot?>(null) }
    var participants by remember { mutableStateOf<List<String>>(List(4) { "" }) }
    val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    var reservationSuccess by remember { mutableStateOf(false) }

    // Coroutine scope to launch coroutines
    val coroutineScope = rememberCoroutineScope()

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

    // Function to save reservation and update slot status
    fun saveReservation(fieldId: String, slotDate: String) {
        if (participants.any { it.isEmpty() }) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("All participant names must be filled!")
            }
            return
        }

        val reservationData = mapOf(
            "participants" to participants,
            "fieldId" to fieldId,
            "slotDate" to slotDate
        )

        val reservationId = database.child("reservations").child("fields").push().key ?: return
        database.child("reservations").child("fields").child(reservationId).setValue(reservationData).addOnSuccessListener {
            val slotKey = selectedField?.availability?.entries?.find { it.value.date == slotDate }?.key
            if (slotKey != null) {
                database.child("fields").child(fieldId).child("availability").child(slotKey).child("status")
                    .setValue(false).addOnSuccessListener {
                        reservationSuccess = true
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
            Text(text = "Reservation Field", fontSize = 32.sp)

            Spacer(modifier = Modifier.height(16.dp))

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
                if (selectedSlot == null) {
                    Text("Select Slot for ${field.name}")
                    LazyColumn {
                        items(field.availability.values.toList()) { slot ->
                            if (slot.status) {
                                Button(
                                    onClick = { selectedSlot = slot },
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                ) {
                                    Text(slot.date)
                                }
                            }
                        }
                    }
                } else {
                    Text("Selected Field: ${field.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Selected Slot: ${selectedSlot!!.date}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Enter Participants")

                    participants.forEachIndexed { index, participant ->
                        TextField(
                            value = participant,
                            onValueChange = { updatedValue ->
                                participants = participants.toMutableList().apply { this[index] = updatedValue }
                            },
                            label = { Text("Participant ${index + 1}") },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            selectedField?.let { field ->
                                saveReservation(field.id, selectedSlot!!.date)
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




