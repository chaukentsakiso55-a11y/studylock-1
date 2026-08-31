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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
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
                var pairingCode by remember { mutableStateOf("") }
                var status by remember { mutableStateOf("Not paired") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF07111F))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("StudyLock Parent", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text("Remote controls and student progress dashboard", color = Color(0xFF9CB4CC))

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Pair Student")
                            OutlinedTextField(
                                value = pairingCode,
                                onValueChange = { pairingCode = it.uppercase() },
                                label = { Text("Pairing code") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(onClick = { status = if (pairingCode.isBlank()) "Enter a pairing code" else "Ready to pair through backend" }) {
                                Text("Connect")
                            }
                            Text(status)
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Student Controls")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { status = "Start-focus command prepared" }) { Text("Start Focus") }
                                Button(onClick = { status = "End-focus command prepared" }) { Text("End Focus") }
                            }
                            Text("Study time: awaiting sync")
                            Text("AI usage: awaiting sync")
                            Text("Blocked apps: awaiting sync")
                        }
                    }
                }
            }
        }
    }
}
