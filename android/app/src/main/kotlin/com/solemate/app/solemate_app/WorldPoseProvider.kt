package com.solemate.app.solemate_app

/**
 * Abstraction layer for world pose tracking providers (ARCore, VIO, etc.)
 * Allows SimpleRenderer to work with different AR backends without tight coupling.
 */
interface WorldPoseProvider {
    /**
     * Get the view matrix (camera pose in world space)
     */
    fun getViewMatrix(): FloatArray

    /**
     * Get the projection matrix for rendering
     * @param near Near clipping plane distance
     * @param far Far clipping plane distance
     */
    fun getProjectionMatrix(near: Float, far: Float): FloatArray

    /**
     * Get camera intrinsics (focal length, principal point, etc.)
     * Returns null if not available
     */
    fun getCameraIntrinsics(): CameraIntrinsics?

    /**
     * Perform hit test at screen coordinates (view space)
     * @param viewX X coordinate in view space [0, width]
     * @param viewY Y coordinate in view space [0, height]
     * @return List of hit results, sorted by preference (best first)
     */
    fun hitTest(viewX: Float, viewY: Float): List<HitResult>

    /**
     * Get all detected planes
     */
    fun getPlanes(): List<PlaneInfo>

    /**
     * Check if tracking is active
     */
    fun isTracking(): Boolean

    /**
     * Get light estimate for realistic rendering
     * Returns null if not available
     */
    fun getLightEstimate(): LightEstimate?

    /**
     * Transform coordinates from one space to another
     * @param srcType Source coordinate space
     * @param srcCoords Source coordinates (x, y pairs)
     * @param dstType Destination coordinate space
     * @return Transformed coordinates (x, y pairs), same length as srcCoords
     */
    fun transformCoordinates2d(
        srcType: CoordinateSpace,
        srcCoords: FloatArray,
        dstType: CoordinateSpace
    ): FloatArray?

    /**
     * Get camera image for ML processing (optional, may return null)
     * Caller must close the image when done
     */
    fun acquireCameraImage(): CameraImage?

    /**
     * Get camera texture ID for background rendering (ARCore-specific, may return -1)
     */
    fun getCameraTextureId(): Int

    /**
     * Set camera texture name (ARCore-specific, no-op for other providers)
     */
    fun setCameraTextureName(textureId: Int)

    /**
     * Update the provider (call once per frame)
     * Returns true if update was successful
     */
    fun update(): Boolean

    /**
     * Release resources
     */
    fun release()
}

/**
 * Coordinate space types for transformation
 */
enum class CoordinateSpace {
    IMAGE_PIXELS,  // Camera image pixel coordinates
    IMAGE_NORMALIZED,  // Normalized image coordinates [0, 1]
    VIEW,         // View/screen coordinates
    WORLD         // 3D world coordinates
}

/**
 * Camera image interface (abstraction over ARCore Image)
 */
interface CameraImage {
    val width: Int
    val height: Int
    val format: Int
    fun close()
    fun getPlanes(): Array<ImagePlane>
}

interface ImagePlane {
    val buffer: java.nio.ByteBuffer
    val pixelStride: Int
    val rowStride: Int
}

/**
 * Camera intrinsics for unprojection and coordinate transforms
 */
data class CameraIntrinsics(
    val fx: Float,  // Focal length X
    val fy: Float,  // Focal length Y
    val cx: Float,  // Principal point X
    val cy: Float,  // Principal point Y
    val width: Int,
    val height: Int
)

/**
 * Result of a hit test (ray intersection with world geometry)
 */
data class HitResult(
    val hitPose: Pose,           // 3D position and orientation of hit
    val distance: Float,         // Distance from camera to hit point
    val trackableType: TrackableType,  // Type of surface hit
    val trackableId: Long? = null      // Optional ID for tracking
)

/**
 * Type of trackable surface
 */
enum class TrackableType {
    PLANE,      // Detected horizontal/vertical plane
    POINT,       // Tracked feature point
    DEPTH_POINT  // Depth-based point
}

/**
 * Information about a detected plane
 */
data class PlaneInfo(
    val centerPose: Pose,
    val extentX: Float,      // Width of plane
    val extentZ: Float,      // Depth of plane
    val polygon: List<FloatArray>? = null,  // Optional polygon vertices
    val trackingState: TrackingState
)

/**
 * 3D pose (position + orientation)
 */
data class Pose(
    val translation: FloatArray,  // [x, y, z] in meters
    val rotation: FloatArray       // Quaternion [x, y, z, w] or rotation matrix
) {
    /**
     * Convert to 4x4 model matrix
     */
    fun toMatrix(): FloatArray {
        val matrix = FloatArray(16)
        android.opengl.Matrix.setIdentityM(matrix, 0)
        
        // If rotation is a quaternion, convert to matrix
        if (rotation.size == 4) {
            android.opengl.Matrix.setRotateM(matrix, 0,
                Math.toDegrees(2 * Math.acos(rotation[3].toDouble())).toFloat(),
                rotation[0], rotation[1], rotation[2])
        } else if (rotation.size == 16) {
            // Already a matrix, copy it
            System.arraycopy(rotation, 0, matrix, 0, 16)
        }
        
        // Apply translation
        matrix[12] = translation[0]
        matrix[13] = translation[1]
        matrix[14] = translation[2]
        
        return matrix
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Pose

        if (!translation.contentEquals(other.translation)) return false
        if (!rotation.contentEquals(other.rotation)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = translation.contentHashCode()
        result = 31 * result + rotation.contentHashCode()
        return result
    }
}

/**
 * Tracking state
 */
enum class TrackingState {
    TRACKING,      // Actively tracking
    PAUSED,        // Temporarily paused
    STOPPED        // Stopped or lost tracking
}

/**
 * Light estimate for realistic rendering
 */
data class LightEstimate(
    val pixelIntensity: Float,      // Average pixel intensity [0, 1]
    val colorCorrection: FloatArray? = null  // RGB color correction [r, g, b, a]
)

