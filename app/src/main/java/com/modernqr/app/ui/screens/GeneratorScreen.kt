package com.modernqr.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.modernqr.app.domain.qr.QrPayload
import com.modernqr.app.domain.qr.QrPayloadEncoder
import com.modernqr.app.domain.qr.QrPayloadParser
import com.modernqr.app.domain.qr.QrPayloadValidator
import com.modernqr.app.domain.qr.WifiSecurity
import com.modernqr.app.viewmodel.MainViewModel
import java.io.File
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedType by remember { mutableStateOf("Smart") }
    val types = listOf("Smart", "Text", "URL", "WiFi", "Location", "Contact", "Email", "Phone", "SMS", "Contact Card")

    // Input states
    var smartInput by remember { mutableStateOf("") }
    var textInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }

    // WiFi
    var ssidInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var wifiType by remember { mutableStateOf("WPA2") }
    var expandedWifiType by remember { mutableStateOf(false) }
    val wifiTypes = listOf("WPA2", "WPA3", "WEP", "nopass")

    // Location
    var latInput by remember { mutableStateOf("") }
    var lngInput by remember { mutableStateOf("") }

    // Contact
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }

    // Email
    var emailOnlyInput by remember { mutableStateOf("") }
    var emailSubjectInput by remember { mutableStateOf("") }
    var emailBodyInput by remember { mutableStateOf("") }

    // Phone
    var phoneOnlyInput by remember { mutableStateOf("") }

    // SMS
    var smsPhoneInput by remember { mutableStateOf("") }
    var smsMessageInput by remember { mutableStateOf("") }

    // All-in-One
    var allName by remember { mutableStateOf("") }
    var allPhone by remember { mutableStateOf("") }
    var allEmail by remember { mutableStateOf("") }
    var allUrl by remember { mutableStateOf("") }
    var allWifiSsid by remember { mutableStateOf("") }
    var allWifiPass by remember { mutableStateOf("") }
    var allLocation by remember { mutableStateOf("") }
    var allNotes by remember { mutableStateOf("") }

    // QR Color customization
    var qrForegroundColor by remember { mutableStateOf(Color.Black) }
    var qrBackgroundColor by remember { mutableStateOf(Color.White) }
    val foregroundPresets = listOf(
        Color.Black, Color(0xFF1A237E), Color(0xFF006494), Color(0xFF1B5E20),
        Color(0xFF4A148C), Color(0xFFBF360C), Color(0xFF37474F)
    )
    val backgroundPresets = listOf(
        Color.White, Color(0xFFF3E5F5), Color(0xFFE3F2FD), Color(0xFFE8F5E9),
        Color(0xFFFFF8E1), Color(0xFFFCE4EC)
    )

    var errorMessage by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        if (errorMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(errorMessage)
            errorMessage = ""
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Type tabs
            ScrollableTabRow(
                selectedTabIndex = types.indexOf(selectedType),
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                types.forEach { type ->
                    Tab(
                        selected = selectedType == type,
                        onClick = { selectedType = type; qrBitmap = null },
                        text = { Text(type) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Input fields per type
            when (selectedType) {
                "Smart" -> {
                    val suggestedPayload = remember(smartInput) { QrPayloadParser.parse(smartInput) }
                    OutlinedTextField(
                        value = smartInput,
                        onValueChange = { smartInput = it },
                        label = { Text("Enter data (smart detection will suggest type)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (smartInput.isNotBlank()) {
                        AssistChip(
                            onClick = { /* no action needed, just indication */ },
                            label = { Text("Detected: ${suggestedPayload.type.displayName}") }
                        )
                    }
                }
                "Text" -> OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Enter text") },
                    modifier = Modifier.fillMaxWidth()
                )
                "URL" -> OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Enter URL (e.g., https://...)") },
                    modifier = Modifier.fillMaxWidth()
                )
                "WiFi" -> {
                    OutlinedTextField(value = ssidInput, onValueChange = { ssidInput = it }, label = { Text("Network Name (SSID)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = expandedWifiType, onExpandedChange = { expandedWifiType = it }) {
                        OutlinedTextField(
                            value = wifiType, onValueChange = {}, readOnly = true,
                            label = { Text("Security Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWifiType) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedWifiType, onDismissRequest = { expandedWifiType = false }) {
                            wifiTypes.forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = { wifiType = type; expandedWifiType = false })
                            }
                        }
                    }
                }
                "Location" -> {
                    OutlinedTextField(value = latInput, onValueChange = { latInput = it }, label = { Text("Latitude") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = lngInput, onValueChange = { lngInput = it }, label = { Text("Longitude") }, modifier = Modifier.fillMaxWidth())
                }
                "Contact" -> {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = emailInput, onValueChange = { emailInput = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                }
                "Email" -> {
                    OutlinedTextField(value = emailOnlyInput, onValueChange = { emailOnlyInput = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = emailSubjectInput, onValueChange = { emailSubjectInput = it }, label = { Text("Subject (Optional)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = emailBodyInput, onValueChange = { emailBodyInput = it }, label = { Text("Message (Optional)") }, modifier = Modifier.fillMaxWidth())
                }
                "Phone" -> OutlinedTextField(
                    value = phoneOnlyInput, onValueChange = { phoneOnlyInput = it },
                    label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth()
                )
                "SMS" -> {
                    OutlinedTextField(value = smsPhoneInput, onValueChange = { smsPhoneInput = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = smsMessageInput, onValueChange = { smsMessageInput = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth())
                }
                "Contact Card" -> {
                    Text(
                        "Creates a vCard contact. Wi-Fi, location, and notes are stored in its notes field.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = allName, onValueChange = { allName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = allPhone, onValueChange = { allPhone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = allEmail, onValueChange = { allEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = allUrl, onValueChange = { allUrl = it }, label = { Text("Website URL") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = allWifiSsid, onValueChange = { allWifiSsid = it }, label = { Text("WiFi Network Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = allWifiPass, onValueChange = { allWifiPass = it }, label = { Text("WiFi Password") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = allLocation, onValueChange = { allLocation = it }, label = { Text("Location Address/Coordinates") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = allNotes, onValueChange = { allNotes = it }, label = { Text("Other Notes") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color customization section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("QR Colors", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Foreground", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(foregroundPresets) { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (qrForegroundColor == color) 3.dp else 1.dp,
                                        color = if (qrForegroundColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { qrForegroundColor = color; qrBitmap = null }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Background", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(backgroundPresets) { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (qrBackgroundColor == color) 3.dp else 1.dp,
                                        color = if (qrBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { qrBackgroundColor = color; qrBitmap = null }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val payload = when (selectedType) {
                        "Smart" -> QrPayloadParser.parse(smartInput)
                        "Text" -> QrPayload.PlainText(textInput)
                        "URL" -> QrPayload.Url(urlInput)
                        "WiFi" -> QrPayload.Wifi(
                            ssid = ssidInput,
                            password = passwordInput,
                            security = WifiSecurity.fromWireValue(wifiType) ?: WifiSecurity.WPA2
                        )
                        "Location" -> QrPayload.Geo(
                            latitude = latInput.toDoubleOrNull() ?: Double.NaN,
                            longitude = lngInput.toDoubleOrNull() ?: Double.NaN
                        )
                        "Contact" -> QrPayload.VCard(nameInput, phoneInput, emailInput)
                        "Email" -> QrPayload.Email(emailOnlyInput, emailSubjectInput, emailBodyInput)
                        "Phone" -> QrPayload.Telephone(phoneOnlyInput)
                        "SMS" -> QrPayload.Sms(smsPhoneInput, smsMessageInput)
                        "Contact Card" -> QrPayload.VCard(
                            name = allName,
                            phone = allPhone,
                            email = allEmail,
                            url = allUrl,
                            note = buildList {
                                if (allWifiSsid.isNotEmpty()) add("Wi-Fi: $allWifiSsid / $allWifiPass")
                                if (allLocation.isNotEmpty()) add("Location: $allLocation")
                                if (allNotes.isNotEmpty()) add(allNotes)
                            }.joinToString("\n")
                        )
                        else -> QrPayload.PlainText("")
                    }

                    val validation = QrPayloadValidator.validate(payload)
                    if (!validation.isValid) {
                        errorMessage = validation.message ?: "Check the entered details."
                        return@Button
                    }

                    try {
                        val contentToEncode = QrPayloadEncoder.encode(payload)
                        val writer = MultiFormatWriter()
                        val matrix = writer.encode(contentToEncode, BarcodeFormat.QR_CODE, 512, 512)
                        val encoder = BarcodeEncoder()
                        val rawBitmap = encoder.createBitmap(matrix)

                        // Apply custom colors
                        val fgColor = qrForegroundColor.toArgb()
                        val bgColor = qrBackgroundColor.toArgb()
                        val coloredBitmap = Bitmap.createBitmap(rawBitmap.width, rawBitmap.height, Bitmap.Config.ARGB_8888)
                        for (x in 0 until rawBitmap.width) {
                            for (y in 0 until rawBitmap.height) {
                                val pixel = rawBitmap.getPixel(x, y)
                                coloredBitmap.setPixel(x, y, if (pixel == android.graphics.Color.BLACK) fgColor else bgColor)
                            }
                        }
                        qrBitmap = coloredBitmap
                        viewModel.addHistoryItem(contentToEncode, "QR_CODE", "Generated")
                    } catch (e: Exception) {
                        errorMessage = "Failed to generate QR code: ${e.message}"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Generate QR Code", modifier = Modifier.padding(vertical = 4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            qrBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier.size(260.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Download + Share row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { saveBitmapToGallery(context, bitmap) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("⬇ Download") }

                    Button(
                        onClick = { shareQrBitmap(context, bitmap) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("↗ Share") }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun shareQrBitmap(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "qr_share")
        cachePath.mkdirs()
        val file = File(cachePath, "qr_code.png")
        val fos = file.outputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share QR: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val filename = "QR_${System.currentTimeMillis()}.png"
    var fos: OutputStream? = null
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val imageUri = context.contentResolver.insert(uri, contentValues)
            if (imageUri != null) {
                fos = context.contentResolver.openOutputStream(imageUri)
                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                    fos.close()
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(imageUri, contentValues, null, null)
                    Toast.makeText(context, "QR Code saved to Downloads!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to save QR Code.", Toast.LENGTH_LONG).show()
            }
        } else {
            val directory = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, filename)
            fos = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            Toast.makeText(context, "QR Code saved to Downloads!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
        try { fos?.close() } catch (_: Exception) {}
    }
}
