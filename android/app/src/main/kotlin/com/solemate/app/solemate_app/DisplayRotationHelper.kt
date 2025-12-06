package com.solemate.app.solemate_app

import android.app.Activity
import android.hardware.camera2.CameraDevice
import android.view.Display
import com.google.ar.core.Session

/**
 * Handles viewport and rotation synchronization between the AR session/display and Camera2.
 * Works with both ARCore Session and Camera2 API for VIO mode.
 */
class DisplayRotationHelper(private val activity: Activity) {
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var viewportChanged = false

    fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        viewportChanged = true
    }

    /**
     * Updates ARCore session with display geometry if viewport changed.
     */
    fun updateSessionIfNeeded(session: Session) {
        if (viewportChanged) {
            val display: Display = activity.windowManager.defaultDisplay
            session.setDisplayGeometry(display.rotation, viewportWidth, viewportHeight)
            viewportChanged = false
        }
    }

    /**
     * Gets current display rotation for Camera2 (VIO mode).
     * Returns rotation value compatible with Camera2 API.
     */
    fun getDisplayRotation(): Int {
        val display: Display = activity.windowManager.defaultDisplay
        return display.rotation
    }

    /**
     * Gets viewport dimensions.
     */
    fun getViewportSize(): Pair<Int, Int> {
        return Pair(viewportWidth, viewportHeight)
    }

    /**
     * Updates Camera2 if needed (placeholder for future Camera2-specific updates).
     * Currently just tracks viewport changes.
     */
    fun updateCamera2IfNeeded(cameraDevice: CameraDevice?) {
        // For now, just reset the viewport changed flag
        // Future: Could configure Camera2 capture session with proper orientation
        if (viewportChanged) {
            viewportChanged = false
        }
    }

    /**
     * Gets rotation in degrees (0, 90, 180, 270).
     */
    fun getRotationDegrees(): Int {
        return when (getDisplayRotation()) {
            android.view.Surface.ROTATION_0 -> 0
            android.view.Surface.ROTATION_90 -> 90
            android.view.Surface.ROTATION_180 -> 180
            android.view.Surface.ROTATION_270 -> 270
            else -> 0
        }
    }
}
