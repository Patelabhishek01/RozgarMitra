package com.rozgarmitra.app.presentation.navigation

sealed class Screen(val route: String) {

    data object Splash : Screen("splash")

    data object Login : Screen("login")

    data object Register : Screen("register")

    data object WorkerHome : Screen("worker_home")

    data object OwnerHome : Screen("owner_home")

    data object LabourDetails : Screen("labour_details")

    data object OwnerDetails : Screen("owner_details")

}