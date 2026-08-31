package com.cyberpulse.studylock.student

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class StudyLockAccessibilityService : AccessibilityService() {
    private val blockedPackages = setOf(
        "com.google.android.youtube",
        "com.android.chrome"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (blockedPackages.contains(packageName)) {
            val launchIntent = packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (launchIntent != null) startActivity(launchIntent)
        }
    }

    override fun onInterrupt() = Unit
}
