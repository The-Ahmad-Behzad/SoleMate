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



package com.solemate.app.solemate_app

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException

class ARActivity : Activity() {

    private var surfaceView: SurfaceView? = null
    private var session: Session? = null
    private var installRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SoleMateAR", "Opened AR View")

        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        // Check ARCore availability
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

        // Listen for surface creation
        surfaceView?.holder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startARSession()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopARSession()
            }
        })
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
            session?.resume()
            Toast.makeText(this, "AR Session started", Toast.LENGTH_SHORT).show()
        } catch (e: CameraNotAvailableException) {
            Toast.makeText(this, "Camera not available. Try restarting the app.", Toast.LENGTH_LONG).show()
            session = null
        }
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
        session?.resume()
    }

    override fun onPause() {
        super.onPause()
        session?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopARSession()
    }
}
