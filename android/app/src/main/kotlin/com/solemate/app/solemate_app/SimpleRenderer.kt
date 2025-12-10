//package com.solemate.app.solemate_app
//
//import android.opengl.GLES20
//import android.opengl.GLSurfaceView
//import com.google.ar.core.Frame
//import com.google.ar.core.Session
//import com.google.ar.core.exceptions.CameraNotAvailableException
//import javax.microedition.khronos.egl.EGLConfig
//import javax.microedition.khronos.opengles.GL10
//
///**
// * Minimal ARCore renderer that clears the screen and draws the AR camera texture.
// */
//class SimpleRenderer(private val session: Session) : GLSurfaceView.Renderer {
//
//    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        // Let ARCore know a new GL context is available
//        session.setCameraTextureName(createTexture())
//    }
//
//    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//        GLES20.glViewport(0, 0, width, height)
//    }
//
//    override fun onDrawFrame(gl: GL10?) {
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
//        try {
//            val frame: Frame = session.update()
//            // Draw the camera image from ARCore’s background texture
//            session.setCameraTextureName(createTexture())
//            frame.camera
//        } catch (e: CameraNotAvailableException) {
//            e.printStackTrace()
//        }
//    }
//
//    /** Creates a simple OpenGL texture for the AR camera feed. */
//    private fun createTexture(): Int {
//        val texture = IntArray(1)
//        GLES20.glGenTextures(1, texture, 0)
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0])
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
//        return texture[0]
//    }
//}


// -------------------------------------------------------------------------
//Last Working Code Below:

//package com.solemate.app.solemate_app
//
//import android.opengl.GLES20
//import android.opengl.GLSurfaceView
//import com.google.ar.core.Frame
//import com.google.ar.core.Session
//import com.google.ar.core.exceptions.CameraNotAvailableException
//import javax.microedition.khronos.egl.EGLConfig
//import javax.microedition.khronos.opengles.GL10
//
//class SimpleRenderer(private val session: Session) : GLSurfaceView.Renderer {
//
//    private val backgroundRenderer = BackgroundRenderer()
//
//    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        GLES20.glClearColor(0f, 0f, 0f, 1f)
//        backgroundRenderer.createOnGlThread()
//        session.setCameraTextureName(backgroundRenderer.getTextureId())
//    }
//
//    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//        GLES20.glViewport(0, 0, width, height)
//    }
//
//    override fun onDrawFrame(gl: GL10?) {
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
//        try {
//            val frame: Frame = session.update()
//            backgroundRenderer.draw()
//        } catch (e: CameraNotAvailableException) {
//            e.printStackTrace()
//        }
//    }
//}

//------------------------------------------------------------------------------------------------

//package com.solemate.app.solemate_app
//
//import android.opengl.GLES20
//import android.opengl.GLSurfaceView
//import com.google.ar.core.Frame
//import com.google.ar.core.Session
//import com.google.ar.core.exceptions.CameraNotAvailableException
//import javax.microedition.khronos.egl.EGLConfig
//import javax.microedition.khronos.opengles.GL10
//
///**
// * Handles rendering the AR camera feed with correct orientation and aspect ratio.
// */
//class SimpleRenderer(
//    private val session: Session,
//    private val rotationHelper: DisplayRotationHelper
//    private val planeRenderer = PlaneRenderer()
//) : GLSurfaceView.Renderer {
//
//    private val backgroundRenderer = BackgroundRenderer()
//
//    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        GLES20.glClearColor(0f, 0f, 0f, 1f)
//
//        // Initialize the background texture used to render the camera feed.
//        backgroundRenderer.createOnGlThread()
//        session.setCameraTextureName(backgroundRenderer.getTextureId())
//        planeRenderer.createOnGlThread()
//
//    }
//
//    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//        // Adjust viewport and inform rotation helper.
//        GLES20.glViewport(0, 0, width, height)
//        rotationHelper.onSurfaceChanged(width, height)
//    }
//
//    override fun onDrawFrame(gl: GL10?) {
//        // Clear frame buffers.
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
//
//        // Apply correct display geometry and orientation.
//        rotationHelper.updateSessionIfNeeded(session)
//
//        try {
////            // Get the current frame from ARCore.
////            val frame: Frame = session.update()
////
////            // Draw the camera background texture.
////            backgroundRenderer.draw()
//
//            // Get the latest camera frame.
//            val frame: Frame = session.update()
//
//            // Draw the live camera feed.
//            backgroundRenderer.draw(frame)
//
//            // ✅ Log detected planes (for debugging and verification)
//            val allPlanes = session.getAllTrackables(com.google.ar.core.Plane::class.java)
//            for (plane in allPlanes) {
//                if (plane.trackingState == com.google.ar.core.TrackingState.TRACKING) {
//                    android.util.Log.d(
//                        "ARPlane",
//                        "✅ Plane detected | Center: ${plane.centerPose.translation.contentToString()} | Extent: ${plane.extentX} x ${plane.extentZ}"
//                    )
//                }
//            }
//
//        } catch (e: CameraNotAvailableException) {
//            e.printStackTrace()
//        }
//    }
//}

