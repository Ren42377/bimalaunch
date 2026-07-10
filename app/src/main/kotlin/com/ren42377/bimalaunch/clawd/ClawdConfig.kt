package com.ren42377.bimalaunch.clawd

import com.ren42377.bimalaunch.core.NativeBridge
import org.json.JSONObject

enum class ClawdRuntime(val id: String) {
    CPU("cpu"),
    GPU("gpu");

    companion object {
        fun fromId(value: String?): ClawdRuntime = entries.firstOrNull { it.id == value } ?: CPU
    }
}

data class ClawdDetectorConfig(
    val modelAsset: String,
    val labels: List<String>,
    val confidenceThreshold: Float,
    val nmsIouThreshold: Float,
    val globalDuplicateIouThreshold: Float,
    val runtime: ClawdRuntime,
    val cpuThreads: Int,
    val alwaysOnDefault: Boolean,
    val drawBoxDefault: Boolean,
    val pollIntervalMs: Long
) {
    companion object {
        val FALLBACK = ClawdDetectorConfig(
            modelAsset = "clawd/clawd_v2_f16.tflite",
            labels = listOf(
                "question_area", "a", "b", "c", "d", "e",
                "button_prev", "button_next", "question_list_button",
                "question_list_popup_close", "question_number",
                "question_number_answered", "question_number_unanswered",
                "question_number_current"
            ),
            confidenceThreshold = 0.25f,
            nmsIouThreshold = 0.45f,
            globalDuplicateIouThreshold = 0.38f,
            runtime = ClawdRuntime.CPU,
            cpuThreads = 2,
            alwaysOnDefault = false,
            drawBoxDefault = false,
            pollIntervalMs = 1500L
        )

        fun fromNative(): ClawdDetectorConfig {
            val response = runCatching {
                JSONObject(NativeBridge.dispatch("clawd_detector_config", "{}"))
            }.getOrNull() ?: return FALLBACK
            val data = response.optJSONObject("data") ?: return FALLBACK
            val labelsArray = data.optJSONArray("labels")
            val labels = buildList {
                if (labelsArray != null) {
                    for (i in 0 until labelsArray.length()) {
                        add(labelsArray.optString(i))
                    }
                }
            }.ifEmpty { FALLBACK.labels }
            return ClawdDetectorConfig(
                modelAsset = data.optString("modelAsset", FALLBACK.modelAsset),
                labels = labels,
                confidenceThreshold = data.optDouble("confidenceThreshold", FALLBACK.confidenceThreshold.toDouble()).toFloat(),
                nmsIouThreshold = data.optDouble("nmsIouThreshold", FALLBACK.nmsIouThreshold.toDouble()).toFloat(),
                globalDuplicateIouThreshold = data.optDouble("globalDuplicateIouThreshold", FALLBACK.globalDuplicateIouThreshold.toDouble()).toFloat(),
                runtime = ClawdRuntime.fromId(data.optString("runtime")),
                cpuThreads = data.optInt("cpuThreads", FALLBACK.cpuThreads).coerceIn(1, 8),
                alwaysOnDefault = data.optBoolean("alwaysOnDefault", false),
                drawBoxDefault = data.optBoolean("drawBoxDefault", false),
                pollIntervalMs = data.optLong("pollIntervalMs", FALLBACK.pollIntervalMs)
            )
        }
    }
}
