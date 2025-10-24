// import 'package:flutter/material.dart';
// import 'package:firebase_core/firebase_core.dart';
// import 'package:permission_handler/permission_handler.dart';
//
// Future<void> main() async {
//   WidgetsFlutterBinding.ensureInitialized();
//
//   // Initialize Firebase
//   await Firebase.initializeApp();
//
//   // Request permissions before launching the app
//   await _requestCameraPermission();
//
//   runApp(const MyApp());
// }
//
// Future<void> _requestCameraPermission() async {
//   var status = await Permission.camera.status;
//
//   if (status.isDenied || status.isRestricted) {
//     await Permission.camera.request();
//   }
//
//   // Optionally handle if permanently denied
//   if (await Permission.camera.isPermanentlyDenied) {
//     openAppSettings();
//   }
// }
//
// class MyApp extends StatelessWidget {
//   const MyApp({super.key});
//
//   @override
//   Widget build(BuildContext context) {
//     return MaterialApp(
//       title: 'SoleMate AR',
//       debugShowCheckedModeBanner: false,
//       theme: ThemeData(
//         colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
//         useMaterial3: true,
//       ),
//       home: const MyHomePage(title: 'SoleMate AR Home'),
//     );
//   }
// }
//
// class MyHomePage extends StatefulWidget {
//   const MyHomePage({super.key, required this.title});
//   final String title;
//
//   @override
//   State<MyHomePage> createState() => _MyHomePageState();
// }
//
// class _MyHomePageState extends State<MyHomePage> {
//   int _counter = 0;
//
//   void _incrementCounter() {
//     setState(() {
//       _counter++;
//     });
//   }
//
//   Future<void> _handleCameraAccess() async {
//     final status = await Permission.camera.status;
//      print("STATUS IS: $status");
//     if (status.isGranted) {
//       // TODO: Navigate to your AR screen here
//       ScaffoldMessenger.of(context).showSnackBar(
//         const SnackBar(content: Text('Camera access granted! Opening AR view...')),
//       );
//     } else {
//       await _requestCameraPermission();
//     }
//   }
//
//   @override
//   Widget build(BuildContext context) {
//     return Scaffold(
//       appBar: AppBar(
//         backgroundColor: Theme.of(context).colorScheme.inversePrimary,
//         title: Text(widget.title),
//       ),
//       body: Center(
//         child: Column(
//           mainAxisAlignment: MainAxisAlignment.center,
//           children: <Widget>[
//             const Text('You have opened the AR camera this many times:'),
//             Text('$_counter', style: Theme.of(context).textTheme.headlineMedium),
//           ],
//         ),
//       ),
//       floatingActionButton: FloatingActionButton(
//         onPressed: () async {
//           _incrementCounter();
//           await _handleCameraAccess();
//         },
//         tooltip: 'Open AR Camera',
//         child: const Icon(Icons.camera_alt),
//       ),
//     );
//   }
// }

//---------------------------------------------------------------------------------------------------------
//
// import 'package:flutter/material.dart';
// import 'package:firebase_core/firebase_core.dart';
// import 'package:permission_handler/permission_handler.dart';
// import 'package:flutter/services.dart';
//
// void main() async {
//   WidgetsFlutterBinding.ensureInitialized();
//   await Firebase.initializeApp();
//   await _requestCameraPermission();
//   runApp(const MyApp());
// }
//
// Future<void> _requestCameraPermission() async {
//   var status = await Permission.camera.status;
//   if (status.isDenied || status.isRestricted) await Permission.camera.request();
//   if (await Permission.camera.isPermanentlyDenied) openAppSettings();
// }
//
// class MyApp extends StatelessWidget {
//   const MyApp({super.key});
//
//   @override
//   Widget build(BuildContext context) {
//     return MaterialApp(
//       title: 'SoleMate AR',
//       debugShowCheckedModeBanner: false,
//       theme: ThemeData(
//         colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
//         useMaterial3: true,
//       ),
//       home: const MyHomePage(title: 'SoleMate AR Home'),
//     );
//   }
// }
//
// class MyHomePage extends StatefulWidget {
//   const MyHomePage({super.key, required this.title});
//   final String title;
//   @override
//   State<MyHomePage> createState() => _MyHomePageState();
// }
//
// class _MyHomePageState extends State<MyHomePage> {
//   static const platform = MethodChannel('solemate/native');
//
//   Future<void> _startARSession() async {
//     try {
//       final result = await platform.invokeMethod('startARSession');
//       ScaffoldMessenger.of(context).showSnackBar(
//         SnackBar(content: Text('Native says: $result')),
//       );
//     } on PlatformException catch (e) {
//       debugPrint("Failed to start AR: ${e.message}");
//     }
//   }
//
//   @override
//   Widget build(BuildContext context) {
//     return Scaffold(
//       appBar: AppBar(
//         backgroundColor: Theme.of(context).colorScheme.inversePrimary,
//         title: Text(widget.title),
//       ),
//       body: const Center(
//         child: Text("Press the camera button to start AR session."),
//       ),
//       floatingActionButton: FloatingActionButton(
//         onPressed: _startARSession,
//         tooltip: 'Open AR Camera',
//         child: const Icon(Icons.camera_alt),
//       ),
//     );
//   }
// }
//



//------------------------------------------------------------------------------------------------------


import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:permission_handler/permission_handler.dart';
import 'firebase_options.dart';
import 'theme/app_theme.dart';
import 'screens/landing_screen.dart';
import 'screens/splash_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
  await _requestCameraPermission();
  runApp(const MyApp());
}

Future<void> _requestCameraPermission() async {
  var status = await Permission.camera.status;
  if (status.isDenied || status.isRestricted) await Permission.camera.request();
  if (await Permission.camera.isPermanentlyDenied) openAppSettings();
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'SoleMate AR',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: ThemeMode.system,
      home: const LandingScreen(),
      routes: {
        '/landing': (context) => const LandingScreen(),
        '/splash': (context) => const SplashScreen(),
      },
    );
  }
}


// OLD TASHFEEN CODE
// class MyHomePage extends StatefulWidget {
//   const MyHomePage({super.key, required this.title});
//   final String title;
//   @override
//   State<MyHomePage> createState() => _MyHomePageState();
// }
//
// class _MyHomePageState extends State<MyHomePage> {
//   static const platform = MethodChannel('solemate/native');
//
//   /// 🔹 This now matches the Kotlin method name in MainActivity.kt
//   Future<void> _openARView() async {
//     try {
//       final result = await platform.invokeMethod('openARView');
//       print(result);
//     } on PlatformException catch (e) {
//       print("Failed to open AR view: ${e.message}");
//     }
//   }
//
//   @override
//   Widget build(BuildContext context) {
//     return Scaffold(
//       appBar: AppBar(
//         backgroundColor: Theme.of(context).colorScheme.inversePrimary,
//         title: Text(widget.title),
//       ),
//       body: const Center(
//         child: Text("Press the camera button to open AR view."),
//       ),
//       floatingActionButton: FloatingActionButton(
//         onPressed: _openARView, // ✅ updated call
//         tooltip: 'Open AR Camera',
//         child: const Icon(Icons.camera_alt),
//       ),
//     );
//   }
// }
//
