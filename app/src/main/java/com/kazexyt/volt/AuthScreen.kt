package com.kazexyt.volt

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kazexyt.volt.ui.theme.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    isLoading: Boolean,
    onBack: () -> Unit,
    onLoginClick: (String, String) -> Unit,
    onSignUpClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    initialIsLogin: Boolean = true
) {
    var isLoginMode by remember { mutableStateOf(initialIsLogin) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 📍 1. New State for Confirm Password
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // 📍 2. Validation Logic
    val passwordsMatch = isLoginMode || (password == confirmPassword && password.isNotBlank())
    val isFormValid = email.isNotBlank() && password.length >= 6 && passwordsMatch && !isLoading

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoltBlack)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        IconButton(
            onClick = onBack,
            modifier = Modifier.background(Color.White.copy(0.05f), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }

        Spacer(modifier = Modifier.height(48.dp))

        AnimatedContent(targetState = isLoginMode, label = "header") { isLogin ->
            Column {
                Text(
                    text = if (isLogin) "Welcome\nBack." else "Start Your\nEvolution.",
                    color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black, lineHeight = 44.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isLogin) "Sign in to sync your macros and hydration." else "Create an account to activate Aira.",
                    color = Color.Gray, fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF161616),
                unfocusedContainerColor = VoltSurface,
                focusedIndicatorColor = VoltCyan,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(image, "Toggle Password", tint = Color.Gray)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF161616),
                unfocusedContainerColor = VoltSurface,
                focusedIndicatorColor = VoltCyan,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // 📍 3. Animated Confirm Password Field
        AnimatedVisibility(
            visible = !isLoginMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(image, "Toggle Password", tint = Color.Gray)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !passwordsMatch && confirmPassword.isNotEmpty(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF161616),
                        unfocusedContainerColor = VoltSurface,
                        focusedIndicatorColor = VoltCyan,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        errorIndicatorColor = VoltError
                    )
                )
                if (!passwordsMatch && confirmPassword.isNotEmpty()) {
                    Text(
                        text = "Passwords do not match",
                        color = VoltError,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Submit Button
        Button(
            onClick = { if (isLoginMode) onLoginClick(email, password) else onSignUpClick(email, password) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            enabled = isFormValid, // 📍 4. Tied to refined validation
            colors = ButtonDefaults.buttonColors(
                containerColor = VoltCyan,
                contentColor = Color.Black,
                disabledContainerColor = Color.DarkGray
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text(if (isLoginMode) "Sign In" else "Create Account", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isLoginMode) "Don't have an account? " else "Already have an account? ", color = Color.Gray)
            Text(
                text = if (isLoginMode) "Sign Up" else "Sign In",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    isLoginMode = !isLoginMode
                    // Reset confirm fields when toggling
                    confirmPassword = ""
                    confirmPasswordVisible = false
                }.padding(8.dp)
            )
        }
    }
}