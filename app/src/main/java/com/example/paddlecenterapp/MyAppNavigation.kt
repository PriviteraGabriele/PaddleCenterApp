package com.example.paddlecenterapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.paddlecenterapp.pages.EditReservationPage
import com.example.paddlecenterapp.pages.HomePage
import com.example.paddlecenterapp.pages.LoginPage
import com.example.paddlecenterapp.pages.SignUpPage
import com.example.paddlecenterapp.pages.SearchPage
import com.example.paddlecenterapp.pages.ProfilePage
import com.example.paddlecenterapp.pages.ReservationFieldPage
import com.example.paddlecenterapp.pages.ReservationLessonPage
import com.example.paddlecenterapp.pages.UserDetailsPage
import com.example.paddlecenterapp.pages.UserReportsPage

@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginPage(modifier, navController, authViewModel)
        }
        composable("signup") {
            SignUpPage(modifier, navController, authViewModel)
        }
        composable("home") {
            HomePage(modifier, navController)
        }
        composable("reservation_field") {
            ReservationFieldPage(modifier, navController, authViewModel)
        }
        composable("search") {
            SearchPage(modifier, navController, authViewModel)
        }
        composable("profile") {
            ProfilePage(modifier, navController, authViewModel)
        }
        composable("reservation_lesson") {
            ReservationLessonPage(modifier, navController)
        }

        // Gestione del parametro dinamico reservationId con authViewModel
        composable("edit_reservation/{reservationId}") { backStackEntry ->
            val reservationId = backStackEntry.arguments?.getString("reservationId")
            if (reservationId != null) {
                EditReservationPage(reservationId = reservationId, navController = navController, authViewModel = authViewModel)
            }
        }

        composable("userDetails/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserDetailsPage(modifier, userId = userId, navController = navController, authViewModel = authViewModel)
        }

        composable("UserReports/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserReportsPage(modifier, userId = userId, navController = navController, authViewModel = authViewModel)
        }
    }
}
