package com.solemate.app.solemate_app

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.util.Log

class MainActivity : FlutterActivity() {

    private val CHANNEL = "solemate/native"

    external fun startARSessionNative(): String

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "startARSession" -> {
                        try {
                            val response = startARSessionNative()
                            result.success(response)
                        } catch (e: Exception) {
                            Log.e("SoleMateNative", "Error: ${e.message}")
                            result.error("NATIVE_ERROR", e.message, null)
                        }
                    }
                    else -> result.notImplemented()
                }
            }
    }
}
