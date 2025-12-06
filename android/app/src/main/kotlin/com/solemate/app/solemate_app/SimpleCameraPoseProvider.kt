package com.solemate.app.solemate_app

import android.util.Log
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.*

/**
 * Simple camera pose provider that focuses on getting camera preview working.
 * Uses IMU sensors for basic orientation tracking.
 * No complex VIO - just camera feed + IMU rotation.
 */
class SimpleCameraPoseProvider(
    private val context: android.content.Context,
    private val rotationHelper: DisplayRotationHelper,
    private val cameraManager: SimpleCameraManager? = null
) : WorldPoseProvider {

    companion object {
        private const val TAG = "SimpleCameraPoseProvider"
    }

    private var isInitialized = false
    private var cameraTextureId = -1
    private val currentViewMatrix = FloatArray(16)
    private val currentProjMatrix = FloatArray(16)
    private var cameraIntrinsics: CameraIntrinsics? = null
    
    // IMU sensors for orientation tracking
    private val sensorManager: SensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private val imuListener = ImuSensorListener()
    
    // IMU-based rotation
    private val imuRotationMatrix = FloatArray(16)
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
    
    // Default camera height estimate
    private var cameraHeightEstimate = 1.5f  // 1.5 meters

    init {
        android.opengl.Matrix.setIdentityM(currentViewMatrix, 0)
        android.opengl.Matrix.setIdentityM(currentProjMatrix, 0)
        android.opengl.Matrix.setIdentityM(imuRotationMatrix, 0)
    }

    /**
     * Initialize with camera parameters.
     */
    fun initialize(width: Int, height: Int, fx: Float, fy: Float, cx: Float, cy: Float): Boolean {
        if (isInitialized) {
            return true
        }

        cameraIntrinsics = CameraIntrinsics(fx, fy, cx, cy, width, height)
        setupSensors()
        isInitialized = true
        Log.d(TAG, "SimpleCameraPoseProvider initialized: ${width}x${height}")
        return true
    }

    private fun setupSensors() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        val sensorDelay = SensorManager.SENSOR_DELAY_GAME
        
        accelerometer?.let {
            try {
                sensorManager.registerListener(imuListener, it, sensorDelay)
                Log.d(TAG, "Accelerometer registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register accelerometer: ${e.message}")
            }
        }
        
        gyroscope?.let {
            try {
                sensorManager.registerListener(imuListener, it, sensorDelay)
                Log.d(TAG, "Gyroscope registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register gyroscope: ${e.message}")
            }
        }
    }

    private fun stopSensors() {
        sensorManager.unregisterListener(imuListener)
    }

    override fun update(): Boolean {
        if (!isInitialized) {
            return false
        }

        // Update rotation from IMU
        updateImuRotation()
        
        // Build view matrix from IMU rotation
        // Camera at fixed height, rotated by IMU
        val poseMatrix = FloatArray(16)
        android.opengl.Matrix.setIdentityM(poseMatrix, 0)
        
        // Apply rotation
        System.arraycopy(imuRotationMatrix, 0, poseMatrix, 0, 12)
        
        // Set position: camera at estimated height
        poseMatrix[12] = 0f
        poseMatrix[13] = cameraHeightEstimate
        poseMatrix[14] = 0f
        
        // Compute view matrix (inverse of pose)
        android.opengl.Matrix.invertM(currentViewMatrix, 0, poseMatrix, 0)
        
        return true
    }

    private fun updateImuRotation() {
        if (lastImuTime == 0L) {
            return
        }
        
        val currentTime = System.nanoTime()
        val deltaTime = (currentTime - lastImuTime) / 1e9f
        
        if (deltaTime > 0.1f || deltaTime < 0f) {
            lastImuTime = currentTime
            return
        }
        
        // Simple gyroscope integration
        val gyroX = lastGyroX * deltaTime
        val gyroY = lastGyroY * deltaTime
        val gyroZ = lastGyroZ * deltaTime
        
        // Build rotation delta
        val rotationDelta = FloatArray(16)
        android.opengl.Matrix.setIdentityM(rotationDelta, 0)
        android.opengl.Matrix.rotateM(rotationDelta, 0, Math.toDegrees(gyroX.toDouble()).toFloat(), 1f, 0f, 0f)
        android.opengl.Matrix.rotateM(rotationDelta, 0, Math.toDegrees(gyroY.toDouble()).toFloat(), 0f, 1f, 0f)
        android.opengl.Matrix.rotateM(rotationDelta, 0, Math.toDegrees(gyroZ.toDouble()).toFloat(), 0f, 0f, 1f)
        
        // Apply rotation delta
        val tempMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(tempMatrix, 0, imuRotationMatrix, 0, rotationDelta, 0)
        System.arraycopy(tempMatrix, 0, imuRotationMatrix, 0, 16)
        
        lastImuTime = currentTime
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

        val proj = FloatArray(16)
        android.opengl.Matrix.setIdentityM(proj, 0)

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
        // Simple hit test: assume floor plane at y=0
        val intrinsics = cameraIntrinsics ?: return emptyList()
        
        val viewWidth = intrinsics.width.toFloat()
        val viewHeight = intrinsics.height.toFloat()
        
        // Normalize coordinates
        val nx = (viewX / viewWidth) * 2.0f - 1.0f
        val ny = 1.0f - (viewY / viewHeight) * 2.0f
        
        // Unproject to 3D ray
        val fx = intrinsics.fx
        val fy = intrinsics.fy
        val cx = intrinsics.cx
        val cy = intrinsics.cy
        
        val rayDir = floatArrayOf(
            (nx * viewWidth / 2.0f - cx) / fx,
            (ny * viewHeight / 2.0f - cy) / fy,
            -1.0f
        )
        
        val rayLen = sqrt(rayDir[0] * rayDir[0] + rayDir[1] * rayDir[1] + rayDir[2] * rayDir[2])
        if (rayLen > 0.001f) {
            rayDir[0] /= rayLen
            rayDir[1] /= rayLen
            rayDir[2] /= rayLen
        }
        
        // Transform ray to world space
        val worldRayDir = FloatArray(3)
        worldRayDir[0] = imuRotationMatrix[0] * rayDir[0] + imuRotationMatrix[4] * rayDir[1] + imuRotationMatrix[8] * rayDir[2]
        worldRayDir[1] = imuRotationMatrix[1] * rayDir[0] + imuRotationMatrix[5] * rayDir[1] + imuRotationMatrix[9] * rayDir[2]
        worldRayDir[2] = imuRotationMatrix[2] * rayDir[0] + imuRotationMatrix[6] * rayDir[1] + imuRotationMatrix[10] * rayDir[2]
        
        // Intersect with floor plane (y=0)
        val worldRayOrigin = floatArrayOf(0f, cameraHeightEstimate, 0f)
        val planeNormal = floatArrayOf(0f, 1f, 0f)
        val planePoint = floatArrayOf(0f, 0f, 0f)
        
        val denom = planeNormal[0] * worldRayDir[0] + planeNormal[1] * worldRayDir[1] + planeNormal[2] * worldRayDir[2]
        
        if (abs(denom) > 0.001f) {
            val t = ((planePoint[0] - worldRayOrigin[0]) * planeNormal[0] +
                     (planePoint[1] - worldRayOrigin[1]) * planeNormal[1] +
                     (planePoint[2] - worldRayOrigin[2]) * planeNormal[2]) / denom
            
            if (t > 0.1f && t < 10.0f) {
                val hitPoint = floatArrayOf(
                    worldRayOrigin[0] + worldRayDir[0] * t,
                    worldRayOrigin[1] + worldRayDir[1] * t,
                    worldRayOrigin[2] + worldRayDir[2] * t
                )
                
                val hitPose = Pose(
                    translation = hitPoint,
                    rotation = floatArrayOf(0f, 0f, 0f, 1f)
                )
                
                return listOf(
                    HitResult(
                        hitPose = hitPose,
                        distance = t,
                        trackableType = TrackableType.PLANE,
                        trackableId = null
                    )
                )
            }
        }
        
        return emptyList()
    }

    override fun getPlanes(): List<PlaneInfo> {
        // Return a simple floor plane
        val floorPose = Pose(
            translation = floatArrayOf(0f, 0f, 0f),
            rotation = floatArrayOf(0f, 0f, 0f, 1f)
        )
        
        return listOf(
            PlaneInfo(
                centerPose = floorPose,
                extentX = 5.0f,  // 5m x 5m floor
                extentZ = 5.0f,
                polygon = null,
                trackingState = TrackingState.TRACKING
            )
        )
    }

    override fun isTracking(): Boolean {
        return isInitialized
    }

    override fun getLightEstimate(): LightEstimate? {
        return null
    }

    override fun transformCoordinates2d(
        srcType: CoordinateSpace,
        srcCoords: FloatArray,
        dstType: CoordinateSpace
    ): FloatArray? {
        val intrinsics = cameraIntrinsics ?: return null
        
        return when {
            srcType == CoordinateSpace.IMAGE_PIXELS && dstType == CoordinateSpace.VIEW -> {
                srcCoords.copyOf()
            }
            srcType == CoordinateSpace.IMAGE_NORMALIZED && dstType == CoordinateSpace.VIEW -> {
                srcCoords.mapIndexed { i, coord ->
                    if (i % 2 == 0) coord * intrinsics.width else coord * intrinsics.height
                }.toFloatArray()
            }
            else -> null
        }
    }

    override fun acquireCameraImage(): CameraImage? {
        // Get latest image from camera manager
        if (cameraManager == null) {
            Log.w(TAG, "Camera manager is null, cannot acquire image")
            return null
        }
        
        val image = cameraManager.acquireLatestImage()
        if (image == null) {
            // Log occasionally to avoid spam
            if (System.currentTimeMillis() % 2000 < 100) {
                Log.d(TAG, "No camera image available from camera manager")
            }
            return null
        }
        
        return try {
            val wrapped = Camera2Image(image)
            Log.v(TAG, "Camera image acquired: ${image.width}x${image.height}")
            wrapped
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wrap camera image: ${e.message}", e)
            image.close()
            null
        }
    }

    override fun getCameraTextureId(): Int {
        return cameraTextureId
    }

    override fun setCameraTextureName(textureId: Int) {
        cameraTextureId = textureId
        Log.d(TAG, "Camera texture ID set: $textureId")
    }

    override fun release() {
        stopSensors()
        isInitialized = false
        Log.d(TAG, "SimpleCameraPoseProvider released")
    }

    /**
     * IMU sensor listener
     */
    private inner class ImuSensorListener : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    lastAccelX = event.values[0]
                    lastAccelY = event.values[1]
                    lastAccelZ = event.values[2]
                    lastImuTime = System.nanoTime()
                }
                Sensor.TYPE_GYROSCOPE -> {
                    lastGyroX = event.values[0]
                    lastGyroY = event.values[1]
                    lastGyroZ = event.values[2]
                    lastImuTime = System.nanoTime()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Ignore
        }
    }
}




