package com.example.paddlecenterapp

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.paddlecenterapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        _authState.value = if (auth.currentUser == null) {
            AuthState.Unauthenticated
        } else {
            AuthState.Authenticated
        }
    }

    fun getCurrentUser() = auth.currentUser

    fun getUserDataFromRealtimeDatabase(uid: String, onComplete: (User?) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.reference.child("users").child(uid)

        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Converte i dati del database in un oggetto User
                val user = snapshot.getValue(User::class.java)
                onComplete(user)
            } else {
                onComplete(null)
            }
        }.addOnFailureListener {
            onComplete(null)
        }
    }

    // Funzione per il login
    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }

        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    // Funzione per la registrazione
    fun signup(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        gender: String
    ) {
        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || birthDate.isEmpty() || gender.isEmpty()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        val userMap = mapOf(
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "birthDate" to birthDate,
                            "gender" to gender,
                            "profileImageUrl" to "" // Inizialmente vuoto
                        )
                        database.child("users").child(uid).setValue(userMap)
                            .addOnSuccessListener {
                                _authState.value = AuthState.Authenticated
                            }
                            .addOnFailureListener { e ->
                                _authState.value = AuthState.Error(e.message ?: "Database Error")
                            }
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong")
                }
            }
    }

    fun updateProfile(
        firstName: String,
        lastName: String,
        birthDate: String,
        gender: String,
        profileImageUri: Uri? = null
    ) {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        if (uid != null) {
            // 1. Aggiorna il profilo in Firebase Authentication
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName("$firstName $lastName")
                .build()

            currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // 2. Rinnova il token di autenticazione per evitare problemi di sessione
                        currentUser.getIdToken(true).addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                // Se necessario, puoi usare il nuovo token aggiornato per ulteriori operazioni

                                // 3. Aggiorna il database Firebase
                                val userMap: Map<String, Any> = mapOf(
                                    "firstName" to firstName,
                                    "lastName" to lastName,
                                    "birthDate" to birthDate,
                                    "gender" to gender
                                ).toMutableMap().apply {
                                    // Aggiungi l'immagine del profilo se presente
                                    profileImageUri?.let { this["profileImageUrl"] = it.toString() }
                                }

                                // Aggiorna i dati nel database
                                database.child("users").child(uid).updateChildren(userMap)
                                    .addOnFailureListener { e ->
                                        _authState.value = AuthState.Error(e.message ?: "Database update failed")
                                    }
                            } else {
                                // Gestisci errore nel rinnovo del token
                                _authState.value = AuthState.Error("Token refresh failed: ${tokenTask.exception?.message}")
                            }
                        }
                    } else {
                        // Gestisci errore nell'aggiornamento del profilo in Firebase Auth
                        _authState.value = AuthState.Error("Profile update failed: ${task.exception?.message}")
                    }
                }
        } else {
            // Gestisci il caso in cui l'utente non sia autenticato
            _authState.value = AuthState.Error("User is not authenticated")
        }
    }


    // Funzione per il logout
    fun signout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}

// Stato dell'autenticazione
sealed class AuthState {
    data object Authenticated : AuthState()
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}
