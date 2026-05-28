package com.erik.despertar.ui

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class AppCategory(val label: String, val color: Color) {
    SOCIAL("Redes sociales", Color(0xFF2196F3)),
    ENTERTAINMENT("Entretenimiento", Color(0xFFFF9800)),
    PRODUCTIVITY("Productividad", Color(0xFF4CAF50)),
    MULTIMEDIA("Multimedia", Color(0xFF9C27B0)),
    OTHERS("Otros", Color(0xFF9E9E9E))
}

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long,
    val category: AppCategory,
    val icon: Drawable? = null
) {
    val timeFormatted: String
        get() = formatMillis(totalTimeInForeground)
}

fun formatMillis(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis / (1000 * 60)) % 60
    return "${hours}h ${minutes}m"
}

data class DailyUsage(
    val dayLabel: String,
    val usageByCategory: Map<AppCategory, Long>
) {
    val totalTime: Long get() = usageByCategory.values.sum()
}

data class UsageSummary(
    val averageDaily: Long = 0L,
    val maxUsage: Pair<String, Long> = "" to 0L,
    val minUsage: Pair<String, Long> = "" to 0L
)

enum class StatsFilter {
    TODAY, WEEK, MONTH
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _usageStats = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val usageStats: StateFlow<List<AppUsageInfo>> = _usageStats

    private val _dailyUsageData = MutableStateFlow<List<DailyUsage>>(emptyList())
    val dailyUsageData: StateFlow<List<DailyUsage>> = _dailyUsageData

    private val _currentFilter = MutableStateFlow(StatsFilter.TODAY)
    val currentFilter: StateFlow<StatsFilter> = _currentFilter

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    private val _totalTime = MutableStateFlow(0L)
    val totalTime: StateFlow<Long> = _totalTime

    private val _offset = MutableStateFlow(0)
    val offset: StateFlow<Int> = _offset

    private val _periodLabel = MutableStateFlow("")
    val periodLabel: StateFlow<String> = _periodLabel

    private val _summary = MutableStateFlow(UsageSummary())
    val summary: StateFlow<UsageSummary> = _summary

    init {
        checkPermission()
    }

