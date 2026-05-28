package com.erik.despertar.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomBarScreen(
        route = "home",
        title = "Inicio",
        icon = Icons.Default.Home
    )

    object Alarm : BottomBarScreen(
        route = "alarm",
        title = "Alarma",
        icon = Icons.Default.Alarm
    )

    object Apps : BottomBarScreen(
        route = "apps",
        title = "Apps",
        icon = Icons.Default.PhoneAndroid
    )

    object Stats : BottomBarScreen(
        route = "stats",
        title = "Estadísticas",
        icon = Icons.Default.BarChart
    )

    object Settings : BottomBarScreen(
        route = "settings",
        title = "Ajustes",
        icon = Icons.Default.Settings
    )
}

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = BottomBarScreen.Home.route,
        modifier = modifier
    ) {
        composable(route = BottomBarScreen.Home.route) {
            HomeScreen()
        }
        composable(route = BottomBarScreen.Alarm.route) {
            AlarmScreen(onSleepConfigClick = {
                // Navegar a configuración de sueño cuando esté lista
            })
        }
        composable(route = BottomBarScreen.Apps.route) {
            AppsScreen()
        }
        composable(route = BottomBarScreen.Stats.route) {
            StatsScreen(onLimitClick = { packageName, appName ->
                val encodedAppName = java.net.URLEncoder.encode(appName, "UTF-8")
                navController.navigate("limit_config/$packageName/$encodedAppName")
            })
        }
        composable(route = BottomBarScreen.Settings.route) {
            SettingsScreen()
        }
        composable(
            route = "limit_config/{packageName}/{appName}",
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("appName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            val encodedAppName = backStackEntry.arguments?.getString("appName") ?: ""
            val appName = java.net.URLDecoder.decode(encodedAppName, "UTF-8")
            LimitConfigScreen(packageName, appName, navController)
        }
    }
}
