import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/voice_service.dart';
import '../services/voice_command_router.dart';

class VoiceMicButton extends StatelessWidget {
  final double size;
  
  const VoiceMicButton({
    super.key,
    this.size = 56.0,
  });

  @override
  Widget build(BuildContext context) {
    return Consumer<VoiceService>(
      builder: (context, voiceService, child) {
        final isListening = voiceService.isListening;
        final isAvailable = voiceService.speechEnabled;

        return GestureDetector(
          onTap: () {
            if (isListening) {
              voiceService.stopListening();
            } else {
              voiceService.startListening(
                onResult: (text) {
                  final router = Provider.of<VoiceCommandRouter>(context, listen: false);
                  router.handleCommand(text, context);
                },
              );
            }
          },
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 300),
            width: size,
            height: size,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: isListening 
                ? Theme.of(context).colorScheme.error 
                : Theme.of(context).colorScheme.primary,
              boxShadow: isListening
                  ? [
                      BoxShadow(
                        color: Theme.of(context).colorScheme.error.withOpacity(0.6),
                        blurRadius: 15,
                        spreadRadius: 5,
                      )
                    ]
                  : [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.2),
                        blurRadius: 5,
                        spreadRadius: 1,
                        offset: const Offset(0, 2),
                      )
                    ],
            ),
            child: Icon(
              isListening ? Icons.mic : Icons.mic_none,
              color: isAvailable ? Colors.white : Colors.white54,
              size: size * 0.5,
            ),
          ),
        );
      },
    );
  }
}
