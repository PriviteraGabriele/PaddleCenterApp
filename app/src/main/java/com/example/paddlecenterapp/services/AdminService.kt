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
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
}

fun unbanUser(bannedUserId: String, onComplete: (Boolean) -> Unit) {
    // Ottieni riferimento al database di Firebase
    val database = FirebaseDatabase.getInstance().reference

    // Specifica il percorso dell'utente e imposta la chiave isBanned a false
    database.child("users").child(bannedUserId).child("banned").setValue(false)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
}

@Composable
fun AddEntityButton(dbType: String) {
    var showDialog by remember { mutableStateOf(false) }
    var entityName by remember { mutableStateOf("") }
    val context = LocalContext.current
    val database = Firebase.database.reference

    Button(
        onClick = { showDialog = true },
        modifier = Modifier.padding(8.dp),
        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.errorContainer)
    ) {
        Text("Add new $dbType")
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add New $dbType") },
            text = {
                Column {
                    Text("Enter the name of the $dbType:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = entityName,
                        onValueChange = { entityName = it },
                        placeholder = { Text("${dbType.capitalize()} Name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (entityName.isNotBlank()) {
                            // Genera un nuovo nodo con ID univoco basato su dbType
                            val newEntityRef = if (dbType == "coach") {
                                database.child("coaches").push() // Se è coach
                            } else {
                                database.child("fields").push() // Se è field
                            }
                            val entityId = newEntityRef.key ?: ""

                            // Struttura dell'entità con nome e ID
                            val newEntity = mapOf(
                                "name" to entityName,
                                "id" to entityId
                            )

                            // Salva l'entità nel database
                            newEntityRef.setValue(newEntity)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "$dbType created successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(
                                        context,
                                        "Failed to create $dbType: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                context,
                                "$dbType name cannot be empty!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showDialog = false
                        entityName = ""
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
fun AddAvailabilityButton(id: String, dbType: String) {
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
                        val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm")
                        try {
                            val date = LocalDateTime.parse(selectedDateTime, dateFormat)

                            val currentDate = LocalDateTime.now()
                            if (date.isBefore(currentDate)) {
                                errorMessage = "The date cannot be in the past."
                            } else {
                                val newSlotRef = if (dbType == "coach") {
                                    database.child("coaches")
                                        .child(id)
                                        .child("availability")
                                        .push()
                                } else {
                                    database.child("fields")
                                        .child(id)
                                        .child("availability")
                                        .push()
                                }

                                val newSlot = mapOf(
                                    "date" to selectedDateTime,
                                    "status" to true
                                )

                                newSlotRef.setValue(newSlot)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            "Availability slot added successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(
                                            context,
                                            "Failed to add availability: ${exception.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                showDialog = false
                                selectedDateTime = ""
                                errorMessage = ""
                            }
                        } catch (e: DateTimeParseException) {
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