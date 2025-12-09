//package com.solemate.app.solemate_app
//
//import android.os.Bundle
//import io.flutter.embedding.android.FlutterActivity
//import io.flutter.embedding.engine.FlutterEngine
//import io.flutter.plugin.common.MethodChannel
//import android.util.Log
//
//class MainActivity : FlutterActivity() {
//
//    private val CHANNEL = "solemate/native"
//
//    external fun startARSessionNative(): String
//
//    companion object {
//        init {
//            System.loadLibrary("native-lib")
//        }
//    }
//
//    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
//        super.configureFlutterEngine(flutterEngine)
//
//        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
//            .setMethodCallHandler { call, result ->
//                when (call.method) {
//                    "startARSession" -> {
//                        try {
//                            val response = startARSessionNative()
//                            result.success(response)
//                        } catch (e: Exception) {
//                            Log.e("SoleMateNative", "Error: ${e.message}")
//                            result.error("NATIVE_ERROR", e.message, null)
//                        }
//                    }
//                    else -> result.notImplemented()
//                }
//            }
//    }
//}



package com.solemate.app.solemate_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "solemate/native"
    private val SCREENSHOT_CHANNEL = "solemate/screenshots"
    
    // Store reference to current AR activity for screenshot access
    companion object {
        var currentARActivity: ARActivity? = null
        
        // Store pending snap paths when AR activity wants to open Image Manager
        var pendingSnapPaths: List<String>? = null
        var pendingOpenImageManager: Boolean = false
        
        init {
            try {
                System.loadLibrary("native-lib")
                Log.i("SoleMateNative", "native-lib loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("SoleMateNative", "Failed to load native-lib: ${e.message}")
            }
        }
    }

    // JNI (native) function – optional
    external fun startARSessionNative(): String

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Main AR channel
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {

                    // 🚀 Flutter calls this to open AR Activity
                    "openARView" -> {
                        val intent = Intent(this, ARActivity::class.java)
                        startActivity(intent)
                        result.success("Opened AR View")
                    }

                    // Optional JNI function
                    "startARSession" -> {
                        try {
                            val response = startARSessionNative()
                            result.success(response)
                        } catch (e: Exception) {
                            Log.e("SoleMateNative", "Error calling native-lib: ${e.message}")
                            result.error("NATIVE_ERROR", e.message, null)
                        }
                    }

                    else -> result.notImplemented()
                }
            }
        
        // Screenshot management channel for Image Manager
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, SCREENSHOT_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    
                    // Get all captured snap paths
                    "getSnapPaths" -> {
                        val paths = currentARActivity?.getSnapPaths() ?: emptyList()
                        result.success(paths)
                    }
                    
                    // Get current snap count
                    "getSnapCount" -> {
                        val count = currentARActivity?.getSnapCount() ?: 0
                        result.success(count)
                    }
                    
                    // Clear all snaps
                    "clearAllSnaps" -> {
                        currentARActivity?.clearAllSnaps()
                        result.success(true)
                    }
                    
                    // Check if there are unsaved snaps
                    "hasUnsavedSnaps" -> {
                        val hasSnaps = (currentARActivity?.getSnapCount() ?: 0) > 0
                        result.success(hasSnaps)
                    }
                    
                    // Get pending snap paths (stored when AR activity finishes)
                    "getPendingSnapPaths" -> {
                        val paths = pendingSnapPaths ?: emptyList<String>()
                        Log.d("MainActivity", "getPendingSnapPaths called, returning ${paths.size} paths")
                        result.success(paths)
                    }
                    
                    // Check if Image Manager should be opened
                    "shouldOpenImageManager" -> {
                        val shouldOpen = pendingOpenImageManager
                        Log.d("MainActivity", "shouldOpenImageManager: $shouldOpen")
                        result.success(shouldOpen)
                    }
                    
                    // Clear pending snap paths after they've been used
                    "clearPendingSnapPaths" -> {
                        pendingSnapPaths = null
                        pendingOpenImageManager = false
                        result.success(true)
                    }
                    
                    else -> result.notImplemented()
                }
            }
    }
}

