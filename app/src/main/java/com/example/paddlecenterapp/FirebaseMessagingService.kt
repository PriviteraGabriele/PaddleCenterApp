package com.example.paddlecenterapp

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nuovo token: $token")
        // Invia il token al server della tua app, se necessario
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Controlla il contenuto del messaggio
        remoteMessage.notification?.let {
            Log.d("FCM", "Messaggio: ${it.body}")
            // Mostra una notifica o gestisci il messaggio
        }
    }
}
