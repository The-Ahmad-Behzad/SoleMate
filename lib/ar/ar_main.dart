import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

/// Handles AR-related logic (permissions + platform channel)
class ARMain {
  static const platform = MethodChannel('solemate/native'); // Channel to native Android AR activity

  /// 🔹 Opens AR Camera via native Kotlin function
  Future<void> openARView(BuildContext context) async {
    try {
      final result = await platform.invokeMethod('openARView');
      debugPrint("✅ AR view opened successfully: $result");
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to open AR view: ${e.message}");
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Failed to open AR view: ${e.message}")),
      );
    }
  }

  /// 🔹 Checks for camera permissions before opening AR view
  Future<void> checkPermissionsAndOpenAR(BuildContext context) async {
    final status = await Permission.camera.request();
    if (status.isGranted) {
      await openARView(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Camera permission denied.")),
      );
    }
  }
}
