package com.example.paddlecenterapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun BottomNavigationBar(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    navController: NavController
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedItem == 0,
            onClick = {
                onItemSelected(0)
                navController.navigate("home") { launchSingleTop = true }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "HomePage") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = selectedItem == 1,
            onClick = {
                onItemSelected(1)
                navController.navigate("reservation_field") { launchSingleTop = true }
            },
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Reservation") },
            label = { Text("Booking") }
        )
        NavigationBarItem(
            selected = selectedItem == 2,
            onClick = {
                onItemSelected(2)
                navController.navigate("search") { launchSingleTop = true }
            },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = selectedItem == 3,
            onClick = {
                onItemSelected(3)
                navController.navigate("reservation_lesson") { launchSingleTop = true }
            },
            icon = { Icon(Icons.Default.School, contentDescription = "Lesson") },
            label = { Text("Lesson") }
        )
        NavigationBarItem(
            selected = selectedItem == 4,
            onClick = {
                onItemSelected(4)
                navController.navigate("profile") { launchSingleTop = true }
            },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}
