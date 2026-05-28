package com.erik.despertar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erik.despertar.data.AlarmDao
import com.erik.despertar.data.AlarmEntity
import com.erik.despertar.data.SleepConfigEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmDao: AlarmDao
) : ViewModel() {

    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepConfig: StateFlow<SleepConfigEntity?> = alarmDao.getSleepConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmDao.updateAlarm(alarm.copy(isEnabled = !alarm.isEnabled))
        }
    }

    fun addAlarm(hour: Int, minute: Int) {
        viewModelScope.launch {
            alarmDao.insertAlarm(AlarmEntity(hour = hour, minute = minute))
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
}
