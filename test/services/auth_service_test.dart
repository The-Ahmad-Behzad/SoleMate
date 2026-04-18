import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:solemate_app/services/auth_service.dart';

class MockFirebaseAuth extends Mock implements FirebaseAuth {}
class MockFirebaseFirestore extends Mock implements FirebaseFirestore {}
class MockUserCredential extends Mock implements UserCredential {}
class MockUser extends Mock implements User {}
class MockCollectionReference extends Mock implements CollectionReference<Map<String, dynamic>> {}
class MockDocumentReference extends Mock implements DocumentReference<Map<String, dynamic>> {}

void main() {
  late AuthService authService;
  late MockFirebaseAuth mockAuth;
  late MockFirebaseFirestore mockDb;
  late MockUser mockUser;
  late MockUserCredential mockUserCredential;

  setUpAll(() {
    registerFallbackValue(<String, dynamic>{});
  });

  setUp(() {
    mockAuth = MockFirebaseAuth();
    mockDb = MockFirebaseFirestore();
    mockUser = MockUser();
    mockUserCredential = MockUserCredential();
    authService = AuthService(auth: mockAuth, db: mockDb);
    
    when(() => mockUser.uid).thenReturn('test-uid-123');
    when(() => mockUserCredential.user).thenReturn(mockUser);
  });

  group('AuthService Tests', () {
    test('signUp creates user and stores data in Firestore', () async {
      final mockCollection = MockCollectionReference();
      final mockDoc = MockDocumentReference();
      
      when(() => mockAuth.createUserWithEmailAndPassword(
              email: 'test@example.com', password: 'password123'))
          .thenAnswer((_) async => mockUserCredential);
          
      when(() => mockDb.collection('users')).thenReturn(mockCollection);
      when(() => mockCollection.doc('test-uid-123')).thenReturn(mockDoc);
      when(() => mockDoc.set(any())).thenAnswer((_) async => {});

      final user = await authService.signUp('test@example.com', 'password123', 'Test User');

      expect(user, equals(mockUser));
      verify(() => mockAuth.createUserWithEmailAndPassword(
          email: 'test@example.com', password: 'password123')).called(1);
      verify(() => mockDoc.set(any())).called(1);
    });

    test('login authenticates user successfully', () async {
      when(() => mockAuth.signInWithEmailAndPassword(
              email: 'test@example.com', password: 'password123'))
          .thenAnswer((_) async => mockUserCredential);

      final user = await authService.login('test@example.com', 'password123');

      expect(user, equals(mockUser));
      verify(() => mockAuth.signInWithEmailAndPassword(
          email: 'test@example.com', password: 'password123')).called(1);
    });

    test('login throws Exception on error', () async {
      when(() => mockAuth.signInWithEmailAndPassword(
              email: 'test@example.com', password: 'wrongpassword'))
          .thenThrow(FirebaseAuthException(code: 'user-not-found', message: 'User not found'));

      expect(
        () => authService.login('test@example.com', 'wrongpassword'),
        throwsA(isA<Exception>()),
      );
    });

    test('logout calls signOut on FirebaseAuth', () async {
      when(() => mockAuth.signOut()).thenAnswer((_) async => {});
      
      await authService.logout();
      
      verify(() => mockAuth.signOut()).called(1);
    });
  });
}
