package com.erik.despertar.ui

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erik.despertar.data.FavoriteApp
import com.erik.despertar.data.FavoriteDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    application: Application,
    private val favoriteDao: FavoriteDao
) : AndroidViewModel(application) {

    private val pm = application.packageManager

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    // Reactividad Real: Observamos el DAO y mapeamos a AppInfo
    val favorites: StateFlow<List<AppInfo>> = favoriteDao.getAllFavorites()
        .map { favoriteEntities ->
            favoriteEntities.mapNotNull { entity ->
                getAppInfoForPackage(entity.packageName)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private fun getAppInfoForPackage(packageName: String): AppInfo? {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            AppInfo(
                packageName = packageName,
                label = pm.getApplicationLabel(appInfo).toString(),
                intent = pm.getLaunchIntentForPackage(packageName),
                icon = pm.getApplicationIcon(appInfo)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun findDockApps(allApps: List<AppInfo>): List<AppInfo> {
        val dockPackages = listOf(
            listOf("dialer", "phone", "contacts"),
            listOf("messaging", "sms", "mms", "chat"),
            listOf("camera"),
            listOf("browser", "chrome", "firefox")
        )
        
        return dockPackages.mapNotNull { keywords ->
            allApps.find { app -> 
                keywords.any { app.packageName.lowercase().contains(it) || app.label.lowercase().contains(it) }
            }
        }.take(4)
    }

    fun addToFavorites(app: AppInfo) {
        viewModelScope.launch {
            // Límite de 6 favoritos
            val currentCount = favoriteDao.getAllFavorites().first().size
            if (currentCount < 6) {
                favoriteDao.insert(FavoriteApp(app.packageName))
            }
        }
    }

    fun removeFromFavorites(app: AppInfo) {
        viewModelScope.launch {
            favoriteDao.delete(FavoriteApp(app.packageName))
        }
    }

    fun setDrawerOpen(open: Boolean) {
        _isDrawerOpen.value = open
    }
}
