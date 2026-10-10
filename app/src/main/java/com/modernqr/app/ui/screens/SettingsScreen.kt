package com.modernqr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modernqr.app.data.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val themeMode by settingsManager.themeModeFlow.collectAsState(initial = 2)
    val hapticEnabled by settingsManager.hapticFeedbackFlow.collectAsState(initial = true)
    val soundEnabled by settingsManager.scanSoundFlow.collectAsState(initial = true)

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Theme Selection
        SettingsSection(title = "Appearance") {
            val themeOptions = listOf("Light", "Dark", "System Default")
            themeOptions.forEachIndexed { index, title ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = (themeMode == index),
                            role = Role.RadioButton,
                            onValueChange = {
                                coroutineScope.launch { settingsManager.setThemeMode(index) }
                            }
                        )
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = (themeMode == index), onClick = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scanner Behavior
        SettingsSection(title = "Scanner") {
            // Haptic Feedback toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Haptic Feedback", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Vibrate on successful scan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = {
                        coroutineScope.launch { settingsManager.setHapticFeedback(it) }
                    }
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Scan Sound toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Scan Sound", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Play beep on successful scan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = {
                        coroutineScope.launch { settingsManager.setScanSound(it) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // About
        SettingsSection(title = "About") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                SettingsInfoRow(label = "Developer", value = "Aarav Khanal")
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Contact: ", style = MaterialTheme.typography.bodyMedium)
                    androidx.compose.foundation.text.ClickableText(
                        text = androidx.compose.ui.text.AnnotatedString("khanalaarav2063@gmail.com"),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                        onClick = { uriHandler.openUri("mailto:khanalaarav2063@gmail.com") }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("GitHub: ", style = MaterialTheme.typography.bodyMedium)
                    androidx.compose.foundation.text.ClickableText(
                        text = androidx.compose.ui.text.AnnotatedString("github.com/Aaravkhanal"),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                        onClick = { uriHandler.openUri("https://github.com/Aaravkhanal") }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                androidx.compose.foundation.text.ClickableText(
                    text = androidx.compose.ui.text.AnnotatedString("Privacy Policy"),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                    onClick = { uriHandler.openUri("https://docs.google.com/document/d/1KkKbtngJ4Nq-I-uX4OAik2w3YD889olWSKZ1AYuTixE/edit?usp=sharing") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Version info at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (_: Exception) { "N/A" }

            Text(
                text = "Version $versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = context.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "© 2026 Aarav Khanal. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
