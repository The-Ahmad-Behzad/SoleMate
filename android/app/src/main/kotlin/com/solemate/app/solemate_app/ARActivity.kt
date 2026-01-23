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

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.TextureView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

class ARActivity : AppCompatActivity() {

    private var glSurfaceView: GLSurfaceView? = null
    private var textureView: TextureView? = null
    private var session: Session? = null
    private var installRequested = false
    private var renderer: ObjectAttachedRenderer? = null  // Using object-based renderer
    private lateinit var rotationHelper: DisplayRotationHelper
    private var shoeRenderer: ShoeRenderer? = null  // Single renderer managing both shoes

    // ✅ new: store a single queued tap for the renderer to consume
    private var queuedSingleTap: MotionEvent? = null
    
    // ==== Screenshot capture components ====
    private lateinit var screenshotManager: ScreenshotManager
    private var miniPreviewCard: CardView? = null
    private var miniPreviewImage: ImageView? = null
    private var snapCountBadge: TextView? = null
    private var captureButton: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SoleMateAR", "Opened AR View")
        
        // Register with MainActivity for platform channel access
        MainActivity.currentARActivity = this
        
        // Initialize screenshot manager
        screenshotManager = ScreenshotManager(this)

        // ✅ Create GLSurfaceView for ARCore (background layer)
        glSurfaceView = GLSurfaceView(this)
        
        // ✅ Create TextureView for Filament overlay (foreground layer)
        textureView = TextureView(this)
        textureView?.isOpaque = false  // Transparent background
        
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
        
        // ==== Add capture button (top-right) ====
        captureButton = ImageButton(this).apply {
            // Use smartphone icon from Material icons
            setImageResource(android.R.drawable.ic_menu_camera) // Fallback camera icon
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.WHITE)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(24, 24, 24, 24)
            elevation = 8f
            
