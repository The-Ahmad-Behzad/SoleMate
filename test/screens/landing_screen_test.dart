import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:solemate_app/screens/landing_screen.dart';
import 'package:solemate_app/widgets/custom_button.dart';
import 'package:solemate_app/theme/theme_config.dart';

void main() {
  testWidgets('LandingScreen renders SoleMate title and CTA buttons', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    // Use a MaterialApp wrapper to provide the necessary themes and media queries
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData(brightness: Brightness.light),
        home: const LandingScreen(),
      ),
    );

    // 1. Check for the Logo/Title
    expect(find.text('SoleMate'), findsOneWidget);

    // 2. Check for the Subtitle
    expect(find.text('AR Shoe Try-On Experience'), findsOneWidget);

    // 3. Check for the Primary Button text
    expect(find.text('Start AR Try-On'), findsOneWidget);

    // 4. Check for the Features Section Title
    await tester.scrollUntilVisible(find.text('Why Choose SoleMate?'), 200);
    expect(find.text('Why Choose SoleMate?'), findsOneWidget);
  });
}
