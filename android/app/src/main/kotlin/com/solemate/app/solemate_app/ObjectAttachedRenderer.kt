package com.solemate.app.solemate_app

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.Anchor
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
    private val isProcessing = AtomicBoolean(false)
    private var frameCounter = 0
    
    // Detected feet from background thread
    private val pendingDetections = AtomicReference<List<DetectedFoot>>(emptyList())
    
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

    fun release() {
        try {
            footDetector.close()
        } catch (_: Exception) { }
        try {
            detectionExecutor.shutdownNow()
        } catch (_: Exception) { }
        try {
            footAnchor?.detach()
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
        footDetector.init()
        Log.i(TAG, "✅ ObjectAttachedRenderer initialized (Instant Placement mode)")
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
            updateShoeFromAnchor(viewMatrix, projMatrix)

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
                    val detections = footDetector.detectFeet(bitmap)
                    
                    if (detections.isNotEmpty()) {
                        pendingDetections.set(detections)
                    }
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
        
        // Only create new anchor if we don't have one, or if it's a different foot
        val needNewAnchor = footAnchor == null || 
                            attachedFootId != foot.id ||
                            footAnchor?.trackingState != TrackingState.TRACKING
        
        if (!needNewAnchor) {
            return  // Keep existing anchor
        }
        
        try {
            // Convert normalized foot center to IMAGE_PIXELS coordinates
            val imageX = foot.centerX * lastImageWidth
            val imageY = foot.centerY * lastImageHeight
            
            // Transform from IMAGE_PIXELS to VIEW coordinates
            val inputCoords = floatArrayOf(imageX, imageY)
            val viewCoords = FloatArray(2)
            
            frame.transformCoordinates2d(
                Coordinates2d.IMAGE_PIXELS,
                inputCoords,
                Coordinates2d.VIEW,
                viewCoords
            )
            
            Log.d(TAG, "Foot center: norm(${foot.centerX}, ${foot.centerY}) → img($imageX, $imageY) → view(${viewCoords[0]}, ${viewCoords[1]})")
            
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
     * Update shoe rendering from anchor pose
     * 
     * The anchor provides a stable 3D pose that ARCore tracks automatically
     */
    private fun updateShoeFromAnchor(viewMatrix: FloatArray, projMatrix: FloatArray) {
        shoeRenderer?.let { sr ->
            sr.setCamera(viewMatrix, projMatrix)
            
            footAnchor?.let { anchor ->
                if (anchor.trackingState == TrackingState.TRACKING) {
                    // Get model matrix from anchor pose
                    val modelMatrix = FloatArray(16)
                    anchor.pose.toMatrix(modelMatrix, 0)
                    
                    // Apply rotation to orient shoe correctly
                    // Rotate 90° around X to lay flat, then adjust for toe direction
                    val rotatedMatrix = FloatArray(16)
                    Matrix.setIdentityM(rotatedMatrix, 0)
                    
                    // Copy translation from anchor
                    Matrix.translateM(rotatedMatrix, 0, modelMatrix[12], modelMatrix[13], modelMatrix[14])
                    
                    // Apply rotations for proper shoe orientation
                    // Rotate to lay shoe flat on ground plane
                    Matrix.rotateM(rotatedMatrix, 0, -90f, 1f, 0f, 0f)
                    
                    // Apply scale
                    Matrix.scaleM(rotatedMatrix, 0, SHOE_SCALE, SHOE_SCALE, SHOE_SCALE)
                    
                    sr.setModelMatrix(rotatedMatrix)
                    
                    if (frameCounter % 60 == 0) {
                        Log.d(TAG, "👟 Rendering shoe at anchor: pos=[${modelMatrix[12]}, ${modelMatrix[13]}, ${modelMatrix[14]}]")
                    }
                }
            }
        }
    }
    
    /**
     * Get current anchor tracking state
     */
    fun isShoeAnchored(): Boolean = footAnchor?.trackingState == TrackingState.TRACKING
}
