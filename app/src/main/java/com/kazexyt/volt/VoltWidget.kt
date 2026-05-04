package com.kazexyt.volt.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
// CRITICAL: New import for the progress bar
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.kazexyt.volt.R

class VoltWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        val CALORIES_KEY = intPreferencesKey("remaining_calories")
        val CALORIES_GOAL_KEY = intPreferencesKey("calories_goal")
        val PROTEIN_KEY = intPreferencesKey("protein")
        val CARBS_KEY = intPreferencesKey("carbs")
        val FAT_KEY = intPreferencesKey("fat")
        val WATER_KEY = intPreferencesKey("water")

        suspend fun updateData(
            context: Context,
            remainingKcal: Int,
            goal: Int = 2200,
            p: Int = 0, c: Int = 0, f: Int = 0, w: Int = 0
        ) {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(VoltWidget::class.java)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[CALORIES_KEY] = remainingKcal
                    prefs[CALORIES_GOAL_KEY] = goal
                    prefs[PROTEIN_KEY] = p
                    prefs[CARBS_KEY] = c
                    prefs[FAT_KEY] = f
                    prefs[WATER_KEY] = w
                }
                VoltWidget().update(context, glanceId)
            }
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            VoltWidgetUI(prefs)
        }
    }

    @Composable
    private fun VoltWidgetUI(prefs: Preferences) {
        val kcalRemaining = prefs[CALORIES_KEY] ?: 2200
        val goal = prefs[CALORIES_GOAL_KEY] ?: 2200
        val p = prefs[PROTEIN_KEY] ?: 0
        val c = prefs[CARBS_KEY] ?: 0
        val f = prefs[FAT_KEY] ?: 0
        val water = prefs[WATER_KEY] ?: 0

        // Calculate progress for the bar (0.0 to 1.0)
        val consumed = (goal - kcalRemaining).coerceAtLeast(0)
        val progressPercent = if (goal > 0) consumed.toFloat() / goal.toFloat() else 0f

        val accentColor = Color(0xFF00E5FF)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_background))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. TOP: Mascot & Branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_volt_mascot),
                    contentDescription = null,
                    modifier = GlanceModifier.size(18.dp)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "VOLT",
                    style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                )
            }

            Spacer(modifier = GlanceModifier.height(16.dp))

            // 2. CENTER: Calorie Focus
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$kcalRemaining",
                    style = TextStyle(color = ColorProvider(accentColor), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "KCAL LEFT",
                    style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 10.sp)
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

                // --- THE PROGRESS BAR (Custom Implementation) ---
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(ColorProvider(Color(0xFF222222)))
                ) {
                    val progress = progressPercent.coerceIn(0f, 1f)
                    if (progress > 0f) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxHeight()
                                .defaultWeight()
                                .background(ColorProvider(accentColor))
                        ) {}
                    }
                    if (progress < 1f) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxHeight()
                                .defaultWeight()
                        ) {}
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(20.dp))

            // 3. MACROS GRID
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MacroItem("PRO", p, Color(0xFFFF4081))
                Spacer(modifier = GlanceModifier.width(16.dp))
                MacroItem("CARB", c, Color(0xFF4CAF50))
                Spacer(modifier = GlanceModifier.width(16.dp))
                MacroItem("FAT", f, Color(0xFFFFC107))
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // 4. WATER BUTTON
            Button(
                text = "💧 Log Water ($water ml)",
                onClick = actionRunCallback<UpdateWaterCallback>(),
                modifier = GlanceModifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = ColorProvider(Color(0xFF1A1A1A)),
                    contentColor = ColorProvider(Color.White)
                )
            )
        }
    }

    @Composable
    private fun MacroItem(label: String, value: Int, color: Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(color), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${value}g",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp)
            )
        }
    }
}

class UpdateWaterCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        try {
            // 1. Get the current count from the widget's local state
            val prefs = getAppWidgetState<Preferences>(context, PreferencesGlanceStateDefinition, glanceId)
            val currentWater = prefs[VoltWidget.WATER_KEY] ?: 0
            val newWater = currentWater + 250 // Increment by a glass

            // 2. Update the Widget UI immediately for that "instant" feel
            VoltWidget.updateData(context,
                remainingKcal = prefs[VoltWidget.CALORIES_KEY] ?: 0,
                w = newWater,
                p = prefs[VoltWidget.PROTEIN_KEY] ?: 0,
                c = prefs[VoltWidget.CARBS_KEY] ?: 0,
                f = prefs[VoltWidget.FAT_KEY] ?: 0
            )

            // 3. Sync to Supabase in the background
            // Note: In a real app, you'd pull the user ID from your Auth session
            /*
            supabase.from("profiles").update(
                mapOf("daily_water_ml" to newWater)
            ) { filter { eq("id", userId) } }
            */

            android.util.Log.d("VoltWidget", "Hydration synced: ${newWater}ml")

        } catch (e: Exception) {
            android.util.Log.e("VoltWidget", "Water sync failed: ${e.message}")
        }
    }
}