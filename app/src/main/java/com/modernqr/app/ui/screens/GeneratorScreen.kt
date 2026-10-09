package com.modernqr.app.ui.screens

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.widget.Toast
import java.io.OutputStream
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

import androidx.lifecycle.viewmodel.compose.viewModel
import com.modernqr.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedType by remember { mutableStateOf("Text") }
    val types = listOf("Text", "URL", "WiFi", "Location", "Contact", "Email", "Phone", "SMS", "All-in-One")

    // State for inputs
    var textInput by remember { mutableStateOf("") }
    
    // WiFi state
    var ssidInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var wifiType by remember { mutableStateOf("WPA") }
    var expandedWifiType by remember { mutableStateOf(false) }
    val wifiTypes = listOf("WPA", "WEP", "nopass")
    
    // Location state
    var latInput by remember { mutableStateOf("") }
    var lngInput by remember { mutableStateOf("") }
    
    // Contact state
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    
    // Email state
    var emailOnlyInput by remember { mutableStateOf("") }
    var emailSubjectInput by remember { mutableStateOf("") }
    var emailBodyInput by remember { mutableStateOf("") }

    // Phone state
    var phoneOnlyInput by remember { mutableStateOf("") }

    // SMS state
    var smsPhoneInput by remember { mutableStateOf("") }
    var smsMessageInput by remember { mutableStateOf("") }

    // All-in-One state
    var allName by remember { mutableStateOf("") }
    var allPhone by remember { mutableStateOf("") }
    var allEmail by remember { mutableStateOf("") }
    var allUrl by remember { mutableStateOf("") }
    var allWifiSsid by remember { mutableStateOf("") }
    var allWifiPass by remember { mutableStateOf("") }
    var allLocation by remember { mutableStateOf("") }
    var allNotes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        when (selectedType) {
            "Text" -> {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Enter text") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "URL" -> {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Enter URL (e.g., https://...)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "WiFi" -> {
                OutlinedTextField(
                    value = ssidInput,
                    onValueChange = { ssidInput = it },
                    label = { Text("Network Name (SSID)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedWifiType,
                    onExpandedChange = { expandedWifiType = it }
                ) {
                    OutlinedTextField(
                        value = wifiType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Security Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWifiType) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedWifiType,
                        onDismissRequest = { expandedWifiType = false }
                    ) {
                        wifiTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    wifiType = type
                                    expandedWifiType = false
                                }
                            )
                        }
                    }
                }
            }
            "Location" -> {
                OutlinedTextField(
                    value = latInput,
                    onValueChange = { latInput = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lngInput,
                    onValueChange = { lngInput = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Contact" -> {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Email" -> {
                OutlinedTextField(
                    value = emailOnlyInput,
                    onValueChange = { emailOnlyInput = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emailSubjectInput,
                    onValueChange = { emailSubjectInput = it },
                    label = { Text("Subject (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = emailBodyInput,
                    onValueChange = { emailBodyInput = it },
                    label = { Text("Message (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Phone" -> {
                OutlinedTextField(
                    value = phoneOnlyInput,
                    onValueChange = { phoneOnlyInput = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "SMS" -> {
                OutlinedTextField(
                    value = smsPhoneInput,
                    onValueChange = { smsPhoneInput = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = smsMessageInput,
                    onValueChange = { smsMessageInput = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "All-in-One" -> {
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
        Button(onClick = {
            val contentToEncode = when (selectedType) {
                "Text", "URL" -> textInput
                "WiFi" -> "WIFI:S:$ssidInput;T:$wifiType;P:$passwordInput;;"
                "Location" -> "geo:$latInput,$lngInput"
                "Contact" -> "BEGIN:VCARD\nVERSION:3.0\nN:$nameInput\nTEL:$phoneInput\nEMAIL:$emailInput\nEND:VCARD"
                "Email" -> "MATMSG:TO:$emailOnlyInput;SUB:$emailSubjectInput;BODY:$emailBodyInput;;"
                "Phone" -> "tel:$phoneOnlyInput"
                "SMS" -> "smsto:$smsPhoneInput:$smsMessageInput"
                "All-in-One" -> {
                    val sb = StringBuilder()
                    sb.append("BEGIN:VCARD\nVERSION:3.0\n")
                    if (allName.isNotEmpty()) sb.append("N:$allName\n")
                    if (allPhone.isNotEmpty()) sb.append("TEL:$allPhone\n")
                    if (allEmail.isNotEmpty()) sb.append("EMAIL:$allEmail\n")
                    if (allUrl.isNotEmpty()) sb.append("URL:$allUrl\n")
                    
                    var noteStr = ""
                    if (allWifiSsid.isNotEmpty()) noteStr += "WiFi: $allWifiSsid / $allWifiPass. "
                    if (allLocation.isNotEmpty()) noteStr += "Loc: $allLocation. "
                    if (allNotes.isNotEmpty()) noteStr += "Notes: $allNotes."
                    if (noteStr.isNotEmpty()) sb.append("NOTE:$noteStr\n")
                    
                    sb.append("END:VCARD")
                    sb.toString()
                }
                else -> ""
            }

            if (contentToEncode.isNotEmpty()) {
                try {
                    val writer = MultiFormatWriter()
                    val matrix = writer.encode(contentToEncode, BarcodeFormat.QR_CODE, 400, 400)
                    val encoder = BarcodeEncoder()
                    qrBitmap = encoder.createBitmap(matrix)
                    viewModel.addHistoryItem(contentToEncode, "QR_CODE", "Generated")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Generate QR Code")
        }
        Spacer(modifier = Modifier.height(32.dp))
        
        qrBitmap?.let { bitmap ->
            Card(
                modifier = Modifier.size(250.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { saveBitmapToGallery(context, bitmap) }) {
                Text("Download QR Code")
            }
        }
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
            // Use MediaStore.Downloads for API 29+
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
                    Toast.makeText(context, "QR Code downloaded successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to download QR Code.", Toast.LENGTH_LONG).show()
            }
        } else {
            // For older devices, write to public Downloads directory
            val directory = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!directory.exists()) directory.mkdirs()
            val file = java.io.File(directory, filename)
            fos = java.io.FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
            android.media.MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
            Toast.makeText(context, "QR Code downloaded successfully!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error downloading: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
        try {
            fos?.close()
        } catch (e: Exception) {}
    }
}
