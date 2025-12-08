# AR Fallback VIO Implementation

## Overview

This document describes the Visual-Inertial Odometry (VIO) fallback implementation for SoleMate AR, which enables markerless AR tracking on devices that do not support ARCore. The system uses a dual-path architecture: ARCore on supported devices, and VIO fallback on non-ARCore devices.

## Architecture

### Dual-Path Design

The application automatically detects ARCore availability and selects the appropriate tracking mode:

- **ARCore Path**: Uses Google ARCore SDK for world tracking, plane detection, anchors, and depth estimation
- **VIO Path**: Uses custom visual-inertial odometry for pose tracking, combined with RANSAC-based plane estimation

### Component Overview

```
┌─────────────────────────────────────────────────────────┐
│                    ARActivity                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Mode Detection: ARCore vs VIO                   │  │
│  └──────────────────────────────────────────────────┘  │
│                        │                                │
│        ┌───────────────┴───────────────┐               │
│        │                               │               │
│   ┌────▼────┐                    ┌────▼────┐          │
│   │ ARCore  │                    │   VIO   │          │
│   │  Path   │                    │  Path   │          │
│   └────┬────┘                    └────┬────┘          │
│        │                               │               │
│   ┌────▼──────────┐            ┌──────▼──────────┐     │
│   │ArcorePose     │            │VioPoseProvider  │     │
│   │Provider       │            │                 │     │
│   └────┬──────────┘            └──────┬──────────┘     │
│        │                               │               │
│        │                    ┌──────────┴──────────┐    │
│        │                    │                     │    │
│        │              ┌─────▼─────┐      ┌───────▼──┐ │
│        │              │ VioEngine │      │Plane     │ │
│        │              │  (JNI)    │      │Estimator │ │
│        │              └─────┬─────┘      └──────────┘ │
│        │                    │                        │
│        │              ┌─────▼─────┐                  │
│        │              │VioBridge  │                  │
│        │              │  (C++)   │                  │
│        │              └───────────┘                  │
│        │                                              │
│   ┌────┴──────────────────────────────────────┐     │
│   │         SimpleRenderer                      │     │
│   │  (Uses WorldPoseProvider interface)         │     │
│   └─────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
```

## Data Flow

### VIO Mode Initialization

1. **ARActivity.onCreate()**:
   - Calls `checkARCapability()` to detect ARCore availability
   - If ARCore unavailable, sets `arMode = ARMode.VIO`
   - Calls `startARSession()` which routes to `startVIOSession()`

2. **startVIOSession()**:
   - Creates `VioEngine` instance
   - Creates `PlaneEstimator` instance
   - Creates `VioPoseProvider` with VioEngine and PlaneEstimator
   - Initializes VioPoseProvider with camera intrinsics (loads calibration if available)
   - Creates `Camera2Manager` for camera feed
   - Sets up SurfaceTexture for camera rendering
   - Creates `SimpleRenderer` with VioPoseProvider
   - Starts VIO tracking

### Frame Processing Loop

1. **Camera2Manager**:
   - Receives camera frames from Camera2 API (YUV420 format)
   - Feeds frames to `VioEngine` via `feedFrame()`
   - Receives IMU data (accelerometer + gyroscope)
   - Feeds IMU data to `VioEngine` via `feedImu()`
   - Updates `PlaneEstimator` gravity vector from accelerometer

2. **VioEngine (Native C++)**:
   - Processes camera frames for feature detection and tracking
   - Integrates IMU data for pose estimation
   - Updates pose matrix and covariance
   - Provides pose via JNI bridge

3. **VioPoseProvider**:
   - Calls `VioEngine.update()` each frame
   - Gets pose matrix from VioEngine
   - Computes view matrix (inverse of pose)
   - Updates plane estimator with tracked points (if available)
   - Provides pose to SimpleRenderer via WorldPoseProvider interface

4. **SimpleRenderer**:
   - Calls `poseProvider.update()` each frame
   - Renders camera feed using `BackgroundRenderer.drawCamera2Texture()`
   - Gets view/projection matrices from pose provider
   - Performs foot detection (MediaPipe)
   - Uses `poseProvider.hitTest()` for foot placement
   - Renders shoe model via ShoeRenderer

### Plane Detection

1. **PlaneEstimator**:
   - Receives 3D tracked points from VIO (or depth estimation)
   - Filters points near bottom of view (floor candidates)
   - Uses RANSAC algorithm with gravity alignment
   - Returns `PlaneInfo` compatible with WorldPoseProvider interface

2. **Gravity Alignment**:
   - Uses IMU accelerometer to estimate gravity direction
   - Enforces horizontal plane constraint (normal aligned with gravity)
   - Improves accuracy of floor plane detection

### Foot Pose Fusion (Optional)

`FootPoseFusion` combines:
- VIO pose (device position/orientation)
- Plane information (floor location)
- Foot landmarks (ankle/toe 3D positions)

Features:
- Exponential smoothing for stability
- Mahalanobis gating using VIO covariance
- Drift detection and re-anchoring
- Scale calibration integration

## Calibration Procedure

### Phone Height Calibration

1. Open AR view
2. Tap "Calibrate" button
3. Select "Phone Height (meters)"
4. Enter your height (e.g., 1.5 for 1.5 meters)
5. Tap "Save"

The calibration is stored in SharedPreferences and loaded automatically on next VIO session.

### Shoe Size Calibration

