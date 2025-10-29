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
    private val activity: ARActivity
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
        val xPx: Int,
        val yPx: Int,
        val toeXPx: Int?,
        val toeYPx: Int?
    )
    private val pendingFootPoint = AtomicReference<FootPoint?>(null)

    fun release() {
        try {
            detector.close()
        } catch (_: Exception) { }
        try {
            detectionExecutor.shutdownNow()
        } catch (_: Exception) { }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        backgroundRenderer.createOnGlThread()
        session.setCameraTextureName(backgroundRenderer.getTextureId())
        planeRenderer.createOnGlThread()
        detector.init()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        rotationHelper.onSurfaceChanged(width, height)
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
                                        imgWidth = bitmap.width,
                                        imgHeight = bitmap.height,
                                        xPx = footResult.xPx,
                                        yPx = footResult.yPx,
                                        toeXPx = footResult.toeXPx,
                                        toeYPx = footResult.toeYPx
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

            // === If we have a pending foot point (in IMAGE pixels), transform to VIEW coords and hitTest ===
            pendingFootPoint.getAndSet(null)?.let { fp ->
                try {
                    if (frame.camera.trackingState == TrackingState.TRACKING) {
                        // Prepare direct buffers for ARCore transform
                        val inBuf: FloatBuffer = ByteBuffer.allocateDirect(2 * 4)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer()
                        // Prefer toe tip if available; otherwise use ankle. Apply small downward bias to favor floor.
                        val srcX = (fp.toeXPx ?: fp.xPx).toFloat()
                        val srcY = ((fp.toeYPx ?: fp.yPx) + maxOf(1, (fp.imgHeight * 0.005f).toInt())).toFloat()
                        inBuf.put(srcX)
                        inBuf.put(srcY)
                        inBuf.position(0)

                        val outBuf: FloatBuffer = ByteBuffer.allocateDirect(2 * 4)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer()

                        frame.transformCoordinates2d(
                            Coordinates2d.IMAGE_PIXELS,
                            inBuf,
                            Coordinates2d.VIEW,
                            outBuf
                        )

                        outBuf.position(0)
                        val viewX = outBuf.get(0)
                        val viewY = outBuf.get(1)

                        var created = false
                        var bestHit: com.google.ar.core.HitResult? = null
                        var bestPref = -1
                        for (hit in frame.hitTest(viewX, viewY)) {
                            val tr = hit.trackable
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
                        bestHit?.let { chosen ->
                            // For planes, ensure pose is inside polygon to avoid backfaces
                            val tr = chosen.trackable
                            val ok = when (tr) {
                                is com.google.ar.core.Plane -> tr.isPoseInPolygon(chosen.hitPose)
                                else -> true
                            }
                            if (ok) {
                                anchors.add(chosen.createAnchor())
                                val typeName = when (tr) {
                                    is com.google.ar.core.Plane -> "Plane"
                                    is com.google.ar.core.Point -> "Point"
                                    is com.google.ar.core.DepthPoint -> "DepthPoint"
                                    else -> "Other"
                                }
                                Log.d(
                                    "FootAnchor",
                                    "✅ Anchor($typeName, d=${"%.2f".format(chosen.distance)}) at ${chosen.hitPose.translation.contentToString()} from img(${fp.xPx},${fp.yPx}) toe(${fp.toeXPx},${fp.toeYPx}) -> view(${"%.1f".format(viewX)},${"%.1f".format(viewY)})"
                                )
                                created = true
                            }
                        }

                        if (!created) {
                            val planes = session.getAllTrackables(Plane::class.java)
                            val trackingPlanes = planes.count { it.trackingState == TrackingState.TRACKING }
                            Log.d(
                                "FootAnchor",
                                "ℹ️ No hit; img=(${fp.imgWidth}x${fp.imgHeight}) src=(${srcX.toInt()},${srcY.toInt()}) -> view=(${"%.1f".format(viewX)},${"%.1f".format(viewY)}), trackingPlanes=$trackingPlanes"
                            )
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("SimpleRenderer", "Foot hitTest failed: ${t.message}")
                }
            }

        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
            // Consider notifying user
        }
    }
}
