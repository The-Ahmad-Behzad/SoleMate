package com.solemate.app.solemate_app

import android.util.Log
import kotlin.math.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Fuses VIO pose + plane + foot landmarks to produce stable shoe transform.
 * Implements exponential smoothing, Mahalanobis gating, and drift detection.
 */
class FootPoseFusion {
    companion object {
        private const val TAG = "FootPoseFusion"
        private const val SMOOTHING_ALPHA = 0.7f  // Exponential smoothing factor (0-1, higher = less smoothing)
        private const val DRIFT_THRESHOLD = 0.15f  // 15cm drift threshold for re-anchoring
        private const val MAHALANOBIS_THRESHOLD = 3.0f  // 3-sigma threshold for gating
        private const val MIN_CONFIDENCE = 0.5f  // Minimum confidence to use fused pose
    }

    private val lock = ReentrantLock()
    
    // Current fused pose
    private var fusedPose: FusedPose? = null
    
    // Anchor point (reference for drift detection)
    private var anchorPose: Pose? = null
    private var anchorTimestamp = 0L
    
    // Calibration
    private var scaleCalibration = 1.0f  // Scale factor from calibration
    private var cameraHeightEstimate = 1.5f  // Estimated camera height in meters (default 1.5m)

    data class FusedPose(
        val matrix: FloatArray,  // 4x4 transformation matrix
        val confidence: Float,   // Confidence [0, 1]
        val timestampNs: Long
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FusedPose
            return matrix.contentEquals(other.matrix) && 
                   confidence == other.confidence &&
                   timestampNs == other.timestampNs
        }

