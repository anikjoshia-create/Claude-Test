package com.anik.later.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anik.later.data.Outcome
import com.anik.later.data.SendAttempt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    attempts: List<SendAttempt>,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (attempts.isNotEmpty()) {
                        TextButton(onClick = onClear) { Text("Clear") }
                    }
                },
            )
        },
    ) { padding ->
        if (attempts.isEmpty()) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Nothing has gone out yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(attempts, key = { it.id }) { AttemptRow(it) }
        }
    }
}

@Composable
private fun AttemptRow(attempt: SendAttempt) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            val (icon, tint) = when (attempt.outcome) {
                Outcome.AUTO_SENT -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
                Outcome.HANDED_OFF -> Icons.Default.TouchApp to MaterialTheme.colorScheme.tertiary
                Outcome.FAILED -> Icons.Default.ErrorOutline to MaterialTheme.colorScheme.error
            }
            Icon(icon, contentDescription = attempt.outcome.label, tint = tint)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(attempt.recipientName, style = MaterialTheme.typography.titleSmall)
                Text(
                    attempt.bodyPreview,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    buildString {
                        append(attempt.outcome.label)
                        attempt.detail?.let { append(" — ").append(it) }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                )
                Text(
                    formatWhen(attempt.attemptedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
