import 'package:flutter/material.dart';
import '../../services/auth_service.dart';
import '../../theme/theme_config.dart';
import '../../widgets/custom_button.dart';
import '../main_shell.dart';
import 'signup_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _authService = AuthService();
  bool _loading = false;

  void _login() async {
    setState(() => _loading = true);
    try {
      await _authService.login(_emailController.text.trim(), _passwordController.text.trim());
      
      if (mounted) {
        Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const MainAppShell()));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return Scaffold(
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: BoxDecoration(
          gradient: isDark ? AppGradients.heroDark : AppGradients.heroLight,
        ),
        child: SafeArea(
          child: SingleChildScrollView(
            padding: AppSpacing.paddingLarge,
            child: Column(
              children: [
                const SizedBox(height: AppSpacing.xl4),
                
                // Logo/Title
                Container(
                  width: 120,
                  height: 120,
                  decoration: BoxDecoration(
                    color: AppColors.primaryForeground.withOpacity(0.1),
                    borderRadius: AppRadius.radiusFull,
                  ),
                  child: const Icon(
                    Icons.shopping_bag,
                    size: 60,
                    color: AppColors.primaryForeground,
                  ),
                ),
                
                const SizedBox(height: AppSpacing.xl2),
                
                Text(
                  'Welcome Back',
                  style: AppTypography.headline2.copyWith(
                    color: AppColors.primaryForeground,
                    fontWeight: AppTypography.bold,
                  ),
                ),
                
                const SizedBox(height: AppSpacing.sm),
                
                Text(
                  'Sign in to continue your AR shoe journey',
                  style: AppTypography.bodyLarge.copyWith(
                    color: AppColors.primaryForeground.withOpacity(0.9),
                  ),
                  textAlign: TextAlign.center,
                ),
                
                const SizedBox(height: AppSpacing.xl4),
                
                // Login Form Card
                Card(
                  elevation: 0,
                  shadowColor: AppColors.lightForeground.withOpacity(0.15),
                  shape: RoundedRectangleBorder(
                    borderRadius: AppRadius.radiusLarge,
                  ),
                  child: Container(
                    padding: AppSpacing.paddingLarge,
                    decoration: BoxDecoration(
                      borderRadius: AppRadius.radiusLarge,
                      gradient: isDark ? AppGradients.cardDark : AppGradients.cardLight,
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // Email Field
                        TextField(
                          controller: _emailController,
                          decoration: const InputDecoration(
                            labelText: 'Email',
                            prefixIcon: Icon(Icons.email_outlined),
                          ),
                          keyboardType: TextInputType.emailAddress,
                          textInputAction: TextInputAction.next,
                        ),
                        
                        const SizedBox(height: AppSpacing.lg),
                        
                        // Password Field
                        TextField(
                          controller: _passwordController,
                          decoration: const InputDecoration(
                            labelText: 'Password',
                            prefixIcon: Icon(Icons.lock_outlined),
                          ),
                          obscureText: true,
                          textInputAction: TextInputAction.done,
                          onSubmitted: (_) => _login(),
                        ),
                        
                        const SizedBox(height: AppSpacing.xl2),
                        
                        // Login Button
                        _loading
                            ? const Center(child: CircularProgressIndicator())
                            : PrimaryButton(
                                text: 'Sign In',
                                onPressed: _login,
                                size: CustomButtonSize.large,
                                isFullWidth: true,
                                isLoading: _loading,
                              ),
                      ],
                    ),
                  ),
                ),
                
                const SizedBox(height: AppSpacing.xl2),
                
                // Sign Up Link
                TextButton(
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => const SignupScreen()),
                  ),
                  child: Text(
                    'Don\'t have an account? Create one',
                    style: AppTypography.bodyMedium.copyWith(
                      color: AppColors.primaryForeground,
                    ),
                  ),
                ),
                
                const SizedBox(height: AppSpacing.xl4),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
