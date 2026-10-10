package com.modernqr.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.provider.Settings
import android.util.Log
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.journeyapps.barcodescanner.CameraPreview
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.camera.CameraSettings
import com.modernqr.app.data.SettingsManager
import com.modernqr.app.domain.qr.QrAction
import com.modernqr.app.domain.qr.QrActionResolver
import com.modernqr.app.domain.qr.QrPayloadParser
import com.modernqr.app.scanner.GalleryDecodeException
import com.modernqr.app.scanner.ScanFeedback
import com.modernqr.app.scanner.ScanSessionGate
import com.modernqr.app.scanner.decodeGalleryBarcode
import com.modernqr.app.ui.label
import com.modernqr.app.ui.launchQrAction
import com.modernqr.app.viewmodel.MainViewModel
import com.ritikqrscanner.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ScannerScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    val hapticEnabled by settingsManager.hapticFeedbackFlow.collectAsState(initial = true)
    val soundEnabled by settingsManager.scanSoundFlow.collectAsState(initial = true)
    val currentHapticEnabled by rememberUpdatedState(hapticEnabled)
    val currentSoundEnabled by rememberUpdatedState(soundEnabled)
    val feedback = remember { ScanFeedback(context) }
    val availableCameras = remember { discoverCameras() }

    var hasCameraPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var permissionPermanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var scannedResult by rememberSaveable { mutableStateOf<String?>(null) }
    var scannedFormat by rememberSaveable { mutableStateOf("QR_CODE") }
    val sessionGate = remember { ScanSessionGate(initiallyLocked = scannedResult != null) }
    var barcodeView by remember { mutableStateOf<DecoratedBarcodeView?>(null) }
    var lifecycleResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var selectedCameraId by rememberSaveable {
        mutableIntStateOf(availableCameras.firstOrNull { !it.isFrontFacing }?.id ?: availableCameras.firstOrNull()?.id ?: -1)
    }
    var isTorchOn by rememberSaveable { mutableStateOf(false) }
    var cameraRetryKey by remember { mutableIntStateOf(0) }
    var cameraError by rememberSaveable {
        mutableStateOf(if (availableCameras.isEmpty()) "No compatible camera was found on this device." else null)
    }
    var galleryError by rememberSaveable { mutableStateOf<String?>(null) }
    var isGalleryDecoding by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            permissionPermanentlyDenied = !granted && activity?.let {
                !ActivityCompat.shouldShowRequestPermissionRationale(it, android.Manifest.permission.CAMERA)
            } == true
        }
    )

    fun requestCameraPermission() {
        permissionRequested = true
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    fun acceptResult(value: String, format: String) {
        if (!sessionGate.tryAccept()) return
        barcodeView?.pause()
        scannedResult = value
        scannedFormat = format
        galleryError = null
        viewModel.addHistoryItem(value, format, "Scanned")
        feedback.confirm(currentSoundEnabled, currentHapticEnabled)
    }

    fun armCamera() {
        val view = barcodeView ?: return
        if (!hasCameraPermission || !lifecycleResumed || scannedResult != null || isGalleryDecoding || selectedCameraId < 0) return
        try {
            view.pause()
            view.cameraSettings = CameraSettings().apply { requestedCameraId = selectedCameraId }
            view.resume()
            view.decodeSingle { result -> acceptResult(result.text, result.barcodeFormat.name) }
        } catch (error: RuntimeException) {
            logScannerFailure("Unable to start camera", error)
            cameraError = "The camera could not be started. Close other camera apps and try again."
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            isGalleryDecoding = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                val result = decodeGalleryBarcode(context, uri)
                acceptResult(result.text, result.barcodeFormat.name)
            } catch (error: CancellationException) {
                throw error
            } catch (error: GalleryDecodeException) {
                galleryError = error.message
            } catch (error: Exception) {
                logScannerFailure("Unable to decode gallery image", error)
                galleryError = "The selected image could not be scanned."
            } finally {
                isGalleryDecoding = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionRequested) requestCameraPermission()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    lifecycleResumed = true
                    hasCameraPermission = context.hasCameraPermission()
                    if (hasCameraPermission) permissionPermanentlyDenied = false
                }
                Lifecycle.Event.ON_PAUSE -> {
                    lifecycleResumed = false
                    barcodeView?.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(
        barcodeView,
        hasCameraPermission,
        lifecycleResumed,
        scannedResult,
        isGalleryDecoding,
        selectedCameraId,
        cameraRetryKey
    ) {
        if (scannedResult == null && !isGalleryDecoding) armCamera() else barcodeView?.pause()
    }

    DisposableEffect(Unit) {
        onDispose {
            barcodeView?.pause()
            feedback.close()
        }
    }

    if (hasCameraPermission) {

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

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (availableCameras.isNotEmpty()) {
                    AndroidView(
                        factory = { ctx ->
                            DecoratedBarcodeView(ctx).apply {
                                cameraSettings = CameraSettings().apply { requestedCameraId = selectedCameraId }
                                getBarcodeView().addStateListener(object : CameraPreview.StateListener {
                                    override fun previewSized() = Unit
                                    override fun previewStarted() {
                                        post {
                                            cameraError = null
                                            if (isTorchOn) runCatching { setTorchOn() }
                                                .onFailure { logScannerFailure("Unable to restore torch", it) }
                                        }
                                    }
                                    override fun previewStopped() = Unit
                                    override fun cameraError(error: Exception) {
                                        post {
                                            logScannerFailure("Camera preview failed", error)
                                            cameraError = "The camera stopped unexpectedly. You can try starting it again."
                                        }
                                    }
                                    override fun cameraClosed() = Unit
                                })
                                barcodeView = this
                            }
                        },
                        update = { barcodeView = it },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color.Black))
                }

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
                        label = if (isGalleryDecoding) "Scanning…" else "Gallery",
                        enabled = scannedResult == null && !isGalleryDecoding,
                        onClick = {
                            galleryError = null
                            isGalleryDecoding = true
                            barcodeView?.pause()
                            galleryLauncher.launch("image/*")
                        }
                    )
                    ScannerActionButton(
                        painter = androidx.compose.ui.res.painterResource(
                            id = if (isTorchOn) com.ritikqrscanner.app.R.drawable.flashlight_on_24
                            else com.ritikqrscanner.app.R.drawable.flashlight_off_24
                        ),
                        label = "Torch",
                        enabled = scannedResult == null && !isGalleryDecoding && availableCameras.isNotEmpty(),
                        onClick = {
                            try {
                                isTorchOn = !isTorchOn
                                if (isTorchOn) barcodeView?.setTorchOn() else barcodeView?.setTorchOff()
                            } catch (error: RuntimeException) {
                                isTorchOn = false
                                logScannerFailure("Unable to change torch", error)
                                cameraError = "The flashlight is not available for this camera."
                            }
                        }
                    )
                    if (availableCameras.size > 1) {
                        ScannerActionButton(
                            icon = Icons.Default.Refresh,
                            label = "Switch",
                            enabled = scannedResult == null && !isGalleryDecoding,
                            onClick = {
                                barcodeView?.pause()
                                isTorchOn = false
                                val currentIndex = availableCameras.indexOfFirst { it.id == selectedCameraId }
                                selectedCameraId = availableCameras[(currentIndex + 1).mod(availableCameras.size)].id
                                cameraError = null
                            }
                        )
                    }
                }

                if (isGalleryDecoding) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                (cameraError ?: galleryError)?.let { message ->
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                            if (cameraError != null && availableCameras.isNotEmpty()) {
                                TextButton(onClick = {
                                    cameraError = null
                                    cameraRetryKey++
                                }) { Text("Try camera again") }
                            }
                        }
                    }
                }
            }

            scannedResult?.let { result ->
                ScannedResultCard(
                    result = result,
                    barcodeFormat = scannedFormat,
                    onDismiss = {
                        sessionGate.dismiss()
                        scannedResult = null
                        cameraError = null
                    }
                )
            }
        }
    } else {
        CameraPermissionContent(
            permanentlyDenied = permissionPermanentlyDenied,
            onTryAgain = ::requestCameraPermission,
            onOpenSettings = { context.openAppSettings() }
        )
    }
}