            // Create circular background
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#80000000")) // Semi-transparent black
            }
            background = bgDrawable
            
            setOnClickListener {
                captureScreenshot()
            }
        }
        
        val captureParams = FrameLayout.LayoutParams(
            dpToPx(56),
            dpToPx(56)
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = dpToPx(48)
            rightMargin = dpToPx(16)
        }
        frameLayout.addView(captureButton, captureParams)
        
        // ==== Add mini-preview card (bottom-right) ====
        miniPreviewCard = CardView(this).apply {
            radius = dpToPx(8).toFloat()
            cardElevation = dpToPx(4).toFloat()
            alpha = 0.3f // 30% opacity as specified
            visibility = android.view.View.GONE // Hidden until first snap
            
            setOnClickListener {
                openImageManager()
            }
        }
        
        // Mini-preview image inside card
        miniPreviewImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.DKGRAY)
        }
        miniPreviewCard?.addView(miniPreviewImage, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        val previewParams = FrameLayout.LayoutParams(
            dpToPx(80),
            dpToPx(100)
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            bottomMargin = dpToPx(16)
            rightMargin = dpToPx(16)
        }
        frameLayout.addView(miniPreviewCard, previewParams)
        
        // ==== Add snap count badge (overlapping mini-preview top-right) ====
        snapCountBadge = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 4)
            visibility = android.view.View.GONE
            
            val badgeBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                setColor(Color.parseColor("#FF5722")) // Orange accent
            }
            background = badgeBg
        }
        
        val badgeParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            // Position at top-right of mini-preview
            bottomMargin = dpToPx(100) // Above the mini-preview
            rightMargin = dpToPx(8)
        }
        frameLayout.addView(snapCountBadge, badgeParams)
        
        setContentView(frameLayout)
        Log.d("ARActivity", "Created AR UI with capture button and mini-preview")

        rotationHelper = DisplayRotationHelper(this)

        // ✅ Listen for screen taps on the frame layout
        frameLayout.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                queuedSingleTap = event
            }
            true
        }

        // ✅ Check ARCore support
        try {
            when (ArCoreApk.getInstance().checkAvailability(this)) {
                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                    Toast.makeText(this, "ARCore not supported on this device", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
                else -> { /* Supported */ }
            }
        } catch (e: Exception) {
            Log.e("SoleMateAR", "Error checking ARCore availability: ${e.message}")
            finish()
        }

        setupSurfaceView()
    }

    private fun setupSurfaceView() {
        glSurfaceView?.preserveEGLContextOnPause = true
        // Filament requires OpenGL ES 3.0+
        glSurfaceView?.setEGLContextClientVersion(3)
    }

    private fun startARSession() {
        if (session == null) {
            try {
                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        return
                    }
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        session = Session(this)

                        // ✅ Configure ARCore - Hybrid mode: ML detection + Instant Placement
                        session?.let { arSession ->
                            val config = Config(arSession)
                            // Disable plane detection - using ML object detection instead
                            config.planeFindingMode = Config.PlaneFindingMode.DISABLED
                            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                            // Enable depth for better positioning
                            config.depthMode = Config.DepthMode.AUTOMATIC
                            // Enable Instant Placement for anchor-based shoe placement
                            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                            arSession.configure(config)
                        }
                    }
                }
            } catch (e: UnavailableUserDeclinedInstallationException) {
                Toast.makeText(this, "ARCore installation declined", Toast.LENGTH_LONG).show()
                return
            } catch (e: UnavailableException) {
                Toast.makeText(this, "ARCore unavailable: ${e.message}", Toast.LENGTH_LONG).show()
                return
            }
        }

        try {
            // ✅ Create single ShoeRenderer that manages both shoes internally
            shoeRenderer = ShoeRenderer(this)
            
            textureView?.let { tv ->
                shoeRenderer!!.attachToTextureView(tv)
            }
            
            // ✅ Use ObjectAttachedRenderer with single shoe renderer
            renderer = ObjectAttachedRenderer(session!!, rotationHelper, this, shoeRenderer)
            glSurfaceView?.setRenderer(renderer)
            glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            session?.resume()
            Toast.makeText(this, "AR Session started (dual foot tracking)", Toast.LENGTH_SHORT).show()
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(this, "Camera not available. Try restarting the app.", Toast.LENGTH_LONG).show()
            session = null
        }
    }

    /** ✅ Safely provides and clears any pending tap */
    fun getQueuedSingleTap(): MotionEvent? {
        val tap = queuedSingleTap
        queuedSingleTap = null
        return tap
    }


    private fun stopARSession() {
        try {
            session?.pause()
            session?.close()
        } catch (e: Exception) {
            Log.e("SoleMateAR", "Error stopping session: ${e.message}")
        }
        session = null
    }

    override fun onResume() {
        super.onResume()

        if (session == null) {
            startARSession()
        } else {
            try {
                session?.resume()
            } catch (e: CameraNotAvailableException) {
                Toast.makeText(this, "Camera not available.", Toast.LENGTH_LONG).show()
                session = null
            }
        }

        rotationHelper.onSurfaceChanged(
            glSurfaceView?.width ?: 0,
            glSurfaceView?.height ?: 0
        )

        glSurfaceView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView?.onPause()
        session?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister from MainActivity
        if (MainActivity.currentARActivity == this) {
            MainActivity.currentARActivity = null
        }
        
        try {
            renderer?.release()
        } catch (_: Exception) { }
        stopARSession()
    }
    
    // ==== Screenshot capture methods ====
    
    /**
     * Capture a screenshot of the current AR frame including the 3D shoe overlay.
     * Composites both the GLSurfaceView (camera feed) and TextureView (shoe model).
     */
    private fun captureScreenshot() {
        if (!screenshotManager.canCapture()) {
            Toast.makeText(this, "Maximum ${ScreenshotManager.MAX_SNAPS} snaps reached", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Request capture from GL thread (camera + planes)
        renderer?.requestCapture { arBitmap ->
            if (arBitmap == null) {
                Toast.makeText(this, "Failed to capture AR frame", Toast.LENGTH_SHORT).show()
                return@requestCapture
            }
            
            try {
                // Get the shoe overlay from TextureView (runs on UI thread)
                val shoeBitmap = textureView?.bitmap
                
                // Composite both layers
                val compositedBitmap = if (shoeBitmap != null && shoeBitmap.width > 0 && shoeBitmap.height > 0) {
                    compositeFrames(arBitmap, shoeBitmap)
                } else {
                    // No shoe overlay, just use AR frame
                    Log.d("Screenshot", "No shoe overlay available, using AR frame only")
                    arBitmap.copy(arBitmap.config ?: Bitmap.Config.ARGB_8888, false)
                }
                
                // Save the composited image
                val saved = screenshotManager.captureAndSave(compositedBitmap)
                if (saved) {
                    updateMiniPreview()
                    Toast.makeText(this, "📸 Snap captured! (${screenshotManager.getSnapCount()}/${ScreenshotManager.MAX_SNAPS})", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save snap", Toast.LENGTH_SHORT).show()
                }
                
                // Clean up
                arBitmap.recycle()
                compositedBitmap.recycle()
                shoeBitmap?.recycle()
                
            } catch (e: Exception) {
                Log.e("Screenshot", "Error compositing frames: ${e.message}")
                Toast.makeText(this, "Failed to capture frame", Toast.LENGTH_SHORT).show()
                arBitmap.recycle()
            }
        }
    }
    
    /**
     * Composite the AR frame (camera + planes) with the shoe overlay.
     * Draws the shoe layer on top of the AR layer.
     */
    private fun compositeFrames(arFrame: Bitmap, shoeOverlay: Bitmap): Bitmap {
        // Create a new bitmap with the same size as the AR frame
        val result = Bitmap.createBitmap(arFrame.width, arFrame.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        
        // Draw the AR camera frame as the base layer
        canvas.drawBitmap(arFrame, 0f, 0f, null)
        
        // Scale and draw the shoe overlay on top
        // The TextureView might be a different size, so we scale it to match
        if (shoeOverlay.width != arFrame.width || shoeOverlay.height != arFrame.height) {
            val scaledShoe = Bitmap.createScaledBitmap(shoeOverlay, arFrame.width, arFrame.height, true)
            canvas.drawBitmap(scaledShoe, 0f, 0f, null)
            scaledShoe.recycle()
        } else {
            canvas.drawBitmap(shoeOverlay, 0f, 0f, null)
        }
        
        Log.d("Screenshot", "Composited frames: ${arFrame.width}x${arFrame.height}")
        return result
    }
    
    /**
     * Update the mini-preview with the latest captured thumbnail
     */
    private fun updateMiniPreview() {
        val thumbnail = screenshotManager.getLatestThumbnail()
        val snapCount = screenshotManager.getSnapCount()
        
        if (thumbnail != null && snapCount > 0) {
            miniPreviewImage?.setImageBitmap(thumbnail)
            miniPreviewCard?.visibility = android.view.View.VISIBLE
            miniPreviewCard?.alpha = 0.7f // Increase opacity when there are snaps
            
            snapCountBadge?.text = snapCount.toString()
            snapCountBadge?.visibility = android.view.View.VISIBLE
        } else {
            miniPreviewCard?.visibility = android.view.View.GONE
            snapCountBadge?.visibility = android.view.View.GONE
        }
    }
    
    /**
     * Open the Image Manager screen (Flutter)
     * Keeps ARActivity alive in background while Flutter shows Image Manager
     */
    private fun openImageManager() {
        val snapCount = screenshotManager.getSnapCount()
        if (snapCount == 0) {
            Toast.makeText(this, "No snaps to view", Toast.LENGTH_SHORT).show()
            return
        }
        
        val snapPaths = screenshotManager.getAllSnapPaths()
        
        Log.d("ARActivity", "Opening Image Manager with ${snapCount} snaps: $snapPaths")
        Toast.makeText(this, "Opening Image Manager...", Toast.LENGTH_SHORT).show()
        
        // Notify Flutter to open Image Manager (this calls the method channel)
        MainActivity.notifyFlutterToOpenImageManager(snapPaths)
        
        // Bring MainActivity (Flutter) to foreground WITHOUT finishing this activity
        // ARActivity stays alive in background and can be resumed
        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
        
        // Don't call finish() - keep AR session alive
    }
    
    /**
     * Handle back button with warning if there are unsaved snaps
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (screenshotManager.hasUnsavedSnaps()) {
            AlertDialog.Builder(this)
                .setTitle("Exit AR Session?")
                .setMessage("You have ${screenshotManager.getSnapCount()} unsaved snaps. They will be deleted if you exit.")
                .setPositiveButton("Exit") { _, _ ->
                    screenshotManager.clearAll()
                    @Suppress("DEPRECATION")
                    super.onBackPressed()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
    
    /**
     * Get all snap paths for platform channel communication
     */
    fun getSnapPaths(): List<String> = screenshotManager.getAllSnapPaths()
    
    /**
     * Get current snap count for platform channel communication
     */
    fun getSnapCount(): Int = screenshotManager.getSnapCount()
    
    /**
     * Clear all snaps (called when exiting AR session)
     */
    fun clearAllSnaps() {
        screenshotManager.clearAll()
        updateMiniPreview()
    }
    
    /**
     * Delete a specific snap by path (called from Flutter Image Manager)
     */
    fun deleteSnapByPath(path: String): Boolean {
        val success = screenshotManager.deleteSnapByPath(path)
        if (success) {
            updateMiniPreview()
        }
        return success
    }
    
    /**
     * Set filter for a specific snap (called from Flutter Image Manager)
     */
    fun setFilter(path: String, filterId: String?) {
        screenshotManager.setFilter(path, filterId)
    }
    
    /**
     * Get all applied filters as a map of path -> filterId
     */
    fun getFilters(): Map<String, String?> {
        val filters = mutableMapOf<String, String?>()
        for (path in screenshotManager.getAllSnapPaths()) {
            filters[path] = screenshotManager.getFilter(path)
        }
        return filters
    }
    
    // ==== Utility methods ====
    
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
