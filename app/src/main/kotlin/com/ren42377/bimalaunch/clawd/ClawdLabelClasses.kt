package com.ren42377.bimalaunch.clawd

import androidx.compose.ui.graphics.Color

data class ClawdLabelClass(val key: String, val displayName: String, val color: Color)

object ClawdLabelClasses {
    val all: List<ClawdLabelClass> = listOf(
        ClawdLabelClass("question_area", "Soal", Color(0xFF3B82F6)),
        ClawdLabelClass("a", "A", Color(0xFF22C55E)),
        ClawdLabelClass("b", "B", Color(0xFFF97316)),
        ClawdLabelClass("c", "C", Color(0xFFA855F7)),
        ClawdLabelClass("d", "D", Color(0xFFEF4444)),
        ClawdLabelClass("e", "E", Color(0xFF14B8A6)),
        ClawdLabelClass("button_prev", "Prev", Color(0xFF64748B)),
        ClawdLabelClass("button_next", "Next", Color(0xFF64748B)),
        ClawdLabelClass("question_list_button", "List", Color(0xFF0EA5E9)),
        ClawdLabelClass("question_list_popup_close", "Close", Color(0xFF94A3B8)),
        ClawdLabelClass("question_number", "No", Color(0xFFEAB308)),
        ClawdLabelClass("question_number_answered", "No (dijawab)", Color(0xFF16A34A)),
        ClawdLabelClass("question_number_unanswered", "No (kosong)", Color(0xFF94A3B8)),
        ClawdLabelClass("question_number_current", "No (aktif)", Color(0xFFDC2626))
    )

    fun colorFor(key: String): Color = all.firstOrNull { it.key == key }?.color ?: Color(0xFF3B82F6)
}
