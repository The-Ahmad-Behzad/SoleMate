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
        
        // Channel reference for calling Flutter from native
        var nativeChannel: MethodChannel? = null
        
        // Function to notify Flutter to open Image Manager with provided paths
        fun notifyFlutterToOpenImageManager(paths: List<String>) {
            pendingSnapPaths = paths
            pendingOpenImageManager = true
            Log.d("MainActivity", "Notifying Flutter to open Image Manager with ${paths.size} paths")
            
            // Call Flutter method channel
            nativeChannel?.invokeMethod("onOpenImageManager", mapOf("snapPaths" to paths))
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // Main AR channel
        val methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        
        // Store channel reference for AR activity to call back
        nativeChannel = methodChannel
        
        methodChannel.setMethodCallHandler { call, result ->
                when (call.method) {

                    // 🚀 Flutter calls this to open AR Activity
                    "openARView" -> {
                        val intent = Intent(this, ARActivity::class.java)
                        startActivity(intent)
                        result.success("Opened AR View")
                    }
                    
                    // 🔙 Flutter calls this to return to AR Activity (if it's still alive)
                    "returnToAR" -> {
                        if (currentARActivity != null) {
                            // AR Activity is still alive, bring it to foreground
                            val intent = Intent(this, ARActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                            startActivity(intent)
                            result.success(true)
                        } else {
                            // AR Activity was destroyed, need to start new session
                            result.success(false)
                        }
                    }
                    
                    // 👟 Flutter calls this to set the shoe model before opening AR
                    "setSelectedShoeModel" -> {
                        val modelPath = call.argument<String>("modelPath")
                        if (modelPath != null) {
                            SelectedShoeManager.setSelectedModel(modelPath)
                            Log.d("MainActivity", "Set selected shoe model: $modelPath")
                            result.success(true)
                        } else {
                            result.error("INVALID_ARGS", "modelPath is required", null)
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
                    
                    // Delete a snap by its file path
                    "deleteSnapByPath" -> {
                        val path = call.argument<String>("path")
                        if (path != null) {
                            val success = currentARActivity?.deleteSnapByPath(path) ?: false
                            Log.d("MainActivity", "deleteSnapByPath: $path -> $success")
                            result.success(success)
                        } else {
                            result.error("INVALID_ARGS", "path is required", null)
                        }
                    }
                    
                    // Set filter for a specific snap path
                    "setFilter" -> {
                        val path = call.argument<String>("path")
                        val filterId = call.argument<String>("filterId")
                        if (path != null) {
                            currentARActivity?.setFilter(path, filterId)
                            result.success(true)
                        } else {
                            result.error("INVALID_ARGS", "path is required", null)
                        }
                    }
                    
                    // Get all filters as a map of path -> filterId
                    "getFilters" -> {
                        val filters = currentARActivity?.getFilters() ?: emptyMap<String, String?>()
                        result.success(filters)
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
                    
                    // Save image to gallery using MediaStore
                    "saveToGallery" -> {
                        val imagePath = call.argument<String>("imagePath")
                        if (imagePath != null) {
                            try {
                                val file = java.io.File(imagePath)
                                if (file.exists()) {
                                    val contentValues = android.content.ContentValues().apply {
                                        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, file.name)
                                        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SoleMate")
                                    }
                                    
                                    val uri = contentResolver.insert(
                                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues
                                    )
                                    
                                    if (uri != null) {
                                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                                            java.io.FileInputStream(file).use { inputStream ->
                                                inputStream.copyTo(outputStream)
                                            }
                                        }
                                        Log.d("MainActivity", "Saved to gallery: $uri")
                                        result.success(uri.toString())
                                    } else {
                                        result.error("SAVE_ERROR", "Failed to create MediaStore entry", null)
                                    }
                                } else {
                                    result.error("FILE_NOT_FOUND", "Image file not found", null)
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error saving to gallery", e)
                                result.error("SAVE_ERROR", e.message, null)
                            }
                        } else {
                            result.error("INVALID_ARGS", "imagePath is required", null)
                        }
                    }
                    
                    // Share to Instagram Stories
                    "shareToInstagram" -> {
                        val imagePath = call.argument<String>("imagePath")
                        if (imagePath != null) {
                            try {
                                val file = java.io.File(imagePath)
                                if (file.exists()) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        this,
                                        "$packageName.fileprovider",
                                        file
                                    )
                                    
                                    // Use regular share intent to let user choose (Feed, Story, Chat, etc.)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        setPackage("com.instagram.android")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    
                                    if (intent.resolveActivity(packageManager) != null) {
                                        startActivity(intent)
                                        result.success(true)
                                    } else {
                                        result.error("APP_NOT_FOUND", "Instagram not installed", null)
                                    }
                                } else {
                                    result.error("FILE_NOT_FOUND", "Image file not found", null)
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error sharing to Instagram", e)
                                result.error("SHARE_ERROR", e.message, null)
                            }
                        } else {
                            result.error("INVALID_ARGS", "imagePath is required", null)
                        }
                    }
                    
                    // Share to Facebook
                    "shareToFacebook" -> {
                        val imagePath = call.argument<String>("imagePath")
                        if (imagePath != null) {
                            try {
                                val file = java.io.File(imagePath)
                                if (file.exists()) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        this,
                                        "$packageName.fileprovider",
                                        file
                                    )
                                    
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        setPackage("com.facebook.katana")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    
                                    if (intent.resolveActivity(packageManager) != null) {
                                        startActivity(intent)
                                        result.success(true)
                                    } else {
                                        result.error("APP_NOT_FOUND", "Facebook not installed", null)
                                    }
                                } else {
                                    result.error("FILE_NOT_FOUND", "Image file not found", null)
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error sharing to Facebook", e)
                                result.error("SHARE_ERROR", e.message, null)
                            }
                        } else {
                            result.error("INVALID_ARGS", "imagePath is required", null)
                        }
                    }
                    
                    // Native share intent (share to any app)
                    "nativeShare" -> {
                        val imagePath = call.argument<String>("imagePath")
                        if (imagePath != null) {
                            try {
                                val file = java.io.File(imagePath)
                                if (file.exists()) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        this,
                                        "$packageName.fileprovider",
                                        file
                                    )
                                    
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    
                                    startActivity(Intent.createChooser(intent, "Share Image"))
                                    result.success(true)
                                } else {
                                    result.error("FILE_NOT_FOUND", "Image file not found", null)
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error sharing", e)
                                result.error("SHARE_ERROR", e.message, null)
                            }
                        } else {
                            result.error("INVALID_ARGS", "imagePath is required", null)
                        }
                    }
                    
                    else -> result.notImplemented()
                }
            }
    }
}

