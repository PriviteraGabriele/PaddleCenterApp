package com.example.paddlecenterapp.models

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class User(
    val birthDate: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "",
    val profileImageUrl: String? = null,
    val friends: Map<String, Boolean>? = null
)

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


