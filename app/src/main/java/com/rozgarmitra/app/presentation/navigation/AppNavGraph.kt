package com.rozgarmitra.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rozgarmitra.app.data.Role
import com.rozgarmitra.app.data.RozgarRepository
import com.rozgarmitra.app.presentation.auth.*
import com.rozgarmitra.app.presentation.chat.ChatScreen
import com.rozgarmitra.app.presentation.job.JobDetailsScreen
import com.rozgarmitra.app.presentation.job.JobResultsScreen
import com.rozgarmitra.app.presentation.main.MainHomeScreen
import com.rozgarmitra.app.presentation.owner.*
import com.rozgarmitra.app.presentation.profile.MyApplicationsScreen
import com.rozgarmitra.app.presentation.profile.MyProfessionsScreen
import com.rozgarmitra.app.presentation.settings.SettingsScreen
import com.rozgarmitra.app.presentation.splash.SplashScreen
import com.rozgarmitra.app.presentation.worker.*

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val currentUserState = RozgarRepository.currentUser.collectAsStateWithLifecycle()
    val currentUser = currentUserState.value

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = { isLanguageSelected ->
                    val destination = when {
                        !isLanguageSelected -> Screen.LanguageSelect.route
                        currentUser != null && currentUser.role == Role.LABOUR -> Screen.WorkerHome.route
                        currentUser != null && currentUser.role == Role.OWNER -> Screen.OwnerHome.route
                        else -> Screen.MainHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LanguageSelect.route) {
            LanguageSelectScreen(
                onLanguageSelected = {
                    navController.navigate(Screen.MainHome.route) {
                        popUpTo(Screen.LanguageSelect.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.MainHome.route) {
            MainHomeScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                onNavigateToJobDetails = { jobId -> navController.navigate(Screen.JobDetails.createRoute(jobId)) },
                onNavigateToSeeAll = { title, filter -> navController.navigate(Screen.JobResults.createRoute(title, filter)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfessions = { navController.navigate(Screen.MyProfessions.route) },
                onNavigateToApplications = { navController.navigate(Screen.MyApplications.route) },
                onNavigateToWorkerHome = {
                    navController.navigate(Screen.WorkerHome.route) {
                        popUpTo(Screen.MainHome.route) { inclusive = true }
                    }
                },
                onNavigateToOwnerHome = {
                    navController.navigate(Screen.OwnerHome.route) {
                        popUpTo(Screen.MainHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.JobResults.route,
            arguments = listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("filterType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val filterType = backStackEntry.arguments?.getString("filterType") ?: ""
            JobResultsScreen(
                title = title,
                filterType = filterType,
                onBackClick = { navController.popBackStack() },
                onJobClick = { jobId -> navController.navigate(Screen.JobDetails.createRoute(jobId)) },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToLanguage = { navController.navigate(Screen.LanguageSelect.route) },
                onNavigateToProfessions = { navController.navigate(Screen.MyProfessions.route) }
            )
        }

        composable(Screen.MyProfessions.route) {
            MyProfessionsScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.MyApplications.route) {
            MyApplicationsScreen(
                onBackClick = { navController.popBackStack() },
                onJobClick = { jobId -> navController.navigate(Screen.JobDetails.createRoute(jobId)) }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onSendOtpSuccess = { phone ->
                    navController.navigate(Screen.Otp.createRoute(phone))
                },
                onLoginSuccess = { profileCompleted ->
                    val user = RozgarRepository.currentUser.value
                    if (profileCompleted) {
                        val route = if (user?.role == Role.LABOUR) Screen.WorkerHome.route else Screen.OwnerHome.route
                        navController.navigate(route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        // User exists but profile not finished. 
                        // If they have a role, send to Details. If not, send to Register.
                        if (user?.role != null) {
                            val route = if (user.role == Role.LABOUR) Screen.LabourDetails.route else Screen.OwnerDetails.route
                            navController.navigate(route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Register.createRoute(user?.phone?.ifBlank { "user" } ?: "user")) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    }
                },
                onNavigateToRegister = { id, pass ->
                    // Pass identifier and password if available
                    navController.navigate(Screen.Register.createRoute(id))
                },
                onLanguageSelectClick = {
                    navController.navigate(Screen.LanguageSelect.route)
                }
            )
        }

        composable(
            route = Screen.Otp.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            OtpScreen(
                phone = phone,
                onVerificationSuccess = { profileCompleted ->
                    if (profileCompleted) {
                        val role = RozgarRepository.currentUser.value?.role
                        val route = if (role == Role.LABOUR) Screen.WorkerHome.route else Screen.OwnerHome.route
                        navController.navigate(route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Register.createRoute(phone)) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Register.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val identifier = backStackEntry.arguments?.getString("phone") ?: ""
            RegisterScreen(
                identifier = identifier,
                onLabourRegistered = {
                    navController.navigate(Screen.LabourDetails.route)
                },
                onOwnerRegistered = {
                    navController.navigate(Screen.OwnerDetails.route)
                }
            )
        }

        composable(Screen.LabourDetails.route) {
            LabourDetailsScreen(
                onProfileCompleted = {
                    navController.navigate(Screen.WorkerHome.route) {
                        popUpTo(Screen.LabourDetails.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.OwnerDetails.route) {
            OwnerDetailsScreen(
                onProfileCompleted = {
                    navController.navigate(Screen.OwnerHome.route) {
                        popUpTo(Screen.OwnerDetails.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.WorkerHome.route) {
            LabourHomeScreen(
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetails.createRoute(jobId))
                },
                onChatThreadClick = { threadId ->
                    navController.navigate(Screen.Chat.createRoute(threadId))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onProfessionsClick = { navController.navigate(Screen.MyProfessions.route) },
                onApplicationsClick = { navController.navigate(Screen.MyApplications.route) },
                onLogoutClick = {
                    RozgarRepository.logout()
                    navController.navigate(Screen.MainHome.route) {
                        popUpTo(Screen.WorkerHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.OwnerHome.route) {
            OwnerHomeScreen(
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetails.createRoute(jobId))
                },
                onManageApplicantsClick = { jobId ->
                    navController.navigate(Screen.Applicants.createRoute(jobId))
                },
                onChatThreadClick = { threadId ->
                    navController.navigate(Screen.Chat.createRoute(threadId))
                },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onLogoutClick = {
                    RozgarRepository.logout()
                    navController.navigate(Screen.MainHome.route) {
                        popUpTo(Screen.OwnerHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.JobDetails.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            JobDetailsScreen(
                jobId = jobId,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToChat = { threadId ->
                    if (currentUser == null) {
                        navController.navigate(Screen.Login.route)
                    } else {
                        navController.navigate(Screen.Chat.createRoute(threadId))
                    }
                },
                onNavigateToRating = { jId ->
                    if (currentUser == null) {
                        navController.navigate(Screen.Login.route)
                    } else {
                        navController.navigate(Screen.Ratings.createRoute(jId))
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }

        composable(
            route = Screen.Applicants.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            ApplicantsScreen(
                jobId = jobId,
                onBackClick = {
                    navController.popBackStack()
                },
                onWorkerClick = { workerId ->
                    navController.navigate(Screen.WorkerProfileView.createRoute(workerId))
                }
            )
        }

        composable(
            route = Screen.WorkerProfileView.route,
            arguments = listOf(navArgument("workerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workerId = backStackEntry.arguments?.getString("workerId") ?: ""
            WorkerProfileView(
                workerId = workerId,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToChat = { threadId ->
                    navController.navigate(Screen.Chat.createRoute(threadId))
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("threadId") { type = NavType.StringType })
        ) { backStackEntry ->
            val threadId = backStackEntry.arguments?.getString("threadId") ?: ""
            ChatScreen(
                threadId = threadId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Ratings.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            RatingsScreen(
                jobId = jobId,
                onBackClick = {
                    navController.popBackStack()
                },
                onSubmitSuccess = {
                    navController.navigate(Screen.OwnerHome.route) {
                        popUpTo(Screen.Ratings.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
