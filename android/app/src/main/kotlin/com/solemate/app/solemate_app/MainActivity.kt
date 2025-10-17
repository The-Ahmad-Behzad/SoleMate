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

    // JNI (native) function – optional
    external fun startARSessionNative(): String

    companion object {
        init {
            try {
                System.loadLibrary("native-lib")
                Log.i("SoleMateNative", "native-lib loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("SoleMateNative", "Failed to load native-lib: ${e.message}")
            }
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

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
    }
}
