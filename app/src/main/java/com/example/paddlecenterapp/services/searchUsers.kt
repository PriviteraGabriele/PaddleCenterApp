package com.example.paddlecenterapp.services

import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.models.Report
import com.example.paddlecenterapp.models.User
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
                    val reports = user.reports ?: emptyMap<String, Report>()

                    // Aggiungi l'utente alla lista
                    userList.add(user.copy(reports = reports))
                }
            }
        }
    }

    return userList
}
