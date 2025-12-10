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
    private var shoeRenderer: ShoeRenderer? = null
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ObjectAttachedRenderer"
        
        // Detection settings
        private const val FRAME_SKIP = 3  // Process every Nth frame
        
        // Instant placement settings
        private const val ESTIMATED_DEPTH = 1.2f  // Estimated distance to feet (meters)
        
        // Shoe model settings
        private const val SHOE_SCALE = 0.08f  // Scale for shoe model
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
    
    // Foot orientation from landmarks (for tilt tracking)
    @Volatile
    private var footTiltAngle: Float = 0f  // Tilt angle in degrees (from toe-heel vector)
    @Volatile
    private var footYawAngle: Float = 0f   // Yaw angle in degrees (left-right orientation)
    
    // Current foot anchor - the 3D anchor where shoe is placed
    private var footAnchor: Anchor? = null
    private var attachedFootId: Int? = null
    
    // Surface and image dimensions
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var lastImageWidth = 640
    private var lastImageHeight = 480
    
    // Recalibration flag
    @Volatile
    private var recalibrationRequested = false
    
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
            footAnchor?.detach()
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
                        
                        // Use bounding box to estimate foot tilt and roll
                        val foot = detections[0]
                        val boxWidth = foot.width
                        val boxHeight = foot.height
                        val centerX = foot.centerX  // Normalized 0-1
                        
                        if (boxWidth > 0.01f && boxHeight > 0.01f) {
                            // === TILT (pitch - toe up/down) ===
                            // When foot tilts up, bounding box becomes taller (lower aspect ratio)
                            val aspectRatio = boxWidth / boxHeight
                            val normalAspect = 2.0f  // Expected when flat
                            val tiltFactor = (normalAspect - aspectRatio) / normalAspect
                            // NEGATED to fix direction: higher bbox = toe up = positive tilt
                            footTiltAngle = -tiltFactor * 90f
                            
                            // === ROLL (left/right side up) ===
                            // When foot rolls to side, the visible height/shape changes
                            // Use centerY deviation - foot rolled to side appears shifted
                            val centerY = foot.centerY
                            val yOffset = centerY - 0.5f  // -0.5 to +0.5
                            footYawAngle = yOffset * 90f  // Increased sensitivity for roll
                            
                            if (frameCounter % 30 == 0) {
                                Log.d(TAG, "Foot: aspect=${"%.2f".format(aspectRatio)} tilt=${"%.1f".format(footTiltAngle)}° roll=${"%.1f".format(footYawAngle)}° centerY=${"%.2f".format(centerY)}")
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
     * Handle recalibration - detach current anchor
     */
    private fun handleRecalibration() {
        recalibrationRequested = false
        footAnchor?.detach()
        footAnchor = null
        attachedFootId = null
        Log.d(TAG, "✅ Recalibration complete - anchor detached")
    }

    /**
     * Process detected feet using ARCore Instant Placement
     * 
     * This replaces manual 3D position computation with ARCore's hitTestInstantPlacement
     */
    private fun processDetectionsWithInstantPlacement(detections: List<DetectedFoot>, frame: Frame) {
        val foot = detections.firstOrNull() ?: return
        
        // ALWAYS update anchor to follow foot movement continuously
        // Previous logic skipped updates if anchor existed, but this made shoe static
        
        try {
            // Use HEEL position (top-center of bounding box) instead of center
            // The bounding box top edge is closer to the ankle/heel
            // boundingBox: left=x1, top=y1, right=x2, bottom=y2 (normalized)
            val heelX = foot.centerX  // Center X is fine
            val heelY = foot.boundingBox.top + foot.height * 0.1f  // 10% down from top = near heel
            
            // Convert normalized heel position to IMAGE_PIXELS coordinates
            val imageX = heelX * lastImageWidth
            val imageY = heelY * lastImageHeight
            
            // Transform from IMAGE_PIXELS to VIEW coordinates
            val inputCoords = floatArrayOf(imageX, imageY)
            val viewCoords = FloatArray(2)
            
            frame.transformCoordinates2d(
                Coordinates2d.IMAGE_PIXELS,
                inputCoords,
                Coordinates2d.VIEW,
                viewCoords
            )
            
            Log.d(TAG, "Foot HEEL: norm($heelX, $heelY) → img($imageX, $imageY) → view(${viewCoords[0]}, ${viewCoords[1]})")
            
            // Use Instant Placement hit test (no planes required)
            val hits = frame.hitTestInstantPlacement(viewCoords[0], viewCoords[1], ESTIMATED_DEPTH)
            
            if (hits.isNotEmpty()) {
                // Detach old anchor if exists
                footAnchor?.detach()
                
                // Create new anchor at instant placement point
                val hit = hits[0]
                footAnchor = hit.createAnchor()
                attachedFootId = foot.id
                
                val pose = footAnchor!!.pose
                Log.i(TAG, "👟 Created anchor for ${foot.label} at pos=[${pose.tx()}, ${pose.ty()}, ${pose.tz()}]")
            } else {
                Log.w(TAG, "No instant placement hit at view coords (${viewCoords[0]}, ${viewCoords[1]})")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Instant placement failed: ${e.message}")
        }
    }

    /**
     * Update shoe rendering from anchor pose with depth occlusion
     * 
     * The anchor provides a stable 3D pose that ARCore tracks automatically.
     * Depth texture is updated and passed to ShoeRenderer for occlusion.
     */
    private fun updateShoeFromAnchor(viewMatrix: FloatArray, projMatrix: FloatArray, frame: Frame) {
        shoeRenderer?.let { sr ->
            sr.setCamera(viewMatrix, projMatrix)
            
            // === Update depth texture for occlusion ===
            if (isDepthSupported) {
                updateDepthForOcclusion(sr, frame)
            }
            
            footAnchor?.let { anchor ->
                if (anchor.trackingState == TrackingState.TRACKING) {
                    // Get model matrix from anchor pose
                    // This includes both position AND rotation from the detected surface/foot
                    val modelMatrix = FloatArray(16)
                    anchor.pose.toMatrix(modelMatrix, 0)
                    
                    // Apply WORLD SPACE offsets to anchor position BEFORE rotations
                    // This ensures the shoe moves backward toward leg in actual world coordinates
                    modelMatrix[12] = modelMatrix[12]  // X unchanged
                    modelMatrix[13] = modelMatrix[13] - 0.05f  // Y down in world space (foot immersion)
                    modelMatrix[14] = modelMatrix[14] + 0.15f  // Z backward in world space (behind leg)
                    
                    // Copy modified anchor to final matrix
                    val finalMatrix = FloatArray(16)
                    System.arraycopy(modelMatrix, 0, finalMatrix, 0, 16)
                    
                    // Rotation corrections for shoe model (in local space):
                    // Model needs: toe forward, sole down
                    Matrix.rotateM(finalMatrix, 0, 180f, 0f, 0f, 1f)  // Flip around Z
                    Matrix.rotateM(finalMatrix, 0, 180f, 1f, 0f, 0f)  // Flip around X
                    
                    // Apply foot tilt tracking from bounding box
                    // footTiltAngle = pitch (toe up/down)
                    if (kotlin.math.abs(footTiltAngle) > 0.5f) {
                        Matrix.rotateM(finalMatrix, 0, footTiltAngle, 1f, 0f, 0f)  // Tilt around X
                    }
                    // footYawAngle = roll (left/right side up)
                    if (kotlin.math.abs(footYawAngle) > 0.5f) {
                        Matrix.rotateM(finalMatrix, 0, footYawAngle, 0f, 0f, 1f)  // Roll around Z
                    }
                    
                    // Apply scale
                    Matrix.scaleM(finalMatrix, 0, SHOE_SCALE, SHOE_SCALE, SHOE_SCALE)
                    
                    sr.setModelMatrix(finalMatrix)
                    
                    if (frameCounter % 60 == 0) {
                        val occlusionStatus = if (sr.isOcclusionActive()) "ON" else "OFF"
                        Log.d(TAG, "👟 Rendering shoe: pos=[${modelMatrix[12]}, ${modelMatrix[13]}, ${modelMatrix[14]}], occlusion=$occlusionStatus")
                    }
                }
            }
        }
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
     * Get current anchor tracking state
     */
    fun isShoeAnchored(): Boolean = footAnchor?.trackingState == TrackingState.TRACKING
}
