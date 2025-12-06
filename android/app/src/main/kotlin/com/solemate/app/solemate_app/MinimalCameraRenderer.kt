package com.solemate.app.solemate_app

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLES11Ext
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Minimal camera renderer for testing - ONLY renders camera preview.
 * No pose tracking, no planes, no objects - just camera feed.
 * Used to verify camera pipeline works before adding AR features.
 */
class MinimalCameraRenderer(
    private val activityWrapper: ARActivityWrapper
) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "MinimalCameraRenderer"
    }

    private val backgroundRenderer = BackgroundRenderer()
    private var cameraTextureId = -1
    private var surfaceTexture: SurfaceTexture? = null
    private var cameraReady = false
    private var cameraInitStartTime = 0L
    private val cameraInitTimeoutMs = 5000L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated() called")
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1f)  // Dark gray background (so we can see if camera isn't working)
        
        // Initialize background renderer
        backgroundRenderer.createOnGlThread()
        Log.d(TAG, "BackgroundRenderer created, textureId=${backgroundRenderer.getTextureId()}")
        
        // Create external OES texture for SurfaceTexture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        cameraTextureId = textures[0]
        Log.d(TAG, "Created camera texture ID: $cameraTextureId")
        
        // Bind and configure texture as external OES (required for SurfaceTexture)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        Log.d(TAG, "Texture configured as GL_TEXTURE_EXTERNAL_OES")
        
        // Create SurfaceTexture with the texture ID
        try {
            surfaceTexture = SurfaceTexture(cameraTextureId)
            Log.d(TAG, "SurfaceTexture created with textureId=$cameraTextureId")
            
            // Set default buffer size (640x480 for testing)
            surfaceTexture?.setDefaultBufferSize(640, 480)
            Log.d(TAG, "SurfaceTexture buffer size set to 640x480")
            
            // Set frame available listener
            surfaceTexture?.setOnFrameAvailableListener { texture ->
                Log.d(TAG, "Frame available callback triggered")
                // Request render on GL thread
                activityWrapper.requestRender()
            }
            Log.d(TAG, "Frame available listener set")
            
            // Store in activity so camera manager can access it
            activityWrapper.setCameraSurfaceTexture(surfaceTexture!!)
            
            // Open camera
            cameraInitStartTime = System.currentTimeMillis()
            activityWrapper.openVioCamera()
            Log.d(TAG, "Camera open requested")
            cameraReady = false
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SurfaceTexture: ${e.message}", e)
            cameraReady = false
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged() called: ${width}x${height}")
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear with dark gray so we can see if camera isn't rendering
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        // Check for camera initialization timeout
        if (cameraInitStartTime > 0 && !cameraReady) {
            val elapsed = System.currentTimeMillis() - cameraInitStartTime
            if (elapsed > cameraInitTimeoutMs) {
                Log.e(TAG, "Camera initialization timeout after ${elapsed}ms")
                cameraInitStartTime = 0L
            } else if (elapsed % 1000 == 0L) {
                Log.d(TAG, "Waiting for camera... (${elapsed}ms elapsed)")
            }
        }
        
        // Skip drawing if camera is not ready
        if (!cameraReady) {
            return
        }
        
        // Update texture with latest camera frame (MUST be on GL thread)
        surfaceTexture?.let { texture ->
            try {
                texture.updateTexImage()
                
                // Mark camera as ready on first successful frame update
                if (!cameraReady && cameraInitStartTime > 0) {
                    val elapsed = System.currentTimeMillis() - cameraInitStartTime
                    Log.d(TAG, "✅ Camera ready! First frame received after ${elapsed}ms")
                    cameraReady = true
                    cameraInitStartTime = 0L
                }
                
                // Get transform matrix from SurfaceTexture
                val matrix = FloatArray(16)
                texture.getTransformMatrix(matrix)
                
                // Draw camera feed
                backgroundRenderer.drawCamera2Texture(cameraTextureId, matrix)
                Log.v(TAG, "Camera frame drawn")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating SurfaceTexture: ${e.message}", e)
            }
        } ?: run {
            Log.w(TAG, "SurfaceTexture is null, cannot draw")
        }
    }
    
    fun release() {
        Log.d(TAG, "release() called")
        surfaceTexture?.release()
        surfaceTexture = null
    }
    
    fun getSurfaceTexture(): SurfaceTexture? {
        return surfaceTexture
    }
}

/**
 * Interface for activities that work with MinimalCameraRenderer.
 */
interface ARActivityWrapper {
    fun setCameraSurfaceTexture(texture: android.graphics.SurfaceTexture)
    fun getCameraSurfaceTexture(): android.graphics.SurfaceTexture?
    fun openVioCamera()
    fun requestRender()
}