    fun checkPermission() {
        val appOps = getApplication<Application>().getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            getApplication<Application>().packageName
        )
        val granted = mode == android.app.AppOpsManager.MODE_ALLOWED
        _hasPermission.value = granted
        if (granted) {
            loadUsageStats(_currentFilter.value, _offset.value)
        }
    }

    fun setFilter(filter: StatsFilter) {
        _currentFilter.value = filter
        _offset.value = 0
        if (_hasPermission.value) {
            loadUsageStats(filter, 0)
        }
    }

    fun navigatePeriod(delta: Int) {
        val newOffset = _offset.value + delta
        if (newOffset <= 0) {
            _offset.value = newOffset
            loadUsageStats(_currentFilter.value, newOffset)
        }
    }

    private fun loadUsageStats(filter: StatsFilter, offset: Int) {
        viewModelScope.launch {
            val periodBounds = getPeriodBounds(filter, offset)
            _periodLabel.value = formatPeriodLabel(filter, periodBounds.first, periodBounds.second)

            val stats = withContext(Dispatchers.IO) {
                getStatsForPeriod(periodBounds.first, periodBounds.second)
            }
            _usageStats.value = stats
            _totalTime.value = stats.sumOf { it.totalTimeInForeground }
            
            val dailyData = withContext(Dispatchers.IO) {
                getDetailedUsageData(filter, periodBounds.first, periodBounds.second)
            }
            _dailyUsageData.value = dailyData
            
            if (filter != StatsFilter.TODAY) {
                calculateSummary(dailyData)
            }
        }
    }

    private fun calculateSummary(data: List<DailyUsage>) {
        if (data.isEmpty()) return
        
        val validData = data.filter { it.totalTime > 0 }
        if (validData.isEmpty()) return

        val avg = data.sumOf { it.totalTime } / data.size
        val max = data.maxBy { it.totalTime }
        val min = validData.minBy { it.totalTime }

        _summary.value = UsageSummary(
            averageDaily = avg,
            maxUsage = max.dayLabel to max.totalTime,
            minUsage = min.dayLabel to min.totalTime
        )
    }

    private fun getPeriodBounds(filter: StatsFilter, offset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val startTime: Long
        val endTime: Long

        when (filter) {
            StatsFilter.TODAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, offset)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                endTime = calendar.timeInMillis - 1
            }
            StatsFilter.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                calendar.add(Calendar.WEEK_OF_YEAR, offset)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime = calendar.timeInMillis
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                endTime = calendar.timeInMillis - 1
            }
            StatsFilter.MONTH -> {
                calendar.add(Calendar.MONTH, offset)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                startTime = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                endTime = calendar.timeInMillis - 1
            }
        }
        
        val actualEndTime = if (endTime > System.currentTimeMillis()) System.currentTimeMillis() else endTime
        return Pair(startTime, actualEndTime)
    }

    private fun formatPeriodLabel(filter: StatsFilter, start: Long, end: Long): String {
        val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
        val sdfFull = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return when (filter) {
            StatsFilter.TODAY -> {
                val cal = Calendar.getInstance().apply { timeInMillis = start }
                val today = Calendar.getInstance()
                if (cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) &&
                    cal.get(Calendar.YEAR) == today.get(Calendar.YEAR)) "Hoy"
                else SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(start))
            }
            StatsFilter.WEEK -> "${sdf.format(Date(start))} - ${sdf.format(Date(end))}"
            StatsFilter.MONTH -> sdfFull.format(Date(start)).replaceFirstChar { it.uppercase() }
        }
    }

    private fun getCategory(packageName: String): AppCategory {
        val pName = packageName.lowercase()
        return when {
            pName.contains("instagram") || pName.contains("tiktok") || pName.contains("twitter") || 
            pName.contains("facebook") || pName.contains("snapchat") || pName.contains("whatsapp") -> AppCategory.SOCIAL
            
            pName.contains("youtube") || pName.contains("netflix") || pName.contains("twitch") || 
            pName.contains("spotify") -> AppCategory.ENTERTAINMENT
            
            pName.contains("docs") || pName.contains("sheets") || pName.contains("gmail") || 
            pName.contains("calendar") || pName.contains("office") -> AppCategory.PRODUCTIVITY
            
            pName.contains("camera") || pName.contains("gallery") || pName.contains("photos") ||
            pName.contains("video") || pName.contains("vlc") -> AppCategory.MULTIMEDIA
            
            else -> AppCategory.OTHERS
        }
    }

    private fun getStatsForPeriod(startTime: Long, endTime: Long): List<AppUsageInfo> {
        val context = getApplication<Application>()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        val pm = context.packageManager

        return stats.mapNotNull { (packageName, usageStats) ->
            if (usageStats.totalTimeInForeground > 0) {
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    AppUsageInfo(packageName, appName, usageStats.totalTimeInForeground, getCategory(packageName), icon)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            } else null
        }.sortedByDescending { it.totalTimeInForeground }
    }

    private fun getDetailedUsageData(filter: StatsFilter, startTime: Long, endTime: Long): List<DailyUsage> {
        val context = getApplication<Application>()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val result = mutableListOf<DailyUsage>()
        
        when (filter) {
            StatsFilter.TODAY -> {
                for (hour in 0..23) {
                    val cal = Calendar.getInstance().apply { 
                        timeInMillis = startTime
                        set(Calendar.HOUR_OF_DAY, hour)
                    }
                    val hStart = cal.timeInMillis
                    cal.add(Calendar.HOUR_OF_DAY, 1)
                    val hEnd = cal.timeInMillis
                    
                    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, hStart, hEnd)
                    val categoryUsage = aggregateByCategory(stats)
                    
                    // Solo poner label en las horas clave para el eje X del diseño
                    val label = when(hour) {
                        0 -> "12 AM"
                        6 -> "6 AM"
                        12 -> "12 PM"
                        18 -> "6 PM"
                        else -> ""
                    }
                    result.add(DailyUsage(label, categoryUsage))
                }
                // Añadir label final para el cierre del eje X si es necesario, 
                // o manejarlo en la UI. El requerimiento dice 5 etiquetas.
            }
            StatsFilter.WEEK -> {
                for (i in 0..6) {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = startTime
                        add(Calendar.DAY_OF_YEAR, i)
                    }
                    val dStart = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val dEnd = cal.timeInMillis
                    
                    val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dStart, dEnd)
                    val categoryUsage = aggregateByCategory(stats)
                    val labels = arrayOf("L", "M", "M", "J", "V", "S", "D")
                    result.add(DailyUsage(labels[i], categoryUsage))
                }
            }
            StatsFilter.MONTH -> {
                val cal = Calendar.getInstance().apply { timeInMillis = startTime }
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                
                // Agrupar por semanas
                var currentWeek = 1
                var weekStartTime = startTime
                val oneDayMillis = 24 * 60 * 60 * 1000L
                
                for (day in 1..daysInMonth) {
                    cal.timeInMillis = startTime
                    cal.add(Calendar.DAY_OF_YEAR, day - 1)
                    
                    // Si es lunes o el primer día del mes, empezamos cuenta? 
                    // El usuario pide "Sem 1", "Sem 2"...
                    // Vamos a agrupar cada 7 días para simplificar o por semanas calendario.
                    // Usemos bloques de 7 días.
                    if (day % 7 == 0 || day == daysInMonth) {
                        val periodEnd = cal.timeInMillis + oneDayMillis
                        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, weekStartTime, periodEnd)
                        val categoryUsage = aggregateByCategory(stats)
                        result.add(DailyUsage("Sem $currentWeek", categoryUsage))
                        
                        weekStartTime = periodEnd
                        currentWeek++
                    }
                }
            }
        }
        return result
    }

    private fun aggregateByCategory(stats: List<android.app.usage.UsageStats>): Map<AppCategory, Long> {
        val map = mutableMapOf<AppCategory, Long>()
        stats.forEach { 
            val cat = getCategory(it.packageName)
            map[cat] = map.getOrDefault(cat, 0L) + it.totalTimeInForeground
        }
        return map
    }
}
