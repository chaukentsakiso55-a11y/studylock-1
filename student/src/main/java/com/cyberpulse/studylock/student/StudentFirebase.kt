package com.cyberpulse.studylock.student

import android.content.Context
import com.cyberpulse.studylock.shared.StudentConfig
import com.cyberpulse.studylock.shared.StudentMetrics
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class StudentFirebase {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val random = SecureRandom()

    suspend fun ensureSignedIn(): String {
        auth.currentUser?.uid?.let { return it }
        return auth.signInAnonymously().await().user!!.uid
    }

    suspend fun createPairingCode(): String {
        val uid = ensureSignedIn()
        val code = buildString {
            repeat(6) { append("ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[random.nextInt(32)]) }
        }
        db.collection("pairings").document(code).set(
            mapOf(
                "studentId" to uid,
                "parentId" to "",
                "paired" to false,
                "confirmed" to false,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        db.collection("students").document(uid).set(
            mapOf("ownerId" to uid, "parentId" to ""),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
        return code
    }

    fun observePairing(code: String, onPaired: (String) -> Unit): ListenerRegistration {
        val ref = db.collection("pairings").document(code)
        return ref.addSnapshotListener { snapshot, _ ->
            val parentId = snapshot?.getString("parentId").orEmpty()
            if (parentId.isNotBlank() && snapshot?.getBoolean("paired") == true) {
                val uid = auth.currentUser?.uid ?: return@addSnapshotListener
                db.collection("students").document(uid)
                    .set(
                        mapOf("ownerId" to uid, "parentId" to parentId),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        ref.update("confirmed", true)
                        onPaired(parentId)
                    }
            }
        }
    }

    suspend fun pushMetrics(metrics: StudentMetrics) {
        val uid = ensureSignedIn()
        db.collection("students").document(uid).collection("metrics").document("current")
            .set(metrics.copy(studentId = uid, lastUpdatedEpochMs = System.currentTimeMillis()))
            .await()
    }

    fun observeConfig(onConfig: (StudentConfig) -> Unit): ListenerRegistration {
        val uid = auth.currentUser!!.uid
        return db.collection("students").document(uid).collection("config").document("current")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.toObject(StudentConfig::class.java)?.let { config ->
                    val context = FirebaseApp.getInstance().applicationContext
                    val prefs = context.getSharedPreferences("studylock", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putStringSet("blocked_apps", config.blockedApps.toSet())
                        .putBoolean("auto_study", config.autoStudyEnabled)
                        .putInt("schedule_hour", config.scheduledStartHour ?: -1)
                        .putInt("schedule_minute", config.scheduledStartMinute ?: -1)
                        .putInt("schedule_length", config.defaultStudyMinutes.coerceIn(25, 300))
                        .apply()
                    StudyScheduleManager.scheduleDaily(
                        context,
                        config.autoStudyEnabled,
                        config.scheduledStartHour,
                        config.scheduledStartMinute,
                        config.defaultStudyMinutes
                    )
                    onConfig(config)
                }
            }
    }

    fun observeCommands(onStart: (Int) -> Unit, onEnd: () -> Unit, onBlockedApps: (List<String>) -> Unit): ListenerRegistration {
        val uid = auth.currentUser!!.uid
        return db.collection("students").document(uid).collection("commands")
            .whereEqualTo("processed", false)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.documents?.forEach { doc ->
                    when (doc.getString("type")) {
                        "START_FOCUS" -> onStart((doc.getLong("minutes") ?: 25L).toInt().coerceIn(25, 300))
                        "END_FOCUS" -> onEnd()
                        "UPDATE_BLOCKED_APPS" -> onBlockedApps((doc.get("blockedApps") as? List<*>)?.filterIsInstance<String>().orEmpty())
                    }
                    doc.reference.update("processed", true)
                }
            }
    }
}
