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
import com.example.paddlecenterapp.BottomNavigationBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val database = FirebaseDatabase.getInstance().getReference("reservations")
    val currentUserId = auth.currentUser?.uid
    var activeReservations by remember { mutableStateOf(listOf<Reservation>()) }

    // Fetch reservations
    LaunchedEffect(Unit) {
        fetchReservations(database, currentUserId) { reservations ->
            activeReservations = reservations
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedItem = 0,
                onItemSelected = {},
                navController = navController
            )
        }
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Prenotazioni Attive",
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(activeReservations) { reservation ->
                    ReservationItem(reservation = reservation)
                }
            }
        }
    }
}

@Composable
fun ReservationItem(reservation: Reservation) {
    var fieldName by remember { mutableStateOf(reservation.fieldName) }  // Gestione del fieldName

    // Se il fieldName è null, carica dal database
    LaunchedEffect(reservation.fieldId) {
        if (fieldName == null && reservation.fieldId != null) {
            // Recupera il fieldName dal database usando fieldId
            getFieldNameFromDatabase(reservation.fieldId) { fetchedFieldName ->
                fieldName = fetchedFieldName
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        onClick = {
            // Navigazione per modificare la prenotazione
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Type: ${reservation.type}", fontSize = 16.sp)
            Text("Date: ${reservation.slotDate}", fontSize = 16.sp)
            if (reservation.type == "field") {
                Text("Field: ${fieldName ?: "Loading..."}", fontSize = 16.sp)  // Mostra il fieldName o un messaggio di caricamento
                ReservationParticipants(participants = reservation.participants)
            } else {
                Text("Coach: ${reservation.coachId}", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ReservationParticipants(participants: List<String>) {
    // Lista mutable per i nomi dei partecipanti
    val participantNames = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(true) } // Stato di caricamento

    // Effetto per caricare i nomi dei partecipanti
    LaunchedEffect(participants) {
        participantNames.clear()  // Pulisci i nomi precedenti
        isLoading = true

        // Recupera i nomi dei partecipanti
        participants.forEach { userId ->
            getUserNameAndSurname(userId) { firstName, lastName ->
                if (firstName != null && lastName != null) {
                    participantNames.add("$firstName $lastName")
                } else {
                    participantNames.add("Unknown User")
                }
            }
        }

        // Dopo aver caricato tutti i partecipanti, imposta isLoading a false
        isLoading = false
    }

    // Se stiamo ancora caricando, mostriamo un messaggio di caricamento
    if (isLoading) {
        Text("Loading participants...", fontSize = 14.sp)
    } else {
        // Mostra i partecipanti una volta caricati
        Text("Participants: ${participantNames.joinToString(", ")}", fontSize = 14.sp)
    }
}

// Funzione per ottenere il fieldName dato un fieldId
fun getFieldNameFromDatabase(fieldId: String, callback: (String) -> Unit) {
    val database = FirebaseDatabase.getInstance()
    val fieldRef = database.reference.child("fields").child(fieldId)

    fieldRef.child("name").get().addOnSuccessListener { snapshot ->
        val fieldName = snapshot.getValue(String::class.java)
        callback(fieldName ?: "Unknown Field")
    }.addOnFailureListener {
        callback("Error retrieving field name")
    }
}

// Funzione per fetchare le prenotazioni attive
fun fetchReservations(
    database: DatabaseReference,
    userId: String?,
    onResult: (List<Reservation>) -> Unit
) {
    if (userId == null) return

    database.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val now = LocalDateTime.now()
            val reservations = mutableListOf<Reservation>()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

            // Fields
            snapshot.child("fields").children.forEach { child ->
                val id = child.key ?: return@forEach
                val slotDate = child.child("slotDate").getValue(String::class.java) ?: return@forEach
                val participants = child.child("participants").children.mapNotNull { it.getValue(String::class.java) }
                val fieldId = child.child("fieldId").getValue(String::class.java)

                if (participants.contains(userId) && LocalDateTime.parse(slotDate, formatter).isAfter(now)) {
                    reservations.add(
                        Reservation(
                            id = id,
                            type = "field",
                            slotDate = slotDate,
                            participants = participants,
                            fieldId = fieldId
                        )
                    )
                }
            }

            // Lessons
            snapshot.child("lessons").children.forEach { child ->
                val lessonId = child.key ?: return@forEach
                val slotDate = child.child("slotDate").getValue(String::class.java) ?: return@forEach
                val lessonUserId = child.child("userId").getValue(String::class.java) ?: return@forEach

                if (lessonUserId == userId && LocalDateTime.parse(slotDate, formatter).isAfter(now)) {
                    reservations.add(
                        Reservation(
                            id = lessonId,
                            type = "lesson",
                            slotDate = slotDate,
                            coachId = child.child("coachId").getValue(String::class.java)
                        )
                    )
                }
            }

            onResult(reservations)
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle error
        }
    })
}

// Funzione per ottenere nome e cognome dell'utente
fun getUserNameAndSurname(userId: String, callback: (String?, String?) -> Unit) {
    // Riferimento al database Firebase
    val database = FirebaseDatabase.getInstance()
    val usersRef = database.reference.child("users")

    // Riferimento all'utente specifico
    val userRef = usersRef.child(userId)

    // Ottieni i dati dell'utente
    userRef.get().addOnSuccessListener { dataSnapshot ->
        if (dataSnapshot.exists()) {
            // Estrai nome e cognome
            val firstName = dataSnapshot.child("firstName").value as? String
            val lastName = dataSnapshot.child("lastName").value as? String
            // Chiamata del callback con nome e cognome
            callback(firstName, lastName)
        } else {
            // Se l'utente non esiste, restituisci null
            callback(null, null)
        }
    }.addOnFailureListener { exception ->
        // In caso di errore, restituisci null
        println("Errore: ${exception.message}")
        callback(null, null)
    }
}

data class Reservation(
    val id: String,
    val type: String,
    val slotDate: String,
    val participants: List<String> = emptyList(),
    val coachId: String? = null,
    val fieldId: String? = null,
    var fieldName: String? = null  // Aggiungi il fieldName qui
)
