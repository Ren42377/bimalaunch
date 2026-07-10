package com.ren42377.bimalaunch.practice

import org.json.JSONArray
import org.json.JSONObject

data class PracticeQuestion(
    val text: String = "Teks soal 1",
    val options: List<String> = listOf("Pilihan A", "Pilihan B", "Pilihan C", "Pilihan D", "Pilihan E")
) {
    fun normalized(index: Int): PracticeQuestion {
        val trimmedOptions = options.take(5).toMutableList()
        while (trimmedOptions.size < 5) {
            trimmedOptions.add("Pilihan ${('A' + trimmedOptions.size)}")
        }
        val cleanedOptions = trimmedOptions.mapIndexed { i, value ->
            value.trim().ifBlank { "Pilihan ${('A' + i)}" }
        }
        val cleanedText = text.trim().ifBlank { "Teks soal ${index + 1}" }
        return PracticeQuestion(cleanedText, cleanedOptions)
    }
}

data class PracticeConfig(
    val username: String = "user",
    val fullName: String = "Student",
    val subject: String = "Simulasi Ujian",
    val subjectCode: String = "SIMULASI-UJIAN",
    val examTitle: String = "Simulasi ujian",
    val subtest: String = "SIMULASI UJIAN",
    val testDate: String = "0000-00-00",
    val testTime: String = "00:00",
    val status: String = "AKTIF",
    val useToken: Boolean = false,
    val sessionToken: String = "",
    val durationMinutes: Int = 0,
    val questions: List<PracticeQuestion> = listOf(PracticeQuestion()),
    val dashboardAdmin: Boolean = false
) {
    fun normalized(): PracticeConfig {
        val cleanedQuestions = (questions.ifEmpty { listOf(PracticeQuestion()) })
            .take(MAX_QUESTIONS)
            .mapIndexed { index, question -> question.normalized(index) }
        val cleanedTestTime = testTime.trim().ifBlank { "00:00" }
        val duration = durationMinutes.coerceIn(0, 240).takeIf { it > 0 }
            ?: durationMinutesFromTime(cleanedTestTime).coerceIn(0, 240)
        return copy(
            username = username.trim().ifBlank { "user" },
            fullName = fullName.trim().ifBlank { "Student" },
            subject = subject.trim().ifBlank { "Simulasi Ujian" },
            subjectCode = subjectCode.trim().uppercase().ifBlank { "SIMULASI-UJIAN" },
            examTitle = examTitle.trim().ifBlank { "Simulasi ujian" },
            subtest = subtest.trim().ifBlank { "SIMULASI UJIAN" },
            testDate = testDate.trim().ifBlank { "0000-00-00" },
            testTime = cleanedTestTime,
            status = "AKTIF",
            durationMinutes = duration,
            questions = cleanedQuestions
        )
    }

    fun toJson(): String {
        val normalized = normalized()
        val obj = JSONObject()
        obj.put("username", normalized.username)
        obj.put("fullName", normalized.fullName)
        obj.put("subject", normalized.subject)
        obj.put("subjectCode", normalized.subjectCode)
        obj.put("examTitle", normalized.examTitle)
        obj.put("subtest", normalized.subtest)
        obj.put("testDate", normalized.testDate)
        obj.put("testTime", normalized.testTime)
        obj.put("status", normalized.status)
        obj.put("useToken", normalized.useToken)
        obj.put("sessionToken", normalized.sessionToken)
        obj.put("durationMinutes", normalized.durationMinutes)
        obj.put("dashboardAdmin", normalized.dashboardAdmin)
        val questionsArray = JSONArray()
        normalized.questions.forEach { question ->
            val questionObj = JSONObject()
            questionObj.put("text", question.text)
            questionObj.put("options", JSONArray(question.options))
            questionsArray.put(questionObj)
        }
        obj.put("questions", questionsArray)
        return obj.toString()
    }

    companion object {
        const val MAX_QUESTIONS = 100
        const val DEFAULT_ADMIN_USERNAME = "admin"
        const val DEFAULT_ADMIN_PASSWORD = "admin123"

        fun fromJson(raw: String?): PracticeConfig {
            if (raw.isNullOrBlank()) return PracticeConfig().normalized()
            val parsed = runCatching { JSONObject(raw) }.getOrNull() ?: return PracticeConfig().normalized()
            val questions = questionsFromJson(parsed.optJSONArray("questions"))
            return PracticeConfig(
                username = parsed.optString("username", "user"),
                fullName = parsed.optString("fullName", "Student"),
                subject = parsed.optString("subject", "Simulasi Ujian"),
                subjectCode = parsed.optString("subjectCode", "SIMULASI-UJIAN"),
                examTitle = parsed.optString("examTitle", "Simulasi ujian"),
                subtest = parsed.optString("subtest", "SIMULASI UJIAN"),
                testDate = parsed.optString("testDate", "0000-00-00"),
                testTime = parsed.optString("testTime", "00:00"),
                status = "AKTIF",
                useToken = parsed.optBoolean("useToken", false),
                sessionToken = parsed.optString("sessionToken", ""),
                durationMinutes = parsed.optInt("durationMinutes", 0),
                questions = questions,
                dashboardAdmin = parsed.optBoolean("dashboardAdmin", false)
            ).normalized()
        }

        private fun questionsFromJson(array: JSONArray?): List<PracticeQuestion> {
            if (array == null) return listOf(PracticeQuestion())
            val result = mutableListOf<PracticeQuestion>()
            for (i in 0 until array.length().coerceAtMost(MAX_QUESTIONS)) {
                val obj = array.optJSONObject(i) ?: continue
                val text = obj.optString("text", "Teks soal ${i + 1}")
                val optionsArray = obj.optJSONArray("options")
                val options = optionsFromJson(optionsArray)
                result.add(PracticeQuestion(text, options))
            }
            return result.ifEmpty { listOf(PracticeQuestion()) }
        }

        private fun optionsFromJson(array: JSONArray?): List<String> {
            if (array == null) return listOf("Pilihan A", "Pilihan B", "Pilihan C", "Pilihan D", "Pilihan E")
            val result = mutableListOf<String>()
            for (i in 0 until array.length().coerceAtMost(5)) {
                result.add(array.optString(i, "Pilihan ${('A' + i)}"))
            }
            return result
        }

        private fun durationMinutesFromTime(value: String): Int {
            val digits = value.filter { it.isDigit() }.take(4).padStart(4, '0')
            val hours = digits.substring(0, 2).toIntOrNull() ?: 0
            val minutes = (digits.substring(2, 4).toIntOrNull() ?: 0).coerceIn(0, 59)
            return hours * 60 + minutes
        }
    }
}
