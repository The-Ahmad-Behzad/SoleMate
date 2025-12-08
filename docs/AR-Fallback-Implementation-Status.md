# AR Fallback Implementation Status

## Overview

This document tracks the implementation status of the AR fallback system for non-ARCore devices. The goal is to enable markerless AR shoe try-on on devices that don't support Google ARCore.

## Current Status: **Phase 1 - Camera Preview (In Progress)**

The implementation is currently focused on getting the camera preview working correctly. Once this is stable, we'll proceed with plane detection, foot tracking, and 3D model placement.

---

## ✅ Completed Work

### 1. Architecture Setup

**What:** Created dual-path architecture to support both ARCore and fallback modes.

**Where:**
- `android/app/src/main/kotlin/com/solemate/app/solemate_app/ARActivity.kt`
  - Added `ARMode` enum (ARCore vs VIO)
  - Added `checkARCapability()` method to detect ARCore availability
  - Modified `startARSession()` to route to appropriate mode
  - Added `startVIOSession()` for fallback mode initialization

**How:**
- On app start, `ARActivity.onCreate()` calls `checkARCapability()`
- If ARCore is available, uses `ArcorePoseProvider` and ARCore Session
- If ARCore is unavailable, uses `SimpleCameraPoseProvider` and `SimpleCameraManager`

---

### 2. Simplified Camera Manager

**What:** Created `SimpleCameraManager` - a simplified Camera2 API wrapper focused only on camera preview.

**Where:**
- `android/app/src/main/kotlin/com/solemate/app/solemate_app/SimpleCameraManager.kt` (new file)

**How:**
- Uses Android Camera2 API to open back camera
- Streams camera frames to `SurfaceTexture` for OpenGL rendering
- Handles camera lifecycle (open, capture, close)
- Provides sensor orientation information for rotation correction
- Chooses optimal preview size (prefers 640x480)

**Key Features:**
- Background thread for camera operations
- Proper error handling and logging
- Camera state callbacks
- Capture session management

---

### 3. Simple Camera Pose Provider

**What:** Created `SimpleCameraPoseProvider` - a basic pose provider using camera preview and IMU sensors.

**Where:**
- `android/app/src/main/kotlin/com/solemate/app/solemate_app/SimpleCameraPoseProvider.kt` (new file)

**How:**
- Implements `WorldPoseProvider` interface (same interface as ARCore)
- Uses IMU sensors (accelerometer + gyroscope) for orientation tracking
- Provides camera intrinsics for projection matrix calculation
- Implements basic hit testing (assumes floor plane at y=0)
- Returns simple floor plane (5m x 5m)

**Key Features:**
- IMU-based rotation tracking
- Camera intrinsics management
- Basic hit test (ray-plane intersection with floor)
- Simple plane estimation (static floor plane)

---

### 4. Renderer Integration

**What:** Updated `SimpleRenderer` to support both ARCore and SimpleCamera modes.

**Where:**
- `android/app/src/main/kotlin/com/solemate/app/solemate_app/SimpleRenderer.kt`

**How:**
- Added detection for `SimpleCameraPoseProvider` in `onDrawFrame()`
- Calls `updateTexImage()` on GL thread to update camera texture
- Applies rotation correction based on device orientation
- Renders camera feed using `BackgroundRenderer.drawCamera2Texture()`
- Gets view/projection matrices from pose provider

**Key Changes:**
- Added camera initialization timeout detection
- Added extensive logging for debugging
- Handles `SurfaceTexture` lifecycle on GL thread
- Applies texture rotation correction (currently being debugged)

---

### 5. Camera Permissions

**What:** Added runtime camera permission handling.

**Where:**
- `android/app/src/main/kotlin/com/solemate/app/solemate_app/ARActivity.kt`
  - `checkCameraPermission()` method
  - `requestCameraPermission()` method
  - `onRequestPermissionsResult()` handler

**How:**
- Checks for `CAMERA` permission in `onCreate()`
- Requests permission if not granted
- Continues initialization after permission is granted

---

### 6. TextureView Overlay Fix

**What:** Fixed "strange frame" overlay issue by hiding TextureView in VIO mode.

**Where:**
- `android/app/src/main/kotlin/com/solemate/app/solemate_app/ARActivity.kt`
  - `initializeAR()`: Sets `textureView?.visibility = View.GONE` by default
  - `startARCoreSession()`: Sets `textureView?.visibility = View.VISIBLE` when ShoeRenderer is used
  - `startVIOSession()`: Ensures `textureView` remains `GONE`

**How:**
- TextureView is only visible in ARCore mode (for Filament rendering)
- In VIO/SimpleCamera mode, only GLSurfaceView is used for camera preview

