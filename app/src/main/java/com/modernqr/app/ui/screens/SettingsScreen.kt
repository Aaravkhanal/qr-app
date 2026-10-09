package com.modernqr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.modernqr.app.data.SettingsManager
import com.ritikqrscanner.app.BuildConfig
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val themeMode by settingsManager.themeModeFlow.collectAsState(initial = 2)

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Selection
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            val themeOptions = listOf("Light", "Dark", "System Default")
            themeOptions.forEachIndexed { index, title ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = (themeMode == index),
                            role = Role.RadioButton,
                            onValueChange = {
                                coroutineScope.launch {
                                    settingsManager.setThemeMode(index)
                                }
                            }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (themeMode == index),
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = title)
                }
            }
        }

        Divider(modifier = Modifier.padding(horizontal = 16.dp))
        
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

        Column(modifier = Modifier.padding(16.dp)) {
            Text("About", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Developer: Aarav Khanal", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text("Contact: ", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.foundation.text.ClickableText(
                    text = androidx.compose.ui.text.AnnotatedString("khanalaarav2063@gmail.com"),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                    onClick = { uriHandler.openUri("mailto:khanalaarav2063@gmail.com") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text("GitHub: ", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.foundation.text.ClickableText(
                    text = androidx.compose.ui.text.AnnotatedString("https://github.com/Aaravkhanal"),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                    onClick = { uriHandler.openUri("https://github.com/Aaravkhanal") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text("Website: ", style = MaterialTheme.typography.bodyMedium)
                androidx.compose.foundation.text.ClickableText(
                    text = androidx.compose.ui.text.AnnotatedString("https://github.com/Aaravkhanal"),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                    onClick = { uriHandler.openUri("https://github.com/Aaravkhanal") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.text.ClickableText(
                text = androidx.compose.ui.text.AnnotatedString("Privacy Policy"),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                onClick = { uriHandler.openUri("https://docs.google.com/document/d/1KkKbtngJ4Nq-I-uX4OAik2w3YD889olWSKZ1AYuTixE/edit?usp=sharing") }
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName
            Text(
                text = "Version: $versionName",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Package Name: com.ritikqrscanner.app",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "© 2026 Aarav Khanal. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
