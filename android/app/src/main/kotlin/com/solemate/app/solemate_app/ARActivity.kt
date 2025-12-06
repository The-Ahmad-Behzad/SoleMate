//package com.solemate.app.solemate_app
//
//import android.os.Bundle
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.google.ar.core.ArCoreApk
//import com.google.ar.core.Session
//
//class ARActivity : AppCompatActivity() {
//    private var arSession: Session? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(android.R.layout.simple_list_item_1) // temporary placeholder layout
//
//        try {
//            // Check ARCore availability
//            val availability = ArCoreApk.getInstance().checkAvailability(this)
//            if (availability.isSupported) {
//                arSession = Session(this)
//                Toast.makeText(this, "AR Session Started", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "ARCore not supported", Toast.LENGTH_LONG).show()
//                finish()
//            }
//        } catch (e: Exception) {
//            Toast.makeText(this, "Error initializing ARCore: ${e.message}", Toast.LENGTH_LONG).show()
//            finish()
//        }
//    }
//
//    override fun onPause() {
//        super.onPause()
//        arSession?.pause()
//    }
//
//    override fun onResume() {
//        super.onResume()
//        arSession?.resume()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        arSession?.close()
//    }
//}


// --------------------------------------------------------------------------------------------
// V2 of last working state without camera rendering
//package com.solemate.app.solemate_app
//
//import android.app.Activity
//import android.os.Bundle
//import android.util.Log
//import android.view.SurfaceHolder
//import android.view.SurfaceView
//import android.widget.Toast
//import com.google.ar.core.*
//import com.google.ar.core.ArCoreApk
//import com.google.ar.core.Session
//import com.google.ar.core.exceptions.CameraNotAvailableException
//import com.google.ar.core.exceptions.UnavailableException
//import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
//
//class ARActivity : Activity() {
//
//    private var surfaceView: SurfaceView? = null
//    private var session: Session? = null
//    private var installRequested = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        Log.d("SoleMateAR", "Opened AR View")
//
//        surfaceView = SurfaceView(this)
//        setContentView(surfaceView)
//
//        // Check ARCore availability
//        try {
//            when (ArCoreApk.getInstance().checkAvailability(this)) {
//                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
//                    Toast.makeText(this, "ARCore not supported on this device", Toast.LENGTH_LONG).show()
//                    finish()
//                    return
//                }
//                else -> { /* Supported */ }
//            }
//        } catch (e: Exception) {
//            Log.e("SoleMateAR", "Error checking ARCore availability: ${e.message}")
//            finish()
//        }
//
//        // Listen for surface creation
//        surfaceView?.holder?.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                startARSession()
//            }
//
//            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                stopARSession()
//            }
//        })
//    }
//
//    private fun startARSession() {
//        if (session == null) {
//            try {
//                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
//                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
//                        installRequested = true
//                        return
//                    }
//                    ArCoreApk.InstallStatus.INSTALLED -> {
//                        session = Session(this)
//                    }
//                }
//            } catch (e: UnavailableUserDeclinedInstallationException) {
//                Toast.makeText(this, "ARCore installation declined", Toast.LENGTH_LONG).show()
//                return
//            } catch (e: UnavailableException) {
//                Toast.makeText(this, "ARCore unavailable: ${e.message}", Toast.LENGTH_LONG).show()
//                return
//            }
//        }
//
//        try {
//            session?.resume()
//            Toast.makeText(this, "AR Session started", Toast.LENGTH_SHORT).show()
//        } catch (e: CameraNotAvailableException) {
//            Toast.makeText(this, "Camera not available. Try restarting the app.", Toast.LENGTH_LONG).show()
//            session = null
//        }
//    }
//
//    private fun stopARSession() {
//        try {
//            session?.pause()
//            session?.close()
//        } catch (e: Exception) {
//            Log.e("SoleMateAR", "Error stopping session: ${e.message}")
//        }
//        session = null
//    }
//
//    override fun onResume() {
//        super.onResume()
//        session?.resume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        session?.pause()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        stopARSession()
//    }
//}






// Last Working Code for ARActivity.kt


