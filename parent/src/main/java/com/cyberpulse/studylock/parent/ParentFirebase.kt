package com.cyberpulse.studylock.parent

import com.cyberpulse.studylock.shared.StudentConfig
import com.cyberpulse.studylock.shared.StudentMetrics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ParentFirebase {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun ensureSignedIn(): String {
        auth.currentUser?.uid?.let { return it }
        return auth.signInAnonymously().await().user!!.uid
    }

    suspend fun joinPairing(code: String): String {
        val parentId = ensureSignedIn()
        val ref = db.collection("pairings").document(code.trim().uppercase())
        val snapshot = ref.get().await()
        val studentId = snapshot.getString("studentId") ?: error("Pairing code not found")
        val existingParent = snapshot.getString("parentId").orEmpty()
        require(existingParent.isBlank() || existingParent == parentId) { "Pairing code already used" }
        ref.update(mapOf("parentId" to parentId, "paired" to true)).await()
        return studentId
    }

    fun observeMetrics(studentId: String, onMetrics: (StudentMetrics) -> Unit): ListenerRegistration {
        return db.collection("students").document(studentId).collection("metrics").document("current")
            .addSnapshotListener { snapshot, _ -> snapshot?.toObject(StudentMetrics::class.java)?.let(onMetrics) }
    }

    suspend fun sendStartFocus(studentId: String, minutes: Int) = sendCommand(
        studentId,
        mapOf("type" to "START_FOCUS", "minutes" to minutes.coerceIn(25, 300), "processed" to false)
    )

    suspend fun sendEndFocus(studentId: String) = sendCommand(
        studentId,
        mapOf("type" to "END_FOCUS", "processed" to false)
    )

    suspend fun updateBlockedApps(studentId: String, packages: List<String>) {
        val parentId = ensureSignedIn()
        db.collection("students").document(studentId).collection("config").document("current")
            .set(StudentConfig(blockedApps = packages.distinct()))
            .await()
        sendCommand(
            studentId,
            mapOf("type" to "UPDATE_BLOCKED_APPS", "blockedApps" to packages.distinct(), "processed" to false, "parentId" to parentId)
        )
    }

    suspend fun updateSchedule(studentId: String, enabled: Boolean, hour: Int, minute: Int, studyMinutes: Int) {
        db.collection("students").document(studentId).collection("config").document("current")
            .set(
                StudentConfig(
                    autoStudyEnabled = enabled,
                    scheduledStartHour = hour.coerceIn(0, 23),
                    scheduledStartMinute = minute.coerceIn(0, 59),
                    defaultStudyMinutes = studyMinutes.coerceIn(25, 300)
                )
            ).await()
    }

    private suspend fun sendCommand(studentId: String, payload: Map<String, Any>) {
        val parentId = ensureSignedIn()
        val command = payload + mapOf(
            "id" to UUID.randomUUID().toString(),
            "parentId" to parentId,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("students").document(studentId).collection("commands").add(command).await()
    }
}
