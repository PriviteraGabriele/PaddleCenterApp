package com.example.paddlecenterapp.models

import android.content.Context
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Report(
    val reason: String = "",
    val reportedById: String = "",
    val timestamp: String = ""  // Cambia il tipo a String
)

fun addReport(
    context: Context,
    reportedUserId: String,
    reportedById: String,
    reason: String
) {
    val database = FirebaseDatabase.getInstance().reference

    // Crea la data leggibile in formato "yyyy-MM-dd HH:mm:ss"
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val timestamp = dateFormat.format(Date())  // Ottieni la data attuale in formato leggibile

    // Crea un nuovo oggetto report
    val report = Report(
        reason = reason,
        reportedById = reportedById,
        timestamp = timestamp  // Salva il timestamp leggibile come Stringa
    )

    val reportRef = database.child("users").child(reportedUserId).child("reports")
    val newReportKey = reportRef.push().key

    if (newReportKey != null) {
        reportRef.child(newReportKey).setValue(report)
            .addOnSuccessListener {
                Toast.makeText(context, "Report sent", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