//package com.solemate.app.solemate_app
//
//import android.opengl.GLSurfaceView
//import android.os.Bundle
//import android.util.Log
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.google.ar.core.*
//import com.google.ar.core.exceptions.CameraNotAvailableException
//import com.google.ar.core.exceptions.UnavailableException
//import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
//
//class ARActivity : AppCompatActivity() {
//
//    private var glSurfaceView: GLSurfaceView? = null
//    private var session: Session? = null
//    private var installRequested = false
//    private var renderer: SimpleRenderer? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        Log.d("SoleMateAR", "Opened AR View")
//
//        // ✅ Use GLSurfaceView instead of SurfaceView
//        glSurfaceView = GLSurfaceView(this)
//        setContentView(glSurfaceView)
//
//        // ARCore session setup
//        try {
//            when (ArCoreApk.getInstance().checkAvailability(this)) {
//                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
//                    Toast.makeText(this, "ARCore not supported on this device", Toast.LENGTH_LONG).show()
//                    finish()
//                    return
//                }
//                else -> { /* Supported */ }
//            }
//        } catch (e: Exception) {
//            Log.e("SoleMateAR", "Error checking ARCore availability: ${e.message}")
//            finish()
//        }
//
//        setupSurfaceView()
//    }
//
//    private fun setupSurfaceView() {
//        glSurfaceView?.preserveEGLContextOnPause = true
//        glSurfaceView?.setEGLContextClientVersion(2)
//    }
//
//    private fun startARSession() {
//        if (session == null) {
//            try {
//                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
//                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
//                        installRequested = true
//                        return
//                    }
//                    ArCoreApk.InstallStatus.INSTALLED -> {
//                        session = Session(this)
//                    }
//                }
//            } catch (e: UnavailableUserDeclinedInstallationException) {
//                Toast.makeText(this, "ARCore installation declined", Toast.LENGTH_LONG).show()
//                return
//            } catch (e: UnavailableException) {
//                Toast.makeText(this, "ARCore unavailable: ${e.message}", Toast.LENGTH_LONG).show()
//                return
//            }
//        }
//
//        try {
//            // ✅ Initialize a simple renderer to draw camera feed
//            renderer = SimpleRenderer(session!!)
//            glSurfaceView?.setRenderer(renderer)
//            glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
//
//            session?.resume()
//            Toast.makeText(this, "AR Session started", Toast.LENGTH_SHORT).show()
//        } catch (e: CameraNotAvailableException) {
//            Toast.makeText(this, "Camera not available. Try restarting the app.", Toast.LENGTH_LONG).show()
//            session = null
//        }
//    }
//
//    private fun stopARSession() {
//        try {
//            session?.pause()
//            session?.close()
//        } catch (e: Exception) {
//            Log.e("SoleMateAR", "Error stopping session: ${e.message}")
//        }
//        session = null
//    }
//
//    override fun onResume() {
//        super.onResume()
//        if (session == null) {
//            startARSession()
//        } else {
//            try {
//                session?.resume()
//            } catch (e: CameraNotAvailableException) {
//                Toast.makeText(this, "Camera not available.", Toast.LENGTH_LONG).show()
//                session = null
//            }
//        }
//        glSurfaceView?.onResume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        glSurfaceView?.onPause()
//        session?.pause()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        stopARSession()
//    }
//}
//----------------------------------------------------------------------------------------







