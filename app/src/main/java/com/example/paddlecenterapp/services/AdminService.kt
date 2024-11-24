package com.example.paddlecenterapp.services

import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeParseException


fun banUser(bannedUserId: String, onComplete: (Boolean) -> Unit) {
    // Ottieni riferimento al database di Firebase
    val database = FirebaseDatabase.getInstance().reference

    // Specifica il percorso dell'utente e imposta la chiave isBanned a true
    database.child("users").child(bannedUserId).child("banned").setValue(true)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Chiamata di successo
                onComplete(true)
            } else {
                // Chiamata fallita
                onComplete(false)
            }
        }
}

fun unbanUser(bannedUserId: String, onComplete: (Boolean) -> Unit) {
    // Ottieni riferimento al database di Firebase
    val database = FirebaseDatabase.getInstance().reference

    // Specifica il percorso dell'utente e imposta la chiave isBanned a true
    database.child("users").child(bannedUserId).child("banned").setValue(false)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Chiamata di successo
                onComplete(true)
            } else {
                // Chiamata fallita
                onComplete(false)
            }
        }
}

@Composable
fun AddCoachButton() {
    var showDialog by remember { mutableStateOf(false) }
    var coachName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val database = Firebase.database.reference

    Button(
        onClick = { showDialog = true },
        modifier = Modifier.padding(8.dp),
        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.errorContainer)
    ) {
        Text("Add new coach")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add New Coach") },
            text = {
                Column {
                    Text("Enter the name of the coach:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = coachName,
                        onValueChange = { coachName = it },
                        placeholder = { Text("Coach Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (coachName.isNotBlank()) {
                            // Genera un nuovo nodo con ID univoco
                            val newCoachRef = database.child("coaches").push()
                            val coachId = newCoachRef.key ?: ""

                            // Struttura del coach con nome e ID
                            val newCoach = mapOf(
                                "name" to coachName,
                                "id" to coachId
                            )

                            // Salva il nuovo coach nel database
                            newCoachRef.setValue(newCoach)
                                .addOnSuccessListener {
                                    // Mostra un Toast per il successo
                                    Toast.makeText(
                                        context,
                                        "Coach created successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { exception ->
                                    // Mostra un Toast per il fallimento
                                    Toast.makeText(
                                        context,
                                        "Failed to create coach: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            // Mostra un Toast se il nome è vuoto
                            Toast.makeText(
                                context,
                                "Coach name cannot be empty!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showDialog = false
                        coachName = ""
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddAvailabilityButton(coachId: String) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDateTime by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val database = Firebase.database.reference

    Button(
        onClick = { showDialog = true },
        modifier = Modifier.padding(8.dp),
        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.errorContainer)
    ) {
        Text("Add Availability Slot")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Availability Slot") },
            text = {
                Column {
                    Text("Enter the date and time for the availability slot (format: yyyy-MM-dd HH:mm):")
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = selectedDateTime,
                        onValueChange = { selectedDateTime = it },
                        placeholder = { Text("YYYY-MM-DD HH:MM") },
                        isError = errorMessage.isNotBlank(),
                        singleLine = true
                    )

                    if (errorMessage.isNotBlank()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Check if the date and time format is valid
                        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm")
                        try {
                            val date = LocalDateTime.parse(selectedDateTime, dateFormat)

                            // Check if the selected date is in the future
                            val currentDate = LocalDateTime.now()
                            if (date.isBefore(currentDate)) {
                                errorMessage = "The date cannot be in the past."
                            } else {
                                // Valid date and time, save it to the database
                                val newSlotRef = database.child("coaches")
                                    .child(coachId)
                                    .child("availability")
                                    .push() // Generate a unique ID for the slot

                                // Structure for the availability slot
                                val newSlot = mapOf(
                                    "date" to selectedDateTime,
                                    "status" to true // Always set status to true
                                )

                                // Save the new availability slot in the database
                                newSlotRef.setValue(newSlot)
                                    .addOnSuccessListener {
                                        // Show a success Toast
                                        Toast.makeText(
                                            context,
                                            "Availability slot added successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener { exception ->
                                        // Show an error Toast
                                        Toast.makeText(
                                            context,
                                            "Failed to add availability: ${exception.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                // Reset the dialog and input field
                                showDialog = false
                                selectedDateTime = ""
                                errorMessage = ""
                            }
                        } catch (e: DateTimeParseException) {
                            // Invalid date format
                            errorMessage = "Invalid date format. Please use yyyy-MM-dd HH:mm."
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
