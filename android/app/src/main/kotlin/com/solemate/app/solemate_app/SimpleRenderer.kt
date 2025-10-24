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








package com.solemate.app.solemate_app

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


/**
 * Handles rendering the AR camera feed and detected planes.
 */
class SimpleRenderer(
    private val session: Session,
    private val rotationHelper: DisplayRotationHelper,
    private val activity: ARActivity
) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()
    private val planeRenderer = PlaneRenderer()
    private val anchors = mutableListOf<Anchor>()


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        // Initialize background (camera feed)
        backgroundRenderer.createOnGlThread()
        session.setCameraTextureName(backgroundRenderer.getTextureId())

        // Initialize plane visualization
        planeRenderer.createOnGlThread()
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

            // Build projection/view matrices
            val projMatrix = FloatArray(16)
            val viewMatrix = FloatArray(16)
            frame.camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)
            frame.camera.getViewMatrix(viewMatrix, 0)

            val viewProjMatrix = FloatArray(16)
            android.opengl.Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

            // ✅ Check for tap queued by ARActivity
            val tap = activity.getQueuedSingleTap()
            if (tap != null && frame.camera.trackingState == TrackingState.TRACKING) {
                for (hit in frame.hitTest(tap)) {
                    val trackable = hit.trackable
                    if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                        anchors.add(hit.createAnchor())
                        android.util.Log.d("ARAnchor", "✅ Anchor created at ${hit.hitPose.translation.contentToString()}")
                        break
                    }
                }
            }

            // ✅ Draw all tracked planes (semi-transparent green)
            val allPlanes = session.getAllTrackables(Plane::class.java)
            for (plane in allPlanes) {
                if (plane.trackingState == TrackingState.TRACKING && plane.subsumedBy == null) {
                    planeRenderer.drawPlane(plane, viewProjMatrix)
                    Log.d(
                        "ARPlane",
                        "✅ Plane detected | Center: ${plane.centerPose.translation.contentToString()} | Extent: ${plane.extentX} x ${plane.extentZ}"
                    )
                }
            }

            // ✅ Visual verification: draw small marker/plane where anchors exist
            for (anchor in anchors) {
                if (anchor.trackingState == TrackingState.TRACKING) {
                    // ✅ Draw a small marker at the anchor position instead of accessing trackable
                    val pose = anchor.pose
                    val modelMatrix = FloatArray(16)
                    pose.toMatrix(modelMatrix, 0)

                    // Reuse planeRenderer for visualization (temporary)
                    planeRenderer.drawAnchorMarker(modelMatrix, viewProjMatrix)
                }
            }

        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
        }
    }

}

