package com.erik.despertar.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppInfo(
    val packageName: String,
    val label: String,
    val intent: Intent? = null,
    val icon: Drawable? = null
)

@HiltViewModel
class LauncherViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _favorites = MutableStateFlow<List<AppInfo>>(emptyList())
    val favorites: StateFlow<List<AppInfo>> = _favorites.asStateFlow()

    private val _dockApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val dockApps: StateFlow<List<AppInfo>> = _dockApps.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

            val blackList = listOf(
                context.packageName,
                "com.android.systemui",
                "com.android.settings"
            )

            val apps = pm.queryIntentActivities(mainIntent, 0).map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                AppInfo(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(pm).toString(),
                    intent = pm.getLaunchIntentForPackage(packageName),
                    icon = try { pm.getApplicationIcon(packageName) } catch (e: Exception) { null }
                )
            }.filter { it.packageName !in blackList }
             .sortedBy { it.label.lowercase() }

            _installedApps.value = apps
            _dockApps.value = findDockApps(apps)
        }
    }

    private fun findDockApps(allApps: List<AppInfo>): List<AppInfo> {
        val dockPackages = listOf(
            listOf("dialer", "phone", "contacts"),     // Phone
            listOf("messaging", "sms", "mms", "chat"), // Messages
            listOf("camera"),                         // Camera
            listOf("browser", "chrome", "firefox")     // Browser
        )
        
        return dockPackages.mapNotNull { keywords ->
            allApps.find { app -> 
                keywords.any { app.packageName.lowercase().contains(it) || app.label.lowercase().contains(it) }
            }
        }.take(4)
    }

    fun addToFavorites(app: AppInfo) {
        if (_favorites.value.size < 6 && _favorites.value.none { it.packageName == app.packageName }) {
            _favorites.value = _favorites.value + app
        }
    }

    fun removeFromFavorites(app: AppInfo) {
        _favorites.value = _favorites.value.filter { it.packageName != app.packageName }
    }

    fun setDrawerOpen(open: Boolean) {
        _isDrawerOpen.value = open
    }
}
