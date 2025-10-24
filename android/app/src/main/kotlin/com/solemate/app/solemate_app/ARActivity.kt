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

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

class ARActivity : AppCompatActivity() {

    private var glSurfaceView: GLSurfaceView? = null
    private var session: Session? = null
    private var installRequested = false
    private var renderer: SimpleRenderer? = null
    private lateinit var rotationHelper: DisplayRotationHelper

    // ✅ new: store a single queued tap for the renderer to consume
    private var queuedSingleTap: MotionEvent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SoleMateAR", "Opened AR View")

        // ✅ Initialize GLSurfaceView for AR rendering
        glSurfaceView = GLSurfaceView(this)
        setContentView(glSurfaceView)

        rotationHelper = DisplayRotationHelper(this)

        // ✅ Listen for screen taps (one at a time)
        glSurfaceView?.setOnTouchListener { _, event ->
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
        glSurfaceView?.setEGLContextClientVersion(2)
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

                        // ✅ Configure ARCore for horizontal plane detection
                        session?.let { arSession ->
                            val config = Config(arSession)
                            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
                            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
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
            // ✅ Pass DisplayRotationHelper to renderer for correct orientation
            renderer = SimpleRenderer(session!!, rotationHelper, this)
            glSurfaceView?.setRenderer(renderer)
            glSurfaceView?.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            session?.resume()
            Toast.makeText(this, "AR Session started", Toast.LENGTH_SHORT).show()
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
        stopARSession()
    }
}
