package com.example.paddlecenterapp.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.paddlecenterapp.MainActivity
import com.example.paddlecenterapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage

class FirebaseNotificationService {

    private val channelId = "test_notification_channel"
    private val channelName = "Test Notifications"

    // Metodo per ottenere il token FCM
    fun getToken(context: Context, onTokenReceived: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM", "Token FCM: $token")
            onTokenReceived(token)
            // Salva il token nel database
            saveTokenToDatabase(token, context)
        }
    }

    // Metodo per salvare il token nel database
    private fun saveTokenToDatabase(token: String, context: Context) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid // Id dell'utente autenticato
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        userId?.let {
            databaseReference.child(it).child("fcmToken").setValue(token)
        }
    }

    // Metodo per inviare una notifica
    fun sendNotification(context: Context, title: String, message: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.morte_nera)  // Aggiungi un'icona per la notifica
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)  // Imposta il PendingIntent
            .setAutoCancel(true)  // La notifica scomparirà quando l'utente la clicca

        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Gestisci la richiesta di permesso
            return
        }
        notificationManager.notify(1, notificationBuilder.build())  // ID della notifica = 1
    }

    // Crea il canale di notifica (necessario per Android 8.0 e versioni successive)
    private fun createNotificationChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Canale per le notifiche di test"
            }

            val notificationManager: NotificationManager =
                context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Metodo per inviare notifiche push tramite FCM
    fun sendPushNotification(targetToken: String, title: String, message: String) {
        val notificationData = mapOf(
            "title" to title,
            "message" to message
        )

        val remoteMessage = RemoteMessage.Builder(targetToken)
            .setMessageId("1")
            .addData("title", title)
            .addData("message", message)
            .build()

        FirebaseMessaging.getInstance().send(remoteMessage)
        Log.d("FCM", "Push notification sent to $targetToken")
    }
}
