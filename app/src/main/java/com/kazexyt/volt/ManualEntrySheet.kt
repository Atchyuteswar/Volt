package com.kazexyt.volt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazexyt.volt.model.MealLog
import com.kazexyt.volt.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntrySheet(
    onDismiss: () -> Unit,
    onSave: (MealLog) -> Unit,
    modifier: Modifier = Modifier,
    initialMeal: MealLog? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 1. BASE DATA: We store the "per 100g" or "AI Parsed" values as the source of truth
    val baseCalories = remember(initialMeal) { initialMeal?.calories?.toFloat() ?: 0f }
    val baseProtein = remember(initialMeal) { initialMeal?.protein?.toFloat() ?: 0f }
    val baseCarbs = remember(initialMeal) { initialMeal?.carbs?.toFloat() ?: 0f }
    val baseFat = remember(initialMeal) { initialMeal?.fat?.toFloat() ?: 0f }

    // 2. INPUT STATES (📍 FIXED: Now using remember(initialMeal) to prevent State Ignorance)
    var foodName by remember(initialMeal) { mutableStateOf(initialMeal?.food_name ?: "") }
    var servingSize by remember(initialMeal) { mutableStateOf("100") }

    // Multiplier Math
    val multiplier = (servingSize.toFloatOrNull() ?: 0f) / 100f

    // 3. SCALED VALUES: These update live as the user types the serving size
    val displayCalories = if (initialMeal != null) (baseCalories * multiplier).toInt() else 0
    val displayProtein = if (initialMeal != null) (baseProtein * multiplier).toInt() else 0
    val displayCarbs = if (initialMeal != null) (baseCarbs * multiplier).toInt() else 0
    val displayFat = if (initialMeal != null) (baseFat * multiplier).toInt() else 0

    // Manual Overrides (Used if user is NOT using AI/Barcode)
    var manualCals by remember(initialMeal) { mutableStateOf("") }
    var manualProt by remember(initialMeal) { mutableStateOf("") }
    var manualCarbs by remember(initialMeal) { mutableStateOf("") }
    var manualFat by remember(initialMeal) { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VoltSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = if (initialMeal != null) "Confirm Log" else "Manual Entry",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- FOOD NAME ---
            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text("Food Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = VoltLavender
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- SERVING SIZE ADJUSTMENT ---
            OutlinedTextField(
                value = servingSize,
                onValueChange = { servingSize = it },
                label = { Text("Quantity (grams)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text("g", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = VoltCyan,
                    focusedIndicatorColor = VoltCyan
                )
            )

            // --- QUICK SCALE BUTTONS ---
            Row(
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50, 100, 200).forEach { amount ->
                    AssistChip(
                        onClick = { servingSize = amount.toString() },
                        label = { Text("${amount}g") },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = if (servingSize == amount.toString()) VoltBlack else Color.White,
                            containerColor = if (servingSize == amount.toString()) VoltLavender else Color.Transparent
                        ),
                        border = BorderStroke(1.dp, Color.DarkGray)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- MACRO PREVIEW GRID ---
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroField(
                    label = "Calories",
                    value = if (initialMeal != null) displayCalories.toString() else manualCals,
                    onValueChange = { manualCals = it },
                    isReadOnly = initialMeal != null,
                    modifier = Modifier.weight(1f)
                )
                MacroField(
                    label = "Protein",
                    value = if (initialMeal != null) displayProtein.toString() else manualProt,
                    onValueChange = { manualProt = it },
                    isReadOnly = initialMeal != null,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MacroField(
                    label = "Carbs",
                    value = if (initialMeal != null) displayCarbs.toString() else manualCarbs,
                    onValueChange = { manualCarbs = it },
                    isReadOnly = initialMeal != null,
                    modifier = Modifier.weight(1f)
                )
                MacroField(
                    label = "Fat",
                    value = if (initialMeal != null) displayFat.toString() else manualFat,
                    onValueChange = { manualFat = it },
                    isReadOnly = initialMeal != null,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 📍 FIXED: Safely handle decimals without crashing
            val finalCals = if (initialMeal != null) displayCalories else manualCals.toFloatOrNull()?.toInt() ?: 0
            val finalProtein = if (initialMeal != null) displayProtein else manualProt.toFloatOrNull()?.toInt() ?: 0
            val finalCarbs = if (initialMeal != null) displayCarbs else manualCarbs.toFloatOrNull()?.toInt() ?: 0
            val finalFat = if (initialMeal != null) displayFat else manualFat.toFloatOrNull()?.toInt() ?: 0

            // 📍 FIXED: Validate data before allowing a save
            val isValid = foodName.isNotBlank() && finalCals > 0

            // --- SAVE BUTTON ---
            Button(
                onClick = {
                    if (isValid) {
                        // Ensure a valid UUID is generated just in case the AI failed to provide one
                        val safeId = initialMeal?.id?.takeIf { it.isNotBlank() && it.length > 10 } ?: java.util.UUID.randomUUID().toString()

                        val meal = initialMeal?.copy(
                            id = safeId,
                            food_name = foodName,
                            calories = finalCals,
                            protein = finalProtein,
                            carbs = finalCarbs,
                            fat = finalFat
                        ) ?: MealLog(
                            id = safeId,
                            user_id = "",
                            food_name = foodName,
                            calories = finalCals,
                            protein = finalProtein,
                            carbs = finalCarbs,
                            fat = finalFat,
                            created_at = java.time.OffsetDateTime.now().toString()
                        )
                        onSave(meal)
                    }
                },
                enabled = isValid, // Button turns gray if invalid!
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VoltLavender,
                    contentColor = VoltBlack,
                    disabledContainerColor = Color.DarkGray,
                    disabledContentColor = Color.Gray
                )
            ) {
                Text(
                    text = if (initialMeal != null) "Log ${servingSize}g" else "Log Meal",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun MacroField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isReadOnly: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (!isReadOnly) onValueChange(it) },
        label = { Text(label) },
        readOnly = isReadOnly,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = if (isReadOnly) VoltLavender else Color.White,
            disabledTextColor = Color.Gray,
            focusedIndicatorColor = if (isReadOnly) Color.Transparent else VoltLavender
        )
    )
}