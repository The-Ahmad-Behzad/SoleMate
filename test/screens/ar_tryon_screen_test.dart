import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:solemate_app/screens/ar_tryon_screen.dart';

void main() {
  Widget createWidgetUnderTest() {
    return const MaterialApp(
      home: ARTryOnScreen(),
    );
  }

  testWidgets('AR Try-On Screen basic element presence test', (WidgetTester tester) async {
    // Phase 3 Checklist: Test AR UI wrapper handles generic AR Box
    // Use testWidgets to pump the AR Screen directly into a headless test window.
    await tester.pumpWidget(createWidgetUnderTest());

    // Verify Main Headers
    expect(find.text('AR Shoe Try-On'), findsOneWidget);
    expect(find.text('Experience shoes in augmented reality before you buy'), findsOneWidget);
    
    // Verify Camera Placeholder/Container overlay logic
    // This matches the fallback/generic container UI in ARTryOnScreen
    expect(find.text('Camera Preview'), findsOneWidget);
    expect(find.text('Tap "Enable AR" to start'), findsOneWidget);
    
    // AR Buttons Verification
    expect(find.text('Enable AR'), findsWidgets);
    expect(find.text('Reset'), findsWidgets);
    expect(find.text('Save'), findsWidgets);

    // Shoe selection area checking
    expect(find.text('Select Shoe'), findsOneWidget);
  });
}
