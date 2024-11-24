package com.example.paddlecenterapp.pages

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.BottomNavigationBar
import com.example.paddlecenterapp.models.Report
import com.example.paddlecenterapp.models.User
import com.example.paddlecenterapp.services.deleteReport
import com.example.paddlecenterapp.services.getUserNameAndSurname

@SuppressLint("RememberReturnType")
@Composable
fun UserReportsPage(
    modifier: Modifier = Modifier,
    userId: String,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var selectedItem by remember { mutableIntStateOf(2) }
    val snackbarHostState = remember { SnackbarHostState() }
    var user by remember { mutableStateOf<User?>(null) }
    var reports by remember { mutableStateOf<Map<String, Report>?>(null) }
    var userName by remember { mutableStateOf<String?>(null) }
    var reporterNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isDataReady by remember { mutableStateOf(false) } // Stato per indicare che i dati sono pronti

    // Funzione lambda per ricaricare i dati
    val fetchData: () -> Unit = {
        authViewModel.getUserDataFromRealtimeDatabase(userId) { retrievedUser ->
            if (retrievedUser != null) {
                user = retrievedUser
                reports = retrievedUser.reports

                // Recupero nome e cognome dell'utente principale
                getUserNameAndSurname(userId) { firstName, lastName ->
                    userName = if (firstName != null && lastName != null) {
                        "$firstName $lastName"
                    } else {
                        null
                    }
                }

                // Recupero nome e cognome dei reporter
                val reporterMap = mutableMapOf<String, String>()
                var reportersLoaded = 0
                reports?.forEach { (_, report) ->
                    getUserNameAndSurname(report.reportedById) { firstName, lastName ->
                        if (firstName != null && lastName != null) {
                            reporterMap[report.reportedById] = "$firstName $lastName"
                        } else {
                            reporterMap[report.reportedById] = "Unknown"
                        }

                        // Incrementa il contatore per verificare quando tutti i reporter sono stati caricati
                        reportersLoaded++
                        if (reportersLoaded == reports?.size) {
                            reporterNames = reporterMap
                            isDataReady = true // Imposta isDataReady a true quando i dati sono pronti
                        }
                    }
                }
            } else {
                Log.e("UserReportsPage", "Error retrieving user data")
            }
        }
    }

    // Inizializza i dati quando il composable viene caricato
    LaunchedEffect(userId) {
        fetchData()
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                navController = navController
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "User Reports Page",
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "List of $userName's reports:", fontSize = 20.sp)

            if (reports.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "There are no reports.", color = Color.Red)
            } else if (!isDataReady) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Loading reporter names...")
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn {
                    items(reports?.entries?.toList() ?: emptyList()) { (reportId, report) ->
                        ReportItem(
                            userId = userId,
                            reportId = reportId,
                            report = report,
                            reporterNames = reporterNames,
                            onDeleteSuccess = {
                                // Ricarica i dati dopo la cancellazione del report
                                fetchData()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItem(userId: String, reportId: String, report: Report, reporterNames: Map<String, String>, onDeleteSuccess: () -> Unit) {
    val context = LocalContext.current
    val reporterName = reporterNames[report.reportedById] ?: "Unknown"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Reporter: $reporterName", fontSize = 16.sp)
            Text("Reason: ${report.reason}", fontSize = 16.sp)
            Text("Date: ${report.timestamp}", fontSize = 14.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp), // Spazio tra i tasti
                verticalAlignment = Alignment.CenterVertically // Allineamento verticale dei tasti
            ) {
                Button(
                    onClick = {
                        deleteReport(context, userId, reportId)
                        onDeleteSuccess()  // Richiama il callback per ricaricare i dati
                    },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete report",
                    )
                }
            }
        }
    }
}