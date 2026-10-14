package com.ritikqrscanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ritikqrscanner.app.data.SettingsManager
import com.ritikqrscanner.app.ui.QrAppTheme
import com.ritikqrscanner.app.ui.OnboardingDialog
import com.ritikqrscanner.app.ui.screens.GeneratorScreen
import com.ritikqrscanner.app.ui.screens.HistoryScreen
import com.ritikqrscanner.app.ui.screens.ScannerScreen
import com.ritikqrscanner.app.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val settingsManager = remember { SettingsManager(context) }
            val themeMode by settingsManager.themeModeFlow.collectAsState(initial = 2)
            val onboardingComplete by settingsManager.onboardingCompleteFlow.collectAsState(initial = true)
            val scope = rememberCoroutineScope()
            val isDarkTheme = when (themeMode) {
                0 -> false
                1 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            QrAppTheme(darkTheme = isDarkTheme) {
                ModernQRApp()
                if (!onboardingComplete) {
                    OnboardingDialog(
                        onComplete = { scope.launch { settingsManager.setOnboardingComplete(true) } }
                    )
                }
            }
        }
    }
}

private data class AppDestination(val route: String, val label: Int, val icon: ImageVector)

private val appDestinations = listOf(
    AppDestination("scanner", R.string.nav_scan, Icons.Default.QrCodeScanner),
    AppDestination("generator", R.string.nav_create, Icons.Default.QrCode2),
    AppDestination("history", R.string.nav_history, Icons.AutoMirrored.Filled.List),
    AppDestination("settings", R.string.nav_settings, Icons.Default.Tune)
)

@Composable
fun ModernQRApp() {
    val navController = rememberNavController()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (maxWidth >= 700.dp) {
            Row(Modifier.fillMaxSize()) {
                AppNavigationRail(navController)
                AppNavHost(navController, Modifier.weight(1f))
            }
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = { BottomNavigationBar(navController) }
            ) { innerPadding ->
                AppNavHost(navController, Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "scanner", modifier = modifier) {
        composable("scanner") { ScannerScreen() }
        composable("generator") { GeneratorScreen() }
        composable("history") { HistoryScreen() }
        composable("settings") { SettingsScreen() }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        appDestinations.forEach { destination ->
            val label = stringResource(destination.label)
            NavigationBarItem(
                icon = { Icon(destination.icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == destination.route,
                onClick = { navController.navigateTo(destination.route) }
            )
        }
    }
}

@Composable
private fun AppNavigationRail(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        appDestinations.forEach { destination ->
            val label = stringResource(destination.label)
            NavigationRailItem(
                icon = { Icon(destination.icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == destination.route,
                onClick = { navController.navigateTo(destination.route) },
                alwaysShowLabel = true
            )
        }
    }
}

private fun NavHostController.navigateTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
