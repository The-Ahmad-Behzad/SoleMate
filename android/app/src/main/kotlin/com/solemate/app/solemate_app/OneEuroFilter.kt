package com.solemate.app.solemate_app

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * One Euro Filter implementation for smooth signal filtering.
 * 
 * This filter reduces jitter while maintaining responsiveness.
 * Based on: https://cristal.univ-lille.fr/~casiez/1euro/
 * 
 * @param minCutoff Minimum cutoff frequency (Hz). Lower = more smoothing
 * @param beta Speed coefficient. Higher = less lag when moving fast
 * @param dCutoff Derivative cutoff frequency (Hz)
 */
class OneEuroFilter(
    private val minCutoff: Float = 1.0f,
    private val beta: Float = 0.5f,
    private val dCutoff: Float = 1.0f
) {
    private var xPrev: Float? = null
    private var dxPrev: Float = 0f
    private var lastTime: Long = 0
    private var initialized = false

    /**
     * Filter a new value with automatic timestamping.
     */
    fun filter(x: Float): Float {
        return filter(x, System.nanoTime())
    }

    /**
     * Filter a new value with explicit timestamp.
     */
    fun filter(x: Float, timestamp: Long): Float {
        if (!initialized) {
            xPrev = x
            dxPrev = 0f
            lastTime = timestamp
            initialized = true
            return x
        }

        // Calculate time delta in seconds
        val dt = ((timestamp - lastTime) / 1_000_000_000.0).toFloat()
        lastTime = timestamp

        // Avoid division by zero
        if (dt <= 0f) return xPrev ?: x

        // Calculate derivative (rate of change)
        val dx = (x - (xPrev ?: x)) / dt

        // Filter the derivative with fixed cutoff
        val alphaDx = smoothingFactor(dt, dCutoff)
        val dxFiltered = alphaDx * dx + (1 - alphaDx) * dxPrev
        dxPrev = dxFiltered

        // Adapt cutoff based on speed (derivative magnitude)
        val cutoff = minCutoff + beta * abs(dxFiltered)

        // Filter the signal
        val alpha = smoothingFactor(dt, cutoff)
        val xFiltered = alpha * x + (1 - alpha) * (xPrev ?: x)
        xPrev = xFiltered

        return xFiltered
    }

    /**
     * Reset the filter state.
     */
    fun reset() {
        xPrev = null
        dxPrev = 0f
        lastTime = 0
        initialized = false
    }

    /**
     * Calculate smoothing factor (alpha) for given time delta and cutoff.
     */
    private fun smoothingFactor(dt: Float, cutoff: Float): Float {
        val tau = 1.0f / (2 * Math.PI.toFloat() * cutoff)
        return 1.0f / (1.0f + tau / dt)
    }
}

/**
 * 2D version of OneEuroFilter for position smoothing.
 */
class OneEuroFilter2D(
    minCutoff: Float = 1.0f,
    beta: Float = 0.5f,
    dCutoff: Float = 1.0f
) {
    private val filterX = OneEuroFilter(minCutoff, beta, dCutoff)
    private val filterY = OneEuroFilter(minCutoff, beta, dCutoff)

    fun filter(x: Float, y: Float): Pair<Float, Float> {
        val timestamp = System.nanoTime()
        return Pair(
            filterX.filter(x, timestamp),
            filterY.filter(y, timestamp)
        )
    }

    fun reset() {
        filterX.reset()
        filterY.reset()
    }
}

/**
 * 3D version of OneEuroFilter for position smoothing.
 */
class OneEuroFilter3D(
    minCutoff: Float = 1.0f,
    beta: Float = 0.5f,
    dCutoff: Float = 1.0f
) {
    private val filterX = OneEuroFilter(minCutoff, beta, dCutoff)
    private val filterY = OneEuroFilter(minCutoff, beta, dCutoff)
    private val filterZ = OneEuroFilter(minCutoff, beta, dCutoff)

    fun filter(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        val timestamp = System.nanoTime()
        return Triple(
            filterX.filter(x, timestamp),
            filterY.filter(y, timestamp),
            filterZ.filter(z, timestamp)
        )
    }

    fun reset() {
        filterX.reset()
        filterY.reset()
        filterZ.reset()
    }
}
