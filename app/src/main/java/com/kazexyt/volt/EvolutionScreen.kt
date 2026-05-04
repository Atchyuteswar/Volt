package com.kazexyt.volt

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazexyt.volt.model.MealLog
import com.kazexyt.volt.ui.theme.VoltBlack
import com.kazexyt.volt.ui.theme.VoltCyan
import com.kazexyt.volt.ui.theme.VoltError
import com.kazexyt.volt.ui.theme.VoltLavender
import com.kazexyt.volt.ui.theme.VoltPurple
import com.kazexyt.volt.ui.theme.VoltSurface
import com.kazexyt.volt.ui.theme.VoltSurfaceVariant
import com.kazexyt.volt.utils.StatsUtils
import com.kazexyt.volt.model.UserProfile.*

@Composable
fun EvolutionScreen(
    loggedMeals: List<MealLog>,
    daily_goal: Int = 2200,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val weeklyData = remember(loggedMeals) { StatsUtils.getWeeklyEvolution(loggedMeals) }

    val avgCalories = if (weeklyData.isNotEmpty()) weeklyData.map { it.second }.average().toInt() else 0
    val peakCalories = if (weeklyData.isNotEmpty()) weeklyData.maxOf { it.second } else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoltBlack) // Pure black for better contrast
            .verticalScroll(scrollState)
    ) {
        // --- VIBRANT HEADER AREA ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            // Background Mesh Gradient Effect
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(VoltPurple.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(size.width * 0.8f, 0f),
                        radius = size.width
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(VoltLavender.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(0f, size.height * 0.5f),
                        radius = size.width * 0.8f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 64.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "Analytics",
                            style = MaterialTheme.typography.labelLarge,
                            color = VoltLavender,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Evolution",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(56.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Highlight Stat Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WEEKLY AVERAGE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$avgCalories", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                            Text("kcal", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(0.8f)) {
                        Text("PEAK DAY", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$peakCalories", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text("kcal", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                        }
                    }
                }
            }
        }

        val chartData = weeklyData.map {
            DailyStat(
                dayName = it.first,
                consumed = it.second.toFloat(),
                goal = daily_goal.toFloat()
            )
        }

        // --- CONTENT SECTION ---
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            // Chart Card
            Surface(
                color = VoltSurface,
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Caloric Intake", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Surface(
                            color = VoltPurple.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                "Last 7 Days",
                                color = VoltLavender,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                    AnimatedBarChart(data = chartData, daily_goal = daily_goal)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EvolutionStatCard(
                    title = "Streak",
                    value = "12",
                    unit = "days",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                EvolutionStatCard(
                    title = "Consistency",
                    value = "94",
                    unit = "%",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = VoltLavender,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- VOLT'S INSIGHT CARD ---
            VoltInsightCard(avgCalories = avgCalories, daily_goal = daily_goal)
        }
    }
}

// --- SUB-COMPONENTS ---

data class DailyStat(val dayName: String, val consumed: Float, val goal: Float)

@Composable
fun AnimatedBarChart(
    data: List<DailyStat>,
    daily_goal: Int,
    modifier: Modifier = Modifier
) {
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }

    val goalY = daily_goal.toFloat() 
    val maxDisplay = (daily_goal * 1.5).toFloat()

    Box(modifier = modifier.fillMaxWidth().height(200.dp)) {
        // Goal Line and Text
        Canvas(modifier = Modifier.fillMaxSize()) {
            val goalRatio = goalY / maxDisplay
            val yPos = size.height * (1f - goalRatio)
            
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, yPos),
                end = Offset(size.width, yPos),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
            )
        }
        
        // Bars
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, stat ->
                val fillRatio = (stat.consumed / maxDisplay).coerceIn(0.05f, 1f)
                val isOver = stat.consumed > stat.goal

                val animatedHeight by animateFloatAsState(
                    targetValue = if (startAnimation) fillRatio else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "bar_anim_$index"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                            val barWidth = size.width
                            val maxBarHeight = size.height
                            val actualHeight = maxBarHeight * animatedHeight
                            val cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())

                            // Track
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.03f),
                                size = Size(barWidth, maxBarHeight),
                                cornerRadius = cornerRadius
                            )

                            // Fill
                            val brush = if (isOver) {
                                Brush.verticalGradient(listOf(VoltError, Color(0xFFFF8A80)))
                            } else {
                                Brush.verticalGradient(listOf(VoltPurple, VoltLavender))
                            }
                            
                            drawRoundRect(
                                brush = brush,
                                topLeft = Offset(0f, maxBarHeight - actualHeight),
                                size = Size(barWidth, actualHeight),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stat.dayName,
                        color = if (index == 6) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = if (index == 6) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EvolutionStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    icon: ImageVector,
    iconTint: Color,
) {
    Surface(
        color = VoltSurface,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconTint.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        unit,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            Text(title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun VoltInsightCard(
    avgCalories: Int,
    daily_goal: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VoltPurple,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(VoltPurple, Color(0xFF3B2A5F))
                    )
                )
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Animated-like Mascot
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp, 12.dp).background(Color.White, CircleShape))
                        Box(modifier = Modifier.size(6.dp, 12.dp).background(Color.White, CircleShape))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Volt's Insight",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Text(
                        "Ai Powered Analysis",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val message = if (avgCalories > daily_goal) {
                val percentage = ((avgCalories - daily_goal).toFloat() / daily_goal * 100).toInt()
                "Your intake is currently $percentage% above your target. Consider reducing portion sizes for dinner to stay in the zone."
            } else {
                "Perfect calibration! You're hitting your targets with 94% accuracy. Keep this momentum for the next 3 days."
            }
            
            Text(
                message,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                onClick = {} // Interaction makes it feel premium
            ) {
                Text(
                    "Details",
                    color = VoltPurple,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}
