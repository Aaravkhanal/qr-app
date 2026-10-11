package com.ritikqrscanner.app.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ritikqrscanner.app.data.SettingsManager
import com.ritikqrscanner.app.R
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val coroutineScope = rememberCoroutineScope()

    val themeMode by settingsManager.themeModeFlow.collectAsState(initial = 2)
    val hapticEnabled by settingsManager.hapticFeedbackFlow.collectAsState(initial = true)
    val soundEnabled by settingsManager.scanSoundFlow.collectAsState(initial = true)
    val saveHistoryEnabled by settingsManager.saveHistoryFlow.collectAsState(initial = true)

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
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Theme Selection
        SettingsSection(title = stringResource(R.string.settings_appearance)) {
            val themeOptions = listOf(
                stringResource(R.string.theme_light),
                stringResource(R.string.theme_dark),
                stringResource(R.string.theme_system)
            )
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
        SettingsSection(title = stringResource(R.string.settings_scanner)) {
            // Haptic Feedback toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = hapticEnabled,
                        role = Role.Switch,
                        onValueChange = { coroutineScope.launch { settingsManager.setHapticFeedback(it) } }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.haptic_feedback), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.haptic_feedback_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = null
                )
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            // Scan Sound toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = soundEnabled,
                        role = Role.Switch,
                        onValueChange = { coroutineScope.launch { settingsManager.setScanSound(it) } }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.scan_sound), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.scan_sound_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingsSection(title = stringResource(R.string.settings_privacy)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = saveHistoryEnabled,
                        role = Role.Switch,
                        onValueChange = { coroutineScope.launch { settingsManager.setSaveHistory(it) } }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.save_history), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.save_history_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = saveHistoryEnabled,
                    onCheckedChange = null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // About
        SettingsSection(title = stringResource(R.string.settings_about)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                SettingsInfoRow(label = stringResource(R.string.developer), value = stringResource(R.string.developer_name))
                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    Text(stringResource(R.string.contact), style = MaterialTheme.typography.labelMedium)
                    val emailUri = stringResource(R.string.developer_email_uri)
                    Text(
                        text = stringResource(R.string.developer_email),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.open_contact_email)) {
                            uriHandler.openUri(emailUri)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Column {
                    Text(stringResource(R.string.github), style = MaterialTheme.typography.labelMedium)
                    val githubUri = stringResource(R.string.github_uri)
                    Text(
                        text = stringResource(R.string.github_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.open_github_profile)) {
                            uriHandler.openUri(githubUri)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val privacyUri = stringResource(R.string.privacy_policy_uri)
                Text(
                    text = stringResource(R.string.privacy_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClickLabel = stringResource(R.string.open_privacy_policy)) {
                        uriHandler.openUri(privacyUri)
                    }
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
            val versionName = remember(context) {
                runCatching {
                    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            PackageManager.PackageInfoFlags.of(0)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }
                    packageInfo.versionName
                }.getOrNull()
            } ?: stringResource(R.string.version_unknown)

            Text(
                text = stringResource(R.string.version_format, versionName),
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
                text = stringResource(R.string.copyright),
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
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
