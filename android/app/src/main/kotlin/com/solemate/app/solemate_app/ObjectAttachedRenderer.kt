package com.solemate.app.solemate_app

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.google.android.filament.Texture
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * AR Renderer using Hybrid Approach:
 * 1. ML/TFLite detects foot location in 2D (screen coordinates)
 * 2. ARCore Instant Placement creates a 3D anchor at that location
 * 3. Shoe model follows the anchor (proper 3D tracking)
 * 
 * This replaces manual 3D position computation with ARCore's built-in positioning.
 */
class ObjectAttachedRenderer(
    private val session: Session,
    private val rotationHelper: DisplayRotationHelper,
    private val activity: ARActivity,
    private var shoeRenderer: ShoeRenderer? = null  // Single renderer managing both shoes
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ObjectAttachedRenderer"
        
        // Detection settings
        private const val FRAME_SKIP = 3  // Process every Nth frame
        
        // Instant placement settings
        private const val DEFAULT_DEPTH = 1.2f  // Fallback distance to feet (meters)
        
        // Shoe model settings
        private const val SHOE_SCALE = 0.08f  // Scale for shoe model
        
        // Smoothing settings
        private const val POSITION_MIN_CUTOFF = 1.0f  // Lower = more smoothing
        private const val POSITION_BETA = 0.5f  // Higher = less lag when fast
        private const val ANCHOR_UPDATE_THRESHOLD = 0.15f  // Min distance (m) to update anchor
        private const val POSITION_BLEND_FACTOR = 0.3f  // Interpolation speed
    }

    // Rendering components
    private val backgroundRenderer = BackgroundRenderer()
    private val planeRenderer = PlaneRenderer()
    
    // Detection infrastructure
    private val detectionExecutor = Executors.newSingleThreadExecutor()
    private val footDetector = FootObjectDetector(activity)
    private val footTracker = FootTracker(activity)  // For landmark-based orientation
    private val isProcessing = AtomicBoolean(false)
    private var frameCounter = 0
    
    // Detected feet from background thread
    private val pendingDetections = AtomicReference<List<DetectedFoot>>(emptyList())
    
    // Foot orientation from bounding box (for tilt tracking) - PER FOOT
    @Volatile
    private var leftTiltAngle: Float = 0f
    @Volatile
    private var leftRollAngle: Float = 0f
    @Volatile
    private var rightTiltAngle: Float = 0f
    @Volatile
    private var rightRollAngle: Float = 0f
    
    // Dual foot anchors - one for each foot
    private var leftFootAnchor: Anchor? = null
    private var rightFootAnchor: Anchor? = null
    private var leftFootId: Int? = null
    private var rightFootId: Int? = null
    
    // Per-foot visibility - only render shoe when foot is currently detected
    @Volatile
    private var leftFootVisible: Boolean = false
    @Volatile
    private var rightFootVisible: Boolean = false
    
    // Surface and image dimensions
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var lastImageWidth = 640
    private var lastImageHeight = 480
    
    // Recalibration flag
    @Volatile
    private var recalibrationRequested = false
    
    // === Temporal Smoothing Filters ===
    private val leftPositionFilter = OneEuroFilter2D(POSITION_MIN_CUTOFF, POSITION_BETA)
    private val rightPositionFilter = OneEuroFilter2D(POSITION_MIN_CUTOFF, POSITION_BETA)
    private val leftTiltFilter = OneEuroFilter(POSITION_MIN_CUTOFF, POSITION_BETA)
    private val rightTiltFilter = OneEuroFilter(POSITION_MIN_CUTOFF, POSITION_BETA)
    private val leftRollFilter = OneEuroFilter(POSITION_MIN_CUTOFF, POSITION_BETA)
    private val rightRollFilter = OneEuroFilter(POSITION_MIN_CUTOFF, POSITION_BETA)
    
    // Smoothed detection positions (normalized 0-1)
    private var smoothedLeftX: Float = 0f
    private var smoothedLeftY: Float = 0f
    private var smoothedRightX: Float = 0f
    private var smoothedRightY: Float = 0f
    
    // Track if initial anchor has been placed
    private var leftAnchorPlaced = false
    private var rightAnchorPlaced = false
    
    // === Depth Occlusion ===
    private var depthTextureHandler: DepthTextureHandler? = null
    private var isDepthSupported = false

    fun release() {
        try {
            footDetector.close()
        } catch (_: Exception) { }
        try {
            footTracker.close()
        } catch (_: Exception) { }
        try {
            detectionExecutor.shutdownNow()
        } catch (_: Exception) { }
        try {
            leftFootAnchor?.detach()
        } catch (_: Exception) { }
        try {
            rightFootAnchor?.detach()
        } catch (_: Exception) { }
        try {
            depthTextureHandler?.release()
        } catch (_: Exception) { }
        try {
            shoeRenderer?.release()
        } catch (_: Exception) { }
    }
    
    /**
     * Request recalibration - detaches anchor and forces new placement
     */
    fun requestRecalibration() {
        recalibrationRequested = true
        Log.d(TAG, "🔄 Recalibration requested")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        backgroundRenderer.createOnGlThread()
        session.setCameraTextureName(backgroundRenderer.getTextureId())
        planeRenderer.createOnGlThread()
        
        // Initialize FootTracker for landmark-based orientation
        footTracker.init()
        footDetector.init()
        
        // Check if depth is supported and enabled
        isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        Log.i(TAG, "✅ ObjectAttachedRenderer initialized (Instant Placement mode)")
        Log.i(TAG, "📐 Depth support: $isDepthSupported")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        rotationHelper.onSurfaceChanged(width, height)
        surfaceWidth = width
        surfaceHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        rotationHelper.updateSessionIfNeeded(session)

        try {
            val frame: Frame = session.update()
            backgroundRenderer.draw(frame)

            // Build camera matrices
            val projMatrix = FloatArray(16)
            val viewMatrix = FloatArray(16)
            frame.camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)
            frame.camera.getViewMatrix(viewMatrix, 0)

            // === Handle recalibration ===
            if (recalibrationRequested) {
                handleRecalibration()
            }

            // === Acquire camera image for foot detection (throttled) ===
            frameCounter++
            if (frameCounter % FRAME_SKIP == 0 && isProcessing.compareAndSet(false, true)) {
                acquireAndProcessFrame(frame)
            }

            // === Process pending detections from background thread ===
            val detections = pendingDetections.getAndSet(emptyList())
            if (detections.isNotEmpty() && frame.camera.trackingState == TrackingState.TRACKING) {
                processDetectionsWithInstantPlacement(detections, frame)
            }

            // === Update shoe rendering using anchor ===
            updateShoeFromAnchor(viewMatrix, projMatrix, frame)

        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available: ${e.message}")
        }
    }

    /**
     * Acquire camera frame and submit for background processing
     */
    private fun acquireAndProcessFrame(frame: Frame) {
        try {
            val cameraImage = frame.acquireCameraImage()
            lastImageWidth = cameraImage.width
            lastImageHeight = cameraImage.height
            
            detectionExecutor.submit {
                try {
                    val bitmap = YuvConverter.imageToBitmap(cameraImage)
                    
                    // Run object detection for bounding box
                    val detections = footDetector.detectFeet(bitmap)
                    if (detections.isNotEmpty()) {
                        pendingDetections.set(detections)
                        
                        // Process each foot for tilt/roll
                        for (foot in detections) {
                            val boxWidth = foot.width
                            val boxHeight = foot.height
                            
                            if (boxWidth > 0.01f && boxHeight > 0.01f) {
                                // === TILT (pitch - toe up/down) from aspect ratio ===
                                val aspectRatio = boxWidth / boxHeight
                                val normalAspect = 2.0f
                                val tiltFactor = (normalAspect - aspectRatio) / normalAspect
                                val tiltAngle = -tiltFactor * 90f
                                
                                // === ROLL from segmentation orientation (image moments) ===
                                // foot.orientation is computed from principal axis of foot pixels
                                val rollAngle = foot.orientation
                                
                                // Store per-foot angles WITH smoothing
                                when (foot.label) {
                                    "left_foot" -> {
                                        leftTiltAngle = leftTiltFilter.filter(tiltAngle)
                                        leftRollAngle = leftRollFilter.filter(rollAngle)
                                    }
                                    "right_foot" -> {
                                        rightTiltAngle = rightTiltFilter.filter(tiltAngle)
                                        rightRollAngle = rightRollFilter.filter(rollAngle)
                                    }
                                }
                                
                                if (frameCounter % 30 == 0) {
                                    Log.d(TAG, "${foot.label}: tilt=${"%.1f".format(tiltAngle)}° roll=${"%.1f".format(rollAngle)}° (orientation)")
                                }
                            }
                        }
                    }
                    
                    // Note: FootTracker (MediaPipe) requires full body view - not used for tilt
                } catch (t: Throwable) {
                    Log.e(TAG, "Detection error: ${t.message}")
                } finally {
                    try { cameraImage.close() } catch (_: Exception) { }
                    isProcessing.set(false)
                }
            }
        } catch (e: NotYetAvailableException) {
            isProcessing.set(false)
        } catch (e: Exception) {
            Log.e(TAG, "acquireCameraImage failed: ${e.message}")
            isProcessing.set(false)
        }
    }

    /**
     * Handle recalibration - detach current anchor and reset filters
     */
    private fun handleRecalibration() {
        recalibrationRequested = false
        leftFootAnchor?.detach()
        leftFootAnchor = null
        leftFootId = null
        leftAnchorPlaced = false
        rightFootAnchor?.detach()
        rightFootAnchor = null
        rightFootId = null
        rightAnchorPlaced = false
        
        // Reset all filters
        leftPositionFilter.reset()
        rightPositionFilter.reset()
        leftTiltFilter.reset()
        rightTiltFilter.reset()
        leftRollFilter.reset()
        rightRollFilter.reset()
        
        Log.d(TAG, "✅ Recalibration complete - anchors detached, filters reset")
    }

    /**
     * Process detected feet using ARCore Instant Placement.
     * 
     * KEY FIX: Only create anchor once per foot. Let ARCore SLAM handle tracking.
     * Only update anchor if:
     * 1. No anchor exists yet
     * 2. Anchor tracking is lost
     * 3. Detection moves significantly (threshold exceeded)
     */
    private fun processDetectionsWithInstantPlacement(detections: List<DetectedFoot>, frame: Frame) {
        for (foot in detections) {
            try {
                // Use HEEL position (top-center of bounding box)
                val rawX = foot.centerX
                val rawY = foot.boundingBox.top + foot.height * 0.1f
                
                // Apply temporal smoothing to reduce jitter
                val isLeft = foot.label == "left_foot"
                val (smoothX, smoothY) = if (isLeft) {
                    leftPositionFilter.filter(rawX, rawY)
                } else {
                    rightPositionFilter.filter(rawX, rawY)
                }
                
                // Store smoothed positions
                if (isLeft) {
                    smoothedLeftX = smoothX
                    smoothedLeftY = smoothY
                } else {
                    smoothedRightX = smoothX
                    smoothedRightY = smoothY
                }
                
                // Check if we need to create/update anchor
                val currentAnchor = if (isLeft) leftFootAnchor else rightFootAnchor
                val anchorPlaced = if (isLeft) leftAnchorPlaced else rightAnchorPlaced
                val needsNewAnchor = !anchorPlaced || 
                                     currentAnchor == null || 
                                     currentAnchor.trackingState != TrackingState.TRACKING
                
                if (needsNewAnchor) {
                    // Convert normalized coords to IMAGE_PIXELS then to VIEW
                    val imageX = smoothX * lastImageWidth
                    val imageY = smoothY * lastImageHeight
                    
                    val inputCoords = floatArrayOf(imageX, imageY)
                    val viewCoords = FloatArray(2)
                    
                    frame.transformCoordinates2d(
                        Coordinates2d.IMAGE_PIXELS,
                        inputCoords,
                        Coordinates2d.VIEW,
                        viewCoords
                    )
                    
                    // Try to get actual depth from ARCore depth image
                    val estimatedDepth = getDepthAtPoint(frame, viewCoords[0].toInt(), viewCoords[1].toInt())
                    
                    // Use Instant Placement hit test
                    val hits = frame.hitTestInstantPlacement(viewCoords[0], viewCoords[1], estimatedDepth)
                    
                    if (hits.isNotEmpty()) {
                        val hit = hits[0]
                        val newAnchor = hit.createAnchor()
                        val pose = newAnchor.pose
                        
                        // Detach old anchor and assign new one
                        if (isLeft) {
                            leftFootAnchor?.detach()
                            leftFootAnchor = newAnchor
                            leftFootId = foot.id
                            leftAnchorPlaced = true
                            Log.i(TAG, "👟 LEFT anchor CREATED at [${"%.2f".format(pose.tx())}, ${"%.2f".format(pose.ty())}, ${"%.2f".format(pose.tz())}] depth=${"%.2f".format(estimatedDepth)}m")
                        } else {
                            rightFootAnchor?.detach()
                            rightFootAnchor = newAnchor
                            rightFootId = foot.id
                            rightAnchorPlaced = true
                            Log.i(TAG, "👟 RIGHT anchor CREATED at [${"%.2f".format(pose.tx())}, ${"%.2f".format(pose.ty())}, ${"%.2f".format(pose.tz())}] depth=${"%.2f".format(estimatedDepth)}m")
                        }
                    }
                }
                // ELSE: Anchor exists and is tracking - let ARCore SLAM handle it!
                // This is the KEY FIX - we don't recreate anchors every frame
                
            } catch (e: Exception) {
                Log.e(TAG, "Instant placement failed for ${foot.label}: ${e.message}")
            }
        }
    }
    
    /**
     * Try to get actual depth from ARCore depth image at a screen point.
     * Falls back to DEFAULT_DEPTH if depth not available.
     */
    private fun getDepthAtPoint(frame: Frame, screenX: Int, screenY: Int): Float {
        if (!isDepthSupported) return DEFAULT_DEPTH
        
        try {
            val depthImage = frame.acquireDepthImage16Bits()
            try {
                val width = depthImage.width
                val height = depthImage.height
                
                // Map screen coords to depth image coords
                val depthX = (screenX * width / surfaceWidth).coerceIn(0, width - 1)
                val depthY = (screenY * height / surfaceHeight).coerceIn(0, height - 1)
                
                val plane = depthImage.planes[0]
                val buffer = plane.buffer
                val rowStride = plane.rowStride
                
                // Read 16-bit depth value (in millimeters)
                val offset = depthY * rowStride + depthX * 2
                if (offset + 1 < buffer.capacity()) {
                    buffer.position(offset)
                    val depthMm = (buffer.get().toInt() and 0xFF) or ((buffer.get().toInt() and 0xFF) shl 8)
                    val depthM = depthMm / 1000f
                    
                    // Validate depth range (0.2m to 3m typical for feet)
                    if (depthM in 0.2f..3.0f) {
                        return depthM
                    }
                }
            } finally {
                depthImage.close()
            }
        } catch (e: Exception) {
            // Depth not available, use default
        }
        
        return DEFAULT_DEPTH
    }

    /**
     * Update shoe rendering from anchor pose with depth occlusion
     * 
     * Renders BOTH left and right shoes using a single ShoeRenderer.
     * Each shoe tracks its respective foot anchor independently.
     */
    private fun updateShoeFromAnchor(viewMatrix: FloatArray, projMatrix: FloatArray, frame: Frame) {
        shoeRenderer?.let { sr ->
            sr.setCamera(viewMatrix, projMatrix)
            
            if (isDepthSupported) {
                updateDepthForOcclusion(sr, frame)
            }
            
            // === Render LEFT shoe ===
            leftFootAnchor?.let { anchor ->
                if (anchor.trackingState == TrackingState.TRACKING) {
                    val matrix = computeShoeMatrix(anchor, leftTiltAngle, leftRollAngle, isMirrored = false)
                    sr.setLeftModelMatrix(matrix)
                    
                    if (frameCounter % 60 == 0) {
                        Log.d(TAG, "👟 LEFT shoe rendering")
                    }
                }
            }
            
            // === Render RIGHT shoe (mirrored) ===
            rightFootAnchor?.let { anchor ->
                if (anchor.trackingState == TrackingState.TRACKING) {
                    val matrix = computeShoeMatrix(anchor, rightTiltAngle, rightRollAngle, isMirrored = true)
                    sr.setRightModelMatrix(matrix)
                    
                    if (frameCounter % 60 == 0) {
                        Log.d(TAG, "👟 RIGHT shoe rendering (mirrored)")
                    }
                }
            }
        }
    }
    
    /**
     * Compute the final transformation matrix for a shoe
     */
    private fun computeShoeMatrix(
        anchor: Anchor,
        tiltAngle: Float,
        rollAngle: Float,
        isMirrored: Boolean
    ): FloatArray {
        val modelMatrix = FloatArray(16)
        anchor.pose.toMatrix(modelMatrix, 0)
        
        // World space offsets
        modelMatrix[13] = modelMatrix[13] - 0.05f  // Y down (foot immersion)
        modelMatrix[14] = modelMatrix[14] + 0.15f  // Z backward (behind leg)
        
        val finalMatrix = FloatArray(16)
        System.arraycopy(modelMatrix, 0, finalMatrix, 0, 16)
        
        // Rotation corrections
        Matrix.rotateM(finalMatrix, 0, 180f, 0f, 0f, 1f)  // Flip around Z
        Matrix.rotateM(finalMatrix, 0, 180f, 1f, 0f, 0f)  // Flip around X
        
        // Per-foot tilt tracking
        if (kotlin.math.abs(tiltAngle) > 0.5f) {
            Matrix.rotateM(finalMatrix, 0, tiltAngle, 1f, 0f, 0f)
        }
        if (kotlin.math.abs(rollAngle) > 0.5f) {
            Matrix.rotateM(finalMatrix, 0, rollAngle, 0f, 0f, 1f)
        }
        
        // Apply scale - mirror on X for right foot
        val xScale = if (isMirrored) -SHOE_SCALE else SHOE_SCALE
        Matrix.scaleM(finalMatrix, 0, xScale, SHOE_SCALE, SHOE_SCALE)
        
        return finalMatrix
    }
    
    /**
     * Update depth texture and pass to ShoeRenderer for occlusion.
     * Lazily initializes DepthTextureHandler when first called with a valid engine.
     */
    private fun updateDepthForOcclusion(sr: ShoeRenderer, frame: Frame) {
        try {
            // Lazy initialization of DepthTextureHandler
            if (depthTextureHandler == null) {
                val engine = sr.getEngine()
                if (engine != null) {
                    depthTextureHandler = DepthTextureHandler(engine)
                    Log.i(TAG, "📐 DepthTextureHandler initialized")
                } else {
                    return // Engine not ready yet
                }
            }
            
            val handler = depthTextureHandler ?: return
            
            // Update depth texture from current frame
            val depthUpdated = handler.updateDepthTexture(frame)
            
            if (depthUpdated) {
                // Pass depth data to ShoeRenderer
                val depthTexture = handler.getDepthTexture()
                val uvTransform = handler.getDepthUvTransform()
                sr.setDepthData(depthTexture, uvTransform)
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Depth update failed: ${e.message}")
            // Continue without occlusion
        }
    }
    
    /**
     * Get current anchor tracking state - true if ANY foot is anchored
     */
    fun isShoeAnchored(): Boolean {
        val leftTracking = leftFootAnchor?.trackingState == TrackingState.TRACKING
        val rightTracking = rightFootAnchor?.trackingState == TrackingState.TRACKING
        return leftTracking || rightTracking
    }
}