1. Open AR view
2. Tap "Calibrate" button
3. Select "Shoe Size (cm)"
4. Enter your shoe size in centimeters (e.g., 26 for 26cm)
5. Tap "Save"

The shoe size is converted to a scale factor (normalized to 26cm reference) and applied to shoe rendering.

### Calibration Persistence

Calibration values are stored in SharedPreferences under the key `solemate_calibration`:
- `phone_height_meters`: Float
- `shoe_size_cm`: Float
- `calibration_method`: String ("height" or "shoe")

Values persist across app restarts and are automatically loaded when VioPoseProvider initializes.

## File Structure

### Native C++ (NDK)

- `android/app/src/main/cpp/vio/VioEngine.h` - VIO engine header
- `android/app/src/main/cpp/vio/VioEngine.cpp` - VIO engine implementation
- `android/app/src/main/cpp/vio/VioBridge.cpp` - JNI bridge
- `android/app/src/main/cpp/vio/vio_config.h` - Configuration constants

### Kotlin

- `WorldPoseProvider.kt` - Abstraction interface
- `ArcorePoseProvider.kt` - ARCore implementation
- `VioPoseProvider.kt` - VIO implementation
- `VioEngine.kt` - JNI wrapper
- `PlaneEstimator.kt` - RANSAC plane fitting
- `FootPoseFusion.kt` - Pose fusion logic
- `Camera2Manager.kt` - Camera2 API wrapper
- `CalibrationManager.kt` - Calibration persistence
- `DebugOverlay.kt` - Debug visualization

## Performance Considerations

### Target Metrics

- **FPS**: 24+ FPS for smooth rendering
- **VIO Update Rate**: 30 Hz (camera frame rate)
- **Latency**: < 50ms from frame capture to pose update
- **Battery**: Monitor 10-minute session impact

### Optimization Strategies

1. **Camera Resolution**: Downscale to 640x480 for VIO processing
2. **Feature Detection**: Limit to 200 features max
3. **Frame Throttling**: Process every 3rd frame for foot detection
4. **RANSAC Iterations**: Limited to 1000 iterations for plane fitting

### Low-End Device Support

- Camera downscaling (640x480)
- Reduced feature count
- Throttled landmark detection
- Simplified plane estimation

## Troubleshooting

### VIO Not Initializing

**Symptoms**: App shows "Failed to initialize VIO" message

**Possible Causes**:
- Native library not loaded (check logcat for "VIO engine native library loaded")
- Camera permissions not granted
- Camera device not available

**Solutions**:
1. Check logcat for JNI errors
2. Verify camera permission in AndroidManifest.xml
3. Ensure device has back camera
4. Check CMake build succeeded (libvio-engine.so exists)

### Poor Tracking Quality

**Symptoms**: Shoe drifts, jitters, or doesn't follow camera movement

**Possible Causes**:
- Insufficient lighting
- Too few features detected
- IMU sensor issues
- Calibration not set

**Solutions**:
1. Ensure good lighting conditions
2. Move camera slowly to allow feature tracking
3. Calibrate phone height or shoe size
4. Check IMU sensor availability (accelerometer, gyroscope)

### Camera Feed Not Displaying

**Symptoms**: Black screen in VIO mode

**Possible Causes**:
- SurfaceTexture not set up correctly
- Camera2 session not started
- Texture ID not passed to renderer

**Solutions**:
1. Check logcat for Camera2 errors
2. Verify SurfaceTexture is attached to GL context
3. Ensure camera is opened after renderer creates texture
4. Check BackgroundRenderer.drawCamera2Texture() is called

### Plane Not Detected

**Symptoms**: Shoe doesn't place on floor

**Possible Causes**:
- Not enough tracked points
- Floor not visible in camera view
- RANSAC threshold too strict

**Solutions**:
1. Point camera at floor and move slowly
2. Ensure floor is visible in bottom portion of view
3. Check PlaneEstimator logs for inlier count
4. Adjust RANSAC parameters if needed

## Known Limitations

1. **Scale Ambiguity**: VIO cannot determine absolute scale without calibration
2. **Drift**: VIO accumulates error over time (mitigated by re-anchoring)
3. **Feature Tracking**: Requires sufficient texture/features in view
4. **Performance**: May be slower than ARCore on low-end devices
5. **Depth Estimation**: No depth buffer like ARCore (relies on plane estimation)

## Future Improvements

1. **VINS-Mono Integration**: Replace minimal VIO with full VINS-Mono implementation
2. **Depth Estimation**: Add stereo or monocular depth estimation
3. **Loop Closure**: Implement loop closure for long-term tracking
4. **Multi-Plane**: Support vertical planes in addition to horizontal
5. **Light Estimation**: Add ambient light estimation for realistic rendering

## Testing Checklist

- [ ] VIO mode initializes on non-ARCore device
- [ ] Camera feed displays correctly
- [ ] Pose tracking works (shoe follows camera)
- [ ] Plane detection works
- [ ] Foot tracking and shoe placement works
- [ ] Calibration persists across restarts
- [ ] Performance is acceptable (24+ FPS)
- [ ] Battery usage is reasonable
- [ ] ARCore mode still works (regression test)

## References

- VINS-Mono: https://github.com/HKUST-Aerial-Robotics/VINS-Mono
- Android Camera2 API: https://developer.android.com/reference/android/hardware/camera2/package-summary
- ARCore Documentation: https://developers.google.com/ar






