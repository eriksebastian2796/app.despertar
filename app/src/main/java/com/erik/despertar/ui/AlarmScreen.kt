package com.erik.despertar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.erik.despertar.data.AlarmEntity
import com.erik.despertar.data.SleepConfigEntity
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel = hiltViewModel(),
    onSleepConfigClick: () -> Unit = {},
    onAddAlarmClick: () -> Unit = {},
    onEditAlarmClick: (Int) -> Unit = {}
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val sleepConfig by viewModel.sleepConfig.collectAsStateWithLifecycle()
    
    // Timer para refrescar los textos de "Suena en..." cada 30 segundos
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(30000)
            tick++
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlarmClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Alarma")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Alarmas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Card de Hora de Sueño
            item {
                SleepConfigCard(
                    config = sleepConfig ?: SleepConfigEntity(),
                    onClick = onSleepConfigClick,
                    onToggle = { config -> viewModel.toggleSleepAlarm(config) }
                )
            }

            item {
                Text(
                    text = "Tus alarmas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Lista de Alarmas
            items(alarms, key = { it.id }) { alarm ->
                // Pasamos 'tick' para forzar recomposición
                key(tick) {
                    AlarmItem(
                        alarm = alarm,
                        onToggle = { viewModel.toggleAlarm(alarm) },
                        onEdit = { onEditAlarmClick(alarm.id) },
                        onDelete = { viewModel.deleteAlarm(alarm) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun SleepConfigCard(
    config: SleepConfigEntity,
    onClick: () -> Unit,
    onToggle: (SleepConfigEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hora de dormir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val hours = calculateSleepDuration(config)
                Text(
                    text = "$hours horas de descanso hoy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = config.isAlarmOn,
                onCheckedChange = { onToggle(config) }
            )
        }
    }
}

@Composable
fun AlarmItem(
    alarm: AlarmEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeText = String.format(Locale.getDefault(), "%02d:%02d", alarm.hour, alarm.minute)
    val alpha = if (alarm.isEnabled) 1f else 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    )
                }
                Text(
                    text = timeText,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Text(
                    text = if (alarm.isEnabled) getTimeRemaining(alarm.hour, alarm.minute, alarm.repeatDays) else "Desactivada",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = formatDays(alarm.repeatDays),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = alpha)
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}

private fun getTimeRemaining(hour: Int, minute: Int, days: List<Int>): String {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (days.isEmpty()) {
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
    } else {
        val mappedDays = days.map { if (it == 7) Calendar.SUNDAY else it + 1 }
        var daysUntil = 0
        while (daysUntil < 8) {
            val checkDay = (now.get(Calendar.DAY_OF_WEEK) + daysUntil - 1) % 7 + 1
            if (mappedDays.contains(checkDay)) {
                if (daysUntil == 0 && target.after(now)) break
                else if (daysUntil > 0) break
            }
            daysUntil++
        }
        target.add(Calendar.DAY_OF_YEAR, daysUntil)
    }

    val diffMillis = target.timeInMillis - now.timeInMillis
    val diffHours = diffMillis / (1000 * 60 * 60)
    val diffMinutes = (diffMillis / (1000 * 60)) % 60

    val isTomorrow = target.get(Calendar.DAY_OF_YEAR) == (now.get(Calendar.DAY_OF_YEAR) + 1) % 366
            && target.get(Calendar.YEAR) >= now.get(Calendar.YEAR)

    return when {
        diffHours < 24 && !isTomorrow -> {
            val hText = if (diffHours > 0) "${diffHours}h " else ""
            "Suena en $hText${diffMinutes}m"
        }
        isTomorrow -> {
            val time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            "Suena mañana a las $time"
        }
        else -> {
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val dayName = dayFormat.format(target.time).replaceFirstChar { it.uppercase() }
            val time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            "Suena el $dayName a las $time"
        }
    }
}

private fun formatDays(days: List<Int>): String {
    if (days.isEmpty()) return "Solo una vez"
    if (days.size == 7) return "Todos los días"
    val dayNames = listOf("Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom")
    return days.sorted().joinToString(", ") { dayNames[it - 1] }
}

private fun calculateSleepDuration(config: SleepConfigEntity): Int {
    val bedtime = config.bedtimeHour * 60 + config.bedtimeMinute
    var wakeup = config.wakeupHour * 60 + config.wakeupMinute
    
    if (wakeup < bedtime) wakeup += 24 * 60
    
    return (wakeup - bedtime) / 60
}
