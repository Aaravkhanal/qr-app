package com.modernqr.app.ui.screens

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.modernqr.app.domain.qr.*
import com.modernqr.app.viewmodel.MainViewModel
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val QR_SIZE = 512
private const val SHARE_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val types = remember { listOf("Smart", "Text", "URL", "WiFi", "Location", "Contact", "Email", "Phone", "SMS", "Contact Card") }

    var selectedType by rememberSaveable { mutableStateOf("Smart") }
    var smartInput by rememberSaveable { mutableStateOf("") }
    var textInput by rememberSaveable { mutableStateOf("") }
    var urlInput by rememberSaveable { mutableStateOf("") }
    var ssidInput by rememberSaveable { mutableStateOf("") }
    var passwordInput by rememberSaveable { mutableStateOf("") }
    var wifiType by rememberSaveable { mutableStateOf("WPA2") }
    var expandedWifiType by remember { mutableStateOf(false) }
    var latInput by rememberSaveable { mutableStateOf("") }
    var lngInput by rememberSaveable { mutableStateOf("") }
    var nameInput by rememberSaveable { mutableStateOf("") }
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var emailInput by rememberSaveable { mutableStateOf("") }
    var emailOnlyInput by rememberSaveable { mutableStateOf("") }
    var emailSubjectInput by rememberSaveable { mutableStateOf("") }
    var emailBodyInput by rememberSaveable { mutableStateOf("") }
    var phoneOnlyInput by rememberSaveable { mutableStateOf("") }
    var smsPhoneInput by rememberSaveable { mutableStateOf("") }
    var smsMessageInput by rememberSaveable { mutableStateOf("") }
    var allName by rememberSaveable { mutableStateOf("") }
    var allPhone by rememberSaveable { mutableStateOf("") }
    var allEmail by rememberSaveable { mutableStateOf("") }
    var allUrl by rememberSaveable { mutableStateOf("") }
    var allWifiSsid by rememberSaveable { mutableStateOf("") }
    var allWifiPass by rememberSaveable { mutableStateOf("") }
    var allLocation by rememberSaveable { mutableStateOf("") }
    var allNotes by rememberSaveable { mutableStateOf("") }

    var foregroundArgb by rememberSaveable { mutableIntStateOf(Color.Black.toArgb()) }
    var backgroundArgb by rememberSaveable { mutableIntStateOf(Color.White.toArgb()) }
    val foregroundColor = Color(foregroundArgb)
    val backgroundColor = Color(backgroundArgb)
    val foregroundPresets = remember { listOf(Color.Black, Color(0xFF1A237E), Color(0xFF006494), Color(0xFF1B5E20), Color(0xFF4A148C), Color(0xFFBF360C), Color(0xFF37474F)) }
    val backgroundPresets = remember { listOf(Color.White, Color(0xFFF3E5F5), Color(0xFFE3F2FD), Color(0xFFE8F5E9), Color(0xFFFFF8E1), Color(0xFFFCE4EC)) }

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingLegacySave by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }

    fun notify(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val legacySaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val bitmap = pendingLegacySave
        pendingLegacySave = null
        if (uri == null || bitmap == null) return@rememberLauncherForActivityResult
        isSaving = true
        scope.launch {
            val result = saveBitmapToUri(context, bitmap, uri)
            isSaving = false
            notify(result.fold(onSuccess = { it }, onFailure = { "Could not save QR code: ${it.userMessage()}" }))
        }
    }

    val payload = when (selectedType) {
        "Smart" -> QrPayloadParser.parse(smartInput)
        "Text" -> QrPayload.PlainText(textInput)
        "URL" -> QrPayload.Url(urlInput)
        "WiFi" -> QrPayload.Wifi(ssidInput, passwordInput, WifiSecurity.fromWireValue(wifiType) ?: WifiSecurity.WPA2)
        "Location" -> QrPayload.Geo(latInput.toDoubleOrNull() ?: Double.NaN, lngInput.toDoubleOrNull() ?: Double.NaN)
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
    val encodedContent = if (validation.isValid) QrPayloadEncoder.encode(payload) else ""
    val payloadError = if (validation.isValid) QrGenerationPolicy.payloadError(encodedContent) else null
    val colorAssessment = QrGenerationPolicy.assessColors(foregroundArgb, backgroundArgb)
    val canGenerate = validation.isValid && payloadError == null && colorAssessment.isReliable && !isGenerating
    fun fieldError(vararg terms: String): String? = validation.errors.firstOrNull { error ->
        terms.any { term -> error.contains(term, ignoreCase = true) }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScrollableTabRow(selectedTabIndex = types.indexOf(selectedType), edgePadding = 8.dp, modifier = Modifier.fillMaxWidth()) {
                types.forEach { type ->
                    Tab(selected = selectedType == type, onClick = { selectedType = type; qrBitmap = null }, text = { Text(type) })
                }
            }
            Spacer(Modifier.height(16.dp))

            when (selectedType) {
                "Smart" -> {
                    GeneratorField(smartInput, { smartInput = it }, "Enter data (smart detection will suggest type)", validation.message)
                    if (smartInput.isNotBlank()) AssistChip(onClick = {}, label = { Text("Detected: ${payload.type.displayName}") })
                }
                "Text" -> GeneratorField(textInput, { textInput = it }, "Enter text", fieldError("text"))
                "URL" -> GeneratorField(urlInput, { urlInput = it }, "Enter URL (e.g., https://…)", fieldError("URL"), KeyboardType.Uri, true)
                "WiFi" -> {
                    GeneratorField(ssidInput, { ssidInput = it }, "Network Name (SSID)", fieldError("network name"), singleLine = true)
                    FieldSpacer()
                    GeneratorField(passwordInput, { passwordInput = it }, "Password", fieldError("password"), KeyboardType.Password, true)
                    FieldSpacer()
                    ExposedDropdownMenuBox(expanded = expandedWifiType, onExpandedChange = { expandedWifiType = it }) {
                        OutlinedTextField(value = wifiType, onValueChange = {}, readOnly = true, label = { Text("Security Type") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedWifiType) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expandedWifiType, onDismissRequest = { expandedWifiType = false }) {
                            listOf("WPA2", "WPA3", "WEP", "nopass").forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = { wifiType = type; expandedWifiType = false })
                            }
                        }
                    }
                }
                "Location" -> {
                    GeneratorField(latInput, { latInput = it }, "Latitude", fieldError("Latitude"), KeyboardType.Decimal, true)
                    FieldSpacer()
                    GeneratorField(lngInput, { lngInput = it }, "Longitude", fieldError("Longitude"), KeyboardType.Decimal, true)
                }
                "Contact" -> {
                    GeneratorField(nameInput, { nameInput = it }, "Name", fieldError("contact name"), singleLine = true)
                    FieldSpacer()
                    GeneratorField(phoneInput, { phoneInput = it }, "Phone", fieldError("contact phone"), KeyboardType.Phone, true)
                    FieldSpacer()
                    GeneratorField(emailInput, { emailInput = it }, "Email", fieldError("contact email"), KeyboardType.Email, true)
                }
                "Email" -> {
                    GeneratorField(emailOnlyInput, { emailOnlyInput = it }, "Email Address", fieldError("email"), KeyboardType.Email, true)
                    FieldSpacer()
                    GeneratorField(emailSubjectInput, { emailSubjectInput = it }, "Subject (Optional)")
                    FieldSpacer()
                    GeneratorField(emailBodyInput, { emailBodyInput = it }, "Message (Optional)")
                }
                "Phone" -> GeneratorField(phoneOnlyInput, { phoneOnlyInput = it }, "Phone Number", fieldError("phone"), KeyboardType.Phone, true)
                "SMS" -> {
                    GeneratorField(smsPhoneInput, { smsPhoneInput = it }, "Phone Number", fieldError("phone"), KeyboardType.Phone, true)
                    FieldSpacer()
                    GeneratorField(smsMessageInput, { smsMessageInput = it }, "Message (Optional)")
                }
                "Contact Card" -> {
                    Text("Creates a vCard contact. Wi-Fi, location, and notes are stored in its notes field.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FieldSpacer()
                    GeneratorField(allName, { allName = it }, "Name", fieldError("contact name"), singleLine = true)
                    FieldSpacer()
                    GeneratorField(allPhone, { allPhone = it }, "Phone", fieldError("contact phone"), KeyboardType.Phone, true)
                    FieldSpacer()
                    GeneratorField(allEmail, { allEmail = it }, "Email", fieldError("contact email"), KeyboardType.Email, true)
                    FieldSpacer()
                    GeneratorField(allUrl, { allUrl = it }, "Website URL", fieldError("URL"), KeyboardType.Uri, true)
                    FieldSpacer()
                    GeneratorField(allWifiSsid, { allWifiSsid = it }, "WiFi Network Name", singleLine = true)
                    FieldSpacer()
                    GeneratorField(allWifiPass, { allWifiPass = it }, "WiFi Password", keyboardType = KeyboardType.Password, singleLine = true)
                    FieldSpacer()
                    GeneratorField(allLocation, { allLocation = it }, "Location Address/Coordinates")
                    FieldSpacer()
                    GeneratorField(allNotes, { allNotes = it }, "Other Notes")
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp)) {
                    Text("QR Colors", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Text("Foreground", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    ColorChoices(foregroundPresets, foregroundColor) { foregroundArgb = it.toArgb(); qrBitmap = null }
                    Spacer(Modifier.height(10.dp))
                    Text("Background", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    ColorChoices(backgroundPresets, backgroundColor) { backgroundArgb = it.toArgb(); qrBitmap = null }
                    colorAssessment.message?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            payloadError?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = canGenerate,
                onClick = {
                    isGenerating = true
                    scope.launch {
                        try {
                            val bitmap = generateQrBitmap(encodedContent, foregroundArgb, backgroundArgb)
                            qrBitmap = bitmap
                            viewModel.addHistoryItem(encodedContent, "QR_CODE", "Generated")
                        } catch (error: Exception) {
                            notify(error.userMessage("Could not generate this QR code."))
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (isGenerating) "Generating…" else "Generate QR Code", modifier = Modifier.padding(vertical = 4.dp)) }

            Spacer(Modifier.height(24.dp))
            qrBitmap?.let { bitmap ->
                Card(modifier = Modifier.size(260.dp), elevation = CardDefaults.cardElevation(8.dp), shape = RoundedCornerShape(16.dp)) {
                    Image(bitmap.asImageBitmap(), "Generated QR Code", Modifier.fillMaxSize().padding(12.dp))
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        enabled = !isSaving,
                        onClick = {
                            val filename = "QR_${System.currentTimeMillis()}.png"
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                isSaving = true
                                scope.launch {
                                    val result = saveBitmapToDownloads(context, bitmap, filename)
                                    isSaving = false
                                    notify(result.fold(onSuccess = { it }, onFailure = { "Could not save QR code: ${it.userMessage()}" }))
                                }
                            } else {
                                pendingLegacySave = bitmap
                                legacySaveLauncher.launch(filename)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(if (isSaving) "Saving…" else "⬇ Save") }

                    Button(
                        enabled = !isSharing,
                        onClick = {
                            isSharing = true
                            scope.launch {
                                val result = prepareShareIntent(context, bitmap)
                                isSharing = false
                                result.onSuccess { shareIntent ->
                                    if (shareIntent.resolveActivity(context.packageManager) == null) {
                                        notify("No app is available to share this image.")
                                    } else {
                                        val chooser = Intent.createChooser(shareIntent, "Share QR Code").apply {
                                            clipData = shareIntent.clipData
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(chooser)
                                    }
                                }.onFailure { notify("Could not share QR code: ${it.userMessage()}") }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(if (isSharing) "Preparing…" else "↗ Share") }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GeneratorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine
    )
}

@Composable
private fun FieldSpacer() = Spacer(Modifier.height(8.dp))

@Composable
private fun ColorChoices(colors: List<Color>, selected: Color, onSelect: (Color) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(colors) { color ->
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(color).border(
                    if (selected == color) 3.dp else 1.dp,
                    if (selected == color) MaterialTheme.colorScheme.primary else Color.Gray,
                    CircleShape
                ).clickable { onSelect(color) }
            )
        }
    }
}

private suspend fun generateQrBitmap(content: String, foregroundArgb: Int, backgroundArgb: Int): Bitmap =
    withContext(Dispatchers.Default) {
        QrGenerationPolicy.payloadError(content)?.let { throw IllegalArgumentException(it) }
        val correction = when (QrGenerationPolicy.errorCorrectionFor(content)) {
            QrErrorCorrection.LOW -> ErrorCorrectionLevel.L
            QrErrorCorrection.MEDIUM -> ErrorCorrectionLevel.M
            QrErrorCorrection.QUARTILE -> ErrorCorrectionLevel.Q
        }
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to correction,
            EncodeHintType.MARGIN to 4
        )
        try {
            val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)
            val pixels = IntArray(QR_SIZE * QR_SIZE)
            var offset = 0
            for (y in 0 until QR_SIZE) {
                for (x in 0 until QR_SIZE) pixels[offset++] = if (matrix[x, y]) foregroundArgb else backgroundArgb
            }
            Bitmap.createBitmap(pixels, QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888)
        } catch (_: WriterException) {
            throw IllegalArgumentException("This content is too large to generate as a reliable QR code.")
        }
    }

@RequiresApi(Build.VERSION_CODES.Q)
private suspend fun saveBitmapToDownloads(context: Context, bitmap: Bitmap, filename: String): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            val resolver = context.contentResolver
            var uri: Uri? = null
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Android could not create the download.")
                resolver.openOutputStream(uri!!, "w")?.use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw IOException("The image could not be encoded.")
                } ?: throw IOException("Android could not open the download.")
                val completed = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                if (resolver.update(uri!!, completed, null, null) <= 0) throw IOException("Android could not finish the download.")
                uri = null
                "Saved to Downloads/$filename"
            } finally {
                uri?.let { resolver.delete(it, null, null) }
            }
        }
    }

private suspend fun saveBitmapToUri(context: Context, bitmap: Bitmap, uri: Uri): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw IOException("The image could not be encoded.")
            } ?: throw IOException("The selected location could not be opened.")
            "Saved to ${uri.lastPathSegment ?: uri}"
        }
    }

private suspend fun prepareShareIntent(context: Context, bitmap: Bitmap): Result<Intent> =
    withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, "qr_share")
            if (!directory.exists() && !directory.mkdirs()) throw IOException("The sharing cache could not be created.")
            val cutoff = System.currentTimeMillis() - SHARE_CACHE_MAX_AGE_MS
            directory.listFiles()?.filter { it.isFile && it.lastModified() < cutoff }?.forEach { it.delete() }
            val file = File(directory, "qr_${System.currentTimeMillis()}_${UUID.randomUUID()}.png")
            file.outputStream().use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw IOException("The image could not be encoded.")
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "QR code", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

private fun Throwable.userMessage(fallback: String = "Please try again."): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
