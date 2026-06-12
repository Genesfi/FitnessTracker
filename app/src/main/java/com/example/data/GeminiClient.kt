package com.example.data

import com.example.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiClient {

    /**
     * Call Gemini to analyze Gusti's workout habits and return personal advice.
     */
    suspend fun getPersonalAdvice(
        legCount: Int,
        pushCount: Int,
        pullCount: Int,
        logs: List<String>
    ): String = withContext(Dispatchers.IO) {

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Halo Gusti! API Key Gemini belum dikonfigurasi melalui panel Secrets."
        }

        val formattedLogs = if (logs.isEmpty()) {
            "Gusti belum mencatat aktivitas latihan harian."
        } else {
            logs.joinToString("\n") { "- $it" }
        }

        val prompt = """
            Kamu adalah AI Workout Advisor & Personal Fitness Coach untuk Gusti.
            Penganalisis pola akumulasi latihan Gusti secara personal.
            
            Data Papan Skor Akumulasi Latihan Gusti saat ini:
            - Leg Day (Kaki): $legCount Kali (🦵)
            - Push Day (Dada/Bahu/Trisep): $pushCount Kali (💪)
            - Pull Day (Punggung/Bisep): $pullCount Kali (🦾)
            
            Catatan Progress & Riwayat Aktivitas Gusti:
            $formattedLogs
            
            Tugasmu:
            1. Analisis rasio keseimbangan latihan Gusti (Leg vs Push vs Pull). Apakah latihannya seimbang?
            2. Berikan evaluasi singkat, saran porsi latihan berikutnya yang personal, dan bimbingan gerakan.
            3. Berikan saran jam & frekuensi pengingat latihan yang tepat.
            4. Tulis pesan penutup yang sangat memotivasi dan ramah khas pelatih gym untuk Gusti. Panggil dia "Gusti".
            
            Tulis seluruh respon dalam Bahasa Indonesia terstruktur dengan apik, mudah dibaca, menggunakan bullet points, padat gizi fitnes, dan bersahabat.
        """.trimIndent()

        return@withContext chatWithCoach(prompt)
    }

    /**
     * Generic chat with Coach Gemini for AI Coach tab - JALUR GRATIS MURNI GOOGLE AI STUDIO.
     */
    suspend fun chatWithCoach(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Halo Gusti! API Key Gemini belum dikonfigurasi."
        }

        try {
            // Menggunakan SDK Google AI Murni (Tembus langsung pakai API Key .env tanpa lewat gerbang Vertex/Billing Firebase!)
            val model = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey
            )

            val response = model.generateContent(prompt)
            return@withContext response.text ?: "Tidak menerima saran teks dari Coach Gemini."
        } catch (e: Exception) {
            return@withContext "Gagal menghubungi Coach Gemini via SDK: ${e.localizedMessage}"
        }
    }
}