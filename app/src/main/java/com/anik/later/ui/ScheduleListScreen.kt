package com.anik.later.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Repeat as RepeatIcon
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anik.later.data.Repeat
import com.anik.later.data.ScheduledMessage
import com.anik.later.send.Permissions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    messages: List<ScheduledMessage>,
    onAdd: () -> Unit,
    onOpen: (ScheduledMessage) -> Unit,
    onToggle: (ScheduledMessage, Boolean) -> Unit,
    onSetup: () -> Unit,
    onHistory: () -> Unit,
) {
    val context = LocalContext.current
    // Deliberately ignores the lockscreen check here — that one is only meaningful at
    // send time, and nagging about it on the list screen would be noise.
    val setupIncomplete = !Permissions.accessibilityEnabled(context) ||
        !Permissions.canDrawOverlays(context) ||
        !Permissions.canScheduleExactAlarms(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Later") },
                actions = {
                    IconButton(onClick = onHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onSetup) {
                        Icon(Icons.Default.Settings, contentDescription = "Setup")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Schedule a message")
            }
        },
    ) { padding ->
        if (messages.isEmpty() && !setupIncomplete) {
            EmptyState(Modifier.padding(padding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (setupIncomplete) {
                item { SetupBanner(onSetup) }
            }
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Nothing scheduled yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            items(messages, key = { it.id }) { message ->
                MessageRow(
                    message = message,
                    onClick = { onOpen(message) },
                    onToggle = { onToggle(message, it) },
                )
            }
        }
    }
}

@Composable
private fun SetupBanner(onSetup: () -> Unit) {
    Card(
        onClick = onSetup,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.WarningAmber, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Setup incomplete", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Messages will wait for a tap instead of sending on their own.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun MessageRow(
    message: ScheduledMessage,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(message.recipientName, style = MaterialTheme.typography.titleMedium)
                Text(
                    message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (message.repeat != Repeat.NONE) {
                        Icon(
                            RepeatIcon,
                            contentDescription = message.repeat.label,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        buildString {
                            append(formatWhen(message.scheduledAt))
                            if (message.enabled) append(" · ${formatCountdown(message.scheduledAt)}")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Switch(checked = message.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nothing scheduled", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.padding(top = 4.dp))
            Text(
                "Tap + to write a message and pick when it should go out.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
