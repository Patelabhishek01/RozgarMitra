package com.rozgarmitra.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rozgarmitra.app.presentation.auth.LoginScreen
import com.rozgarmitra.app.presentation.auth.RegisterScreen
import com.rozgarmitra.app.presentation.owner.OwnerDetailsScreen
import com.rozgarmitra.app.presentation.splash.SplashScreen
import com.rozgarmitra.app.presentation.worker.LabourDetailsScreen

@Composable
fun AppNavGraph() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        composable(Screen.Splash.route) {

            SplashScreen(
                onSplashFinished = {

                    navController.navigate(Screen.Login.route) {

                        popUpTo(Screen.Splash.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {

            LoginScreen(

                onSendOtpClick = { mobileNumber ->

                    // Firebase OTP will be connected here later.
                    // For now, we are only testing the UI.

                },

                onRegisterClick = {

                    navController.navigate(Screen.Register.route)

                }
            )
        }

        composable(Screen.Register.route) {

            RegisterScreen(

                onLabourSelected = {
                    navController.navigate(Screen.LabourDetails.route)
                },

                onOwnerSelected = {
                    navController.navigate(Screen.OwnerDetails.route)
                }
            )
        }
        composable(Screen.LabourDetails.route) {
            LabourDetailsScreen()
        }

        composable(Screen.OwnerDetails.route) {
            OwnerDetailsScreen()
        }

    }

}