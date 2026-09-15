package com.anik.later.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import com.anik.later.send.Permissions
import com.anik.later.send.WhatsApp

private data class Step(
    val title: String,
    val why: String,
    val done: Boolean,
    val required: Boolean,
    val action: (() -> Unit)?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Every one of these is changed in system Settings, so re-read them each time the
    // user comes back to the app rather than caching anything.
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val steps = remember(refreshKey) {
        listOf(
            Step(
                title = "WhatsApp installed",
                why = "Everything else depends on it.",
                done = WhatsApp.isInstalled(context),
                required = true,
                action = null,
            ),
            Step(
                title = "Exact alarms",
                why = "Without this Android may hold a message back by several minutes.",
                done = Permissions.canScheduleExactAlarms(context),
                required = true,
                action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                .setData(Uri.parse("package:${context.packageName}"))
                        )
                    }
                } else {
                    null
                },
            ),
            Step(
                title = "Notifications",
                why = "Used for the tap-to-send fallback when auto-send cannot run.",
                done = Permissions.notificationsAllowed(context),
                required = true,
                action = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                },
            ),
            Step(
                title = "Auto-send (accessibility)",
                why = "Lets Later press WhatsApp's send button for you. " +
                    "Without it, every message waits for a tap.",
                done = Permissions.accessibilityEnabled(context),
                required = false,
                action = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            ),
            Step(
                title = "Display over other apps",
                why = "The reliable way to let Later open WhatsApp in the background. " +
                    "Some phones manage without it — Later tries either way.",
                done = Permissions.canDrawOverlays(context),
                required = false,
                action = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            .setData(Uri.parse("package:${context.packageName}"))
                    )
                },
            ),
            Step(
                title = "Unrestricted battery use",
                why = "Stops Android from putting Later to sleep and missing a send.",
                done = Permissions.batteryUnrestricted(context),
                required = false,
                action = {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                },
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Later can send on its own, but only when all of the boxes below are " +
                    "ticked and your phone is unlocked at the scheduled moment. " +
                    "Anything it cannot send automatically turns into a notification " +
                    "that sends with one tap.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(top = 8.dp))
            steps.forEach { StepCard(it) }
        }
    }
}

@Composable
private fun StepCard(step: Step) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (step.done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (step.done) "Done" else "Not set up",
                tint = if (step.done) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    step.title + if (step.required) "" else " (optional)",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    step.why,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!step.done && step.action != null) {
                TextButton(onClick = step.action) { Text("Open") }
            }
        }
    }
}
