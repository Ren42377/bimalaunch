package com.ren42377.bimalaunch.clawd

import android.graphics.Bitmap

object VisualFrameFingerprint {
    private const val GRID_WIDTH = 32
    private const val GRID_HEIGHT = 32

    fun compute(bitmap: Bitmap): IntArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, GRID_WIDTH, GRID_HEIGHT, true)
        val pixels = IntArray(GRID_WIDTH * GRID_HEIGHT)
        scaled.getPixels(pixels, 0, GRID_WIDTH, 0, 0, GRID_WIDTH, GRID_HEIGHT)
        if (scaled !== bitmap) scaled.recycle()
        return pixels
    }

    fun isDifferent(previous: IntArray?, current: IntArray, threshold: Int = 12): Boolean {
        if (previous == null || previous.size != current.size) return true
        var differing = 0
        for (i in current.indices) {
            if (previous[i] != current[i]) differing++
        }
        val percentDifferent = (differing * 100) / current.size
        return percentDifferent >= threshold
    }
}
