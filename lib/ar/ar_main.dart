import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import '../screens/image_manager_screen.dart';

/// Handles AR-related logic (permissions + platform channel)
class ARMain {
  static const platform = MethodChannel('solemate/native'); // Channel to native Android AR activity
  static const screenshotChannel = MethodChannel('solemate/screenshots'); // Channel for screenshot management

  // Store context for navigation callbacks
  BuildContext? _lastContext;
  
  /// Initialize the platform channel handler for callbacks from native
  void initMethodCallHandler(BuildContext context) {
    _lastContext = context;
    platform.setMethodCallHandler((call) async {
      if (call.method == 'openImageManager') {
        final List<dynamic>? paths = call.arguments['snapPaths'];
        if (paths != null && paths.isNotEmpty && _lastContext != null && _lastContext!.mounted) {
          _navigateToImageManager(_lastContext!, paths.cast<String>());
        }
      }
    });
  }
  
  void _navigateToImageManager(BuildContext context, List<String> paths) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => ImageManagerScreen(
          imagePaths: paths,
          onImagesChanged: () {
            debugPrint("Images changed in Image Manager");
          },
        ),
      ),
    );
  }

  /// 🔹 Opens AR Camera via native Kotlin function
  Future<void> openARView(BuildContext context) async {
    _lastContext = context;
    try {
      final result = await platform.invokeMethod('openARView');
      debugPrint("✅ AR view opened successfully: $result");
      
      // After AR view closes, check if we should open Image Manager
      await _checkAndOpenImageManager(context);
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to open AR view: ${e.message}");
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Failed to open AR view: ${e.message}")),
      );
    }
  }
  
  /// Check if there are snaps to show after AR session ends
  Future<void> _checkAndOpenImageManager(BuildContext context) async {
    try {
      // Check if AR session set pending snaps for Image Manager
      final bool shouldOpen = await screenshotChannel.invokeMethod('shouldOpenImageManager');
      debugPrint("shouldOpenImageManager: $shouldOpen");
      
      if (shouldOpen && context.mounted) {
        // Get the pending snap paths
        final List<dynamic> pathsDynamic = await screenshotChannel.invokeMethod('getPendingSnapPaths');
        final List<String> paths = pathsDynamic.cast<String>();
        debugPrint("Got ${paths.length} pending snap paths");
        
        if (paths.isNotEmpty && context.mounted) {
          // Clear pending state
          await screenshotChannel.invokeMethod('clearPendingSnapPaths');
          
          // Small delay to ensure proper navigation
          await Future.delayed(const Duration(milliseconds: 100));
          if (context.mounted) {
            _navigateToImageManager(context, paths);
          }
        }
      }
    } catch (e) {
      debugPrint("Error checking for snaps: $e");
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
  
  // ==== Screenshot Management Methods ====
  
  /// Get all captured snap file paths from native AR session
  Future<List<String>> getSnapPaths() async {
    try {
      final List<dynamic> paths = await screenshotChannel.invokeMethod('getSnapPaths');
      return paths.cast<String>();
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to get snap paths: ${e.message}");
      return [];
    }
  }
  
  /// Get current snap count
  Future<int> getSnapCount() async {
    try {
      final int count = await screenshotChannel.invokeMethod('getSnapCount');
      return count;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to get snap count: ${e.message}");
      return 0;
    }
  }
  
  /// Clear all captured snaps
  Future<bool> clearAllSnaps() async {
    try {
      final bool result = await screenshotChannel.invokeMethod('clearAllSnaps');
      return result;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to clear snaps: ${e.message}");
      return false;
    }
  }
  
  /// Check if there are unsaved snaps
  Future<bool> hasUnsavedSnaps() async {
    try {
      final bool hasSnaps = await screenshotChannel.invokeMethod('hasUnsavedSnaps');
      return hasSnaps;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to check unsaved snaps: ${e.message}");
      return false;
    }
  }
  
  /// Open the Image Manager screen with current snaps
  Future<void> openImageManager(BuildContext context) async {
    try {
      final paths = await getSnapPaths();
      if (paths.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('No snaps to view')),
        );
        return;
      }
      
      // Navigate to Image Manager screen
      if (context.mounted) {
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (context) => ImageManagerScreen(
              imagePaths: paths,
              onImagesChanged: () {
                // Optionally sync changes back to native
                debugPrint("Images changed in Image Manager");
              },
            ),
          ),
        );
      }
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to open Image Manager: ${e.message}");
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Failed to open Image Manager: ${e.message}")),
      );
    }
  }
}
