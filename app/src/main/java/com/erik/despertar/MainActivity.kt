package com.erik.despertar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.erik.despertar.ui.BottomBarScreen
import com.erik.despertar.ui.LauncherScreen
import com.erik.despertar.ui.NavGraph
import com.erik.despertar.ui.theme.DespertarTheme
import com.erik.despertar.ui.LauncherViewModel
import com.erik.despertar.util.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AlarmScheduler.checkExactAlarmPermission(this)) {
            AlarmScheduler.requestExactAlarmPermission(this)
        }
        
        setContent {
            DespertarTheme {
                BatteryOptimizationCheck()
                if (isLauncherMode(intent)) {
                    LauncherScreen()
                } else {
                    MainScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // IMPORTANTE: Actualizar el intent de la actividad
        
        if (isLauncherMode(intent)) {
            val viewModel: LauncherViewModel by viewModels()
            viewModel.setDrawerOpen(false)
        }
    }

    private fun isLauncherMode(intent: Intent?): Boolean {
        return intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)
    }
}

@Composable
fun BatteryOptimizationCheck() {
    val context = LocalContext.current
    val pm = remember { context.getSystemService(PowerManager::class.java) }
    var showDialog by remember { 
        mutableStateOf(!pm.isIgnoringBatteryOptimizations(context.packageName)) 
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Aviso de optimización") },
            text = { 
                Text("Para que la alarma suene a tiempo, Despertar necesita estar excluida del ahorro de batería del sistema.") 
            },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Configurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Más tarde")
                }
            }
        )
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val screens = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Alarm,
        BottomBarScreen.Apps,
        BottomBarScreen.Stats,
        BottomBarScreen.Settings,
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        label = { Text(text = screen.title) },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavGraph(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}
