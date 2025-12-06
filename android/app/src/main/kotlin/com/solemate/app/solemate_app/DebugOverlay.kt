package com.solemate.app.solemate_app

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View

/**
 * Debug overlay for displaying VIO tracking status, performance metrics, and plane visualization.
 * Can be toggled on/off for debugging purposes.
 */
class DebugOverlay(context: android.content.Context) : View(context) {
    companion object {
        private const val TEXT_SIZE = 24f
        private const val PADDING = 16f
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = TEXT_SIZE
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)  // Semi-transparent black
    }

    // Debug information
    var isTracking = false
    var trackingStatus = "Initializing"
    var fps = 0f
    var vioUpdateRate = 0f
    var latencyMs = 0f
    var planeCount = 0
    var featureCount = 0
    var poseConfidence = 0f
    var driftDistance = 0f
    
    private var isEnabled = false

    /**
     * Enable or disable debug overlay.
     */
    fun setDebugEnabled(enabled: Boolean) {
        isEnabled = enabled
        visibility = if (enabled) VISIBLE else GONE
        invalidate()
    }

    /**
     * Update tracking status.
     */
    fun updateTrackingStatus(tracking: Boolean, status: String) {
        isTracking = tracking
        trackingStatus = status
        if (isEnabled) invalidate()
    }

    /**
     * Update performance metrics.
     */
    fun updateMetrics(fps: Float, vioRate: Float, latency: Float) {
        this.fps = fps
        this.vioUpdateRate = vioRate
        this.latencyMs = latency
        if (isEnabled) invalidate()
    }

    /**
     * Update plane and feature counts.
     */
    fun updateCounts(planes: Int, features: Int) {
        this.planeCount = planes
        this.featureCount = features
        if (isEnabled) invalidate()
    }

    /**
     * Update pose confidence and drift.
     */
    fun updatePoseInfo(confidence: Float, drift: Float) {
        this.poseConfidence = confidence
        this.driftDistance = drift
        if (isEnabled) invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!isEnabled) return

        super.onDraw(canvas)

        val lines = mutableListOf<String>()
        
        // Tracking status
        val statusColor = if (isTracking) Color.GREEN else Color.RED
        textPaint.color = statusColor
        lines.add("VIO: $trackingStatus")
        
        // Performance metrics
        textPaint.color = Color.WHITE
        lines.add("FPS: ${"%.1f".format(fps)}")
        lines.add("VIO Rate: ${"%.1f".format(vioUpdateRate)} Hz")
        lines.add("Latency: ${"%.1f".format(latencyMs)} ms")
        
        // Plane and feature info
        lines.add("Planes: $planeCount")
        lines.add("Features: $featureCount")
        
        // Pose info
        lines.add("Confidence: ${"%.2f".format(poseConfidence)}")
        lines.add("Drift: ${"%.3f".format(driftDistance)} m")

        // Draw background
        val textHeight = textPaint.fontMetrics.let { it.descent - it.ascent }
        val totalHeight = lines.size * textHeight + PADDING * 2
        val totalWidth = lines.maxOfOrNull { textPaint.measureText(it) }?.plus(PADDING * 2) ?: 200f
        
        canvas.drawRect(0f, 0f, totalWidth, totalHeight, backgroundPaint)

        // Draw text lines
        var y = PADDING - textPaint.fontMetrics.ascent
        for (line in lines) {
            canvas.drawText(line, PADDING, y, textPaint)
            y += textHeight
        }
    }
}

