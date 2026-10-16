package com.ritikqrscanner.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ritikqrscanner.app.data.HistoryItem
import com.ritikqrscanner.app.domain.qr.QrAction
import com.ritikqrscanner.app.domain.qr.QrActionResolver
import com.ritikqrscanner.app.domain.qr.QrPayloadParser
import com.ritikqrscanner.app.domain.qr.QrPayloadType
import com.ritikqrscanner.app.ui.label
import com.ritikqrscanner.app.ui.launchQrAction
import com.ritikqrscanner.app.ui.ScreenHeader
import com.ritikqrscanner.app.viewmodel.MainViewModel
import com.ritikqrscanner.app.R
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel = viewModel()) {
    val historyItems by viewModel.history.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val historyCount by viewModel.historyCount.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<HistoryItem?>(null) }
    val filterOptions = listOf(
        "All" to stringResource(R.string.history_filter_all),
        "Scanned" to stringResource(R.string.history_filter_scanned),
        "Generated" to stringResource(R.string.history_filter_generated),
        "Pinned" to stringResource(R.string.history_filter_pinned)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.history_title),
            subtitle = if (historyCount == 0) {
                stringResource(R.string.history_subtitle_empty)
            } else {
                pluralStringResource(R.plurals.history_subtitle_count, historyCount, historyCount)
            },
            icon = Icons.AutoMirrored.Filled.List,
            action = if (historyCount > 0) {
                {
                    IconButton(onClick = { showClearHistoryDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.history_clear))
                    }
                }
            } else null
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text(stringResource(R.string.history_search)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                AnimatedVisibility(visible = searchQuery.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.history_clear_search))
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { (option, label) ->
                FilterChip(
                    selected = filterType == option,
                    onClick = { viewModel.setFilterType(option) },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (historyItems.isEmpty()) {
            val hasSearch = searchQuery.isNotBlank()
            val isFiltered = filterType != "All"
            val emptyTitle = when {
                hasSearch -> stringResource(R.string.history_no_match)
                isFiltered -> stringResource(R.string.history_no_filter_items, filterType.lowercase())
                else -> stringResource(R.string.history_empty)
            }
            val emptyDescription = when {
                hasSearch && isFiltered -> stringResource(R.string.history_no_filter_match, filterType)
                hasSearch -> stringResource(R.string.history_try_search)
                isFiltered -> stringResource(R.string.history_choose_filter)
                else -> stringResource(R.string.history_empty_description)
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (hasSearch || isFiltered) Icons.Default.Search else Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(
                            if (hasSearch || isFiltered) R.string.history_search_empty_icon_description
                            else R.string.history_empty_icon_description
                        ),
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = emptyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = emptyDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(historyItems, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onPin = { viewModel.updateHistoryItem(item.copy(isPinned = !item.isPinned)) },
                        onDelete = { pendingDelete = item }
                    )
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryDialog = false
                }) { Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHistoryItem(item)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val copyLabel = stringResource(R.string.copy)
    var showToast by remember { mutableStateOf(false) }
    var actionMessage by rememberSaveable(item.id) { mutableStateOf<String?>(null) }

    if (showToast) {
        LaunchedEffect(Unit) {
            android.widget.Toast.makeText(context, context.getString(R.string.copied), android.widget.Toast.LENGTH_SHORT).show()
            showToast = false
        }
    }

    val payload = remember(item.content, item.format) {
        QrPayloadParser.parse(item.content, item.format)
    }
    val action = remember(payload) { QrActionResolver.resolve(payload) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = copyLabel) {
                clipboardManager.setText(AnnotatedString(item.content))
                showToast = true
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPinned)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        HistoryBadge(
                            text = stringResource(if (item.type == "Scanned") R.string.history_type_scanned else R.string.history_type_generated),
                            containerColor = if (item.type == "Scanned") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                        )
                    }
                    item {
                        HistoryBadge(
                            text = if (payload.type == QrPayloadType.UNKNOWN) {
                                stringResource(R.string.unknown_barcode_format, payloadTypeLabel(payload.type), item.format)
                            } else {
                                payloadTypeLabel(payload.type)
                            }
                        )
                    }
                }
                Text(
                    text = formatTimestamp(item.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val narrow = maxWidth < 300.dp
                if (narrow) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        HistoryPrimaryAction(action) { actionMessage = launchQrAction(context, action) }
                        HistoryManagementActions(item, onPin, onDelete)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HistoryPrimaryAction(action) { actionMessage = launchQrAction(context, action) }
                        HistoryManagementActions(item, onPin, onDelete)
                    }
                }
            }
            actionMessage?.let { message ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { actionMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.dismiss))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryBadge(text: String, containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface) {
    Surface(color = containerColor, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HistoryPrimaryAction(action: QrAction, onAction: () -> Unit) {
    val context = LocalContext.current
    if (action != QrAction.None) {
        OutlinedButton(
            onClick = onAction,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) { Text(action.label(context), style = MaterialTheme.typography.labelSmall) }
    } else {
        Text(
            stringResource(R.string.tap_to_copy),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HistoryManagementActions(item: HistoryItem, onPin: () -> Unit, onDelete: () -> Unit) {
    Row {
        IconButton(onClick = onPin, modifier = Modifier.size(36.dp)) {
            Icon(
                if (item.isPinned) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = stringResource(if (item.isPinned) R.string.unpin else R.string.pin),
                tint = if (item.isPinned) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(timestamp))
}
