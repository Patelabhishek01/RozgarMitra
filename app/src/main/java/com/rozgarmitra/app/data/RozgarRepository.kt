package com.rozgarmitra.app.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.TimeUnit

object RozgarRepository {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var preferenceManager: PreferenceManager? = null

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var jobsListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var applicationsListener: ListenerRegistration? = null
    private var notificationsListener: ListenerRegistration? = null
    private var conversationsListener: ListenerRegistration? = null

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

    private val _userLocationData = MutableStateFlow<LocationData?>(null)
    val userLocationData: StateFlow<LocationData?> = _userLocationData

    private val _searchRadiusKm = MutableStateFlow<Double>(0.0) // 0.0 = All Jobs
    val searchRadiusKm: StateFlow<Double> = _searchRadiusKm

    fun updateUserLocation(locationData: LocationData) {
        _userLocationData.value = locationData
        recalculateJobDistances()
    }

    fun setSearchRadius(radiusKm: Double) {
        _searchRadiusKm.value = radiusKm
    }

    fun recalculateJobDistances() {
        val userLoc = _userLocationData.value ?: return
        val currentJobs = _jobs.value
        val updatedJobs = currentJobs.map { job ->
            if (job.latitude != 0.0 && job.longitude != 0.0) {
                val dist = LocationHelper.calculateDistanceKm(
                    userLoc.latitude, userLoc.longitude,
                    job.latitude, job.longitude
                )
                job.copy(distanceKm = dist)
            } else job
        }
        _jobs.value = updatedJobs
    }


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
        // Mock data omitted for live Firestore usage
    }

    fun initialize(context: Context) {
        preferenceManager = PreferenceManager(context)
        val lang = preferenceManager?.getLanguage()
        _isLanguageSelected.value = lang != null

        val theme = preferenceManager?.getTheme() ?: ThemeMode.SYSTEM
        _themeMode.value = theme

        // Always observe jobs for public and logged in users
        observeJobs()

        // Auth state listener ensures real-time updates when user switches accounts or logs out
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                fetchUserProfile(firebaseUser.uid)
            } else {
                userListener?.remove()
                userListener = null
                applicationsListener?.remove()
                applicationsListener = null
                notificationsListener?.remove()
                notificationsListener = null
                conversationsListener?.remove()
                conversationsListener = null
                _currentUser.value = null
                observeJobs()
            }
        }
    }

    private fun fetchUserProfile(uid: String) {
        userListener?.remove()
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firebase", "User profile listen failed", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = try {
                        val parsed = snapshot.toObject(User::class.java)
                        if (parsed != null && parsed.id.isBlank()) parsed.copy(id = snapshot.id) else parsed
                    } catch (err: Exception) {
                        Log.e("Firebase", "Failed to parse user profile ${snapshot.id}", err)
                        try {
                            val name = snapshot.getString("name") ?: ""
                            val phone = snapshot.getString("phone") ?: ""
                            val roleStr = snapshot.getString("role") ?: "LABOUR"
                            val role = try { Role.valueOf(roleStr.uppercase()) } catch (ex: Exception) { Role.LABOUR }
                            val language = snapshot.getString("language") ?: "English"
                            val isVerified = snapshot.getBoolean("isVerified") ?: false
                            val profileCompleted = snapshot.getBoolean("profileCompleted") ?: false
                            User(id = snapshot.id, name = name, phone = phone, role = role, language = language, isVerified = isVerified, profileCompleted = profileCompleted)
                        } catch (fatal: Exception) { null }
                    }
                    _currentUser.value = user
                    user?.id?.let { userId ->
                        observeUserApplications(userId)
                        observeUserNotifications(userId)
                        observeUserConversations(userId)
                    }
                }
            }
        observeJobs()
    }

    fun observeJobs() {
        jobsListener?.remove()
        Log.d("Firebase", "Starting Jobs observer...")
        jobsListener = db.collection("jobs")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firebase", "Jobs observer failed", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    var jobList = snapshots.documents.mapNotNull { doc -> doc.toJobSafe() }
                        .sortedByDescending { it.createdAt }
                    val userLoc = _userLocationData.value
                    if (userLoc != null) {
                        jobList = jobList.map { job ->
                            if (job.latitude != 0.0 && job.longitude != 0.0) {
                                val dist = LocationHelper.calculateDistanceKm(
                                    userLoc.latitude, userLoc.longitude,
                                    job.latitude, job.longitude
                                )
                                job.copy(distanceKm = dist)
                            } else job
                        }
                    }
                    Log.d("Firebase", "Jobs Snapshot received. Total count: ${jobList.size}")
                    _jobs.value = jobList
                }

            }
    }

    private fun DocumentSnapshot.toJobSafe(): Job? {
        if (!exists()) return null
        return try {
            val parsed = toObject(Job::class.java)
            if (parsed != null) {
                val finalId = if (parsed.id.isNotBlank()) parsed.id else id
                parsed.copy(id = finalId)
            } else null
        } catch (e: Exception) {
            try {
                val title = getString("title") ?: ""
                val category = getString("category") ?: ""
                val description = getString("description") ?: ""
                val location = getString("location") ?: ""
                val latitude = getDouble("latitude") ?: 0.0
                val longitude = getDouble("longitude") ?: 0.0
                val distanceKm = getDouble("distanceKm") ?: 0.0
                val date = getString("date") ?: ""
                val startTime = getString("startTime") ?: ""
                val duration = getString("duration") ?: ""
                val durationDays = getLong("durationDays")?.toInt() ?: 1
                val hoursPerDay = getLong("hoursPerDay")?.toInt() ?: 8
                val numberOfWorkersRequired = getLong("numberOfWorkersRequired")?.toInt() ?: 1
                val acceptedWorkersCount = getLong("acceptedWorkersCount")?.toInt() ?: 0
                val wage = getLong("wage")?.toInt() ?: (getDouble("wage")?.toInt() ?: 0)
                val wageType = getString("wageType") ?: "per day"
                val perks = (get("perks") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val difficulty = getString("difficulty") ?: "Medium"
                val skillsRequired = (get("skillsRequired") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val isUrgent = getBoolean("isUrgent") ?: false
                val ownerId = getString("ownerId") ?: ""
                val ownerName = getString("ownerName") ?: ""
                val ownerVerified = getBoolean("ownerVerified") ?: false
                val ownerRating = getDouble("ownerRating")?.toFloat() ?: 5.0f
                
                val statusStr = getString("status") ?: "ACTIVE"
                val status = try {
                    JobStatus.valueOf(statusStr.uppercase())
                } catch (err: Exception) {
                    JobStatus.ACTIVE
                }
                
                val createdAt = getLong("createdAt") ?: System.currentTimeMillis()
                val updatedAt = getLong("updatedAt") ?: System.currentTimeMillis()

                Job(
                    id = id,
                    title = title,
                    category = category,
                    description = description,
                    location = location,
                    latitude = latitude,
                    longitude = longitude,
                    distanceKm = distanceKm,
                    date = date,
                    startTime = startTime,
                    duration = duration,
                    durationDays = durationDays,
                    hoursPerDay = hoursPerDay,
                    numberOfWorkersRequired = numberOfWorkersRequired,
                    acceptedWorkersCount = acceptedWorkersCount,
                    wage = wage,
                    wageType = wageType,
                    perks = perks,
                    difficulty = difficulty,
                    skillsRequired = skillsRequired,
                    isUrgent = isUrgent,
                    ownerId = ownerId,
                    ownerName = ownerName,
                    ownerVerified = ownerVerified,
                    ownerRating = ownerRating,
                    status = status,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            } catch (fatal: Exception) {
                Log.e("Firebase", "Fatal job parsing error for $id", fatal)
                null
            }
        }
    }

    private fun observeUserApplications(uid: String) {
        val user = _currentUser.value ?: return
        applicationsListener?.remove()
        applicationsListener = db.collection("applications")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firebase", "Applications listener failed", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val allApps = snapshots.documents.mapNotNull { doc ->
                        try { doc.toObject(JobApplication::class.java)?.copy(id = doc.id) } catch (err: Exception) { null }
                    }
                    val filtered = if (user.role == Role.LABOUR) {
                        allApps.filter { it.labourId == uid }
                    } else {
                        allApps.filter { it.ownerId == uid || _jobs.value.any { job -> job.ownerId == uid && job.id == it.jobId } }
                    }
                    Log.d("Firebase", "Applications Synced: ${filtered.size}")
                    _applications.value = filtered
                }
            }
    }

    private fun observeUserNotifications(uid: String) {
        notificationsListener?.remove()
        notificationsListener = db.collection("notifications")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firebase", "Notifications listener failed", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val allNotifs = snapshots.documents.mapNotNull { doc ->
                        try { doc.toObject(Notification::class.java)?.copy(id = doc.id) } catch (err: Exception) { null }
                    }
                    val userNotifs = allNotifs
                        .filter { it.recipientUserId == uid }
                        .sortedByDescending { it.timestamp }
                    _notifications.value = userNotifs
                }
            }
    }

    private fun observeUserConversations(uid: String) {
        conversationsListener?.remove()
        conversationsListener = db.collection("conversations")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firebase", "Conversations listener failed", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val allThreads = snapshots.documents.mapNotNull { doc ->
                        try { doc.toObject(ChatThread::class.java)?.copy(id = doc.id) } catch (err: Exception) { null }
                    }
                    val userThreads = allThreads.filter { uid in it.participants || it.ownerId == uid || it.workerId == uid }
                    _threads.value = userThreads
                    
                    // Observe messages for each active thread
                    userThreads.forEach { thread ->
                        observeMessages(thread.id)
                    }
                }
            }
    }

    fun observeMessages(threadId: String) {
        db.collection("messages")
            .whereEqualTo("threadId", threadId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firebase", "Messages listener failed", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val messageList = snapshots.documents.mapNotNull { doc ->
                        try { doc.toObject(ChatMessage::class.java)?.copy(id = doc.id) } catch (err: Exception) { null }
                    }.sortedBy { it.timestamp }
                    val currentMap = _messages.value.toMutableMap()
                    currentMap[threadId] = messageList
                    _messages.value = currentMap
                }
            }
    }

    fun seedDatabase() {
        val mockJobs = listOf(
            Job("job_1", "Need 3 Masons for Bricklaying", "Mason", "Need bricklayer helpers", "HSR Layout, Bengaluru", 12.91, 77.64, 1.8, "25 Sept", "8:00 AM", "1 Day", 1, 8, 3, 0, 750, "per day", listOf("Food"), "Medium", listOf("Brickwork"), true, "owner_1", "Harish Sharma", true, 4.7f, JobStatus.ACTIVE),
            Job("job_2", "House Painting Helper", "Painter", "Wall scraper needed", "Koramangala, Bengaluru", 12.93, 77.62, 3.2, "26 Sept", "9:00 AM", "2 Days", 2, 9, 2, 0, 600, "per day", listOf("Food"), "Easy", listOf("Wall Scraping"), false, "owner_2", "Anil Mehta", false, 4.2f, JobStatus.ACTIVE)
        )
        mockJobs.forEach { db.collection("jobs").document(it.id).set(it) }
    }

    fun setLanguage(language: String) {
        preferenceManager?.setLanguage(language)
        _isLanguageSelected.value = true
        _currentUser.value?.let { user ->
            db.collection("users").document(user.id).update("language", language)
        }
    }

    fun setTheme(mode: ThemeMode) {
        _themeMode.value = mode
        preferenceManager?.setTheme(mode)
        _currentUser.value?.let { user ->
            db.collection("users").document(user.id).update("theme", mode)
        }
    }

    fun toggleOffline(offline: Boolean) {
        _isOffline.value = offline
    }

    // --- Auth ---

    fun loginWithEmail(email: String, password: String): Flow<Result<User?>> = flow {
        try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val doc = db.collection("users").document(firebaseUser.uid).get().await()
                val user = if (doc.exists()) doc.toObject(User::class.java) else null
                _currentUser.value = user
                user?.id?.let { uid ->
                    observeUserApplications(uid)
                    observeUserNotifications(uid)
                    observeUserConversations(uid)
                }
                emit(Result.success(user))
            } else { emit(Result.failure(Exception("Login failed: User null"))) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun loginWithGoogle(idToken: String): Flow<Result<User?>> = flow {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val doc = db.collection("users").document(firebaseUser.uid).get().await()
                val user = if (doc.exists()) doc.toObject(User::class.java) else null
                _currentUser.value = user
                user?.id?.let { uid ->
                    observeUserApplications(uid)
                    observeUserNotifications(uid)
                    observeUserConversations(uid)
                }
                emit(Result.success(user))
            } else { emit(Result.failure(Exception("Google Sign-In failed"))) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun registerWithEmail(email: String, password: String, name: String, role: Role, language: String): Flow<Result<User>> = flow {
        try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val user = User(firebaseUser.uid, name, email, role, language, ThemeMode.SYSTEM, false, false)
                db.collection("users").document(user.id).set(user).await()
                _currentUser.value = user
                observeUserApplications(user.id)
                observeUserNotifications(user.id)
                observeUserConversations(user.id)
                emit(Result.success(user))
            } else { emit(Result.failure(Exception("Registration failed"))) }
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun login(activity: Activity, phone: String): Flow<Result<Boolean>> = callbackFlow {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {}
            override fun onVerificationFailed(e: FirebaseException) { trySend(Result.failure(e)) }
            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                verificationId = id
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

    private var verificationId: String? = null

    fun verifyOtp(phone: String, otp: String): Flow<Result<User?>> = flow {
        val id = verificationId ?: return@flow emit(Result.failure(Exception("Verification Session Expired")))
        val credential = PhoneAuthProvider.getCredential(id, otp)
        try {
            val res = auth.signInWithCredential(credential).await()
            val uid = res.user?.uid ?: return@flow emit(Result.failure(Exception("Authentication Failed")))
            val doc = db.collection("users").document(uid).get().await()
            val user = if (doc.exists()) doc.toObject(User::class.java) else null
            _currentUser.value = user
            user?.id?.let { userId ->
                observeUserApplications(userId)
                observeUserNotifications(userId)
                observeUserConversations(userId)
            }
            emit(Result.success(user))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun register(name: String, phone: String, role: Role, language: String): Flow<Result<User>> = flow {
        val uid = auth.currentUser?.uid ?: return@flow emit(Result.failure(Exception("Not Authenticated")))
        val user = User(uid, name, phone, role, language, ThemeMode.SYSTEM, false, false)
        try {
            db.collection("users").document(uid).set(user).await()
            _currentUser.value = user
            observeUserApplications(uid)
            observeUserNotifications(uid)
            observeUserConversations(uid)
            emit(Result.success(user))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun logout() {
        auth.signOut()
        userListener?.remove()
        userListener = null
        applicationsListener?.remove()
        applicationsListener = null
        notificationsListener?.remove()
        notificationsListener = null
        conversationsListener?.remove()
        conversationsListener = null

        _currentUser.value = null
        _applications.value = emptyList()
        _threads.value = emptyList()
        _messages.value = emptyMap()
        _notifications.value = emptyList()
        preferenceManager?.setLoggedInPhone(null)

        // Refresh jobs observer for guest/logged-out view
        observeJobs()
    }

    // --- Profiles ---

    fun completeLabourProfile(skills: List<String>, experience: String, wage: Int): Flow<Result<Boolean>> = flow {
        val current = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))
        val updated = current.copy(profileCompleted = true, labourProfile = LabourProfile(skills, experience, wage))
        try {
            db.collection("users").document(current.id).set(updated).await()
            _currentUser.value = updated
            emit(Result.success(true))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun completeOwnerProfile(address: String, company: String): Flow<Result<Boolean>> = flow {
        val current = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))
        val updated = current.copy(profileCompleted = true, ownerProfile = OwnerProfile(address, company))
        try {
            db.collection("users").document(current.id).set(updated).await()
            _currentUser.value = updated
            emit(Result.success(true))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun suggestNewCategory(name: String, description: String): Flow<Result<Boolean>> = flow {
        val user = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))
        val suggestion = mapOf(
            "name" to name,
            "description" to description,
            "suggestedBy" to user.id,
            "timestamp" to System.currentTimeMillis(),
            "status" to "PENDING"
        )
        try {
            db.collection("category_suggestions").add(suggestion).await()
            emit(Result.success(true))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun toggleAvailability(available: Boolean) {
        val user = _currentUser.value ?: return
        if (user.role == Role.LABOUR) {
            val profile = user.labourProfile ?: LabourProfile()
            val updatedProfile = profile.copy(isAvailable = available)
            val updateMap = mapOf("labourProfile" to updatedProfile)

            db.collection("users").document(user.id)
                .set(updateMap, SetOptions.merge())
                .addOnSuccessListener { Log.d("Firebase", "Availability saved: $available") }
        }
    }

    fun uploadIdDocument(type: String): Flow<Result<Boolean>> = flow {
        delay(1000)
        _currentUser.value?.let { user ->
            db.collection("users").document(user.id).update("isVerified", true)
        }
        emit(Result.success(true))
    }

    // --- Jobs ---

    fun postJob(
        title: String,
        category: String,
        description: String = "",
        location: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        date: String = "",
        startTime: String = "",
        duration: String = "",
        numberOfWorkersRequired: Int = 1,
        wage: Int,
        wageType: String = "per day",
        days: Int = 1,
        hours: Int = 8,
        perks: List<String> = emptyList(),
        difficulty: String = "Medium",
        skills: List<String> = emptyList(),
        urgent: Boolean = false
    ): Flow<Result<Boolean>> = flow {
        val user = _currentUser.value ?: throw Exception("User not logged in")
        val jobId = UUID.randomUUID().toString()

        val userLoc = _userLocationData.value
        val calculatedDistance = if (userLoc != null && latitude != 0.0 && longitude != 0.0) {
            LocationHelper.calculateDistanceKm(userLoc.latitude, userLoc.longitude, latitude, longitude)
        } else 0.0

        val job = Job(
            id = jobId,
            title = title,
            category = category,
            description = description,
            location = location,
            latitude = latitude,
            longitude = longitude,
            distanceKm = calculatedDistance,

            date = date,
            startTime = startTime,
            duration = duration,
            durationDays = days,
            hoursPerDay = hours,
            numberOfWorkersRequired = numberOfWorkersRequired,
            acceptedWorkersCount = 0,
            wage = wage,
            wageType = wageType,
            perks = perks,
            difficulty = difficulty,
            skillsRequired = skills,
            isUrgent = urgent,
            ownerId = user.id,
            ownerName = user.name,
            ownerVerified = user.isVerified,
            ownerRating = 5.0f,
            status = JobStatus.ACTIVE,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        try {
            db.collection("jobs").document(job.id).set(job).await()
            sendNotificationToUser(
                recipientUserId = user.id,
                type = NotificationType.JOB_STATUS_CHANGED.name,
                title = "Job Posted Successfully",
                message = "Your post '$title' is now live for workers.",
                relatedJobId = jobId
            )
            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Post job failed", e)
            emit(Result.failure(e))
        }
    }

    fun applyForJob(jobId: String): Flow<Result<Boolean>> = flow {
        val user = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))

        // Fetch fresh job details from Firestore
        val jobDoc = db.collection("jobs").document(jobId).get().await()
        if (!jobDoc.exists()) {
            return@flow emit(Result.failure(Exception("Job no longer exists")))
        }

        val job = jobDoc.toObject(Job::class.java) ?: return@flow emit(Result.failure(Exception("Failed to read job")))

        if (job.status == JobStatus.FILLED || job.status == JobStatus.CLOSED || job.acceptedWorkersCount >= job.numberOfWorkersRequired) {
            return@flow emit(Result.failure(Exception("Job is already filled or closed.")))
        }

        // Check for duplicate application in Firestore
        val existingAppsQuery = db.collection("applications")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("labourId", user.id)
            .get()
            .await()

        if (!existingAppsQuery.isEmpty) {
            return@flow emit(Result.failure(Exception("You have already responded to this job.")))
        }

        val appId = "app_${UUID.randomUUID()}"
        val app = JobApplication(
            id = appId,
            jobId = jobId,
            jobTitle = job.title,
            ownerId = job.ownerId,
            labourId = user.id,
            labourName = user.name,
            labourSkills = user.labourProfile?.skills ?: emptyList(),
            labourRating = user.labourProfile?.rating ?: 4.5f,
            labourExperience = user.labourProfile?.experience ?: "Available",
            labourPhone = user.phone,
            status = ApplicationStatus.APPLIED,
            appliedAt = System.currentTimeMillis()
        )

        try {
            db.collection("applications").document(app.id).set(app).await()

            // Notify Owner
            sendNotificationToUser(
                recipientUserId = job.ownerId,
                type = NotificationType.JOB_APPLICATION.name,
                title = "${user.name} responded to your job",
                message = "${job.title} – ₹${job.wage}/${job.wageType}",
                relatedJobId = jobId,
                relatedApplicationId = app.id
            )

            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Apply job failed", e)
            emit(Result.failure(e))
        }
    }

    fun acceptApplicant(applicationId: String): Flow<Result<Boolean>> = flow {
        val owner = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))

        val appDoc = db.collection("applications").document(applicationId).get().await()
        if (!appDoc.exists()) {
            return@flow emit(Result.failure(Exception("Application not found")))
        }

        val app = appDoc.toObject(JobApplication::class.java) ?: return@flow emit(Result.failure(Exception("Failed to read application")))
        val jobDoc = db.collection("jobs").document(app.jobId).get().await()
        if (!jobDoc.exists()) {
            return@flow emit(Result.failure(Exception("Associated job not found")))
        }

        val job = jobDoc.toObject(Job::class.java) ?: return@flow emit(Result.failure(Exception("Failed to read job")))

        if (job.acceptedWorkersCount >= job.numberOfWorkersRequired) {
            return@flow emit(Result.failure(Exception("Job capacity reached (${job.numberOfWorkersRequired} workers already hired)")))
        }

        val newAcceptedCount = job.acceptedWorkersCount + 1
        val newJobStatus = if (newAcceptedCount >= job.numberOfWorkersRequired) JobStatus.FILLED else job.status

        try {
            // Update application status
            db.collection("applications").document(applicationId).update("status", ApplicationStatus.ACCEPTED).await()

            // Update job accepted count and status if filled
            db.collection("jobs").document(job.id).update(
                mapOf(
                    "acceptedWorkersCount" to newAcceptedCount,
                    "status" to newJobStatus,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            // Send notification to worker
            sendNotificationToUser(
                recipientUserId = app.labourId,
                type = NotificationType.APPLICATION_ACCEPTED.name,
                title = "Application Accepted! 🎉",
                message = "Your application for '${job.title}' was accepted by ${owner.name}.",
                relatedJobId = job.id,
                relatedApplicationId = app.id
            )

            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Accept applicant failed", e)
            emit(Result.failure(e))
        }
    }

    fun rejectApplicant(applicationId: String): Flow<Result<Boolean>> = flow {
        val appDoc = db.collection("applications").document(applicationId).get().await()
        if (!appDoc.exists()) {
            return@flow emit(Result.failure(Exception("Application not found")))
        }

        val app = appDoc.toObject(JobApplication::class.java) ?: return@flow emit(Result.failure(Exception("Failed to read application")))

        try {
            db.collection("applications").document(applicationId).update("status", ApplicationStatus.REJECTED).await()

            sendNotificationToUser(
                recipientUserId = app.labourId,
                type = NotificationType.APPLICATION_REJECTED.name,
                title = "Application Update",
                message = "Your application for '${app.jobTitle}' was not selected.",
                relatedJobId = app.jobId,
                relatedApplicationId = app.id
            )

            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Reject applicant failed", e)
            emit(Result.failure(e))
        }
    }

    fun completeJob(id: String): Flow<Result<Boolean>> = flow {
        try {
            db.collection("jobs").document(id).update(
                mapOf(
                    "status" to JobStatus.COMPLETED,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            emit(Result.success(true))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    fun submitRating(jobId: String, targetUserId: String, stars: Float, comment: String): Flow<Result<Boolean>> = flow {
        val rating = mapOf("jobId" to jobId, "targetId" to targetUserId, "stars" to stars, "comment" to comment)
        try {
            db.collection("ratings").add(rating).await()
            emit(Result.success(true))
        } catch (e: Exception) { emit(Result.failure(e)) }
    }

    // --- Notifications Helper ---

    private fun sendNotificationToUser(
        recipientUserId: String,
        type: String,
        title: String,
        message: String,
        relatedJobId: String = "",
        relatedApplicationId: String = ""
    ) {
        val notifId = "notif_${UUID.randomUUID()}"
        val notif = Notification(
            id = notifId,
            recipientUserId = recipientUserId,
            type = type,
            title = title,
            message = message,
            relatedJobId = relatedJobId,
            relatedApplicationId = relatedApplicationId,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        db.collection("notifications").document(notifId).set(notif)
            .addOnFailureListener { Log.e("Firebase", "Send notification failed", it) }
    }

    fun addNotification(title: String, msg: String) {
        val user = _currentUser.value ?: return
        sendNotificationToUser(user.id, NotificationType.JOB_STATUS_CHANGED.name, title, msg)
    }

    fun markNotificationRead(id: String) {
        db.collection("notifications").document(id).update("isRead", true)
            .addOnFailureListener { Log.e("Firebase", "Mark notification read failed", it) }
    }

    // --- Chat & Conversations ---

    fun getOrCreateConversation(jobId: String, applicationId: String, targetUserId: String): Flow<Result<String>> = flow {
        val current = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))

        val participantIds = listOf(current.id, targetUserId).sorted()
        val convId = "conv_${jobId}_${participantIds.joinToString("_")}"

        val convDoc = db.collection("conversations").document(convId).get().await()
        if (convDoc.exists()) {
            emit(Result.success(convId))
            return@flow
        }

        // Fetch target user metadata
        val targetUserDoc = db.collection("users").document(targetUserId).get().await()
        val targetUser = if (targetUserDoc.exists()) targetUserDoc.toObject(User::class.java) else null

        val ownerId = if (current.role == Role.OWNER) current.id else targetUserId
        val workerId = if (current.role == Role.LABOUR) current.id else targetUserId

        val newThread = ChatThread(
            id = convId,
            jobId = jobId,
            applicationId = applicationId,
            participants = participantIds,
            ownerId = ownerId,
            workerId = workerId,
            otherUserId = targetUserId,
            otherUserName = targetUser?.name ?: "User",
            otherUserRole = targetUser?.role ?: Role.LABOUR,
            otherUserVerified = targetUser?.isVerified ?: false,
            lastMessageText = "Conversation started",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0
        )

        try {
            db.collection("conversations").document(convId).set(newThread).await()
            emit(Result.success(convId))
        } catch (e: Exception) {
            Log.e("Firebase", "Create conversation failed", e)
            emit(Result.failure(e))
        }
    }

    fun sendMessage(
        threadId: String,
        text: String,
        type: ChatMessageType = ChatMessageType.TEXT,
        mediaDuration: String? = null,
        mediaUrl: String? = null
    ): Flow<Result<Boolean>> = flow {
        val sender = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))

        val threadDoc = db.collection("conversations").document(threadId).get().await()
        val thread = if (threadDoc.exists()) threadDoc.toObject(ChatThread::class.java) else null

        val receiverId = thread?.participants?.firstOrNull { it != sender.id } ?: ""

        val msgId = "msg_${UUID.randomUUID()}"
        val msg = ChatMessage(
            id = msgId,
            threadId = threadId,
            senderId = sender.id,
            senderName = sender.name,
            receiverId = receiverId,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = type,
            mediaDuration = mediaDuration,
            mediaUrl = mediaUrl,
            isRead = false
        )

        try {
            db.collection("messages").document(msgId).set(msg).await()

            // Update thread last message
            db.collection("conversations").document(threadId).update(
                mapOf(
                    "lastMessageText" to text,
                    "lastMessageTime" to System.currentTimeMillis()
                )
            ).await()

            if (receiverId.isNotBlank()) {
                sendNotificationToUser(
                    recipientUserId = receiverId,
                    type = NotificationType.NEW_MESSAGE.name,
                    title = "New Message from ${sender.name}",
                    message = text,
                    relatedJobId = thread?.jobId ?: ""
                )
            }

            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Send message failed", e)
            emit(Result.failure(e))
        }
    }
}

