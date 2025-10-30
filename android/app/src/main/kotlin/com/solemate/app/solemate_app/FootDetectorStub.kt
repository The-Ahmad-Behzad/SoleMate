package com.solemate.app.solemate_app

import android.graphics.Bitmap
import android.util.Log

/**
 * Replace this stub with a real detector (MediaPipe / TFLite).
 * This stub just searches for a bright region center as a placeholder.
 */
class FootDetectorStub {

    data class FootResult(
        val detected: Boolean,
        val xPx: Int,
        val yPx: Int
    )

    /**
     * Synchronous detection - should be called by background thread.
     */
    fun detectFoot(bitmap: Bitmap): FootResult {
        // Demo stub: returns center point (replace with model inference)
        val x = bitmap.width / 2
        val y = bitmap.height / 2
        Log.d("FootDetectorStub", "Stub detect at ($x,$y)")
        return FootResult(true, x, y)
    }
}
