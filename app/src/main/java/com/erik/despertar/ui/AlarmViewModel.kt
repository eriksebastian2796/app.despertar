package com.erik.despertar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.erik.despertar.data.AlarmDao
import com.erik.despertar.data.AlarmEntity
import com.erik.despertar.data.SleepConfigEntity
import com.erik.despertar.util.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    application: Application,
    private val alarmDao: AlarmDao
) : AndroidViewModel(application) {

    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepConfig: StateFlow<SleepConfigEntity?> = alarmDao.getSleepConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            alarmDao.updateAlarm(updated)
            AlarmScheduler.schedule(getApplication(), updated)
        }
    }

    fun saveAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            if (alarm.id == 0) {
                alarmDao.insertAlarm(alarm)
                // Después de insertar, necesitamos el ID para programarla. 
                // En un flujo real, el DAO devolvería el ID o lo observaríamos.
                // Por simplicidad, re-programamos todas las habilitadas o 
                // usamos el flow para detectar el cambio.
            } else {
                alarmDao.updateAlarm(alarm)
                AlarmScheduler.schedule(getApplication(), alarm)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), alarm)
            alarmDao.deleteAlarm(alarm)
        }
    }

    fun toggleSleepAlarm(config: SleepConfigEntity) {
        viewModelScope.launch {
            alarmDao.saveSleepConfig(config.copy(isAlarmOn = !config.isAlarmOn))
        }
    }

    fun saveSleepConfig(config: SleepConfigEntity) {
        viewModelScope.launch {
            alarmDao.saveSleepConfig(config)
        }
    }
    
    // Observar cambios en la lista para programar nuevas alarmas (ID generado por Room)
    init {
        alarms.onEach { list ->
            list.forEach { alarm ->
                if (alarm.isEnabled) {
                    AlarmScheduler.schedule(getApplication(), alarm)
                }
            }
        }.launchIn(viewModelScope)
    }
}
