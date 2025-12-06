package com.solemate.app.solemate_app

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.opengl.GLSurfaceView

/**
 * Test activity for camera preview ONLY.
 * No AR features, no tracking - just camera feed to verify pipeline works.
 */
class CameraTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraTestActivity"
        private const val REQUEST_CAMERA_PERMISSION = 2001
    }

    private var glSurfaceView: GLSurfaceView? = null
    private var renderer: MinimalCameraRenderer? = null
    private var simpleCameraManager: SimpleCameraManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        
        // Check camera permissions
        if (!checkCameraPermission()) {
            Log.w(TAG, "Camera permission not granted, requesting...")
            requestCameraPermission()
            return
        }
        
        initializeCamera()
    }
    
    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        val granted = result == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Camera permission check: $granted")
        return granted
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted by user")
                initializeCamera()
            } else {
                Log.e(TAG, "Camera permission denied by user")
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun initializeCamera() {
        Log.d(TAG, "Initializing camera test...")
        
        // Create GLSurfaceView
        glSurfaceView = GLSurfaceView(this)
        glSurfaceView?.setEGLContextClientVersion(2)
        
        // Create wrapper activity for MinimalCameraRenderer
        val wrapperActivity = object : ARActivityWrapper {
            override fun setCameraSurfaceTexture(texture: android.graphics.SurfaceTexture) {
                // Store in test activity
                Log.d(TAG, "SurfaceTexture set")
            }
            
            override fun getCameraSurfaceTexture(): android.graphics.SurfaceTexture? {
                return null  // Will be set by renderer
            }
            
            override fun openVioCamera() {
                Log.d(TAG, "openVioCamera() called")
                simpleCameraManager?.let { manager ->
                    val texture = (renderer as? MinimalCameraRenderer)?.getSurfaceTexture()
                    texture?.let {
                        try {
                            manager.openCamera(it)
                            Log.d(TAG, "Camera opened successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error opening camera: ${e.message}", e)
                            Toast.makeText(this@CameraTestActivity, "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            
            override fun requestRender() {
                glSurfaceView?.requestRender()
            }
        }
        
        // Create MinimalCameraRenderer
        renderer = MinimalCameraRenderer(wrapperActivity)
        
        // Initialize camera manager
        simpleCameraManager = SimpleCameraManager(this)
        simpleCameraManager?.initialize()
        
        // Set renderer
        glSurfaceView?.setRenderer(renderer)
        glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        
        setContentView(glSurfaceView)
        Log.d(TAG, "Camera test activity initialized")
    }
    
    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
        simpleCameraManager?.startCapture()
    }
    
    override fun onPause() {
        super.onPause()
        glSurfaceView?.onPause()
        simpleCameraManager?.stopCapture()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        renderer?.release()
        simpleCameraManager?.release()
    }
}


