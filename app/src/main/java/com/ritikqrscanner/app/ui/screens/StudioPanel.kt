package com.ritikqrscanner.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Intent
import android.content.ClipData
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ritikqrscanner.app.studio.QrStudio
import com.ritikqrscanner.app.studio.AiQrTheme
import com.ritikqrscanner.app.studio.AiQrThemePlanner
import kotlinx.coroutines.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioPanel(content: String, foreground: Int, background: Int, valid: Boolean, onExport: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var rounded by rememberSaveable { mutableStateOf(false) }
    var template by rememberSaveable { mutableStateOf("QR only") }
    var title by rememberSaveable { mutableStateOf("") }
    var subtitle by rememberSaveable { mutableStateOf("") }
    var themePrompt by rememberSaveable { mutableStateOf("") }
    var aiTheme by remember { mutableStateOf<AiQrTheme?>(null) }
    var logo by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var message by remember { mutableStateOf("Enter valid content to preview your design") }
    var pendingContent by remember { mutableStateOf("") }
    var pendingSave by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                require(bounds.outWidth > 0 && bounds.outHeight > 0)
                val options = BitmapFactory.Options().apply { inSampleSize = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 256) }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            } }.onSuccess { logo = it }.onFailure { message = "Could not open this logo" }
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val image = pendingSave
        if (uri != null && image == null) message = "Preview was lost. Generate the design again before saving."
        if (uri != null && image != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) } ?: error("Cannot open destination")
            } }.onSuccess { message = "Design saved"; onExport(pendingContent) }.onFailure { message = "Could not save design" }
        }
    }
    LaunchedEffect(content, foreground, background, rounded, logo, template, title, subtitle, aiTheme, valid) {
        preview = null
        if (!valid || content.isBlank()) { message = "Complete the form to preview your design"; return@LaunchedEffect }
        busy = true
        try {
            delay(350)
            preview = withContext(Dispatchers.Default) { QrStudio.render(content, foreground, background, rounded, logo, template, title, subtitle, aiTheme) }
            message = "Scan verified • ready to export"
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { message = e.message ?: "Design could not be verified"
        } finally { busy = false }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("QR design studio", style = MaterialTheme.typography.titleLarge)
            Text("Live preview • verified before export")
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("Design with AI", style = MaterialTheme.typography.titleMedium)
                            Text("Describe a look and AI will build a colorful, scan-safe theme.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedTextField(
                        value = themePrompt,
                        onValueChange = { themePrompt = it.take(120) },
                        label = { Text("What should it look like?") },
                        placeholder = { Text("e.g. a bold red sports car") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Your prompt stays on this device") },
                        minLines = 2
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AiQrThemePlanner.suggestions.size) { index ->
                            val suggestion = AiQrThemePlanner.suggestions[index]
                            SuggestionChip(onClick = { themePrompt = suggestion }, label = { Text(suggestion) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = themePrompt.isNotBlank(),
                            onClick = {
                                runCatching { AiQrThemePlanner.create(themePrompt) }
                                    .onSuccess { aiTheme = it; message = "${it.name} theme created • checking scan quality" }
                                    .onFailure { message = it.message ?: "Could not create this theme" }
                            }
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (aiTheme == null) "Create AI theme" else "Regenerate theme")
                        }
                        if (aiTheme != null) TextButton(onClick = { aiTheme = null; message = "AI theme removed" }) { Text("Remove") }
                    }
                    aiTheme?.let { Text("Using ${it.name} • ${it.motif.name.lowercase()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("QR only", "Wi-Fi card", "Business card", "Event invitation", "Printable sign").forEach { option ->
                    FilterChip(selected = template == option, onClick = { template = option }, label = { Text(option) })
                }
            }
            if (template != "QR only") OutlinedTextField(title, { title = it }, label = { Text("Card heading") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            if (template != "QR only") OutlinedTextField(subtitle, { subtitle = it }, label = { Text("Subtitle, date, or location") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !rounded, onClick = { rounded = false }, label = { Text("Squares") })
                FilterChip(selected = rounded, onClick = { rounded = true }, label = { Text("Rounded dots") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { logoPicker.launch("image/*") }) { Text("Choose logo") }
                if (logo != null) TextButton(onClick = { logo = null }) { Text("Remove logo") }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            preview?.let { Image(it.asImageBitmap(), "Live QR design preview", Modifier.fillMaxWidth().heightIn(max = 360.dp)) }
            Text(message, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = preview != null && !busy, onClick = { pendingSave = preview; pendingContent = content; save.launch("QR-design.png") }) { Text("Save design") }
                OutlinedButton(enabled = preview != null && !busy, onClick = {
                    val image = preview ?: return@OutlinedButton
                    val exportedContent = content
                    scope.launch {
                        runCatching {
                            val file = withContext(Dispatchers.IO) {
                                File(context.cacheDir, "qr_share").apply { mkdirs() }.let { File(it, "design-${System.currentTimeMillis()}.png") }.also { f -> f.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                            }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); clipData = ClipData.newRawUri("QR design", uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Share design"))
                        }.onSuccess { onExport(exportedContent) }.onFailure { message = "Could not share design" }
                    }
                }) { Text("Share design") }
            }
        }
    }
}
