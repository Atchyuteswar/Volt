package com.kazexyt.volt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazexyt.volt.model.MealLog
import com.kazexyt.volt.utils.AiCoachManager
import com.kazexyt.volt.utils.StatsUtils
import kotlinx.coroutines.launch

@Composable
fun VoltCoachScreen(
    loggedMeals: List<MealLog>, // <--- 1. Now accepts real data
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var aiDebrief by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }

    // 2. Crunch the real numbers dynamically
    val averageKcal = remember(loggedMeals) { StatsUtils.getAverageCalories(loggedMeals) }
    val averageProtein = remember(loggedMeals) {
        if (loggedMeals.isEmpty()) 0 else {
            val uniqueDays = loggedMeals.mapNotNull { it.created_at?.take(10) }.distinct().size
            if (uniqueDays > 0) loggedMeals.sumOf { it.protein } / uniqueDays else 0
        }
    }
    val currentStreak = remember(loggedMeals) { StatsUtils.calculateStreak(loggedMeals) }
    val consistencyStatus = if (currentStreak >= 3) "great" else "needs improvement"

    // Dynamic background matching the "Volt" aesthetic
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1A2E), Color(0xFF121212))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Volt Coach", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- AI WELCOME / DEBRIEF AREA ---
        Surface(
            color = Color(0xFF0A0A0A),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Subtle cyan glow behind the text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.1f), Color.Transparent), radius = 600f))
                )

                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(24.dp))

                    if (aiDebrief != null) {
                        // The Result
                        Text(
                            text = aiDebrief!!,
                            color = Color.White,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    } else if (isGenerating) {
                        // The Loading State
                        CircularProgressIndicator(color = Color(0xFF00E5FF))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyzing your week...", color = Color.Gray)
                    } else {
                        // The Prompt - Now hints at real data!
                        Text(
                            text = if (loggedMeals.isEmpty()) {
                                "Log some meals first so I can analyze your habits!"
                            } else {
                                "Ready for your weekly breakdown? I'll analyze your $currentStreak-day streak and give you a custom game plan."
                            },
                            color = Color.Gray,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ACTION BUTTON ---
        Button(
            onClick = {
                if (!isGenerating && aiDebrief == null && loggedMeals.isNotEmpty()) {
                    isGenerating = true
                    coroutineScope.launch {
                        // 3. Injecting REAL data into the AI
                        aiDebrief = AiCoachManager.generateWeeklyDebrief(
                            avgCalories = averageKcal,
                            avgProtein = averageProtein,
                            waterConsistency = consistencyStatus
                        )
                        isGenerating = false
                    }
                }
            },
            enabled = loggedMeals.isNotEmpty() || aiDebrief != null,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (aiDebrief != null) Color(0xFF252525) else Color(0xFF00E5FF),
                contentColor = if (aiDebrief != null) Color.White else Color(0xFF121212),
                disabledContainerColor = Color.DarkGray,
                disabledContentColor = Color.Gray
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text(
                text = if (loggedMeals.isEmpty()) "Need Data to Analyze"
                else if (aiDebrief != null) "Analysis Complete"
                else "Generate My Debrief",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }
    }
}