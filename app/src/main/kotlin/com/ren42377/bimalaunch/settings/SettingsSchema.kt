package com.ren42377.bimalaunch.settings

import com.ren42377.bimalaunch.core.NativeBridge
import org.json.JSONObject

sealed interface SettingsRow {
    val id: String
    val title: String

    data class Menu(override val id: String, override val title: String, val body: String) : SettingsRow
    data class TextField(
        override val id: String,
        override val title: String,
        val password: Boolean,
        val value: String
    ) : SettingsRow

    data class Choice(
        override val id: String,
        override val title: String,
        val options: List<Pair<String, String>>,
        val selected: String
    ) : SettingsRow

    data class Switch(
        override val id: String,
        override val title: String,
        val body: String,
        val value: Boolean
    ) : SettingsRow
}

data class SettingsSection(
    val id: String,
    val title: String,
    val rows: List<SettingsRow>
)

data class SettingsSchema(val sections: List<SettingsSection>) {
    fun section(id: String): SettingsSection? = sections.firstOrNull { it.id == id }

    companion object {
        fun fromNative(): SettingsSchema {
            val response = runCatching {
                JSONObject(NativeBridge.dispatch("settings_schema", "{}"))
            }.getOrNull()
            val data = response?.optJSONObject("data") ?: JSONObject()
            val sectionsArray = data.optJSONArray("sections")
            val sections = buildList {
                if (sectionsArray != null) {
                    for (i in 0 until sectionsArray.length()) {
                        val sectionObj = sectionsArray.optJSONObject(i) ?: continue
                        add(parseSection(sectionObj))
                    }
                }
            }
            return SettingsSchema(sections)
        }

        private fun parseSection(obj: JSONObject): SettingsSection {
            val rowsArray = obj.optJSONArray("rows")
            val rows = buildList {
                if (rowsArray != null) {
                    for (i in 0 until rowsArray.length()) {
                        val rowObj = rowsArray.optJSONObject(i) ?: continue
                        parseRow(rowObj)?.let { add(it) }
                    }
                }
            }
            return SettingsSection(
                id = obj.optString("id"),
                title = obj.optString("title"),
                rows = rows
            )
        }

        private fun parseRow(obj: JSONObject): SettingsRow? {
            val id = obj.optString("id")
            val title = obj.optString("title")
            return when (obj.optString("type")) {
                "menu" -> SettingsRow.Menu(id, title, obj.optString("body"))
                "text" -> SettingsRow.TextField(id, title, obj.optBoolean("password", false), obj.optString("value"))
                "switch" -> SettingsRow.Switch(id, title, obj.optString("body"), obj.optBoolean("value", false))
                "choice" -> {
                    val optionsArray = obj.optJSONArray("options")
                    val options = buildList {
                        if (optionsArray != null) {
                            for (i in 0 until optionsArray.length()) {
                                val optObj = optionsArray.optJSONObject(i) ?: continue
                                add(optObj.optString("id") to optObj.optString("label"))
                            }
                        }
                    }
                    SettingsRow.Choice(id, title, options, obj.optString("selected"))
                }
                else -> null
            }
        }
    }
}
