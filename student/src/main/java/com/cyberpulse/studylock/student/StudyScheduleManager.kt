package com.cyberpulse.studylock.student

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

object StudyScheduleManager {
    fun scheduleDaily(context: Context, enabled: Boolean, hour: Int?, minute: Int?, studyMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StudyScheduleReceiver::class.java).putExtra("minutes", studyMinutes.coerceIn(25, 300))
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            5001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        if (!enabled || hour == null || minute == null) return

        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.timeInMillis, pendingIntent)
    }
}

class StudyScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val minutes = intent.getIntExtra("minutes", 25).coerceIn(25, 300)
        context.getSharedPreferences("studylock", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("focus_active", true)
            .putInt("scheduled_minutes", minutes)
            .apply()
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(launch)
        }
        val prefs = context.getSharedPreferences("studylock", Context.MODE_PRIVATE)
        StudyScheduleManager.scheduleDaily(
            context,
            prefs.getBoolean("auto_study", false),
            prefs.getInt("schedule_hour", -1).takeIf { it >= 0 },
            prefs.getInt("schedule_minute", -1).takeIf { it >= 0 },
            prefs.getInt("schedule_length", 25)
        )
    }
}
