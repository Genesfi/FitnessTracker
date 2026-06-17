package com.example.data

import com.example.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiClient {

    private var currentKeyIndex = 0

    private fun getApiKeys(): List<String> {
        val keys = mutableListOf<String>()

        fun addKeyIfValid(keyName: String) {
            try {
                val field = BuildConfig::class.java.getField(keyName)
                val value = field.get(null) as? String
                if (!value.isNullOrBlank() && value != "MY_GEMINI_API_KEY") {
                    keys.add(value)
                }
            } catch (e: Exception) {
                // Key not found in BuildConfig
            }
        }

        addKeyIfValid("GEMINI_API_KEY")
        for (i in 2..10) {
            addKeyIfValid("GEMINI_API_KEY_$i")
        }

        return keys
    }

    /**
     * Generic chat with Coach Gemini with Automatic Key Rotation and Model Fallback.
     */
    suspend fun chatWithCoach(prompt: String): String = withContext(Dispatchers.IO) {
        val keys = getApiKeys()
        if (keys.isEmpty()) {
            return@withContext "Halo! API Key Gemini belum dikonfigurasi. Silakan tambahkan di secrets.properties."
        }

        var lastError: Exception? = null

        // Model hierarchy: Primary is 3.5-flash, Fallback is 2.5-flash then 1.5-flash
        val modelsToTry = listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-1.5-flash")

        // Try each key
        for (i in keys.indices) {
            val index = (currentKeyIndex + i) % keys.size
            val apiKey = keys[index]

            // For each key, try models in order
            for (modelName in modelsToTry) {
                try {
                    val model = GenerativeModel(
                        modelName = modelName,
                        apiKey = apiKey
                    )
                    val response = model.generateContent(prompt)
                    val text = response.text
                    if (text != null) {
                        currentKeyIndex = index // Success, keep using this key
                        return@withContext text
                    }
                } catch (e: Exception) {
                    lastError = e
                    val errorMsg = e.localizedMessage ?: ""
                    
                    // If high demand (503), quota (429), or serialization/grpc errors, try next model or next key
                    if (errorMsg.contains("503") || errorMsg.contains("demand", ignoreCase = true) ||
                        errorMsg.contains("429") || errorMsg.contains("quota", ignoreCase = true) ||
                        errorMsg.contains("serialization", ignoreCase = true) ||
                        errorMsg.contains("grpc", ignoreCase = true)) {
                        continue // Try next model for this key, or if last model, it will move to next key
                    } else {
                        // Other errors (like 400 Bad Request), break model loop to try next key
                        break 
                    }
                }
            }
        }

        return@withContext "Gagal menghubungi Coach Gemini (Jalur Padat/Quota Habis): ${lastError?.localizedMessage}"
    }

    /**
     * Legacy method kept for compatibility, now redirects to rotated chatWithCoach.
     */
    suspend fun getPersonalAdvice(
        legCount: Int,
        pushCount: Int,
        pullCount: Int,
        logs: List<String>
    ): String = chatWithCoach("Data: Leg $legCount, Push $pushCount, Pull $pullCount. Analisis!")
}