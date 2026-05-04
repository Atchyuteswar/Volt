package com.kazexyt.volt

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazexyt.volt.model.VoltOnboardingData
import com.kazexyt.volt.ui.theme.VoltBlack
import com.kazexyt.volt.ui.theme.VoltCyan
import com.kazexyt.volt.ui.theme.VoltLavender
import com.kazexyt.volt.ui.theme.VoltSurface

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (VoltOnboardingData) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 5
    val data = remember { mutableStateOf(VoltOnboardingData()) }

    Column(modifier = modifier.fillMaxSize().background(VoltBlack).padding(top = 48.dp)) {

        // --- HEADER & PROGRESS BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (currentStep > 1) currentStep-- },
                enabled = currentStep > 1,
                modifier = Modifier.background(if (currentStep > 1) VoltSurface else Color.Transparent, CircleShape)
            ) {
                if (currentStep > 1) Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Animated Progress Track
            Row(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(VoltSurface)) {
                Box(
                    modifier = Modifier.fillMaxHeight()
                        .fillMaxWidth(animateFloatAsState(targetValue = currentStep.toFloat() / totalSteps, label = "").value)
                        .background(VoltCyan)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))
            Text("$currentStep of $totalSteps", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- ANIMATED CONTENT BODY ---
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(tween(500)) { width -> width } + fadeIn(tween(500)) togetherWith
                            slideOutHorizontally(tween(500)) { width -> -width } + fadeOut(tween(500))
                } else {
                    slideInHorizontally(tween(500)) { width -> -width } + fadeIn(tween(500)) togetherWith
                            slideOutHorizontally(tween(500)) { width -> width } + fadeOut(tween(500))
                }
            },
            modifier = Modifier.weight(1f),
            label = "step_transition"
        ) { step ->
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                when (step) {
                    1 -> StepIdentity(data)
                    2 -> StepBaseline(data)
                    3 -> StepLifestyle(data)
                    4 -> StepDestination(data)
                    5 -> StepMindset(data)
                }
            }
        }

        // --- STICKY FOOTER BUTTON ---
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            val isStepValid = when (currentStep) {
                1 -> data.value.name.isNotBlank() && data.value.gender.isNotBlank() && data.value.birthDate.length >= 8
                2 -> data.value.height.isNotBlank() && data.value.weight.isNotBlank()
                3 -> data.value.activityLevel.isNotBlank() && data.value.exerciseFreq.isNotBlank()
                4 -> data.value.primaryGoal.isNotBlank() && data.value.improvementGoals.isNotEmpty()
                5 -> data.value.history.isNotBlank() && data.value.obstacles.isNotEmpty()
                else -> false
            }

            Button(
                onClick = {
                    if (currentStep < totalSteps) currentStep++
                    else onComplete(data.value)
                },
                enabled = isStepValid,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoltCyan,
                    contentColor = VoltBlack,
                    disabledContainerColor = VoltSurface,
                    disabledContentColor = Color.DarkGray
                )
            ) {
                Text(
                    text = if (currentStep == totalSteps) "Activate Aira" else "Continue",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

// ============================================================================
// STEP 1: IDENTITY
// ============================================================================
@Composable
fun StepIdentity(
    data: MutableState<VoltOnboardingData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("Let's build your profile.", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Text("Aira needs this to calculate your baseline.", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

        VoltTextField(value = data.value.name, onValueChange = { data.value = data.value.copy(name = it) }, label = "First Name")
        Spacer(modifier = Modifier.height(24.dp))

        Text("Biological Sex", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Male", "Female").forEach { gender ->
                VoltSelectionCard(
                    text = gender,
                    isSelected = data.value.gender == gender,
                    onClick = { data.value = data.value.copy(gender = gender) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        VoltTextField(
            value = data.value.birthDate,
            onValueChange = { data.value = data.value.copy(birthDate = it) },
            label = "Birth Date (DD/MM/YYYY)",
            keyboardType = KeyboardType.Number
        )
    }
}

// ============================================================================
// STEP 2: BASELINE
// ============================================================================
@Composable
fun StepBaseline(
    data: MutableState<VoltOnboardingData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text("Your starting point.", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Text("Required for the Mifflin-St Jeor equation.", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

        Row(modifier = Modifier.background(VoltSurface, RoundedCornerShape(32.dp)).padding(4.dp)) {
            listOf("Metric", "Imperial").forEach { unit ->
                val isSelected = data.value.units == unit
                Box(
                    modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(28.dp))
                        .background(if (isSelected) VoltCyan else Color.Transparent)
                        .clickable { data.value = data.value.copy(units = unit) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(unit, color = if (isSelected) VoltBlack else Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            VoltTextField(
                value = data.value.height, onValueChange = { data.value = data.value.copy(height = it) },
                label = if (data.value.units == "Metric") "Height (cm)" else "Height (ft/in)",
                keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f)
            )
            VoltTextField(
                value = data.value.weight, onValueChange = { data.value = data.value.copy(weight = it) },
                label = if (data.value.units == "Metric") "Weight (kg)" else "Weight (lbs)",
                keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f)
            )
        }
    }
}

// ============================================================================
// STEP 3: LIFESTYLE
// ============================================================================
@Composable
fun StepLifestyle(
    data: MutableState<VoltOnboardingData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text("How active are you?", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Daily Activity Level", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        listOf("Sedentary (Desk Job)", "Lightly Active (Standing)", "Highly Active (Labor)").forEach { level ->
            VoltSelectionCard(text = level, isSelected = data.value.activityLevel == level, onClick = { data.value = data.value.copy(activityLevel = level) }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Exercise Frequency", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 200.dp)) {
            items(listOf("None", "1-2x Week", "3-5x Week", "Everyday")) { freq ->
                VoltSelectionCard(text = freq, isSelected = data.value.exerciseFreq == freq, onClick = { data.value = data.value.copy(exerciseFreq = freq) }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ============================================================================
// STEP 4: DESTINATION
// ============================================================================
@Composable
fun StepDestination(
    data: MutableState<VoltOnboardingData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text("Your objective.", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Primary Goal", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        listOf("Lose Weight", "Maintain Weight", "Gain Muscle").forEach { goal ->
            VoltSelectionCard(text = goal, isSelected = data.value.primaryGoal == goal, onClick = { data.value = data.value.copy(primaryGoal = goal) }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Areas of Improvement (Select Multiple)", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        val options = listOf("Eat more protein", "Drink more water", "Cut sugar", "Better sleep")
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 200.dp)) {
            items(options) { opt ->
                val isSelected = data.value.improvementGoals.contains(opt)
                VoltSelectionCard(
                    text = opt, isSelected = isSelected,
                    onClick = {
                        val newSet = if (isSelected) data.value.improvementGoals - opt else data.value.improvementGoals + opt
                        data.value = data.value.copy(improvementGoals = newSet)
                    }, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ============================================================================
// STEP 5: MINDSET
// ============================================================================
@Composable
fun StepMindset(
    data: MutableState<VoltOnboardingData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text("Almost done.", color = VoltLavender, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Have you tracked macros before?", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Yes, consistently", "Tried and quit", "Never").forEach { hist ->
                VoltSelectionCard(text = hist, isSelected = data.value.history == hist, onClick = { data.value = data.value.copy(history = hist) }, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Biggest Obstacles (Select Multiple)", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        val obstacles = listOf("Time to log", "Confusing apps", "Cravings", "Eating out")
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 200.dp)) {
            items(obstacles) { obs ->
                val isSelected = data.value.obstacles.contains(obs)
                VoltSelectionCard(
                    text = obs, isSelected = isSelected,
                    onClick = {
                        val newSet = if (isSelected) data.value.obstacles - obs else data.value.obstacles + obs
                        data.value = data.value.copy(obstacles = newSet)
                    }, modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- REUSABLE PREMIUM UI COMPONENTS ---

@Composable
fun VoltTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, keyboardType: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, color = Color.Gray) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true, shape = RoundedCornerShape(24.dp), modifier = modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = VoltSurface, unfocusedContainerColor = VoltSurface,
            focusedIndicatorColor = VoltCyan, unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White, unfocusedTextColor = Color.White
        )
    )
}

@Composable
fun VoltSelectionCard(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = if (isSelected) VoltCyan.copy(alpha = 0.15f) else VoltSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isSelected) VoltCyan else Color.White.copy(0.05f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text, color = if (isSelected) Color.White else Color.Gray, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = VoltCyan, modifier = Modifier.size(18.dp))
            }
        }
    }
}