        override fun hashCode(): Int {
            var result = matrix.contentHashCode()
            result = 31 * result + confidence.hashCode()
            result = 31 * result + timestampNs.hashCode()
            return result
        }
    }

    /**
     * Set scale calibration (from user input or measurement).
     */
    fun setScaleCalibration(scale: Float) {
        lock.withLock {
            scaleCalibration = scale.coerceIn(0.5f, 2.0f)
            Log.d(TAG, "Scale calibration set to: $scaleCalibration")
        }
    }

    /**
     * Set camera height estimate (for scale calculation).
     */
    fun setCameraHeight(height: Float) {
        lock.withLock {
            cameraHeightEstimate = height.coerceIn(0.5f, 3.0f)
            Log.d(TAG, "Camera height estimate set to: ${cameraHeightEstimate}m")
        }
    }

    /**
     * Fuse VIO pose, plane, and foot landmarks to produce shoe transform.
     * @param vioPose Current VIO pose (4x4 matrix)
     * @param vioCovariance VIO pose covariance (6x6 matrix, optional)
     * @param planeInfo Detected plane information
     * @param footLandmarks Foot landmarks (ankle, toe positions in 3D)
     * @param timestampNs Current timestamp
     * @return Fused pose matrix or null if fusion failed
     */
    fun fuse(
        vioPose: FloatArray,
        vioCovariance: FloatArray?,
        planeInfo: PlaneInfo?,
        footLandmarks: FootLandmarks?,
        timestampNs: Long
    ): FloatArray? {
        return lock.withLock {
            if (vioPose.size < 16) {
                return null
            }

            // Calculate shoe transform from inputs
            val shoeTransform = calculateShoeTransform(
                vioPose,
                planeInfo,
                footLandmarks
            ) ?: return null

            // Apply Mahalanobis gating if covariance available
            if (vioCovariance != null && fusedPose != null) {
                if (!passMahalanobisGate(shoeTransform, fusedPose!!.matrix, vioCovariance)) {
                    Log.d(TAG, "Pose rejected by Mahalanobis gate")
                    return fusedPose?.matrix  // Return previous pose
                }
            }

            // Apply exponential smoothing
            val smoothedTransform = if (fusedPose != null) {
                smoothPose(fusedPose!!.matrix, shoeTransform)
            } else {
                shoeTransform
            }

            // Check for drift and re-anchor if needed
            checkDriftAndReanchor(smoothedTransform, timestampNs)

            // Calculate confidence
            val confidence = calculateConfidence(planeInfo, footLandmarks, vioCovariance)

            // Update fused pose
            fusedPose = FusedPose(
                matrix = smoothedTransform,
                confidence = confidence,
                timestampNs = timestampNs
            )

            smoothedTransform
        }
    }

    /**
     * Calculate shoe transform from VIO pose, plane, and foot landmarks.
     */
    private fun calculateShoeTransform(
        vioPose: FloatArray,
        planeInfo: PlaneInfo?,
        footLandmarks: FootLandmarks?
    ): FloatArray? {
        if (planeInfo == null || footLandmarks == null) {
            return null
        }

        // Get foot contact point on plane
        val footContactPoint = footLandmarks.getContactPoint(planeInfo) ?: return null

        // Calculate forward direction from ankle to toe
        val forwardDir = footLandmarks.getForwardDirection() ?: floatArrayOf(0f, 0f, 1f)

        // Project forward direction onto plane
        val planeNormal = floatArrayOf(0f, 1f, 0f)  // Floor plane normal (Y-up)
        val projectedForward = projectVectorOntoPlane(forwardDir, planeNormal)

        // Normalize forward direction
        val forwardLen = sqrt(projectedForward[0] * projectedForward[0] + 
                              projectedForward[2] * projectedForward[2])
        if (forwardLen < 0.01f) {
            return null  // Invalid direction
        }
        projectedForward[0] /= forwardLen
        projectedForward[2] /= forwardLen

        // Calculate yaw angle from forward direction
        val yaw = atan2(projectedForward[0], projectedForward[2])

        // Build transformation matrix
        val transform = FloatArray(16)
        android.opengl.Matrix.setIdentityM(transform, 0)

        // Translation: foot contact point
        transform[12] = footContactPoint[0]
        transform[13] = footContactPoint[1]
        transform[14] = footContactPoint[2]

        // Rotation: Y-axis rotation (yaw)
        android.opengl.Matrix.rotateM(transform, 0, Math.toDegrees(yaw.toDouble()).toFloat(), 0f, 1f, 0f)

        // Apply scale calibration
        val scale = scaleCalibration
        android.opengl.Matrix.scaleM(transform, 0, scale, scale, scale)

        return transform
    }

    /**
     * Project vector onto plane (remove component along plane normal).
     */
    private fun projectVectorOntoPlane(vector: FloatArray, planeNormal: FloatArray): FloatArray {
        // Project vector onto plane: v - (v·n) * n
        val dot = vector[0] * planeNormal[0] + vector[1] * planeNormal[1] + vector[2] * planeNormal[2]
        return floatArrayOf(
            vector[0] - dot * planeNormal[0],
            vector[1] - dot * planeNormal[1],
            vector[2] - dot * planeNormal[2]
        )
    }

    /**
     * Apply exponential smoothing to pose.
     */
    private fun smoothPose(previous: FloatArray, current: FloatArray): FloatArray {
        val smoothed = FloatArray(16)
        for (i in 0 until 16) {
            smoothed[i] = SMOOTHING_ALPHA * current[i] + (1f - SMOOTHING_ALPHA) * previous[i]
        }
        return smoothed
    }

    /**
     * Mahalanobis gating: reject poses that are too far from expected distribution.
     */
    private fun passMahalanobisGate(
        current: FloatArray,
        previous: FloatArray,
        covariance: FloatArray
    ): Boolean {
        // Calculate difference in translation
        val dx = current[12] - previous[12]
        val dy = current[13] - previous[13]
        val dz = current[14] - previous[14]

        // Simplified: use diagonal covariance (position only)
        // Real implementation would use full 6x6 covariance
        val covX = covariance[0].coerceAtLeast(0.01f)
        val covY = covariance[7].coerceAtLeast(0.01f)
        val covZ = covariance[14].coerceAtLeast(0.01f)

        // Mahalanobis distance
        val mahalDist = sqrt(
            (dx * dx) / covX +
            (dy * dy) / covY +
            (dz * dz) / covZ
        )

        return mahalDist < MAHALANOBIS_THRESHOLD
    }

    /**
     * Check for drift and re-anchor if needed.
     */
    private fun checkDriftAndReanchor(currentPose: FloatArray, timestampNs: Long) {
        if (anchorPose == null) {
            // Set initial anchor
            anchorPose = Pose(
                translation = floatArrayOf(currentPose[12], currentPose[13], currentPose[14]),
                rotation = floatArrayOf(0f, 0f, 0f, 1f)
            )
            anchorTimestamp = timestampNs
            return
        }

        // Calculate drift from anchor
        val anchorTrans = anchorPose!!.translation
        val dx = currentPose[12] - anchorTrans[0]
        val dy = currentPose[13] - anchorTrans[1]
        val dz = currentPose[14] - anchorTrans[2]
        val drift = sqrt(dx * dx + dy * dy + dz * dz)

        if (drift > DRIFT_THRESHOLD) {
            // Re-anchor
            anchorPose = Pose(
                translation = floatArrayOf(currentPose[12], currentPose[13], currentPose[14]),
                rotation = floatArrayOf(0f, 0f, 0f, 1f)
            )
            anchorTimestamp = timestampNs
            Log.d(TAG, "Re-anchored due to drift: ${drift}m")
        }
    }

    /**
     * Calculate confidence in fused pose.
     */
    private fun calculateConfidence(
        planeInfo: PlaneInfo?,
        footLandmarks: FootLandmarks?,
        covariance: FloatArray?
    ): Float {
        var confidence = 1.0f

        // Reduce confidence if plane not available
        if (planeInfo == null) {
            confidence *= 0.5f
        }

        // Reduce confidence if foot landmarks not available
        if (footLandmarks == null) {
            confidence *= 0.5f
        }

        // Reduce confidence if covariance is high (uncertainty)
        if (covariance != null && covariance.size >= 36) {
            val avgCov = (covariance[0] + covariance[7] + covariance[14]) / 3.0f
            if (avgCov > 1.0f) {
                confidence *= (1.0f / avgCov).coerceIn(0.1f, 1.0f)
            }
        }

        return confidence.coerceIn(0f, 1f)
    }

    /**
     * Get current fused pose.
     */
    fun getFusedPose(): FusedPose? {
        return lock.withLock {
            fusedPose
        }
    }

    /**
     * Reset fusion state.
     */
    fun reset() {
        lock.withLock {
            fusedPose = null
            anchorPose = null
            anchorTimestamp = 0L
        }
    }
}

