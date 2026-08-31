package com.cyberpulse.studylock.student

import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val repo = remember { StudentFirebase() }
                val tutor = remember { GeminiTutorClient() }
                val scope = rememberCoroutineScope()
                val prefs = remember { getSharedPreferences("studylock", MODE_PRIVATE) }
                var minutes by remember { mutableIntStateOf(25) }
                var focusActive by remember { mutableStateOf(prefs.getBoolean("focus_active", false)) }
                var blockedApps by remember { mutableStateOf(prefs.getStringSet("blocked_apps", setOf("com.google.android.youtube", "com.android.chrome"))!!.toList()) }
                var pairingCode by remember { mutableStateOf(prefs.getString("pairing_code", "").orEmpty()) }
                var pairingStatus by remember { mutableStateOf(if (pairingCode.isBlank()) "Not paired" else "Waiting for parent") }
                var apiKey by remember { mutableStateOf(prefs.getString("gemini_key", "").orEmpty()) }
                var question by remember { mutableStateOf("") }
                var answer by remember { mutableStateOf("") }
                var busy by remember { mutableStateOf(false) }
                var aiUsage by remember { mutableIntStateOf(prefs.getInt("ai_usage", 0)) }
                var listener by remember { mutableStateOf<ListenerRegistration?>(null) }
                var commandListener by remember { mutableStateOf<ListenerRegistration?>(null) }
                var configListener by remember { mutableStateOf<ListenerRegistration?>(null) }
                var tts by remember { mutableStateOf<TextToSpeech?>(null) }

                fun saveFocus(active: Boolean) {
                    focusActive = active
                    prefs.edit().putBoolean("focus_active", active).apply()
                }

                fun saveBlocked(packages: List<String>) {
                    blockedApps = packages.distinct()
                    prefs.edit().putStringSet("blocked_apps", blockedApps.toSet()).apply()
                }

                LaunchedEffect(Unit) {
                    repo.ensureSignedIn()
                    commandListener = repo.observeCommands(
                        onStart = { remoteMinutes -> minutes = remoteMinutes; saveFocus(true) },
                        onEnd = { saveFocus(false) },
                        onBlockedApps = { saveBlocked(it) }
                    )
                    configListener = repo.observeConfig { config ->
                        if (config.blockedApps.isNotEmpty()) saveBlocked(config.blockedApps)
                        minutes = config.defaultStudyMinutes.coerceIn(25, 300)
                    }
                    if (pairingCode.isNotBlank()) {
                        listener = repo.observePairing(pairingCode) { pairingStatus = "Paired and synced" }
                    }
                }

                LaunchedEffect(focusActive, minutes, blockedApps, aiUsage) {
                    runCatching {
                        repo.pushMetrics(
                            StudentMetrics(
                                totalStudyMinutes = prefs.getLong("study_minutes", 0),
                                aiUsageCount = aiUsage.toLong(),
                                activeFocusSession = focusActive,
                                currentSessionMinutes = if (focusActive) minutes else 0,
                                blockedApps = blockedApps
                            )
                        )
                    }
                }

                DisposableEffect(Unit) {
                    tts = TextToSpeech(this@MainActivity) { status ->
                        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
                    }
                    onDispose {
                        listener?.remove()
                        commandListener?.remove()
                        configListener?.remove()
                        tts?.shutdown()
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF07111F)).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("StudyLock Student", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text(if (focusActive) "Focus session active" else "Ready to study", color = Color(0xFF9CB4CC))

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Focus")
                            Text("$minutes minutes")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { minutes = (minutes - 5).coerceAtLeast(25) }, enabled = !focusActive) { Text("-5") }
                                Button(onClick = { minutes = (minutes + 5).coerceAtMost(300) }, enabled = !focusActive) { Text("+5") }
                                Button(onClick = { saveFocus(!focusActive) }) { Text(if (focusActive) "End" else "Start") }
                            }
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Parent Pairing")
                            Text(if (pairingCode.isBlank()) "No pairing code yet" else "Code: $pairingCode")
                            Text(pairingStatus)
                            Button(onClick = {
                                scope.launch {
                                    runCatching {
                                        listener?.remove()
                                        val code = repo.createPairingCode()
                                        pairingCode = code
                                        pairingStatus = "Waiting for parent"
                                        prefs.edit().putString("pairing_code", code).apply()
                                        listener = repo.observePairing(code) { pairingStatus = "Paired and synced" }
                                    }.onFailure { pairingStatus = it.message ?: "Pairing failed" }
                                }
                            }) { Text("Generate Pairing Code") }
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Live Tutor")
                            OutlinedTextField(apiKey, { apiKey = it; prefs.edit().putString("gemini_key", it).apply() }, label = { Text("Gemini API key") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(question, { question = it }, label = { Text("Ask a study question") }, modifier = Modifier.fillMaxWidth())
                            Button(enabled = !busy, onClick = {
                                scope.launch {
                                    busy = true
                                    answer = runCatching { tutor.ask(apiKey, question) }.getOrElse { it.message ?: "Tutor request failed" }
                                    if (!answer.startsWith("Gemini request failed")) {
                                        aiUsage += 1
                                        prefs.edit().putInt("ai_usage", aiUsage).apply()
                                    }
                                    busy = false
                                }
                            }) { Text(if (busy) "Thinking…" else "Ask Tutor") }
                            if (answer.isNotBlank()) {
                                Text(answer)
                                Button(onClick = { tts?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "studylock_tutor") }) { Text("Read Aloud") }
                            }
                        }
                    }

                    Text("Blocked apps: ${blockedApps.joinToString()}", color = Color(0xFF9CB4CC))
                    Text("Accessibility access must be enabled by the device user for app blocking to work.", color = Color(0xFF9CB4CC))
                }
            }
        }
    }
}
