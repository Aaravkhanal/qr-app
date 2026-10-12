package com.ritikqrscanner.app.ui.screens

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.ritikqrscanner.app.data.SettingsManager
import com.ritikqrscanner.app.domain.qr.QrAction
import com.ritikqrscanner.app.domain.qr.QrActionResolver
import com.ritikqrscanner.app.domain.qr.QrPayloadParser
import com.ritikqrscanner.app.domain.qr.QrPayloadType
import com.ritikqrscanner.app.scanner.GalleryDecodeException
import com.ritikqrscanner.app.scanner.ScanFeedback
import com.ritikqrscanner.app.scanner.ScanSessionGate
import com.ritikqrscanner.app.scanner.decodeGalleryBarcode
import com.ritikqrscanner.app.ui.label
import com.ritikqrscanner.app.ui.launchQrAction
import com.ritikqrscanner.app.ui.PrivacyPill
import com.ritikqrscanner.app.viewmodel.MainViewModel
import com.ritikqrscanner.app.BuildConfig
import com.ritikqrscanner.app.R
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
        mutableStateOf(if (availableCameras.isEmpty()) context.getString(R.string.no_camera) else null)
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
            cameraError = context.getString(R.string.camera_start_error)
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
                galleryError = context.getString(R.string.gallery_scan_error)
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
                                            cameraError = context.getString(R.string.camera_stopped_error)
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

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PrivacyPill(label = stringResource(R.string.privacy_on_device))
                    Spacer(Modifier.height(14.dp))
                    ScannerViewfinder(scanLineY)
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.62f),
                        shape = CircleShape,
                        contentColor = Color.White
                    ) {
                        Text(
                            stringResource(R.string.scanner_aim_hint),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Controls Overlay
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    ScannerActionButton(
                        icon = Icons.Default.PhotoLibrary,
                        label = if (isGalleryDecoding) stringResource(R.string.scanning) else stringResource(R.string.gallery),
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
                        label = stringResource(if (isTorchOn) R.string.torch_on else R.string.torch_off),
                        enabled = scannedResult == null && !isGalleryDecoding && availableCameras.isNotEmpty(),
                        onClick = {
                            try {
                                isTorchOn = !isTorchOn
                                if (isTorchOn) barcodeView?.setTorchOn() else barcodeView?.setTorchOff()
                            } catch (error: RuntimeException) {
                                isTorchOn = false
                                logScannerFailure("Unable to change torch", error)
                                cameraError = context.getString(R.string.flashlight_error)
                            }
                        }
                    )
                    if (shouldShowCameraSwitch(availableCameras.size)) {
                        ScannerActionButton(
                            icon = Icons.Default.Cameraswitch,
                            label = stringResource(R.string.switch_camera),
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
                                }) { Text(stringResource(R.string.try_camera_again)) }
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
private fun ScannerViewfinder(scanLineY: Float) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth(0.72f)
            .sizeIn(maxWidth = 280.dp)
            .aspectRatio(1f)
            .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 3.dp.toPx()
            val corner = 42.dp.toPx()
            val stroke = 5.dp.toPx()
            val right = size.width - inset
            val bottom = size.height - inset

            drawLine(primary, start = androidx.compose.ui.geometry.Offset(inset, corner), end = androidx.compose.ui.geometry.Offset(inset, inset), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(primary, start = androidx.compose.ui.geometry.Offset(inset, inset), end = androidx.compose.ui.geometry.Offset(corner, inset), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(primary, start = androidx.compose.ui.geometry.Offset(right - corner, inset), end = androidx.compose.ui.geometry.Offset(right, inset), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(primary, start = androidx.compose.ui.geometry.Offset(right, inset), end = androidx.compose.ui.geometry.Offset(right, corner), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(primary, start = androidx.compose.ui.geometry.Offset(inset, bottom - corner), end = androidx.compose.ui.geometry.Offset(inset, bottom), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(primary, start = androidx.compose.ui.geometry.Offset(inset, bottom), end = androidx.compose.ui.geometry.Offset(corner, bottom), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(primary, start = androidx.compose.ui.geometry.Offset(right - corner, bottom), end = androidx.compose.ui.geometry.Offset(right, bottom), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(primary, start = androidx.compose.ui.geometry.Offset(right, bottom), end = androidx.compose.ui.geometry.Offset(right, bottom - corner), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(2.dp)
                .offset(y = (18 + scanLineY * 242).dp)
                .background(primary.copy(alpha = 0.92f), CircleShape)
        )
    }
}

@Composable
internal fun CameraPermissionContent(
    permanentlyDenied: Boolean,
    onTryAgain: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = stringResource(R.string.camera_permission_icon_description),
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.camera_permission_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (permanentlyDenied) {
                    stringResource(R.string.camera_permission_denied)
                } else {
                    stringResource(R.string.camera_permission_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.try_again)) }
                        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_settings)) }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onTryAgain, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.try_again)) }
                        Button(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.open_settings)) }
                    }
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
    var resultMessage by rememberSaveable(result) { mutableStateOf<String?>(null) }

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
                Text(stringResource(R.string.scanned_result), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dismiss_result), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Text(
                    stringResource(R.string.result_format, payloadTypeLabel(payload.type), barcodeFormat),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (payload.type in setOf(
                    QrPayloadType.WIFI,
                    QrPayloadType.VCARD,
                    QrPayloadType.EMAIL,
                    QrPayloadType.TELEPHONE,
                    QrPayloadType.SMS
                )
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        stringResource(R.string.sensitive_result_not_saved),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(result, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(14.dp))

            // Copy + Share row
            val largeText = LocalDensity.current.fontScale > 1.3f
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val vertical = maxWidth < 320.dp || largeText
                val buttonContent: @Composable (Modifier) -> Unit = { buttonModifier ->
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(result))
                            android.widget.Toast.makeText(context, context.getString(R.string.copied), android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = buttonModifier
                    ) { Text(stringResource(R.string.copy)) }

                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                this.action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, result)
                                type = "text/plain"
                            }
                            if (sendIntent.resolveActivity(context.packageManager) == null) {
                                resultMessage = context.getString(R.string.share_text_unavailable)
                            } else {
                                runCatching {
                                    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_scanned_result)))
                                }.onFailure {
                                    resultMessage = context.getString(R.string.action_open_failed)
                                }
                            }
                        },
                        modifier = buttonModifier
                    ) { Text(stringResource(R.string.share)) }
                }
                if (vertical) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { buttonContent(Modifier.fillMaxWidth()) }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { buttonContent(Modifier.weight(1f)) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            resultMessage?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { resultMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dismiss))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (action != QrAction.None) {
                Button(
                    onClick = { resultMessage = launchQrAction(context, action) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(action.label(context)) }
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
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) { onClick() }
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
                contentDescription = null,
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
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label) { onClick() }
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
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

private data class CameraOption(val id: Int, val isFrontFacing: Boolean)

internal fun shouldShowCameraSwitch(cameraCount: Int): Boolean = cameraCount > 1

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
