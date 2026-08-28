package com.rozgarmitra.app.data

import android.app.Activity
import android.content.Context
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.TimeUnit

object RozgarRepository {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var preferenceManager: PreferenceManager? = null
    
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLanguageSelected = MutableStateFlow(false)
    val isLanguageSelected: StateFlow<Boolean> = _isLanguageSelected

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    val applications: StateFlow<List<JobApplication>> = _applications

    private val _threads = MutableStateFlow<List<ChatThread>>(emptyList())
    val threads: StateFlow<List<ChatThread>> = _threads

    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    private val _pendingAction = MutableStateFlow<(() -> Unit)?>(null)
    val pendingAction: StateFlow<(() -> Unit)?> = _pendingAction

    fun setPendingAction(action: (() -> Unit)?) {
        _pendingAction.value = action
    }

    fun executePendingAction() {
        _pendingAction.value?.invoke()
        _pendingAction.value = null
    }

    init {
        // Preload mock data
        loadMockData()
    }

    fun initialize(context: Context) {
        preferenceManager = PreferenceManager(context)
        val lang = preferenceManager?.getLanguage()
        _isLanguageSelected.value = lang != null
        
        val theme = preferenceManager?.getTheme() ?: ThemeMode.SYSTEM
        _themeMode.value = theme
        
        // Check for Firebase Auth session
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            fetchUserProfile(firebaseUser.uid)
        }
        
