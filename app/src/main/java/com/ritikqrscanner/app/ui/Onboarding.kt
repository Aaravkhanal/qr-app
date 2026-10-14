package com.ritikqrscanner.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ritikqrscanner.app.R

@Composable
fun OnboardingDialog(onComplete: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = listOf(
        OnboardingPage(Icons.Default.QrCodeScanner, R.string.onboarding_scan_title, R.string.onboarding_scan_body),
        OnboardingPage(Icons.Default.AutoAwesome, R.string.onboarding_create_title, R.string.onboarding_create_body),
        OnboardingPage(Icons.Default.Lock, R.string.onboarding_private_title, R.string.onboarding_private_body)
    )
    val current = pages[page]

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onComplete) { Text(stringResource(R.string.onboarding_skip)) }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            current.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(28.dp).size(64.dp)
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(stringResource(current.title), style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(current.body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pages.indices.forEach { index ->
                            Surface(
                                modifier = Modifier.size(if (index == page) 24.dp else 8.dp, 8.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                color = if (index == page) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ) {}
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { if (page == pages.lastIndex) onComplete() else page++ },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) {
                        Text(stringResource(if (page == pages.lastIndex) R.string.onboarding_start else R.string.onboarding_next))
                    }
                }
            }
        }
    }
}

private data class OnboardingPage(val icon: ImageVector, val title: Int, val body: Int)