/**
 * Foot landmarks data structure.
 */
data class FootLandmarks(
    val ankle3D: FloatArray,  // [x, y, z] in world space
    val toe3D: FloatArray?,   // [x, y, z] in world space (optional)
    val heel3D: FloatArray?,  // [x, y, z] in world space (optional)
    val visibility: Float     // Overall visibility [0, 1]
) {
    /**
     * Get foot contact point on plane (typically toe or heel).
     */
    fun getContactPoint(plane: PlaneInfo): FloatArray? {
        // Prefer toe, fall back to ankle
        val point3D = toe3D ?: ankle3D
        
        // Project onto plane (simplified: assume plane is horizontal at y=plane.centerPose.translation[1])
        val planeY = plane.centerPose.translation[1]
        return floatArrayOf(point3D[0], planeY, point3D[2])
    }

    /**
     * Get forward direction vector (ankle to toe).
     */
    fun getForwardDirection(): FloatArray? {
        val toe = toe3D ?: return null
        val dx = toe[0] - ankle3D[0]
        val dy = toe[1] - ankle3D[1]
        val dz = toe[2] - ankle3D[2]
        return floatArrayOf(dx, dy, dz)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FootLandmarks
        return ankle3D.contentEquals(other.ankle3D) &&
               toe3D?.contentEquals(other.toe3D) != false &&
               heel3D?.contentEquals(other.heel3D) != false &&
               visibility == other.visibility
    }

    override fun hashCode(): Int {
        var result = ankle3D.contentHashCode()
        result = 31 * result + (toe3D?.contentHashCode() ?: 0)
        result = 31 * result + (heel3D?.contentHashCode() ?: 0)
        result = 31 * result + visibility.hashCode()
        return result
    }
}






