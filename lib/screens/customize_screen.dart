import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'dart:ui' as ui;
import 'dart:typed_data';
import 'dart:io';
import 'package:image_picker/image_picker.dart';
import 'package:http/http.dart' as http;
import 'package:painter/painter.dart';

import '../theme/theme_config.dart';
import '../widgets/custom_button.dart';
import '../widgets/logo_button.dart';
import '../services/auth_service.dart';
import '../services/api_client.dart';
import 'auth/login_screen.dart';

class CustomizeScreen extends StatefulWidget {
  const CustomizeScreen({super.key});

  @override
  State<CustomizeScreen> createState() => _CustomizeScreenState();
}

class _CustomizeScreenState extends State<CustomizeScreen> {
  final AuthService _authService = AuthService();
  final ApiClient _apiClient = ApiClient();
  final TextEditingController _descController = TextEditingController();

  final List<Color> _colors = [
    const Color(0xFF000000), const Color(0xFF8B4513), const Color(0xFFDC143C),
    const Color(0xFF0000FF), const Color(0xFF228B22), const Color(0xFF800080),
    const Color(0xFFFFFFFF), const Color(0xFFC0C0C0),
  ];

  int _selectedPrimaryColorIndex = 0;
  int? _selectedSecondaryColorIndex;

  List<String> _selectedImagePaths = [];
  Map<String, Uint8List> _editedImages = {}; // Maps original path to edited bytes
  
  bool _isSending = false;

  @override
  void dispose() {
    _descController.dispose();
    super.dispose();
  }

  Future<void> _logout() async {
    await _authService.logout();
    if (mounted) {
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const LoginScreen()));
    }
  }

  Future<void> _pickImages() async {
    final ImagePicker picker = ImagePicker();
    final List<XFile> images = await picker.pickMultiImage();
    if (images.isNotEmpty) {
      setState(() {
        for (var img in images) {
          if (!_selectedImagePaths.contains(img.path)) {
            _selectedImagePaths.add(img.path);
          }
        }
      });
    }
  }

  void _removeImage(int index) {
    setState(() {
      final path = _selectedImagePaths[index];
      _selectedImagePaths.removeAt(index);
      _editedImages.remove(path);
    });
  }

  void _openCanvasForImage(String path) async {
    final editedBytes = await Navigator.push<Uint8List?>(
      context,
      MaterialPageRoute(
        builder: (context) => FullScreenCanvas(
          imagePath: path,
          initialBytes: _editedImages[path],
        ),
      ),
    );

    if (editedBytes != null) {
      setState(() {
        _editedImages[path] = editedBytes;
      });
    }
  }

  Future<void> _submitRequest() async {
    if (_descController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Please add a description.')));
      return;
    }

    setState(() => _isSending = true);

    try {
      final List<http.MultipartFile> files = [];
      for (var path in _selectedImagePaths) {
        if (_editedImages.containsKey(path)) {
          files.add(http.MultipartFile.fromBytes('images', _editedImages[path]!, filename: 'edited_${DateTime.now().millisecondsSinceEpoch}.png'));
        } else {
          files.add(await http.MultipartFile.fromPath('images', path));
        }
      }

      String primaryColorHex = _colors[_selectedPrimaryColorIndex].value.toRadixString(16).substring(2);
      String? secondaryColorHex = _selectedSecondaryColorIndex != null 
          ? _colors[_selectedSecondaryColorIndex!].value.toRadixString(16).substring(2) 
          : null;

      final fields = {
        'shoeId': '65eaf15c0000000000000001', // Stub Shoe ID for Catalog
        'description': _descController.text,
        'primaryColor': '#$primaryColorHex',
      };
      
      if (secondaryColorHex != null) {
        fields['secondaryColor'] = '#$secondaryColorHex';
      }

      final response = await _apiClient.postMultipart(
        '/skins/request-redesign',
        fields: fields,
        files: files.isNotEmpty ? files : null,
        requiresAuth: true,
      );

      if (response.statusCode == 201 && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Redesign request sent successfully!')));
        setState(() {
          _selectedImagePaths.clear();
          _editedImages.clear();
          _descController.clear();
        });
      } else {
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Failed: ${response.statusCode}')));
      }
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error: $e')));
    } finally {
      if (mounted) setState(() => _isSending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        leading: const LogoButton(),
        title: const Text('Customize'),
        actions: [
          IconButton(icon: const Icon(Icons.logout), onPressed: _logout),
        ],
      ),
      body: SingleChildScrollView(
        padding: AppSpacing.paddingLarge,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Request Custom Redesign', style: AppTypography.headline2, textAlign: TextAlign.center),
            const SizedBox(height: AppSpacing.sm),
            Text('Upload images, draw your concept, and send it to our sellers!',
                style: AppTypography.bodyLarge.copyWith(color: isDark ? AppColors.darkMutedForeground : AppColors.lightMutedForeground),
                textAlign: TextAlign.center),
            const SizedBox(height: AppSpacing.xl),

            ElevatedButton.icon(
              onPressed: () {},
              icon: const Icon(Icons.shopping_bag),
              label: const Text('Select Shoe Base'),
            ),
            const SizedBox(height: AppSpacing.xl),

            Text('Uploaded Reference Images (Tap to Draw)', style: AppTypography.headline4),
            const SizedBox(height: AppSpacing.md),
            if (_selectedImagePaths.isEmpty)
              Container(
                height: 120,
                decoration: BoxDecoration(color: isDark ? AppColors.darkCard : AppColors.lightCard, borderRadius: AppRadius.radiusLarge),
                alignment: Alignment.center,
                child: const Text('No images uploaded yet.'),
              )
            else
              SizedBox(
                height: 120,
                child: ListView.builder(
                  scrollDirection: Axis.horizontal,
                  itemCount: _selectedImagePaths.length,
                  itemBuilder: (context, index) {
                    final path = _selectedImagePaths[index];
                    final hasEdits = _editedImages.containsKey(path);
                    return Stack(
                      children: [
                        GestureDetector(
                          onTap: () => _openCanvasForImage(path),
                          child: Container(
                            margin: const EdgeInsets.only(right: AppSpacing.md),
                            width: 120,
                            decoration: BoxDecoration(
                              borderRadius: AppRadius.radiusLarge,
                              border: Border.all(color: hasEdits ? AppColors.accent : Colors.transparent, width: 2),
                              image: DecorationImage(
                                image: hasEdits ? MemoryImage(_editedImages[path]!) : FileImage(File(path)) as ImageProvider,
                                fit: BoxFit.cover,
                              ),
                            ),
                          ),
                        ),
                        Positioned(
                          top: 4, right: 12,
                          child: GestureDetector(
                            onTap: () => _removeImage(index),
                            child: const CircleAvatar(radius: 12, backgroundColor: Colors.red, child: Icon(Icons.close, size: 14, color: Colors.white)),
                          ),
                        ),
                      ],
                    );
                  },
                ),
              ),
            const SizedBox(height: AppSpacing.md),
            PrimaryButton(text: 'Upload Images', onPressed: _pickImages, icon: Icons.image),

            const SizedBox(height: AppSpacing.xl2),
            Text('Primary Color (Mandatory)', style: AppTypography.headline4),
            const SizedBox(height: AppSpacing.md),
            _buildColorPicker(
                selectedIndex: _selectedPrimaryColorIndex,
                onSelected: (i) => setState(() => _selectedPrimaryColorIndex = i)),

            const SizedBox(height: AppSpacing.xl),
            Text('Secondary Color (Optional)', style: AppTypography.headline4),
            const SizedBox(height: AppSpacing.md),
            _buildColorPicker(
                selectedIndex: _selectedSecondaryColorIndex,
                allowNull: true,
                onSelected: (i) => setState(() => _selectedSecondaryColorIndex = _selectedSecondaryColorIndex == i ? null : i)),

            const SizedBox(height: AppSpacing.xl),
            Text('Description / Instructions', style: AppTypography.headline4),
            const SizedBox(height: AppSpacing.md),
            TextField(
              controller: _descController,
              maxLines: 4,
              decoration: InputDecoration(
                hintText: 'e.g. Please put the logo on the side and make the laces match the primary color.',
                border: OutlineInputBorder(borderRadius: AppRadius.radiusLarge),
              ),
            ),
            
            const SizedBox(height: AppSpacing.xl2),
            PrimaryButton(text: _isSending ? 'Sending Request...' : 'Send Request', onPressed: _isSending ? null : _submitRequest, isFullWidth: true),
            const SizedBox(height: AppSpacing.xl),
          ],
        ),
      ),
    );
  }

  Widget _buildColorPicker({required int? selectedIndex, required Function(int) onSelected, bool allowNull = false}) {
    return Wrap(
      spacing: AppSpacing.md,
      runSpacing: AppSpacing.md,
      children: List.generate(_colors.length, (index) {
        final color = _colors[index];
        final isSelected = selectedIndex == index;
        return GestureDetector(
          onTap: () => onSelected(index),
          child: Container(
            width: 45, height: 45,
            decoration: BoxDecoration(
              color: color,
              shape: BoxShape.circle,
              border: Border.all(color: isSelected ? AppColors.accent : Colors.grey, width: isSelected ? 3 : 1),
            ),
            child: isSelected ? const Icon(Icons.check, color: Colors.blueAccent) : null,
          ),
        );
      }),
    );
  }
}

