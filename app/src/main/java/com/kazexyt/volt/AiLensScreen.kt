package com.kazexyt.volt

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kazexyt.volt.ui.theme.*
import java.io.File
import java.util.concurrent.Executors

@Composable
fun AiLensScreen(
    onClose: () -> Unit,
    onPhotoCaptured: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // CameraX States
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // HUD Animations
    val infiniteTransition = rememberInfiniteTransition(label = "hud_anim")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )
    val hudAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VoltBlack)
    ) {
        if (hasCameraPermission) {
            // --- 1. THE ACTUAL LIVE CAMERA FEED ---
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageCapture = ImageCapture.Builder().build()
                        imageCaptureUseCase = imageCapture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("VoltCamera", "Use case binding failed", exc)
                        }
                    }, executor)

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // --- 2. THE HUD OVERLAY ---
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(VoltError, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI VISION ACTIVE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Targeting Reticle & Laser
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .align(Alignment.Center)
                        .padding(32.dp)
                ) {
                    val bracketLength = 40.dp.toPx()
                    val strokeWidth = 3.dp.toPx()
                    val bracketColor = VoltLavender.copy(alpha = hudAlpha)

                    // Brackets
                    drawLine(bracketColor, Offset(0f, 0f), Offset(bracketLength, 0f), strokeWidth)
                    drawLine(bracketColor, Offset(0f, 0f), Offset(0f, bracketLength), strokeWidth)
                    drawLine(
                        bracketColor,
                        Offset(size.width, 0f),
                        Offset(size.width - bracketLength, 0f),
                        strokeWidth
                    )
                    drawLine(bracketColor, Offset(size.width, 0f), Offset(size.width, bracketLength), strokeWidth)
                    drawLine(
                        bracketColor,
                        Offset(0f, size.height),
                        Offset(bracketLength, size.height),
                        strokeWidth
                    )
                    drawLine(
                        bracketColor,
                        Offset(0f, size.height),
                        Offset(0f, size.height - bracketLength),
                        strokeWidth
                    )
                    drawLine(
                        bracketColor,
                        Offset(size.width, size.height),
                        Offset(size.width - bracketLength, size.height),
                        strokeWidth
                    )
                    drawLine(
                        bracketColor,
                        Offset(size.width, size.height),
                        Offset(size.width, size.height - bracketLength),
                        strokeWidth
                    )

                    // Laser
                    val currentLaserY = size.height * laserY
                    val laserColor = VoltPurple
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                laserColor.copy(alpha = 0.8f),
                                Color.Transparent
                            ),
                            startY = currentLaserY - 20f, endY = currentLaserY + 20f
                        ),
                        topLeft = Offset(0f, currentLaserY - 10f), size = Size(size.width, 20f)
                    )
                    drawLine(
                        laserColor,
                        Offset(0f, currentLaserY),
                        Offset(size.width, currentLaserY),
                        strokeWidth
                    )
                }

                // --- 3. THE SHUTTER BUTTON ---
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Center food in frame",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            .padding(8.dp)
                            .background(Color.White, CircleShape)
                            .clickable {
                                val photoFile =
                                    File(context.cacheDir, "volt_scan_${System.currentTimeMillis()}.jpg")
                                val outputOptions =
                                    ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                imageCaptureUseCase?.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            onPhotoCaptured(photoFile)
                                        }

                                        override fun onError(exc: ImageCaptureException) {
                                            Log.e("VoltCamera", "Photo capture failed: ${exc.message}", exc)
                                            Toast.makeText(context, "Failed to capture photo. Please try again.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                    )
                }
            }
        } else {
            // Fallback UI if they deny permission
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Camera permission is required to use this feature.",
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}
