package com.kazexyt.volt.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val icon: ImageVector? = null,
    val label: String = ""
) {
    // --- MAIN NAVIGATION (Shown in Bottom Bar) ---
    object Dashboard : Screen(
        route = "dashboard",
        icon = Icons.Default.GridView,
        label = "Home"
    )

    object Analytics : Screen(
        route = "analytics",
        icon = Icons.Default.PieChart,
        label = "Stats"
    )

    object Profile : Screen(
        route = "profile",
        icon = Icons.Default.Person,
        label = "You"
    )

    // --- SECONDARY SCREENS (Accessible via buttons) ---
    object Coach : Screen(
        route = "coach",
        icon = Icons.Default.AutoAwesome,
        label = "Aira"
    )

    object Settings : Screen(
        route = "settings",
        icon = Icons.Default.Settings,
        label = "Settings"
    )

    object Evolution : Screen(
        route = "evolution",
        icon = Icons.AutoMirrored.Filled.ShowChart,
        label = "Evolution"
    )

    // --- UTILITY & AUTH SCREENS (No Icons Needed) ---
    object Auth : Screen("auth")
    object Lens : Screen("lens")
    object Barcode : Screen("barcode")
    object Onboarding : Screen("onboarding")
    object Paywall : Screen("paywall")
    object Voice : Screen("voice")
}