package com.example.paddlecenterapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.BottomNavigationBar
import com.example.paddlecenterapp.R
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

@Composable
fun ProfilePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var selectedItem by remember { mutableIntStateOf(4) } // Profilo è la quarta voce

    // Recupera l'utente attuale da Firebase Authentication
    val currentUser = authViewModel.getCurrentUser()

    // Stati per i dati personali
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") } // Aggiunto per la bio
    var averageReputation by remember { mutableFloatStateOf(0f) } // Media dinamica della reputazione

    // Stato per la modalità di modifica
    var isEditing by remember { mutableStateOf(false) }

    // Recupera i dati utente e la media della reputazione
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            // Recupera i dati utente
            authViewModel.getUserDataFromRealtimeDatabase(currentUser.uid) { user ->
                if (user != null) {
                    firstName = user.firstName
                    lastName = user.lastName
                    birthDate = user.birthDate
                    gender = user.gender
                    bio = user.bio
                }
            }

            // Calcola la media della reputazione
            val reputationRef = FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(currentUser.uid)
                .child("reputation")

            reputationRef.get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val reputationMap = dataSnapshot.value as? Map<String, Int>
                    if (!reputationMap.isNullOrEmpty()) {
                        val totalVotes = reputationMap.values.sum()
                        val numberOfVotes = reputationMap.size
                        averageReputation = totalVotes.toFloat() / numberOfVotes
                    }
                }
            }
        }
    }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            birthDate = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                navController = navController
            )
        }
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(end = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isEditing) {
                // Titolo della pagina
                Text(
                    text = "Profile Page",
                    fontSize = 32.sp,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Foto profilo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                        .padding(6.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.profile_picture),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "$firstName $lastName", fontSize = 22.sp)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    enabled = false,
                    modifier = Modifier
                        .widthIn(min = 80.dp, max = 320.dp)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = "${"%.1f".format(averageReputation)}/5",
                    onValueChange = {},
                    label = { Text("Reputation") },
                    enabled = false
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { isEditing = true },
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50))
                ) {
                    Text(
                        text = "Edit Profile",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                TextButton(
                    onClick = {
                        authViewModel.signout()
                        navController.navigate("login") { launchSingleTop = true }
                    },
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.error, shape = RoundedCornerShape(50))
                ) {
                    Text(
                        text = "Sign out",
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Text(
                    text = "Edit Profile Page",
                    fontSize = 32.sp,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Foto profilo con immagine personalizzata
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp) // Dimensione del cerchio
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                        .padding(4.dp) // Spaziatura interna per l'immagine
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.profile_picture), // Usa l'immagine caricata
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp) // Dimensione dell'immagine all'interno del cerchio
                            .clip(CircleShape), // Ritaglia l'immagine a forma di cerchio
                        contentScale = ContentScale.Crop // Adatta l'immagine al contenitore
                    )
                }

                Spacer(modifier = Modifier.height(12.dp)) // Spazio tra immagine e dati utente
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = birthDate,
                    singleLine = true,
                    onValueChange = { birthDate = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = {
                        Text(text = "dd/mm/yyyy", fontWeight = FontWeight(400), fontSize = 14.sp)
                    },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(imageVector = Icons.Rounded.CalendarMonth, contentDescription = null)
                        }
                    },
                    label = { Text(text = "Birth Date") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier
                        .widthIn(min = 80.dp, max = 280.dp) // Imposta la larghezza minima e massima
                        .heightIn(min = 56.dp) // Imposta un'altezza minima, può espandersi
                        .fillMaxWidth() // Mantiene la larghezza fissa all'interno dei limiti
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Sezione del genere con RadioButton
                Text(text = "Gender", fontSize = 18.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Male", "Female", "Other").forEach {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == it, onClick = { gender = it })
                            Text(text = it)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        authViewModel.updateProfile(
                            firstName, lastName, birthDate, gender, bio
                        )
                        isEditing = false  // Esci dalla modalità di modifica
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text(text = "Save Changes", color = Color.White)
                }
            }
        }
    }
}


