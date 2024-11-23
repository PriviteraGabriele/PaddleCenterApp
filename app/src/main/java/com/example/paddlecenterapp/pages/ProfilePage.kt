package com.example.paddlecenterapp.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.BottomNavigationBar
import java.util.Calendar

@Composable
fun ProfilePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var selectedItem by remember { mutableIntStateOf(4) } // Profilo è la terza voce

    // Recupera l'utente attuale da Firebase Authentication
    val currentUser = authViewModel.getCurrentUser()

    // Stati per i dati personali, preimpostati con i dati dell'utente
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var bio by remember { mutableStateOf("") }  // Aggiunto per la bio
    var ranking by remember { mutableStateOf("") }  // Aggiunto per il rank

    // Stato per la modalità di modifica
    var isEditing by remember { mutableStateOf(false) }

    // Recupera i dati aggiuntivi da Firebase Realtime Database se l'utente è loggato
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            authViewModel.getUserDataFromRealtimeDatabase(currentUser.uid) { user ->
                if (user != null) {
                    firstName = user.firstName
                    lastName = user.lastName
                    birthDate = user.birthDate
                    gender = user.gender
                    profileImageUri = user.profileImageUrl?.let { Uri.parse(it) }
                    bio = user.bio  // Recupera la bio
                    ranking = user.ranking // Recupera il rank
                }
            }
        }
    }

    // Launcher per selezionare una nuova immagine
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> profileImageUri = uri }

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
        // Aggiunta scrollabilità con verticalScroll
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(enabled = true, state = rememberScrollState()) // Scroll abilita
                .padding(end = 4.dp), // Aggiungi padding per la scrollbar
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Profile Page", fontSize = 32.sp)

            // Sezione per la foto profilo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp)
            ) {
                ProfileImage(profileImageUri = profileImageUri)
            }

            // Mostra dati di default (foto, bio, rank)
            if (!isEditing) {
                Text(text = "Bio: $bio")
                Text(text = "Rank: $ranking")
                Spacer(modifier = Modifier.height(18.dp))

                // Tasto per entrare in modalità modifica
                TextButton(onClick = { isEditing = true }) {
                    Text(text = "Edit Profile")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = {
                    authViewModel.signout()
                    navController.navigate("login") { launchSingleTop = true }
                }) {
                    Text(text = "Sign out")
                }
            } else {
                TextButton(onClick = { launcher.launch("image/*") }) {
                    Text(text = "Change Profile Picture")
                }

                Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        authViewModel.updateProfile(
                            firstName, lastName, birthDate, gender, profileImageUri, bio
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

@Composable
fun ProfileImage(profileImageUri: Uri?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = profileImageUri?.let {
            ImageRequest.Builder(context)
                .data(it)
                .size(Size.ORIGINAL)
                .build()
        }
    )

    Image(
        painter = painter,
        contentDescription = "Profile Image",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
    )
}


