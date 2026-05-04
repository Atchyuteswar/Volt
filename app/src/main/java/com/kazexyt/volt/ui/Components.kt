package com.kazexyt.volt.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazexyt.volt.model.MascotState
import com.kazexyt.volt.model.Screen
import androidx.compose.material.icons.filled.Add
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.animation.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.vector.ImageVector
import com.kazexyt.volt.ui.theme.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState

// Color constants moved to ui.theme.Color.kt

@Composable
fun PulseMascot(
    modifier: Modifier = Modifier,
    size: Dp = 90.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .background(VoltLavender, RoundedCornerShape(size * 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(size * 0.12f)) {
            Box(
                modifier = Modifier
                    .size(size * 0.12f, size * 0.28f)
                    .background(VoltSurface, RoundedCornerShape(50))
            )
            Box(
                modifier = Modifier
                    .size(size * 0.12f, size * 0.28f)
                    .background(VoltSurface, RoundedCornerShape(50))
            )
        }
    }
}

@Composable
fun CalorieDashboard(
    consumed: Int,
    goal: Int,
    emotion: MascotState,
    modifier: Modifier = Modifier
) {
    val progress = (consumed.toFloat() / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val remaining = goal - consumed

    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val eyeScaleY by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3500
                1f at 0
                1f at 3300
                0.1f at 3400
                1f at 3500
            }
        ),
        label = "eye_blink"
    )

    Surface(
        color = VoltSurfaceVariant,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${remaining.coerceAtLeast(0)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (remaining >= 0) "kcal remaining" else "kcal over goal",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(VoltLavender)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        when (emotion) {
                            MascotState.HAPPY -> VoltLavender
                            MascotState.SLEEPY -> VoltPurple
                            MascotState.NORMAL -> Color(0xFF333333)
                        },
                        RoundedCornerShape(35.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Simplified Mascot Eyes based on emotion
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (emotion) {
                        MascotState.SLEEPY -> {
                            Box(modifier = Modifier.size(12.dp, 4.dp).background(Color.White.copy(alpha = 0.5f), CircleShape))
                            Box(modifier = Modifier.size(12.dp, 4.dp).background(Color.White.copy(alpha = 0.5f), CircleShape))
                        }
                        MascotState.HAPPY -> {
                             Box(modifier = Modifier.size(10.dp, 10.dp).border(3.dp, VoltSurface, CircleShape))
                             Box(modifier = Modifier.size(10.dp, 10.dp).border(3.dp, VoltSurface, CircleShape))
                        }
                        MascotState.NORMAL -> {
                            Box(
                                modifier = Modifier
                                    .size(10.dp, 20.dp)
                                    .graphicsLayer { scaleY = eyeScaleY }
                                    .background(Color.White, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp, 20.dp)
                                    .graphicsLayer { scaleY = eyeScaleY }
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MacroProgressCard(
    label: String,
    value: Int,
    goal: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = (value.toFloat() / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    
    Surface(
        color = VoltSurfaceVariant,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .width(105.dp)
            .height(140.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(24.dp),
                    color = color,
                    strokeWidth = 3.dp,
                    trackColor = color.copy(alpha = 0.1f)
                )
            }
            
            Column {
                Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("${value}g", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun Modifier.glassEffect(
    blurRadius: Dp = 20.dp,
    glassColor: Color = Color.White.copy(alpha = 0.08f),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    cornerRadius: Dp = 50.dp,
    shadowColor: Color = Color.Black.copy(alpha = 0.3f),
): Modifier = this
    .drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        blurRadius.toPx(),
                        0f, 0f,
                        android.graphics.Color.argb(
                            (shadowColor.alpha * 255).toInt(),
                            (shadowColor.red * 255).toInt(),
                            (shadowColor.green * 255).toInt(),
                            (shadowColor.blue * 255).toInt()
                        )
                    )
                }
            }
            canvas.drawRoundRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height,
                radiusX = cornerRadius.toPx(),
                radiusY = cornerRadius.toPx(),
                paint = paint
            )
        }
    }
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                glassColor.copy(alpha = glassColor.alpha * 1.5f),
                glassColor,
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = 0.8.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                borderColor,
                borderColor.copy(alpha = borderColor.alpha * 0.3f),
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )


// ─── Main NavBar ──────────────────────────────────────────────────────────────
@Composable
fun VoltFloatingNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCameraClick: () -> Unit,
    onBarcodeClick: () -> Unit,
    onManualClick: () -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(Screen.Dashboard, Screen.Analytics, Screen.Profile)
    var isExpanded by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_rotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomEnd
    ) {

        // ── Speed Dial Menu ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier.padding(bottom = 80.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                SpeedDialItem("Manual Entry",    Icons.Default.Edit)         { onManualClick();  isExpanded = false }
                SpeedDialItem("Barcode Scanner", Icons.Default.QrCodeScanner){ onBarcodeClick(); isExpanded = false }
                SpeedDialItem("AI Vision",       Icons.Default.CameraAlt)    { onCameraClick();  isExpanded = false }
                SpeedDialItem("Voice Log",       Icons.Default.Mic)          { onVoiceClick();   isExpanded = false }
            }
        }

        // ── Dock Row ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {

            // Nav Pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .glassEffect(
                        blurRadius = 18.dp,
                        glassColor = Color.White.copy(alpha = 0.07f),
                        borderColor = Color.White.copy(alpha = 0.18f),
                        cornerRadius = 50.dp,
                        shadowColor = Color.Black.copy(alpha = 0.35f)
                    )
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screenItem ->
                    NavIcon(
                        item = screenItem,
                        isSelected = currentRoute == screenItem.route,
                        onClick = { route ->
                            isExpanded = false
                            onNavigate(route)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action Orb
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .glassEffect(
                        blurRadius = 18.dp,
                        glassColor = if (isExpanded)
                            VoltPurple.copy(alpha = 0.6f)   // deep purple when open
                        else
                            Color.White.copy(alpha = 0.07f),
                        borderColor = if (isExpanded)
                            VoltCyan.copy(alpha = 0.55f)   // cyan ring when open
                        else
                            Color.White.copy(alpha = 0.2f),
                        cornerRadius = 50.dp,
                        shadowColor = if (isExpanded)
                            VoltCyan.copy(alpha = 0.25f)
                        else
                            Color.Black.copy(alpha = 0.4f)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isExpanded = !isExpanded }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Action",
                    tint = if (isExpanded) VoltCyan else Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}


// ─── Speed Dial Item ──────────────────────────────────────────────────────────
@Composable
private fun SpeedDialItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        // Glass label pill
        Box(
            modifier = Modifier
                .glassEffect(
                    blurRadius = 14.dp,
                    glassColor = Color.White.copy(alpha = 0.07f),
                    borderColor = Color.White.copy(alpha = 0.15f),
                    cornerRadius = 12.dp,
                    shadowColor = Color.Black.copy(alpha = 0.3f)
                )
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Icon orb
        Box(
            modifier = Modifier
                .size(46.dp)
                .glassEffect(
                    blurRadius = 14.dp,
                    glassColor = VoltLavender.copy(alpha = 0.08f),  // lavender tint
                    borderColor = Color.White.copy(alpha = 0.15f),
                    cornerRadius = 50.dp,
                    shadowColor = Color.Black.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = VoltLavender,   // lavender icons
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


// ─── Nav Icon ─────────────────────────────────────────────────────────────────
@Composable
private fun NavIcon(
    item: Screen,
    isSelected: Boolean,
    onClick: (String) -> Unit
) {
    // Animate the pill background color behind the icon
    val bgAlpha by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(250),
        label = "nav_bg"
    )

    // Animate the icon color (Bright white when active, muted grey when inactive)
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFFA0A0A5),
        animationSpec = tween(250),
        label = "nav_content"
    )

    // The touch target container
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Removes ripple
                onClick = { onClick(item.route) }
            )
            .padding(horizontal = 4.dp, vertical = 8.dp) // Generous touch padding
    ) {
        // The Icon contained within the animated pill background
        Box(
            modifier = Modifier
                .height(36.dp) // Slightly taller to look balanced without text
                .width(56.dp)  // Slightly wider for a premium pill shape
                .background(bgAlpha, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            item.icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = item.label, // Screen readers still read this!
                    tint = contentColor,
                    modifier = Modifier.size(24.dp) // Bumped icon size up slightly
                )
            }
        }
    }
}

@Composable
fun VoltTimelinePicker(
    selectedDate: LocalDate,
    startDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    // 📍 CRITICAL FIX: The 'remember' block now tracks 'startDate'
    // and generates dates from your first log until today.
    val dates = remember(startDate) {
        val today = LocalDate.now()
        val list = mutableListOf<LocalDate>()
        var current = startDate

        // Ensure we show at least today if the list is empty
        if (current.isAfter(today)) {
            list.add(today)
        } else {
            while (!current.isAfter(today)) {
                list.add(current)
                current = current.plusDays(1)
            }
        }
        list
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (dates.size - 1).coerceAtLeast(0))

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            val bgColor by animateColorAsState(if (isSelected) VoltPurple else Color.Transparent, label = "date_bg")
            val textColor by animateColorAsState(if (isSelected) Color.White else Color(0xFF888888), label = "date_text")

            Surface(
                color = bgColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(52.dp)
                    .height(72.dp)
                    .clickable { onDateSelected(date) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        color = textColor.copy(alpha = if (isSelected) 0.8f else 1f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = date.dayOfMonth.toString(), color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}