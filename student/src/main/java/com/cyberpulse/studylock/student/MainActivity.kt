package com.cyberpulse.studylock.student

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var focusActive by remember { mutableStateOf(false) }
                var minutes by remember { mutableIntStateOf(25) }
                var apiKey by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF07111F))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("StudyLock Student", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text("Focus protection, Live Tutor and parent sync foundation", color = Color(0xFF9CB4CC))

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(if (focusActive) "Focus session active" else "Ready to study")
                            Text("Session length: $minutes minutes")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { if (minutes > 25) minutes -= 5 }, enabled = !focusActive) { Text("-5") }
                                Button(onClick = { if (minutes < 300) minutes += 5 }, enabled = !focusActive) { Text("+5") }
                            }
                            Button(onClick = { focusActive = !focusActive }) {
                                Text(if (focusActive) "End Focus" else "Start Focus")
                            }
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Live Tutor")
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("Gemini API key") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("API calls are intentionally not hard-coded into this public repository.")
                        }
                    }

                    Text("Default blocked apps: YouTube and Chrome", color = Color(0xFF9CB4CC))
                    Text("Pairing and remote control use the shared StudyLock data models.", color = Color(0xFF9CB4CC))
                }
            }
        }
    }
}
