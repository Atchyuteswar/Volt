package com.kazexyt.volt

import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.util.concurrent.Executors

import com.kazexyt.volt.model.MealLog

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun BarcodeScannerScreen(
    onClose: () -> Unit,
    onBarcodeScanned: (MealLog) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Ktor Client for Open Food Facts API
    val httpClient = remember { HttpClient(Android) }
    val jsonParser = remember { Json { ignoreUnknownKeys = true } }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var hasScanned by remember { mutableStateOf(false) }
    var isFetchingData by remember { mutableStateOf(false) } // Shows the loading spinner!

    // HUD Animations
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_anim")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "laser"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
                    val scanner = BarcodeScanning.getClient(options)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (hasScanned) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty() && !hasScanned) {
                                        val rawValue = barcodes.first().rawValue
                                        if (rawValue != null) {
                                            hasScanned = true
                                            isFetchingData = true

                                            // --- REAL DATABASE LOOKUP ---
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    val url = "https://world.openfoodfacts.org/api/v2/product/$rawValue.json"
                                                    val response = httpClient.get(url)
                                                    val jsonText = response.bodyAsText()
                                                    val rootObject = jsonParser.parseToJsonElement(jsonText).jsonObject

                                                    if (rootObject["status"]?.jsonPrimitive?.int == 1) {
                                                        val product = rootObject["product"]?.jsonObject
                                                        val nutriments = product?.get("nutriments")?.jsonObject

                                                        val name = product?.get("product_name")?.jsonPrimitive?.contentOrNull ?: "Unknown Beverage"

                                                        // --- ENHANCED ENERGY DETECTION ---
                                                        val energyKcal = nutriments?.get("energy-kcal_100g")?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
                                                        val energyKj = nutriments?.get("energy-kj_100g")?.jsonPrimitive?.contentOrNull?.toFloatOrNull()
                                                        val energyGeneral = nutriments?.get("energy_100g")?.jsonPrimitive?.contentOrNull?.toFloatOrNull()

                                                        // Conversion: 1 kcal = 4.184 kJ
                                                        val finalCalories = when {
                                                            energyKcal != null -> energyKcal.toInt()
                                                            energyKj != null -> (energyKj / 4.184f).toInt()
                                                            energyGeneral != null -> (energyGeneral / 4.184f).toInt()
                                                            else -> 0
                                                        }

                                                        val realMeal = MealLog(
                                                            id = java.util.UUID.randomUUID().toString(),
                                                            food_name = name,
                                                            calories = finalCalories,
                                                            protein = nutriments?.get("proteins_100g")?.jsonPrimitive?.contentOrNull?.toFloatOrNull()?.toInt() ?: 0,
                                                            carbs = nutriments?.get("carbohydrates_100g")?.jsonPrimitive?.contentOrNull?.toFloatOrNull()?.toInt() ?: 0,
                                                            fat = nutriments?.get("fat_100g")?.jsonPrimitive?.contentOrNull?.toFloatOrNull()?.toInt() ?: 0,
                                                            created_at = java.time.OffsetDateTime.now().toString()
                                                        )

                                                        withContext(Dispatchers.Main) {
                                                            onBarcodeScanned(realMeal)
                                                        }
                                                    } else {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Product not found in database.", Toast.LENGTH_SHORT).show()
                                                            isFetchingData = false
                                                            hasScanned = false // Let them try another barcode
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        isFetchingData = false
                                                        hasScanned = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                    } catch (e: Exception) {
                        Log.e("VoltCamera", "Binding failed", e)
                    }
                }, executor)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // --- BARCODE HUD OVERLAY ---
        Box(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF00E676), CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BARCODE SCANNER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 2.sp)
                }
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Targeting Box (Hidden when fetching data)
            if (!isFetchingData) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val boxWidth = size.width * 0.7f
                    val boxHeight = size.height * 0.25f
                    val left = (size.width - boxWidth) / 2
                    val top = (size.height - boxHeight) / 2

                    val strokeWidth = 4.dp.toPx()
                    val cornerLength = 30.dp.toPx()
                    val bracketColor = Color.White

                    drawLine(bracketColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
                    drawLine(bracketColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)
                    drawLine(bracketColor, Offset(left + boxWidth, top), Offset(left + boxWidth - cornerLength, top), strokeWidth)
                    drawLine(bracketColor, Offset(left + boxWidth, top), Offset(left + boxWidth, top + cornerLength), strokeWidth)
                    drawLine(bracketColor, Offset(left, top + boxHeight), Offset(left + cornerLength, top + boxHeight), strokeWidth)
                    drawLine(bracketColor, Offset(left, top + boxHeight), Offset(left, top + boxHeight - cornerLength), strokeWidth)
                    drawLine(bracketColor, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth - cornerLength, top + boxHeight), strokeWidth)
                    drawLine(bracketColor, Offset(left + boxWidth, top + boxHeight), Offset(left + boxWidth, top + boxHeight - cornerLength), strokeWidth)

                    val currentLaserY = top + (boxHeight * laserY)
                    drawLine(color = Color(0xFFFF5252), start = Offset(left + 10f, currentLaserY), end = Offset(left + boxWidth - 10f, currentLaserY), strokeWidth = 3.dp.toPx())
                }

                Text("Align barcode within the frame", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp))
            }
        }

        // --- FETCHING DATA OVERLAY ---
        if (isFetchingData) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFEADBFF))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Fetching Macros...", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}