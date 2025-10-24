import 'package:flutter/material.dart';
import '../../services/auth_service.dart';
import '../../theme/theme_config.dart';
import '../../widgets/custom_button.dart';
import '../main_shell.dart';

class SignupScreen extends StatefulWidget {
  const SignupScreen({super.key});

  @override
  State<SignupScreen> createState() => _SignupScreenState();
}

class _SignupScreenState extends State<SignupScreen> {
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _authService = AuthService();
  bool _loading = false;

  void _signup() async {
    setState(() => _loading = true);
    try {
      await _authService.signUp(
        _emailController.text.trim(),
        _passwordController.text.trim(),
        _nameController.text.trim(),
      );
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
      appBar: AppBar(
        title: const Text('Create Account'),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
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
                const SizedBox(height: AppSpacing.xl2),
                
                // Logo/Title
                Container(
                  width: 100,
                  height: 100,
                  decoration: BoxDecoration(
                    color: AppColors.primaryForeground.withOpacity(0.1),
                    borderRadius: AppRadius.radiusFull,
                  ),
                  child: const Icon(
                    Icons.person_add,
                    size: 50,
                    color: AppColors.primaryForeground,
                  ),
                ),
                
                const SizedBox(height: AppSpacing.xl2),
                
                Text(
                  'Join SoleMate',
                  style: AppTypography.headline2.copyWith(
                    color: AppColors.primaryForeground,
                    fontWeight: AppTypography.bold,
                  ),
                ),
                
                const SizedBox(height: AppSpacing.sm),
                
                Text(
                  'Create your account to start your AR shoe journey',
                  style: AppTypography.bodyLarge.copyWith(
                    color: AppColors.primaryForeground.withOpacity(0.9),
                  ),
                  textAlign: TextAlign.center,
                ),
                
                const SizedBox(height: AppSpacing.xl3),
                
                // Signup Form Card
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
                        // Name Field
                        TextField(
                          controller: _nameController,
                          decoration: const InputDecoration(
                            labelText: 'Full Name',
                            prefixIcon: Icon(Icons.person_outlined),
                          ),
                          textInputAction: TextInputAction.next,
                        ),
                        
                        const SizedBox(height: AppSpacing.lg),
                        
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
                          onSubmitted: (_) => _signup(),
                        ),
                        
                        const SizedBox(height: AppSpacing.xl2),
                        
                        // Signup Button
                        _loading
                            ? const Center(child: CircularProgressIndicator())
                            : PrimaryButton(
                                text: 'Create Account',
                                onPressed: _signup,
                                size: CustomButtonSize.large,
                                isFullWidth: true,
                                isLoading: _loading,
                              ),
                      ],
                    ),
                  ),
                ),
                
                const SizedBox(height: AppSpacing.xl2),
                
                // Login Link
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: Text(
                    'Already have an account? Sign in',
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
