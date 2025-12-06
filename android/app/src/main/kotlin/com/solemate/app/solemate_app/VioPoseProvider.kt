package com.solemate.app.solemate_app

import android.util.Log
import android.media.Image
import java.nio.ByteBuffer
import kotlin.math.*

/**
 * VIO implementation of WorldPoseProvider.
 * Uses VioEngine for pose tracking and PlaneEstimator for plane detection.
 */
class VioPoseProvider(
    private val vioEngine: VioEngine,
    private val planeEstimator: PlaneEstimator,
    private val rotationHelper: DisplayRotationHelper,
    private val camera2Manager: Camera2Manager? = null
) : WorldPoseProvider {

    companion object {
        private const val TAG = "VioPoseProvider"
    }

    private var isInitialized = false
    private var cameraTextureId = -1
    private val currentPoseMatrix = FloatArray(16)
    private val currentViewMatrix = FloatArray(16)
    private val currentProjMatrix = FloatArray(16)
    private var cameraIntrinsics: CameraIntrinsics? = null
    
    // Calibration values
    private var scaleCalibration = 1.0f
    private var cameraHeightEstimate = 1.5f  // Default 1.5m
    
    // IMU-only fallback mode
    private var useImuFallback = false
    private var vioFailureCount = 0
    private val maxVioFailures = 10  // Switch to IMU after 10 consecutive failures
    
    // IMU data for fallback tracking
    @Volatile
    private var lastGyroX = 0f
    @Volatile
    private var lastGyroY = 0f
    @Volatile
    private var lastGyroZ = 0f
    @Volatile
    private var lastAccelX = 0f
    @Volatile
    private var lastAccelY = 0f
    @Volatile
    private var lastAccelZ = 0f
    @Volatile
    private var lastImuTime = 0L
    
    // IMU-based rotation (quaternion)
    private val imuRotation = FloatArray(4)  // [x, y, z, w]
    private var imuRotationMatrix = FloatArray(16)

    init {
        android.opengl.Matrix.setIdentityM(currentPoseMatrix, 0)
        android.opengl.Matrix.setIdentityM(currentViewMatrix, 0)
        android.opengl.Matrix.setIdentityM(currentProjMatrix, 0)
        // Initialize IMU rotation as identity quaternion
        imuRotation[3] = 1.0f  // w = 1
        android.opengl.Matrix.setIdentityM(imuRotationMatrix, 0)
    }

    override fun update(): Boolean {
        if (!isInitialized) {
            return false
        }

        // Try VIO tracking first (if not already in fallback mode)
        if (!useImuFallback) {
            if (vioEngine.isTracking()) {
                val poseMatrix = FloatArray(16)
                if (vioEngine.getPose(poseMatrix)) {
                    // VIO tracking successful - reset failure count
                    vioFailureCount = 0
                    
                    // Update current pose
                    System.arraycopy(poseMatrix, 0, currentPoseMatrix, 0, 16)
                    
                    // Compute view matrix (inverse of pose matrix)
                    android.opengl.Matrix.invertM(currentViewMatrix, 0, currentPoseMatrix, 0)
                    
                    return true
                } else {
                    // VIO tracking failed
                    vioFailureCount++
                    if (vioFailureCount >= maxVioFailures) {
                        useImuFallback = true
                        Log.w(TAG, "VIO tracking failed $vioFailureCount times, switching to IMU-only fallback mode")
                    }
                }
            } else {
                // VIO not tracking - increment failure count
                vioFailureCount++
                if (vioFailureCount >= maxVioFailures) {
                    useImuFallback = true
                    Log.w(TAG, "VIO not tracking after $vioFailureCount attempts, switching to IMU-only fallback mode")
                }
            }
        }
        
        // Use IMU-only fallback if VIO is not available or has failed
        if (useImuFallback || !vioEngine.isTracking()) {
            return updateImuFallback()
        }
        
        return false
    }
    
    /**
     * Update pose using IMU-only tracking (gyroscope + accelerometer).
     * Simple orientation-based tracking without visual features.
     */
    private fun updateImuFallback(): Boolean {
        if (lastImuTime == 0L) {
            // No IMU data yet
            return false
        }
        
        // Update rotation from gyroscope (simple integration)
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastImuTime) / 1e9f  // Convert to seconds
        
        if (deltaTime > 0.1f || deltaTime < 0f) {
            // Too large time step or invalid - reset
            lastImuTime = currentTime
            return false
        }
        
        // Simple gyroscope integration (Euler angles approximation)
        // This is a simplified approach - for production, use proper quaternion integration
        val gyroX = lastGyroX * deltaTime
        val gyroY = lastGyroY * deltaTime
        val gyroZ = lastGyroZ * deltaTime
        
        // Build rotation matrix from gyroscope (simplified)
        val rotationDelta = FloatArray(16)
        android.opengl.Matrix.setIdentityM(rotationDelta, 0)
        android.opengl.Matrix.rotateM(rotationDelta, 0, Math.toDegrees(gyroX.toDouble()).toFloat(), 1f, 0f, 0f)
        android.opengl.Matrix.rotateM(rotationDelta, 0, Math.toDegrees(gyroY.toDouble()).toFloat(), 0f, 1f, 0f)
        android.opengl.Matrix.rotateM(rotationDelta, 0, Math.toDegrees(gyroZ.toDouble()).toFloat(), 0f, 0f, 1f)
        
        // Apply rotation delta to current rotation
        val tempMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(tempMatrix, 0, imuRotationMatrix, 0, rotationDelta, 0)
        System.arraycopy(tempMatrix, 0, imuRotationMatrix, 0, 16)
        
        // Build pose matrix: camera at fixed height, rotated by IMU
        android.opengl.Matrix.setIdentityM(currentPoseMatrix, 0)
        
        // Apply rotation
        System.arraycopy(imuRotationMatrix, 0, currentPoseMatrix, 0, 12)  // Copy rotation part
        
        // Set position: camera at estimated height above ground
        currentPoseMatrix[12] = 0f  // X position (centered)
        currentPoseMatrix[13] = cameraHeightEstimate  // Y position (height)
        currentPoseMatrix[14] = 0f  // Z position (centered)
        
        // Compute view matrix (inverse of pose matrix)
        android.opengl.Matrix.invertM(currentViewMatrix, 0, currentPoseMatrix, 0)
        
        lastImuTime = currentTime
        return true
    }

    override fun getViewMatrix(): FloatArray {
        return currentViewMatrix.copyOf()
    }

    override fun getProjectionMatrix(near: Float, far: Float): FloatArray {
        val intrinsics = cameraIntrinsics ?: return FloatArray(16).apply {
            android.opengl.Matrix.setIdentityM(this, 0)
        }

        val fx = intrinsics.fx
        val fy = intrinsics.fy
        val cx = intrinsics.cx
        val cy = intrinsics.cy
        val width = intrinsics.width.toFloat()
        val height = intrinsics.height.toFloat()

        // Compute projection matrix from intrinsics
        val proj = FloatArray(16)
        android.opengl.Matrix.setIdentityM(proj, 0)

        // OpenGL-style projection matrix
        proj[0] = 2.0f * fx / width
        proj[5] = 2.0f * fy / height
        proj[8] = 1.0f - 2.0f * cx / width
        proj[9] = 2.0f * cy / height - 1.0f
        proj[10] = -(far + near) / (far - near)
        proj[11] = -1.0f
        proj[14] = -2.0f * far * near / (far - near)
        proj[15] = 0.0f

        return proj
    }

    override fun getCameraIntrinsics(): CameraIntrinsics? {
        return cameraIntrinsics
    }

    override fun hitTest(viewX: Float, viewY: Float): List<HitResult> {
        val results = mutableListOf<HitResult>()

        // Get plane from estimator
        val planeInfo = planeEstimator.getPlaneInfo() ?: return results

        // Unproject view coordinates to 3D ray
        val intrinsics = cameraIntrinsics ?: return results
        val viewWidth = intrinsics.width.toFloat()
        val viewHeight = intrinsics.height.toFloat()

        // Normalize coordinates to [-1, 1]
        val nx = (viewX / viewWidth) * 2.0f - 1.0f
        val ny = 1.0f - (viewY / viewHeight) * 2.0f

        // Unproject to 3D ray (in camera space)
        val fx = intrinsics.fx
        val fy = intrinsics.fy
        val cx = intrinsics.cx
        val cy = intrinsics.cy

        // Ray direction in camera space
        val rayDir = floatArrayOf(
            (nx * viewWidth / 2.0f - cx) / fx,
            (ny * viewHeight / 2.0f - cy) / fy,
            -1.0f  // Camera looks down -Z
        )

        // Normalize ray direction
        val rayLen = sqrt(rayDir[0] * rayDir[0] + rayDir[1] * rayDir[1] + rayDir[2] * rayDir[2])
        if (rayLen > 0.001f) {
            rayDir[0] /= rayLen
            rayDir[1] /= rayLen
            rayDir[2] /= rayLen
        }

        // Transform ray to world space using current pose
        val worldRayDir = FloatArray(3)
        val worldRayOrigin = FloatArray(3)
        
        // Ray origin is camera position (from pose matrix)
        worldRayOrigin[0] = currentPoseMatrix[12]
        worldRayOrigin[1] = currentPoseMatrix[13]
        worldRayOrigin[2] = currentPoseMatrix[14]

        // Transform ray direction
        worldRayDir[0] = currentPoseMatrix[0] * rayDir[0] + currentPoseMatrix[4] * rayDir[1] + currentPoseMatrix[8] * rayDir[2]
        worldRayDir[1] = currentPoseMatrix[1] * rayDir[0] + currentPoseMatrix[5] * rayDir[1] + currentPoseMatrix[9] * rayDir[2]
        worldRayDir[2] = currentPoseMatrix[2] * rayDir[0] + currentPoseMatrix[6] * rayDir[1] + currentPoseMatrix[10] * rayDir[2]

        // Intersect ray with plane
        val planeNormal = floatArrayOf(0f, 1f, 0f)  // Floor plane normal (Y-up)
        val planePoint = planeInfo.centerPose.translation

        // Ray-plane intersection
        val denom = planeNormal[0] * worldRayDir[0] + planeNormal[1] * worldRayDir[1] + planeNormal[2] * worldRayDir[2]
        
        if (abs(denom) > 0.001f) {
            val t = ((planePoint[0] - worldRayOrigin[0]) * planeNormal[0] +
                     (planePoint[1] - worldRayOrigin[1]) * planeNormal[1] +
                     (planePoint[2] - worldRayOrigin[2]) * planeNormal[2]) / denom

            if (t > 0.1f && t < 10.0f) {  // Valid intersection distance
                val hitPoint = floatArrayOf(
                    worldRayOrigin[0] + worldRayDir[0] * t,
                    worldRayOrigin[1] + worldRayDir[1] * t,
                    worldRayOrigin[2] + worldRayDir[2] * t
                )

                val hitPose = Pose(
                    translation = hitPoint,
                    rotation = floatArrayOf(0f, 0f, 0f, 1f)  // Identity rotation
                )

                results.add(
                    HitResult(
                        hitPose = hitPose,
                        distance = t,
                        trackableType = TrackableType.PLANE,
                        trackableId = null
                    )
                )
            }
        }

        return results
    }

    override fun getPlanes(): List<PlaneInfo> {
        val planeInfo = planeEstimator.getPlaneInfo()
        return if (planeInfo != null) {
            listOf(planeInfo)
        } else {
            emptyList()
        }
    }

    override fun isTracking(): Boolean {
        return isInitialized && (vioEngine.isTracking() || useImuFallback)
    }
    
    /**
     * Check if currently using IMU-only fallback mode.
     */
    fun isUsingImuFallback(): Boolean {
        return useImuFallback
    }
    
    /**
     * Reset fallback mode and try VIO again.
     */
    fun resetFallback() {
        useImuFallback = false
        vioFailureCount = 0
        Log.d(TAG, "Reset IMU fallback, attempting VIO tracking again")
    }

    override fun getLightEstimate(): LightEstimate? {
        // VIO doesn't provide light estimation
        return null
    }

    override fun transformCoordinates2d(
        srcType: CoordinateSpace,
        srcCoords: FloatArray,
        dstType: CoordinateSpace
    ): FloatArray? {
        // Simplified coordinate transformation for VIO
        // Real implementation would use proper camera calibration
        
        val intrinsics = cameraIntrinsics ?: return null
        
        return when {
            srcType == CoordinateSpace.IMAGE_PIXELS && dstType == CoordinateSpace.VIEW -> {
                // Image pixels to view coordinates (same for VIO)
                srcCoords.copyOf()
            }
            srcType == CoordinateSpace.IMAGE_NORMALIZED && dstType == CoordinateSpace.VIEW -> {
                // Normalized [0,1] to view pixels
                srcCoords.mapIndexed { i, coord ->
                    if (i % 2 == 0) coord * intrinsics.width else coord * intrinsics.height
                }.toFloatArray()
            }
            else -> null  // Other transformations not implemented
        }
    }

    override fun acquireCameraImage(): CameraImage? {
        // Get latest Camera2 image from Camera2Manager
        val camera2Image = camera2Manager?.acquireLatestImage() ?: return null
        return Camera2Image(camera2Image)
    }

    override fun getCameraTextureId(): Int {
        return cameraTextureId
    }

    override fun setCameraTextureName(textureId: Int) {
        cameraTextureId = textureId
        // VIO doesn't use external texture like ARCore
    }

    override fun release() {
        vioEngine.release()
        planeEstimator.clearPoints()
        isInitialized = false
    }

    /**
     * Initialize VIO pose provider with camera parameters.
     * Loads calibration values from SharedPreferences if available.
     */
    fun initialize(width: Int, height: Int, fx: Float, fy: Float, cx: Float, cy: Float, context: android.content.Context? = null): Boolean {
        if (isInitialized) {
            return true
        }

        // Load calibration values if context provided
        context?.let { ctx ->
            val phoneHeight = CalibrationManager.getPhoneHeight(ctx)
            val shoeSize = CalibrationManager.getShoeSize(ctx)
            
            phoneHeight?.let {
                cameraHeightEstimate = it
                Log.d(TAG, "Loaded phone height calibration: ${it}m")
            }
            
            shoeSize?.let {
                // Convert shoe size to scale factor (normalize to reference 26cm)
                scaleCalibration = (it / 26.0f).coerceIn(0.7f, 1.3f)
                Log.d(TAG, "Loaded shoe size calibration: ${it}cm (scale: $scaleCalibration)")
            }
        }

        Log.d(TAG, "Initializing VIO engine: ${width}x${height}, fx=$fx fy=$fy cx=$cx cy=$cy")
        val success = vioEngine.init(width, height, fx, fy, cx, cy)
        if (success) {
            cameraIntrinsics = CameraIntrinsics(fx, fy, cx, cy, width, height)
            isInitialized = true
            Log.d(TAG, "VioPoseProvider initialized successfully: ${width}x${height}, scale=$scaleCalibration, height=$cameraHeightEstimate")
        } else {
            Log.e(TAG, "Failed to initialize VioPoseProvider - VIO engine init returned false")
            Log.e(TAG, "Check native library loading and VIO engine implementation")
        }

        return success
    }
    
    /**
     * Get scale calibration factor.
     */
    fun getScaleCalibration(): Float = scaleCalibration
    
    /**
     * Get camera height estimate.
     */
    fun getCameraHeightEstimate(): Float = cameraHeightEstimate
    
    /**
     * Update calibration values (called when user calibrates).
     */
    fun updateCalibration(scale: Float, height: Float) {
        scaleCalibration = scale.coerceIn(0.5f, 2.0f)
        cameraHeightEstimate = height.coerceIn(0.5f, 3.0f)
        Log.d(TAG, "Calibration updated: scale=$scaleCalibration, height=$cameraHeightEstimate")
    }

    /**
     * Start tracking.
     */
    fun start(): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "Cannot start VIO tracking: not initialized")
            return false
        }
        Log.d(TAG, "Starting VIO tracking...")
        val success = vioEngine.start()
        if (success) {
            Log.d(TAG, "VIO tracking started successfully")
        } else {
            Log.e(TAG, "Failed to start VIO tracking - engine start returned false")
        }
        return success
    }

    /**
     * Stop tracking.
     */
    fun stop() {
        vioEngine.stop()
    }

    /**
     * Feed IMU data to VIO and plane estimator.
     * Also stores IMU data for fallback mode.
     */
    fun feedImu(accelX: Float, accelY: Float, accelZ: Float, 
                gyroX: Float, gyroY: Float, gyroZ: Float, timestampNs: Long) {
        // Store IMU data for fallback mode
        lastAccelX = accelX
        lastAccelY = accelY
        lastAccelZ = accelZ
        lastGyroX = gyroX
        lastGyroY = gyroY
        lastGyroZ = gyroZ
        lastImuTime = timestampNs
        
        // Feed to VIO engine (if not in fallback mode)
        if (!useImuFallback) {
            vioEngine.feedImu(accelX, accelY, accelZ, gyroX, gyroY, gyroZ, timestampNs)
        }
        
        // Update plane estimator with gravity
        planeEstimator.updateGravity(accelX, accelY, accelZ)
    }

    /**
     * Feed camera frame to VIO.
     */
    fun feedFrame(yuvData: ByteArray, width: Int, height: Int, timestampNs: Long): Boolean {
        return vioEngine.feedFrame(yuvData, width, height, timestampNs)
    }
}

