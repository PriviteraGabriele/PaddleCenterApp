package com.example.paddlecenterapp

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.paddlecenterapp.ui.theme.PaddleCenterAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    // Launcher per richiedere i permessi di notifica
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Autorizzazione concessa, l'app può mostrare notifiche
            Log.d("NotificationPermission", "Autorizzazione concessa!")
            sendTestNotification()  // Invia una notifica di test quando il permesso è concesso
        } else {
            // L'utente ha negato l'autorizzazione
            Log.d("NotificationPermission", "Autorizzazione negata. Nessuna notifica.")
        }
    }

    private val channelId = "test_notification_channel"
    private val channelName = "Test Notifications"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Abilita la modalità edge-to-edge per un'esperienza utente moderna
        enableEdgeToEdge()

        // Recupera il ViewModel per l'autenticazione
        val authViewModel: AuthViewModel by viewModels()

        // Imposta il contenuto dell'UI
        setContent {
            PaddleCenterAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyAppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        authViewModel = authViewModel
                    )
                }
            }
        }

        // Crea il canale di notifica
        createNotificationChannel()

        // Richiedi il permesso di notifica
        askNotificationPermission()

        // Ottieni il token FCM e salvalo nel database
        getFCMTokenAndSave()
    }

    /**
     * Chiede il permesso di notifica all'utente (necessario per Android 13+)
     */
    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                // Controlla se l'autorizzazione è già concessa
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("NotificationPermission", "Autorizzazione gia' concessa.")
                    sendTestNotification()  // Invia una notifica di test se il permesso è già concesso
                }

                // Mostra una spiegazione se necessario
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionExplanation()
                }

                // Richiedi direttamente l'autorizzazione
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    /**
     * Mostra una UI educativa per spiegare la necessità dell'autorizzazione
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showPermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Permesso notifiche richiesto")
            .setMessage("Concedi l'autorizzazione per ricevere notifiche utili sulla tua attività nell'app.")
            .setPositiveButton("OK") { _, _ ->
                // Richiedi l'autorizzazione dopo aver spiegato
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("No grazie", null)
            .show()
    }

    /**
     * Crea il canale di notifica (necessario per Android 8.0 e versioni successive)
     */
    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = "Canale per le notifiche di test"
        }

        val notificationManager: NotificationManager =
            getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Invia una notifica di test
     */
    private fun sendTestNotification() {
        // Crea un intent che aprirà la MainActivity quando l'utente clicca sulla notifica
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Crea la notifica
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.morte_nera)  // Aggiungi un'icona per la notifica
            .setContentTitle("Notifica di test")
            .setContentText("Questa è una notifica di test!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)  // Imposta il PendingIntent
            .setAutoCancel(true)  // La notifica scomparirà quando l'utente la clicca

        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(1, notificationBuilder.build())  // ID della notifica = 1
    }

    /**
     * Ottiene il token FCM e lo salva nel database
     */
    private fun getFCMTokenAndSave() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM", "Token FCM: $token")

            // Salva il token nel database
            saveTokenToDatabase(token)
        }
    }

    /**
     * Salva il token FCM nel database (Firebase Realtime Database)
     */
    private fun saveTokenToDatabase(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid // ID dell'utente autenticato
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
        userId?.let {
            databaseReference.child(it).child("fcmToken").setValue(token)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FCM", "Token salvato con successo nel database!")
                    } else {
                        Log.w("FCM", "Errore nel salvare il token", task.exception)
                    }
                }
        }
    }
}
