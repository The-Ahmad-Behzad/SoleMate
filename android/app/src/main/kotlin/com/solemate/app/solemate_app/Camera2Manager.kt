package com.solemate.app.solemate_app

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Manages Camera2 API for VIO mode.
 * Handles camera device, capture session, frame callbacks, and IMU sensor integration.
 */
class Camera2Manager(
    private val context: Context,
    private val vioEngine: VioEngine,
    private var vioPoseProvider: VioPoseProvider?
) {
    /**
     * Set VioPoseProvider after creation (to break circular dependency)
     */
    fun setVioPoseProvider(provider: VioPoseProvider) {
        vioPoseProvider = provider
    }
    companion object {
        private const val TAG = "Camera2Manager"
        private const val IMAGE_READER_WIDTH = 640
        private const val IMAGE_READER_HEIGHT = 480
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080
    }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    // IMU sensors
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private val imuListener = ImuSensorListener()
    
    private val cameraStateCallback = CameraStateCallback()
    private val captureStateCallback = CaptureStateCallback()
    
    private val cameraOpenCloseLock = Semaphore(1, true)
    private var isCapturing = false
    
    // Surface for rendering (from BackgroundRenderer)
    private var previewSurface: Surface? = null
    
    // Latest camera image for ML processing (thread-safe access)
    @Volatile
    private var latestImage: android.media.Image? = null
    private val imageLock = Any()

    /**
     * Initialize camera manager and start background thread.
     */
    fun initialize() {
        startBackgroundThread()
        setupSensors()
    }

    /**
     * Open camera and create capture session.
     * @param surfaceTexture SurfaceTexture for camera preview rendering
     */
    fun openCamera(surfaceTexture: SurfaceTexture) {
        Log.d(TAG, "openCamera() called")
        if (cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS).not()) {
            Log.e(TAG, "Time out waiting to lock camera opening")
            throw RuntimeException("Time out waiting to lock camera opening.")
        }

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIdList = cameraManager.cameraIdList
            if (cameraIdList.isEmpty()) {
                Log.e(TAG, "No cameras available on device")
                throw RuntimeException("No cameras available on device")
            }
            val cameraId = cameraIdList[0] // Use back camera
            Log.d(TAG, "Opening camera: $cameraId (${cameraIdList.size} cameras available)")
            
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (map == null) {
                Log.e(TAG, "Cannot get available preview/video sizes for camera $cameraId")
                throw RuntimeException("Cannot get available preview/video sizes")
            }

            // Log available sizes for diagnostics
            val availableSizes = map.getOutputSizes(SurfaceTexture::class.java)
            Log.d(TAG, "Available SurfaceTexture sizes: ${availableSizes.joinToString { "${it.width}x${it.height}" }}")

            // Choose preview size (prefer 640x480 for VIO performance)
            val previewSize = chooseOptimalSize(
                availableSizes,
                IMAGE_READER_WIDTH,
                IMAGE_READER_HEIGHT
            )
            Log.d(TAG, "Selected preview size: ${previewSize.width}x${previewSize.height}")

            // Set preview surface texture size
            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
            previewSurface = Surface(surfaceTexture)
            Log.d(TAG, "Preview surface created: ${previewSize.width}x${previewSize.height}")

            // Create ImageReader for YUV frames to feed VIO
            imageReader = ImageReader.newInstance(
                IMAGE_READER_WIDTH,
                IMAGE_READER_HEIGHT,
                android.graphics.ImageFormat.YUV_420_888,
                2
            ).apply {
                setOnImageAvailableListener(ImageAvailableListener(), backgroundHandler)
            }
            Log.d(TAG, "ImageReader created: ${IMAGE_READER_WIDTH}x${IMAGE_READER_HEIGHT} (YUV_420_888)")

            // Open camera
            Log.d(TAG, "Requesting camera open...")
            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception opening camera: ${e.message}", e)
            cameraOpenCloseLock.release()
            throw RuntimeException("Camera permission denied: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception opening camera: ${e.message}", e)
            Log.e(TAG, "Stack trace:", e)
            cameraOpenCloseLock.release()
            throw RuntimeException("Failed to open camera: ${e.message}", e)
        }
    }

    /**
     * Start capturing frames.
     */
    fun startCapture() {
        if (isCapturing) {
            return
        }
        
        captureSession?.let { session ->
            try {
                val requestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                requestBuilder?.let { builder ->
                    previewSurface?.let { builder.addTarget(it) }
                    imageReader?.surface?.let { builder.addTarget(it) }
                    
                    builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                    
                    val request = builder.build()
                    session.setRepeatingRequest(request, null, backgroundHandler)
                    isCapturing = true
                    Log.d(TAG, "Capture started")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start capture: ${e.message}", e)
            }
        }
    }

    /**
     * Stop capturing frames.
     */
    fun stopCapture() {
        if (!isCapturing) {
            return
        }
        
        try {
            captureSession?.stopRepeating()
            isCapturing = false
            Log.d(TAG, "Capture stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop capture: ${e.message}", e)
        }
    }

    /**
     * Close camera and release resources.
     */
    fun closeCamera() {
        try {
            stopCapture()
            stopSensors()
            
            captureSession?.close()
            captureSession = null
            
            cameraDevice?.close()
            cameraDevice = null
            
            imageReader?.close()
            imageReader = null
            
            previewSurface?.release()
            previewSurface = null
            
            // Clean up latest image
            synchronized(imageLock) {
                latestImage?.close()
                latestImage = null
            }
            
            cameraOpenCloseLock.release()
            Log.d(TAG, "Camera closed")
        } catch (e: Exception) {
            Log.e(TAG, "Exception closing camera: ${e.message}", e)
        }
    }

    /**
     * Release all resources.
     */
    fun release() {
        closeCamera()
        stopBackgroundThread()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper!!)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while stopping background thread: ${e.message}")
        }
    }

    private fun setupSensors() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        
        // Use SENSOR_DELAY_GAME (20ms) instead of SENSOR_DELAY_FASTEST (0ms)
        // SENSOR_DELAY_FASTEST requires HIGH_SAMPLING_RATE_SENSORS permission
        // SENSOR_DELAY_GAME is fast enough for VIO and doesn't require special permission
        val sensorDelay = SensorManager.SENSOR_DELAY_GAME
        
        accelerometer?.let {
            try {
                sensorManager.registerListener(imuListener, it, sensorDelay)
                Log.d(TAG, "Accelerometer registered with delay: $sensorDelay")
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to register accelerometer: ${e.message}")
                // Try with slower delay as fallback
                try {
                    sensorManager.registerListener(imuListener, it, SensorManager.SENSOR_DELAY_UI)
                    Log.d(TAG, "Accelerometer registered with SENSOR_DELAY_UI as fallback")
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to register accelerometer with fallback: ${e2.message}")
                }
            }
        }
        gyroscope?.let {
            try {
                sensorManager.registerListener(imuListener, it, sensorDelay)
                Log.d(TAG, "Gyroscope registered with delay: $sensorDelay")
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to register gyroscope: ${e.message}")
                // Try with slower delay as fallback
                try {
                    sensorManager.registerListener(imuListener, it, SensorManager.SENSOR_DELAY_UI)
                    Log.d(TAG, "Gyroscope registered with SENSOR_DELAY_UI as fallback")
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to register gyroscope with fallback: ${e2.message}")
                }
            }
        }
        
        Log.d(TAG, "Sensors registered: accel=${accelerometer != null}, gyro=${gyroscope != null}")
    }

    private fun stopSensors() {
        sensorManager.unregisterListener(imuListener)
    }

    private fun chooseOptimalSize(
        choices: Array<Size>,
        textureViewWidth: Int,
        textureViewHeight: Int
    ): Size {
        // Prefer sizes close to target for VIO
        val targetSize = Size(IMAGE_READER_WIDTH, IMAGE_READER_HEIGHT)
        
        // Find closest match
        var optimalSize = choices[0]
        var minDiff = Int.MAX_VALUE
        
        for (size in choices) {
            val diff = kotlin.math.abs(size.width - targetSize.width) + 
                      kotlin.math.abs(size.height - targetSize.height)
            if (diff < minDiff) {
                minDiff = diff
                optimalSize = size
            }
        }
        
        return optimalSize
    }

    /**
     * Camera state callback.
     */
    private inner class CameraStateCallback : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "Camera opened successfully: ${camera.id}")
            cameraOpenCloseLock.release()
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.w(TAG, "Camera disconnected: ${camera.id}")
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            val errorMsg = when (error) {
                CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "Camera device error"
                CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "Camera disabled"
                CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "Camera in use"
                CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "Camera service error"
                CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "Max cameras in use"
                else -> "Unknown error ($error)"
            }
            Log.e(TAG, "Camera error on ${camera.id}: $errorMsg (code: $error)")
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
        }

        private fun createCaptureSession() {
            try {
                val surfaces = mutableListOf<Surface>()
                previewSurface?.let { 
                    surfaces.add(it)
                    Log.d(TAG, "Added preview surface to capture session")
                } ?: Log.w(TAG, "Preview surface is null")
                
                imageReader?.surface?.let { 
                    surfaces.add(it)
                    Log.d(TAG, "Added ImageReader surface to capture session")
                } ?: Log.w(TAG, "ImageReader surface is null")

                if (surfaces.isEmpty()) {
                    Log.e(TAG, "No surfaces available for capture session")
                    return
                }

                Log.d(TAG, "Creating capture session with ${surfaces.size} surfaces")
                cameraDevice?.createCaptureSession(
                    surfaces,
                    captureStateCallback,
                    backgroundHandler
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating capture session: ${e.message}", e)
            }
        }
    }

    /**
     * Capture session state callback.
     */
    private inner class CaptureStateCallback : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            Log.d(TAG, "Capture session configured successfully")
            captureSession = session
            startCapture()
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e(TAG, "Capture session configuration failed - camera may not support requested configuration")
            Log.e(TAG, "Preview surface: ${previewSurface != null}, ImageReader surface: ${imageReader?.surface != null}")
        }

        override fun onReady(session: CameraCaptureSession) {
            Log.d(TAG, "Capture session ready")
        }

        override fun onActive(session: CameraCaptureSession) {
            Log.d(TAG, "Capture session active")
        }

        override fun onClosed(session: CameraCaptureSession) {
            Log.d(TAG, "Capture session closed")
        }
    }

    /**
     * Get the latest camera image for ML processing.
     * Returns null if no image is available.
     * Caller must close the returned image when done.
     */
    fun acquireLatestImage(): android.media.Image? {
        synchronized(imageLock) {
            return latestImage?.let { image ->
                // Create a reference to the image (caller will close it)
                latestImage = null  // Clear so next frame can be stored
                image
            }
        }
    }

    /**
     * Image available listener - feeds frames to VIO engine.
     */
    private inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {
            val image = reader.acquireLatestImage() ?: return

            try {
                // Store latest image for ML processing (thread-safe)
                synchronized(imageLock) {
                    latestImage?.close()  // Close previous image if any
                    latestImage = image
                }
                
                // Convert Image to YUV byte array for VIO
                val yuvData = imageToYuvByteArray(image)
                val timestampNs = System.nanoTime()
                
                // Feed to VIO engine
                vioPoseProvider?.feedFrame(yuvData, IMAGE_READER_WIDTH, IMAGE_READER_HEIGHT, timestampNs)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
                synchronized(imageLock) {
                    latestImage?.close()
                    latestImage = null
                }
            }
            // Note: Don't close image here - it's stored in latestImage for ML processing
        }

        private fun imageToYuvByteArray(image: android.media.Image): ByteArray {
            val planes = image.planes
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            return nv21
        }
    }

    /**
     * IMU sensor listener - feeds IMU data to VIO engine.
     */
    private inner class ImuSensorListener : SensorEventListener {
        private var lastAccelTime = 0L
        private var lastGyroTime = 0L
        private var accelX = 0f
        private var accelY = 0f
        private var accelZ = 0f
        private var gyroX = 0f
        private var gyroY = 0f
        private var gyroZ = 0f

        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    accelX = event.values[0]
                    accelY = event.values[1]
                    accelZ = event.values[2]
                    lastAccelTime = System.nanoTime()
                    
                    // Feed to VIO (use last gyro values if available)
                    if (lastGyroTime > 0) {
                        val timestampNs = (lastAccelTime + lastGyroTime) / 2
                        vioPoseProvider?.feedImu(
                            accelX, accelY, accelZ,
                            gyroX, gyroY, gyroZ,
                            timestampNs
                        )
                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    gyroX = event.values[0]
                    gyroY = event.values[1]
                    gyroZ = event.values[2]
                    lastGyroTime = System.nanoTime()
                    
                    // Feed to VIO (use last accel values if available)
                    if (lastAccelTime > 0) {
                        val timestampNs = (lastAccelTime + lastGyroTime) / 2
                        vioPoseProvider?.feedImu(
                            accelX, accelY, accelZ,
                            gyroX, gyroY, gyroZ,
                            timestampNs
                        )
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Ignore
        }
    }
}

