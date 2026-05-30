package com.erik.despertar.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.erik.despertar.data.AlarmEntity
import java.util.*

object AlarmScheduler {
    private const val TAG = "AlarmDebug"

    fun schedule(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // El Intent debe apuntar al Receiver
        // Intent 1: El que dispara el Receiver (operación)
        val receiverIntent = Intent("com.erik.despertar.ALARM_ACTION").apply {
            setPackage(context.packageName)
            putExtra("ALARM_ID", alarm.id)
        }

        val operationIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent 2: El que abre la UI al tocar el ícono de alarma (visual)
        val uiIntent = Intent(context, com.erik.despertar.MainActivity::class.java)
        val showIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            uiIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!alarm.isEnabled) {
            Log.d(TAG, "Cancelando alarma ID: ${alarm.id}")
            alarmManager.cancel(operationIntent)
            return
        }

        // CÁLCULO EXACTO DE TIEMPO
        val now = System.currentTimeMillis()
        val targetMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si el tiempo objetivo ya pasó (o es exactamente ahora), programamos para mañana
        if (targetMillis.timeInMillis <= now) {
            targetMillis.add(Calendar.DAY_OF_MONTH, 1)
        }

        val triggerTime = targetMillis.timeInMillis
        val diffSeg = (triggerTime - now) / 1000
        Log.d(TAG, "Ahora: ${Date(now)}, Alarma: ${Date(triggerTime)}, Diff: ${diffSeg}seg")
        
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, operationIntent)
    }

    fun cancel(context: Context, alarm: AlarmEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent("com.erik.despertar.ALARM_ACTION").apply {
            setPackage(context.packageName)
            putExtra("ALARM_ID", alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarma ID: ${alarm.id} cancelada manualmente")
    }

    fun checkExactAlarmPermission(context: Context): Boolean {
        // Pedir permiso solo en Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
        }
    }
}
