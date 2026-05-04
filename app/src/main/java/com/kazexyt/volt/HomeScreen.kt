package com.kazexyt.volt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

import com.kazexyt.volt.model.MealLog
import com.kazexyt.volt.model.UserProfile
import com.kazexyt.volt.model.MascotState
import com.kazexyt.volt.model.WaterLog
import com.kazexyt.volt.ui.CalorieDashboard
import com.kazexyt.volt.ui.MacroProgressCard
import com.kazexyt.volt.supabase
import com.kazexyt.volt.ui.VoltTimelinePicker
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.kazexyt.volt.utils.AiMealParser

import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import com.kazexyt.volt.utils.ShareUtils
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.kazexyt.volt.ui.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    loggedMeals: List<MealLog>,
    userProfile: UserProfile?,
    allLoggedMeals: List<MealLog>,
    allWaterLogs: List<WaterLog>,
    dailyGoal: Int,
    onWaterAdd: (Int) -> Unit,
    currentWater: Int,
    onCameraClick: () -> Unit,
    onBarcodeClick: () -> Unit,
    onManualClick: () -> Unit,
    onVoiceParsed: (MealLog) -> Unit,
    onCoachClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onDeleteMeal: (MealLog) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 📍 1. Calculate Age using the DD/MM/YYYY format
    val dailyHydrationGoal = remember(userProfile) {
        val birthDateStr = userProfile?.birthDate
        val gender = userProfile?.gender ?: "Male"

        val age = try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val birthDate = java.time.LocalDate.parse(birthDateStr, formatter)

            java.time.Period.between(birthDate, java.time.LocalDate.now()).years
        } catch (e: Exception) {
            android.util.Log.e("VoltHydration", "Date parsing failed: ${e.message}")
            25
        }
        when {
            age < 4  -> 1300
            age < 9  -> 1700
            age < 14 -> 2100
            age < 19 -> if (gender == "Male") 3000 else 2400
            // Adult standards (19+)
            else     -> if (gender == "Male") 3700 else 2700
        }
    }

    val dynamicGoal = remember(allWaterLogs, dailyHydrationGoal) {
        val personalBest = allWaterLogs
            .groupBy { it.created_at?.take(10) ?: "" }
            .filter { it.key.isNotEmpty() }
            .map { group -> group.value.sumOf { it.amount } }
            .maxOrNull() ?: 0

        maxOf(dailyHydrationGoal, personalBest)
    }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    fun String?.toLocalDate(): LocalDate? = try {
        this?.replace(" ", "T")?.take(10)?.let { LocalDate.parse(it) }
    } catch (e: Exception) { null }

    val startDate: LocalDate = remember(allLoggedMeals, allWaterLogs) {
        val earliestMeal = allLoggedMeals.mapNotNull { it.created_at.toLocalDate() }.minOrNull()
        val earliestWater = allWaterLogs.mapNotNull { it.created_at.toLocalDate() }.minOrNull()

        listOfNotNull(earliestMeal, earliestWater).minOrNull() ?: LocalDate.now()
    }

    val displayMeals = remember(allLoggedMeals, selectedDate) {
        allLoggedMeals.filter { it.created_at.toLocalDate() == selectedDate }
    }

    val displayWater = remember(allWaterLogs, selectedDate) {
        allWaterLogs.filter { it.created_at.toLocalDate() == selectedDate }.sumOf { it.amount }
    }

    val totalCals = displayMeals.sumOf { it.calories }
    val remainingCals = (userProfile?.daily_goal ?: 2200) - totalCals

    var showShareDialog by remember { mutableStateOf(false) }

    // UPDATE: Now sum off of `displayMeals` instead of `todayMeals`
    val totalConsumed = displayMeals.sumOf { it.calories }
    val totalProtein = displayMeals.sumOf { it.protein }
    val totalCarbs = displayMeals.sumOf { it.carbs }
    val totalFat = displayMeals.sumOf { it.fat }
    val pullState = rememberPullToRefreshState()

    var selectedMeal by remember { mutableStateOf<MealLog?>(null) }
    var isFabExpanded by remember { mutableStateOf(false) }

    // UPDATE: Emotion based on the selected day's logs
    val emotion = remember(displayMeals, dailyGoal) {
        when {
            displayMeals.isEmpty() -> MascotState.SLEEPY
            totalConsumed >= dailyGoal -> MascotState.HAPPY
            else -> MascotState.NORMAL
        }
    }

    val currentHour = java.time.LocalTime.now().hour
    val (greetingString, timeAccentColor) = when (currentHour) {
        in 5..11 -> "Good Morning," to Color(0xFFFFD180)
        in 12..17 -> "Good Afternoon," to VoltCyan
        else -> "Good Evening," to VoltLavender
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullState,
                    isRefreshing = isRefreshing,
                    color = Color(0xFF00E5FF),
                    containerColor = Color(0xFF161616),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp) // Removed horizontal padding here to let the timeline bleed to edges
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), // Re-applied horizontal padding
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = greetingString, style = MaterialTheme.typography.titleMedium, color = timeAccentColor)
                        Text(text = "Stay on track ⚡", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row {
                        IconButton(
                            onClick = onCoachClick,
                            modifier = Modifier.background(VoltCyan.copy(alpha = 0.1f), CircleShape)
                        ) { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Coach", tint = VoltCyan) }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showShareDialog = true },
                            modifier = Modifier.background(VoltSurfaceVariant, CircleShape)
                        ) { Icon(Icons.Default.IosShare, contentDescription = "Share", tint = Color.White) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- NEW: VOLT TIMELINE PICKER ---
                VoltTimelinePicker(
                    selectedDate = selectedDate,
                    startDate = startDate,
                    onDateSelected = { newDate ->
                        selectedDate = newDate
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Wrap the rest of the content in a column with padding
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {

                    // --- CALORIE DASHBOARD ---
                    CalorieDashboard(consumed = totalConsumed, goal = dailyGoal, emotion = emotion)

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- UPDATED WATER TILE ---
                    WaterTile(
                        ml = displayWater,
                        goalMl = dynamicGoal,
                        onAddWater = {
                            onWaterAdd(250)
                        },
                        onLongClick = {
                            if (currentWater >= 250) {
                                onWaterAdd(-250)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MacroProgressCard("Protein", totalProtein, 150, Color(0xFFF2B8B5))
                        MacroProgressCard("Carbs", totalCarbs, 250, Color(0xFFA8C7FA))
                        MacroProgressCard("Fat", totalFat, 65, Color(0xFFFFD180))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // ... Rest of your logs UI ...
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        // Change text based on if looking at today or a past day
                        val logText = if (selectedDate == LocalDate.now()) "Today's Logs" else "${selectedDate.dayOfMonth} ${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} Logs"
                        Text(logText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // UPDATE: Use displayMeals instead of todayMeals
                    if (displayMeals.isEmpty()) {
                        Text("No meals logged on this day.", color = Color.Gray, modifier = Modifier.padding(24.dp))
                    } else {
                        displayMeals.reversed().forEach { meal ->
                            RecentMealRow(meal = meal, onClick = { selectedMeal = meal })
                        }
                    }

                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }

        // --- 3. OVERLAYS (Outside refresh wrapper to stay fixed) ---
        AnimatedVisibility(visible = isFabExpanded, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isFabExpanded = false })
        }

        if (showShareDialog) {
            ShareDayDialog(
                calories = totalConsumed, goal = dailyGoal, protein = totalProtein,
                carbs = totalCarbs, fat = totalFat, waterCount = currentWater, streak = 5,
                onDismiss = { showShareDialog = false }
            )
        }

        selectedMeal?.let { meal ->
            DetailedMealSheet(
                modifier = Modifier,
                meal = meal,
                onDismiss = { selectedMeal = null },
                onDelete = {
                    onDeleteMeal(meal)
                    selectedMeal = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WaterTile(
    ml: Int,
    goalMl: Int,
    onAddWater: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isGoalAchieved = ml >= goalMl
    val progress = (ml.toFloat() / goalMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val haptic = LocalHapticFeedback.current

    // --- ANIMATIONS ---
    // Smoothly transition border color from dim to neon cyan
    val borderColor by animateColorAsState(
        targetValue = if (isGoalAchieved) VoltCyan else VoltCyan.copy(alpha = 0.2f),
        animationSpec = tween(durationMillis = 500), label = "border_glow"
    )

    // Increase border thickness slightly when goal is hit
    val borderWidth by animateDpAsState(
        targetValue = if (isGoalAchieved) 2.dp else 1.dp,
        label = "border_width"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(VoltSurfaceVariant)
            .border(borderWidth, borderColor, RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddWater()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        // Progress Fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                // 📍 Fill gets slightly more opaque when goal is hit
                .background(VoltCyan.copy(alpha = if (isGoalAchieved) 0.15f else 0.1f))
                .align(Alignment.CenterStart)
        )

        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isGoalAchieved) "Goal Reached! ⚡" else "Daily Hydration",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${ml}ml / ${goalMl}ml",
                    color = if (isGoalAchieved) Color.White else VoltCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 📍 ICON SWAP: Drop icon transforms into a Checkmark
            Crossfade(targetState = isGoalAchieved, label = "icon_swap") { achieved ->
                Icon(
                    imageVector = if (achieved) Icons.Default.CheckCircle else Icons.Default.Opacity,
                    contentDescription = null,
                    tint = VoltCyan,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

// --- SUB-COMPONENTS ---
@Composable
fun RecentMealRow(
    meal: MealLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VoltSurfaceVariant,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(meal.food_name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(6.dp))
                Text("${meal.protein}g P  •  ${meal.carbs}g C  •  ${meal.fat}g F", color = Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("+${meal.calories} kcal", color = VoltLavender, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedMealSheet(
    modifier: Modifier = Modifier,
    meal: MealLog,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VoltSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) },
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
            Text(meal.food_name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${meal.calories}", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold), color = VoltLavender)
                Text(" kcal", style = MaterialTheme.typography.titleLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Macro Split", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            val totalMacros = (meal.protein + meal.carbs + meal.fat).coerceAtLeast(1)
            val pRatio = meal.protein.toFloat() / totalMacros
            val cRatio = meal.carbs.toFloat() / totalMacros
            val fRatio = meal.fat.toFloat() / totalMacros

            Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50))) {
                Box(modifier = Modifier.weight(if (pRatio > 0) pRatio else 0.01f).fillMaxHeight().background(Color(0xFFF2B8B5)))
                Box(modifier = Modifier.weight(if (cRatio > 0) cRatio else 0.01f).fillMaxHeight().background(Color(0xFFA8C7FA)))
                Box(modifier = Modifier.weight(if (fRatio > 0) fRatio else 0.01f).fillMaxHeight().background(Color(0xFFFFD180)))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MacroLabel("Protein", "${meal.protein}g", Color(0xFFF2B8B5))
                MacroLabel("Carbs", "${meal.carbs}g", Color(0xFFA8C7FA))
                MacroLabel("Fat", "${meal.fat}g", Color(0xFFFFD180))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(color = VoltPurple.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, VoltPurple.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VoltLavender)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Volt's Analysis", color = VoltLavender, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        val insight = if (meal.protein >= 25) "Excellent protein source! This will keep you satiated and help with muscle recovery."
                        else if (meal.carbs > 60) "High carbohydrate content. Great for fueling up before a workout or recovering energy levels."
                        else "A balanced meal. Keep tracking to stay aligned with your daily goals."
                        Text(insight, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltError),
                border = BorderStroke(1.dp, VoltError.copy(alpha = 0.5f))
            ) {
                Text("Delete Meal Log", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MacroLabel(name: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text("$name: ", color = Color.Gray, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ShareDayDialog(
    calories: Int,
    goal: Int,
    protein: Int,
    carbs: Int,
    fat: Int,
    waterCount: Int,
    streak: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    Dialog(onDismissRequest = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // --- THE BEAUTIFIED STORY CARD ---
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .aspectRatio(9f / 16f) // Instagram Story Size
                    .clip(RoundedCornerShape(36.dp)) // Android 16 Squircle
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawContent()
                    }
                    .background(Color(0xFF0A0A0A))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(36.dp))
            ) {
                // 1. Dynamic Mesh Gradient Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(Color(0xFF5D4291).copy(alpha = 0.5f), Color.Transparent),
                                radius = 800f
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxSize().padding(vertical = 40.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    // --- HEADER (Updated Mascot Block) ---
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_volt_mascot),
                                contentDescription = "Volt Mascot",
                                // Make sure your mascot drawable itself is white, just like the dashboard.
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))

                            Column(horizontalAlignment = Alignment.Start) {
                                Text("VOLT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 3.sp)
                                Text("DAILY SUMMARY", color = Color.Gray, fontSize = 12.sp, letterSpacing = 1.sp)
                            }
                        }
                        // Separate Date text below branding
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(java.time.LocalDate.now().toString(), color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    // --- THE DATA RING (Keep previous beautiful design) ---
                    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Background Track
                            drawArc(
                                color = Color.White.copy(alpha = 0.05f),
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Neon Progress Arc
                            val progress = (calories.toFloat() / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                            drawArc(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(VoltCyan, VoltLavender)
                                ),
                                startAngle = 135f,
                                sweepAngle = 270f * progress,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$calories", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("/ $goal KCAL", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }

                    // --- MACRO PILLS (Keep previous beautiful design) ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        VoltMacroPill("Protein", "${protein}g", Color(0xFFF2B8B5))
                        VoltMacroPill("Carbs", "${carbs}g", Color(0xFFA8C7FA))
                        VoltMacroPill("Fat", "${fat}g", Color(0xFFFFD180))
                    }

                    // --- FOOTER STATS (Keep previous beautiful design) ---
                    Surface(
                        color = Color.White.copy(0.05f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color.White.copy(0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${waterCount}ml", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFFD180), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("$streak Days", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SHARE BUTTON (Keep previous design) ---
            Button(
                onClick = {
                    coroutineScope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        ShareUtils.shareBitmap(context, bitmap)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VoltLavender, contentColor = VoltBlack),
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
            ) {
                Icon(Icons.Default.IosShare, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share to Story", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VoltMacroPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