@Composable
private fun CameraPermissionContent(
    permanentlyDenied: Boolean,
    onTryAgain: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📷", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Camera Permission Required", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (permanentlyDenied) {
                    "Camera access has been permanently denied. Enable it in system settings to scan with the camera."
                } else {
                    "Camera access is needed to scan QR codes and barcodes."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTryAgain) { Text("Try again") }
                Button(onClick = onOpenSettings) { Text("Open Settings") }
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
                            this.action = Intent.ACTION_SEND
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

@Composable
fun ScannerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .then(if (enabled) Modifier else Modifier.background(Color.Black.copy(alpha = 0.25f)))
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
fun ScannerActionButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .then(if (enabled) Modifier else Modifier.background(Color.Black.copy(alpha = 0.25f)))
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

private data class CameraOption(val id: Int, val isFrontFacing: Boolean)

@Suppress("DEPRECATION")
private fun discoverCameras(): List<CameraOption> = try {
    (0 until Camera.getNumberOfCameras()).mapNotNull { cameraId ->
        try {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(cameraId, info)
            CameraOption(cameraId, info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        } catch (error: RuntimeException) {
            logScannerFailure("Unable to inspect camera $cameraId", error)
            null
        }
    }
} catch (error: RuntimeException) {
    logScannerFailure("Unable to discover cameras", error)
    emptyList()
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    })
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun logScannerFailure(message: String, error: Throwable) {
    if (BuildConfig.DEBUG) Log.w("ScannerScreen", message, error)
}