//package com.solemate.app.solemate_app
//
//import android.opengl.GLSurfaceView
//import android.os.Bundle
//import android.util.Log
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import com.google.ar.core.*
//import com.google.ar.core.exceptions.CameraNotAvailableException
//import com.google.ar.core.exceptions.UnavailableException
//import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
//
//class ARActivity : AppCompatActivity() {
//
//    private var glSurfaceView: GLSurfaceView? = null
//    private var session: Session? = null
//    private var installRequested = false
//    private var renderer: SimpleRenderer? = null
//    private lateinit var rotationHelper: DisplayRotationHelper
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        Log.d("SoleMateAR", "Opened AR View")
//
//        // ✅ Initialize GLSurfaceView for AR rendering
//        glSurfaceView = GLSurfaceView(this)
//        setContentView(glSurfaceView)
//
//        rotationHelper = DisplayRotationHelper(this)
//
//        // ✅ Check ARCore support
//        try {
//            when (ArCoreApk.getInstance().checkAvailability(this)) {
//                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
//                    Toast.makeText(this, "ARCore not supported on this device", Toast.LENGTH_LONG).show()
//                    finish()
//                    return
//                }
//                else -> { /* Supported */ }
//            }
//        } catch (e: Exception) {
//            Log.e("SoleMateAR", "Error checking ARCore availability: ${e.message}")
//            finish()
//        }
//
//        setupSurfaceView()
//    }
//
//    private fun setupSurfaceView() {
//        glSurfaceView?.preserveEGLContextOnPause = true
//        glSurfaceView?.setEGLContextClientVersion(2)
//    }
//
//    private fun startARSession() {
//        if (session == null) {
//            try {
//                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
//                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
//                        installRequested = true
//                        return
//                    }
//                    ArCoreApk.InstallStatus.INSTALLED -> {
//                        session = Session(this)
//                    }
//                    ArCoreApk.InstallStatus.INSTALLED -> {
//                        session = Session(this)
//
//                        // ✅ Configure ARCore for horizontal plane detection
//                        session?.let { arSession ->
//                            val config = Config(arSession)
//                            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
//                            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
//                            arSession.configure(config)
//                        }
//                    }
//                }
//            } catch (e: UnavailableUserDeclinedInstallationException) {
//                Toast.makeText(this, "ARCore installation declined", Toast.LENGTH_LONG).show()
//                return
//            } catch (e: UnavailableException) {
//                Toast.makeText(this, "ARCore unavailable: ${e.message}", Toast.LENGTH_LONG).show()
//                return
//            }
//        }
//
//        try {
//            // ✅ Pass DisplayRotationHelper to renderer for correct orientation
//            renderer = SimpleRenderer(session!!, rotationHelper)
//            glSurfaceView?.setRenderer(renderer)
//            glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
//
//            session?.resume()
//            Toast.makeText(this, "AR Session started", Toast.LENGTH_SHORT).show()
//        } catch (e: CameraNotAvailableException) {
//            Toast.makeText(this, "Camera not available. Try restarting the app.", Toast.LENGTH_LONG).show()
//            session = null
//        }
//    }
//
//    private fun stopARSession() {
//        try {
//            session?.pause()
//            session?.close()
//        } catch (e: Exception) {
//            Log.e("SoleMateAR", "Error stopping session: ${e.message}")
//        }
//        session = null
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        if (session == null) {
//            startARSession()
//        } else {
//            try {
//                session?.resume()
//            } catch (e: CameraNotAvailableException) {
//                Toast.makeText(this, "Camera not available.", Toast.LENGTH_LONG).show()
//                session = null
//            }
//        }
//
//        rotationHelper.onSurfaceChanged(
//            glSurfaceView?.width ?: 0,
//            glSurfaceView?.height ?: 0
//        )
//
//        glSurfaceView?.onResume()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        glSurfaceView?.onPause()
//        session?.pause()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        stopARSession()
//    }
//}



//__________________________________________________________________________________









package com.solemate.app.solemate_app

import android.graphics.Color
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.TextureView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

enum class ARMode {
    ARCORE,  // ARCore-supported device
    VIO      // Fallback using VIO (not yet implemented)
}

class ARActivity : AppCompatActivity() {

    private var glSurfaceView: GLSurfaceView? = null
    private var textureView: TextureView? = null
    private var session: Session? = null
    private var installRequested = false
    private var renderer: SimpleRenderer? = null
    private lateinit var rotationHelper: DisplayRotationHelper
    private var shoeRendererInstance: ShoeRenderer? = null
    private var arMode: ARMode? = null
    
    // VIO mode components
    private var vioEngine: VioEngine? = null
    private var vioPoseProvider: VioPoseProvider? = null
    private var camera2Manager: Camera2Manager? = null
    private var simpleCameraManager: SimpleCameraManager? = null
    private var planeEstimator: PlaneEstimator? = null
    private var cameraSurfaceTexture: android.graphics.SurfaceTexture? = null

    // ✅ new: store a single queued tap for the renderer to consume
    private var queuedSingleTap: MotionEvent? = null

    companion object {
        private const val TAG = "ARActivity"
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate() called")
        
        // Step 1.1: Check camera permissions first
        if (!checkCameraPermission()) {
            Log.w(TAG, "Camera permission not granted, requesting...")
            requestCameraPermission()
            return  // Will continue in onRequestPermissionsResult
        }
        
        Log.d(TAG, "Camera permission granted, proceeding with setup")
        initializeAR()
    }
    
    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        val granted = result == android.content.pm.PackageManager.PERMISSION_GRANTED
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
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted by user")
                initializeAR()
            } else {
                Log.e(TAG, "Camera permission denied by user")
                Toast.makeText(this, "Camera permission is required for AR features", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun initializeAR() {
        Log.d(TAG, "Initializing AR...")

        // ✅ Create GLSurfaceView for ARCore (background layer)
        glSurfaceView = GLSurfaceView(this)
        
        // ✅ Create TextureView for Filament overlay (foreground layer) - only used in ARCore mode
        textureView = TextureView(this)
        textureView?.isOpaque = false  // Transparent background
        textureView?.visibility = android.view.View.GONE  // Hidden by default, shown only in ARCore mode
        
        // ✅ Wrap both views in FrameLayout
        val frameLayout = FrameLayout(this)
        frameLayout.addView(glSurfaceView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        frameLayout.addView(textureView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        // ✅ Add recalibration button
        val recalibrateButton = Button(this).apply {
            text = "Recalibrate"
            setBackgroundColor(Color.parseColor("#2196F3"))  // Material Blue
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(32, 16, 32, 16)
            elevation = 8f
            
            setOnClickListener {
                renderer?.requestRecalibration()
                Toast.makeText(this@ARActivity, "Recalibrating shoe placement...", Toast.LENGTH_SHORT).show()
                Log.d("ARActivity", "Recalibration requested by user")
            }
        }
        
        // Position button at bottom-center
        val buttonParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 48  // 48dp from bottom
        }
        frameLayout.addView(recalibrateButton, buttonParams)
        
        setContentView(frameLayout)
        Log.d(TAG, "setContentView() called with frameLayout")
        Log.d(TAG, "GLSurfaceView: ${glSurfaceView != null}, TextureView: ${textureView != null}")
        Log.d(TAG, "Created TextureView overlay with recalibration button")

        rotationHelper = DisplayRotationHelper(this)

        // ✅ Listen for screen taps on the frame layout
        frameLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                queuedSingleTap = event
            }
            true
        }

        // ✅ Check AR capability and determine mode
        arMode = checkARCapability()
        if (arMode == null) {
            Toast.makeText(this, "AR not available on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupSurfaceView()
        
        // ✅ Start AR session immediately in onCreate (before surface is created)
        // This ensures renderer is set before GLSurfaceView creates the surface
        Log.d(TAG, "Starting AR session from onCreate()")
        startARSession()
    }

    /**
     * Check AR capability and return the appropriate mode
     * Returns null if AR is not available at all
     */
    private fun checkARCapability(): ARMode? {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(this)
            Log.d("ARActivity", "ARCore availability: $availability")
            when (availability) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED,
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> {
                    arMode = ARMode.ARCORE
                    Log.d("ARActivity", "ARCore supported and installed, using ARCore mode")
                    ARMode.ARCORE
                }
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    // ARCore says it's supported but not installed
                    // On non-ARCore devices, this is often a false positive
                    // Try to install, but if it fails, fall back to VIO
                    Log.d("ARActivity", "ARCore not installed, will try to install but may fall back to VIO")
                    arMode = ARMode.ARCORE  // Try ARCore first
                    ARMode.ARCORE
                }
                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                    // ARCore not supported - use VIO fallback
                    arMode = ARMode.VIO
                    Log.d("ARActivity", "ARCore not supported, using VIO fallback")
                    ARMode.VIO
                }
                else -> {
                    Log.w("ARActivity", "ARCore availability unknown: $availability, defaulting to VIO")
                    arMode = ARMode.VIO
                    ARMode.VIO
                }
            }
        } catch (e: Exception) {
            Log.e("ARActivity", "Error checking ARCore availability: ${e.message}", e)
            // On error, default to VIO mode
            Log.d("ARActivity", "Defaulting to VIO mode due to error")
            arMode = ARMode.VIO
            ARMode.VIO
        }
    }

    private fun setupSurfaceView() {
        glSurfaceView?.preserveEGLContextOnPause = true
        // Filament requires OpenGL ES 3.0+, but BackgroundRenderer uses ES 2.0
        // We'll use ES 2.0 for compatibility with BackgroundRenderer
        glSurfaceView?.setEGLContextClientVersion(2)
        Log.d("ARActivity", "GLSurfaceView configured: preserveEGLContextOnPause=true, EGLContextClientVersion=2")
    }

    private fun startARSession() {
        when (arMode) {
            ARMode.ARCORE -> startARCoreSession()
            ARMode.VIO -> startVIOSession()
            null -> {
                Log.e(TAG, "AR mode not selected, cannot start session")
                Toast.makeText(this, "AR mode not selected", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startARCoreSession() {
        if (session == null) {
            try {
                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        Log.d("ARActivity", "ARCore installation requested, waiting for user")
                        return
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        try {
                            session = Session(this)

                            // ✅ Configure ARCore for horizontal plane detection
                            session?.let { arSession ->
                                val config = Config(arSession)
                                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                                config.depthMode = Config.DepthMode.AUTOMATIC
                                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                                arSession.configure(config)
                            }
                            Log.d("ARActivity", "ARCore session created successfully")
                        } catch (e: Exception) {
                            Log.e("ARActivity", "Failed to create ARCore session: ${e.message}", e)
                            // Fall back to VIO if ARCore session creation fails
                            Log.d("ARActivity", "Falling back to VIO mode")
                            arMode = ARMode.VIO
                            startVIOSession()
                            return
                        }
                    }
                }
            } catch (e: UnavailableUserDeclinedInstallationException) {
                Log.w("ARActivity", "ARCore installation declined, falling back to VIO")
                Toast.makeText(this, "ARCore not available, using VIO mode", Toast.LENGTH_SHORT).show()
                arMode = ARMode.VIO
                startVIOSession()
                return
            } catch (e: UnavailableException) {
                Log.w("ARActivity", "ARCore unavailable: ${e.message}, falling back to VIO")
                Toast.makeText(this, "ARCore not available, using VIO mode", Toast.LENGTH_SHORT).show()
                arMode = ARMode.VIO
                startVIOSession()
                return
            }
        }

        try {
            // ✅ Create ShoeRenderer and attach to TextureView (only in ARCore mode)
            shoeRendererInstance = ShoeRenderer(this)
            textureView?.let { tv ->
                tv.visibility = android.view.View.VISIBLE  // Show TextureView for ARCore mode
                shoeRendererInstance!!.attachToTextureView(tv)
            }
            
            // ✅ Pass Session directly to SimpleRenderer (ARCore mode)
            renderer = SimpleRenderer(
                session = session!!,
                rotationHelper = rotationHelper,
                activity = this,
                shoeRenderer = shoeRendererInstance,
                poseProvider = null  // No pose provider for ARCore mode
            )
            glSurfaceView?.setRenderer(renderer)
            glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            session?.resume()
            Toast.makeText(this, "AR Session started (ARCore)", Toast.LENGTH_SHORT).show()
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(this, "Camera not available. Try restarting the app.", Toast.LENGTH_LONG).show()
            session = null
        }
    }

    private fun startVIOSession() {
        try {
            Log.d("ARActivity", "Starting SimpleCamera session (replacement for VIO)")
            
            // Use SimpleCameraManager instead of Camera2Manager
            camera2Manager = null  // Clear old VIO manager
            simpleCameraManager = SimpleCameraManager(this)
            simpleCameraManager!!.initialize()
            
            // Create SimpleCameraPoseProvider with camera manager reference
            val simpleCameraPoseProvider = SimpleCameraPoseProvider(this, rotationHelper, simpleCameraManager)
            
            // Default camera intrinsics (640x480)
            val imageWidth = 640
            val imageHeight = 480
            val fx = 500.0f
            val fy = 500.0f
            val cx = imageWidth / 2.0f
            val cy = imageHeight / 2.0f
            
            // Initialize SimpleCameraPoseProvider
            if (!simpleCameraPoseProvider.initialize(imageWidth, imageHeight, fx, fy, cx, cy)) {
                Toast.makeText(this, "Failed to initialize camera", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            // Don't create SurfaceTexture here - it will be created in createCameraSurfaceTexture()
            cameraSurfaceTexture = null
            
            // Hide TextureView in VIO mode (we only use GLSurfaceView for camera preview)
            textureView?.visibility = android.view.View.GONE
            shoeRendererInstance = null  // Don't create ShoeRenderer in VIO mode
            
            // Create SimpleRenderer with SimpleCameraPoseProvider (camera preview only)
            renderer = SimpleRenderer(
                session = null,  // No ARCore session for SimpleCamera mode
                rotationHelper = rotationHelper,
                activity = this,
                shoeRenderer = null,  // No ShoeRenderer in VIO mode
                poseProvider = simpleCameraPoseProvider
            )
            glSurfaceView?.setRenderer(renderer)
            glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            Log.d("ARActivity", "Renderer set synchronously (SimpleCamera pattern), renderMode=${glSurfaceView?.renderMode}")
            
            // Verify renderer was set successfully
            if (renderer == null) {
                Log.e("ARActivity", "Renderer creation failed")
                Toast.makeText(this, "Failed to create renderer", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            Toast.makeText(this, "AR Session started (VIO)", Toast.LENGTH_SHORT).show()
            Log.d("ARActivity", "VIO session started successfully")
        } catch (e: Exception) {
            Log.e("ARActivity", "Error starting VIO session: ${e.message}", e)
            Toast.makeText(this, "Failed to start VIO: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /** ✅ Safely provides and clears any pending tap */
    fun getQueuedSingleTap(): MotionEvent? {
        val tap = queuedSingleTap
        queuedSingleTap = null
        return tap
    }
    
    /**
     * Get camera SurfaceTexture for VIO mode (for renderer to set up).
     */
    fun getCameraSurfaceTexture(): android.graphics.SurfaceTexture? {
        return cameraSurfaceTexture
    }
    
    /**
     * Set camera SurfaceTexture (called by MinimalCameraRenderer).
     */
    fun setCameraSurfaceTexture(texture: android.graphics.SurfaceTexture) {
        Log.d(TAG, "setCameraSurfaceTexture() called")
        cameraSurfaceTexture = texture
    }
    
    /**
     * Request render on GL thread (for MinimalCameraRenderer).
     */
    fun requestRender() {
        glSurfaceView?.requestRender()
    }
    
    /**
     * Get SimpleCameraManager (for rotation correction).
     */
    fun getSimpleCameraManager(): SimpleCameraManager? {
        return simpleCameraManager
    }
    
    // Make ARActivity implement ARActivityWrapper
    fun asWrapper(): ARActivityWrapper = object : ARActivityWrapper {
        override fun setCameraSurfaceTexture(texture: android.graphics.SurfaceTexture) {
            this@ARActivity.setCameraSurfaceTexture(texture)
        }
        
        override fun getCameraSurfaceTexture(): android.graphics.SurfaceTexture? {
            return this@ARActivity.getCameraSurfaceTexture()
        }
        
        override fun openVioCamera() {
            this@ARActivity.openVioCamera()
        }
        
        override fun requestRender() {
            this@ARActivity.requestRender()
        }
    }
    
    /**
     * Create SurfaceTexture for camera preview with the given texture ID.
     * Called by renderer when texture is ready.
     */
    fun createCameraSurfaceTexture(textureId: Int): android.graphics.SurfaceTexture? {
        Log.d("ARActivity", "createCameraSurfaceTexture() called with textureId=$textureId")
        Log.d("ARActivity", "SurfaceTexture lifecycle: current=${cameraSurfaceTexture != null}")
        
        // Release existing SurfaceTexture if any
        cameraSurfaceTexture?.let { oldTexture ->
            try {
                Log.d("ARActivity", "Releasing old SurfaceTexture before creating new one")
                oldTexture.release()
                Log.d("ARActivity", "Old SurfaceTexture released successfully")
            } catch (e: Exception) {
                Log.e("ARActivity", "Error releasing old SurfaceTexture: ${e.message}", e)
            }
            cameraSurfaceTexture = null
        }
        
        try {
            // Create SurfaceTexture with the actual texture ID
            Log.d("ARActivity", "Creating new SurfaceTexture with textureId=$textureId")
            cameraSurfaceTexture = android.graphics.SurfaceTexture(textureId)
            
            // Default camera intrinsics (640x480)
            val imageWidth = 640
            val imageHeight = 480
            cameraSurfaceTexture?.setDefaultBufferSize(imageWidth, imageHeight)
            Log.d("ARActivity", "SurfaceTexture buffer size set to ${imageWidth}x${imageHeight}")
            
            // Set up texture update listener to request render when frame is available
            // Note: updateTexImage() must be called on GL thread, so we only request render here
            cameraSurfaceTexture?.setOnFrameAvailableListener { texture ->
                // Request render - updateTexImage() will be called on GL thread in renderer
                glSurfaceView?.requestRender()
            }
            Log.d("ARActivity", "SurfaceTexture frame available listener set")
            
            Log.d("ARActivity", "SurfaceTexture created successfully with textureId=$textureId")
            return cameraSurfaceTexture
        } catch (e: Exception) {
            Log.e("ARActivity", "Error creating SurfaceTexture: ${e.message}", e)
            cameraSurfaceTexture = null
            return null
        }
    }
    
    /**
     * Open camera after renderer has set up the texture.
     */
    fun openVioCamera() {
        Log.d(TAG, "openVioCamera() called")
        Log.d(TAG, "SurfaceTexture state: cameraSurfaceTexture=${cameraSurfaceTexture != null}, simpleCameraManager=${simpleCameraManager != null}, camera2Manager=${camera2Manager != null}")
        
        cameraSurfaceTexture?.let { texture ->
            Log.d(TAG, "Opening camera with SurfaceTexture")
            try {
                // Use SimpleCameraManager if available, otherwise fall back to Camera2Manager
                when {
                    simpleCameraManager != null -> {
                        Log.d(TAG, "Using SimpleCameraManager to open camera")
                        simpleCameraManager!!.openCamera(texture)
                    }
                    camera2Manager != null -> {
                        Log.d(TAG, "Using Camera2Manager to open camera")
                        camera2Manager!!.openCamera(texture)
                    }
                    else -> {
                        Log.e(TAG, "No camera manager available (simpleCameraManager=${simpleCameraManager != null}, camera2Manager=${camera2Manager != null})")
                        Toast.makeText(this, "Camera manager not initialized", Toast.LENGTH_LONG).show()
                    }
                }
                Log.d(TAG, "Camera open requested successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error opening camera: ${e.message}", e)
                Log.e(TAG, "Stack trace:", e)
                Toast.makeText(this, "Failed to open camera: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Log.e(TAG, "cameraSurfaceTexture is null, cannot open camera")
        }
    }


    private fun stopARSession() {
        when (arMode) {
            ARMode.ARCORE -> {
                try {
                    session?.pause()
                    session?.close()
                } catch (e: Exception) {
                    Log.e("SoleMateAR", "Error stopping ARCore session: ${e.message}")
                }
                session = null
            }
            ARMode.VIO -> {
                try {
                    // Stop SimpleCameraManager if using new approach
                    simpleCameraManager?.stopCapture()
                    simpleCameraManager?.closeCamera()
                    // Stop old VIO components if using old approach
                    camera2Manager?.stopCapture()
                    vioPoseProvider?.stop()
                } catch (e: Exception) {
                    Log.e("SoleMateAR", "Error stopping VIO session: ${e.message}")
                }
            }
            null -> { /* No session */ }
        }
    }

    override fun onResume() {
        super.onResume()
        
        Log.d("ARActivity", "onResume() called, arMode=$arMode")

        when (arMode) {
            ARMode.ARCORE -> {
                Log.d(TAG, "ARCore mode: session=${session != null}, installRequested=$installRequested")
                if (session == null) {
                    // If we previously requested installation but still don't have a session,
                    // it means installation failed or was declined - fall back to VIO
                    if (installRequested) {
                        Log.w(TAG, "ARCore installation was requested but session is still null, falling back to VIO")
                        arMode = ARMode.VIO
                        startVIOSession()
                    } else {
                        // Session should already be started in onCreate, but start it if not
                        if (renderer == null) {
                            Log.d(TAG, "Renderer not set, starting AR session from onResume()")
                            startARSession()
                        }
                    }
                } else {
                    try {
                        session?.resume()
                    } catch (e: CameraNotAvailableException) {
                        Toast.makeText(this, "Camera not available.", Toast.LENGTH_LONG).show()
                        session = null
                    }
                }
            }
            ARMode.VIO -> {
                Log.d(TAG, "VIO mode: simpleCameraManager=${simpleCameraManager != null}, renderer=${renderer != null}")
                if (renderer == null) {
                    Log.d(TAG, "Renderer not set, starting VIO session from onResume()")
                    startVIOSession()
                } else {
                    // Resume camera tracking
                    Log.d(TAG, "Resuming camera tracking")
                    simpleCameraManager?.startCapture()
                    camera2Manager?.startCapture()
                    vioPoseProvider?.start()
                }
            }
            null -> { 
                Log.w(TAG, "No AR mode set in onResume()")
            }
        }

        // Don't continue if activity is finishing (e.g., if session initialization failed)
        if (isFinishing) {
            return
        }

        rotationHelper.onSurfaceChanged(
            glSurfaceView?.width ?: 0,
            glSurfaceView?.height ?: 0
        )

        // Resume GLSurfaceView if it exists and has been initialized
        // Only call onResume() if glSurfaceView is not null and renderer has been set
        if (glSurfaceView != null && renderer != null) {
            try {
                glSurfaceView?.onResume()
                Log.d(TAG, "GLSurfaceView.onResume() called successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error resuming GLSurfaceView: ${e.message}", e)
            }
        } else {
            Log.d(TAG, "GLSurfaceView or renderer is null in onResume(), skipping onResume() call (glSurfaceView=${glSurfaceView != null}, renderer=${renderer != null})")
        }
    }

    override fun onPause() {
        super.onPause()
        
        Log.d(TAG, "onPause() called")
        
        // Pause GLSurfaceView if it exists
        glSurfaceView?.let { view ->
            try {
                view.onPause()
                Log.d(TAG, "GLSurfaceView.onPause() called successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error pausing GLSurfaceView: ${e.message}", e)
            }
        } ?: run {
            Log.d(TAG, "GLSurfaceView is null in onPause(), skipping onPause() call")
        }
        
        // Detach SurfaceTexture before pausing GLSurfaceView to avoid context issues
        Log.d(TAG, "onPause() - SurfaceTexture lifecycle: cameraSurfaceTexture=${cameraSurfaceTexture != null}")
        cameraSurfaceTexture?.let { texture ->
            try {
                Log.d(TAG, "Detaching SurfaceTexture from GL context in onPause()")
                texture.detachFromGLContext()
                Log.d(TAG, "SurfaceTexture detached from GL context successfully in onPause()")
            } catch (e: Exception) {
                // Not attached, that's fine
                Log.d("ARActivity", "SurfaceTexture not attached in onPause() (expected if not initialized): ${e.message}")
            }
        } ?: run {
            Log.d(TAG, "No SurfaceTexture to detach in onPause()")
        }
        
        when (arMode) {
            ARMode.ARCORE -> {
                session?.pause()
            }
            ARMode.VIO -> {
                camera2Manager?.stopCapture()
                vioPoseProvider?.stop()
            }
            null -> { /* No session */ }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("ARActivity", "onDestroy() called - cleaning up resources")
        
        try {
            renderer?.release()
        } catch (_: Exception) { }
        
        // Release VIO/SimpleCamera resources
        simpleCameraManager?.release()
        camera2Manager?.release()
        vioPoseProvider?.release()
        vioEngine?.release()
        
        // Release SurfaceTexture
        Log.d("ARActivity", "onDestroy() - Releasing SurfaceTexture: cameraSurfaceTexture=${cameraSurfaceTexture != null}")
        cameraSurfaceTexture?.let { texture ->
            try {
                texture.release()
                Log.d("ARActivity", "SurfaceTexture released in onDestroy()")
            } catch (e: Exception) {
                Log.e("ARActivity", "Error releasing SurfaceTexture in onDestroy(): ${e.message}", e)
            }
        }
        cameraSurfaceTexture = null
        
        stopARSession()
        Log.d("ARActivity", "onDestroy() completed")
    }
}
