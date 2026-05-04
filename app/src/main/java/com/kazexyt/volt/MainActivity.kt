package com.kazexyt.volt

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Project Imports
import com.kazexyt.volt.ui.theme.*
import com.kazexyt.volt.model.*
import com.kazexyt.volt.utils.*
import com.kazexyt.volt.widget.VoltWidget
import com.kazexyt.volt.model.WaterLog
import com.kazexyt.volt.ui.VoltFloatingNavBar

// Supabase & Ktor
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.*
import io.github.jan.supabase.functions.*
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.auth.providers.builtin.Email
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.statement.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kazexyt.volt.utils.AiMealParser

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

// 1. GLOBAL SUPABASE CLIENT (Singleton)
@OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
val supabase = createSupabaseClient(
    supabaseUrl = "https://hstlhmqcennuvkcrfaos.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhzdGxobXFjZW5udXZrY3JmYW9zIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYzNTIxMDUsImV4cCI6MjA5MTkyODEwNX0.tNd-RQvKMikCLwEPqugpJFYnMyp66Jcgcm7fXJs1dy8"
) {
    install(Postgrest)
    install(Auth)
    install(Functions)

    defaultSerializer = KotlinXSerializer(Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    })
    httpConfig {
        install(HttpTimeout) {
            requestTimeoutMillis = 45_000
            connectTimeoutMillis = 45_000
            socketTimeoutMillis  = 45_000
        }
    }
}