class FullScreenCanvas extends StatefulWidget {
  final String imagePath;
  final Uint8List? initialBytes;

  const FullScreenCanvas({super.key, required this.imagePath, this.initialBytes});

  @override
  State<FullScreenCanvas> createState() => _FullScreenCanvasState();
}

class _FullScreenCanvasState extends State<FullScreenCanvas> {
  late PainterController _controller;
  final GlobalKey _globalKey = GlobalKey();

  @override
  void initState() {
    super.initState();
    _controller = PainterController();
    _controller.thickness = 5.0;
    _controller.backgroundColor = Colors.transparent;
  }

  Future<void> _saveAndExit() async {
    try {
      RenderRepaintBoundary boundary = _globalKey.currentContext!.findRenderObject() as RenderRepaintBoundary;
      ui.Image image = await boundary.toImage(pixelRatio: 3.0);
      ByteData? byteData = await image.toByteData(format: ui.ImageByteFormat.png);
      Uint8List pngBytes = byteData!.buffer.asUint8List();
      if (mounted) Navigator.pop(context, pngBytes);
    } catch (e) {
      if (mounted) Navigator.pop(context, null);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        title: const Text('Draw on Image', style: TextStyle(color: Colors.white)),
        iconTheme: const IconThemeData(color: Colors.white),
        actions: [
          IconButton(icon: const Icon(Icons.undo), onPressed: () => _controller.isEmpty ? null : _controller.undo()),
          IconButton(icon: const Icon(Icons.delete), onPressed: () => _controller.clear()),
          IconButton(icon: const Icon(Icons.check, color: Colors.green), onPressed: _saveAndExit),
        ],
      ),
      body: Center(
        child: RepaintBoundary(
          key: _globalKey,
          child: Stack(
            fit: StackFit.loose,
            children: [
              widget.initialBytes != null
                  ? Image.memory(widget.initialBytes!, fit: BoxFit.contain)
                  : Image.file(File(widget.imagePath), fit: BoxFit.contain),
              Positioned.fill(
                child: Painter(_controller),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
