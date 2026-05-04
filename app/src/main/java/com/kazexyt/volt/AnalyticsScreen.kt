package com.kazexyt.volt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazexyt.volt.model.MealLog
import com.kazexyt.volt.model.WaterLog
import com.kazexyt.volt.ui.theme.VoltBlack
import com.kazexyt.volt.ui.theme.VoltCarb
import com.kazexyt.volt.ui.theme.VoltCyan
import com.kazexyt.volt.ui.theme.VoltFat
import com.kazexyt.volt.ui.theme.VoltLavender
import com.kazexyt.volt.ui.theme.VoltProtein
import com.kazexyt.volt.ui.theme.VoltPurple
import com.kazexyt.volt.ui.theme.VoltSurface
import com.kazexyt.volt.ui.theme.VoltSurfaceVariant
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class TimeFilter { Week, Month, Year }

@Composable
fun AnalyticsScreen(
    loggedMeals: List<MealLog>,
    allWaterLogs: List<WaterLog>,
    dailyGoal: Int,
    waterCount: Int,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<TimeFilter>(TimeFilter.Week) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Calories", "Macros", "Water")

    fun String?.toSafeLocalDate(): LocalDate = try {
        this?.replace(" ", "T")?.take(10)?.let { LocalDate.parse(it) } ?: LocalDate.now()
    } catch (e: Exception) { LocalDate.now() }

    val startOfJourney = remember(loggedMeals) {
        loggedMeals.minByOrNull { it.created_at }?.created_at.toSafeLocalDate()
    }

    val filteredMeals = remember(loggedMeals, selectedFilter, startOfJourney) {
        val today = LocalDate.now()
        val filterDate = when (selectedFilter) {
            TimeFilter.Week -> today.minusDays(7)
            TimeFilter.Month -> today.minusMonths(1)
            TimeFilter.Year -> today.minusYears(1)
        }

        loggedMeals.filter {
            val logDate = it.created_at.toSafeLocalDate()
            // 📍 Match the filter (Week/Month) AND stay within the user's history
            logDate.isAfter(filterDate.minusDays(1)) && !logDate.isBefore(startOfJourney)
        }.sortedBy { it.created_at }
    }

    var waterHistory by remember { mutableStateOf<List<WaterLog>>(emptyList()) }
    LaunchedEffect(selectedFilter) {
        try {
            val results = supabase.from("water_logs").select().decodeList<WaterLog>()
            waterHistory = results
        } catch (e: Exception) {
            android.util.Log.e("VoltAnalytics", "Water Fetch Error: ${e.message}")
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = VoltBlack,
        topBar = {
            Column(modifier = Modifier.background(VoltBlack).padding(top = 16.dp)) {
                AnalyticsHeader()
                TimePillFilter(selectedFilter) { selectedFilter = it }
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = VoltBlack,
                    contentColor = VoltLavender,
                    edgePadding = 24.dp,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = VoltCyan
                            )
                        }
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                if (filteredMeals.isEmpty() && selectedTab != 3) {
                    EmptyStateMascot("No meal data for this period")
                } else {
                    when (selectedTab) {
                        0 -> OverviewTabContent(filteredMeals, dailyGoal)
                        1 -> CaloriesTabContent(filteredMeals, dailyGoal, selectedFilter)
                        2 -> MacrosTabContent(filteredMeals, dailyGoal)
                        3 -> WaterTabContent(waterHistory, selectedFilter)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun OverviewTabContent(meals: List<MealLog>, goal: Int) {
    val mealsByDate = meals.groupBy { it.created_at.replace(" ", "T").take(10) }

    val totalDaysInRange = mealsByDate.size.coerceAtLeast(1)
    val totalCals = meals.sumOf { it.calories }

    val avgCals = if (meals.isEmpty()) 0 else totalCals / totalDaysInRange
    val adherenceRate = if (meals.isEmpty()) 0 else (mealsByDate.values.count {
        it.sumOf { m -> m.calories } in (goal * 0.85).toInt()..(goal * 1.15).toInt()
    } * 100) / totalDaysInRange

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {

        // 1. EXPANSIVE ACTIVITY FLOW (20 WEEKS)
        Text(
            text = "PROGRESS MATRIX (140 DAYS)",
            style = MaterialTheme.typography.labelSmall,
            color = VoltCyan,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            letterSpacing = 2.sp
        )
        // Set to 20 weeks to fill the vision
        VoltConsistencyHeatmap(meals, goal, weeks = 20)

        Spacer(Modifier.height(32.dp))

        // 2. THE BENTO GRID (Perfectly Aligned)
        Text(
            text = "PERFORMANCE METRICS",
            style = MaterialTheme.typography.labelSmall,
            color = VoltCyan,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            letterSpacing = 2.sp
        )

        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            // Adherence Card now fills the vertical height of the stacked small cards
            VoltPremiumMetricCard(
                label = "Adherence",
                value = "$adherenceRate%",
                accent = VoltCyan,
                icon = Icons.Default.CheckCircle,
                description = "Goal precision",
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(0.9f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                VoltSmallMetricCard("Streak", "${calculateStreak(meals)}d", VoltFat, Icons.Default.ElectricBolt)
                Spacer(Modifier.height(16.dp))
                VoltSmallMetricCard("Total Logs", "${meals.size}", VoltCarb, Icons.Default.BarChart)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth()) {
            VoltPremiumMetricCard(
                label = "Daily Avg",
                value = "$avgCals kcal",
                accent = VoltLavender,
                icon = Icons.Default.LocalFireDepartment,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(16.dp))
            VoltPremiumMetricCard(
                label = "Avg Protein",
                value = "${if(totalDaysInRange > 0) meals.sumOf { it.protein } / totalDaysInRange else 0}g",
                accent = VoltProtein,
                icon = Icons.Default.FitnessCenter,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))
        VoltInsightCard(meals)
    }
}

@Composable
private fun VoltConsistencyHeatmap(meals: List<MealLog>, goal: Int, weeks: Int) {
    val mealsByDate = meals.groupBy {
        try { OffsetDateTime.parse(it.created_at).toLocalDate() }
        catch (e: Exception) { LocalDate.now() }
    }
    val today = LocalDate.now()
    val totalCells = weeks * 7
    val dates = (0 until totalCells).map { today.minusDays(it.toLong()) }.reversed()

    Surface(
        color = VoltSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            // We use a simple Row here, and each Column (week) gets a weight to "fill" the space
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                (0 until weeks).forEach { week ->
                    Column(
                        modifier = Modifier.weight(1f), // Each week takes equal width
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        (0 until 7).forEach { day ->
                            val date = dates.getOrNull(week * 7 + day)
                            val dayMeals = mealsByDate[date] ?: emptyList()
                            val totalCals = dayMeals.sumOf { it.calories }

                            val color = when {
                                totalCals == 0 -> Color.White.copy(alpha = 0.05f)
                                totalCals >= goal -> VoltPurple
                                totalCals > 0 -> VoltLavender.copy(alpha = 0.4f)
                                else -> Color.White.copy(alpha = 0.05f)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth() // Fill the weight-assigned width
                                    .aspectRatio(1f) // Keep them as perfect squares
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                                    .then(
                                        if (date == today) Modifier.border(1.dp, VoltCyan, RoundedCornerShape(2.dp))
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Inactive", color = Color.Gray, fontSize = 9.sp)
                Spacer(Modifier.width(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.05f, 0.4f, 1f).forEach { alpha ->
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(VoltPurple.copy(alpha = alpha)))
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text("Peak", color = Color.Gray, fontSize = 9.sp)

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "${(mealsByDate.size.toFloat() / totalCells * 100).toInt()}% Active",
                    color = VoltCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun VoltPremiumMetricCard(
    label: String,
    value: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Surface(
        color = VoltSurface,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, null, tint = accent.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                if (description != null) {
                    Text(description, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(value, color = accent, fontSize = 26.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VoltSmallMetricCard(label: String, value: String, accent: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = VoltSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(label, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun VoltInsightCard(meals: List<MealLog>) {
    val bestProtein = meals.maxByOrNull { it.protein }

    Surface(
        color = VoltPurple.copy(alpha = 0.1f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, VoltPurple.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(VoltPurple.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, tint = VoltLavender, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("AI Insight", color = VoltLavender, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (bestProtein != null) {
                        "Your best protein source lately was ${bestProtein.food_name}. Aira recommends more of this for better recovery."
                    } else {
                        "Start logging your meals to see deep nutritional insights here."
                    },
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun CaloriesTabContent(meals: List<MealLog>, goal: Int, filter: TimeFilter) {
    val dailyTotals = meals.groupBy {
        try { OffsetDateTime.parse(it.created_at).toLocalDate() }
        catch (e: Exception) { LocalDate.now() }
    }.mapValues { it.value.sumOf { m -> m.calories } }

    val chartData = dailyTotals.values.toList().takeLast(if (filter == TimeFilter.Week) 7 else 14).map { it.toFloat() }
    val maxDay = dailyTotals.values.maxOrNull() ?: 0
    val minDay = dailyTotals.values.minOrNull() ?: 0
    val daysOver = dailyTotals.values.count { it > goal }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        // HERO: ENERGY FLOW
        Text("ENERGY FLOW", style = MaterialTheme.typography.labelSmall, color = VoltCyan, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        Surface(
            color = VoltSurface,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(Modifier.padding(20.dp)) {
                VoltEnergyFlowChart(meals, goal)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cumulative Intake", color = Color.Gray, fontSize = 10.sp)
                    Text("Target: $goal kcal", color = VoltLavender, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max) // Ensures both columns are the same height
        ) {
            // LEFT: High-Impact Card
            VoltPremiumMetricCard(
                label = "Highest Day",
                value = "$maxDay",
                accent = VoltCyan,
                icon = Icons.Default.VerticalAlignTop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            Spacer(Modifier.width(16.dp)) // Horizontal Gutter

            // RIGHT: Stacked Cards with fixed spacing
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                VoltSmallMetricCard(
                    label = "Lowest",
                    value = "$minDay",
                    accent = VoltLavender,
                    icon = Icons.Default.VerticalAlignBottom
                )

                // This fixed spacer matches your horizontal width for a perfect grid gutter
                Spacer(Modifier.height(16.dp))

                VoltSmallMetricCard(
                    label = "Over Goal",
                    value = "$daysOver Days",
                    accent = Color(0xFFF44336),
                    icon = Icons.Default.Warning
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // DAILY DISTRIBUTION BARS
        Text("GOAL PRECISION", style = MaterialTheme.typography.labelSmall, color = VoltCyan, letterSpacing = 2.sp)
        VoltAnalyticsChart(chartData, goal.toFloat(), VoltCyan, true) { valKcal ->
            if (valKcal > goal * 1.15) Color(0xFFF44336) else if (valKcal >= goal * 0.85) Color(0xFF4CAF50) else VoltCyan.copy(0.6f)
        }
    }
}

@Composable
private fun MacrosTabContent(meals: List<MealLog>, dailyGoal: Int) {
    val totalDays = meals.groupBy { OffsetDateTime.parse(it.created_at).toLocalDate() }.size.coerceAtLeast(1)
    val avgP = meals.sumOf { it.protein } / totalDays
    val avgC = meals.sumOf { it.carbs } / totalDays
    val avgF = meals.sumOf { it.fat } / totalDays

    val targetP = (dailyGoal.toFloat() * 0.30f / 4f).toInt().coerceAtLeast(1)
    val targetC = (dailyGoal.toFloat() * 0.40f / 4f).toInt().coerceAtLeast(1)
    val targetF = (dailyGoal.toFloat() * 0.30f / 9f).toInt().coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        // HERO: RADAR BALANCE
        Surface(
            color = VoltSurface,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SYMMETRY ANALYSIS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 2.sp)
                VoltMacroRadar(avgP, avgC, avgF, targetP, targetC, targetF)
            }
        }

        Spacer(Modifier.height(32.dp))

        // MACRO RINGS BENTO
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            VoltMacroRing("Protein", avgP, targetP, VoltProtein)
            VoltMacroRing("Carbs", avgC, targetC, VoltCarb)
            VoltMacroRing("Fat", avgF, targetF, VoltFat)
        }

        Spacer(Modifier.height(32.dp))

        // CALORIC PARTITION
        Text("CALORIC PARTITION", style = MaterialTheme.typography.labelSmall, color = VoltCyan, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        Surface(
            color = VoltSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
        ) {
            Column(Modifier.padding(20.dp)) {
                VoltMacroEnergyPartition(avgP, avgC, avgF)
                Spacer(Modifier.height(12.dp))
                Text("Real-world energy density distribution (kcal)", color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun WaterTabContent(history: List<WaterLog>, filter: TimeFilter) {
    val dailyTotals = history.groupBy {
        try {
            // Standardize Supabase string (replace space with T)
            it.created_at?.replace(" ", "T")?.take(10)?.let { dateStr ->
                java.time.LocalDate.parse(dateStr)
            } ?: java.time.LocalDate.now()
        } catch (e: Exception) {
            java.time.LocalDate.now()
        }
    }.mapValues { it.value.sumOf { w -> w.amount } }

    val hydrationGoal = 3750

    val totalDays = dailyTotals.size.coerceAtLeast(1)
    val avgWater = if (dailyTotals.isEmpty()) 0 else dailyTotals.values.sum() / totalDays
    val goalReached = dailyTotals.values.count { it >= 2000 }
    val peakVolume = dailyTotals.values.maxOrNull() ?: 0
    val currentStreak = calculateWaterStreak(dailyTotals, hydrationGoal)

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {

        // 1. HERO: HYDRATION VELOCITY (Large Glassy Card)
        Text(
            text = "HYDRATION STATUS",
            style = MaterialTheme.typography.labelSmall,
            color = VoltCyan,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            letterSpacing = 2.sp
        )

        Surface(
            color = VoltCyan.copy(alpha = 0.08f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, VoltCyan.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated-style Water Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(VoltCyan.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Waves, null, tint = VoltCyan, modifier = Modifier.size(28.dp))
                }

                Spacer(Modifier.width(20.dp))

                Column {
                    Text(
                        "CURRENT STREAK",
                        color = VoltCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "$currentStreak Days",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // 2. BENTO GRID: VOLUMETRIC DATA
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
            // Left: Large Goal Success Card
            VoltPremiumMetricCard(
                label = "Goal Success",
                value = "$goalReached Days",
                accent = VoltCyan,
                icon = Icons.Default.Verified,
                description = "Hitting 2L+",
                modifier = Modifier.weight(1.1f).fillMaxHeight()
            )

            Spacer(Modifier.width(16.dp))

            // Right: Stacked Small Cards
            Column(
                modifier = Modifier.weight(0.9f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VoltSmallMetricCard("Daily Avg", "${avgWater}ml", VoltLavender, Icons.Default.Opacity)
                VoltSmallMetricCard("Peak Vol", "${peakVolume}ml", VoltFat, Icons.Default.Leaderboard)
            }
        }

        Spacer(Modifier.height(32.dp))

        // 3. VOLUME HISTORY (Bar Chart)
        Text(
            text = "VOLUME HISTORY (7 DAYS)",
            style = MaterialTheme.typography.labelSmall,
            color = VoltCyan,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
            letterSpacing = 2.sp
        )

        val chartData = dailyTotals.values.toList().takeLast(7).map { it.toFloat() }

        Surface(
            color = VoltSurface,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                VoltAnalyticsChart(
                    data = chartData,
                    goal = 2000f,
                    primaryColor = VoltCyan,
                    isDetailed = true
                )

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Baseline: 2000ml", color = Color.Gray, fontSize = 10.sp)
                    Text(
                        if(avgWater >= 2000) "HYDRATED" else "UNDER TARGET",
                        color = if(avgWater >= 2000) Color(0xFF4CAF50) else VoltFat,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- ADVANCED CUSTOM VISUALS ---

@Composable
private fun VoltMacroRadar(p: Int, c: Int, f: Int, targetP: Int, targetC: Int, targetF: Int) {
    val pRatio = (p.toFloat() / targetP.toFloat()).coerceAtMost(1.2f)
    val cRatio = (c.toFloat() / targetC.toFloat()).coerceAtMost(1.2f)
    val fRatio = (f.toFloat() / targetF.toFloat()).coerceAtMost(1.2f)

    Box(Modifier.size(240.dp).padding(24.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.width / 2f

            val path = Path()
            for (i in 0..2) {
                val angle = (i * 120.0 - 90.0) * (PI / 180.0)
                val x = center.x + radius * cos(angle).toFloat()
                val y = center.y + radius * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, Color.White.copy(0.05f), style = Stroke(2f))

            val dataPath = Path()
            val ratios = listOf(pRatio, cRatio, fRatio)
            ratios.forEachIndexed { i, ratio ->
                val angle = (i * 120.0 - 90.0) * (PI / 180.0)
                val x = center.x + (radius * ratio) * cos(angle).toFloat()
                val y = center.y + (radius * ratio) * sin(angle).toFloat()
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()
            drawPath(dataPath, VoltPurple.copy(0.3f))
            drawPath(dataPath, VoltCyan, style = Stroke(4f, cap = StrokeCap.Round))
        }
        Text("P", Modifier.align(Alignment.TopCenter), color = VoltProtein, fontWeight = FontWeight.Bold)
        Text("C", Modifier.align(Alignment.BottomEnd), color = VoltCarb, fontWeight = FontWeight.Bold)
        Text("F", Modifier.align(Alignment.BottomStart), color = VoltFat, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VoltEnergyFlowChart(meals: List<MealLog>, goal: Int) {
    val dailyTotals = meals.groupBy { OffsetDateTime.parse(it.created_at).toLocalDate() }
        .mapValues { it.value.sumOf { m -> m.calories } }.values.toList()

    if (dailyTotals.size < 2) return

    val maxDaily = dailyTotals.maxOrNull()?.toFloat() ?: goal.toFloat()
    val maxValue = maxDaily * 1.2f

    Canvas(Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp)) {
        val path = Path()
        val fillPath = Path()
        val stepX = size.width / (dailyTotals.size - 1).toFloat()

        dailyTotals.forEachIndexed { i, value ->
            val x = i * stepX
            val y = size.height - (value.toFloat() / maxValue * size.height)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(fillPath, Brush.verticalGradient(listOf(VoltCyan.copy(0.2f), Color.Transparent)))
        drawPath(path, VoltCyan, style = Stroke(4f, cap = StrokeCap.Round))
    }
}

@Composable
private fun VoltMacroEnergyPartition(p: Int, c: Int, f: Int) {
    val pkcal = p.toFloat() * 4f
    val ckcal = c.toFloat() * 4f
    val fkcal = f.toFloat() * 9f
    // Force total to be at least 1f to prevent division by zero (NaN)
    val total = (pkcal + ckcal + fkcal).coerceAtLeast(1f)

    Column(Modifier.fillMaxWidth()) {
        Text("Energy Contribution", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp))) {
            // Weights must be > 0.0f and NOT NaN
            Box(Modifier.weight((pkcal / total).coerceIn(0.01f, 1f)).fillMaxHeight().background(VoltProtein))
            Box(Modifier.weight((ckcal / total).coerceIn(0.01f, 1f)).fillMaxHeight().background(VoltCarb))
            Box(Modifier.weight((fkcal / total).coerceIn(0.01f, 1f)).fillMaxHeight().background(VoltFat))
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
private fun AnalyticsHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column {
            Text("Deep Insights", color = VoltCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Analytics", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = VoltSurface) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = VoltLavender, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun TimePillFilter(selected: TimeFilter, onSelected: (TimeFilter) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(24.dp).height(44.dp).background(VoltSurface, CircleShape).padding(4.dp)) {
        TimeFilter.entries.forEach { filter ->
            val isSelected = selected == filter
            val bg by animateColorAsState(if (isSelected) VoltPurple else Color.Transparent, label = "")
            Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(bg).clickable { onSelected(filter) }, Alignment.Center) {
                Text(filter.name, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun VoltMetricCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(color = VoltSurface, shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun VoltAnalyticsChart(data: List<Float>, goal: Float, primaryColor: Color, isDetailed: Boolean = false, highlightLogic: (Float) -> Color = { primaryColor }) {
    val maxValue = (data.maxOrNull() ?: goal).coerceAtLeast(goal) * 1.2f
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) { animProgress.animateTo(1f, tween(1000)) }
    Box(Modifier.fillMaxWidth().height(200.dp).padding(vertical = 16.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val barWidth = size.width / (data.size * 1.5f).coerceAtLeast(1f)
            val spacing = barWidth / 2f
            val goalY = size.height - (goal / maxValue * size.height)

            drawLine(
                color = Color.Gray,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )

            data.forEachIndexed { index, value ->
                val barHeight = (value / maxValue) * size.height * animProgress.value
                val x = spacing + index * (barWidth + spacing)
                drawRoundRect(
                    color = highlightLogic(value),
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }
    }
}

@Composable
private fun VoltMacroRing(label: String, value: Int, goal: Int, color: Color) {
    val sweepAnim = remember { Animatable(0f) }
    LaunchedEffect(value) {
        val target = (value.toFloat() / goal.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
        sweepAnim.animateTo(target, tween(1200))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp), Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(Color.White.copy(0.05f), 0f, 360f, false, style = Stroke(12f))
                drawArc(color, -90f, 360f * sweepAnim.value, false, style = Stroke(12f, cap = StrokeCap.Round))
            }
            Text("$value", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ConsistencyBadge(label: String, stdDev: Double) {
    // Check for NaN to prevent crash in 'when'
    val safeStdDev = if (stdDev.isNaN()) 100.0 else stdDev
    val (text, color) = when {
        safeStdDev < 15.0 -> "High" to Color(0xFF4CAF50)
        safeStdDev < 35.0 -> "Med" to VoltFat
        else -> "Low" to Color(0xFFF44336)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = CircleShape,
            border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
        ) {
            Text(
                text = text,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp).background(VoltSurfaceVariant, RoundedCornerShape(12.dp)).padding(16.dp), Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray)
        Text(value, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyStateMascot(msg: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally, // 📍 FIXED: Removed the 'as Arrangement' cast
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚡", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = msg,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// --- UTILS ---

private fun calculateStreak(meals: List<MealLog>): Int {
    if (meals.isEmpty()) return 0
    val dates = meals.map { OffsetDateTime.parse(it.created_at).toLocalDate() }.distinct().sortedDescending()
    var streak = 0
    var current = LocalDate.now()
    if (!dates.contains(current)) current = current.minusDays(1)
    for (date in dates) {
        if (date == current) { streak++; current = current.minusDays(1) } else break
    }
    return streak
}

private fun calculateConsistency(values: List<Double>): Double {
    if (values.size < 2) return 100.0
    val avg = values.average()
    return sqrt(values.sumOf { (it - avg).pow(2.0) } / values.size.toDouble())
}

private fun calculateWaterStreak(totals: Map<java.time.LocalDate, Int>, goal: Int): Int {
    if (totals.isEmpty()) return 0

    var streak = 0
    val today = java.time.LocalDate.now()

    var currentCheckDate = if ((totals[today] ?: 0) >= goal) today else today.minusDays(1)

    while ((totals[currentCheckDate] ?: 0) >= goal) {
        streak++
        currentCheckDate = currentCheckDate.minusDays(1)
    }

    return streak
}