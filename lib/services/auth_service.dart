import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';

class AuthService {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final FirebaseFirestore _db = FirebaseFirestore.instance;

  Stream<User?> get userStream => _auth.authStateChanges();

  Future<User?> signUp(String email, String password, String name) async {
    try {
      final creds = await _auth.createUserWithEmailAndPassword(email: email, password: password);
      await _db.collection('users').doc(creds.user!.uid).set({
        'uid': creds.user!.uid,
        'email': email,
        'name': name,
        'createdAt': DateTime.now(),
      });
      return creds.user;
    } on FirebaseAuthException catch (e) {
      throw Exception(e.message);
    }
  }

  Future<User?> login(String email, String password) async {
    try {
      final creds = await _auth.signInWithEmailAndPassword(email: email, password: password);
      return creds.user;
    } on FirebaseAuthException catch (e) {
      throw Exception(e.message);
    }
  }

  Future<void> logout() async => await _auth.signOut();
}
