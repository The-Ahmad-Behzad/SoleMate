import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:image_picker/image_picker.dart';
import 'dart:io';

class MockImagePicker extends Mock implements ImagePicker {}
class MockXFile extends Mock implements XFile {}

void main() {
  late MockImagePicker mockImagePicker;

  setUp(() {
    mockImagePicker = MockImagePicker();
  });

  group('Device Capabilities (ImagePicker) Tests', () {
    test('Simulates user successfully picking an image from gallery', () async {
      final mockXFile = MockXFile();
      when(() => mockXFile.path).thenReturn('/test/path/to/image.png');
      when(() => mockImagePicker.pickImage(source: ImageSource.gallery))
          .thenAnswer((_) async => mockXFile);

      final pickedFile = await mockImagePicker.pickImage(source: ImageSource.gallery);
      
      expect(pickedFile, isNotNull);
      expect(pickedFile?.path, equals('/test/path/to/image.png'));
      verify(() => mockImagePicker.pickImage(source: ImageSource.gallery)).called(1);
      
      // Simulate conversion to dart:io File based on Phase 3 Checklist
      final file = File(pickedFile!.path);
      expect(file.path, equals('/test/path/to/image.png'));
    });

    test('Simulates user successfully capturing an image from camera', () async {
      final mockXFile = MockXFile();
      when(() => mockXFile.path).thenReturn('/test/path/to/camera_image.png');
      when(() => mockImagePicker.pickImage(source: ImageSource.camera))
          .thenAnswer((_) async => mockXFile);

      final pickedFile = await mockImagePicker.pickImage(source: ImageSource.camera);
      
      expect(pickedFile, isNotNull);
      expect(pickedFile?.path, equals('/test/path/to/camera_image.png'));
      verify(() => mockImagePicker.pickImage(source: ImageSource.camera)).called(1);
    });

    test('Simulates user cancelling image selection', () async {
      when(() => mockImagePicker.pickImage(source: ImageSource.gallery))
          .thenAnswer((_) async => null);

      final pickedFile = await mockImagePicker.pickImage(source: ImageSource.gallery);
      
      expect(pickedFile, isNull);
      verify(() => mockImagePicker.pickImage(source: ImageSource.gallery)).called(1);
    });
  });
}