//---------------------------------------------------------------------------








//package com.solemate.app.solemate_app
//
//import android.opengl.GLES20
//import android.opengl.GLSurfaceView
//import android.util.Log
//import com.google.ar.core.Anchor
//import com.google.ar.core.Frame
//import com.google.ar.core.Plane
//import com.google.ar.core.Session
//import com.google.ar.core.TrackingState
//import com.google.ar.core.exceptions.CameraNotAvailableException
//import javax.microedition.khronos.egl.EGLConfig
//import javax.microedition.khronos.opengles.GL10
//
//
///**
// * Handles rendering the AR camera feed and detected planes.
// */
//
//class SimpleRenderer(
//    private val session: Session,
//    private val rotationHelper: DisplayRotationHelper,
//    private val activity: ARActivity
//) : GLSurfaceView.Renderer {
//
//    private val backgroundRenderer = BackgroundRenderer()
//    private val planeRenderer = PlaneRenderer()
//    private val anchors = mutableListOf<Anchor>()
//
//
//    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//        GLES20.glClearColor(0f, 0f, 0f, 1f)
//
//        // Initialize background (camera feed)
//        backgroundRenderer.createOnGlThread()
//        session.setCameraTextureName(backgroundRenderer.getTextureId())
//
//        // Initialize plane visualization
//        planeRenderer.createOnGlThread()
//
//    }
//
//    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//        GLES20.glViewport(0, 0, width, height)
//        rotationHelper.onSurfaceChanged(width, height)
//    }
//
//    override fun onDrawFrame(gl: GL10?) {
//        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
//        rotationHelper.updateSessionIfNeeded(session)
//
//        try {
//            val frame: Frame = session.update()
//            backgroundRenderer.draw(frame)
//
//            // Build projection/view matrices
//            val projMatrix = FloatArray(16)
//            val viewMatrix = FloatArray(16)
//            frame.camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)
//            frame.camera.getViewMatrix(viewMatrix, 0)
//
//            val viewProjMatrix = FloatArray(16)
//            android.opengl.Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)
//
//            // ✅ Check for tap queued by ARActivity
//            val tap = activity.getQueuedSingleTap()
//            if (tap != null && frame.camera.trackingState == TrackingState.TRACKING) {
//                for (hit in frame.hitTest(tap)) {
//                    val trackable = hit.trackable
//                    if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
//                        anchors.add(hit.createAnchor())
//                        android.util.Log.d("ARAnchor", "✅ Anchor created at ${hit.hitPose.translation.contentToString()}")
//                        break
//                    }
//                }
//            }
//
//            // ✅ Draw all tracked planes (semi-transparent green)
//            val allPlanes = session.getAllTrackables(Plane::class.java)
//            for (plane in allPlanes) {
//                if (plane.trackingState == TrackingState.TRACKING && plane.subsumedBy == null) {
//                    planeRenderer.drawPlane(plane, viewProjMatrix)
//                    Log.d(
//                        "ARPlane",
//                        "✅ Plane detected | Center: ${plane.centerPose.translation.contentToString()} | Extent: ${plane.extentX} x ${plane.extentZ}"
//                    )
//                }
//            }
//
//            // ✅ Visual verification: draw small marker/plane where anchors exist
//            for (anchor in anchors) {
//                if (anchor.trackingState == TrackingState.TRACKING) {
//                    // ✅ Draw a small marker at the anchor position instead of accessing trackable
//                    val pose = anchor.pose
//                    val modelMatrix = FloatArray(16)
//                    pose.toMatrix(modelMatrix, 0)
//
//                    // Reuse planeRenderer for visualization (temporary)
//                    planeRenderer.drawAnchorMarker(modelMatrix, viewProjMatrix)
//                }
//            }
//
//        } catch (e: CameraNotAvailableException) {
//            e.printStackTrace()
//        }
//    }
//
//}





