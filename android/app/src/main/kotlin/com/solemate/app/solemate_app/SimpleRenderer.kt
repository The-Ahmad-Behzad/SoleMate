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
    private val placementCooldownMs = 500L  // Update placement every 500ms max
    private val soleOffsetMeters = 0.02f
    private val baseModelScale = 0.05f  // Base scale for the shoe model (5% of original size)
    private val minFootLength = 0.15f   // Minimum realistic foot length (15cm)
    private val maxFootLength = 0.35f   // Maximum realistic foot length (35cm)
    private val referenceFootLength = 0.26f  // Average adult foot length (26cm)
    private val maxHitDistance = 3.0f   // Maximum distance for valid hit-test (3 meters)
    private val minVisibility = 0.3f    // Minimum landmark visibility threshold
    
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
                // Clear old anchors
                anchors.forEach { it.detach() }
                anchors.clear()
                Log.d("SimpleRenderer", "✅ Recalibration: Reset placement state")
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
                            
                            // Calculate foot dimensions and orientation if we have both ankle and toe
                            if (ankleHit != null && toeHit != null) {
                                val anklePos = ankleHit.hitPose.translation
                                val toePos = toeHit.hitPose.translation
                                
                                // Calculate foot length in 3D (XZ plane)
                                val dx = toePos[0] - anklePos[0]
                                val dz = toePos[2] - anklePos[2]
                                val dy = toePos[1] - anklePos[1]
                                val footLengthMeters = kotlin.math.sqrt(dx * dx + dz * dz)
                                
                                // Check if foot length is realistic
                                val isRealisticLength = footLengthMeters >= minFootLength && footLengthMeters <= maxFootLength
                                
                                // Calculate foot yaw (direction from ankle to toe)
                                val footYaw = kotlin.math.atan2(dx, dz) // radians
                                
                                // Calculate scale based on foot length
                                // If unrealistic, use 1.0x (assume reference size)
                                val footScale = if (isRealisticLength) {
                                    (footLengthMeters / referenceFootLength).coerceIn(0.7f, 1.3f)
                                } else {
                                    Log.w("FootAnchor", "Unrealistic foot length ${footLengthMeters}m, using 1.0x scale")
                                    1.0f
                                }
                                
                                // Build model matrix with correct transformation order:
                                // 1. Start at toe anchor position
                                // 2. Rotate to match foot orientation
                                // 3. Translate in the rotated coordinate system (up + back toward heel)
                                // 4. Scale uniformly
                                
                                val shoeMatrix = FloatArray(16)
                                chosen.hitPose.toMatrix(shoeMatrix, 0)
                                
                                // First, rotate to match foot orientation (around toe position)
                                android.opengl.Matrix.rotateM(shoeMatrix, 0, Math.toDegrees(footYaw.toDouble()).toFloat(), 0f, 1f, 0f)
                                
                                // Now translate in the ROTATED coordinate system:
                                // - Y-up to lift shoe above floor
                                // - Z-back to move shoe origin from toe toward heel (40% of foot length)
                                val backOffset = footLengthMeters * 0.4f
                                android.opengl.Matrix.translateM(shoeMatrix, 0, 0f, soleOffsetMeters, -backOffset)
                                
                                // Finally, apply scale: base model scale * foot-size scale
                                val finalScale = baseModelScale * footScale
                                android.opengl.Matrix.scaleM(shoeMatrix, 0, finalScale, finalScale, finalScale)
                                
                                shoeModelMatrix = shoeMatrix
                                shoePlaced = true
                                lastPlacementTime = currentTime
                                
                                Log.d(
                                    "FootAnchor",
                                    "✅ Anchor($typeName, d=${"%.2f".format(chosen.distance)}) side=${fp.side} " +
                                    "footLen=${"%.3f".format(footLengthMeters)}m (${if(isRealisticLength) "OK" else "BAD"}) " +
                                    "footScale=${"%.2f".format(footScale)} baseScale=$baseModelScale finalScale=${"%.4f".format(finalScale)} " +
                                    "yaw=${"%.1f".format(Math.toDegrees(footYaw.toDouble()))}° " +
                                    "ankle=${anklePos.contentToString()} toe=${toePos.contentToString()}"
                                )
                                
                                // Debug: Log the model matrix values
                                Log.d("FootAnchor", "Model matrix set: [${shoeMatrix[12]}, ${shoeMatrix[13]}, ${shoeMatrix[14]}] (translation)")
                            } else {
                                // Fallback: simple placement if only one point available
                                val anchorM = FloatArray(16)
                                chosen.hitPose.toMatrix(anchorM, 0)
                                android.opengl.Matrix.translateM(anchorM, 0, 0f, soleOffsetMeters, 0f)
                                // Apply base scale
                                android.opengl.Matrix.scaleM(anchorM, 0, baseModelScale, baseModelScale, baseModelScale)
                                shoeModelMatrix = anchorM
                                shoePlaced = true
                                lastPlacementTime = currentTime
                                
                                Log.d(
                                    "FootAnchor",
                                    "✅ Anchor($typeName, d=${"%.2f".format(chosen.distance)}) at ${chosen.hitPose.translation.contentToString()} (simple placement, single point)"
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
