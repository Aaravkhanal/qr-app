package com.modernqr.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.modernqr.app.viewmodel.MainViewModel

@Composable
fun ScannerScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
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
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(android.Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        var barcodeView: DecoratedBarcodeView? by remember { mutableStateOf(null) }
        var scannedResult by remember { mutableStateOf("") }
        var isTorchOn by remember { mutableStateOf(false) }
        var isFrontCamera by remember { mutableStateOf(false) }

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
                    if (result.text != scannedResult) {
                        scannedResult = result.text
                        viewModel.addHistoryItem(result.text, result.barcodeFormat.name, "Scanned")
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
                                if (scannedResult != result.text) {
                                    scannedResult = result.text
                                    viewModel.addHistoryItem(result.text, result.barcodeFormat.name, "Scanned")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Controls Overlay
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter)
                ) {
                    ScannerActionButton(
                        icon = Icons.Default.Add,
                        label = "Gallery",
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                    ScannerActionButton(
                        painter = androidx.compose.ui.res.painterResource(id = if (isTorchOn) com.ritikqrscanner.app.R.drawable.flashlight_on_24 else com.ritikqrscanner.app.R.drawable.flashlight_off_24),
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text("Scanned Content:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(scannedResult)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

                            Button(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(scannedResult))
                                    android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Copy")
                            }

                            Button(
                                onClick = {
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, scannedResult)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Share")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Smart Actions
                        if (android.util.Patterns.WEB_URL.matcher(scannedResult).matches()) {
                            Button(
                                onClick = { uriHandler.openUri(scannedResult) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Open URL")
                            }
                        } else if (scannedResult.startsWith("WIFI:")) {
                            Button(
                                onClick = {
                                    val ssidMatch = Regex("S:([^;]+)").find(scannedResult)
                                    val passMatch = Regex("P:([^;]+)").find(scannedResult)
                                    val ssid = ssidMatch?.groupValues?.get(1) ?: ""
                                    val pass = passMatch?.groupValues?.get(1) ?: ""
                                    
                                    if (ssid.isNotEmpty()) {
                                        if (android.os.Build.VERSION.SDK_INT >= 30) {
                                            val suggestion = android.net.wifi.WifiNetworkSuggestion.Builder()
                                                .setSsid(ssid)
                                                .setWpa2Passphrase(pass)
                                                .build()
                                            
                                            val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
                                                putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(suggestion))
                                            }
                                            context.startActivity(intent)
                                        } else if (android.os.Build.VERSION.SDK_INT == 29) {
                                            val specifier = android.net.wifi.WifiNetworkSpecifier.Builder()
                                                .setSsid(ssid)
                                                .setWpa2Passphrase(pass)
                                                .build()

                                            val request = android.net.NetworkRequest.Builder()
                                                .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                                                .setNetworkSpecifier(specifier)
                                                .build()

                                            val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                                            connectivityManager.requestNetwork(request, object : android.net.ConnectivityManager.NetworkCallback() {})
                                            android.widget.Toast.makeText(context, "Please approve the connection to $ssid", android.widget.Toast.LENGTH_LONG).show()
                                        } else {
                                            val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                                            val conf = android.net.wifi.WifiConfiguration()
                                            conf.SSID = "\"" + ssid + "\""
                                            conf.preSharedKey = "\"" + pass + "\""
                                            val netId = wifiManager.addNetwork(conf)
                                            wifiManager.disconnect()
                                            wifiManager.enableNetwork(netId, true)
                                            wifiManager.reconnect()
                                            android.widget.Toast.makeText(context, "Connecting to $ssid...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                        val clipboardManagerInstance = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("password", pass)
                                        clipboardManagerInstance.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Password copied. Please connect manually.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Connect to WiFi")
                            }
                        } else if (scannedResult.startsWith("MATMSG:") || scannedResult.startsWith("mailto:")) {
                            val email = if (scannedResult.startsWith("mailto:")) {
                                scannedResult.removePrefix("mailto:")
                            } else {
                                val match = Regex("TO:(.*?);").find(scannedResult)
                                match?.groupValues?.get(1) ?: ""
                            }
                            Button(
                                onClick = { 
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:$email")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Send Email to $email")
                            }
                        }
                    }
                }
            }
        }

        DisposableEffect(key1 = true) {
            barcodeView?.resume()
            onDispose {
                barcodeView?.pause()
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission is required to scan QR codes.")
        }
    }
}

@Composable
fun ScannerActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ScannerActionButton(painter: androidx.compose.ui.graphics.painter.Painter, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                )
        ) {
            Icon(
                painter = painter,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