//-------------------------------------------------------------------









package com.solemate.app.solemate_app

import android.graphics.Bitmap
import android.opengl.GLES20
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
    private val session: Session,
    private val rotationHelper: DisplayRotationHelper,
    private val activity: ARActivity,
    private var shoeRenderer: ShoeRenderer? = null
) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()
    private val planeRenderer = PlaneRenderer()
    private val anchors = mutableListOf<Anchor>()

    // ==== Detection infrastructure ====
    private val detectionExecutor = Executors.newSingleThreadExecutor()
    private val detector = FootTracker(activity)
    private val isProcessing = AtomicBoolean(false)
    private var frameCounter = 0
    private val frameSkip = 3 // process every 3rd frame (tune as needed)
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
    private val placementCooldownMs = 100L  // Update placement every 100ms (faster tracking)
    private val soleOffsetMeters = 0.0f  // Model origin is at sole center, no Y offset needed
    private val baseModelScale = 0.05f  // Base scale for the shoe model (5% of original size)
    private val minFootLength = 0.08f   // Minimum realistic foot length (8cm - projected on ground)
    private val maxFootLength = 0.50f   // Maximum realistic foot length (50cm - generous for angles)
    private val referenceFootLength = 0.26f  // Average adult foot length (26cm)
    private val maxHitDistance = 3.0f   // Maximum distance for valid hit-test (3 meters)
    private val minVisibility = 0.3f    // Minimum landmark visibility threshold
    
    // ==== Temporal Smoothing ====
    private val smoothingAlpha = 0.3f  // EMA smoothing factor (0.0=max smooth, 1.0=no smooth)
    private var smoothedPosition: FloatArray? = null  // [x, y, z]
    private var smoothedYaw: Float? = null  // radians
    private val minMovementThreshold = 0.01f  // 1cm - ignore tiny movements
    private val minRotationThreshold = 0.05f  // ~3 degrees - ignore tiny rotations
    
    // Recalibration request flag
    @Volatile
    private var recalibrationRequested = false

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

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        backgroundRenderer.createOnGlThread()
        session.setCameraTextureName(backgroundRenderer.getTextureId())
        planeRenderer.createOnGlThread()
        detector.init()
        // ShoeRenderer will be fully initialized when we know the surface size (onSurfaceChanged)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        rotationHelper.onSurfaceChanged(width, height)
        // ShoeRenderer is now managed by ARActivity via TextureView
        // No initialization needed here
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        rotationHelper.updateSessionIfNeeded(session)

        try {
            val frame: Frame = session.update()
            backgroundRenderer.draw(frame)

            // Matrices for drawing
            val projMatrix = FloatArray(16)
            val viewMatrix = FloatArray(16)
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

            // === Acquire camera image for ML (throttled, safe) ===
            frameCounter++
            if (frameCounter % frameSkip == 0 && isProcessing.compareAndSet(false, true)) {
                try {
                    val cameraImage = frame.acquireCameraImage() // MAY throw NotYetAvailableException
                    // Submit to background thread for detection; MUST close image
                    detectionExecutor.submit {
                        try {
                            val bitmap = YuvConverter.imageToBitmap(cameraImage)
                            // Run detection (replace with MediaPipe / TFLite inference)
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
                } catch (nye: NotYetAvailableException) {
                    // Camera image not available for this frame - clear processing flag
                    isProcessing.set(false)
                } catch (e: Exception) {
                    Log.e("SimpleRenderer", "acquireCameraImage failed: ${e.message}")
                    isProcessing.set(false)
                }
            }

            // === Handle recalibration request ===
            if (recalibrationRequested) {
                recalibrationRequested = false
                shoePlaced = false
                lastPlacementTime = 0L
                // Clear smoothing buffers
                smoothedPosition = null
                smoothedYaw = null
                // Clear old anchors
                anchors.forEach { it.detach() }
                anchors.clear()
                Log.d("SimpleRenderer", "✅ Recalibration: Reset placement state and smoothing")
            }
            
            // === If we have a pending foot point (normalized coords), transform to VIEW coords and hitTest ===
            pendingFootPoint.getAndSet(null)?.let { fp ->
                try {
                    if (frame.camera.trackingState == TrackingState.TRACKING) {
                        // Check if enough time has passed since last placement (avoid spam)
                        val currentTime = System.currentTimeMillis()
                        val canUpdate = !shoePlaced || (currentTime - lastPlacementTime) > placementCooldownMs
                        
                        // Validate visibility threshold
                        if (fp.visibility < minVisibility) {
                            Log.d("FootAnchor", "⚠️ Low visibility ${fp.visibility} < $minVisibility, skipping")
                            return@let
                        }
                        
                        if (!canUpdate) {
                            return@let  // Skip this update due to cooldown
                        }
                        // Helper function to convert normalized coords to pixels then to view coords
                        fun normalizedToView(normX: Float, normY: Float, bias: Float = 0f): Pair<Float, Float>? {
                            val srcX = (normX * fp.imgWidth).coerceIn(0f, fp.imgWidth - 1f)
                            val srcY = ((normY + bias) * fp.imgHeight).coerceIn(0f, fp.imgHeight - 1f)
                            
                            val inBuf = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                            inBuf.put(srcX)
                            inBuf.put(srcY)
                            inBuf.position(0)
                            
                            val outBuf = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                            frame.transformCoordinates2d(Coordinates2d.IMAGE_PIXELS, inBuf, Coordinates2d.VIEW, outBuf)
                            
                            outBuf.position(0)
                            return Pair(outBuf.get(0), outBuf.get(1))
                        }
                        
                        // Helper to perform hit-test and return best hit with distance validation
                        fun hitTestPoint(viewX: Float, viewY: Float, label: String): com.google.ar.core.HitResult? {
                            var bestHit: com.google.ar.core.HitResult? = null
                            var bestPref = -1
                            
                            // Validate view coordinates are within screen bounds
                            if (viewX < 0 || viewY < 0) {
                                Log.w("FootAnchor", "❌ Invalid view coords for $label: ($viewX, $viewY)")
                                return null
                            }
                            
                            for (hit in frame.hitTest(viewX, viewY)) {
                                val tr = hit.trackable
                                
                                // Validate distance - reject hits too far away
                                if (hit.distance > maxHitDistance) {
                                    Log.d("FootAnchor", "⚠️ $label hit too far: ${hit.distance}m > ${maxHitDistance}m")
                                    continue
                                }
                                
                                val pref = when (tr) {
                                    is com.google.ar.core.Plane -> 3
                                    is com.google.ar.core.Point -> 2
                                    is com.google.ar.core.DepthPoint -> 1
                                    else -> 0
                                }
                                val ok = when (tr) {
                                    is com.google.ar.core.Plane -> tr.isPoseInPolygon(hit.hitPose) && tr.trackingState == TrackingState.TRACKING
                                    else -> true
                                }
                                if (ok && (pref > bestPref || (pref == bestPref && bestHit != null && hit.distance < bestHit!!.distance))) {
                                    bestHit = hit
                                    bestPref = pref
                                }
                            }
                            return bestHit
                        }
                        
                        // Hit-test ankle, heel, and toe positions to get 3D world coordinates
                        Log.d("FootAnchor", "Processing foot: side=${fp.side} vis=${fp.visibility} ankle=(${fp.ankleX},${fp.ankleY}) toe=(${fp.toeX},${fp.toeY}) heel=(${fp.heelX},${fp.heelY})")
                        
                        val ankleView = normalizedToView(fp.ankleX, fp.ankleY)
                        val toeView = if (fp.toeX != null && fp.toeY != null) {
                            normalizedToView(fp.toeX, fp.toeY, 0.005f) // small downward bias
                        } else null
                        val heelView = if (fp.heelX != null && fp.heelY != null) {
                            normalizedToView(fp.heelX, fp.heelY, 0.005f) // small downward bias
                        } else null
                        
                        Log.d("FootAnchor", "Converted to view coords: ankle=$ankleView toe=$toeView heel=$heelView")
                        
                        val ankleHit = ankleView?.let { (vx, vy) -> hitTestPoint(vx, vy, "ankle") }
                        val toeHit = toeView?.let { (vx, vy) -> hitTestPoint(vx, vy, "toe") }
                        val heelHit = heelView?.let { (vx, vy) -> hitTestPoint(vx, vy, "heel") }
                        
                        Log.d("FootAnchor", "Hit test results: ankleHit=${ankleHit != null} toeHit=${toeHit != null} heelHit=${heelHit != null}")
                        
                        // For anchor creation, prefer heel (closest to ground), fall back to toe, then ankle
                        val primaryHit = heelHit ?: toeHit ?: ankleHit
                        
                        primaryHit?.let { chosen ->
                            // Validate that landmark hits are reasonably close to each other
                            if (heelHit != null && toeHit != null) {
                                val distanceBetween = kotlin.math.abs(heelHit.distance - toeHit.distance)
                                if (distanceBetween > 0.5f) {
                                    Log.w("FootAnchor", "❌ Heel/toe distance mismatch: ${distanceBetween}m, skipping")
                                    return@let
                                }
                            }
                            
                            // Clear old anchors to avoid memory leak
                            if (shoePlaced) {
                                anchors.forEach { it.detach() }
                                anchors.clear()
                            }
                            
                            anchors.add(chosen.createAnchor())
                            val tr = chosen.trackable
                            val typeName = when (tr) {
                                is com.google.ar.core.Plane -> "Plane"
                                is com.google.ar.core.Point -> "Point"
                                is com.google.ar.core.DepthPoint -> "DepthPoint"
                                else -> "Other"
                            }
                            
                            // Build the model matrix using available landmarks
                            // Best case: heel + toe for direction and position
                            // Fallback: ankle + toe, or single point
                            
                            val hasHeelAndToe = heelHit != null && toeHit != null
                            val hasAnkleAndToe = ankleHit != null && toeHit != null
                            
                            if (hasHeelAndToe || hasAnkleAndToe) {
                                // Use heel→toe for direction (or ankle→toe as fallback)
                                val backPos = if (hasHeelAndToe) heelHit!!.hitPose.translation else ankleHit!!.hitPose.translation
                                val frontPos = toeHit!!.hitPose.translation
                                
                                // Calculate foot direction vector (from back to front)
                                val dx = frontPos[0] - backPos[0]
                                val dy = frontPos[1] - backPos[1]
                                val dz = frontPos[2] - backPos[2]
                                
                                // Calculate foot length in XZ plane (horizontal projection)
                                val footLengthXZ = kotlin.math.sqrt(dx * dx + dz * dz)
                                // Full 3D length
                                val footLength3D = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
                                
                                // Check if foot length is realistic
                                val isRealisticLength = footLengthXZ >= minFootLength && footLengthXZ <= maxFootLength
                                
                                // Calculate foot yaw (rotation around Y axis)
                                // atan2(dx, dz) gives angle from +Z axis
                                val rawYaw = kotlin.math.atan2(dx, dz)
                                
                                // Calculate scale based on foot length
                                val footScale = if (isRealisticLength) {
                                    (footLengthXZ / referenceFootLength).coerceIn(0.7f, 1.3f)
                                } else {
                                    Log.w("FootAnchor", "Unrealistic foot length ${footLengthXZ}m, using 1.0x scale")
                                    1.0f
                                }
                                
                                // Calculate shoe position: midpoint between heel and toe (sole center)
                                val rawPosition = floatArrayOf(
                                    (backPos[0] + frontPos[0]) / 2f,
                                    (backPos[1] + frontPos[1]) / 2f + soleOffsetMeters,
                                    (backPos[2] + frontPos[2]) / 2f
                                )
                                
                                // ==== Apply Temporal Smoothing ====
                                val finalPosition: FloatArray
                                val finalYaw: Float
                                
                                if (smoothedPosition != null && smoothedYaw != null) {
                                    // Calculate movement distance
                                    val moveDist = kotlin.math.sqrt(
                                        (rawPosition[0] - smoothedPosition!![0]).let { it * it } +
                                        (rawPosition[1] - smoothedPosition!![1]).let { it * it } +
                                        (rawPosition[2] - smoothedPosition!![2]).let { it * it }
                                    )
                                    
                                    // Calculate rotation difference
                                    var yawDiff = rawYaw - smoothedYaw!!
                                    // Normalize to [-PI, PI]
                                    while (yawDiff > kotlin.math.PI) yawDiff -= (2 * kotlin.math.PI).toFloat()
                                    while (yawDiff < -kotlin.math.PI) yawDiff += (2 * kotlin.math.PI).toFloat()
                                    
                                    // Only update if movement exceeds threshold (hysteresis)
                                    if (moveDist > minMovementThreshold || kotlin.math.abs(yawDiff) > minRotationThreshold) {
                                        // Apply EMA smoothing
                                        finalPosition = floatArrayOf(
                                            smoothedPosition!![0] + smoothingAlpha * (rawPosition[0] - smoothedPosition!![0]),
                                            smoothedPosition!![1] + smoothingAlpha * (rawPosition[1] - smoothedPosition!![1]),
                                            smoothedPosition!![2] + smoothingAlpha * (rawPosition[2] - smoothedPosition!![2])
                                        )
                                        finalYaw = smoothedYaw!! + smoothingAlpha * yawDiff
                                        
                                        // Update smoothed values
                                        smoothedPosition = finalPosition.copyOf()
                                        smoothedYaw = finalYaw
                                    } else {
                                        // Use existing smoothed values (no update)
                                        finalPosition = smoothedPosition!!
                                        finalYaw = smoothedYaw!!
                                    }
                                } else {
                                    // First detection - initialize smoothed values
                                    finalPosition = rawPosition
                                    finalYaw = rawYaw
                                    smoothedPosition = rawPosition.copyOf()
                                    smoothedYaw = rawYaw
                                }
                                
                                // ==== Build Model Matrix with Correct Coordinate System ====
                                // Model coordinate system (from Blender image):
                                //   - Origin: center of sole
                                //   - -Y axis: points toward toe
                                //   - +Z axis: points upward
                                // 
                                // ARCore coordinate system:
                                //   - +Y axis: points upward
                                //   - -Z axis: typically forward (but we use footYaw to orient)
                                //
                                // To align model to ARCore:
                                // 1. Rotate 90° around X to convert model's +Z-up to ARCore's +Y-up
                                // 2. The model's -Y (toe direction) becomes -Z after this rotation
                                // 3. Apply yaw rotation around Y to orient toe direction
                                
                                val shoeMatrix = FloatArray(16)
                                android.opengl.Matrix.setIdentityM(shoeMatrix, 0)
                                
                                // Step 1: Translate to final position (sole center)
                                android.opengl.Matrix.translateM(shoeMatrix, 0, 
                                    finalPosition[0], finalPosition[1], finalPosition[2])
                                
                                // Step 2: Apply yaw rotation (foot direction)
                                // We need to rotate the model so its toe points in the foot direction
                                // Since after the X rotation, model's -Y becomes -Z,
                                // and ARCore's -Z is forward, we need footYaw + 180° to flip
                                val yawDegrees = Math.toDegrees(finalYaw.toDouble()).toFloat()
                                android.opengl.Matrix.rotateM(shoeMatrix, 0, yawDegrees, 0f, 1f, 0f)
                                
                                // Step 3: Apply axis correction rotation
                                // Rotate 90° around X to convert model +Z-up to ARCore +Y-up
                                // This makes model's original Y-axis align with ARCore's -Z (forward)
                                android.opengl.Matrix.rotateM(shoeMatrix, 0, -90f, 1f, 0f, 0f)
                                
                                // Step 4: Apply scale
                                val finalScale = baseModelScale * footScale
                                android.opengl.Matrix.scaleM(shoeMatrix, 0, finalScale, finalScale, finalScale)
                                
                                shoeModelMatrix = shoeMatrix
                                shoePlaced = true
                                lastPlacementTime = currentTime
                                
                                Log.d(
                                    "FootAnchor",
                                    "✅ Placed($typeName) side=${fp.side} " +
                                    "footLen=${"%.3f".format(footLengthXZ)}m (${if(isRealisticLength) "OK" else "??"}) " +
                                    "scale=${"%.3f".format(finalScale)} " +
                                    "yaw=${"%.1f".format(Math.toDegrees(finalYaw.toDouble()))}° " +
                                    "pos=[${"%.2f".format(finalPosition[0])}, ${"%.2f".format(finalPosition[1])}, ${"%.2f".format(finalPosition[2])}]"
                                )
                            } else {
                                // Fallback: simple placement if only one point available
                                val anchorM = FloatArray(16)
                                chosen.hitPose.toMatrix(anchorM, 0)
                                
                                // Apply axis correction even for single-point placement
                                android.opengl.Matrix.rotateM(anchorM, 0, -90f, 1f, 0f, 0f)
                                
                                // Apply base scale
                                android.opengl.Matrix.scaleM(anchorM, 0, baseModelScale, baseModelScale, baseModelScale)
                                shoeModelMatrix = anchorM
                                shoePlaced = true
                                lastPlacementTime = currentTime
                                
                                Log.d(
                                    "FootAnchor",
                                    "✅ Simple placement($typeName) at ${chosen.hitPose.translation.contentToString()}"
                                )
                            }
                        }
                        
                        if (primaryHit == null) {
                            val planes = session.getAllTrackables(Plane::class.java)
                            val trackingPlanes = planes.count { it.trackingState == TrackingState.TRACKING }
                            Log.d(
                                "FootAnchor",
                                "ℹ️ No hit for ${fp.side} foot; trackingPlanes=$trackingPlanes"
                            )
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("SimpleRenderer", "Foot hitTest failed: ${t.message}")
                }
            }

            // ==== Update shoe camera/model matrices (Filament renders on its own thread) ====
            try {
                shoeRenderer?.let { sr ->
                    // Only update matrices - TextureView handles rendering independently
                    sr.setCamera(viewMatrix, projMatrix)
                    shoeModelMatrix?.let { mm -> 
                        sr.setModelMatrix(mm)
                        if (frameCounter % 60 == 0) { // Log every 60 frames (~2 seconds)
                            Log.d("ShoeUpdate", "Sending matrix to renderer: pos=[${mm[12]}, ${mm[13]}, ${mm[14]}]")
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e("ShoeUpdate", "Failed to update matrices: ${t.message}")
            }

        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
            // Consider notifying user
        }
    }
}
