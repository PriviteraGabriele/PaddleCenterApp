package com.example.paddlecenterapp.pages

import android.annotation.SuppressLint
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
import com.example.paddlecenterapp.BottomNavigationBar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch

// Data model to represent a Coach
data class Coach(val id: String, val name: String, val availability: Map<String, Slot>)
data class Slot(val date: String, val status: Boolean)

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ReservationLessonPage(modifier: Modifier = Modifier, navController: NavController) {
    var selectedItem by remember { mutableIntStateOf(2) }
    var coaches by remember { mutableStateOf<List<Coach>>(emptyList()) }
    var selectedCoach by remember { mutableStateOf<Coach?>(null) }
    var selectedSlot by remember { mutableStateOf<Slot?>(null) }
    val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }
    var reservationSuccess by remember { mutableStateOf(false) }

    // Coroutine scope to launch coroutines
    val coroutineScope = rememberCoroutineScope()

    // Fetch coaches data from Firebase
    LaunchedEffect(Unit) {
        database.child("coaches").get().addOnSuccessListener {
            val coachMap = it.getValue<Map<String, Map<String, Any>>>()
            if (coachMap != null) {
                val coachList = coachMap.map { (key, value) ->
                    val availability = (value["availability"] as Map<String, Map<String, Any>>).mapValues { entry ->
                        Slot(entry.value["date"] as String, entry.value["status"] as Boolean)
                    }
                    Coach(key, value["name"] as String, availability)
                }
                coaches = coachList
            } else {
                Log.e("ReservationLessonPage", "Failed to load coaches.")
            }
        }
    }

    // Function to save reservation and update slot status
    fun saveReservation(coachId: String, slotDate: String) {
        val reservationData = mapOf(
            "userId" to userId,
            "coachId" to coachId,
            "slotDate" to slotDate
        )

        // Save reservation data under "reservations" node in Firebase
        val reservationId = database.child("reservations").child("lessons").push().key ?: return
        database.child("reservations").child("lessons").child(reservationId).setValue(reservationData).addOnSuccessListener {
            // Once reservation is saved, update the slot status to false
            val slotKey = selectedCoach?.availability?.entries?.find { it.value.date == slotDate }?.key
            if (slotKey != null) {
                database.child("coaches").child(coachId).child("availability").child(slotKey).child("status")
                    .setValue(false).addOnSuccessListener {
                        Log.d("ReservationLessonPage", "Slot successfully updated to false.")
                        // Indicate reservation success
                        reservationSuccess = true
                    }.addOnFailureListener {
                        Log.e("ReservationLessonPage", "Failed to update slot status.")
                    }
            }
        }.addOnFailureListener {
            Log.e("ReservationLessonPage", "Failed to save reservation.")
        }
    }

    // Show the Snackbar when reservation is successful
    if (reservationSuccess) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Reservation successfully saved!")
        }
        reservationSuccess = false // Reset the success flag after showing the Snackbar
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Reservation Lesson Page", fontSize = 32.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // Show coach selection
            Text("Select Coach")
            LazyColumn {
                items(coaches) { coach ->
                    Button(
                        onClick = { selectedCoach = coach },
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text(coach.name)
                    }
                }
            }

            // Show available slots if a coach is selected
            if (selectedCoach != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Slot for ${selectedCoach!!.name}")

                LazyColumn {
                    items(selectedCoach!!.availability.values.toList()) { slot ->
                        if (slot.status) { // Only show available slots
                            Button(
                                onClick = { selectedSlot = slot },
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            ) {
                                Text("Date: ${slot.date}")
                            }
                        }
                    }
                }
            }

            // Display selected slot and button to confirm reservation
            selectedSlot?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Selected Slot: ${it.date}")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        selectedCoach?.let { coach ->
                            saveReservation(coach.id, it.date)
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
