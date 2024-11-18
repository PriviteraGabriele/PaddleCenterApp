package com.example.paddlecenterapp.services

import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

suspend fun searchUsers(query: String, authViewModel: AuthViewModel): List<User> {
    val databaseRef = FirebaseDatabase.getInstance().getReference("users")
    val userList = mutableListOf<User>()

    val currentUser = authViewModel.getCurrentUser()
    val snapshot = databaseRef.get().await()

    for (userSnapshot in snapshot.children) {
        val user = userSnapshot.getValue(User::class.java)

        if (user != null && currentUser != null) {
            // Assicurati che non venga trovato l'utente corrente
            if (user.email != currentUser.email) {
                val firstName = user.firstName
                val lastName = user.lastName

                // Filtro per la ricerca
                if (query.isEmpty() || firstName.startsWith(query, ignoreCase = true) || lastName.startsWith(query, ignoreCase = true)) {
                    // Se il campo reports è presente ma non è una mappa, lo trattiamo come null
                    val reports = user.reports ?: emptyMap()

                    // Aggiungi l'utente alla lista
                    userList.add(user.copy(reports = reports))
                }
            }
        }
    }

    return userList
}

// Funzione per ottenere l'ID utente passato come input
fun getUserIdByUserObject(user: User, callback: (String?) -> Unit) {
    val databaseRef = FirebaseDatabase.getInstance().getReference("users")
    databaseRef.orderByChild("email").equalTo(user.email)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val userId = userSnapshot.key
                        callback(userId)
                        return
                    }
                }
                callback(null)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
}

// Funzione per aggiungere un amico all'utente autenticato
fun addFriendToCurrentUser(friendId: String, callback: (Boolean) -> Unit) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (currentUserId != null) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("users")
            .child(currentUserId)
            .child("friends")

        // Aggiungi l'ID dell'amico sotto "friends"
        databaseRef.child(friendId).setValue(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true)
                } else {
                    callback(false)
                }
            }
    } else {
        callback(false)
    }
}

fun removeFriendFromCurrentUser(friendId: String, onResult: (Boolean) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser != null) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)

        // Rimuovi l'ID del nuovo amico dalla lista degli amici
        userRef.child("friends").child(friendId).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Rimuovi anche il riferimento inverso nella lista dell'amico
                FirebaseDatabase.getInstance().getReference("users").child(friendId)
                    .child("friends").child(currentUser.uid).removeValue().addOnCompleteListener { task2 ->
                        onResult(task2.isSuccessful)
                    }
            } else {
                onResult(false)
            }
        }
    } else {
        onResult(false)
    }
}

// Funzione per verificare se l'ID dell'amico è presente nell'array di amici
fun checkFriendship(friendId: String, friends: Map<String, Boolean>, callback: (Boolean) -> Unit) {
    // Controlla se l'ID dell'amico è presente nell'array 'friends'
    if (friendId in friends) {
        // L'ID dell'amico è presente
        callback(true)
    } else {
        // L'ID dell'amico non è presente
        callback(false)
    }
}