---

### 7. Code Backup

**What:** Backed up original VIO implementation files.

**Where:**
- `android/app/src/main/kotlin/com/solemate/app/solemate_app/vio_backup/` directory
- `android/app/src/main/cpp/vio/` (native C++ VIO code - still exists but not used)

**Files Backed Up:**
- `VioEngine.kt`
- `VioPoseProvider.kt`
- `Camera2Manager.kt` (original complex version)
- `PlaneEstimator.kt`

---

## 🔄 In Progress / Known Issues

### 1. Camera Rotation Issue (CRITICAL)

**Status:** Active debugging

**Problem:** Camera feed appears horizontal/landscape when device is in portrait mode.

**Location:**
- `android/app/src/main/kotlin/com/solemate/app/solemate_app/SimpleRenderer.kt` (lines 615-642)

**Current Approach:**
- Applying rotation correction based on `deviceRotation` (0°, 90°, 180°, 270°)
- For portrait (0°): Applying 90° clockwise rotation
- Using `applyTextureRotation()` to transform the SurfaceTexture matrix

**Debugging:**
- Added logging for `deviceRotation`, `sensorOrientation`, and matrix values
- Testing different rotation values (90°, 270°, no rotation)

**Next Steps:**
- Analyze logcat output to determine correct rotation
- May need to adjust rotation based on `sensorOrientation` + `deviceRotation`
- Consider using SurfaceTexture's built-in rotation handling

---

## ❌ Not Yet Implemented

### Phase 2: Plane Detection

**What:** Detect horizontal floor planes using visual features.

**Status:** Not started

**Required:**
- Feature detection (ORB, SIFT, or similar)
- RANSAC-based plane fitting
- Integration with `SimpleCameraPoseProvider.getPlanes()`

**Files to Modify:**
- `SimpleCameraPoseProvider.kt` - Update `getPlanes()` to return detected planes
- Potentially create `SimplePlaneDetector.kt` for plane detection logic

---

### Phase 3: Foot/Object Detection

**What:** Detect feet in camera frame and extract landmarks (ankle, toe, heel).

**Status:** Partially implemented (MediaPipe exists but not integrated)

**Required:**
- Integrate MediaPipe Pose Landmarker (already exists in codebase)
- Process camera frames through MediaPipe
- Extract foot landmarks (ankle, toe positions)
- Visualize landmarks for debugging

**Files to Modify:**
- `SimpleRenderer.kt` - Add MediaPipe processing in `onDrawFrame()`
- `SimpleCameraPoseProvider.kt` - Add landmark extraction methods

**Note:** MediaPipe integration exists for ARCore mode but needs to be adapted for SimpleCamera mode.

---

### Phase 4: Landmark to 3D Conversion

**What:** Convert 2D foot landmarks to 3D world coordinates.

**Status:** Not started

**Required:**
- Unproject 2D screen coordinates to 3D rays
- Intersect rays with detected floor plane
- Calculate 3D foot position
- Validate anchor positions (distance, visibility)

**Files to Modify:**
- `SimpleCameraPoseProvider.kt` - Enhance `hitTest()` to use detected landmarks
- Add landmark-to-3D conversion methods

---

### Phase 5: 3D Model Placement

**What:** Render 3D shoe model at correct position and scale.

**Status:** Not started

**Required:**
- Create anchor from 3D foot position
- Calculate shoe orientation (from ankle-to-toe direction)
- Apply scale calibration (shoe size)
- Render using `ShoeRenderer` (already exists)

**Files to Modify:**
- `SimpleRenderer.kt` - Add shoe rendering for SimpleCamera mode
- `SimpleCameraPoseProvider.kt` - Add anchor creation from landmarks
- Integrate with existing `ShoeRenderer` (currently only used in ARCore mode)

---

### Additional Improvements

1. **Better Error Handling**
   - Add diagnostics for camera initialization failures
   - Handle IMU sensor unavailability gracefully
   - Add user-friendly error messages

2. **IMU-Only Fallback**
   - If camera fails, use IMU-only tracking
   - Basic pose estimation from gyroscope/accelerometer
   - Reduced accuracy but still functional

3. **Texture Type Fixes**
   - Verify `GL_TEXTURE_EXTERNAL_OES` is used correctly
   - Ensure shader uses `samplerExternalOES`
   - Fix any texture binding issues

4. **Performance Optimization**
   - Optimize camera frame processing
   - Reduce frame processing overhead
   - Target 24+ FPS on low-end devices

---

## 📁 File Structure

### New Files Created

