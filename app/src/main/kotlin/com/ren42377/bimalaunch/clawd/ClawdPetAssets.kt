package com.ren42377.bimalaunch.clawd

object ClawdPetAssets {
    const val ASSET_DIR = "clawd_pet"

    val INTRO_FRAMES = listOf(
        "clawd-intro-00.svg", "clawd-intro-01.svg", "clawd-intro-02.svg", "clawd-intro-03.svg"
    )
    val IDLE_FRAMES = listOf("clawd-idle-00.svg", "clawd-idle-01.svg")
    val THINKING_FRAMES = listOf(
        "clawd-thinking-00.svg", "clawd-thinking-01.svg", "clawd-thinking-02.svg",
        "clawd-thinking-03.svg", "clawd-thinking-04.svg"
    )
    val ERROR_FRAMES = listOf("clawd-error-00.svg", "clawd-error-01.svg")
    const val WAKE_FRAME = "clawd-wake.svg"
    const val FALLBACK_FRAME = "clawd.svg"

    const val IDLE_DELAY_MS = 30_000L
    const val THINKING_INTERVAL_MS = 60_000L
    const val FRAME_INTERVAL_MS = 220L
}

enum class ClawdPetState {
    INTRO, IDLE, THINKING, ERROR, WAKE, FALLBACK
}
