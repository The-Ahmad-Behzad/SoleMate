# 🧪 SoleMate Comprehensive Testing Checklist & Methodology

This task list breaks down the entire SoleMate project into logical testing categories and explicitly details **HOW** to execute each test.

---

## 🐍 Phase 1: Python ML Microservice (`outfit_rec/backend`)
*Targeting logic and API endpoints.*
**Tools Used:** `pytest` (Runner), `pytest-cov` (Coverage), `unittest.mock` (Mocking), `httpx` (API calls)

### 1. Utility & Business Logic (Unit Tests)
- [ ] **Color Extraction (`color_extractor.py`)**
  - *How to test:* Create small, synthetic 10x10 pixel numpy arrays (e.g., all `[255, 255, 255]` for white) and pass them to the extractor. Assert that `webcolors` correctly returns "white". Pass empty blocks to test error handling.
- [ ] **Feedback Engine (`feedback_engine.py`)**
  - *How to test:* Pass pre-constructed Python dictionary payloads representing outfits (e.g., `{"top": "red", "bottom": "blue"}`). Write `assert` statements checking if the scoring mechanism returns the expected integer logic.
- [ ] **Shoe Recommendation (`recommend.py`)**
  - *How to test:* Pass hardcoded user style preferences to the function. Assert that the string paths to the `.glb` files match the expected rules.

### 2. AI Model Wrappers (Integration Tests)
- [ ] **YOLO/Transformers Integrity**
  - *How to test:* Do NOT load the real `.pt` or `.onnx` files during CI (they are too heavy). Instead, use `@patch('ultralytics.YOLO')` from `unittest.mock` to intercept the YOLO call and force it to return a fake bounding box `[x1, y1, x2, y2]`. Assert your pipeline cleanly handles the data.

### 3. API Endpoints (`api.py`)
- [ ] **FastAPI TestClient endpoints**
  - *How to test:* Use `from fastapi.testclient import TestClient`. Send actual `client.post("/recommend", json={...})` requests inline. 
  - *Validation:* Assert the status code is `200 OK` and evaluate `response.json()` to ensure the backend wrapper behaves correctly.

---

## ⚙️ Phase 2: Node.js BFF (`backend/`)
*Targeting database operations, caching, and server routing.*
**Tools Used:** `Jest` (Runner/Mocker), `Supertest` (API caller)

### 1. Data Validation & Middleware
- [ ] **Zod Schema Tests**
  - *How to test:* Directly import your Zod schemas into a Jest test file. Call `Schema.safeParse(fakeData)`. Assert `success === true` for valid data and `success === false` for malformed data.
- [ ] **Authentication Middleware**
  - *How to test:* Create a mock Express `req, res, next` object suite. Call the middleware directly. If a header is missing, assert `res.status` was called with `401`. If valid, assert `next()` was called.

### 2. Controllers & Routes (Supertest Integration)
- [ ] **API Endpoints** 
  - *How to test:* Initialize your Express app in memory within Jest. Use `request(app).get('/api/users')`. 
  - *Validation:* Assert that the HTTP response code is correct and the JSON body matches your database fixtures. Use `jest.mock()` to prevent real S3 uploads when testing the Multer routes.

### 3. Database Services (Mongoose & Redis)
- [ ] **Mongoose Operations**
  - *How to test:* Use the `mongodb-memory-server` package. This spins up a fake MongoDB instance instantly in memory. Call `Outfit.save()`, then query it back out to assert the saving logic works. No real DB connection required.
- [ ] **Redis Caching (`ioredis`)**
  - *How to test:* Mock the `ioredis` library using `jest.mock('ioredis')`. Force it to return `null` (cache miss) and assert your DB function was called. Force it to return a JSON string (cache hit) and assert your DB function was *not* called using `expect(dbQuery).not.toHaveBeenCalled()`.

---

## 📱 Phase 3: Flutter Mobile App (`SoleMate/`)
*Targeting UI states, state management, and edge-cases.*
**Tools Used:** `flutter_test` (Runner), `mockito` or `mocktail` (Mocking)

### 1. Services & Repositories (Unit Tests)
- [ ] **Firebase Authentication Service**
  - *How to test:* Generate a mock class using Mockito: `class MockFirebaseAuth extends Mock implements FirebaseAuth {}`. Inject this mock into your Auth service. Use `when(mockAuth.signInWithEmail...).thenAnswer((_) => Future.value(mockUser))`. Assert your service returns success.
- [ ] **Device Capabilities (`image_picker`)**
  - *How to test:* Mock the `ImagePicker` platform channel. Simulate the user selecting an image and assert your repository passes the expected `File` object forward.

### 2. State Management (Provider Unit Tests)
- [ ] **Auth / UI Providers**
  - *How to test:* Instantiate your `ChangeNotifier` class in the test. Setup a `addListener` spy. Trigger a login function, wait, and assert that `notifyListeners()` was fired and the internal boolean `isLoading` flipped from true to false.
  
### 3. Screen & Widget Testing (Widget Tests)
- [ ] **Login/Sign-Up Screens**
  - *How to test:* Use `testWidgets` to pump the `LoginScreen()` directly into a headless test window. Use `await tester.enterText(find.byType(TextField), 'email@test.com')`. Tap the button using `await tester.tap(find.text('Login'))`.
  - *Validation:* Use `expect(find.text('Invalid Email'), findsOneWidget)` or assert navigation occurred.
- [ ] **AR Try-On View**
  - *How to test:* Complex native-views like AR cannot run in basic Widget tests. You must mock the AR View Widget entirely, substituting it with a generic `Container` during tests, and assert that the surrounding Flutter overlay elements (buttons/text) render correctly on top.

---

## 🚀 Phase 4: CI/CD Pipeline (Proof)
- [ ] **Automated Github / Gitlab checks**
  - *How to test:* Write a `.yml` workflow script. It should install dependencies, run `flutter test --coverage`, run `pytest --cov`, and run `npm test --coverage`. It must intentionally fail the build if any pipeline returns a non-zero exit code due to low coverage.
