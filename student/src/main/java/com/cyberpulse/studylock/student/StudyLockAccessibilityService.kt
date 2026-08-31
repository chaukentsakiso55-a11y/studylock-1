package com.cyberpulse.studylock.student

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class StudyLockAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val prefs = getSharedPreferences("studylock", MODE_PRIVATE)
        if (!prefs.getBoolean("focus_active", false)) return
        val blocked = prefs.getStringSet(
            "blocked_apps",
            setOf("com.google.android.youtube", "com.android.chrome")
        ).orEmpty()
        val packageName = event.packageName?.toString() ?: return
        if (packageName in blocked && packageName != applicationContext.packageName) {
            packageManager.getLaunchIntentForPackage(applicationContext.packageName)?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() = Unit
}
