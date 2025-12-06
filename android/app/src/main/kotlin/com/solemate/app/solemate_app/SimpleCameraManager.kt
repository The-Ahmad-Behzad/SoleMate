package com.solemate.app.solemate_app

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Simplified Camera2 manager focused ONLY on getting camera preview working.
 * No VIO engine integration, no complex frame processing.
 * Just opens camera and streams to SurfaceTexture.
 */
class SimpleCameraManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "SimpleCameraManager"
        private const val MAX_PREVIEW_WIDTH = 1920
        private const val MAX_PREVIEW_HEIGHT = 1080
    }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    
    private val cameraStateCallback = CameraStateCallback()
    private val captureStateCallback = CaptureStateCallback()
    
    private val cameraOpenCloseLock = Semaphore(1, true)
    private var isCapturing = false
    
    private var previewSurface: Surface? = null
    private var sensorOrientation: Int = 0
    private var cameraId: String? = null
    
    // ImageReader for capturing frames for MediaPipe
    private var imageReader: ImageReader? = null
    private val imageLock = Object()
    @Volatile
    private var latestImage: Image? = null
    private val IMAGE_READER_WIDTH = 640
    private val IMAGE_READER_HEIGHT = 480

    /**
     * Initialize camera manager and start background thread.
     */
    fun initialize() {
        startBackgroundThread()
        Log.d(TAG, "SimpleCameraManager initialized")
    }
    
    /**
     * Get camera sensor orientation (needed for rotation correction).
     */
    fun getSensorOrientation(): Int {
        return sensorOrientation
    }
    
    /**
     * Get camera ID.
     */
    fun getCameraId(): String? {
        return cameraId
    }
    
    /**
     * Get the latest camera image for processing (e.g., MediaPipe).
     * Caller must close the image when done.
     */
    fun acquireLatestImage(): Image? {
        synchronized(imageLock) {
            val image = latestImage
            latestImage = null  // Transfer ownership to caller
            if (image != null) {
                Log.v(TAG, "Image acquired by caller: ${image.width}x${image.height}")
            }
            return image
        }
    }

    /**
     * Open camera and create capture session.
     * @param surfaceTexture SurfaceTexture for camera preview rendering
     */
    fun openCamera(surfaceTexture: SurfaceTexture) {
        Log.d(TAG, "openCamera() called, surfaceTexture=$surfaceTexture")
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
            cameraId = cameraIdList[0] // Use back camera
            val currentCameraId = cameraId!! // Non-null for use in this scope
            Log.d(TAG, "Opening camera: $currentCameraId (${cameraIdList.size} cameras available)")
            Log.d(TAG, "SurfaceTexture state: isReleased=${try { surfaceTexture.isReleased } catch (e: Exception) { "unknown" }}")
            
            val characteristics = cameraManager.getCameraCharacteristics(currentCameraId)
            
            // Get sensor orientation for rotation correction
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            Log.d(TAG, "Camera sensor orientation: $sensorOrientation degrees")
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (map == null) {
                Log.e(TAG, "Cannot get available preview/video sizes for camera $currentCameraId")
                throw RuntimeException("Cannot get available preview/video sizes")
            }

            // Get available sizes
            val availableSizes = map.getOutputSizes(SurfaceTexture::class.java)
            Log.d(TAG, "Available SurfaceTexture sizes: ${availableSizes.joinToString { "${it.width}x${it.height}" }}")

            // Choose preview size (prefer 640x480 or closest)
            val previewSize = chooseOptimalSize(availableSizes)
            Log.d(TAG, "Selected preview size: ${previewSize.width}x${previewSize.height}")

            // Set preview surface texture size
            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
            Log.d(TAG, "SurfaceTexture buffer size set to ${previewSize.width}x${previewSize.height}")
            previewSurface = Surface(surfaceTexture)
            Log.d(TAG, "Preview surface created: ${previewSize.width}x${previewSize.height}, surface=$previewSurface")
            
            // Create ImageReader for capturing frames for MediaPipe
            imageReader = ImageReader.newInstance(IMAGE_READER_WIDTH, IMAGE_READER_HEIGHT, android.graphics.ImageFormat.YUV_420_888, 2)
            imageReader?.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)
            Log.d(TAG, "ImageReader created: ${IMAGE_READER_WIDTH}x${IMAGE_READER_HEIGHT}")

            // Open camera
            Log.d(TAG, "Requesting camera open with cameraId=$currentCameraId, handler=${backgroundHandler != null}")
            cameraManager.openCamera(currentCameraId, cameraStateCallback, backgroundHandler)
            Log.d(TAG, "Camera open request submitted")
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
                    Log.d(TAG, "Capture started (preview + ImageReader)")
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
            
            captureSession?.close()
            captureSession = null
            
            cameraDevice?.close()
            cameraDevice = null
            
            previewSurface?.release()
            previewSurface = null
            
            synchronized(imageLock) {
                latestImage?.close()
                latestImage = null
            }
            imageReader?.close()
            imageReader = null
            
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
        backgroundThread = HandlerThread("SimpleCameraBackground").also { it.start() }
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

    private fun chooseOptimalSize(choices: Array<Size>): Size {
        // Prefer 640x480 or closest match
        val targetSize = Size(640, 480)
        
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
            Log.e(TAG, "Capture session configuration failed")
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
     * Image available listener - stores latest image for MediaPipe processing.
     * Uses acquireNextImage() to avoid maxImages limit issues.
     */
    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        // Close old image first to avoid maxImages limit
        synchronized(imageLock) {
            latestImage?.close()  // Close previous image if any
            latestImage = null
        }
        
        // Try to acquire the latest image
        val image = try {
            reader.acquireNextImage()
        } catch (e: IllegalStateException) {
            // maxImages limit reached - this shouldn't happen now, but handle gracefully
            Log.w(TAG, "Cannot acquire image (maxImages limit): ${e.message}")
            return@OnImageAvailableListener
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring image: ${e.message}", e)
            return@OnImageAvailableListener
        }
        
        if (image == null) {
            return@OnImageAvailableListener
        }
        
        try {
            synchronized(imageLock) {
                latestImage = image
            }
            // Log occasionally to verify images are being captured
            val timestamp = System.currentTimeMillis()
            if (timestamp % 2000 < 100) {
                Log.d(TAG, "Camera image captured: ${image.width}x${image.height}, format=${image.format}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing image: ${e.message}", e)
            image.close()  // Close image if we can't store it
            synchronized(imageLock) {
                latestImage = null
            }
        }
        // Note: Don't close image here - it's stored in latestImage for processing
    }
}

