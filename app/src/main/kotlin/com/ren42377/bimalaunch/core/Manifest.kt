package com.ren42377.bimalaunch.core

import org.json.JSONObject

data class RenderNode(
    val id: String,
    val component: String,
    val title: String,
    val subtitle: String,
    val action: String,
    val available: Boolean,
    val children: List<RenderNode>
)

data class Manifest(
    val version: Int,
    val appName: String,
    val root: RenderNode?
)

object ManifestParser {
    fun parse(json: String): Manifest {
        val obj = runCatching { JSONObject(json) }.getOrNull()
            ?: return Manifest(0, "Bimalaunch", null)
        val version = obj.optInt("version", 0)
        val appName = obj.optString("appName", "Bimalaunch")
        val rootObj = obj.optJSONObject("root")
        val root = rootObj?.let { parseNode(it) }
        return Manifest(version, appName, root)
    }

    private fun parseNode(obj: JSONObject): RenderNode {
        val childrenArray = obj.optJSONArray("children")
        val children = buildList {
            if (childrenArray != null) {
                for (index in 0 until childrenArray.length()) {
                    val child = childrenArray.optJSONObject(index) ?: continue
                    add(parseNode(child))
                }
            }
        }
        val props = obj.optJSONObject("props")
        val available = props?.optBoolean("available", true) ?: true
        return RenderNode(
            id = obj.optString("id"),
            component = obj.optString("component"),
            title = obj.optString("title"),
            subtitle = obj.optString("subtitle"),
            action = obj.optString("action"),
            available = available,
            children = children
        )
    }
}

data class DispatchResult(
    val ok: Boolean,
    val effect: String,
    val title: String,
    val message: String,
    val target: String,
    val errorType: String
) {
    companion object {
        fun parse(json: String): DispatchResult {
            val obj = runCatching { JSONObject(json) }.getOrNull() ?: JSONObject()
            return DispatchResult(
                ok = obj.optBoolean("ok", false),
                effect = obj.optString("effect"),
                title = obj.optString("title"),
                message = obj.optString("message"),
                target = obj.optString("target"),
                errorType = obj.optString("errorType")
            )
        }
    }
}
