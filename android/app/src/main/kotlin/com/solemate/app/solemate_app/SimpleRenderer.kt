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

package com.solemate.app.solemate_app

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SimpleRenderer(private val session: Session) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        backgroundRenderer.createOnGlThread()
        session.setCameraTextureName(backgroundRenderer.getTextureId())
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        try {
            val frame: Frame = session.update()
            backgroundRenderer.draw()
        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
        }
    }
}
