import 'package:flutter/foundation.dart';
import 'package:speech_to_text/speech_recognition_result.dart';
import 'package:speech_to_text/speech_to_text.dart';
import 'package:permission_handler/permission_handler.dart';

class VoiceService extends ChangeNotifier {
  final SpeechToText _speechToText = SpeechToText();
  
  bool _speechEnabled = false;
  bool _isListening = false;
  String _lastRecognizedWords = "";

  bool get isListening => _isListening;
  String get lastRecognizedWords => _lastRecognizedWords;
  bool get speechEnabled => _speechEnabled;

  VoiceService() {
    _initSpeech();
  }

  /// Initialize the speech_to_text plugin.
  Future<void> _initSpeech() async {
    try {
      _speechEnabled = await _speechToText.initialize(
        onStatus: (status) {
          if (status == 'done' || status == 'notListening') {
            _handleStopListening();
          }
        },
        onError: (errorNotification) {
          debugPrint("Speech Error: ${errorNotification.errorMsg}");
          _handleStopListening();
        },
      );
    } catch (e) {
      debugPrint("Speech Initialization Error: $e");
      _speechEnabled = false;
    }
    notifyListeners();
  }

  /// Starts listening if permission is granted.
  Future<void> startListening({required Function(String) onResult}) async {
    if (!_speechEnabled) {
      await _initSpeech();
      if (!_speechEnabled) return;
    }
    
    // Ensure microphone permission is granted
    var status = await Permission.microphone.status;
    if (!status.isGranted) {
      status = await Permission.microphone.request();
      if (!status.isGranted) return;
    }

    _lastRecognizedWords = "";
    _isListening = true;
    notifyListeners();

    await _speechToText.listen(
      onResult: (SpeechRecognitionResult result) {
        _lastRecognizedWords = result.recognizedWords;
        // Check if it's the final result to trigger the callback
        if (result.finalResult) {
          onResult(_lastRecognizedWords);
          _handleStopListening();
        }
        notifyListeners();
      },
      listenFor: const Duration(seconds: 5),
      pauseFor: const Duration(seconds: 3),
      partialResults: true,
      cancelOnError: true,
      listenMode: ListenMode.confirmation,
    );
  }

  /// Manually stop listening
  Future<void> stopListening() async {
    await _speechToText.stop();
    _handleStopListening();
  }

  void _handleStopListening() {
    if (_isListening) {
      _isListening = false;
      notifyListeners();
    }
  }
}
