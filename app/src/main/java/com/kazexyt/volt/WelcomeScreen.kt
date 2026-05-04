package com.kazexyt.volt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// --- VOLT PREMIUM COLOR PALETTE ---
private val VoltBlack = Color(0xFF080808)
private val VoltSurface = Color(0xFF161616)
private val VoltCyan = Color(0xFF00E5FF)
private val VoltLavender = Color(0xFFEADBFF)
private val VoltPurple = Color(0xFF5D4291)
private val VoltProtein = Color(0xFFF2B8B5)
private val VoltCarb = Color(0xFFA8C7FA)
private val VoltFat = Color(0xFFFFD180)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateIn = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "scale"
    )


    Box(modifier = Modifier.fillMaxSize().background(VoltBlack)) {

        // --- THE 3-PAGE PREMIUM ONBOARDING ---
        AnimatedVisibility(
            visible = animateIn,
            enter = fadeIn(tween(800)) + slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(1000, easing = EaseOutExpo)),
            modifier = Modifier.fillMaxSize()
        ) {
            val pagerState = rememberPagerState(pageCount = { 3 })

            Box(modifier = Modifier.fillMaxSize()) {

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> PageOneAira()
                        1 -> PageTwoHydration()
                        2 -> PageThreeAnalytics(onNavigateToSignUp, onNavigateToLogin)
                    }
                }

                // --- FLOATING PAGE INDICATORS ---
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { iteration ->
                        val color = if (pagerState.currentPage == iteration) VoltCyan else Color.DarkGray
                        val width by animateDpAsState(targetValue = if (pagerState.currentPage == iteration) 32.dp else 12.dp, label = "indicator")
                        Box(
                            modifier = Modifier.padding(horizontal = 6.dp).height(6.dp).width(width).clip(CircleShape).background(color)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// PAGE 1: AIRA & HARDWARE INTEGRATION (4 CELLS)
// ============================================================================
@Composable
private fun PageOneAira() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 64.dp, bottom = 64.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Meet Volt.", color = VoltLavender, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Text("Your On-the-go Calorie Tracker", color = Color.Gray, fontSize = 18.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // CELL 1: Hero
        BentoCard(modifier = Modifier.weight(1.3f).fillMaxWidth().background(Brush.radialGradient(listOf(VoltPurple.copy(alpha = 0.2f), Color.Transparent), radius = 600f))) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AutoAwesome, null, tint = VoltLavender, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Powered by AI Models, Volt eliminates manual searching. He parses exactly what you eat instantly. Experience the next generation of health tracking without the friction.",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CELLS 2 & 3: Voice and Vision
        Row(modifier = Modifier.weight(1.2f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // CELL 2
            BentoCard(modifier = Modifier.weight(1f).fillMaxHeight(), padding = 20.dp) {
                val voiceTransition = rememberInfiniteTransition(label = "voice")
                val waveScale by voiceTransition.animateFloat(initialValue = 0.5f, targetValue = 1.6f, animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse), label = "")

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(64.dp).background(VoltCyan.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(32.dp * waveScale).border(2.dp, VoltCyan.copy(alpha = 0.4f), CircleShape))
                        Icon(Icons.Rounded.Mic, null, tint = VoltCyan, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Voice AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Speak meals naturally & Aira calculates the macros.", color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                }
            }

            // CELL 3
            BentoCard(modifier = Modifier.weight(1f).fillMaxHeight(), padding = 20.dp) {
                val visionTransition = rememberInfiniteTransition(label = "scan")
                val laserY by visionTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "")

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(12.dp)).background(VoltLavender.copy(0.05f)).drawBehind {
                        val y = size.height * laserY
                        drawLine(VoltLavender, Offset(0f, y), Offset(size.width, y), strokeWidth = 6f)
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, VoltLavender.copy(0.4f))), topLeft = Offset(0f, y - 30f), size = Size(size.width, 30f))
                    }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.QrCodeScanner, null, tint = VoltLavender, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Vision", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Scan over 2 million global barcodes instantly.", color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CELL 4: Speed
        BentoCard(modifier = Modifier.weight(0.8f).fillMaxWidth(), padding = 20.dp) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(56.dp).background(Color(0xFFFFD180).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Bolt, null, tint = Color(0xFFFFD180), modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text("Lightning Fast Logging", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Zero friction entry. Your entire day logged in under 60 seconds with absolute precision.", color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

// ============================================================================
// PAGE 2: DYNAMIC HYDRATION (2 CELLS)
// ============================================================================
@Composable
private fun PageTwoHydration() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 64.dp, bottom = 64.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Smart Hydration.", color = VoltCyan, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Text("Precision Liquid Tracking.", color = Color.Gray, fontSize = 18.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // CELL 1: Massive Animated Ring (Responsive)
        val infiniteTransition = rememberInfiniteTransition(label = "water")
        val waveOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse), label = "")

        BentoCard(modifier = Modifier.weight(2f).fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidthPx = 24.dp.toPx() // Massively increased thickness
                    val radius = (size.minDimension - strokeWidthPx) / 2.2f
                    val arcTopLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2, radius * 2)

                    drawArc(Color.White.copy(0.05f), 0f, 360f, false, style = Stroke(width = strokeWidthPx), topLeft = arcTopLeft, size = arcSize)
                    drawArc(
                        brush = Brush.linearGradient(listOf(VoltCyan, Color(0xFF007BFF))),
                        startAngle = -90f, sweepAngle = 180f + (40f * waveOffset), useCenter = false,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round), topLeft = arcTopLeft, size = arcSize
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.WaterDrop, null, tint = VoltCyan, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("2750ml", color = Color.White, fontWeight = FontWeight.Black, fontSize = 48.sp)
                    Text("Goal: 3500ml", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CELL 2: Biology Math
        BentoCard(modifier = Modifier.weight(1f).fillMaxWidth(), padding = 24.dp) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Biology-Based Goals", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Standard 2-liter rules are obsolete. Volt leverages your real-time biological profile (age, gender, weight) to calculate a dynamic hydration baseline. We track your personal bests to push you further every single day.",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ============================================================================
// PAGE 3: DEEP ANALYTICS + AUTH BUTTONS (3 CELLS)
// ============================================================================
@Composable
private fun PageThreeAnalytics(onNavigateToSignUp: () -> Unit, onNavigateToLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 64.dp, bottom = 48.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Deep Insights.", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Text("Visualize your evolution.", color = Color.Gray, fontSize = 18.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // CELLS 1 & 2: Macros & Matrix
        Row(modifier = Modifier.weight(1.3f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // CELL 1: Macros
            BentoCard(modifier = Modifier.weight(1f).fillMaxHeight(), padding = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(50))) {
                        Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(VoltProtein))
                        Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(VoltCarb))
                        Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(VoltFat))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Precision Macros", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Break down your caloric intake into exact P/C/F splits.", color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                }
            }

            // CELL 2: Matrix
            BentoCard(modifier = Modifier.weight(1f).fillMaxHeight(), padding = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(64.dp), verticalAlignment = Alignment.Bottom) {
                        listOf(0.4f, 0.7f, 1f, 0.6f, 0.9f, 0.5f, 0.8f).forEach { heightRatio ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight(heightRatio).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(VoltPurple))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("140-Day Matrix", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Track consistency heatmaps & monitor adherence.", color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CELL 3: AI Coach
        BentoCard(modifier = Modifier.weight(1f).fillMaxWidth(), padding = 20.dp) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(56.dp).background(VoltLavender.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Analytics, null, tint = VoltLavender, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text("Weekly Coach Debriefs", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Let Volt analyze your week and generate custom strategies to overcome plateaus.", color = Color.Gray, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- AUTH BUTTONS (Only rendered on the last page) ---
        Button(
            onClick = onNavigateToSignUp,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VoltCyan, contentColor = Color.Black)
        ) {
            Text("Create Account", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text("I already have an account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- SHARED BENTO CARD COMPONENT ---
@Composable
private fun BentoCard(
    modifier: Modifier = Modifier,
    padding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = VoltSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.05f)),
        modifier = modifier
    ) {
        // 📍 USING fillMaxSize() AND Arrangement.Center PREVENTS ALL EMPTY SPACES
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}