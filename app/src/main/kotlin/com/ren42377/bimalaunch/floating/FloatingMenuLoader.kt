package com.ren42377.bimalaunch.floating

import com.ren42377.bimalaunch.core.NativeBridge
import org.json.JSONObject

data class FloatingMenuModel(
    val title: String,
    val items: List<Pair<Triple<String, String, String>, String>>,
    val geometry: FloatingGeometryConfig
)

data class FloatingGeometryConfig(
    val bubbleSize: Float,
    val bubbleInset: Float,
    val edgeMargin: Float,
    val verticalMargin: Float,
    val panelCornerRadius: Float,
    val baseMenuWidth: Float,
    val baseMenuHeight: Float,
    val defaultBubbleYFraction: Float,
    val snapDurationMillis: Int,
    val discardStartOffset: Float,
    val discardRange: Float
) {
    companion object {
        val DEFAULT = FloatingGeometryConfig(
            bubbleSize = 60f,
            bubbleInset = 2f,
            edgeMargin = 12f,
            verticalMargin = 48f,
            panelCornerRadius = 24f,
            baseMenuWidth = 160f,
            baseMenuHeight = 300f,
            defaultBubbleYFraction = 0.33f,
            snapDurationMillis = 300,
            discardStartOffset = 330f,
            discardRange = 260f
        )
    }
}

object FloatingMenuLoader {
    fun load(): FloatingMenuModel {
        val response = runCatching {
            JSONObject(NativeBridge.dispatch("floating_menu", "{}"))
        }.getOrNull()
        val data = response?.optJSONObject("data")
        val title = data?.optString("title", "Menu") ?: "Menu"
        val itemsArray = data?.optJSONArray("items")
        val items = buildList {
            if (itemsArray != null) {
                for (index in 0 until itemsArray.length()) {
                    val obj = itemsArray.optJSONObject(index) ?: continue
                    add(
                        Triple(
                            obj.optString("id"),
                            obj.optString("title"),
                            obj.optString("icon")
                        ) to obj.optString("action")
                    )
                }
            }
        }
        val geometryObj = data?.optJSONObject("geometry")
        val geometry = if (geometryObj == null) {
            FloatingGeometryConfig.DEFAULT
        } else {
            val d = FloatingGeometryConfig.DEFAULT
            FloatingGeometryConfig(
                bubbleSize = geometryObj.optDouble("bubbleSize", d.bubbleSize.toDouble()).toFloat(),
                bubbleInset = geometryObj.optDouble("bubbleInset", d.bubbleInset.toDouble()).toFloat(),
                edgeMargin = geometryObj.optDouble("edgeMargin", d.edgeMargin.toDouble()).toFloat(),
                verticalMargin = geometryObj.optDouble("verticalMargin", d.verticalMargin.toDouble()).toFloat(),
                panelCornerRadius = geometryObj.optDouble("panelCornerRadius", d.panelCornerRadius.toDouble()).toFloat(),
                baseMenuWidth = geometryObj.optDouble("baseMenuWidth", d.baseMenuWidth.toDouble()).toFloat(),
                baseMenuHeight = geometryObj.optDouble("baseMenuHeight", d.baseMenuHeight.toDouble()).toFloat(),
                defaultBubbleYFraction = geometryObj.optDouble("defaultBubbleYFraction", d.defaultBubbleYFraction.toDouble()).toFloat(),
                snapDurationMillis = geometryObj.optInt("snapDurationMillis", d.snapDurationMillis),
                discardStartOffset = geometryObj.optDouble("discardStartOffset", d.discardStartOffset.toDouble()).toFloat(),
                discardRange = geometryObj.optDouble("discardRange", d.discardRange.toDouble()).toFloat()
            )
        }
        return FloatingMenuModel(title, items, geometry)
    }
}
