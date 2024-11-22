package com.example.paddlecenterapp.pages

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.paddlecenterapp.BottomNavigationBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.content.Context
import com.example.paddlecenterapp.models.Reservation

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Active Bookings",
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(22.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(activeReservations) { reservation ->
                    ReservationItem(reservation = reservation, navController, onDeleteSuccess = {
                        // Refresh the list after deletion
                        fetchReservations(database, currentUserId) { reservations ->
                            activeReservations = reservations
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun ReservationItem(reservation: Reservation, navController: NavController, onDeleteSuccess: () -> Unit) {
    val context = LocalContext.current
    var fieldName by remember { mutableStateOf(reservation.fieldName) }
    var coachName by remember { mutableStateOf(reservation.coachName) }
    val database = FirebaseDatabase.getInstance().getReference("reservations")

    LaunchedEffect(reservation.fieldId) {
        if (fieldName == null && reservation.fieldId != null) {
            getFieldNameFromDatabase(reservation.fieldId) { fetchedFieldName ->
                fieldName = fetchedFieldName
            }
        }
    }

    LaunchedEffect(reservation.coachId) {
        if (coachName == null && reservation.coachId != null) {
            getCoachNameFromDatabase(reservation.coachId) { fetchedCoachName ->
                coachName = fetchedCoachName
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Type: ${reservation.type}", fontSize = 16.sp)
            Text("Date: ${reservation.slotDate}", fontSize = 16.sp)
            if (reservation.type == "field") {
                Text("Field: ${fieldName ?: "Loading..."}", fontSize = 16.sp)
                ReservationParticipants(participants = reservation.participants)
            } else {
                Text("Coach: ${coachName ?: "Loading..."}", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    navController.navigate("edit_reservation/${reservation.id}") // Naviga alla pagina di modifica
                }) {
                    Text("Edit")
                }
                Button(
                    onClick = {
                        deleteReservation(context, database, reservation.id, onDeleteSuccess)
                    },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}


@Composable
fun ReservationParticipants(participants: List<String>) {
    val participantNames = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(participants) {
        participantNames.clear()
        isLoading = true

        participants.forEach { userId ->
            getUserNameAndSurname(userId) { firstName, lastName ->
                if (firstName != null && lastName != null) {
                    participantNames.add("$firstName $lastName")
                } else {
                    participantNames.add("Unknown User")
                }
            }
        }

        isLoading = false
    }

    if (isLoading) {
        Text("Loading participants...", fontSize = 14.sp)
    } else {
        Text("Participants: ${participantNames.joinToString(", ")}", fontSize = 14.sp)
    }
}

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

fun getCoachNameFromDatabase(coachId: String, callback: (String) -> Unit) {
    val database = FirebaseDatabase.getInstance()
    val coachRef = database.reference.child("coaches").child(coachId)

    coachRef.child("name").get().addOnSuccessListener { snapshot ->
        val fieldName = snapshot.getValue(String::class.java)
        callback(fieldName ?: "Unknown Field")
    }.addOnFailureListener {
        callback("Error retrieving field name")
    }
}

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

            val sortedReservations = reservations.sortedBy { LocalDateTime.parse(it.slotDate, formatter) }
            onResult(sortedReservations)
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle error
        }
    })
}

fun getUserNameAndSurname(userId: String, callback: (String?, String?) -> Unit) {
    val database = FirebaseDatabase.getInstance()
    val usersRef = database.reference.child("users")
    val userRef = usersRef.child(userId)

    userRef.get().addOnSuccessListener { dataSnapshot ->
        if (dataSnapshot.exists()) {
            val firstName = dataSnapshot.child("firstName").value as? String
            val lastName = dataSnapshot.child("lastName").value as? String
            callback(firstName, lastName)
        } else {
            callback(null, null)
        }
    }.addOnFailureListener {
        callback(null, null)
    }
}

fun deleteReservation(
    context: Context,
    database: DatabaseReference,
    reservationId: String,
    onDeleteSuccess: () -> Unit
) {
    val lessonRef = database.child("lessons").child(reservationId)
    val fieldRef = database.child("fields").child(reservationId)

    fun removeReservation(ref: DatabaseReference) {
        ref.get().addOnSuccessListener { snapshot ->
            val coachId = snapshot.child("coachId").getValue(String::class.java)
            if (coachId != null) {
                database.child("lessons").child(reservationId).removeValue()
            } else {
                database.child("fields").child(reservationId).removeValue()
            }
            onDeleteSuccess()
        }.addOnFailureListener { error ->
            Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    removeReservation(lessonRef)
    removeReservation(fieldRef)

    Toast.makeText(context, "Reservation deleted successfully!", Toast.LENGTH_SHORT).show()
}
