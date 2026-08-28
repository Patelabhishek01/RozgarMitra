package com.rozgarmitra.app.data

enum class Role {
    LABOUR, OWNER
}

enum class JobStatus {
    ACTIVE, COMPLETED, DRAFT
}

enum class ApplicationStatus {
    APPLIED, ACCEPTED, COMPLETED, REJECTED
}

enum class ChatMessageType {
    TEXT, LOCATION, VOICE, IMAGE
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

data class LabourProfile(
    val skills: List<String> = emptyList(),
    val experience: String = "",
    val expectedWage: Int = 0,
    val isAvailable: Boolean = true,
    val rating: Float = 4.2f,
    val completedJobsCount: Int = 0,
    val portfolioImages: List<Int> = emptyList() // Mock resource IDs or offsets
)

data class OwnerProfile(
    val address: String = "",
    val companyName: String = "",
    val rating: Float = 4.5f,
    val completedJobsCount: Int = 0
)

data class User(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val role: Role = Role.LABOUR,
    val language: String = "English",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val isVerified: Boolean = false,
    val profileCompleted: Boolean = false,
    val labourProfile: LabourProfile? = null,
    val ownerProfile: OwnerProfile? = null,
    val savedJobIds: List<String> = emptyList()
)

data class Job(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val location: String = "",
    val distanceKm: Double = 0.0,
    val wage: Int = 0,
    val durationDays: Int = 1,
    val hoursPerDay: Int = 8,
    val perks: List<String> = emptyList(), // Food, Accommodation, Transport
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val skillsRequired: List<String> = emptyList(),
    val isUrgent: Boolean = false,
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerVerified: Boolean = false,
    val ownerRating: Float = 5.0f,
    val status: JobStatus = JobStatus.ACTIVE
)

data class JobApplication(
    val id: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val labourId: String = "",
    val labourName: String = "",
    val labourSkills: List<String> = emptyList(),
    val labourRating: Float = 0.0f,
    val labourExperience: String = "",
    val labourPhone: String = "",
    val status: ApplicationStatus = ApplicationStatus.APPLIED
)

data class ChatMessage(
    val id: String = "",
    val threadId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: ChatMessageType = ChatMessageType.TEXT,
    val mediaDuration: String? = null, // e.g. "0:12" for voice
    val mediaUrl: String? = null // for image URL simulation
)

data class ChatThread(
    val id: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserRole: Role = Role.LABOUR,
    val otherUserVerified: Boolean = false,
    val lastMessageText: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