val jsonParser = Json { ignoreUnknownKeys = true }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            false
        }

        enableEdgeToEdge()

        setContent {
            VoltTheme {
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                // --- APP STATES ---
                val sessionStatus by supabase.auth.sessionStatus.collectAsState(initial = SessionStatus.Initializing)
                val isLoggedIn = sessionStatus is SessionStatus.Authenticated

                var isCheckingProfile by remember { mutableStateOf(true) }
                var isRefreshing by remember { mutableStateOf(false) }

                var userProfile by remember { mutableStateOf<UserProfile?>(null) }
                val loggedMeals = remember { mutableStateListOf<MealLog>() }
                var waterLogs by remember {
                    mutableStateOf<List<WaterLog>>(emptyList())
                }
                var currentWaterIntake by remember { mutableIntStateOf(0) }

                // --- UI OVERLAYS ---
                var showManualEntry by remember { mutableStateOf(false) }
                var isScanning by remember { mutableStateOf(false) }
                var currentMealData by remember { mutableStateOf<MealLog?>(null) }

                // --- VOICE STATE ---
                var isProcessingVoice by remember { mutableStateOf(false) }

                val voiceLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        val spokenText = data?.firstOrNull()

                        if (!spokenText.isNullOrBlank()) {
                            isProcessingVoice = true
                            coroutineScope.launch {
                                try {
                                    val parsedMeal = AiMealParser.parseSpokenMeal(spokenText)
                                    currentMealData = parsedMeal
                                    showManualEntry = true
                                } catch (e: Exception) {
                                    android.util.Log.e("VoltVoice", "FATAL VOICE ERROR", e)

                                    // 📍 THIS IS THE MAGIC LINE: It prints the EXACT error to your screen
                                    Toast.makeText(context, "Error: ${e.message?.take(60)}...", Toast.LENGTH_LONG).show()
                                } finally {
                                    isProcessingVoice = false
                                }
                            }
                        }
                    }
                }

                // --- CORE REFRESH LOGIC ---
                val refreshAppData: suspend () -> Unit = {
                    if (isLoggedIn) {
                        val currentUid = supabase.auth.currentUserOrNull()?.id ?: ""
                        isRefreshing = true
                        try {
                            val today = java.time.LocalDate.now().toString()
                            // Fetch only data belonging to THIS user
                            val profile = supabase.from("profiles").select {
                                filter { eq("id", currentUid) }
                            }.decodeSingleOrNull<UserProfile>()
                            val pastMeals = supabase.from("logs").select {
                                filter { eq("user_id", currentUid) }
                            }.decodeList<MealLog>()

                            val fetchedWater = supabase.from("water_logs").select {
                                filter { eq("user_id", currentUid) }
                            }.decodeList<WaterLog>()

                            // Update the main app states
                            waterLogs = fetchedWater
                            loggedMeals.clear()
                            loggedMeals.addAll(pastMeals)
                            userProfile = profile

                            currentWaterIntake = fetchedWater
                                .filter { it.created_at?.startsWith(today) == true }
                                .sumOf { it.amount }

                            if (profile != null) {
                                val todayMeals = pastMeals.filter { it.created_at?.startsWith(today) == true }
                                val remaining = (profile.daily_goal - todayMeals.sumOf { it.calories }).coerceAtLeast(0)
                                VoltWidget.updateData(
                                    context = applicationContext,
                                    remainingKcal = remaining,
                                    goal = profile.daily_goal,
                                    p = todayMeals.sumOf { it.protein },
                                    c = todayMeals.sumOf { it.carbs },
                                    f = todayMeals.sumOf { it.fat },
                                    w = currentWaterIntake
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Volt", "Refresh Error: ${e.message}")
                        } finally {
                            isRefreshing = false
                            isCheckingProfile = false
                        }
                    } else {
                        isCheckingProfile = false
                        userProfile = null
                        loggedMeals.clear()
                        currentWaterIntake = 0
                    }
                }

                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsStateWithLifecycle()

                LaunchedEffect(lifecycleState, isLoggedIn) {
                    if (lifecycleState == Lifecycle.State.RESUMED) {
                        refreshAppData()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (sessionStatus is SessionStatus.Initializing || isCheckingProfile) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(VoltBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = VoltCyan)
                        }
                    } else {
                        val navController = rememberNavController()

                        // 📍 1. DYNAMIC START DESTINATION
                        val startDest = remember(isLoggedIn, userProfile) {
                            when {
                                !isLoggedIn -> "welcome"
                                userProfile?.name.isNullOrBlank() -> Screen.Onboarding.route
                                else -> Screen.Dashboard.route
                            }
                        }

                        // 📍 2. REDIRECT GUARD (Triggered on state changes)
                        LaunchedEffect(isLoggedIn, userProfile, isCheckingProfile) {
                            if (isLoggedIn && !isCheckingProfile) {
                                if (userProfile?.name.isNullOrBlank()) {
                                    navController.navigate(Screen.Onboarding.route) {
                                        popUpTo(0)
                                    }
                                }
                            }
                        }

                        Scaffold(
                            containerColor = VoltBlack,
                            bottomBar = {
                                if (isLoggedIn && !userProfile?.name.isNullOrBlank()) {
                                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                                    val currentRoute = navBackStackEntry?.destination?.route
                                    
                                    val hideNavBarScreens = listOf(
                                        Screen.Lens.route,
                                        Screen.Barcode.route,
                                        "welcome",
                                        "auth_login",
                                        "auth_signup",
                                        Screen.Onboarding.route
                                    )

                                    if (currentRoute !in hideNavBarScreens) {
                                        VoltFloatingNavBar(
                                            currentRoute = currentRoute,
                                            onNavigate = { route ->
                                                navController.navigate(route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            onManualClick = {
                                                currentMealData = null
                                                showManualEntry = true
                                            },
                                            onBarcodeClick = { navController.navigate(Screen.Barcode.route) },
                                            onCameraClick = { navController.navigate(Screen.Lens.route) },
                                            onVoiceClick = {
                                                val intent =
                                                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                        putExtra(
                                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                                        )
                                                        putExtra(
                                                            RecognizerIntent.EXTRA_PROMPT,
                                                            "Tell Aira what you ate..."
                                                        )
                                                    }
                                                try {
                                                    voiceLauncher.launch(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(
                                                        context,
                                                        "Your device doesn't support Voice Input.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {
                                // 📍 3. SET NavHost TO startDest
                                NavHost(navController, startDestination = startDest) {

                                    // 📍 1. The New Splash & Pager
                                    composable("welcome") {
                                        WelcomeScreen(
                                            onNavigateToLogin = { navController.navigate("auth_login") },
                                            onNavigateToSignUp = { navController.navigate("auth_signup") }
                                        )
                                    }

                                    // 📍 1. THE LOGIN MODE ROUTE
                                    composable("auth_login") {
                                        AuthScreen(
                                            initialIsLogin = true,
                                            isLoading = isRefreshing,
                                            onBack = { navController.popBackStack() },
                                            onLoginClick = { e, p ->
                                                coroutineScope.launch {
                                                    try {
                                                        isRefreshing = true
                                                        supabase.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                                                            email = e
                                                            password = p
                                                        }
                                                        // After successful login, the startDestination logic
                                                        // in MainActivity will automatically route to Dashboard
                                                    } catch (err: Exception) {
                                                        Toast.makeText(context, "Login Failed: ${err.message}", Toast.LENGTH_LONG).show()
                                                    } finally {
                                                        isRefreshing = false
                                                    }
                                                }
                                            },
                                            onSignUpClick = { _, _ -> } // Not used in login mode
                                        )
                                    }

                                    // 📍 2. THE SIGN UP MODE ROUTE
                                    composable("auth_signup") {
                                        AuthScreen(
                                            initialIsLogin = false,
                                            isLoading = isRefreshing,
                                            onBack = { navController.popBackStack() },
                                            onLoginClick = { _, _ -> },
                                            onSignUpClick = { e, p ->
                                                coroutineScope.launch {
                                                    try {
                                                        isRefreshing = true
                                                        supabase.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                                                            email = e
                                                            password = p
                                                        }
                                                        // 📍 CRITICAL: Tell the user to check their email!
                                                        Toast.makeText(context, "Check your email to confirm your account!", Toast.LENGTH_LONG).show()
                                                        navController.navigate("auth_login") // Move back to login
                                                    } catch (err: Exception) {
                                                        Toast.makeText(context, "Sign Up Failed: ${err.message}", Toast.LENGTH_LONG).show()
                                                    } finally {
                                                        isRefreshing = false
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    composable(Screen.Onboarding.route) {
                                        OnboardingScreen(onComplete = { collectedData ->
                                            coroutineScope.launch {
                                                try {
                                                    val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                                                    val calculatedGoal = GoalCalculator.calculateDailyGoal(collectedData)
                                                    val profileRow = ProfileDatabaseRow(
                                                        id = userId,
                                                        name = collectedData.name,
                                                        gender = collectedData.gender,
                                                        birth_date = collectedData.birthDate,
                                                        units = collectedData.units,
                                                        height = collectedData.height,
                                                        weight = collectedData.weight,
                                                        activity_level = collectedData.activityLevel,
                                                        exercise_freq = collectedData.exerciseFreq,
                                                        diet = collectedData.diet,
                                                        primary_goal = collectedData.primaryGoal,
                                                        improvement_goals = collectedData.improvementGoals.toList(),
                                                        history = collectedData.history,
                                                        obstacles = collectedData.obstacles.toList(),
                                                        commitment = collectedData.commitment,
                                                        tracking_prefs = collectedData.trackingPrefs,
                                                        daily_goal = calculatedGoal
                                                    )
                                                    supabase.from("profiles").upsert(profileRow)
                                                    refreshAppData()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Setup failed: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        })
                                    }

                                    composable(Screen.Dashboard.route) {
                                        HomeScreen(
                                            loggedMeals = loggedMeals,
                                            userProfile = userProfile,
                                            dailyGoal = userProfile?.daily_goal ?: 2200,
                                            currentWater = currentWaterIntake,
                                            isRefreshing = isRefreshing,
                                            onRefresh = { coroutineScope.launch { refreshAppData() } },
                                            allLoggedMeals = loggedMeals,
                                            allWaterLogs = waterLogs,
                                            onWaterAdd = { delta ->
                                                val userId = userProfile?.id ?: return@HomeScreen
                                                val now = java.time.OffsetDateTime.now().toString()
                                                val tempLog = WaterLog(
                                                    user_id = userId,
                                                    amount = delta,
                                                    created_at = now
                                                )
                                                waterLogs = waterLogs + tempLog
                                                coroutineScope.launch {
                                                    try {
                                                        supabase.from("water_logs").insert(tempLog)
                                                    } catch (e: Exception) {
                                                        waterLogs = waterLogs - tempLog
                                                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            onCameraClick = { navController.navigate(Screen.Lens.route) },
                                            onBarcodeClick = { navController.navigate(Screen.Barcode.route) },
                                            onManualClick = {
                                                currentMealData = null
                                                showManualEntry = true
                                            },
                                            onVoiceParsed = {
                                                currentMealData = it
                                                showManualEntry = true
                                            },
                                            onCoachClick = { navController.navigate("coach") },
                                            onSettingsClick = { navController.navigate(Screen.Profile.route) },
                                            onDeleteMeal = { meal ->
                                                coroutineScope.launch {
                                                    try {
                                                        supabase.from("logs").delete { filter { eq("id", meal.id) } }
                                                        refreshAppData()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Failed to delete meal", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    composable(Screen.Analytics.route) {
                                        AnalyticsScreen(loggedMeals, allWaterLogs = waterLogs, userProfile?.daily_goal ?: 2200, currentWaterIntake)
                                    }

                                    composable(Screen.Lens.route) {
                                        AiLensScreen(
                                            onClose = { navController.popBackStack() },
                                            onPhotoCaptured = { file ->
                                                coroutineScope.launch {
                                                    try {
                                                        isScanning = true
                                                        val meal = AiMealParser.parseImage(file)
                                                        currentMealData = meal
                                                        showManualEntry = true
                                                        navController.popBackStack()
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("VoltLens", "AI Vision Error", e)
                                                        Toast.makeText(context, "AI Vision Error: ${e.message}", Toast.LENGTH_LONG).show()
                                                    } finally {
                                                        isScanning = false
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    composable(Screen.Barcode.route) {
                                        BarcodeScannerScreen(
                                            onClose = { navController.popBackStack() },
                                            onBarcodeScanned = { meal ->
                                                currentMealData = meal
                                                showManualEntry = true
                                                navController.popBackStack()
                                            }
                                        )
                                    }

                                    composable(Screen.Profile.route) {
                                        ProfileScreen(userProfile, loggedMeals, onSettingsClick = { navController.navigate(Screen.Settings.route) }, onSignOut = {
                                            navController.navigate("auth") { popUpTo(0) }
                                        })
                                    }

                                    composable(Screen.Settings.route) {
                                        SettingsScreen(
                                            userProfile = userProfile,
                                            onNavigateBack = { navController.popBackStack() },
                                            onUpdateProfile = { newName, newGoal ->
                                                coroutineScope.launch {
                                                    try {
                                                        val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                                                        supabase.from("profiles").update(mapOf("name" to newName, "daily_goal" to newGoal)) { filter { eq("id", userId) } }
                                                        refreshAppData()
                                                        Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onResetData = {
                                                coroutineScope.launch {
                                                    try {
                                                        val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                                                        supabase.from("logs").delete { filter { eq("user_id", userId) } }
                                                        supabase.from("water_logs").delete { filter { eq("user_id", userId) } }
                                                        refreshAppData()
                                                        Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Reset failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onLogout = { coroutineScope.launch { supabase.auth.signOut() } },
                                            onDeleteAccount = { Toast.makeText(context, "Feature coming soon", Toast.LENGTH_SHORT).show() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- OVERLAYS ---
                    if (isScanning) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = VoltCyan)
                        }
                    }

                    if (showManualEntry) {
                        ManualEntrySheet(
                            modifier = Modifier,
                            initialMeal = currentMealData,
                            onDismiss = { showManualEntry = false },
                            onSave = { meal ->
                                coroutineScope.launch {
                                    try {
                                        val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                                        supabase.from("logs").insert(meal.copy(user_id = userId))
                                        showManualEntry = false
                                        refreshAppData()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }

                    if (isProcessingVoice) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable(enabled = false) {},
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = VoltSurfaceVariant,
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, VoltCyan.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = VoltCyan)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Aira is parsing your meal...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}