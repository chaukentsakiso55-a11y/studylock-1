package com.cyberpulse.studylock.shared

data class PairingSession(
    val code: String = "",
    val studentId: String = "",
    val parentId: String = "",
    val paired: Boolean = false
)

data class StudentMetrics(
    val studentId: String = "",
    val totalStudyMinutes: Long = 0,
    val aiUsageCount: Long = 0,
    val activeFocusSession: Boolean = false,
    val currentSessionMinutes: Int = 0,
    val blockedApps: List<String> = emptyList(),
    val lastUpdatedEpochMs: Long = 0
)

data class StudentConfig(
    val blockedApps: List<String> = listOf("com.google.android.youtube", "com.android.chrome"),
    val autoStudyEnabled: Boolean = false,
    val scheduledStartHour: Int? = null,
    val scheduledStartMinute: Int? = null,
    val defaultStudyMinutes: Int = 25
)

sealed class ParentCommand {
    data class StartFocus(val minutes: Int) : ParentCommand()
    data object EndFocus : ParentCommand()
    data class UpdateBlockedApps(val packages: List<String>) : ParentCommand()
    data class UpdateSchedule(val hour: Int, val minute: Int, val minutes: Int) : ParentCommand()
}
