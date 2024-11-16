package com.example.paddlecenterapp.services

import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.models.User
import com.google.firebase.database.*
import kotlinx.coroutines.tasks.await

suspend fun searchUsers(query: String, authViewModel: AuthViewModel): List<User> {
    val databaseRef = FirebaseDatabase.getInstance().getReference("users")
    val userList = mutableListOf<User>()

    val currentUser = authViewModel.getCurrentUser()

    // Ottieni il DataSnapshot dalla query
    val snapshot = databaseRef.get().await()

    for (userSnapshot in snapshot.children) {
        val user = userSnapshot.getValue(User::class.java)
        if (user != null) {
            if (currentUser != null) {
                if (user.email != currentUser.email) {
                    val firstName = user.firstName
                    val lastName = user.lastName

                    // Controlla se il query è presente nel firstName o nel lastName
                    if (firstName.contains(query, ignoreCase = true) || lastName.contains(
                            query,
                            ignoreCase = true
                        )
                    ) {
                        userList.add(user)
                    }
                }
            }
        }
    }
    return userList
}
