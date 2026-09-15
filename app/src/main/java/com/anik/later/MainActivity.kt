package com.anik.later

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.anik.later.data.ScheduledMessage
import com.anik.later.send.Permissions
import com.anik.later.ui.EditorScreen
import com.anik.later.ui.HistoryScreen
import com.anik.later.ui.LaterTheme
import com.anik.later.ui.ScheduleListScreen
import com.anik.later.ui.ScheduleViewModel
import com.anik.later.ui.SetupScreen

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Permissions.notificationsAllowed(this)
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            LaterTheme {
                LaterNavHost()
            }
        }
    }
}

@Composable
private fun LaterNavHost() {
    val nav = rememberNavController()
    val vm: ScheduleViewModel = viewModel()
    val messages by vm.messages.collectAsState()
    val history by vm.history.collectAsState()

    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            ScheduleListScreen(
                messages = messages,
                onAdd = { nav.navigate("edit/0") },
                onOpen = { nav.navigate("edit/${it.id}") },
                onToggle = vm::setEnabled,
                onSetup = { nav.navigate("setup") },
                onHistory = { nav.navigate("history") },
            )
        }
        composable(
            route = "edit/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            var existing by remember { mutableStateOf<ScheduledMessage?>(null) }
            var loaded by remember { mutableStateOf(id == 0L) }
            LaunchedEffect(id) {
                if (id != 0L) {
                    existing = vm.load(id)
                    loaded = true
                }
            }
            if (loaded) {
                EditorScreen(
                    existing = existing,
                    onSave = {
                        vm.save(it)
                        nav.popBackStack()
                    },
                    onDelete = {
                        vm.delete(it)
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() },
                )
            }
        }
        composable("setup") {
            SetupScreen(onBack = { nav.popBackStack() })
        }
        composable("history") {
            HistoryScreen(
                attempts = history,
                onClear = vm::clearHistory,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
