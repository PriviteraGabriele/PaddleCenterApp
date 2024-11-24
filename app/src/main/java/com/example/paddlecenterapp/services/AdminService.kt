package com.example.paddlecenterapp.services

import com.google.firebase.database.FirebaseDatabase

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