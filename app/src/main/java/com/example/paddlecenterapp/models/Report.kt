package com.example.paddlecenterapp.models

import android.content.Context
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase

data class Report(
    val reportedById: String, // ID di chi ha segnalato
    val reason: String, // Motivo del report
    val timestamp: Long = System.currentTimeMillis() // Timestamp della segnalazione
)

fun addReport(
    context: Context,
    reportedUserId: String,
    reportedById: String,
    reason: String
) {
    val database = FirebaseDatabase.getInstance().reference

    // Crea un nuovo report
    val report = Report(
        reportedById = reportedById,
        reason = reason
    )

    val reportRef = database.child("users").child(reportedUserId).child("reports")
    val newReportKey = reportRef.push().key

    if (newReportKey != null) {
        reportRef.child(newReportKey).setValue(report)
            .addOnSuccessListener {
                Toast.makeText(context, "Report inviato", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}


