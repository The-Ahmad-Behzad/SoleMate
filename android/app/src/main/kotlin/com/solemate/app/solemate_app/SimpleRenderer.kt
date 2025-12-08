package com.solemate.app.solemate_app

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLES11Ext
import android.opengl.GLSurfaceView
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.Coordinates2d
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SimpleRenderer(
    private val session: Session? = null,  // ARCore session (nullable for VIO mode)
    private val rotationHelper: DisplayRotationHelper,
    private val activity: ARActivity,
    private var shoeRenderer: ShoeRenderer? = null,
    private val poseProvider: WorldPoseProvider? = null  // VIO pose provider (nullable for ARCore mode)
) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()
    private val planeRenderer = PlaneRenderer()
    private val anchors = mutableListOf<Anchor>()
    private val vioAnchors = mutableListOf<FloatArray>() // Model matrices for VIO anchors
    // === Phase 2.1: Current anchor reference for tracking state management ===
    private var currentAnchor: Anchor? = null
    
    // VIO mode state
    private var cameraReady = false
    private var cameraInitStartTime = 0L
    private val cameraInitTimeoutMs = 5000L
    
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    // ==== Detection infrastructure ====
    private val detectionExecutor = Executors.newSingleThreadExecutor()
    private val detector = FootTracker(activity)
    private val isProcessing = AtomicBoolean(false)
    private var frameCounter = 0
    private var adaptiveFrameSkip = 3 // Will be adjusted based on movement velocity
    private val MIN_FRAME_SKIP = 1  // Every frame for fast movement
    private val MAX_FRAME_SKIP = 4  // Every 4th frame for stationary
    private data class FootPoint(
        val imgWidth: Int,
        val imgHeight: Int,
        val side: String,
        val ankleX: Float,      // normalized 0-1
        val ankleY: Float,
        val toeX: Float?,
        val toeY: Float?,
        val heelX: Float?,
        val heelY: Float?,
        val visibility: Float
    )
    private val pendingFootPoint = AtomicReference<FootPoint?>(null)

    // ==== Filament shoe rendering ====
    // shoeRenderer is now passed via constructor from ARActivity
    private var shoeModelMatrix: FloatArray? = null
    private var shoePlaced = false
    private var lastPlacementTime = 0L
    // === Phase 1.2: Removed placement cooldown for real-time tracking ===
    // private val placementCooldownMs = 500L  // REMOVED - now updates every frame
    private val soleOffsetMeters = 0.02f
    private val baseModelScale = 0.05f  // Base scale for the shoe model (5% of original size)
    private val minFootLength = 0.15f   // Minimum realistic foot length (15cm)
    private val maxFootLength = 0.35f   // Maximum realistic foot length (35cm)
    private val referenceFootLength = 0.26f  // Average adult foot length (26cm)
    private val maxHitDistance = 3.0f   // Maximum distance for valid hit-test (3 meters)
    private val minHitDistance = 0.3f   // Minimum distance for valid hit-test (30cm)
    private val minVisibility = 0.15f    // Minimum landmark visibility threshold (lowered to allow more detections)
    
    // Recalibration request flag
    @Volatile
    private var recalibrationRequested = false
    
    // ==== Phase 2: Pose Smoothing & Stabilization ====
    private var smoothedShoeMatrix: FloatArray? = null
    private var lastSmoothUpdateTime = 0L
    private val SMOOTHING_ALPHA_POSITION = 0.3f  // Position smoothing factor (lower = more smoothing)
    private val SMOOTHING_ALPHA_ROTATION = 0.5f  // Rotation smoothing factor
    private var lastSmoothedQuaternion: FloatArray? = null  // [x, y, z, w] quaternion
    
    // ==== Phase 3: Calibration Integration ====
    private var calibratedShoeSize: Float? = null  // in cm
    private var calibratedPhoneHeight: Float? = null  // in meters
    
    // ==== Phase 4: Anchor Stability Tracking ====
    private var anchorPositionHistory = mutableListOf<FloatArray>()  // Track last 5 anchor positions
    private val ANCHOR_HISTORY_SIZE = 5
    private val ANCHOR_DRIFT_THRESHOLD = 0.10f  // 10cm drift threshold
    private var lastAnchorPosition: FloatArray? = null
    
    // ==== Phase 4.2: Foot Rotation Tracking ====
    private var lastFootDirection: FloatArray? = null
    private val ROTATION_THRESHOLD = 0.1f  // 0.1 radians (~5.7 degrees) threshold for significant rotation
    
    // ==== Phase 5: Occlusion Handling ====
    private var occlusionStartTime = 0L
    private val OCCLUSION_TIMEOUT_MS = 2000L  // 2 seconds before fading out
    private val OCCLUSION_VISIBILITY_THRESHOLD = 0.1f  // Visibility threshold for occlusion
    private var modelOpacity = 1.0f  // For fade in/out during occlusion
    
    // ==== Phase 1: Multi-sample Hit Testing ====
    private val HIT_TEST_SAMPLE_COUNT = 5  // Number of samples for multi-sampling
    private val HIT_TEST_SAMPLE_RADIUS = 10f  // Pixel radius for sampling around target point
    
    // ==== Phase 5: Plane Quality Validation ====
    private val MIN_PLANE_EXTENT = 0.5f  // Minimum plane extent in meters
    private val MIN_PLANE_CONFIDENCE = 0.7f  // Minimum plane confidence (if available)
    
    // ==== Phase 6: Debug & Performance ====
    private var hitTestPerformanceMs = 0L
    private var smoothingPerformanceMs = 0L
    private var lastPerformanceLogTime = 0L
    private val PERFORMANCE_LOG_INTERVAL_MS = 5000L  // Log every 5 seconds
    
    // ==== Phase 6.1: Camera Pose Compensation ====
    private var lastCameraPose: com.google.ar.core.Pose? = null
    private var lastCameraPoseMatrix: FloatArray? = null
    
    // ==== Phase 1.3: Adaptive Throttling ====
    private var movementVelocity: Float = 0f  // meters per frame
    private val FAST_MOVEMENT_THRESHOLD = 0.05f  // 5cm/frame
    private val MEDIUM_MOVEMENT_THRESHOLD = 0.02f  // 2cm/frame
    private val positionHistory = mutableListOf<FloatArray>()  // For velocity calculation
    private val MAX_POSITION_HISTORY = 5
    
    // ==== Phase 3: Frame Rate Control ====
    @Volatile
    private var targetFPS = 30  // Default to 30 FPS
    private var frameSkipForFPS = 2  // 60fps / 30fps = 2 (skip every 2nd frame)
    private val frameTimeHistory = mutableListOf<Long>()
    private var lastFrameTime = 0L

    fun release() {
        try {
            detector.close()
        } catch (_: Exception) { }
        try {
            detectionExecutor.shutdownNow()
        } catch (_: Exception) { }
        try {
            shoeRenderer?.release()
        } catch (_: Exception) { }
    }
    
    /**
     * Request immediate recalibration of shoe placement.
     * Called from UI button. Resets placement state and clears cooldown.
     */
    fun requestRecalibration() {
        recalibrationRequested = true
        Log.d("SimpleRenderer", "🔄 Recalibration requested")
    }
    
    /**
     * === Phase 3.1: Set target FPS (30 or 60) ===
     */
    fun setTargetFPS(fps: Int) {
        targetFPS = fps.coerceIn(30, 60)
        frameSkipForFPS = if (targetFPS == 60) 1 else 2
        Log.d("SimpleRenderer", "Target FPS set to $targetFPS (frameSkip=$frameSkipForFPS)")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("SimpleRenderer", "✅ onSurfaceCreated() called, session=${session != null}, poseProvider=${poseProvider != null}")
        // Use light gray so we can see if renderer is working (will be overridden by camera feed)
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1f)
        backgroundRenderer.createOnGlThread()
        
        if (session != null) {
            // ARCore mode
            Log.d("SimpleRenderer", "Setting up ARCore texture")
            session.setCameraTextureName(backgroundRenderer.getTextureId())
        } else if (poseProvider is SimpleCameraPoseProvider || poseProvider is VioPoseProvider) {
            // SimpleCamera or VIO mode: create external OES texture for Camera2 SurfaceTexture
            val modeName = if (poseProvider is SimpleCameraPoseProvider) "SimpleCamera" else "VIO"
            Log.d("SimpleRenderer", "Setting up $modeName texture")
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            val textureId = textures[0]
            Log.d("SimpleRenderer", "Created texture ID: $textureId")
            
            // Bind and configure texture as external OES (required for SurfaceTexture)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            
            // Set texture ID in pose provider
            poseProvider.setCameraTextureName(textureId)
            Log.d("SimpleRenderer", "Set texture ID in $modeName pose provider")
            
            // Check if SurfaceTexture already exists (from previous session)
            val existingSurfaceTexture = activity.getCameraSurfaceTexture()
            if (existingSurfaceTexture != null) {
                Log.d("SimpleRenderer", "Existing SurfaceTexture found, detaching before creating new one")
                try {
                    existingSurfaceTexture.detachFromGLContext()
                    Log.d("SimpleRenderer", "Existing SurfaceTexture detached successfully")
                } catch (e: Exception) {
                    Log.d("SimpleRenderer", "Existing SurfaceTexture not attached (expected): ${e.message}")
                }
                try {
                    existingSurfaceTexture.release()
                    Log.d("SimpleRenderer", "Existing SurfaceTexture released")
                } catch (e: Exception) {
                    Log.w("SimpleRenderer", "Error releasing existing SurfaceTexture: ${e.message}")
                }
            }
            
            // Create new SurfaceTexture with the texture ID using activity's helper method
            val surfaceTexture = activity.createCameraSurfaceTexture(textureId)
            if (surfaceTexture != null) {
                Log.d("SimpleRenderer", "SurfaceTexture created successfully, textureId=$textureId")
                
                // SurfaceTexture is already attached to the texture ID in createCameraSurfaceTexture()
                // Now we can open the camera
                try {
                    cameraInitStartTime = System.currentTimeMillis()
                    activity.openVioCamera()
                    Log.d("SimpleRenderer", "Camera open requested after SurfaceTexture creation")
                    // Camera will be ready when first frame arrives
                    cameraReady = false  // Will be set to true when first frame is received
                } catch (e: Exception) {
                    Log.e("SimpleRenderer", "Error opening camera: ${e.message}", e)
                    Log.e("SimpleRenderer", "Stack trace:", e)
                    cameraReady = false
                    cameraInitStartTime = 0L
                }
            } else {
                Log.e("SimpleRenderer", "Failed to create SurfaceTexture! Cannot open camera")
                cameraReady = false
            }
        }
        
        planeRenderer.createOnGlThread()
        // === Phase 1.1: Initialize FootTracker with LIVE_STREAM mode ===
        detector.init(useLiveStream = true)
        
        // ==== Phase 3: Load calibration values ====
        try {
            calibratedShoeSize = CalibrationManager.getShoeSize(activity)
            calibratedPhoneHeight = CalibrationManager.getPhoneHeight(activity)
            if (calibratedShoeSize != null) {
                Log.d("SimpleRenderer", "✅ Loaded shoe size calibration: ${calibratedShoeSize}cm")
            }
            if (calibratedPhoneHeight != null) {
                Log.d("SimpleRenderer", "✅ Loaded phone height calibration: ${calibratedPhoneHeight}m")
            }
        } catch (e: Exception) {
            Log.w("SimpleRenderer", "Failed to load calibration: ${e.message}")
        }
        
        // ShoeRenderer will be fully initialized when we know the surface size (onSurfaceChanged)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height
        rotationHelper.onSurfaceChanged(width, height)
        // ShoeRenderer is now managed by ARActivity via TextureView
        // No initialization needed here
    }

    override fun onDrawFrame(gl: GL10?) {
        val frameStartTime = System.currentTimeMillis()
        frameCounter++
        
        // === Phase 3.2: Frame rate throttling ===
        if (targetFPS == 30 && frameCounter % frameSkipForFPS != 0) {
            // Skip detection/updates but still render camera for 30 FPS mode
            // This reduces CPU/GPU load while maintaining visual smoothness
            val frame: Frame? = if (session != null) {
                session.update()
            } else {
                null
            }
            
            if (session != null && frame != null) {
                backgroundRenderer.draw(frame)
            } else if (poseProvider != null) {
                // VIO mode - render camera only
                val textureId = poseProvider.getCameraTextureId()
                if (textureId > 0) {
                    val surfaceTexture = activity.getCameraSurfaceTexture()
                    surfaceTexture?.let {
                        try {
                            it.updateTexImage()
                            val matrix = FloatArray(16)
                            it.getTransformMatrix(matrix)
                            backgroundRenderer.drawCamera2Texture(textureId, matrix)
                        } catch (e: Exception) {
                            // No frame available yet
                        }
                    }
                }
            }
            
            // Still update camera matrices for shoe rendering
            val viewMatrix = if (session != null && frame != null) {
                val vm = FloatArray(16)
                frame.camera.getViewMatrix(vm, 0)
                vm
            } else {
                poseProvider?.getViewMatrix()
            }
            val projMatrix = if (session != null && frame != null) {
                val pm = FloatArray(16)
                frame.camera.getProjectionMatrix(pm, 0, 0.1f, 100f)
                pm
            } else {
                poseProvider?.getProjectionMatrix(0.1f, 100f)
            }
            
            if (viewMatrix != null && projMatrix != null) {
                shoeRenderer?.setCamera(viewMatrix, projMatrix)
                shoeModelMatrix?.let { shoeRenderer?.setModelMatrix(it) }
            }
            
            // Update frame time history
            val frameTime = System.currentTimeMillis() - frameStartTime
            frameTimeHistory.add(frameTime)
            if (frameTimeHistory.size > 60) frameTimeHistory.removeAt(0)
            lastFrameTime = frameStartTime
            
            return
        }
        
        // Clear with a visible color so we can see if renderer is working
        // Use dark gray instead of black so we can distinguish from "not rendering"
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        // Log first few frames to verify renderer is being called
        if (frameCounter <= 5) {
            Log.d("SimpleRenderer", "onDrawFrame() called, frame=$frameCounter, poseProvider=${poseProvider != null}, session=${session != null}")
        }

        // Update pose provider (for VIO mode) or session (for ARCore mode)
        if (poseProvider != null) {
            if (!poseProvider.update()) {
                if (frameCounter < 5) {
                    Log.d("SimpleRenderer", "Pose provider update failed")
                }
                return // Skip rendering if update failed
            }
        } else if (session != null) {
            rotationHelper.updateSessionIfNeeded(session)
        } else {
            if (frameCounter < 10) {
                Log.e("SimpleRenderer", "Neither session nor poseProvider available!")
            }
            return
        }

        // Matrices for shoe rendering (used in both ARCore and VIO modes)
        var projMatrix: FloatArray? = null
        var viewMatrix: FloatArray? = null
        
        try {
            // Draw background - different paths for ARCore vs VIO
            val frame: Frame? = if (session != null) {
                session.update()
            } else {
                null
            }
            
            if (session != null && frame != null) {
                // ARCore path: use external texture
                backgroundRenderer.draw(frame)
                
                // Matrices for drawing (ARCore)
                projMatrix = FloatArray(16)
                viewMatrix = FloatArray(16)
                frame.camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)
                frame.camera.getViewMatrix(viewMatrix, 0)
                val viewProjMatrix = FloatArray(16)
                android.opengl.Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

                // Handle queued taps (existing anchor creation logic)
                val tap = activity.getQueuedSingleTap()
                if (tap != null && frame.camera.trackingState == TrackingState.TRACKING) {
                    for (hit in frame.hitTest(tap)) {
                        val trackable = hit.trackable
                        if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                            anchors.add(hit.createAnchor())
                            Log.d("ARAnchor", "✅ Anchor created at ${hit.hitPose.translation.contentToString()}")
                            break
                        }
                    }
                }

                // Draw all tracked planes
                val allPlanes = session.getAllTrackables(Plane::class.java)
                for (plane in allPlanes) {
                    if (plane.trackingState == TrackingState.TRACKING && plane.subsumedBy == null) {
                        planeRenderer.drawPlane(plane, viewProjMatrix)
                    }
                }

                // Draw anchor markers
                for (anchor in anchors) {
                    if (anchor.trackingState == TrackingState.TRACKING) {
                        val pose = anchor.pose
                        val modelMatrix = FloatArray(16)
                        pose.toMatrix(modelMatrix, 0)
                        planeRenderer.drawAnchorMarker(modelMatrix, viewProjMatrix)
                    }
                }
            } else if (poseProvider is SimpleCameraPoseProvider || poseProvider is VioPoseProvider) {
                // SimpleCamera or VIO path: use Camera2 texture
                // Check for camera initialization timeout
                if (cameraInitStartTime > 0 && !cameraReady) {
                    val elapsed = System.currentTimeMillis() - cameraInitStartTime
                    if (elapsed > cameraInitTimeoutMs) {
                        Log.e("SimpleRenderer", "Camera initialization timeout after ${elapsed}ms")
                        Log.e("SimpleRenderer", "Camera may not be responding. Check camera permissions and device compatibility.")
                        cameraInitStartTime = 0L  // Stop checking
                    } else if (elapsed % 1000 == 0L) {  // Log every second
                        Log.d("SimpleRenderer", "Waiting for camera... (${elapsed}ms elapsed)")
                    }
                }
                
                // Try to update and draw camera even if not ready yet (to detect first frame)
                val textureId = poseProvider.getCameraTextureId()
                if (textureId > 0) {
                    // Get SurfaceTexture and update texture on GL thread (required for SurfaceTexture)
                    val surfaceTexture = activity.getCameraSurfaceTexture()
                    if (surfaceTexture != null) {
                        try {
                            // Update texture with latest camera frame (must be on GL thread)
                            // This will throw if no frame is available yet, which is fine
                            surfaceTexture.updateTexImage()
                            
                            // Mark camera as ready on first successful frame update
                            if (!cameraReady && cameraInitStartTime > 0) {
                                val elapsed = System.currentTimeMillis() - cameraInitStartTime
                                Log.d("SimpleRenderer", "✅ Camera ready! First frame received after ${elapsed}ms")
                                cameraReady = true
                                cameraInitStartTime = 0L
                            }
                            
                            // Get transform matrix from SurfaceTexture
                            val matrix = FloatArray(16)
                            surfaceTexture.getTransformMatrix(matrix)
                            
                            // Get device rotation and sensor orientation
                            val deviceRotation = rotationHelper.getRotationDegrees()
                            val sensorOrientation = try {
                                activity.getSimpleCameraManager()?.getSensorOrientation() ?: 0
                            } catch (e: Exception) { 0 }
                            
                            // Log rotation info for debugging (only first few frames)
                            if (frameCounter <= 5) {
                                Log.d("SimpleRenderer", "Rotation debug: deviceRotation=$deviceRotation°, sensorOrientation=$sensorOrientation°")
                                Log.d("SimpleRenderer", "SurfaceTexture matrix: [${matrix[0]}, ${matrix[1]}, ${matrix[4]}, ${matrix[5]}]")
                            }
                            
                            // Try different rotation corrections based on device orientation
                            // If camera appears horizontal in portrait, we need to rotate it
                            // Convert device rotation to degrees
                            val deviceRotationDegrees = when (deviceRotation) {
                                1 -> 90
                                2 -> 180
                                3 -> 270
                                else -> 0
                            }
                            
                            // Calculate rotation dynamically
                            // We assume SurfaceTexture might already handle sensor orientation, 
                            // so we only compensate for device rotation.
                            val rotation = (360 - deviceRotationDegrees) % 360
                            var correctedMatrix = applyTextureRotation(matrix, rotation)
                            
                            // Apply horizontal flip to correct mirroring
                            correctedMatrix = applyHorizontalFlip(correctedMatrix)
                            
                            if (frameCounter % 60 == 0) {
                                Log.d("SimpleRenderer", "Rotation: device=$deviceRotationDegrees -> final=$rotation (with horizontal flip)")
                            }
                            
                            // Debug: Log the matrix values to understand what's happening
                            if (frameCounter <= 3) {
                                Log.d("SimpleRenderer", "Original matrix: [${matrix[0]}, ${matrix[1]}, ${matrix[4]}, ${matrix[5]}]")
                                Log.d("SimpleRenderer", "Corrected matrix: [${correctedMatrix[0]}, ${correctedMatrix[1]}, ${correctedMatrix[4]}, ${correctedMatrix[5]}]")
                            }
                            
                            // Draw camera feed
                            backgroundRenderer.drawCamera2Texture(textureId, correctedMatrix)
                            
                            // === SimpleCamera Foot Detection ===
                            // Use camera image from pose provider instead of glReadPixels
                            val currentFrameSkip = calculateAdaptiveFrameSkip()
                            if (frameCounter % currentFrameSkip == 0 && isProcessing.compareAndSet(false, true)) {
                                if (frameCounter % 60 == 0) {
                                    Log.d("SimpleRenderer", "SimpleCamera: Attempting to acquire camera image, frame=$frameCounter")
                                }
                                
                                val cameraImage = try {
                                    poseProvider.acquireCameraImage()
                                } catch (e: Exception) {
                                    if (frameCounter % 60 == 0) {
                                        Log.e("SimpleRenderer", "SimpleCamera: Failed to acquire camera image: ${e.message}")
                                    }
                                    null
                                }
                                
                                if (cameraImage != null) {
                                    if (frameCounter % 60 == 0) {
                                        Log.d("SimpleRenderer", "SimpleCamera: Camera image acquired: ${cameraImage.width}x${cameraImage.height}, type=${cameraImage.javaClass.simpleName}")
                                    }
                                    
                                    // Submit to background thread for detection; MUST close image
                                    detectionExecutor.submit {
                                        try {
                                            // Get device rotation for bitmap rotation
                                            val deviceRotation = rotationHelper.getRotationDegrees()
                                            val sensorOrientation = try {
                                                activity.getSimpleCameraManager()?.getSensorOrientation() ?: 90
                                            } catch (e: Exception) { 90 }
                                            
                                            // Convert CameraImage to Bitmap
                                            val bitmap = when (cameraImage) {
                                                is Camera2Image -> {
                                                    try {
                                                        val converted = YuvConverter.imageToBitmap(cameraImage.getCamera2Image())
                                                        if (frameCounter % 60 == 0) {
                                                            Log.d("SimpleRenderer", "SimpleCamera: Bitmap converted: ${converted.width}x${converted.height}")
                                                        }
                                                        
                                                        // Rotate bitmap to portrait orientation for MediaPipe
                                                        // Camera sensor is 90° (landscape), device is 0° (portrait)
                                                        // Need to rotate 90° clockwise to make it portrait
                                                        val rotationDegrees = when {
                                                            deviceRotation == 0 && sensorOrientation == 90 -> 90  // Portrait device, landscape sensor -> rotate 90° CW
                                                            deviceRotation == 90 -> 0  // Landscape left -> no rotation needed
                                                            deviceRotation == 180 -> 270  // Upside down -> rotate 270° CW
                                                            deviceRotation == 270 -> 180  // Landscape right -> rotate 180°
                                                            else -> 90  // Default: rotate 90° CW
                                                        }
                                                        
                                                        val rotatedBitmap = if (rotationDegrees != 0) {
                                                            val matrix = android.graphics.Matrix()
                                                            matrix.postRotate(rotationDegrees.toFloat())
                                                            android.graphics.Bitmap.createBitmap(
                                                                converted, 0, 0, converted.width, converted.height, matrix, true
                                                            ).also {
                                                                if (converted != it) converted.recycle()  // Recycle original if different
                                                                // Log rotation more frequently for debugging
                                                                if (frameCounter % 10 == 0) {
                                                                    Log.d("SimpleRenderer", "SimpleCamera: Bitmap rotated ${rotationDegrees}°: ${converted.width}x${converted.height} -> ${it.width}x${it.height}")
                                                                }
                                                            }
                                                        } else {
                                                            if (frameCounter % 10 == 0) {
                                                                Log.d("SimpleRenderer", "SimpleCamera: Bitmap not rotated (rotationDegrees=0): ${converted.width}x${converted.height}")
                                                            }
                                                            converted
                                                        }
                                                        
                                                        rotatedBitmap
                                                    } catch (e: Exception) {
                                                        Log.e("SimpleRenderer", "SimpleCamera: Bitmap conversion failed: ${e.message}", e)
                                                        null
                                                    }
                                                }
                                                else -> {
                                                    // Only Camera2Image is supported for SimpleCamera mode
                                                    Log.w("SimpleRenderer", "Unsupported camera image type: ${cameraImage.javaClass.simpleName}")
                                                    null
                                                }
                                            }
                                            
                                            if (bitmap != null) {
                                                if (frameCounter % 60 == 0) {
                                                    Log.d("SimpleRenderer", "SimpleCamera: Processing frame ${bitmap.width}x${bitmap.height} for detection")
                                                }
                                                
                                                val footResult = detector.detectFoot(bitmap)
                                                if (footResult.detected) {
                                                    // Log detection details more frequently
                                                    Log.d("SimpleRenderer", "✅ Foot detected: side=${footResult.side}, ankle=(${"%.3f".format(footResult.ankleX)},${"%.3f".format(footResult.ankleY)}), toe=(${footResult.toeX?.let{"%.3f".format(it)}},${footResult.toeY?.let{"%.3f".format(it)}}), vis=${"%.3f".format(footResult.visibility)}, imgSize=${footResult.imgWidth}x${footResult.imgHeight}")
                                                    
                                                    pendingFootPoint.set(
                                                        FootPoint(
                                                            imgWidth = footResult.imgWidth,
                                                            imgHeight = footResult.imgHeight,
                                                            side = footResult.side,
                                                            ankleX = footResult.ankleX,
                                                            ankleY = footResult.ankleY,
                                                            toeX = footResult.toeX,
                                                            toeY = footResult.toeY,
                                                            heelX = footResult.heelX,
                                                            heelY = footResult.heelY,
                                                            visibility = footResult.visibility
                                                        )
                                                    )
                                                } else {
                                                    if (frameCounter % 60 == 0) {
                                                        Log.d("SimpleRenderer", "SimpleCamera: No foot detected in frame ${bitmap.width}x${bitmap.height}")
                                                    }
                                                }
                                            } else {
                                                if (frameCounter % 60 == 0) {
                                                    Log.w("SimpleRenderer", "SimpleCamera: Bitmap is null, cannot process")
                                                }
                                            }
                                        } catch (t: Throwable) {
                                            Log.e("SimpleRenderer", "SimpleCamera detection error: ${t.message}", t)
                                        } finally {
                                            try {
                                                cameraImage.close()
                                            } catch (e: Exception) {
                                                // safe-guard
                                            }
                                            isProcessing.set(false)
                                        }
                                    }
                                } else {
                                    // Camera image not available for this frame - clear processing flag
                                    if (frameCounter % 60 == 0) {
                                        Log.w("SimpleRenderer", "SimpleCamera: Camera image is null, skipping detection")
                                    }
                                    isProcessing.set(false)
                                }
                            }
                            
                            // Log success occasionally
                            if (frameCounter % 60 == 0) {
                                Log.v("SimpleRenderer", "Camera frame drawn successfully, frame=$frameCounter")
                            }
                        } catch (e: Exception) {
                            // updateTexImage() throws if no frame is available yet - this is expected
                            if (cameraInitStartTime > 0) {
                                val elapsed = System.currentTimeMillis() - cameraInitStartTime
                                // Log more frequently at the start
                                if (elapsed < 3000 || elapsed % 500 == 0L) {
                                    Log.d("SimpleRenderer", "Waiting for camera frame... (${elapsed}ms elapsed, exception: ${e.javaClass.simpleName})")
                                }
                            } else if (frameCounter < 10) {
                                Log.d("SimpleRenderer", "updateTexImage() exception (expected if no frame yet): ${e.javaClass.simpleName}: ${e.message}")
                            }
                            // Don't draw this frame, but continue to next frame
                            return
                        }
                    } else {
                        if (frameCounter < 10 || frameCounter % 60 == 0) {
                            Log.w("SimpleRenderer", "SurfaceTexture not available, skipping draw (frame=$frameCounter)")
                        }
                        return
                    }
                } else {
                    if (frameCounter < 10 || frameCounter % 60 == 0) {
                        Log.w("SimpleRenderer", "Texture ID not set yet (textureId=$textureId), skipping draw (frame=$frameCounter)")
                    }
                    return
                }
                
                // VIO mode: get matrices from pose provider
                projMatrix = poseProvider.getProjectionMatrix(0.1f, 100f)
                viewMatrix = poseProvider.getViewMatrix()
                val viewProjMatrix = FloatArray(16)
                android.opengl.Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)
                
                // Draw VIO planes (if any)
                val planes = poseProvider.getPlanes()
                if (planes.isNotEmpty() && frameCounter % 60 == 0) {
                    Log.d("SimpleRenderer", "Drawing ${planes.size} VIO plane(s)")
                }
                for (planeInfo in planes) {
                    planeRenderer.drawPlane(planeInfo, viewProjMatrix)
                }
                
                // Draw VIO anchors
                if (vioAnchors.isNotEmpty() && frameCounter % 60 == 0) {
                    Log.d("SimpleRenderer", "Drawing ${vioAnchors.size} VIO anchor(s)")
                }
                for (anchorMatrix in vioAnchors) {
                    planeRenderer.drawAnchorMarker(anchorMatrix, viewProjMatrix)
                }
                
                // Continue to foot placement processing (don't return early)
            }

            // === Acquire camera image for ML (adaptive throttling) ===
            // Only for ARCore mode (VIO mode handled separately above)
            if (session != null && frame != null) {
                frameCounter++
                // === Phase 1.3: Adaptive frame skipping based on movement velocity ===
                val currentFrameSkip = calculateAdaptiveFrameSkip()
                if (frameCounter % currentFrameSkip == 0 && isProcessing.compareAndSet(false, true)) {
                    val cameraImage = try {
                        frame.acquireCameraImage() // MAY throw NotYetAvailableException
                    } catch (nye: NotYetAvailableException) {
                        null
                    }
                
                    if (cameraImage != null) {
                        // === Phase 1.1: Use LIVE_STREAM async mode if available ===
                        detectionExecutor.submit {
                            try {
                                // Convert CameraImage to Bitmap
                                val bitmap = when (cameraImage) {
                                    is ArcoreCameraImage -> {
                                        YuvConverter.imageToBitmap(cameraImage.getArImage())
                                    }
                                    is Camera2Image -> {
                                        YuvConverter.imageToBitmap(cameraImage.getCamera2Image())
                                    }
                                    else -> {
                                        // Try direct conversion for ARCore Image
                                        try {
                                            YuvConverter.imageToBitmap(cameraImage)
                                        } catch (e: Exception) {
                                            Log.e("SimpleRenderer", "Failed to convert camera image: ${e.message}")
                                            null
                                        }
                                    }
                                }
                                
                                if (bitmap != null) {
                                    // Try LIVE_STREAM async mode first
                                    try {
                                        val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(bitmap).build()
                                        detector.detectFootAsync(mpImage)
                                        // Results will be available via getLatestResult() in onDrawFrame
                                    } catch (e: Exception) {
                                        // Fallback to synchronous detection
                                        Log.d("SimpleRenderer", "LIVE_STREAM not available, using sync detection: ${e.message}")
                                        val footResult = detector.detectFoot(bitmap)

                                        if (footResult.detected) {
                                            pendingFootPoint.set(
                                                FootPoint(
                                                    imgWidth = footResult.imgWidth,
                                                    imgHeight = footResult.imgHeight,
                                                    side = footResult.side,
                                                    ankleX = footResult.ankleX,
                                                    ankleY = footResult.ankleY,
                                                    toeX = footResult.toeX,
                                                    toeY = footResult.toeY,
                                                    heelX = footResult.heelX,
                                                    heelY = footResult.heelY,
                                                    visibility = footResult.visibility
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (t: Throwable) {
                                Log.e("SimpleRenderer", "Detection error: ${t.message}")
                            } finally {
                                try {
                                    cameraImage.close()
                                } catch (e: Exception) {
                                    // safe-guard
                                }
                                isProcessing.set(false)
                            }
                        }
                    } else {
                        // Camera image not available for this frame - clear processing flag
                        isProcessing.set(false)
                    }
                }
            }

            // === Handle recalibration request ===
            if (recalibrationRequested) {
                recalibrationRequested = false
                shoePlaced = false
                lastPlacementTime = 0L
                // Clear old anchors
                anchors.forEach { it.detach() }
                anchors.clear()
                vioAnchors.clear()
                // ==== Phase 2: Reset smoothing state ====
                smoothedShoeMatrix = null
                lastSmoothedQuaternion = null
                lastSmoothUpdateTime = 0L
                // ==== Phase 2 & 4: Reset anchor tracking ====
                currentAnchor = null
                lastAnchorPosition = null
                anchorPositionHistory.clear()
                positionHistory.clear()
                movementVelocity = 0f
                // Reload calibration values
                try {
                    calibratedShoeSize = CalibrationManager.getShoeSize(activity)
                    calibratedPhoneHeight = CalibrationManager.getPhoneHeight(activity)
                    Log.d("SimpleRenderer", "✅ Recalibration: Reset placement state and reloaded calibration")
                } catch (e: Exception) {
                    Log.w("SimpleRenderer", "Failed to reload calibration: ${e.message}")
                }
            }
            
            // === Phase 1.1: Check for LIVE_STREAM results first ===
            val liveStreamResult = detector.getLatestResult()
            if (liveStreamResult != null && liveStreamResult.detected) {
                // Convert LIVE_STREAM result to FootPoint format
                val footPoint = FootPoint(
                    imgWidth = liveStreamResult.imgWidth,
                    imgHeight = liveStreamResult.imgHeight,
                    side = liveStreamResult.side,
                    ankleX = liveStreamResult.ankleX,
                    ankleY = liveStreamResult.ankleY,
                    toeX = liveStreamResult.toeX,
                    toeY = liveStreamResult.toeY,
                    heelX = liveStreamResult.heelX,
                    heelY = liveStreamResult.heelY,
                    visibility = liveStreamResult.visibility
                )
                // Process immediately without cooldown
                if ((session != null && frame != null) || poseProvider != null) {
                    try {
                        val isTracking = if (session != null && frame != null) {
                            frame.camera.trackingState == TrackingState.TRACKING
                        } else {
                            poseProvider?.isTracking() == true
                        }
                        
                        if (isTracking) {
                            // === Phase 5.1: Handle occlusion ===
                            handleOcclusion(footPoint.visibility)
                            
                            if (footPoint.visibility >= minVisibility) {
                                processFootPlacement(footPoint, frame)
                            } else {
                                // === Phase 5.2: Use predicted position during occlusion ===
                                val predictedPos = predictPositionDuringOcclusion()
                                if (predictedPos != null && modelOpacity > 0f) {
                                    Log.v("FootAnchor", "Using predicted position during occlusion")
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        Log.e("SimpleRenderer", "LIVE_STREAM foot placement failed: ${t.message}", t)
                    }
                }
            }
            
            // === If we have a pending foot point (normalized coords), transform to VIEW coords and hitTest ===
            val pendingFoot = pendingFootPoint.getAndSet(null)
            if (pendingFoot != null) {
                Log.d("FootAnchor", "📥 Processing pending foot detection: side=${pendingFoot.side}, vis=${pendingFoot.visibility}, mode=${if (session != null) "ARCore" else "VIO"}")
                // Check if we can process (ARCore or VIO)
                if ((session != null && frame != null) || poseProvider != null) {
                    try {
                        // Check tracking state
                        val isTracking = if (session != null && frame != null) {
                            frame.camera.trackingState == TrackingState.TRACKING
                        } else {
                            poseProvider?.isTracking() == true
                        }
                        
                        if (isTracking) {
                            // === Phase 1.2: Removed cooldown - now updates every frame for real-time tracking ===
                            // === Phase 5.1: Handle occlusion ===
                            handleOcclusion(pendingFoot.visibility)
                            
                            // Validate visibility threshold
                            if (pendingFoot.visibility >= minVisibility) {
                                Log.d("FootAnchor", "✅ Processing foot placement: vis=${pendingFoot.visibility} >= $minVisibility")
                                processFootPlacement(pendingFoot, frame)
                            } else {
                                // === Phase 5.2: Use predicted position during occlusion ===
                                val predictedPos = predictPositionDuringOcclusion()
                                if (predictedPos != null && modelOpacity > 0f) {
                                    Log.d("FootAnchor", "Using predicted position during occlusion")
                                    // Could update model with predicted position here
                                } else {
                                    Log.d("FootAnchor", "⚠️ Low visibility ${pendingFoot.visibility} < $minVisibility, skipping")
                                }
                            }
                        } else {
                            Log.d("FootAnchor", "⚠️ Not tracking, skipping foot placement")
                        }
                    } catch (t: Throwable) {
                        Log.e("SimpleRenderer", "Foot placement failed: ${t.message}", t)
                    }
                } else {
                    Log.w("FootAnchor", "⚠️ No session or poseProvider available for foot placement")
                }
            }

            // ==== Phase 2.2 & 6.2: Update shoe camera/model matrices every frame ====
            // Only update if we have valid matrices (ARCore or VIO mode)
            if (viewMatrix != null && projMatrix != null) {
                try {
                    shoeRenderer?.let { sr ->
                        // === Phase 6.2: Update camera matrices every frame ===
                        sr.setCamera(viewMatrix!!, projMatrix!!)
                        
                        // === Phase 2.2: Update model matrix every frame if anchor is tracking ===
                        if (currentAnchor != null && currentAnchor!!.trackingState == TrackingState.TRACKING) {
                            // Update transform relative to anchor every frame
                            shoeModelMatrix?.let { mm ->
                                // Apply anchor pose offset if needed
                                val anchorPose = currentAnchor!!.pose
                                val updatedMatrix = FloatArray(16)
                                System.arraycopy(mm, 0, updatedMatrix, 0, 16)
                                // Update position relative to anchor
                                val anchorTrans = anchorPose.translation
                                updatedMatrix[12] = anchorTrans[0] + (mm[12] - anchorTrans[0])
                                updatedMatrix[13] = anchorTrans[1] + (mm[13] - anchorTrans[1])
                                updatedMatrix[14] = anchorTrans[2] + (mm[14] - anchorTrans[2])
                                sr.setModelMatrix(updatedMatrix)
                            } ?: shoeModelMatrix?.let { mm ->
                                sr.setModelMatrix(mm)
                            }
                        } else {
                            // No anchor or anchor not tracking - use existing matrix
                            shoeModelMatrix?.let { mm -> 
                                sr.setModelMatrix(mm)
                            }
                        }
                        
                        if (frameCounter % 60 == 0 && shoeModelMatrix != null) {
                            val mm = shoeModelMatrix!!
                            Log.d("ShoeUpdate", "Updating matrix: pos=[${mm[12]}, ${mm[13]}, ${mm[14]}], anchorState=${currentAnchor?.trackingState}")
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("ShoeUpdate", "Failed to update matrices: ${t.message}")
                }
            }
            
            // ==== Phase 3.3 & 6: Performance monitoring ====
            val frameTime = System.currentTimeMillis() - frameStartTime
            frameTimeHistory.add(frameTime)
            if (frameTimeHistory.size > 60) frameTimeHistory.removeAt(0)
            lastFrameTime = frameStartTime
            
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPerformanceLogTime > PERFORMANCE_LOG_INTERVAL_MS) {
                val avgHitTest = if (hitTestPerformanceMs > 0) hitTestPerformanceMs else 0L
                val avgSmoothing = if (smoothingPerformanceMs > 0) smoothingPerformanceMs else 0L
                val avgFrameTime = if (frameTimeHistory.isNotEmpty()) {
                    frameTimeHistory.average().toLong()
                } else 0L
                val currentFPS = if (avgFrameTime > 0) (1000 / avgFrameTime) else 0
                val targetFrameTime = if (targetFPS == 60) 16L else 33L
                
                Log.d("Performance", "FPS: $currentFPS (target: $targetFPS), FrameTime: ${avgFrameTime}ms (target: ${targetFrameTime}ms), HitTest: ${avgHitTest}ms, Smoothing: ${avgSmoothing}ms")
                
                // === Phase 3.3: Adaptive quality reduction if performance degrades ===
                if (avgFrameTime > targetFrameTime * 1.5f && frameCounter % 300 == 0) {
                    Log.w("Performance", "⚠️ Performance degradation detected, consider reducing quality")
                    // Could implement adaptive quality reduction here
                }
                
                lastPerformanceLogTime = currentTime
            }

        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
            // Consider notifying user
        }
    }
    
    /**
     * Apply rotation to texture coordinates.
     * @param transformMatrix The SurfaceTexture transform matrix
     * @param degrees Rotation in degrees (90, 180, or 270)
     */
    private fun applyTextureRotation(transformMatrix: FloatArray, degrees: Int): FloatArray {
        val rotationMatrix = FloatArray(16)
        android.opengl.Matrix.setIdentityM(rotationMatrix, 0)
        
        when (degrees) {
            90 -> {
                // 90° clockwise: (x, y) -> (1-y, x)
                rotationMatrix[0] = 0f
                rotationMatrix[1] = 1f
                rotationMatrix[4] = -1f
                rotationMatrix[5] = 0f
                rotationMatrix[12] = 1f
                rotationMatrix[13] = 0f
            }
            180 -> {
                // 180°: (x, y) -> (1-x, 1-y)
                rotationMatrix[0] = -1f
                rotationMatrix[5] = -1f
                rotationMatrix[12] = 1f
                rotationMatrix[13] = 1f
            }
            270 -> {
                // 270° clockwise (90° counter-clockwise): (x, y) -> (y, 1-x)
                rotationMatrix[0] = 0f
                rotationMatrix[1] = -1f
                rotationMatrix[4] = 1f
                rotationMatrix[5] = 0f
                rotationMatrix[12] = 0f
                rotationMatrix[13] = 1f
            }
            else -> {
                // No rotation
                return transformMatrix
            }
        }
        
        // Multiply: result = rotationMatrix * transformMatrix
        val result = FloatArray(16)
        android.opengl.Matrix.multiplyMM(result, 0, rotationMatrix, 0, transformMatrix, 0)
        
        return result
    }
    
    /**
     * Apply horizontal flip to texture coordinates.
     * @param transformMatrix The texture transform matrix
     */
    private fun applyHorizontalFlip(transformMatrix: FloatArray): FloatArray {
        val flipMatrix = FloatArray(16)
        android.opengl.Matrix.setIdentityM(flipMatrix, 0)
        
        // Horizontal flip: (x, y) -> (1-x, y)
        flipMatrix[0] = -1f
        flipMatrix[12] = 1f
        
        // Multiply: result = flipMatrix * transformMatrix
        val result = FloatArray(16)
        android.opengl.Matrix.multiplyMM(result, 0, flipMatrix, 0, transformMatrix, 0)
        
        return result
    }
    // Unified Hit Result to handle both ARCore and VIO
    private data class UnifiedHit(
        val hitPoseMatrix: FloatArray,
        val distance: Float,
        val trackableType: String,
        val arCoreHit: com.google.ar.core.HitResult? = null
    )
    
    // ==== Phase 2: Helper functions for pose smoothing ====
    /**
     * Extract quaternion from 4x4 matrix.
     */
    private fun matrixToQuaternion(matrix: FloatArray): FloatArray {
        val q = FloatArray(4)
        val trace = matrix[0] + matrix[5] + matrix[10]
        
        if (trace > 0f) {
            val s = kotlin.math.sqrt(trace + 1.0f) * 2f
            q[3] = 0.25f * s
            q[0] = (matrix[9] - matrix[6]) / s
            q[1] = (matrix[2] - matrix[8]) / s
            q[2] = (matrix[4] - matrix[1]) / s
        } else if (matrix[0] > matrix[5] && matrix[0] > matrix[10]) {
            val s = kotlin.math.sqrt(1.0f + matrix[0] - matrix[5] - matrix[10]) * 2f
            q[3] = (matrix[9] - matrix[6]) / s
            q[0] = 0.25f * s
            q[1] = (matrix[1] + matrix[4]) / s
            q[2] = (matrix[2] + matrix[8]) / s
        } else if (matrix[5] > matrix[10]) {
            val s = kotlin.math.sqrt(1.0f + matrix[5] - matrix[0] - matrix[10]) * 2f
            q[3] = (matrix[2] - matrix[8]) / s
            q[0] = (matrix[1] + matrix[4]) / s
            q[1] = 0.25f * s
            q[2] = (matrix[6] + matrix[9]) / s
        } else {
            val s = kotlin.math.sqrt(1.0f + matrix[10] - matrix[0] - matrix[5]) * 2f
            q[3] = (matrix[4] - matrix[1]) / s
            q[0] = (matrix[2] + matrix[8]) / s
            q[1] = (matrix[6] + matrix[9]) / s
            q[2] = 0.25f * s
        }
        
        // Normalize
        val len = kotlin.math.sqrt(q[0]*q[0] + q[1]*q[1] + q[2]*q[2] + q[3]*q[3])
        if (len > 0.0001f) {
            q[0] /= len
            q[1] /= len
            q[2] /= len
            q[3] /= len
        }
        
        return q
    }
    
    /**
     * Spherical Linear Interpolation (SLERP) between two quaternions.
     */
    private fun slerpQuaternion(q1: FloatArray, q2: FloatArray, t: Float): FloatArray {
        var dot = q1[0]*q2[0] + q1[1]*q2[1] + q1[2]*q2[2] + q1[3]*q2[3]
        
        // If dot < 0, negate one quaternion to take shorter path
        if (dot < 0f) {
            dot = -dot
            val q2Neg = FloatArray(4) { -q2[it] }
            return slerpQuaternion(q1, q2Neg, t)
        }
        
        // If quaternions are very close, use linear interpolation
        if (dot > 0.9995f) {
            val result = FloatArray(4)
            for (i in 0..3) {
                result[i] = q1[i] + t * (q2[i] - q1[i])
            }
            val len = kotlin.math.sqrt(result[0]*result[0] + result[1]*result[1] + result[2]*result[2] + result[3]*result[3])
            if (len > 0.0001f) {
                for (i in 0..3) result[i] /= len
            }
            return result
        }
        
        val theta = kotlin.math.acos(dot.coerceIn(-1f, 1f))
        val sinTheta = kotlin.math.sin(theta)
        val w1 = kotlin.math.sin((1f - t) * theta) / sinTheta
        val w2 = kotlin.math.sin(t * theta) / sinTheta
        
        val result = FloatArray(4)
        for (i in 0..3) {
            result[i] = w1 * q1[i] + w2 * q2[i]
        }
        
        // Normalize
        val len = kotlin.math.sqrt(result[0]*result[0] + result[1]*result[1] + result[2]*result[2] + result[3]*result[3])
        if (len > 0.0001f) {
            for (i in 0..3) result[i] /= len
        }
        
        return result
    }
    
    /**
     * Convert quaternion to rotation matrix (3x3, then insert into 4x4).
     */
    private fun quaternionToMatrix(q: FloatArray, matrix: FloatArray, offset: Int) {
        val x = q[0]
        val y = q[1]
        val z = q[2]
        val w = q[3]
        
        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z
        
        matrix[offset + 0] = 1f - 2f * (yy + zz)
        matrix[offset + 1] = 2f * (xy - wz)
        matrix[offset + 2] = 2f * (xz + wy)
        matrix[offset + 3] = 0f
        
        matrix[offset + 4] = 2f * (xy + wz)
        matrix[offset + 5] = 1f - 2f * (xx + zz)
        matrix[offset + 6] = 2f * (yz - wx)
        matrix[offset + 7] = 0f
        
        matrix[offset + 8] = 2f * (xz - wy)
        matrix[offset + 9] = 2f * (yz + wx)
        matrix[offset + 10] = 1f - 2f * (xx + yy)
        matrix[offset + 11] = 0f
        
        matrix[offset + 12] = 0f
        matrix[offset + 13] = 0f
        matrix[offset + 14] = 0f
        matrix[offset + 15] = 1f
    }
    
    /**
     * === Phase 4.3: Get adaptive smoothing alpha based on movement velocity ===
     */
    private fun getAdaptiveSmoothingAlpha(velocity: Float): Float {
        return when {
            velocity > FAST_MOVEMENT_THRESHOLD -> 0.7f  // Less smoothing, more responsive for fast movement
            velocity > MEDIUM_MOVEMENT_THRESHOLD -> 0.5f
            else -> 0.3f  // More smoothing, less jitter for slow movement
        }
    }
    
    /**
     * Smooth pose using exponential smoothing for position and SLERP for rotation.
     * === Phase 4.3: Now supports adaptive smoothing alpha ===
     */
    private fun smoothPose(currentMatrix: FloatArray, alpha: Float = SMOOTHING_ALPHA_POSITION): FloatArray {
        val smoothStartTime = System.currentTimeMillis()
        
        if (smoothedShoeMatrix == null) {
            smoothedShoeMatrix = FloatArray(16)
            System.arraycopy(currentMatrix, 0, smoothedShoeMatrix!!, 0, 16)
            lastSmoothedQuaternion = matrixToQuaternion(currentMatrix)
            lastSmoothUpdateTime = System.currentTimeMillis()
            smoothingPerformanceMs = System.currentTimeMillis() - smoothStartTime
            return smoothedShoeMatrix!!
        }
        
        val smoothed = FloatArray(16)
        
        // === Phase 4.3: Smooth position using adaptive alpha ===
        smoothed[12] = alpha * currentMatrix[12] + (1f - alpha) * smoothedShoeMatrix!![12]
        smoothed[13] = alpha * currentMatrix[13] + (1f - alpha) * smoothedShoeMatrix!![13]
        smoothed[14] = alpha * currentMatrix[14] + (1f - alpha) * smoothedShoeMatrix!![14]
        
        // Smooth rotation using quaternion SLERP
        val currentQuat = matrixToQuaternion(currentMatrix)
        val smoothedQuat = if (lastSmoothedQuaternion != null) {
            slerpQuaternion(lastSmoothedQuaternion!!, currentQuat, SMOOTHING_ALPHA_ROTATION)
        } else {
            currentQuat
        }
        lastSmoothedQuaternion = smoothedQuat
        
        // Convert smoothed quaternion back to rotation matrix
        quaternionToMatrix(smoothedQuat, smoothed, 0)
        
        // Copy translation
        smoothed[12] = smoothed[12]  // Already set above
        smoothed[13] = smoothed[13]
        smoothed[14] = smoothed[14]
        smoothed[15] = 1f
        
        smoothedShoeMatrix = smoothed
        lastSmoothUpdateTime = System.currentTimeMillis()
        smoothingPerformanceMs = System.currentTimeMillis() - smoothStartTime
        
        return smoothed
    }
    
    /**
     * === Phase 1.3: Calculate adaptive frame skip based on movement velocity ===
     */
    private fun calculateAdaptiveFrameSkip(): Int {
        return when {
            movementVelocity > FAST_MOVEMENT_THRESHOLD -> MIN_FRAME_SKIP  // Every frame for fast movement
            movementVelocity > MEDIUM_MOVEMENT_THRESHOLD -> 2  // Every 2nd frame for medium movement
            else -> MAX_FRAME_SKIP  // Every 4th frame for slow/stationary
        }
    }
    
    /**
     * === Phase 4.2: Calculate rotation angle between two direction vectors ===
     */
    private fun calculateRotationAngle(dir1: FloatArray, dir2: FloatArray): Float {
        val dot = dir1[0]*dir2[0] + dir1[1]*dir2[1] + dir1[2]*dir2[2]
        val clampedDot = dot.coerceIn(-1f, 1f)
        return kotlin.math.acos(clampedDot)
    }
    
    /**
     * === Phase 5.1: Handle occlusion based on visibility ===
     */
    private fun handleOcclusion(visibility: Float) {
        if (visibility < OCCLUSION_VISIBILITY_THRESHOLD) {
            if (occlusionStartTime == 0L) {
                occlusionStartTime = System.currentTimeMillis()
                Log.d("Occlusion", "Foot occlusion detected (visibility=$visibility)")
            }
            
            val occlusionDuration = System.currentTimeMillis() - occlusionStartTime
            if (occlusionDuration > OCCLUSION_TIMEOUT_MS) {
                // Fade out model after timeout
                modelOpacity = kotlin.math.max(0f, 1.0f - (occlusionDuration - OCCLUSION_TIMEOUT_MS).toFloat() / 1000f)
                if (modelOpacity <= 0f && frameCounter % 60 == 0) {
                    Log.d("Occlusion", "Model faded out due to extended occlusion")
                }
            } else {
                // Hold last known position during brief occlusion
                modelOpacity = 1.0f
            }
        } else {
            // Foot visible again
            if (occlusionStartTime > 0L) {
                val occlusionDuration = System.currentTimeMillis() - occlusionStartTime
                Log.d("Occlusion", "Foot visible again after ${occlusionDuration}ms occlusion")
            }
            occlusionStartTime = 0L
            modelOpacity = 1.0f
        }
    }
    
    /**
     * === Phase 5.2: Predict position during occlusion ===
     */
    private fun predictPositionDuringOcclusion(): FloatArray? {
        if (positionHistory.size < 2) return null
        
        val lastPosition = positionHistory.last()
        val velocity = movementVelocity
        
        val occlusionDuration = System.currentTimeMillis() - occlusionStartTime
        if (occlusionDuration > OCCLUSION_TIMEOUT_MS) {
            return null  // Don't predict after timeout
        }
        
        // Simple linear prediction based on last velocity
        val predictedOffset = velocity * (occlusionDuration / 1000f)
        
        return FloatArray(3).apply {
            this[0] = lastPosition[0] + predictedOffset * 0.1f  // Scale down prediction
            this[1] = lastPosition[1]
            this[2] = lastPosition[2] + predictedOffset * 0.1f
        }
    }
    
    /**
     * === Phase 6.1: Compensate model position for camera movement ===
     */
    private fun compensateForCameraMovement(footPosition: FloatArray, currentCameraPose: com.google.ar.core.Pose): FloatArray {
        if (lastCameraPose == null) {
            return footPosition
        }
        
        // Calculate camera movement
        val lastCamTrans = lastCameraPose!!.translation
        val currentCamTrans = currentCameraPose.translation
        
        val camDx = currentCamTrans[0] - lastCamTrans[0]
        val camDy = currentCamTrans[1] - lastCamTrans[1]
        val camDz = currentCamTrans[2] - lastCamTrans[2]
        
        // Compensate foot position for camera movement (inverse)
        return floatArrayOf(
            footPosition[0] - camDx,
            footPosition[1] - camDy,
            footPosition[2] - camDz
        )
    }
    
    /**
     * === Phase 1.3: Calculate movement velocity from position history ===
     */
    private fun calculateMovementVelocity(newPosition: FloatArray) {
        positionHistory.add(newPosition.copyOf())
        if (positionHistory.size > MAX_POSITION_HISTORY) {
            positionHistory.removeAt(0)
        }
        
        if (positionHistory.size >= 2) {
            val lastPos = positionHistory[positionHistory.size - 1]
            val prevPos = positionHistory[positionHistory.size - 2]
            
            val dx = lastPos[0] - prevPos[0]
            val dy = lastPos[1] - prevPos[1]
            val dz = lastPos[2] - prevPos[2]
            
            movementVelocity = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            
            if (frameCounter % 60 == 0) {
                Log.d("AdaptiveThrottle", "Movement velocity: ${"%.4f".format(movementVelocity)} m/frame, frameSkip=${calculateAdaptiveFrameSkip()}")
            }
        }
    }
    
    /**
     * Check for anchor drift and trigger re-anchoring if needed.
     */
    private fun checkAnchorDrift(currentPosition: FloatArray): Boolean {
        if (lastAnchorPosition == null) {
            lastAnchorPosition = FloatArray(3)
            System.arraycopy(currentPosition, 0, lastAnchorPosition!!, 0, 3)
            anchorPositionHistory.clear()
            anchorPositionHistory.add(FloatArray(3).apply {
                System.arraycopy(currentPosition, 0, this, 0, 3)
            })
            return false
        }
        
        val dx = currentPosition[0] - lastAnchorPosition!![0]
        val dy = currentPosition[1] - lastAnchorPosition!![1]
        val dz = currentPosition[2] - lastAnchorPosition!![2]
        val drift = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        
        // Add to history
        anchorPositionHistory.add(FloatArray(3).apply {
            System.arraycopy(currentPosition, 0, this, 0, 3)
        })
        if (anchorPositionHistory.size > ANCHOR_HISTORY_SIZE) {
            anchorPositionHistory.removeAt(0)
        }
        
        if (drift > ANCHOR_DRIFT_THRESHOLD) {
            Log.w("FootAnchor", "⚠️ Anchor drift detected: ${"%.3f".format(drift)}m > ${ANCHOR_DRIFT_THRESHOLD}m")
            lastAnchorPosition = FloatArray(3)
            System.arraycopy(currentPosition, 0, lastAnchorPosition!!, 0, 3)
            anchorPositionHistory.clear()
            anchorPositionHistory.add(FloatArray(3).apply {
                System.arraycopy(currentPosition, 0, this, 0, 3)
            })
            return true
        }
        
        return false
    }

    private fun processFootPlacement(fp: FootPoint, frame: Frame?) {
        // Helper function to convert normalized coords to pixels then to view coords
        fun normalizedToView(normX: Float, normY: Float, bias: Float = 0f): Pair<Float, Float>? {
            val srcX = (normX * fp.imgWidth).coerceIn(0f, fp.imgWidth - 1f)
            val srcY = ((normY + bias) * fp.imgHeight).coerceIn(0f, fp.imgHeight - 1f)
            
            if (frame != null) {
                // ARCore: Convert camera image coords to view coords
                val inBuf = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                inBuf.put(srcX)
                inBuf.put(srcY)
                inBuf.position(0)
                
                val outBuf = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                frame.transformCoordinates2d(Coordinates2d.IMAGE_PIXELS, inBuf, Coordinates2d.VIEW, outBuf)
                
                outBuf.position(0)
                return Pair(outBuf.get(0), outBuf.get(1))
            } else {
                // VIO: Coords are already in screen space (captured via glReadPixels)
                return Pair(srcX, srcY)
            }
        }
        
        // Helper to perform hit-test and return best hit with distance validation
        // ==== Phase 1: Enhanced with multi-sampling ====
        fun hitTestPoint(viewX: Float, viewY: Float, label: String): UnifiedHit? {
            val startTime = System.currentTimeMillis()
            
            // Validate view coordinates are within screen bounds
            if (viewX < 0 || viewY < 0 || viewX >= surfaceWidth || viewY >= surfaceHeight) {
                Log.w("FootAnchor", "❌ Invalid view coords for $label: ($viewX, $viewY)")
                return null
            }
            
            if (frame != null) {
                // ==== Phase 1: Multi-sample hit testing ====
                val sampleHits = mutableListOf<UnifiedHit>()
                val sampleOffsets = arrayOf(
                    Pair(0f, 0f),  // Center
                    Pair(-HIT_TEST_SAMPLE_RADIUS, 0f),
                    Pair(HIT_TEST_SAMPLE_RADIUS, 0f),
                    Pair(0f, -HIT_TEST_SAMPLE_RADIUS),
                    Pair(0f, HIT_TEST_SAMPLE_RADIUS)
                )
                
                for (i in 0 until HIT_TEST_SAMPLE_COUNT) {
                    val offset = sampleOffsets[i % sampleOffsets.size]
                    val sampleX = (viewX + offset.first).coerceIn(0f, surfaceWidth - 1f)
                    val sampleY = (viewY + offset.second).coerceIn(0f, surfaceHeight - 1f)
                    
                    var bestHit: com.google.ar.core.HitResult? = null
                    var bestPref = -1
                    
                    for (hit in frame.hitTest(sampleX, sampleY)) {
                        val tr = hit.trackable
                        
                        // ==== Phase 5: Plane quality validation ====
                        val isValidDistance = hit.distance >= minHitDistance && hit.distance <= maxHitDistance
                        if (!isValidDistance) {
                            continue
                        }
                        
                        // Enhanced plane validation
                        val planeValid = when (tr) {
                            is com.google.ar.core.Plane -> {
                                val isTracking = tr.trackingState == TrackingState.TRACKING
                                val inPolygon = tr.isPoseInPolygon(hit.hitPose)
                                // Check plane extent (if available)
                                val hasGoodExtent = try {
                                    val centerPose = tr.centerPose
                                    val extentX = tr.extentX
                                    val extentZ = tr.extentZ
                                    extentX >= MIN_PLANE_EXTENT && extentZ >= MIN_PLANE_EXTENT
                                } catch (e: Exception) {
                                    true  // If extent not available, assume valid
                                }
                                isTracking && inPolygon && hasGoodExtent
                            }
                            else -> true
                        }
                        
                        if (!planeValid) continue
                        
                        val pref = when (tr) {
                            is com.google.ar.core.Plane -> 3
                            is com.google.ar.core.Point -> 2
                            is com.google.ar.core.DepthPoint -> 1
                            else -> 0
                        }
                        
                        if (pref > bestPref || (pref == bestPref && bestHit != null && hit.distance < bestHit!!.distance)) {
                            bestHit = hit
                            bestPref = pref
                        }
                    }
                    
                    bestHit?.let { hit ->
                        val poseMatrix = FloatArray(16)
                        hit.hitPose.toMatrix(poseMatrix, 0)
                        sampleHits.add(UnifiedHit(
                            hitPoseMatrix = poseMatrix,
                            distance = hit.distance,
                            trackableType = when (hit.trackable) {
                                is com.google.ar.core.Plane -> "Plane"
                                is com.google.ar.core.Point -> "Point"
                                is com.google.ar.core.DepthPoint -> "DepthPoint"
                                else -> "Other"
                            },
                            arCoreHit = hit
                        ))
                    }
                }
                
                // Average the sample hits (weighted by preference and distance)
                if (sampleHits.isEmpty()) {
                    hitTestPerformanceMs = System.currentTimeMillis() - startTime
                    return null
                }
                
                // Filter outliers: reject hits that are too far from the median distance
                val distances = sampleHits.map { it.distance }.sorted()
                val medianDistance = distances[distances.size / 2]
                val filteredHits = sampleHits.filter { 
                    kotlin.math.abs(it.distance - medianDistance) < 0.2f  // Within 20cm of median
                }
                
                if (filteredHits.isEmpty()) {
                    hitTestPerformanceMs = System.currentTimeMillis() - startTime
                    return null
                }
                
                // Weight hits: prefer Plane > Point > DepthPoint, and closer distances
                val weightedHits = filteredHits.map { hit ->
                    val typeWeight = when (hit.trackableType) {
                        "Plane" -> 3.0f
                        "Point" -> 2.0f
                        "DepthPoint" -> 1.0f
                        else -> 0.5f
                    }
                    val distanceWeight = 1.0f / (hit.distance + 0.1f)  // Inverse distance weight
                    Pair(hit, typeWeight * distanceWeight)
                }
                
                // Average poses weighted by preference and distance
                val totalWeight = weightedHits.sumOf { it.second.toDouble() }.toFloat()
                if (totalWeight == 0f) {
                    hitTestPerformanceMs = System.currentTimeMillis() - startTime
                    return filteredHits.first()  // Fallback to first hit
                }
                
                // Average translation
                var avgX = 0f
                var avgY = 0f
                var avgZ = 0f
                var avgDistance = 0f
                
                weightedHits.forEach { (hit, weight) ->
                    val w = weight / totalWeight
                    avgX += hit.hitPoseMatrix[12] * w
                    avgY += hit.hitPoseMatrix[13] * w
                    avgZ += hit.hitPoseMatrix[14] * w
                    avgDistance += hit.distance * w
                }
                
                // Use the best hit's rotation (from highest weighted hit)
                val bestWeightedHit = weightedHits.maxByOrNull { it.second }?.first ?: filteredHits.first()
                val averagedMatrix = FloatArray(16)
                System.arraycopy(bestWeightedHit.hitPoseMatrix, 0, averagedMatrix, 0, 16)
                averagedMatrix[12] = avgX
                averagedMatrix[13] = avgY
                averagedMatrix[14] = avgZ
                
                hitTestPerformanceMs = System.currentTimeMillis() - startTime
                
                if (frameCounter % 60 == 0) {
                    Log.d("FootAnchor", "Multi-sample hit test: ${sampleHits.size} samples, ${filteredHits.size} after filtering, avgDist=${"%.2f".format(avgDistance)}m")
                }
                
                return UnifiedHit(
                    hitPoseMatrix = averagedMatrix,
                    distance = avgDistance,
                    trackableType = bestWeightedHit.trackableType,
                    arCoreHit = bestWeightedHit.arCoreHit
                )
            } else if (poseProvider != null) {
                // VIO Hit Test (keep original implementation for now)
                val hits = poseProvider.hitTest(viewX, viewY)
                val bestHit = hits.minByOrNull { it.distance }
                
                hitTestPerformanceMs = System.currentTimeMillis() - startTime
                
                return bestHit?.let { hit ->
                    UnifiedHit(
                        hitPoseMatrix = hit.hitPose.toMatrix(),
                        distance = hit.distance,
                        trackableType = hit.trackableType.name,
                        arCoreHit = null
                    )
                }
            }
            
            hitTestPerformanceMs = System.currentTimeMillis() - startTime
            return null
        }
        
        // Hit-test ankle and toe positions to get 3D world coordinates
        Log.d("FootAnchor", "Processing foot: side=${fp.side} vis=${fp.visibility} ankle=(${fp.ankleX},${fp.ankleY}) toe=(${fp.toeX},${fp.toeY})")
        
        val ankleView = normalizedToView(fp.ankleX, fp.ankleY)
        val toeView = if (fp.toeX != null && fp.toeY != null) {
            normalizedToView(fp.toeX, fp.toeY, 0.005f) // small downward bias
        } else null
        
        Log.d("FootAnchor", "Converted to view coords: ankle=$ankleView toe=$toeView")
        
        val ankleHit = ankleView?.let { (vx, vy) -> hitTestPoint(vx, vy, "ankle") }
        val toeHit = toeView?.let { (vx, vy) -> hitTestPoint(vx, vy, "toe") }
        
        Log.d("FootAnchor", "Hit test results: ankleHit=${ankleHit != null} (d=${ankleHit?.distance?.let{"%.2f".format(it)}}) toeHit=${toeHit != null} (d=${toeHit?.distance?.let{"%.2f".format(it)}})")
        
        // Prefer toe for anchor, fall back to ankle
        val primaryHit = toeHit ?: ankleHit
        
        primaryHit?.let { chosen ->
            // Validate that ankle and toe are reasonably close to each other
            if (ankleHit != null && toeHit != null) {
                val distanceBetween = kotlin.math.abs(ankleHit.distance - toeHit.distance)
                if (distanceBetween > 0.5f) {
                    Log.w("FootAnchor", "❌ Ankle/toe distance mismatch: ${distanceBetween}m, skipping")
                    return@let
                }
            }
            
            // === Phase 2.1: Anchor tracking state management ===
            // Check if we need to create a new anchor or update existing one
            val currentPos = floatArrayOf(chosen.hitPoseMatrix[12], chosen.hitPoseMatrix[13], chosen.hitPoseMatrix[14])
            val needsNewAnchor = when {
                currentAnchor == null -> true
                currentAnchor!!.trackingState == TrackingState.STOPPED -> {
                    Log.d("FootAnchor", "Anchor stopped, creating new anchor")
                    true
                }
                currentAnchor!!.trackingState == TrackingState.PAUSED -> {
                    Log.d("FootAnchor", "Anchor paused, holding position")
                    false  // Hold position when paused
                }
                else -> {
                    // Check drift from anchor position
                    val anchorTrans = currentAnchor!!.pose.translation
                    val dx = currentPos[0] - anchorTrans[0]
                    val dy = currentPos[1] - anchorTrans[1]
                    val dz = currentPos[2] - anchorTrans[2]
                    val drift = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                    if (drift > ANCHOR_DRIFT_THRESHOLD) {
                        Log.d("FootAnchor", "Anchor drift ${"%.3f".format(drift)}m > ${ANCHOR_DRIFT_THRESHOLD}m, re-anchoring")
                        true
                    } else {
                        false  // Update transform relative to existing anchor
                    }
                }
            }
            
            if (needsNewAnchor) {
                // Clear old anchors
                if (shoePlaced) {
                    anchors.forEach { it.detach() }
                    anchors.clear()
                    vioAnchors.clear()
                }
                
                // Create new anchor
                if (chosen.arCoreHit != null) {
                    currentAnchor = chosen.arCoreHit.createAnchor()
                    anchors.add(currentAnchor!!)
                    Log.d("FootAnchor", "✅ Created new anchor (trackingState=${currentAnchor!!.trackingState})")
                } else {
                    vioAnchors.add(chosen.hitPoseMatrix)
                    currentAnchor = null
                }
            } else if (currentAnchor != null && currentAnchor!!.trackingState == TrackingState.TRACKING) {
                // === Phase 2.2: Update transform relative to existing anchor ===
                // Anchor exists and is tracking, we'll update transform in matrix calculation
                Log.v("FootAnchor", "Updating transform relative to existing anchor")
            }
            
            // Calculate foot dimensions and orientation if we have both ankle and toe
            if (ankleHit != null && toeHit != null) {
                val anklePos = FloatArray(3)
                val toePos = FloatArray(3)
                
                // Extract translation from matrices
                anklePos[0] = ankleHit.hitPoseMatrix[12]
                anklePos[1] = ankleHit.hitPoseMatrix[13]
                anklePos[2] = ankleHit.hitPoseMatrix[14]
                
                toePos[0] = toeHit.hitPoseMatrix[12]
                toePos[1] = toeHit.hitPoseMatrix[13]
                toePos[2] = toeHit.hitPoseMatrix[14]
                
                // ==== Phase 4: Improved coordinate transformations ====
                // Get plane normal if available (for proper up-vector alignment)
                var planeNormal: FloatArray? = null
                if (chosen.arCoreHit != null && chosen.arCoreHit.trackable is Plane) {
                    val plane = chosen.arCoreHit.trackable as Plane
                    planeNormal = FloatArray(3)
                    val centerPose = plane.centerPose
                    // Plane normal is Y-up in ARCore (0, 1, 0)
                    planeNormal[0] = 0f
                    planeNormal[1] = 1f
                    planeNormal[2] = 0f
                } else {
                    // Default to Y-up
                    planeNormal = floatArrayOf(0f, 1f, 0f)
                }
                
                // Calculate foot direction vector (ankle to toe)
                val footDir = FloatArray(3)
                footDir[0] = toePos[0] - anklePos[0]
                footDir[1] = toePos[1] - anklePos[1]
                footDir[2] = toePos[2] - anklePos[2]
                val footDirLength = kotlin.math.sqrt(footDir[0]*footDir[0] + footDir[1]*footDir[1] + footDir[2]*footDir[2])
                
                if (footDirLength < 0.01f) {
                    Log.w("FootAnchor", "Foot direction vector too short, skipping")
                    return@let
                }
                
                // Normalize foot direction
                footDir[0] /= footDirLength
                footDir[1] /= footDirLength
                footDir[2] /= footDirLength
                
                // Project foot direction onto plane (remove component along plane normal)
                val dot = footDir[0]*planeNormal[0] + footDir[1]*planeNormal[1] + footDir[2]*planeNormal[2]
                val forwardDir = FloatArray(3)
                forwardDir[0] = footDir[0] - dot * planeNormal[0]
                forwardDir[1] = footDir[1] - dot * planeNormal[1]
                forwardDir[2] = footDir[2] - dot * planeNormal[2]
                
                // Normalize forward direction
                val forwardLength = kotlin.math.sqrt(forwardDir[0]*forwardDir[0] + forwardDir[1]*forwardDir[1] + forwardDir[2]*forwardDir[2])
                if (forwardLength > 0.01f) {
                    forwardDir[0] /= forwardLength
                    forwardDir[1] /= forwardLength
                    forwardDir[2] /= forwardLength
                } else {
                    // Fallback: use X-axis if projection fails
                    forwardDir[0] = 1f
                    forwardDir[1] = 0f
                    forwardDir[2] = 0f
                }
                
                // === Phase 4.2: Detect foot rotation/pivoting ===
                lastFootDirection?.let { lastDir ->
                    val rotationAngle = calculateRotationAngle(lastDir, forwardDir)
                    if (rotationAngle > ROTATION_THRESHOLD && frameCounter % 30 == 0) {
                        Log.d("FootRotation", "Significant rotation detected: ${"%.2f".format(Math.toDegrees(rotationAngle.toDouble()))}°")
                    }
                }
                lastFootDirection = forwardDir.copyOf()
                
                // Build orthonormal basis: forward, right, up
                val rightDir = FloatArray(3)
                rightDir[0] = forwardDir[1]*planeNormal[2] - forwardDir[2]*planeNormal[1]
                rightDir[1] = forwardDir[2]*planeNormal[0] - forwardDir[0]*planeNormal[2]
                rightDir[2] = forwardDir[0]*planeNormal[1] - forwardDir[1]*planeNormal[0]
                val rightLength = kotlin.math.sqrt(rightDir[0]*rightDir[0] + rightDir[1]*rightDir[1] + rightDir[2]*rightDir[2])
                if (rightLength > 0.01f) {
                    rightDir[0] /= rightLength
                    rightDir[1] /= rightLength
                    rightDir[2] /= rightLength
                }
                
                // Calculate foot length in 3D (projected onto plane)
                val footLengthMeters = footDirLength
                
                // Check if foot length is realistic
                val isRealisticLength = footLengthMeters >= minFootLength && footLengthMeters <= maxFootLength
                
                // ==== Phase 3: Apply calibration to scale ====
                val footScale = if (calibratedShoeSize != null) {
                    // Use calibrated shoe size
                    val calibratedLengthMeters = calibratedShoeSize!! / 100f  // Convert cm to meters
                    (calibratedLengthMeters / referenceFootLength).coerceIn(0.7f, 1.3f)
                } else if (isRealisticLength) {
                    // Use measured foot length
                    (footLengthMeters / referenceFootLength).coerceIn(0.7f, 1.3f)
                } else {
                    Log.w("FootAnchor", "Unrealistic foot length ${footLengthMeters}m, using 1.0x scale")
                    1.0f
                }
                
                // Build model matrix with improved coordinate system
                val shoeMatrix = FloatArray(16)
                android.opengl.Matrix.setIdentityM(shoeMatrix, 0)
                
                // Set rotation matrix from orthonormal basis (forward, right, up)
                shoeMatrix[0] = rightDir[0]
                shoeMatrix[1] = rightDir[1]
                shoeMatrix[2] = rightDir[2]
                shoeMatrix[4] = planeNormal[0]
                shoeMatrix[5] = planeNormal[1]
                shoeMatrix[6] = planeNormal[2]
                shoeMatrix[8] = -forwardDir[0]
                shoeMatrix[9] = -forwardDir[1]
                shoeMatrix[10] = -forwardDir[2]
                
                // === Phase 2.2: Use anchor pose if updating relative to existing anchor ===
                // === Phase 6.1: Compensate for camera movement ===
                val baseTranslation = if (currentAnchor != null && currentAnchor!!.trackingState == TrackingState.TRACKING) {
                    // Use anchor pose as base, then offset to current foot position
                    val anchorPose = currentAnchor!!.pose
                    val anchorTrans = anchorPose.translation
                    // Calculate offset from anchor to current toe position
                    val offsetX = toePos[0] - anchorTrans[0]
                    val offsetY = toePos[1] - anchorTrans[1]
                    val offsetZ = toePos[2] - anchorTrans[2]
                    // Start from anchor, apply offset
                    floatArrayOf(anchorTrans[0] + offsetX, anchorTrans[1] + offsetY, anchorTrans[2] + offsetZ)
                } else {
                    // New anchor or no anchor - use toe position directly
                    // === Phase 6.1: Compensate for camera movement when no anchor ===
                    if (lastCameraPose != null && frame != null) {
                        compensateForCameraMovement(toePos, frame.camera.pose)
                    } else {
                        toePos
                    }
                }
                
                // Set translation
                shoeMatrix[12] = baseTranslation[0]
                shoeMatrix[13] = baseTranslation[1]
                shoeMatrix[14] = baseTranslation[2]
                
                // Translate in the rotated coordinate system:
                // - Y-up to lift shoe above floor
                // - Z-back to move shoe origin from toe toward heel (40% of foot length)
                val backOffset = footLengthMeters * 0.4f
                android.opengl.Matrix.translateM(shoeMatrix, 0, 0f, soleOffsetMeters, -backOffset)
                
                // Apply scale: base model scale * foot-size scale
                val finalScale = baseModelScale * footScale
                android.opengl.Matrix.scaleM(shoeMatrix, 0, finalScale, finalScale, finalScale)
                
                // ==== Phase 4: Check anchor drift ====
                val finalPos = floatArrayOf(shoeMatrix[12], shoeMatrix[13], shoeMatrix[14])
                // === Phase 1.3: Update movement velocity for adaptive throttling ===
                calculateMovementVelocity(finalPos)
                // Note: Drift check already done above in anchor management
                
                // ==== Phase 2: Apply pose smoothing ====
                // === Phase 4.3: Use velocity-based adaptive smoothing ===
                val adaptiveSmoothingAlpha = getAdaptiveSmoothingAlpha(movementVelocity)
                val smoothedMatrix = smoothPose(shoeMatrix, adaptiveSmoothingAlpha)
                
                shoeModelMatrix = smoothedMatrix
                shoePlaced = true
                lastPlacementTime = System.currentTimeMillis()
                
                // ==== Phase 6: Debug logging ====
                val calibrationInfo = if (calibratedShoeSize != null) {
                    "calibrated=${calibratedShoeSize}cm"
                } else {
                    "measured"
                }
                
                if (frameCounter % 60 == 0) {
                    Log.d(
                        "FootAnchor",
                        "✅ Anchor(${chosen.trackableType}, d=${"%.2f".format(chosen.distance)}) side=${fp.side} " +
                        "footLen=${"%.3f".format(footLengthMeters)}m (${if(isRealisticLength) "OK" else "BAD"}) " +
                        "footScale=${"%.2f".format(footScale)} ($calibrationInfo) finalScale=${"%.4f".format(finalScale)} " +
                        "hitTest=${hitTestPerformanceMs}ms smooth=${smoothingPerformanceMs}ms"
                    )
                }
            } else {
                // Fallback: simple placement if only one point available
                val anchorM = FloatArray(16)
                System.arraycopy(chosen.hitPoseMatrix, 0, anchorM, 0, 16)
                android.opengl.Matrix.translateM(anchorM, 0, 0f, soleOffsetMeters, 0f)
                
                // Apply calibration if available
                val footScale = calibratedShoeSize?.let {
                    (it / 100f / referenceFootLength).coerceIn(0.7f, 1.3f)
                } ?: 1.0f
                val finalScale = baseModelScale * footScale
                
                android.opengl.Matrix.scaleM(anchorM, 0, finalScale, finalScale, finalScale)
                
                // Apply smoothing
                val smoothedMatrix = smoothPose(anchorM)
                
                shoeModelMatrix = smoothedMatrix
                shoePlaced = true
                lastPlacementTime = System.currentTimeMillis()
                
                if (frameCounter % 60 == 0) {
                    Log.d(
                        "FootAnchor",
                        "✅ Anchor(${chosen.trackableType}, d=${"%.2f".format(chosen.distance)}) (simple placement, single point)"
                    )
                }
            }
        }
        
        if (primaryHit == null) {
            Log.d("FootAnchor", "ℹ️ No hit for ${fp.side} foot")
        }
    }
}
