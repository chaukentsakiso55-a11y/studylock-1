package com.cyberpulse.studylock.student

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiTutorClient {
    suspend fun ask(apiKey: String, prompt: String): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Add a Gemini API key first" }
        require(prompt.isNotBlank()) { "Enter a question first" }

        val connection = (URL("https://generativelanguage.googleapis.com/v1/interactions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey.trim())
        }

        val body = JSONObject()
            .put("model", "gemini-3.7-flash")
            .put("input", "You are StudyLock Live Tutor. Teach clearly, encourage independent thinking, and give age-appropriate study help. Student question: $prompt")
            .toString()

        connection.outputStream.bufferedWriter().use { it.write(body) }
        val responseText = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            .bufferedReader().use { it.readText() }
        if (connection.responseCode !in 200..299) error("Gemini request failed (${connection.responseCode}): $responseText")

        val response = JSONObject(responseText)
        val steps = response.optJSONArray("steps") ?: return@withContext "No tutor response returned."
        val parts = mutableListOf<String>()
        for (i in 0 until steps.length()) {
            val step = steps.optJSONObject(i) ?: continue
            if (step.optString("type") != "model_output") continue
            val content = step.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val item = content.optJSONObject(j) ?: continue
                if (item.optString("type") == "text") item.optString("text").takeIf { it.isNotBlank() }?.let(parts::add)
            }
        }
        parts.joinToString("\n").ifBlank { "No tutor response returned." }
    }
}
