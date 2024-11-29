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
import com.example.paddlecenterapp.models.Coach
import com.example.paddlecenterapp.models.Slot
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.services.AddAvailabilityButton
import com.example.paddlecenterapp.services.AddEntityButton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun ReservationLessonPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel
) {
    var selectedItem by remember { mutableIntStateOf(3) }
    val currentUser = authViewModel.getCurrentUser()
    var isAdmin by remember { mutableStateOf(false) }
    var coaches by remember { mutableStateOf<List<Coach>>(emptyList()) }
    var selectedCoach by remember { mutableStateOf<Coach?>(null) }
    var selectedSlot by remember { mutableStateOf<Slot?>(null) }
    val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: ""
    val snackbarHostState = remember { SnackbarHostState() }
    var reservationSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Carica i dati dell'utente autenticato
    LaunchedEffect(currentUser) {
        currentUser?.let {
            authViewModel.getUserDataFromRealtimeDatabase(it.uid) { userData ->
                if (userData != null) {
                    isAdmin = userData.admin
                }
            }
        }
    }

    // Fetch dei dati dei coach dal database
    LaunchedEffect(Unit) {
        database.child("coaches").get().addOnSuccessListener {
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

            // Verifica se i dati sono nulli
            val coachMap = it.getValue<Map<String, Map<String, Any>>>()
            if (coachMap == null) {
                // Se i dati sono nulli, logghiamo un errore e usciamo dalla funzione
                Log.e("ReservationLessonPage", "Failed to load coaches: Data is null.")
                return@addOnSuccessListener
            }

            // Se i dati sono validi, proseguiamo con il parsing
            val coachList = coachMap.map { (key, value) ->
                // Filtra gli slot disponibili
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

                Coach(key, value["name"] as? String ?: "Unknown", availability)
            }

            // Aggiorna la lista dei coach
            coaches = coachList
        }.addOnFailureListener { exception ->
            Log.e("ReservationLessonPage", "Failed to load coaches: ${exception.message}")
        }
    }


    // funzione per salvare la reservation e aggiornare lo status
    fun saveReservation(coachId: String, slotDate: String) {
        val reservationData = mapOf(
            "userId" to userId,
            "coachId" to coachId,
            "slotDate" to slotDate
        )

        val reservationId = database.child("reservations").child("lessons").push().key ?: return
        database.child("reservations").child("lessons").child(reservationId).setValue(reservationData).addOnSuccessListener {
            val slotKey = selectedCoach?.availability?.entries?.find { it.value.date == slotDate }?.key
            if (slotKey != null) {
                database.child("coaches").child(coachId).child("availability").child(slotKey).child("status")
                    .setValue(false).addOnSuccessListener {
                        Log.d("ReservationLessonPage", "Slot successfully updated to false.")
                        reservationSuccess = true
                    }.addOnFailureListener {
                        Log.e("ReservationLessonPage", "Failed to update slot status.")
                    }
            }
        }.addOnFailureListener {
            Log.e("ReservationLessonPage", "Failed to save reservation.")
        }
    }

    if (reservationSuccess) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Reservation successfully saved!")
        }
        reservationSuccess = false
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
            Text(text = "Booking Lesson", fontSize = 32.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // mostra la lista dei coach per selezionarne uno
            if (selectedCoach == null) {
                Text("Select Coach")
                LazyColumn {
                    items(coaches) { coach ->
                        OutlinedButton(
                            onClick = { selectedCoach = coach },
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        ) {
                            Text(coach.name)
                        }
                    }
                }

                if(selectedCoach == null && isAdmin){
                    AddEntityButton("coach")
                }
            }

            // mostra la lista degli slot disponibili se viene selezioanto il coach
            selectedCoach?.let { coach ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Slot for ${coach.name}")

                val availableSlots = coach.availability.values.filter { it.status }

                if (availableSlots.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // mostra un messaggio nel caso non ci siano slot disponibili
                    Text(
                        text = "No slots available for ${coach.name}.",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
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

                if(isAdmin){
                    AddAvailabilityButton(coach.id, "coach")
                }
            }

            // Breve riassunto delle scelte fatte con bottone di conferma prenotazione
            selectedSlot?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Selected Slot: ${it.date}")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
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
