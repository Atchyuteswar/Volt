package com.kazexyt.volt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.kazexyt.volt.model.UserProfile
import com.kazexyt.volt.ui.theme.*

@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    onNavigateBack: () -> Unit,
    onUpdateProfile: (name: String, goal: Int) -> Unit,
    onResetData: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Control States for Popups
    var showEditSheet by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoltBlack)
            .padding(top = 48.dp, bottom = 32.dp)
            .verticalScroll(scrollState)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(48.dp).background(VoltSurfaceVariant, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Settings", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- ACCOUNT SECTION ---
        Text("Account", color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        SettingsRow(
            title = "Edit Profile",
            subtitle = "Change your name or daily calorie goal",
            icon = Icons.Default.Edit,
            iconColor = VoltLavender,
            onClick = { showEditSheet = true }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- DANGER ZONE ---
        Text("Danger Zone", color = VoltError, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        SettingsRow(
            title = "Reset All Data",
            subtitle = "Wipe all your logged meals (Cannot be undone)",
            icon = Icons.Default.Warning,
            iconColor = VoltFat,
            onClick = { showResetDialog = true }
        )
        SettingsRow(
            title = "Log Out",
            subtitle = "Sign out of your Volt account",
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            iconColor = Color.Gray,
            onClick = { showLogoutDialog = true }
        )
        SettingsRow(
            title = "Delete Account",
            subtitle = "Permanently destroy your account and data",
            icon = Icons.Default.DeleteForever,
            iconColor = VoltError,
            onClick = { showDeleteDialog = true }
        )
    }

    // --- POPUPS & DIALOGS ---

    if (showEditSheet) {
        EditProfileSheet(
            currentName = userProfile?.name ?: "",
            currentGoal = userProfile?.daily_goal ?: 2200,
            onDismiss = { showEditSheet = false },
            onSave = { newName, newGoal ->                 // Restored the proper save action!
                onUpdateProfile(newName, newGoal)
                showEditSheet = false
            }
        )
    }

    if (showResetDialog) {
        VoltWarningDialog(
            title = "Reset All Data?",
            text = "This will permanently delete all your meal logs from the cloud. This action cannot be undone.",
            confirmText = "Yes, Reset Data",
            onConfirm = { onResetData(); showResetDialog = false },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showLogoutDialog) {
        VoltWarningDialog(
            title = "Log Out?",
            text = "Are you sure you want to log out? You will need to sign back in to view your data.",
            confirmText = "Log Out",
            onConfirm = { onLogout(); showLogoutDialog = false },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showDeleteDialog) {
        VoltWarningDialog(
            title = "Delete Account?",
            text = "This is a catastrophic action. Your account, profile, and all meal logs will be wiped from our servers forever.",
            confirmText = "Delete Everything",
            isDestructive = true,
            onConfirm = { onDeleteAccount(); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).background(VoltSurfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    currentName: String,
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(currentName) }
    var goal by remember { mutableStateOf(currentGoal.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VoltSurface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
            Text("Edit Profile", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = goal, onValueChange = { goal = it }, label = { Text("Daily Calorie Goal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                // THIS WAS THE CULPRIT: It is now safely locked to toIntOrNull()
                onClick = { onSave(name, goal.toIntOrNull() ?: 2200) },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoltLavender, contentColor = VoltSurface)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun VoltWarningDialog(
    title: String,
    text: String,
    confirmText: String,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VoltSurfaceVariant,
        modifier = modifier,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = { Text(text, color = Color.LightGray) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (isDestructive) VoltError else VoltLavender)
            ) { Text(confirmText, color = if (isDestructive) Color.White else VoltSurface) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}