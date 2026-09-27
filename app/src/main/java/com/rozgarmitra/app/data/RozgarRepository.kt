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
        val user = _currentUser.value
        if (user != null) {
            _currentUser.value = user.copy(
                latitude = locationData.latitude,
                longitude = locationData.longitude
            )
            val uid = auth.currentUser?.uid
            if (uid != null && uid == user.id) {
                scope.launch {
                    try {
                        val updates = mutableMapOf<String, Any>(
                            "latitude" to locationData.latitude,
                            "longitude" to locationData.longitude
                        )
                        if (locationData.addressName.isNotBlank()) {
                            updates["addressName"] = locationData.addressName
                        }
                        db.collection("users").document(uid).set(updates, SetOptions.merge())
                    } catch (e: Exception) {
                        Log.e("Firebase", "Failed to save user location to Firestore", e)
                    }
                }
            }
        }
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
                    val lat = user?.latitude ?: snapshot.getDouble("latitude") ?: 0.0
                    val lng = user?.longitude ?: snapshot.getDouble("longitude") ?: 0.0
                    val addrName = snapshot.getString("addressName") ?: ""
                    if ((lat != 0.0 || lng != 0.0 || addrName.isNotBlank()) && _userLocationData.value == null) {
                        _userLocationData.value = LocationData(latitude = lat, longitude = lng, addressName = addrName)
                        recalculateJobDistances()
                    }
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

        val query = if (user.role == Role.LABOUR) {
            db.collection("applications").whereEqualTo("labourId", uid)
        } else {
            db.collection("applications").whereEqualTo("ownerId", uid)
        }

        applicationsListener = query.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Log.e("Firebase", "Applications listener failed", e)
                return@addSnapshotListener
            }
            if (snapshots != null) {
                val appList = snapshots.documents.mapNotNull { doc ->
                    try { doc.toObject(JobApplication::class.java)?.copy(id = doc.id) } catch (err: Exception) { null }
                }.sortedByDescending { it.appliedAt }
                Log.d("Firebase", "Applications Synced: ${appList.size}")
                _applications.value = appList
            }
        }
    }

    private fun observeUserNotifications(uid: String) {
        notificationsListener?.remove()
        notificationsListener = db.collection("notifications")
            .whereEqualTo("recipientUserId", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firebase", "Notifications listener failed", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val userNotifs = snapshots.documents.mapNotNull { doc ->
                        try { doc.toObject(Notification::class.java)?.copy(id = doc.id) } catch (err: Exception) { null }
                    }
                    .filter { it.recipientUserId == uid && !it.isRead }
                    .sortedByDescending { it.timestamp }
                    _notifications.value = userNotifs
                }
            }
    }

    private fun observeUserConversations(uid: String) {
        conversationsListener?.remove()
        conversationsListener = db.collection("conversations")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firebase", "Conversations listener failed", e)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val userThreads = snapshots.documents.mapNotNull { doc ->
                        try { doc.toObject(ChatThread::class.java)?.copy(id = doc.id) } catch (err: Exception) { null }
                    }.sortedByDescending { it.lastMessageTime }
                    _threads.value = userThreads
                    
                    // Observe messages for each active thread
                    userThreads.forEach { thread ->
                        observeMessages(thread.id)
                    }
                }
            }
    }

    fun observeMessages(threadId: String) {
        val user = _currentUser.value ?: return
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
                    }.filter { it.senderId == user.id || it.receiverId == user.id || it.threadId == threadId }
                    .sortedBy { it.timestamp }

                    val currentMap = _messages.value.toMutableMap()
                    currentMap[threadId] = messageList
                    _messages.value = currentMap
                }
            }
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

    fun sendPasswordResetEmail(email: String): Flow<Result<Boolean>> = flow {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return@flow emit(Result.failure(Exception("Please enter a valid email address.")))
        }
        try {
            auth.sendPasswordResetEmail(trimmedEmail).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Password reset email failed", e)
            emit(Result.failure(e))
        }
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

    fun skipLabourProfile(): Flow<Result<Boolean>> = flow {
        // Skip does NOT mark profileCompleted=true and does NOT create fake/placeholder data.
        // User can continue using the app while profile status remains incomplete.
        emit(Result.success(true))
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

    fun skipOwnerProfile(): Flow<Result<Boolean>> = flow {
        // Skip does NOT mark profileCompleted=true and does NOT create fake/placeholder data.
        // User can continue using the app while profile status remains incomplete.
        emit(Result.success(true))
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
        if (user.role != Role.OWNER) {
            throw Exception("Unauthorized: Only employers/owners can post jobs.")
        }
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

    fun approveChat(applicationId: String): Flow<Result<Boolean>> = flow {
        val owner = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))

        val appDoc = db.collection("applications").document(applicationId).get().await()
        if (!appDoc.exists()) {
            return@flow emit(Result.failure(Exception("Application not found")))
        }

        val app = appDoc.toObject(JobApplication::class.java)
            ?: return@flow emit(Result.failure(Exception("Failed to read application")))

        if (app.ownerId != owner.id) {
            return@flow emit(Result.failure(Exception("Unauthorized: Only job owner can approve chat")))
        }

        if (app.chatApproved) {
            emit(Result.success(true))
            return@flow
        }

        try {
            db.collection("applications").document(applicationId).update("chatApproved", true).await()

            sendNotificationToUser(
                recipientUserId = app.labourId,
                type = NotificationType.NEW_MESSAGE.name,
                title = "Chat Approved 🎉",
                message = "The employer has approved chat for '${app.jobTitle}'. You can now chat with the employer before hiring.",
                relatedJobId = app.jobId,
                relatedApplicationId = app.id
            )

            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Approve chat failed", e)
            emit(Result.failure(e))
        }
    }

    fun acceptApplicant(applicationId: String): Flow<Result<Boolean>> = flow {
        val owner = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))

        try {
            var workerIdToNotify = ""
            var jobTitleToNotify = ""
            var jobIdToNotify = ""

            db.runTransaction { transaction ->
                val appRef = db.collection("applications").document(applicationId)
                val appSnapshot = transaction.get(appRef)
                if (!appSnapshot.exists()) {
                    throw FirebaseException("Application not found")
                }

                val app = appSnapshot.toObject(JobApplication::class.java)
                    ?: throw FirebaseException("Failed to read application")

                val jobRef = db.collection("jobs").document(app.jobId)
                val jobSnapshot = transaction.get(jobRef)
                if (!jobSnapshot.exists()) {
                    throw FirebaseException("Associated job not found")
                }

                val job = jobSnapshot.toObject(Job::class.java)
                    ?: throw FirebaseException("Failed to read job")

                if (job.ownerId != owner.id && app.ownerId != owner.id) {
                    throw FirebaseException("Unauthorized: Only job owner can accept applicants")
                }

                if (app.status == ApplicationStatus.ACCEPTED) {
                    return@runTransaction
                }

                if (job.acceptedWorkersCount >= job.numberOfWorkersRequired) {
                    throw FirebaseException("Job capacity reached (${job.numberOfWorkersRequired} workers already hired)")
                }

                val newAcceptedCount = job.acceptedWorkersCount + 1
                val newJobStatus = if (newAcceptedCount >= job.numberOfWorkersRequired) JobStatus.FILLED else job.status

                transaction.update(appRef, mapOf(
                    "status" to ApplicationStatus.ACCEPTED.name,
                    "chatApproved" to true
                ))
                transaction.update(jobRef, mapOf(
                    "acceptedWorkersCount" to newAcceptedCount,
                    "status" to newJobStatus.name,
                    "updatedAt" to System.currentTimeMillis()
                ))

                workerIdToNotify = app.labourId
                jobTitleToNotify = job.title
                jobIdToNotify = job.id
            }.await()

            if (workerIdToNotify.isNotBlank()) {
                sendNotificationToUser(
                    recipientUserId = workerIdToNotify,
                    type = NotificationType.APPLICATION_ACCEPTED.name,
                    title = "You Have Been Hired! 🎉",
                    message = "You have been hired for '$jobTitleToNotify'.",
                    relatedJobId = jobIdToNotify,
                    relatedApplicationId = applicationId
                )
            }

            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Accept applicant failed", e)
            emit(Result.failure(e))
        }
    }

    fun rejectApplicant(applicationId: String): Flow<Result<Boolean>> = flow {
        val owner = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))

        val appDoc = db.collection("applications").document(applicationId).get().await()
        if (!appDoc.exists()) {
            return@flow emit(Result.failure(Exception("Application not found")))
        }

        val app = appDoc.toObject(JobApplication::class.java) ?: return@flow emit(Result.failure(Exception("Failed to read application")))
        if (app.ownerId != owner.id) {
            return@flow emit(Result.failure(Exception("Unauthorized: Only job owner can reject applicants")))
        }

        try {
            db.collection("applications").document(applicationId).update("status", ApplicationStatus.REJECTED.name).await()

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

    fun completeJob(jobId: String): Flow<Result<Boolean>> = flow {
        val user = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))

        val jobDoc = db.collection("jobs").document(jobId).get().await()
        if (!jobDoc.exists()) {
            return@flow emit(Result.failure(Exception("Job not found")))
        }

        val job = jobDoc.toJobSafe() ?: return@flow emit(Result.failure(Exception("Failed to read job")))
        if (job.ownerId != user.id) {
            return@flow emit(Result.failure(Exception("Unauthorized: Only the job owner can mark this job as completed")))
        }

        try {
            // Update job status to COMPLETED
            db.collection("jobs").document(jobId).update(
                mapOf(
                    "status" to JobStatus.COMPLETED.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            // Fetch and update all ACCEPTED applications for this job to COMPLETED
            val acceptedAppsQuery = db.collection("applications")
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", ApplicationStatus.ACCEPTED.name)
                .get()
                .await()

            if (!acceptedAppsQuery.isEmpty) {
                val batch = db.batch()
                acceptedAppsQuery.documents.forEach { doc ->
                    batch.update(doc.reference, "status", ApplicationStatus.COMPLETED.name)
                }
                batch.commit().await()

                acceptedAppsQuery.documents.forEach { doc ->
                    val labourId = doc.getString("labourId")
                    if (!labourId.isNullOrBlank()) {
                        sendNotificationToUser(
                            recipientUserId = labourId,
                            type = NotificationType.JOB_STATUS_CHANGED.name,
                            title = "Job Completed! 🏆",
                            message = "The job '${job.title}' has been marked completed by the owner.",
                            relatedJobId = jobId,
                            relatedApplicationId = doc.id
                        )
                    }
                }
            }

            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Complete job failed", e)
            emit(Result.failure(e))
        }
    }

    fun submitRating(jobId: String, targetUserId: String, stars: Float, comment: String): Flow<Result<Boolean>> = flow {
        val reviewer = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))

        val jobDoc = db.collection("jobs").document(jobId).get().await()
        if (!jobDoc.exists()) {
            return@flow emit(Result.failure(Exception("Job not found")))
        }

        val job = jobDoc.toJobSafe() ?: return@flow emit(Result.failure(Exception("Failed to read job")))
        if (job.status != JobStatus.COMPLETED) {
            return@flow emit(Result.failure(Exception("Reviews can only be submitted for completed jobs")))
        }

        // Verify reviewer was accepted/completed worker on this job or is job owner
        val appQuery = db.collection("applications")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("labourId", reviewer.id)
            .get()
            .await()

        val validApp = appQuery.documents.mapNotNull { doc ->
            try { doc.toObject(JobApplication::class.java) } catch (e: Exception) { null }
        }.firstOrNull { it.status == ApplicationStatus.ACCEPTED || it.status == ApplicationStatus.COMPLETED }

        if (validApp == null && reviewer.id != job.ownerId) {
            return@flow emit(Result.failure(Exception("Only accepted workers for this job can submit a review")))
        }

        val targetId = if (reviewer.role == Role.LABOUR) job.ownerId else targetUserId
        if (targetId.isBlank()) {
            return@flow emit(Result.failure(Exception("Invalid target user for rating")))
        }

        val existingReviewQuery = db.collection("ratings")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("reviewerId", reviewer.id)
            .get()
            .await()

        if (!existingReviewQuery.isEmpty) {
            return@flow emit(Result.failure(Exception("You have already submitted a review for this job")))
        }

        val ratingId = "rating_${jobId}_${reviewer.id}"
        val rating = Rating(
            id = ratingId,
            reviewerId = reviewer.id,
            targetId = targetId,
            jobId = jobId,
            stars = stars,
            comment = comment,
            createdAt = System.currentTimeMillis()
        )

        try {
            db.collection("ratings").document(ratingId).set(rating).await()
            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Submit rating failed", e)
            emit(Result.failure(e))
        }
    }

    fun getPublicUserProfile(userId: String): Flow<Result<User>> = flow {
        val current = _currentUser.value
        try {
            val doc = db.collection("users").document(userId).get().await()
            if (!doc.exists()) {
                return@flow emit(Result.failure(Exception("User profile not found")))
            }
            val user = doc.toObject(User::class.java) ?: return@flow emit(Result.failure(Exception("Failed to read user profile")))
            val sanitized = if (current?.id == userId) user else user.copy(phone = "")
            emit(Result.success(sanitized))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // --- Notifications Helper ---

    private fun sendNotificationToUser(
        recipientUserId: String,
        type: String,
        title: String,
        message: String,
        relatedJobId: String = "",
        relatedApplicationId: String = "",
        relatedThreadId: String = ""
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
            relatedThreadId = relatedThreadId,
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
        if (id.isBlank()) return
        _notifications.value = _notifications.value.filter { it.id != id }
        db.collection("notifications").document(id).update("isRead", true)
            .addOnFailureListener { Log.e("Firebase", "Mark notification read failed", it) }
    }

    // --- Chat & Conversations ---

    fun getOrCreateConversation(jobId: String, applicationId: String, targetUserId: String): Flow<Result<String>> = flow {
        try {
            val current = _currentUser.value ?: return@flow emit(Result.failure(Exception("Not logged in")))

            // 1. Verify Job exists and is valid
            val jobDoc = db.collection("jobs").document(jobId).get().await()
            if (!jobDoc.exists()) {
                return@flow emit(Result.failure(Exception("Job no longer exists")))
            }
            val job = jobDoc.toJobSafe() ?: return@flow emit(Result.failure(Exception("Failed to read job")))

            // 2. Fetch/Verify Application
            val appQuery = if (applicationId.isNotBlank()) {
                val appDoc = db.collection("applications").document(applicationId).get().await()
                if (appDoc.exists()) listOf(appDoc) else emptyList()
            } else {
                val workerUid = if (current.role == Role.LABOUR) current.id else targetUserId
                db.collection("applications")
                    .whereEqualTo("jobId", jobId)
                    .whereEqualTo("labourId", workerUid)
                    .get()
                    .await()
                    .documents
            }

            if (appQuery.isEmpty()) {
                return@flow emit(Result.failure(Exception("Apply to this job first to start chatting with the employer.")))
            }

            val appDoc = appQuery.first()
            val app = appDoc.toObject(JobApplication::class.java)
                ?: return@flow emit(Result.failure(Exception("Failed to read application")))

            val canChat = app.status == ApplicationStatus.ACCEPTED ||
                          app.status == ApplicationStatus.COMPLETED ||
                          app.chatApproved

            if (!canChat) {
                if (app.status == ApplicationStatus.REJECTED || app.status == ApplicationStatus.CANCELLED) {
                    return@flow emit(Result.failure(Exception("Chat is unavailable for this application.")))
                }
                return@flow emit(Result.failure(Exception("Chat will be available after your application is accepted or chat is approved.")))
            }

            val resolvedOwnerId = job.ownerId
            val resolvedWorkerId = app.labourId

            val participantIds = listOf(current.id, targetUserId).sorted()
            val expectedParticipants = listOf(resolvedOwnerId, resolvedWorkerId).sorted()

            if (participantIds != expectedParticipants) {
                return@flow emit(Result.failure(Exception("Unauthorized chat creation: Participants do not match job/application")))
            }

            val convId = "conv_${jobId}_${participantIds.joinToString("_")}"

            val convDoc = try {
                db.collection("conversations").document(convId).get().await()
            } catch (ex: Exception) {
                null
            }

            if (convDoc != null && convDoc.exists()) {
                emit(Result.success(convId))
                return@flow
            }

            val ownerName = job.ownerName.ifBlank {
                if (current.id == resolvedOwnerId) current.name else "Employer"
            }
            val workerName = app.labourName.ifBlank {
                if (current.id == resolvedWorkerId) current.name else "Worker"
            }
            val otherUserName = if (targetUserId == resolvedOwnerId) ownerName else workerName
            val otherUserRole = if (targetUserId == resolvedOwnerId) Role.OWNER else Role.LABOUR

            val newThread = ChatThread(
                id = convId,
                jobId = jobId,
                applicationId = app.id.ifBlank { applicationId },
                participants = participantIds,
                ownerId = resolvedOwnerId,
                ownerName = ownerName,
                workerId = resolvedWorkerId,
                workerName = workerName,
                otherUserId = targetUserId,
                otherUserName = otherUserName,
                otherUserRole = otherUserRole,
                otherUserVerified = false,
                lastMessageText = "Conversation started",
                lastMessageTime = System.currentTimeMillis(),
                unreadCount = 0
            )

            db.collection("conversations").document(convId).set(newThread).await()
            emit(Result.success(convId))
        } catch (e: Exception) {
            Log.e("Firebase", "getOrCreateConversation failed", e)
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
        if (!threadDoc.exists()) {
            return@flow emit(Result.failure(Exception("Conversation not found")))
        }

        val thread = threadDoc.toObject(ChatThread::class.java)
            ?: return@flow emit(Result.failure(Exception("Failed to read conversation")))

        if (sender.id !in thread.participants) {
            return@flow emit(Result.failure(Exception("Unauthorized: You are not a participant of this chat")))
        }

        val receiverId = thread.participants.firstOrNull { it != sender.id } ?: ""

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
                    relatedJobId = thread.jobId,
                    relatedApplicationId = thread.applicationId,
                    relatedThreadId = threadId
                )
            }

            emit(Result.success(true))
        } catch (e: Exception) {
            Log.e("Firebase", "Send message failed", e)
            emit(Result.failure(e))
        }
    }
}

