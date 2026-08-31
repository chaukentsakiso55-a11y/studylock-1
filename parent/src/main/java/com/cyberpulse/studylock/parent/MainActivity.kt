package com.cyberpulse.studylock.parent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cyberpulse.studylock.shared.StudentMetrics
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val repo = remember { ParentFirebase() }
                val scope = rememberCoroutineScope()
                val prefs = remember { getSharedPreferences("studylock_parent", MODE_PRIVATE) }
                var pairingCode by remember { mutableStateOf("") }
                var studentId by remember { mutableStateOf(prefs.getString("student_id", "").orEmpty()) }
                var status by remember { mutableStateOf(if (studentId.isBlank()) "Not paired" else "Paired") }
                var metrics by remember { mutableStateOf(StudentMetrics()) }
                var metricsListener by remember { mutableStateOf<ListenerRegistration?>(null) }
                var focusMinutes by remember { mutableIntStateOf(25) }
                var blockedText by remember { mutableStateOf("com.google.android.youtube, com.android.chrome") }
                var autoStudy by remember { mutableStateOf(false) }
                var scheduleHour by remember { mutableStateOf("18") }
                var scheduleMinute by remember { mutableStateOf("00") }

                fun attachMetrics(id: String) {
                    metricsListener?.remove()
                    metricsListener = repo.observeMetrics(id) { metrics = it }
                }

                DisposableEffect(studentId) {
                    if (studentId.isNotBlank()) attachMetrics(studentId)
                    onDispose { metricsListener?.remove() }
                }

                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF07111F)).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("StudyLock Parent", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text("Paired oversight and remote study controls", color = Color(0xFF9CB4CC))

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Pair Student")
                            OutlinedTextField(pairingCode, { pairingCode = it.uppercase().take(6) }, label = { Text("6-character code") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                scope.launch {
                                    status = "Connecting…"
                                    runCatching { repo.joinPairing(pairingCode) }
                                        .onSuccess { id ->
                                            studentId = id
                                            prefs.edit().putString("student_id", id).apply()
                                            status = "Pair request accepted — waiting for Student sync"
                                            attachMetrics(id)
                                        }
                                        .onFailure { status = it.message ?: "Pairing failed" }
                                }
                            }) { Text("Connect") }
                            Text(status)
                        }
                    }

                    if (studentId.isNotBlank()) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Student Status")
                                Text("Focus active: ${metrics.activeFocusSession}")
                                Text("Current session: ${metrics.currentSessionMinutes} minutes")
                                Text("Total study: ${metrics.totalStudyMinutes} minutes")
                                Text("AI tutor uses: ${metrics.aiUsageCount}")
                                Text("Blocked apps: ${metrics.blockedApps.joinToString()}")
                            }
                        }

                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Remote Focus")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { focusMinutes = (focusMinutes - 5).coerceAtLeast(25) }) { Text("-5") }
                                    Text("$focusMinutes min", modifier = Modifier.padding(top = 12.dp))
                                    Button(onClick = { focusMinutes = (focusMinutes + 5).coerceAtMost(300) }) { Text("+5") }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { scope.launch { runCatching { repo.sendStartFocus(studentId, focusMinutes) }.onFailure { status = it.message ?: "Command failed" } } }) { Text("Start Focus") }
                                    Button(onClick = { scope.launch { runCatching { repo.sendEndFocus(studentId) }.onFailure { status = it.message ?: "Command failed" } } }) { Text("End Focus") }
                                }
                            }
                        }

                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Blocked Apps")
                                OutlinedTextField(blockedText, { blockedText = it }, label = { Text("Package names, comma-separated") }, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    val packages = blockedText.split(',').map { it.trim() }.filter { it.isNotBlank() }
                                    scope.launch { runCatching { repo.updateBlockedApps(studentId, packages) }.onFailure { status = it.message ?: "Update failed" } }
                                }) { Text("Update Block List") }
                            }
                        }

                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Daily schedule", modifier = Modifier.padding(top = 12.dp))
                                    Switch(checked = autoStudy, onCheckedChange = { autoStudy = it })
                                }
                                OutlinedTextField(scheduleHour, { scheduleHour = it.filter(Char::isDigit).take(2) }, label = { Text("Hour 0-23") })
                                OutlinedTextField(scheduleMinute, { scheduleMinute = it.filter(Char::isDigit).take(2) }, label = { Text("Minute 0-59") })
                                Button(onClick = {
                                    val h = scheduleHour.toIntOrNull() ?: 18
                                    val m = scheduleMinute.toIntOrNull() ?: 0
                                    scope.launch { runCatching { repo.updateSchedule(studentId, autoStudy, h, m, focusMinutes) }.onFailure { status = it.message ?: "Schedule update failed" } }
                                }) { Text("Save Schedule") }
                            }
                        }
                    }
                }
            }
        }
    }
}
