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
    debugPrint("✅ ARMain: Setting up method call handler for native callbacks");
    platform.setMethodCallHandler((call) async {
      debugPrint("📞 ARMain received method call: ${call.method}");
      if (call.method == 'onOpenImageManager') {
        final Map<dynamic, dynamic>? args = call.arguments as Map<dynamic, dynamic>?;
        final List<dynamic>? paths = args?['snapPaths'];
        debugPrint("📞 onOpenImageManager called with ${paths?.length ?? 0} paths");
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

  /// 🔹 Set the selected shoe model path before opening AR
  Future<bool> setSelectedShoeModel(String modelPath) async {
    try {
      final bool success = await platform.invokeMethod(
        'setSelectedShoeModel',
        {'modelPath': modelPath},
      );
      debugPrint("👟 setSelectedShoeModel: $modelPath -> $success");
      return success;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to set selected shoe model: ${e.message}");
      return false;
    }
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
  
  /// 🔹 Opens AR Camera with a specific shoe model
  Future<void> openARViewWithShoe(BuildContext context, String modelPath) async {
    // First set the selected model
    await setSelectedShoeModel(modelPath);
    // Then open AR view
    await openARView(context);
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
    // Initialize handler for native callbacks (must be set up before AR opens)
    initMethodCallHandler(context);
    
    final status = await Permission.camera.request();
    if (status.isGranted) {
      await openARView(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Camera permission denied.")),
      );
    }
  }
  
  /// 🔙 Returns to AR Activity if it's still alive in background
  Future<bool> returnToAR() async {
    try {
      final bool success = await platform.invokeMethod('returnToAR');
      debugPrint("✅ returnToAR: $success");
      return success;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to return to AR: ${e.message}");
      return false;
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
  
  /// Delete a snap by its file path (syncs with native ScreenshotManager)
  Future<bool> deleteSnapByPath(String path) async {
    try {
      final bool success = await screenshotChannel.invokeMethod(
        'deleteSnapByPath',
        {'path': path},
      );
      debugPrint("🗑️ deleteSnapByPath: $path -> $success");
      return success;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to delete snap: ${e.message}");
      return false;
    }
  }
  
  /// Set filter for a specific snap path (persists to native)
  Future<bool> setFilter(String path, String? filterId) async {
    try {
      final bool success = await screenshotChannel.invokeMethod(
        'setFilter',
        {'path': path, 'filterId': filterId},
      );
      debugPrint("🎨 setFilter: $path -> $filterId");
      return success;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to set filter: ${e.message}");
      return false;
    }
  }
  
  /// Get all applied filters as a map of path -> filterId
  Future<Map<String, String?>> getFilters() async {
    try {
      final Map<dynamic, dynamic> result = await screenshotChannel.invokeMethod('getFilters');
      final filters = <String, String?>{};
      result.forEach((key, value) {
        filters[key.toString()] = value?.toString();
      });
      debugPrint("🎨 getFilters: ${filters.length} filters");
      return filters;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to get filters: ${e.message}");
      return {};
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
  
  /// Save image to device gallery using MediaStore API
  Future<String?> saveToGallery(String imagePath) async {
    try {
      final String? result = await screenshotChannel.invokeMethod(
        'saveToGallery',
        {'imagePath': imagePath},
      );
      debugPrint("💾 saveToGallery: $imagePath -> $result");
      return result;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to save to gallery: ${e.message}");
      return null;
    }
  }
  
  /// Share image to Instagram
  Future<bool> shareToInstagram(String imagePath) async {
    try {
      final bool success = await screenshotChannel.invokeMethod(
        'shareToInstagram',
        {'imagePath': imagePath},
      );
      debugPrint("📸 shareToInstagram: $imagePath -> $success");
      return success;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to share to Instagram: ${e.message}");
      throw e;
    }
  }
  
  /// Share image to Facebook
  Future<bool> shareToFacebook(String imagePath) async {
    try {
      final bool success = await screenshotChannel.invokeMethod(
        'shareToFacebook',
        {'imagePath': imagePath},
      );
      debugPrint("📘 shareToFacebook: $imagePath -> $success");
      return success;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to share to Facebook: ${e.message}");
      throw e;
    }
  }
  
  /// Share image using native share dialog
  Future<bool> nativeShare(String imagePath) async {
    try {
      final bool success = await screenshotChannel.invokeMethod(
        'nativeShare',
        {'imagePath': imagePath},
      );
      debugPrint("📤 nativeShare: $imagePath -> $success");
      return success;
    } on PlatformException catch (e) {
      debugPrint("⚠️ Failed to native share: ${e.message}");
      throw e;
    }
  }
}
