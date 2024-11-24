package com.example.paddlecenterapp.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paddlecenterapp.AuthViewModel
import com.example.paddlecenterapp.models.User
import com.example.paddlecenterapp.BottomNavigationBar
import com.example.paddlecenterapp.R
import com.example.paddlecenterapp.services.getUserIdByUserObject
import com.example.paddlecenterapp.services.searchUsers

@Composable
fun SearchPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    var selectedItem by remember { mutableIntStateOf(2) }
    var query by remember { mutableStateOf("") }
    val context = LocalContext.current
    var users by remember { mutableStateOf<List<User>>(emptyList()) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(query) {
        try {
            users = searchUsers(query, authViewModel)
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search users...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { user ->
                    UserItem(user, navController)
                }
            }
        }
    }
}

@Composable
fun UserItem(user: User, navController: NavController) {
    var userIdd by remember { mutableStateOf("") }

    // Recupera l'ID dell'utente cercato
    getUserIdByUserObject(user) { userId ->
        if (userId != null) {
            userIdd = userId
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                // Naviga alla pagina dei dettagli dell'utente
                navController.navigate("userDetails/${userIdd}")
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp) // Aumentata la dimensione per un look più prominente
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                )
                .padding(6.dp) // Maggiore spaziatura per l'immagine
        ) {
            Image(
                painter = painterResource(id = R.drawable.profile_picture), // Usa l'immagine caricata
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(60.dp) // Aggiusta la dimensione dell'immagine
                    .clip(CircleShape), // Ritaglia l'immagine a forma di cerchio
                contentScale = ContentScale.Crop // Adatta l'immagine al contenitore
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = "${user.firstName} ${user.lastName}", fontSize = 18.sp)
            Text(text = user.email, fontSize = 14.sp)
        }
    }
}
