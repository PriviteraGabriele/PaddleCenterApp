package com.example.paddlecenterapp.services

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paddlecenterapp.models.Report
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    val reportOptions = listOf(
        "False claim of victory",
        "Unsportsmanlike behavior",
        "Offensive language"
    )

    var selectedReason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report user") },
        text = {
            Column {
                Text("Reason for reporting:")
                reportOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = option }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically // Assicura che i pallini siano allineati correttamente
                    ) {
                        RadioButton(
                            selected = selectedReason == option,
                            onClick = { selectedReason = option }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            option,
                            modifier = Modifier.align(Alignment.CenterVertically) // Centra solo il testo
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedReason.isNotEmpty()) {
                        onReport(selectedReason)
                        onDismiss()
                    }
                }
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
