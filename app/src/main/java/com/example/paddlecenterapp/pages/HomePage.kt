package com.example.paddlecenterapp.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.paddlecenterapp.BottomNavigationBar

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController) {
    var selectedItem by remember { mutableIntStateOf(0) }
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
                .padding(contentPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Home Page", fontSize = 32.sp)
            Text(text = "- Aggiugere lista prenotazioni attive (da oggi in poi)", fontSize = 12.sp)
            Text(text = "- Gestire update e delete prenotazione", fontSize = 12.sp)
            Text(text = "- update dei soli partecipanti (caso caio non può più venire (cagarello a fischio) e si aggiungo pippo al suo posto)", fontSize = 12.sp)

        }
    }
}