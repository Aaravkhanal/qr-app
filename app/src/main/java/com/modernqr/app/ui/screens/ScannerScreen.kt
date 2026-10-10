package com.modernqr.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.camera.CameraSettings
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import androidx.lifecycle.viewmodel.compose.viewModel
import com.modernqr.app.data.SettingsManager
import com.modernqr.app.domain.qr.QrAction
import com.modernqr.app.domain.qr.QrActionResolver
import com.modernqr.app.domain.qr.QrPayloadParser
import com.modernqr.app.ui.label
import com.modernqr.app.ui.launchQrAction
import com.modernqr.app.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun ScannerScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val hapticEnabled by settingsManager.hapticFeedbackFlow.collectAsState(initial = true)
    val soundEnabled by settingsManager.scanSoundFlow.collectAsState(initial = true)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        var barcodeView: DecoratedBarcodeView? by remember { mutableStateOf(null) }
        var scannedResult by remember { mutableStateOf("") }
        var scannedFormat by remember { mutableStateOf("QR_CODE") }
        var isTorchOn by remember { mutableStateOf(false) }
        var isFrontCamera by remember { mutableStateOf(false) }
        var lastScanTime by remember { mutableStateOf(0L) }

        // Animated scan line
        val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
        val scanLineY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scan_line_y"
        )

        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val intArray = IntArray(bitmap.width * bitmap.height)
                    bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                    val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                    val result = MultiFormatReader().decode(binaryBitmap)
                    val now = System.currentTimeMillis()
                    if (result.text != scannedResult || now - lastScanTime > 2000) {
                        scannedResult = result.text
                        scannedFormat = result.barcodeFormat.name
                        lastScanTime = now
                        viewModel.addHistoryItem(result.text, result.barcodeFormat.name, "Scanned")
                        if (hapticEnabled) triggerHaptic(context)
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Could not decode QR code from image", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        DecoratedBarcodeView(ctx).apply {
                            barcodeView = this
                            val settings = CameraSettings()
                            settings.requestedCameraId = if (isFrontCamera) 1 else 0
                            this.cameraSettings = settings
                            this.decodeContinuous { result ->
                                val now = System.currentTimeMillis()
                                if (result.text != scannedResult || now - lastScanTime > 2000) {
                                    scannedResult = result.text
                                    scannedFormat = result.barcodeFormat.name
                                    lastScanTime = now
                                    viewModel.addHistoryItem(result.text, result.barcodeFormat.name, "Scanned")
                                    if (hapticEnabled) triggerHaptic(ctx)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Animated scan line overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 48.dp, vertical = 80.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .align(Alignment.TopStart)
                            .offset(y = (scanLineY * 300).dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }

                // Controls Overlay
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    ScannerActionButton(
                        icon = Icons.Default.Add,
                        label = "Gallery",
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                    ScannerActionButton(
                        painter = androidx.compose.ui.res.painterResource(
                            id = if (isTorchOn) com.ritikqrscanner.app.R.drawable.flashlight_on_24
                            else com.ritikqrscanner.app.R.drawable.flashlight_off_24
                        ),
                        label = "Torch",
                        onClick = {
                            isTorchOn = !isTorchOn
                            if (isTorchOn) barcodeView?.setTorchOn() else barcodeView?.setTorchOff()
                        }
                    )
                    ScannerActionButton(
                        icon = Icons.Default.Refresh,
                        label = "Switch",
                        onClick = {
                            isFrontCamera = !isFrontCamera
                            barcodeView?.pause()
                            val settings = CameraSettings()
                            settings.requestedCameraId = if (isFrontCamera) 1 else 0
                            barcodeView?.cameraSettings = settings
                            barcodeView?.resume()
                        }
                    )
                }
            }

            if (scannedResult.isNotEmpty()) {
                ScannedResultCard(
                    result = scannedResult,
                    barcodeFormat = scannedFormat,
                    onDismiss = { scannedResult = "" }
                )
            }
        }

        DisposableEffect(key1 = true) {
            barcodeView?.resume()
            onDispose { barcodeView?.pause() }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📷", style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Camera Permission Required", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Camera access is needed to scan QR codes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            }
        }
    }
}

@Composable
private fun ScannedResultCard(result: String, barcodeFormat: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val payload = remember(result, barcodeFormat) { QrPayloadParser.parse(result, barcodeFormat) }
    val action = remember(payload) { QrActionResolver.resolve(payload) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .padding(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scanned Result", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(result, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(14.dp))

            // Copy + Share row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(result))
                        android.widget.Toast.makeText(context, "Copied!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Copy") }

                OutlinedButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, result)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, null))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Share") }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (action != QrAction.None) {
                Button(
                    onClick = { launchQrAction(context, action) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(action.label()) }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun triggerHaptic(context: android.content.Context) {
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(60)
            }
        }
    } catch (_: Exception) {}
}

@Composable
fun ScannerActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = CircleShape
                )
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.5f), shape = CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

@Composable
fun ScannerActionButton(painter: androidx.compose.ui.graphics.painter.Painter, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.45f),
                    shape = CircleShape
                )
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.5f), shape = CircleShape)
        ) {
            Icon(
                painter = painter,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}
