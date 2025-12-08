package com.solemate.app.solemate_app

import android.app.Activity
import android.view.Display
import com.google.ar.core.Session

/**
 * Handles viewport and rotation synchronization between the AR session and display.
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

    fun updateSessionIfNeeded(session: Session) {
        if (viewportChanged) {
            val display: Display = activity.windowManager.defaultDisplay
            session.setDisplayGeometry(display.rotation, viewportWidth, viewportHeight)
            viewportChanged = false
        }
    }
}
