package com.erik.despertar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ChallengeType {
    MATH, BARCODE, PHOTO, NONE
}

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDays: List<Int> = emptyList(), // 1=Lun, 7=Dom
    val challengeType: ChallengeType = ChallengeType.NONE,
    val difficulty: Int = 1,
    val problemsCount: Int = 1,
    val soundUri: String? = null,
    val vibrate: Boolean = true
)

@Entity(tableName = "sleep_config")
data class SleepConfigEntity(
    @PrimaryKey val id: Int = 1, // Fila única
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val wakeupHour: Int = 7,
    val wakeupMinute: Int = 0,
    val activeDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val isAlarmOn: Boolean = true,
    val warmFilterActive: Boolean = false
)
