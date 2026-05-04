package com.kazexyt.volt

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazexyt.volt.model.MealLog
import com.kazexyt.volt.model.UserProfile
import com.kazexyt.volt.ui.theme.*
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    loggedMeals: List<MealLog>,
    onSettingsClick: () -> Unit,
    onSignOut: () -> Unit, // Add this to your NavHost to handle screen transition
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Real-time Data Intelligence
    val totalMeals = loggedMeals.size
    val totalCals = loggedMeals.sumOf { it.calories }
    val avgCals = if (totalMeals > 0) totalCals / totalMeals else 0

    // Dynamic Macro Targets (30/40/30 split)
    val goal = userProfile?.daily_goal ?: 2200
    val targetP = (goal * 0.30 / 4).toInt()
    val targetC = (goal * 0.40 / 4).toInt()
    val targetF = (goal * 0.30 / 9).toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoltBlack)
            .verticalScroll(scrollState)
    ) {
        // --- 1. MESH GRADIENT HEADER ---
        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(VoltPurple.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Avatar with "Nothing" Border
                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = CircleShape,
                    color = VoltSurface,
                    border = BorderStroke(2.dp, Brush.linearGradient(listOf(VoltCyan, VoltPurple)))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userProfile?.name?.take(1)?.uppercase() ?: "V",
                            style = MaterialTheme.typography.displayMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = userProfile?.name ?: "Volt Member",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Lvl. ${totalMeals / 10} Athlete", // Gamification hint
                    color = VoltCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 120.dp)) {

            // --- 2. BENTO STATS GRID ---
            Text("LIFETIME METRICS", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                VoltProfileBentoCard(
                    label = "Energy",
                    value = "${totalCals / 1000}k",
                    subValue = "kcal tracked",
                    accent = VoltFat,
                    icon = Icons.Default.LocalFireDepartment,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    VoltSmallStatRow("Meals", "$totalMeals", VoltCarb, Icons.Default.Restaurant)
                    Spacer(Modifier.height(16.dp))
                    VoltSmallStatRow("Avg Log", "$avgCals", VoltLavender, Icons.Default.Analytics)
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- 3. COMMITMENT CARD ---
            Text("DAILY COMMITMENT", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Surface(
                color = VoltSurface,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, VoltSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, null, tint = VoltCyan, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Primary Calorie Target", color = Color.Gray, fontSize = 11.sp)
                            Text("$goal kcal / day", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = VoltSurfaceVariant)
                    Spacer(Modifier.height(24.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        MacroTarget(label = "Protein", value = "${targetP}g", color = VoltProtein)
                        MacroTarget(label = "Carbs", value = "${targetC}g", color = VoltCarb)
                        MacroTarget(label = "Fat", value = "${targetF}g", color = VoltFat)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- 4. SYSTEM SETTINGS ---
            Text("ACCOUNT & SYSTEM", style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Surface(
                color = VoltSurface,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, VoltSurfaceVariant)
            ) {
                Column {
                    ProfileSettingsItem(Icons.Default.Person, "Edit Profile", onClick = {})
                    ProfileSettingsItem(Icons.Default.Notifications, "Notification Preferences", onClick = {})
                    ProfileSettingsItem(Icons.Default.Security, "Privacy & Data", onClick = {})
                    ProfileSettingsItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Sign Out",
                        isLast = true,
                        color = VoltError,
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    supabase.auth.signOut()
                                    onSignOut()
                                    Toast.makeText(context, "Signed out safely", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
private fun VoltProfileBentoCard(
    label: String,
    value: String,
    subValue: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VoltSurface,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, VoltSurfaceVariant),
        modifier = modifier
    ) {
        Column(Modifier.padding(24.dp)) {
            Icon(icon, null, tint = accent.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
            Spacer(Modifier.weight(1f))
            Text(value, color = accent, fontSize = 32.sp, fontWeight = FontWeight.Black)
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subValue, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
private fun VoltSmallStatRow(
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VoltSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, VoltSurfaceVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(label, color = Color.Gray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun MacroTarget(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileSettingsItem(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    isLast: Boolean = false,
    onClick: () -> Unit
) {
    Column(modifier = modifier.clickable { onClick() }) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if(color == Color.White) Color.Gray else color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
        }
        if (!isLast) HorizontalDivider(Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp, color = VoltSurfaceVariant)
    }
}