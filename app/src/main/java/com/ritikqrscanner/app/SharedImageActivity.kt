package com.ritikqrscanner.app

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ritikqrscanner.app.domain.qr.QrPayloadParser
import com.ritikqrscanner.app.domain.qr.QrActionResolver
import com.ritikqrscanner.app.ui.launchQrAction
import kotlinx.coroutines.flow.first

class SharedImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION") val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        setContent {
            val settings = remember { com.ritikqrscanner.app.data.SettingsManager(this@SharedImageActivity) }
            val theme by settings.themeModeFlow.collectAsState(initial = 2)
            val dark = when (theme) { 0 -> false; 1 -> true; else -> androidx.compose.foundation.isSystemInDarkTheme() }
            com.ritikqrscanner.app.ui.QrAppTheme(dark) {
                var results by remember { mutableStateOf<List<com.google.zxing.Result>>(emptyList()) }
                var message by remember { mutableStateOf("Scanning image…") }
                LaunchedEffect(uri) {
                    try {
                        require(intent.action == Intent.ACTION_SEND && uri?.scheme == "content")
                        results = com.ritikqrscanner.app.scanner.decodeGalleryBarcodes(this@SharedImageActivity, uri!!)
                        message = "${results.size} code(s) found. Choose a result below."
                        val settings = com.ritikqrscanner.app.data.SettingsManager(this@SharedImageActivity)
                        if (settings.saveHistoryFlow.first()) {
                            val repository = com.ritikqrscanner.app.data.HistoryRepository(com.ritikqrscanner.app.data.AppDatabase.getDatabase(this@SharedImageActivity).historyDao())
                            results.forEach { repository.add(it.text, it.barcodeFormat.name, "Scanned") }
                        }
                    } catch (e: Exception) { message = e.message ?: "Could not scan this image" }
                }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.systemBarsPadding().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Scan shared image", style = MaterialTheme.typography.headlineMedium)
                        Text(message)
                        results.forEach { result ->
                            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                                Text(result.barcodeFormat.name, style = MaterialTheme.typography.labelMedium)
                                Text(result.text)
                                TextButton(onClick = { (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("QR result", result.text)) }) { Text("Copy") }
                                val action = QrActionResolver.resolve(QrPayloadParser.parse(result.text, result.barcodeFormat.name))
                                if (action != com.ritikqrscanner.app.domain.qr.QrAction.None) TextButton(onClick = { message = launchQrAction(this@SharedImageActivity, action) ?: "Opened" }) { Text("Open result") }
                            } }
                        }
                        Button(onClick = { finish() }) { Text("Done") }
                    }
                }
            }
        }
    }
}
