import 'package:flutter/material.dart';
import 'tts_service.dart';
import 'package:provider/provider.dart';
import 'navigation_service.dart';
import '../screens/catalog_screen.dart';

class VoiceCommandRouter {
  
  void handleCommand(String rawCommand, BuildContext context) {
    if (rawCommand.trim().isEmpty) return;

    final String command = rawCommand.toLowerCase().replaceAll(RegExp(r'[^\w\s]'), '').trim();
    final tts = Provider.of<TtsService>(context, listen: false);
    final nav = Provider.of<NavigationService>(context, listen: false);

    // Intent: Open AR Try On
    if (_matchesAny(command, ["open try on", "start try on", "open camera", "launch ar", "try on", "try on camera"])) {
      tts.speak("Opening try-on camera");
      nav.setIndex(0);
      nav.emitVoiceEvent('open_ar');
    }
    // Intent: View Catalogue
    else if (_matchesAny(command, ["show catalogue", "open catalogue", "view products", "catalogue"])) {
      tts.speak("Showing catalogue");
      Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CatalogScreen()));
    }
    // Intent: Show Recommendations
    else if (_matchesAny(command, ["show recommendations", "outfit recommendations", "suggest outfit", "recommendations"])) {
      tts.speak("Showing outfit recommendations");
      nav.setIndex(2);
    }
    // Intent: Save Item
    else if (_matchesAny(command, ["save this", "save item", "add to saved", "save"])) {
      tts.speak("Saving item to your profile");
      nav.setIndex(0);
      nav.emitVoiceEvent('save');
    }
    // Intent: Share Item
    else if (_matchesAny(command, ["share this", "share look", "share item", "share"])) {
      tts.speak("Ready to share");
      nav.emitVoiceEvent('share');
    }
    // Intent: Go Back
    else if (_matchesAny(command, ["go back", "back"])) {
      if (Navigator.canPop(context)) {
        Navigator.pop(context);
      } else {
        tts.speak("You are already on the first screen.");
      }
    }
    // Intent: Go Home
    else if (_matchesAny(command, ["go home", "home screen", "home"])) {
      tts.speak("Going home");
      Navigator.popUntil(context, (route) => route.isFirst);
      // OR if using named route: Navigator.pushReplacementNamed(context, '/home');
    }
    // Intent: Help
    else if (_matchesAny(command, ["help", "what can i say"])) {
      tts.speak("You can say: open camera, show catalogue, or go home.");
      _showHelpDialog(context);
    }
    // Unrecognized Command
    else {
      tts.speak("I didn't catch that. Please say help for options.");
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text("Unrecognized command: '$rawCommand'")),
      );
    }
  }

  bool _matchesAny(String command, List<String> synonyms) {
    for (String synonym in synonyms) {
      if (command.contains(synonym)) return true;
    }
    return false;
  }

  void _handleOpenAR(BuildContext context) {
    // Deprecated. Handled by generic Provider events now.
  }

  void _showHelpDialog(BuildContext context) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("Voice Commands"),
        content: const Text("Try saying:\n- Open try on camera\n- Show catalogue\n- Show recommendations\n- Save this\n- Share this\n- Go back\n- Go home"),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("Got it"),
          ),
        ],
      ),
    );
  }
}
