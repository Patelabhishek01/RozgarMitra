package com.rozgarmitra.app.presentation.navigation

sealed class Screen(val route: String) {

    data object Splash : Screen("splash")

    data object LanguageSelect : Screen("language_select")

    data object MainHome : Screen("main_home")

    data object JobResults : Screen("job_results/{title}/{filterType}") {
        fun createRoute(title: String, filterType: String) = "job_results/$title/$filterType"
    }

    data object Settings : Screen("settings")

    data object HelpSupport : Screen("help_support")

    data object MyProfessions : Screen("my_professions")

    data object MyApplications : Screen("my_applications")

    data object Login : Screen("login")

    data object Otp : Screen("otp/{phone}") {
        fun createRoute(phone: String) = "otp/$phone"
    }

    data object Register : Screen("register/{phone}") {
        fun createRoute(phone: String) = "register/$phone"
    }

    data object LabourDetails : Screen("labour_details")

    data object OwnerDetails : Screen("owner_details")

    data object WorkerHome : Screen("worker_home")

    data object OwnerHome : Screen("owner_home")

    data object OwnerActiveJobs : Screen("owner_active_jobs")

    data object OwnerApplications : Screen("owner_applications")

    data object OwnerHiredWorkers : Screen("owner_hired_workers")

    data object JobDetails : Screen("job_details/{jobId}") {
        fun createRoute(jobId: String) = "job_details/$jobId"
    }

    data object Applicants : Screen("applicants/{jobId}") {
        fun createRoute(jobId: String) = "applicants/$jobId"
    }

    data object WorkerProfileView : Screen("worker_profile/{workerId}") {
        fun createRoute(workerId: String) = "worker_profile/$workerId"
    }

    data object Chat : Screen("chat/{threadId}") {
        fun createRoute(threadId: String) = "chat/$threadId"
    }

    data object Ratings : Screen("ratings/{jobId}") {
        fun createRoute(jobId: String) = "ratings/$jobId"
    }
}