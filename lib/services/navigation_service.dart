import 'dart:async';
import 'package:flutter/material.dart';

class NavigationService extends ChangeNotifier {
  int _currentIndex = 0;
  int get currentIndex => _currentIndex;

  // Stream used to broadcast specific voice intents to active screens
  // e.g., 'save', 'share', 'open_ar'
  final StreamController<String> _voiceEventController = StreamController<String>.broadcast();
  Stream<String> get voiceEventStream => _voiceEventController.stream;

  void setIndex(int index) {
    if (_currentIndex != index) {
      _currentIndex = index;
      notifyListeners();
    }
  }

  void emitVoiceEvent(String event) {
    _voiceEventController.add(event);
  }

  @override
  void dispose() {
    _voiceEventController.close();
    super.dispose();
  }
}