```
android/app/src/main/kotlin/com/solemate/app/solemate_app/
├── SimpleCameraManager.kt          # Simplified Camera2 manager
├── SimpleCameraPoseProvider.kt      # Basic pose provider with IMU
└── vio_backup/                      # Backup of original VIO code
    ├── VioEngine.kt.backup
    ├── VioPoseProvider.kt.backup
    ├── Camera2Manager.kt.backup
    └── PlaneEstimator.kt.backup
```

### Modified Files

```
android/app/src/main/kotlin/com/solemate/app/solemate_app/
├── ARActivity.kt                    # Added SimpleCamera mode support
├── SimpleRenderer.kt                # Added SimpleCamera rendering path
└── BackgroundRenderer.kt             # Already supports GL_TEXTURE_EXTERNAL_OES
```

### Unchanged Files (Still Used)

```
android/app/src/main/kotlin/com/solemate/app/solemate_app/
├── WorldPoseProvider.kt             # Interface (used by both modes)
├── ArcorePoseProvider.kt            # ARCore implementation (unchanged)
├── ShoeRenderer.kt                  # 3D model rendering (needs integration)
├── PlaneRenderer.kt                  # Plane visualization (works with both)
└── DisplayRotationHelper.kt         # Rotation utilities (used by both)
```

---

## 🔍 Testing Status

### ✅ Tested

- [x] App launches on non-ARCore device
- [x] Camera permissions are requested correctly
- [x] Camera opens and starts streaming
- [x] Camera feed renders (but with rotation issue)
- [x] TextureView overlay is hidden in VIO mode
- [x] No crashes on initialization

### ❌ Not Tested

- [ ] Camera rotation correction (in progress)
- [ ] Plane detection accuracy
- [ ] Foot landmark detection
- [ ] 3D model placement accuracy
- [ ] Performance on low-end devices
- [ ] Battery usage
- [ ] Long-term tracking stability

---

## 🐛 Known Bugs

1. **Camera Rotation Issue**
   - **Severity:** High
   - **Status:** Active debugging
   - **Description:** Camera feed appears horizontal in portrait mode
   - **Impact:** Blocks user experience, makes app unusable

2. **No Error Recovery**
   - **Severity:** Medium
   - **Status:** Not addressed
   - **Description:** If camera fails to initialize, app shows black screen
   - **Impact:** Poor user experience on incompatible devices

---

## 📝 Implementation Notes

### Design Decisions

1. **Simplified Approach First**
   - Started with minimal implementation (camera preview only)
   - Will add features incrementally
   - Easier to debug and test

2. **Reuse Existing Interfaces**
   - `SimpleCameraPoseProvider` implements `WorldPoseProvider`
   - Same interface as `ArcorePoseProvider`
   - Allows `SimpleRenderer` to work with both

3. **Backup Original Code**
   - Original VIO implementation backed up
   - Can revert if needed
   - Preserves complex features for future use

### Technical Details

- **Camera API:** Android Camera2 API
- **Rendering:** OpenGL ES 2.0 via GLSurfaceView
- **Texture Type:** `GL_TEXTURE_EXTERNAL_OES` (for SurfaceTexture)
- **IMU Sensors:** Accelerometer + Gyroscope
- **Coordinate System:** OpenGL convention (Y-up, right-handed)

---

## 🎯 Next Steps (Priority Order)

1. **Fix Camera Rotation** (CRITICAL)
   - Analyze logcat output
   - Determine correct rotation calculation
   - Test on device

2. **Add Plane Detection** (HIGH)
   - Implement feature detection
   - Add RANSAC plane fitting
   - Visualize detected planes

3. **Integrate MediaPipe** (HIGH)
   - Process camera frames
   - Extract foot landmarks
   - Visualize landmarks

4. **Landmark to 3D** (MEDIUM)
   - Unproject 2D to 3D
   - Intersect with plane
   - Create anchors

5. **3D Model Rendering** (MEDIUM)
   - Integrate ShoeRenderer
   - Apply scale calibration
   - Test placement accuracy

6. **Error Handling** (LOW)
   - Add diagnostics
   - Improve error messages
   - Add fallback modes

---

## 📚 References

- [AR-Fallback-VIO.md](./AR-Fallback-VIO.md) - Original VIO implementation plan
- [AR-Feature-Plan.md](./AR-Feature-Plan.md) - Overall AR feature plan
- Android Camera2 API: https://developer.android.com/reference/android/hardware/camera2/package-summary
- OpenGL ES External Texture: https://www.khronos.org/registry/OpenGL/extensions/OES/OES_EGL_image_external.txt

---

## Last Updated

**Date:** 2025-01-18  
**Status:** Phase 1 - Camera Preview (Rotation issue being debugged)  
**Next Milestone:** Fix camera rotation, then proceed to Phase 2 (Plane Detection)

