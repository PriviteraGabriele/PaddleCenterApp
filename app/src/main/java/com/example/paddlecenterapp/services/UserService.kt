package com.example.paddlecenterapp.services

import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.models.User
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

suspend fun searchUsers(query: String, authViewModel: AuthViewModel, flag: Boolean = false): List<User> {
    val databaseRef = FirebaseDatabase.getInstance().getReference("users")
    val userList = mutableListOf<User>()

    // Se la query è vuota, ritorna una lista vuota
    if (flag && query.isEmpty()) {
        return userList
    }

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

fun addFriendToBothUsers(friendId: String, callback: (Boolean) -> Unit) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (currentUserId != null) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("users")

        // Aggiungi l'amico alla lista dell'utente autenticato
        val currentUserTask = databaseRef
            .child(currentUserId)
            .child("friends")
            .child(friendId)
            .setValue(true)

        // Aggiungi l'utente autenticato alla lista dell'amico
        val friendTask = databaseRef
            .child(friendId)
            .child("friends")
            .child(currentUserId)
            .setValue(true)

        // Esegui entrambe le operazioni e verifica il risultato
        Tasks.whenAll(currentUserTask, friendTask)
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

fun removeFriendFromBothUsers(friendId: String, onResult: (Boolean) -> Unit) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser != null) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("users")

        // Rimuovi l'amico dalla lista degli amici dell'utente corrente
        val currentUserTask = databaseRef
            .child(currentUser.uid)
            .child("friends")
            .child(friendId)
            .removeValue()

        // Rimuovi l'utente corrente dalla lista degli amici dell'amico
        val friendTask = databaseRef
            .child(friendId)
            .child("friends")
            .child(currentUser.uid)
            .removeValue()

        // Esegui entrambe le operazioni e verifica il risultato
        Tasks.whenAll(currentUserTask, friendTask)
            .addOnCompleteListener { task ->
                onResult(task.isSuccessful)
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