        // Fetch jobs from Firestore
        observeJobs()
    }

    private fun fetchUserProfile(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                _currentUser.value = user
            }
    }

    private fun observeJobs() {
        db.collection("jobs").whereEqualTo("status", "ACTIVE")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                val jobList = snapshots?.toObjects(Job::class.java) ?: emptyList()
                _jobs.value = jobList
            }
    }

    fun setLanguage(language: String) {
        preferenceManager?.setLanguage(language)
        _isLanguageSelected.value = true
        _currentUser.value?.let { user ->
            _currentUser.value = user.copy(language = language)
        }
    }

    fun setTheme(mode: ThemeMode) {
        _themeMode.value = mode
        preferenceManager?.setTheme(mode)
        _currentUser.value?.let { user ->
            _currentUser.value = user.copy(theme = mode)
        }
    }

    fun toggleOffline(offline: Boolean) {
        _isOffline.value = offline
    }

    fun toggleJobSaved(jobId: String) {
        val user = _currentUser.value ?: return
        val currentSaved = user.savedJobIds
        val newSaved = if (currentSaved.contains(jobId)) {
            currentSaved - jobId
        } else {
            currentSaved + jobId
        }
        _currentUser.value = user.copy(savedJobIds = newSaved)
        // Persist to Firestore
        scope.launch {
            db.collection("users").document(user.id).update("savedJobIds", newSaved)
        }
    }

    private var verificationId: String? = null

    // --- Authentication ---

    fun login(activity: Activity, phone: String): Flow<Result<Boolean>> = callbackFlow {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Not using auto-verification for simplicity
            }

            override fun onVerificationFailed(e: FirebaseException) {
                trySend(Result.failure(e))
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                this@RozgarRepository.verificationId = verificationId
                trySend(Result.success(true))
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$phone")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
            
        PhoneAuthProvider.verifyPhoneNumber(options)
        
        awaitClose { }
    }

    fun verifyOtp(phone: String, otp: String): Flow<Result<User?>> = flow {
        val id = verificationId ?: return@flow emit(Result.failure(Exception("Verification ID not found")))
        val credential = PhoneAuthProvider.getCredential(id, otp)
        
        try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                // Fetch profile from Firestore
                val doc = db.collection("users").document(firebaseUser.uid).get().await()
                val user = if (doc.exists()) doc.toObject(User::class.java) else null
                _currentUser.value = user
                emit(Result.success(user))
            } else {
                emit(Result.failure(Exception("Authentication failed")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun setVerificationId(id: String) {
        this.verificationId = id
    }

    fun register(name: String, phone: String, role: Role, language: String): Flow<Result<User>> = flow {
        val firebaseUser = auth.currentUser ?: return@flow emit(Result.failure(Exception("Not authenticated")))
        
        val user = User(
            id = firebaseUser.uid,
            name = name,
            phone = phone,
            role = role,
            language = language,
            isVerified = false,
            profileCompleted = false
        )
        
        try {
            db.collection("users").document(user.id).set(user).await()
            _currentUser.value = user
            emit(Result.success(user))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        preferenceManager?.setLoggedInPhone(null)
    }

    // --- Profile Management ---

    fun completeLabourProfile(skills: List<String>, experience: String, expectedWage: Int): Flow<Result<Boolean>> = flow {
        if (_isOffline.value) {
            emit(Result.failure(Exception("Offline: Cannot save profile.")))
            return@flow
        }
        delay(1000)
        val current = _currentUser.value ?: return@flow
        val updated = current.copy(
            profileCompleted = true,
            labourProfile = LabourProfile(
                skills = skills,
                experience = experience,
                expectedWage = expectedWage,
                isAvailable = true,
                rating = 4.0f,
                completedJobsCount = 0
            )
        )
        _currentUser.value = updated
        emit(Result.success(true))
    }

    fun completeOwnerProfile(address: String, companyName: String): Flow<Result<Boolean>> = flow {
        if (_isOffline.value) {
            emit(Result.failure(Exception("Offline: Cannot save profile.")))
            return@flow
        }
        delay(1000)
        val current = _currentUser.value ?: return@flow
        val updated = current.copy(
            profileCompleted = true,
            ownerProfile = OwnerProfile(
                address = address,
                companyName = companyName,
                rating = 4.0f,
                completedJobsCount = 0
            )
        )
        _currentUser.value = updated
        emit(Result.success(true))
    }

    fun toggleAvailability(available: Boolean) {
        val current = _currentUser.value ?: return
        if (current.role == Role.LABOUR) {
            val profile = current.labourProfile ?: LabourProfile()
            _currentUser.value = current.copy(
                labourProfile = profile.copy(isAvailable = available)
            )
        }
    }

    fun uploadIdDocument(docType: String): Flow<Result<Boolean>> = flow {
        if (_isOffline.value) {
            emit(Result.failure(Exception("Offline: Document upload failed.")))
            return@flow
        }
        delay(2000)
        val current = _currentUser.value ?: return@flow
        _currentUser.value = current.copy(isVerified = true) 
        emit(Result.success(true))
    }

    // --- Jobs Flow ---

    fun postJob(
        title: String,
        category: String,
        location: String,
        wage: Int,
        durationDays: Int,
        hoursPerDay: Int,
        perks: List<String>,
        difficulty: String,
        skillsRequired: List<String>,
        isUrgent: Boolean
    ): Flow<Result<Boolean>> = flow {
        if (_isOffline.value) {
            emit(Result.failure(Exception("No internet connection. Saved to local drafts.")))
            return@flow
        }
        delay(1500)
        val current = _currentUser.value ?: return@flow
        val newJob = Job(
            id = "job_${UUID.randomUUID().toString().take(6)}",
            title = title,
            category = category,
            location = location,
            distanceKm = (1..15).random() + Math.random(),
            wage = wage,
            durationDays = durationDays,
            hoursPerDay = hoursPerDay,
            perks = perks,
            difficulty = difficulty,
            skillsRequired = skillsRequired,
            isUrgent = isUrgent,
            ownerId = current.id,
            ownerName = current.name,
            ownerVerified = current.isVerified,
            ownerRating = current.ownerProfile?.rating ?: 5.0f,
            status = JobStatus.ACTIVE
        )
        
        _jobs.value = listOf(newJob) + _jobs.value
        addNotification("Job Posted Successfully", "Your job post '$title' is now active.")
        emit(Result.success(true))
    }

    fun applyForJob(jobId: String): Flow<Result<Boolean>> = flow {
        if (_isOffline.value) {
            emit(Result.failure(Exception("Offline: Could not submit application. Check internet.")))
            return@flow
        }
        delay(1000)
        val user = _currentUser.value ?: return@flow
        val job = _jobs.value.firstOrNull { it.id == jobId } ?: return@flow
        
        val alreadyApplied = _applications.value.any { it.jobId == jobId && it.labourId == user.id }
        if (alreadyApplied) {
            emit(Result.failure(Exception("You have already applied for this job.")))
            return@flow
        }

        val profile = user.labourProfile ?: LabourProfile()
        val newApp = JobApplication(
            id = "app_${UUID.randomUUID().toString().take(6)}",
            jobId = jobId,
            jobTitle = job.title,
            labourId = user.id,
            labourName = user.name,
            labourSkills = profile.skills,
            labourRating = profile.rating,
            labourExperience = profile.experience,
            labourPhone = user.phone,
            status = ApplicationStatus.APPLIED
        )
        
        _applications.value = _applications.value + newApp
        addNotification(
            title = "New Applicant for ${job.title}",
            message = "${user.name} applied for your job post."
        )
        emit(Result.success(true))
    }

    fun acceptApplicant(applicationId: String): Flow<Result<Boolean>> = flow {
        if (_isOffline.value) {
            emit(Result.failure(Exception("Offline: Cannot update status.")))
            return@flow
        }
        delay(800)
        
        val apps = _applications.value.map { app ->
            if (app.id == applicationId) {
                createChatThread(app.labourId, app.labourName, Role.LABOUR, true)
                app.copy(status = ApplicationStatus.ACCEPTED)
            } else app
        }
        _applications.value = apps
        emit(Result.success(true))
    }

    fun rejectApplicant(applicationId: String): Flow<Result<Boolean>> = flow {
        if (_isOffline.value) {
            emit(Result.failure(Exception("Offline: Cannot update status.")))
            return@flow
        }
        delay(800)
        val apps = _applications.value.map { app ->
            if (app.id == applicationId) app.copy(status = ApplicationStatus.REJECTED) else app
        }
        _applications.value = apps
        emit(Result.success(true))
    }

    fun completeJob(jobId: String): Flow<Result<Boolean>> = flow {
        if (_isOffline.value) {
            emit(Result.failure(Exception("Offline: Cannot complete job.")))
            return@flow
        }
        delay(1000)
        
        _jobs.value = _jobs.value.map { job ->
            if (job.id == jobId) job.copy(status = JobStatus.COMPLETED) else job
        }
        
        _applications.value = _applications.value.map { app ->
            if (app.jobId == jobId && app.status == ApplicationStatus.ACCEPTED) {
                app.copy(status = ApplicationStatus.COMPLETED)
            } else app
        }

        emit(Result.success(true))
    }

    fun submitRating(jobId: String, targetUserId: String, stars: Float, comment: String): Flow<Result<Boolean>> = flow {
        if (_isOffline.value) {
            emit(Result.failure(Exception("Offline: Cannot submit rating.")))
            return@flow
        }
        delay(1000)
        emit(Result.success(true))
    }

    // --- Chat Flow ---

    private fun createChatThread(otherId: String, name: String, role: Role, verified: Boolean) {
        val exists = _threads.value.any { it.otherUserId == otherId }
        if (!exists) {
            val newThread = ChatThread(
                id = "thread_$otherId",
                otherUserId = otherId,
                otherUserName = name,
                otherUserRole = role,
                otherUserVerified = verified,
                lastMessageText = "Contract started. You can coordinate here.",
                lastMessageTime = System.currentTimeMillis()
            )
            _threads.value = listOf(newThread) + _threads.value
            _messages.value = _messages.value + ("thread_$otherId" to listOf(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    threadId = "thread_$otherId",
                    senderId = "system",
                    senderName = "System",
                    text = "Contract started. Coordinates and details can be shared here.",
                    type = ChatMessageType.TEXT
                )
            ))
        }
    }

    fun sendMessage(
        threadId: String,
        text: String,
        type: ChatMessageType = ChatMessageType.TEXT,
        mediaDuration: String? = null,
        mediaUrl: String? = null
    ): Flow<Result<Boolean>> = flow {
        if (_isOffline.value) {
            emit(Result.failure(Exception("Offline: Message pending transmission.")))
            return@flow
        }
        
        val sender = _currentUser.value ?: return@flow
        val newMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            senderId = sender.id,
            senderName = sender.name,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = type,
            mediaDuration = mediaDuration,
            mediaUrl = mediaUrl
        )

        val currentMsgs = _messages.value[threadId] ?: emptyList()
        _messages.value = _messages.value + (threadId to (currentMsgs + newMsg))

        val previewText = when (type) {
            ChatMessageType.TEXT -> text
            ChatMessageType.LOCATION -> "📍 Shared Location"
            ChatMessageType.VOICE -> "🎙️ Voice Message ($mediaDuration)"
            ChatMessageType.IMAGE -> "🖼️ Shared Image"
        }
        
        _threads.value = _threads.value.map { thread ->
            if (thread.id == threadId) {
                thread.copy(
                    lastMessageText = previewText,
                    lastMessageTime = System.currentTimeMillis()
                )
            } else thread
        }

        emit(Result.success(true))

        scope.launch {
            delay(2000)
            if (!_isOffline.value) {
                val thread = _threads.value.firstOrNull { it.id == threadId } ?: return@launch
                val replyMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    threadId = threadId,
                    senderId = thread.otherUserId,
                    senderName = thread.otherUserName,
                    text = getMockReply(text),
                    timestamp = System.currentTimeMillis(),
                    type = ChatMessageType.TEXT
                )
                val updatedMsgs = _messages.value[threadId] ?: emptyList()
                _messages.value = _messages.value + (threadId to (updatedMsgs + replyMsg))
                
                _threads.value = _threads.value.map { t ->
                    if (t.id == threadId) {
                        t.copy(
                            lastMessageText = replyMsg.text,
                            lastMessageTime = System.currentTimeMillis(),
                            unreadCount = t.unreadCount + 1
                        )
                    } else t
                }
            }
        }
    }

    private fun getMockReply(userText: String): String {
        val txt = userText.lowercase()
        return when {
            txt.contains("location") || txt.contains("where") -> "I am at Sector 4, near the Metro Station."
            txt.contains("money") -> "Wages will be paid daily."
            else -> "Acha, I will confirm the timings."
        }
    }

    fun addNotification(title: String, message: String) {
        val newNotif = Notification(
            id = UUID.randomUUID().toString(),
            title = title,
            message = message,
            timestamp = System.currentTimeMillis()
        )
        _notifications.value = listOf(newNotif) + _notifications.value
    }

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    // --- Mock Data ---

    private fun loadMockData() {
        val mockJobs = listOf(
            Job(
                id = "job_1",
                title = "Need 3 Masons for Bricklaying",
                category = "Mason",
                location = "HSR Layout, Bengaluru",
                distanceKm = 1.8,
                wage = 750,
                durationDays = 3,
                hoursPerDay = 8,
                perks = listOf("Food Provided", "Transport Provided"),
                difficulty = "Medium",
                skillsRequired = listOf("Brickwork", "Cement Mixing"),
                isUrgent = true,
                ownerId = "owner_1",
                ownerName = "Harish Sharma (Contractor)",
                ownerVerified = true,
                ownerRating = 4.7f,
                status = JobStatus.ACTIVE
            ),
            Job(
                id = "job_2",
                title = "Urgent House Painting Labour",
                category = "Painter",
                location = "Koramangala, Bengaluru",
                distanceKm = 3.2,
                wage = 600,
                durationDays = 5,
                hoursPerDay = 9,
                perks = listOf("Food Provided"),
                difficulty = "Easy",
                skillsRequired = listOf("Wall Scraping"),
                isUrgent = false,
                ownerId = "owner_2",
                ownerName = "Anil Mehta",
                ownerVerified = false,
                ownerRating = 4.2f,
                status = JobStatus.ACTIVE
            ),
            Job(
                id = "job_5",
                title = "Experienced Carpenter for Furniture",
                category = "Carpenter",
                location = "HSR Layout, Bengaluru",
                distanceKm = 0.5,
                wage = 850,
                durationDays = 7,
                hoursPerDay = 8,
                perks = listOf("Tools Provided"),
                difficulty = "Hard",
                skillsRequired = listOf("Wood cutting"),
                isUrgent = true,
                ownerId = "owner_5",
                ownerName = "Rajesh Woods",
                ownerVerified = true,
                ownerRating = 4.8f,
                status = JobStatus.ACTIVE
            ),
            Job(
                id = "job_3",
                title = "Electrician for Commercial Wiring",
                category = "Electrician",
                location = "Indiranagar, Bengaluru",
                distanceKm = 6.4,
                wage = 900,
                durationDays = 2,
                hoursPerDay = 8,
                perks = listOf("Transport Provided"),
                difficulty = "Hard",
                skillsRequired = listOf("Conduit wiring"),
                isUrgent = true,
                ownerId = "owner_3",
                ownerName = "Vikas Buildcon",
                ownerVerified = true,
                ownerRating = 4.9f,
                status = JobStatus.ACTIVE
            ),
            Job(
                id = "job_4",
                title = "Plumber for Clogged Pipeline",
                category = "Plumber",
                location = "Bellandur, Bengaluru",
                distanceKm = 4.8,
                wage = 800,
                durationDays = 1,
                hoursPerDay = 4,
                perks = emptyList(),
                difficulty = "Medium",
                skillsRequired = listOf("Leakage fixes"),
                isUrgent = false,
                ownerId = "owner_4",
                ownerName = "Pratap Reddy",
                ownerVerified = true,
                ownerRating = 4.5f,
                status = JobStatus.ACTIVE
            )
        )
        _jobs.value = mockJobs
    }
}
