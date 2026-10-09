package com.modernqr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.modernqr.app.ui.screens.*

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import com.modernqr.app.data.SettingsManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager(context) }
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = 2)
            
            val isDarkTheme = when (themeMode) {
                0 -> false
                1 -> true
                else -> isSystemInDarkTheme()
            }

            val darkColors = darkColorScheme(
                background = androidx.compose.ui.graphics.Color(0xFF000000),
                surface = androidx.compose.ui.graphics.Color(0xFF161A1D),
                surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1F2429),
                primary = androidx.compose.ui.graphics.Color(0xFF90C2F9),
                onPrimary = androidx.compose.ui.graphics.Color(0xFF000000)
            )
            
            val lightColors = lightColorScheme(
                background = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                surface = androidx.compose.ui.graphics.Color(0xFFF7F9FC),
                surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE9EEF5),
                primary = androidx.compose.ui.graphics.Color(0xFF006494),
                onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
            )

            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColors else lightColors
            ) {
                ModernQRApp()
            }
        }
    }
}

@Composable
fun ModernQRApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "scanner",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("scanner") { ScannerScreen() }
            composable("generator") { GeneratorScreen() }
            composable("history") { HistoryScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf("scanner", "generator", "history", "settings")
    val icons = listOf(Icons.Default.Search, Icons.Default.Create, Icons.AutoMirrored.Filled.List, Icons.Default.Settings)
    val labels = listOf("Scan", "Create", "History", "Settings")

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEachIndexed { index, screen ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = labels[index]) },
                label = { Text(labels[index]) },
                selected = currentRoute == screen,
                onClick = {
                    navController.navigate(screen) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
