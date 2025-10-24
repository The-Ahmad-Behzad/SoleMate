// import 'package:flutter/material.dart';
// import 'package:flutter/services.dart';
// import '../services/auth_service.dart';
// import 'auth/login_screen.dart';
// import 'package:permission_handler/permission_handler.dart';
//
// class HomeScreen extends StatefulWidget {
//   const HomeScreen({super.key});
//
//   @override
//   State<HomeScreen> createState() => _HomeScreenState();
// }
//
// class _HomeScreenState extends State<HomeScreen> {
//   final AuthService _authService = AuthService();
//   static const platform = MethodChannel('solemate/native'); // Channel to Android AR activity
//
//   /// 🔹 Opens AR Camera (invokes native Kotlin method `openARView`)
//   Future<void> _openARView() async {
//     try {
//       final result = await platform.invokeMethod('openARView');
//       debugPrint("AR view opened successfully: $result");
//     } on PlatformException catch (e) {
//       debugPrint("⚠️ Failed to open AR view: ${e.message}");
//       ScaffoldMessenger.of(context).showSnackBar(
//         SnackBar(content: Text("Failed to open AR view: ${e.message}")),
//       );
//     }
//   }
//
//   /// 🔹 Requests camera permission (if not granted)
//   Future<void> _checkPermissionsAndOpenAR() async {
//     final status = await Permission.camera.request();
//     if (status.isGranted) {
//       await _openARView();
//     } else {
//       ScaffoldMessenger.of(context).showSnackBar(
//         const SnackBar(content: Text("Camera permission denied.")),
//       );
//     }
//   }
//
//   @override
//   Widget build(BuildContext context) {
//     return Scaffold(
//       appBar: AppBar(
//         title: const Text('SoleMate AR Try-On'),
//         actions: [
//           IconButton(
//             icon: const Icon(Icons.logout),
//             tooltip: 'Logout',
//             onPressed: () async {
//               await _authService.logout();
//               if (context.mounted) {
//                 Navigator.pushReplacement(
//                   context,
//                   MaterialPageRoute(builder: (_) => const LoginScreen()),
//                 );
//               }
//             },
//           ),
//         ],
//       ),
//       body: const Center(
//         child: Text(
//           "👟 Tap the camera button below to start AR Try-On!",
//           textAlign: TextAlign.center,
//           style: TextStyle(fontSize: 16),
//         ),
//       ),
//       floatingActionButton: FloatingActionButton(
//         onPressed: _checkPermissionsAndOpenAR,
//         tooltip: 'Open AR Camera',
//         child: const Icon(Icons.camera_alt),
//       ),
//     );
//   }
// }


import 'package:flutter/material.dart';
import '../services/auth_service.dart';
import 'auth/login_screen.dart';
import '../ar/ar_main.dart'; // ✅ Import the AR module

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final AuthService _authService = AuthService();
  final ARMain _arMain = ARMain(); // ✅ Instance of AR functionality handler

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('SoleMate AR Try-On'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'Logout',
            onPressed: () async {
              await _authService.logout();
              if (context.mounted) {
                Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                );
              }
            },
          ),
        ],
      ),
      body: const Center(
        child: Text(
          "👟 Tap the camera button below to start AR Try-On!",
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 16),
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _arMain.checkPermissionsAndOpenAR(context),
        tooltip: 'Open AR Camera',
        child: const Icon(Icons.camera_alt),
      ),
    );
  }
}
