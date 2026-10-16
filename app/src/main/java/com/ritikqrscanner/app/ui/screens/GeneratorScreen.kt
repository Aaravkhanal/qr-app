package com.ritikqrscanner.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.ritikqrscanner.app.domain.qr.*
import com.ritikqrscanner.app.ui.PrivacyPill
import com.ritikqrscanner.app.ui.ScreenHeader
import com.ritikqrscanner.app.viewmodel.MainViewModel
import com.ritikqrscanner.app.R
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
    val types = listOf(
        "Smart" to stringResource(R.string.mode_smart),
        "Text" to stringResource(R.string.mode_text),
        "URL" to stringResource(R.string.mode_url),
        "WiFi" to stringResource(R.string.mode_wifi),
        "Location" to stringResource(R.string.mode_location),
        "Contact" to stringResource(R.string.mode_contact),
        "Email" to stringResource(R.string.mode_email),
        "Phone" to stringResource(R.string.mode_phone),
        "SMS" to stringResource(R.string.mode_sms),
        "Contact Card" to stringResource(R.string.mode_contact_card)
    )

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
    var lastGeneratedContent by rememberSaveable { mutableStateOf<String?>(null) }
    var lastGeneratedForeground by rememberSaveable { mutableIntStateOf(Color.Black.toArgb()) }
    var lastGeneratedBackground by rememberSaveable { mutableIntStateOf(Color.White.toArgb()) }
    var pendingLegacySave by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var operationError by rememberSaveable { mutableStateOf<String?>(null) }

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
            result.onSuccess(::notify).onFailure {
                operationError = context.getString(
                    R.string.save_error,
                    it.userMessage(context.getString(R.string.try_again_message))
                )
            }
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
    val wifiTypeLabels = listOf(
        "WPA2" to stringResource(R.string.wifi_security_wpa2),
        "WPA3" to stringResource(R.string.wifi_security_wpa3),
        "WEP" to stringResource(R.string.wifi_security_wep),
        "nopass" to stringResource(R.string.wifi_security_open)
    )
    val canGenerate = validation.isValid && payloadError == null && colorAssessment.isReliable && !isGenerating
    fun fieldError(vararg terms: String): String? = validation.errors.firstOrNull { error ->
        terms.any { term -> error.contains(term, ignoreCase = true) }
    }?.let { localizedDomainMessage(context, it) }

    LaunchedEffect(lastGeneratedContent, lastGeneratedForeground, lastGeneratedBackground) {
        val savedContent = lastGeneratedContent ?: return@LaunchedEffect
        if (qrBitmap == null) {
            runCatching { generateQrBitmap(savedContent, lastGeneratedForeground, lastGeneratedBackground) }
                .onSuccess { qrBitmap = it }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(
                title = stringResource(R.string.create_title),
                subtitle = stringResource(R.string.create_subtitle),
                icon = Icons.Default.QrCode2
            )
            PrivacyPill(
                label = stringResource(R.string.privacy_on_device),
                modifier = Modifier.align(Alignment.Start).padding(top = 10.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.create_choose_type),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(types) { (type, label) ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type; qrBitmap = null; lastGeneratedContent = null },
                        label = { Text(label) },
                        leadingIcon = if (type == "Smart") {
                            { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            when (selectedType) {
                "Smart" -> {
                    GeneratorField(smartInput, { smartInput = it }, stringResource(R.string.smart_input_label), localizedDomainMessage(context, validation.message))
                    if (smartInput.isNotBlank()) Text(
                        stringResource(R.string.detected_type, payloadTypeLabel(payload.type)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (smartInput.isBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    stringResource(R.string.create_smart_recommendation),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
                "Text" -> GeneratorField(textInput, { textInput = it }, stringResource(R.string.text_input_label), fieldError("text"))
                "URL" -> GeneratorField(urlInput, { urlInput = it }, stringResource(R.string.url_input_label), fieldError("URL"), KeyboardType.Uri, true)
                "WiFi" -> {
                    GeneratorField(ssidInput, { ssidInput = it }, stringResource(R.string.ssid_label), fieldError("network name"), singleLine = true)
                    FieldSpacer()
                    GeneratorField(passwordInput, { passwordInput = it }, stringResource(R.string.password_label), fieldError("password"), KeyboardType.Password, true)
                    FieldSpacer()
                    ExposedDropdownMenuBox(expanded = expandedWifiType, onExpandedChange = { expandedWifiType = it }) {
                        OutlinedTextField(value = wifiTypeLabels.first { it.first == wifiType }.second, onValueChange = {}, readOnly = true, label = { Text(stringResource(R.string.security_type_label)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedWifiType) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = expandedWifiType, onDismissRequest = { expandedWifiType = false }) {
                            wifiTypeLabels.forEach { (type, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { wifiType = type; expandedWifiType = false })
                            }
                        }
                    }
                }
                "Location" -> {
                    GeneratorField(latInput, { latInput = it }, stringResource(R.string.latitude_label), fieldError("Latitude"), KeyboardType.Decimal, true)
                    FieldSpacer()
                    GeneratorField(lngInput, { lngInput = it }, stringResource(R.string.longitude_label), fieldError("Longitude"), KeyboardType.Decimal, true)
                }
                "Contact" -> {
                    GeneratorField(nameInput, { nameInput = it }, stringResource(R.string.name_label), fieldError("contact name"), singleLine = true)
                    FieldSpacer()
                    GeneratorField(phoneInput, { phoneInput = it }, stringResource(R.string.phone_label), fieldError("contact phone"), KeyboardType.Phone, true)
                    FieldSpacer()
                    GeneratorField(emailInput, { emailInput = it }, stringResource(R.string.email_label), fieldError("contact email"), KeyboardType.Email, true)
                }
                "Email" -> {
                    GeneratorField(emailOnlyInput, { emailOnlyInput = it }, stringResource(R.string.email_address_label), fieldError("email"), KeyboardType.Email, true)
                    FieldSpacer()
                    GeneratorField(emailSubjectInput, { emailSubjectInput = it }, stringResource(R.string.subject_optional_label))
                    FieldSpacer()
                    GeneratorField(emailBodyInput, { emailBodyInput = it }, stringResource(R.string.message_optional_label))
                }
                "Phone" -> GeneratorField(phoneOnlyInput, { phoneOnlyInput = it }, stringResource(R.string.phone_number_label), fieldError("phone"), KeyboardType.Phone, true)
                "SMS" -> {
                    GeneratorField(smsPhoneInput, { smsPhoneInput = it }, stringResource(R.string.phone_number_label), fieldError("phone"), KeyboardType.Phone, true)
                    FieldSpacer()
                    GeneratorField(smsMessageInput, { smsMessageInput = it }, stringResource(R.string.message_optional_label))
                }
                "Contact Card" -> {
                    Text(stringResource(R.string.contact_card_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FieldSpacer()
                    GeneratorField(allName, { allName = it }, stringResource(R.string.name_label), fieldError("contact name"), singleLine = true)
                    FieldSpacer()
                    GeneratorField(allPhone, { allPhone = it }, stringResource(R.string.phone_label), fieldError("contact phone"), KeyboardType.Phone, true)
                    FieldSpacer()
                    GeneratorField(allEmail, { allEmail = it }, stringResource(R.string.email_label), fieldError("contact email"), KeyboardType.Email, true)
                    FieldSpacer()
                    GeneratorField(allUrl, { allUrl = it }, stringResource(R.string.website_url_label), fieldError("URL"), KeyboardType.Uri, true)
                    FieldSpacer()
                    GeneratorField(allWifiSsid, { allWifiSsid = it }, stringResource(R.string.wifi_network_label), singleLine = true)
                    FieldSpacer()
                    GeneratorField(allWifiPass, { allWifiPass = it }, stringResource(R.string.wifi_password_label), keyboardType = KeyboardType.Password, singleLine = true)
                    FieldSpacer()
                    GeneratorField(allLocation, { allLocation = it }, stringResource(R.string.location_address_label))
                    FieldSpacer()
                    GeneratorField(allNotes, { allNotes = it }, stringResource(R.string.notes_label))
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.qr_colors), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.foreground), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    ColorChoices(foregroundPresets, foregroundColor, stringResource(R.string.foreground_color_option)) { foregroundArgb = it.toArgb(); qrBitmap = null; lastGeneratedContent = null }
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.background), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    ColorChoices(backgroundPresets, backgroundColor, stringResource(R.string.background_color_option)) { backgroundArgb = it.toArgb(); qrBitmap = null; lastGeneratedContent = null }
                    colorAssessment.message?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(localizedDomainMessage(context, it) ?: it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            payloadError?.let {
                Spacer(Modifier.height(8.dp))
                Text(localizedDomainMessage(context, it) ?: it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            QrReadinessCard(
                isValid = validation.isValid && payloadError == null,
                colorsReliable = colorAssessment.isReliable,
                byteCount = encodedContent.toByteArray(Charsets.UTF_8).size,
                typeLabel = payloadTypeLabel(payload.type)
            )
            Spacer(Modifier.height(12.dp))
            operationError?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(
                            onClick = { operationError = null },
                            modifier = Modifier.align(Alignment.End)
                        ) { Text(stringResource(R.string.dismiss)) }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Button(
                enabled = canGenerate,
                onClick = {
                    isGenerating = true
                    operationError = null
                    scope.launch {
                        try {
                            val bitmap = generateQrBitmap(encodedContent, foregroundArgb, backgroundArgb)
                            qrBitmap = bitmap
                            lastGeneratedContent = encodedContent
                            lastGeneratedForeground = foregroundArgb
                            lastGeneratedBackground = backgroundArgb
                            viewModel.addHistoryItem(encodedContent, "QR_CODE", "Generated")
                        } catch (error: Exception) {
                            operationError = if (error is QrContentTooLargeException) {
                                context.getString(R.string.qr_content_too_large)
                            } else {
                                error.userMessage(context.getString(R.string.generate_error))
                            }
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(if (isGenerating) R.string.generating else R.string.generate_qr), modifier = Modifier.padding(vertical = 4.dp)) }

            Spacer(Modifier.height(24.dp))
            qrBitmap?.let { bitmap ->
                Card(modifier = Modifier.size(260.dp), elevation = CardDefaults.cardElevation(8.dp), shape = RoundedCornerShape(16.dp)) {
                    Image(bitmap.asImageBitmap(), stringResource(R.string.generated_qr_description), Modifier.fillMaxSize().padding(12.dp))
                }
                Spacer(Modifier.height(16.dp))
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val stackActions = maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f
                    @Composable fun SaveButton(modifier: Modifier) = OutlinedButton(
                        enabled = !isSaving,
                        onClick = {
                            operationError = null
                            val filename = "QR_${System.currentTimeMillis()}.png"
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                isSaving = true
                                scope.launch {
                                    val result = saveBitmapToDownloads(context, bitmap, filename)
                                    isSaving = false
                                    result.onSuccess(::notify).onFailure {
                                        operationError = context.getString(
                                            R.string.save_error,
                                            it.userMessage(context.getString(R.string.try_again_message))
                                        )
                                    }
                                }
                            } else {
                                pendingLegacySave = bitmap
                                legacySaveLauncher.launch(filename)
                            }
                        },
                        modifier = modifier,
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(stringResource(if (isSaving) R.string.saving else R.string.save)) }

                    @Composable fun ShareButton(modifier: Modifier) = Button(
                        enabled = !isSharing,
                        onClick = {
                            operationError = null
                            isSharing = true
                            scope.launch {
                                val result = prepareShareIntent(context, bitmap)
                                isSharing = false
                                result.onSuccess { shareIntent ->
                                    if (shareIntent.resolveActivity(context.packageManager) == null) {
                                        operationError = context.getString(R.string.share_unavailable)
                                    } else {
                                        val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_qr)).apply {
                                            clipData = shareIntent.clipData
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(chooser)
                                    }
                                }.onFailure {
                                    operationError = context.getString(
                                        R.string.share_error,
                                        it.userMessage(context.getString(R.string.try_again_message))
                                    )
                                }
                            }
                        },
                        modifier = modifier,
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(stringResource(if (isSharing) R.string.preparing else R.string.share)) }

                    if (stackActions) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SaveButton(Modifier.fillMaxWidth())
                            ShareButton(Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SaveButton(Modifier.weight(1f))
                            ShareButton(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun QrReadinessCard(
    isValid: Boolean,
    colorsReliable: Boolean,
    byteCount: Int,
    typeLabel: String
) {
    val ready = isValid && colorsReliable
    val title = when {
        !isValid -> stringResource(R.string.qr_needs_input)
        !colorsReliable -> stringResource(R.string.qr_fix_colors)
        else -> stringResource(R.string.qr_ready)
    }
    val quality = when {
        byteCount > 1_200 -> stringResource(R.string.qr_quality_dense)
        byteCount > 450 -> stringResource(R.string.qr_quality_good)
        else -> stringResource(R.string.qr_quality_excellent)
    }
    val accent = if (ready) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (ready) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (ready) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = accent
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.qr_readiness_title), style = MaterialTheme.typography.labelMedium, color = accent)
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (isValid) {
                    Text(
                        pluralStringResource(R.plurals.qr_payload_size, byteCount, byteCount, "$typeLabel · $quality"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
private fun ColorChoices(colors: List<Color>, selected: Color, accessibilityLabel: String, onSelect: (Color) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(colors) { color ->
            Box(
                Modifier.size(48.dp).semantics {
                    this.selected = selected == color
                }.clickable(role = Role.RadioButton, onClickLabel = accessibilityLabel) { onSelect(color) }
                    .padding(6.dp).clip(CircleShape).background(color).border(
                    if (selected == color) 3.dp else 1.dp,
                    if (selected == color) MaterialTheme.colorScheme.primary else Color.Gray,
                    CircleShape
                )
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
            throw QrContentTooLargeException()
        }
    }

@RequiresApi(Build.VERSION_CODES.Q)
private suspend fun saveBitmapToDownloads(context: Context, bitmap: Bitmap, filename: String): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
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
                    ?: throw IOException(context.getString(R.string.download_create_error))
                resolver.openOutputStream(uri!!, "w")?.use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw IOException(context.getString(R.string.image_encode_error))
                } ?: throw IOException(context.getString(R.string.download_open_error))
                val completed = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                if (resolver.update(uri!!, completed, null, null) <= 0) throw IOException(context.getString(R.string.download_finish_error))
                uri = null
                context.getString(R.string.saved_to_downloads, filename)
            } finally {
                uri?.let { resolver.delete(it, null, null) }
            }
        }
    }

private suspend fun saveBitmapToUri(context: Context, bitmap: Bitmap, uri: Uri): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "w")?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw IOException(context.getString(R.string.image_encode_error))
            } ?: throw IOException(context.getString(R.string.save_location_open_error))
            context.getString(R.string.saved_to_location, uri.lastPathSegment ?: uri.toString())
        }
    }

private suspend fun prepareShareIntent(context: Context, bitmap: Bitmap): Result<Intent> =
    withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, "qr_share")
            if (!directory.exists() && !directory.mkdirs()) throw IOException(context.getString(R.string.share_cache_error))
            val cutoff = System.currentTimeMillis() - SHARE_CACHE_MAX_AGE_MS
            directory.listFiles()?.filter { it.isFile && it.lastModified() < cutoff }?.forEach { it.delete() }
            val file = File(directory, "qr_${System.currentTimeMillis()}_${UUID.randomUUID()}.png")
            file.outputStream().use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw IOException(context.getString(R.string.image_encode_error))
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

private class QrContentTooLargeException : Exception()

private fun localizedDomainMessage(context: Context, message: String?): String? = when (message) {
    "Enter some text." -> context.getString(R.string.validation_enter_text)
    "Enter a URL." -> context.getString(R.string.validation_enter_url)
    "Enter a valid HTTP or HTTPS URL." -> context.getString(R.string.validation_invalid_url)
    "Enter a Wi-Fi network name (SSID)." -> context.getString(R.string.validation_enter_ssid)
    "The Wi-Fi network name must be 32 bytes or fewer." -> context.getString(R.string.validation_ssid_too_long)
    "Enter the Wi-Fi password, or choose no password." -> context.getString(R.string.validation_enter_wifi_password)
    "WPA2 and WPA3 passwords must contain 8 to 63 characters." -> context.getString(R.string.validation_wifi_password_length)
    "Latitude must be between -90 and 90." -> context.getString(R.string.validation_latitude)
    "Longitude must be between -180 and 180." -> context.getString(R.string.validation_longitude)
    "Enter a contact name." -> context.getString(R.string.validation_contact_name)
    "Enter a valid contact phone number." -> context.getString(R.string.validation_contact_phone)
    "Enter a valid contact email address." -> context.getString(R.string.validation_contact_email)
    "Enter an email address." -> context.getString(R.string.validation_email)
    "Enter valid email recipients separated by commas." -> context.getString(R.string.validation_email_recipients)
    "Enter a phone number." -> context.getString(R.string.validation_phone)
    "Enter a valid phone number." -> context.getString(R.string.validation_invalid_phone)
    "This barcode does not contain any data." -> context.getString(R.string.validation_empty_barcode)
    "Enter content before generating a QR code." -> context.getString(R.string.validation_enter_content)
    "This content is too large for a reliable QR code. Shorten it and try again." -> context.getString(R.string.validation_payload_too_large)
    "Use a darker foreground than background for reliable scanning." -> context.getString(R.string.validation_foreground_too_light)
    "These colors have low contrast. Choose a darker foreground or lighter background." -> context.getString(R.string.validation_low_contrast)
    null -> null
    else -> message
}

private fun Throwable.userMessage(fallback: String): String =
    message?.takeIf { it.isNotBlank() } ?: fallback
