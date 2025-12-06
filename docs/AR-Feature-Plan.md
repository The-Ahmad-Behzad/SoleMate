<!-- 66aa09eb-9167-40c9-936e-7b2d93f95907 ffab4eb1-13de-4c4e-b642-01c637e63da8 -->
# 3D Shoe Overlay Implementation Plan

## Current System (What's implemented now)

### Dual-Path AR Architecture

The system now supports two AR tracking modes:

1. **ARCore Path** (default on supported devices):
   - Uses Google ARCore SDK for world tracking
   - Full plane detection, anchors, depth estimation
   - Automatic light estimation

2. **VIO Path** (fallback on non-ARCore devices):
   - Custom Visual-Inertial Odometry (VIO) implementation
   - RANSAC-based plane estimation
   - Camera2 API for camera feed
   - IMU sensor fusion for pose tracking

The system automatically detects ARCore availability and selects the appropriate mode. See `docs/AR-Fallback-VIO.md` for detailed VIO implementation documentation.

### AR Session/Activity

- File: `android/app/src/main/kotlin/com/solemate/app/solemate_app/ARActivity.kt`
- **ARCore Mode**: Initializes ARCore `Session` with:
    - PlaneFinding: HORIZONTAL
    - UpdateMode: LATEST_CAMERA_IMAGE
    - DepthMode: AUTOMATIC
    - InstantPlacement: LOCAL_Y_UP
- **VIO Mode**: Initializes VioEngine, VioPoseProvider, and Camera2Manager
- Hosts a `GLSurfaceView` and hands rendering to `SimpleRenderer`.

- Rendering & hit testing
  - File: `android/app/src/main/kotlin/com/solemate/app/solemate_app/SimpleRenderer.kt`
  - Uses `WorldPoseProvider` abstraction interface (supports both ARCore and VIO)
  - **ARCore Mode**: Draws camera feed using `BackgroundRenderer.draw(frame)` with external OES texture
  - **VIO Mode**: Draws camera feed using `BackgroundRenderer.drawCamera2Texture()` with regular 2D texture
  - Renders detected planes via `PlaneRenderer` (works with both modes)
  - Grabs throttled camera images; runs foot detection in a background executor
  - Transforms MediaPipe foot pixel coords (IMAGE_PIXELS) → VIEW using `poseProvider.transformCoordinates2d()`
  - Performs `poseProvider.hitTest(viewX, viewY)` and selects best hit among Plane/Point/DepthPoint
  - Keeps anchors list (ARCore mode) or uses plane-based placement (VIO mode)

- Foot detection (MediaPipe)
  - File: `android/app/src/main/kotlin/com/solemate/app/solemate_app/FootTracker.kt`
  - Uses `com.google.mediapipe:tasks-vision` Pose Landmarker.
  - Returns ankle px and toe-tip px (if present) in camera image space.
  - Renderer prefers toe tip or downward-biased ankle when hit testing.

- Utilities
  - `DisplayRotationHelper`: sync viewport with AR session (works with both ARCore Session and Camera2)
  - `YuvConverter`: YUV_420_888 → Bitmap
  - `WorldPoseProvider`: Abstraction interface for AR pose tracking
  - `ArcorePoseProvider`: ARCore implementation of WorldPoseProvider
  - `VioPoseProvider`: VIO implementation of WorldPoseProvider
  - `Camera2Manager`: Camera2 API wrapper for VIO mode
  - `PlaneEstimator`: RANSAC-based plane fitting with gravity alignment
  - `FootPoseFusion`: Optional pose fusion with smoothing and drift detection
  - `CalibrationManager`: Calibration persistence (phone height, shoe size)

- Dependencies already present
  - ARCore: `com.google.ar:core:1.46.0` (optional, only on supported devices)
  - Filament: `filament-android`, `filament-utils-android`, `gltfio-android` (for .glb/.gltf)
  - MediaPipe Tasks Vision: `0.10.14`
  - Android NDK: For native VIO implementation (C++)

## Goal

Overlay a 3D shoe model accurately on the detected foot, tracking position, orientation, and scale with stability and visual realism (lighting, occlusion), while maintaining real-time performance.

## High-Level Approach

1) Convert 2D foot keypoints (ankle, toe) into a stable 3D anchor pose on the floor/plane.

2) Load 3D shoe model (GLB/GLTF) via Filament gltfio and attach to ARCore anchor.

3) Orient shoe using ankle→toe vector; align up-axis to plane normal (Y-up).

4) Calibrate scale using known foot/shoe measurements or dynamic estimation.

5) Add depth-based occlusion and lighting estimation for realism.

6) Smooth pose updates and handle re-anchoring for drift/visibility changes.

## Detailed Steps

### 1) Pose and Anchor Computation

- Source of position:
  - Use `frame.hitTest(viewX, viewY)` from toe tip (fallback: ankle with downward bias) as now. Keep accepting Plane > Point > DepthPoint.
  - If Plane: ensure `isPoseInPolygon`; else accept `Point`/`DepthPoint` for early placement.
- Orientation (yaw) derivation:
  - Compute 2D direction vector `d = normalize(toe - ankle)` in IMAGE_PIXELS or normalized [0..1].
  - Convert both ankle and toe IMAGE_PIXELS → VIEW; then unproject rays using camera intrinsics (optional), or keep 2D yaw (screen-based) when near orthographic top-down alignment. Prefer: Use hit result pose’s Y-up plane normal and construct a model quaternion:
    - Up vector: plane normal (Y-up)
    - Forward vector: project 2D direction onto plane; compute right via cross products; build orthonormal basis.
- Pitch/roll:
  - Initially set to zero (align with plane). Later can estimate via heel/toe difference and plane normal.
- Implementation notes (in `SimpleRenderer.kt`):
  - Store last valid `HitResult` / anchor. Update orientation each frame using new keypoints while reusing existing anchor (to avoid anchor churn) unless anchor drifts far; then re-anchor.
  - Maintain a small state struct: `currentAnchor`, `smoothedPose`, `lastUpdateTime`.

### 2) Load and Render the 3D Shoe Model (Filament)

- Asset preparation:
  - Place `.glb` or `.gltf` + bin/textures under `android/app/src/main/assets/models/shoes/`.
  - Prefer `.glb` for simpler packaging.
- Filament setup (new helper `ShoeRenderer`):
  - Use `gltfio-android` `AssetLoader` and `ResourceLoader` to load model into a Filament `Engine`/`Scene`.
  - Reuse Filament integration already transitively used by ARCore background, or create a dedicated Filament context compatible with your GL thread.
  - Expose APIs: `loadModel(assetPath)`, `setModelTransform(pose)`, `setScale(s)`, `draw(viewProj)`.
- Attachment to anchor:
  - Keep a `Pose` (model matrix) derived from anchor’s `hitPose` combined with orientation and scale offset (toe-to-origin alignment). Apply an offset so the shoe sole sits on the plane and toe aligns with the direction vector.
- Materials/textures:
  - Ensure PBR materials from GLTF are supported; verify gamma/IBL if adding HDR environment later.

### 3) Orientation, Offsets, and Alignment

- Coordinate frames:
  - ARCore plane: Y-up. Ensure shoe model is authored Y-up; if Z-forward, adjust with a fixed pre-rotation.
- Forward alignment:
  - Compute rotation that maps model’s forward axis to the projected ankle→toe vector on the plane.
- Vertical alignment:
  - Lift/lower model so sole rests on plane: add a small Y-translation equal to sole thickness in meters.
- Left vs right foot:
  - If side detection is known (from landmarks), pick left/right shoe asset variant or mirror the model.

### 4) Scale Calibration

- Simple heuristic:
  - Estimate foot length in image space from (heel ↔ toe) pixels; map to meters using depth (if DepthPoint) or plane hit pose distances.
  - Alternatively, ask the user to place phone at a known height and use plane geometry extents; or prompt a one-time calibration (select shoe size)
- Implementation:
  - Compute a base scale factor `s0` by comparing model’s authoring length (meters) to estimated foot length (meters). Apply smoothing on scale.
  - Provide manual override via UI.

### 5) Depth Occlusion and Lighting

- Depth-based occlusion:
  - Use ARCore Depth API to obtain depth texture; render shoe with depth testing against the environment.
  - Option A: Use ARCore’s depth compositing in your pipeline if available; Option B: Sample depth texture and write custom depth-aware shader in Filament (advanced).
- Lighting estimation:
  - Use ARCore’s `LightEstimate` from `frame.lightEstimate` to adjust exposure and color correction; pass as uniforms to Filament or use tone-mapping settings.

### 6) Stabilization and Re-anchoring

- Temporal smoothing:
  - Low-pass filter the pose (position and yaw) across frames. E.g., exponential smoothing with alpha ~0.2–0.3.
  - Smooth scale separately.
- Robustness:
  - If no planes TRACKING, hold last pose and reduce alpha.
  - If keypoints are missing for N frames, fade out the model; reappear when tracking resumes.
  - Re-anchor if anchor drift exceeds threshold (e.g., >5 cm) from desired foot position.

### 7) Performance & Threading

- Keep detection throttled (`frameSkip = 2..4`).
- Avoid `session.update()` off GL thread. Only transform/hitTest on the current frame in GL thread.
- Do not recreate Filament resources per frame. Load once; update transforms only.
- Use `ARGB_8888` bitmap conversion already implemented; consider lowering JPEG quality in `YuvConverter` to 80 for speed if needed.

### 8) Validation & Debugging Steps

- Placement validation
  - Log: source px (ankle/toe), transformed VIEW coords, hit type and distance, anchor pose.
  - Visual: draw a 2D debug reticle at `viewX,viewY`; verify it sits under the foot.
- Orientation validation
  - Render a small arrow along model forward axis; verify it follows ankle→toe direction.
- Scale validation
  - Draw a ground-plane grid and foot-length measuring gizmo to compare model length to foot keypoints.
- Occlusion validation
  - Move the real foot partially in front of the virtual shoe; verify correct occlusion.
- Performance validation
  - Target 24–30 FPS on mid-range device; log detection time and render time.

## Key Files to Create/Update

- Update
  - `SimpleRenderer.kt`:
    - Manage `currentAnchor`, `smoothedPose`, conversion of model transform from anchor + orientation + scale.
    - Integrate lighting estimate.
  - `ARActivity.kt`:
    - Optional: add UI toggles for model variant/size, debug overlays.
- New
  - `ShoeRenderer.kt` (Filament wrapper): load GLB, manage scene entities, set transforms, draw.
  - `assets/models/shoes/<shoe>.glb`.
  - Optional: `OcclusionRenderer.kt` if custom depth occlusion is used.

## Minimal Snippets (Illustrative)

- Build model transform from anchor pose + orientation + scale (Kotlin):
```kotlin
val modelMatrix = FloatArray(16)
android.opengl.Matrix.setIdentityM(modelMatrix, 0)
// M_anchor from hitPose.toMatrix(...), R_forward from ankle->toe, S from calibration
// modelMatrix = M_anchor * R_align * T_soleOffset * S
```

- Selecting best hit:
```kotlin
var best: HitResult? = null; var pref = -1
for (hit in frame.hitTest(viewX, viewY)) {
  val p = when(val tr = hit.trackable){
    is Plane -> 3; is Point -> 2; is DepthPoint -> 1; else -> 0
  }
  if (p > pref || (p==pref && best!=null && hit.distance < best!!.distance)) { best = hit; pref = p }
}
```

- Light estimate:
```kotlin
val le = frame.lightEstimate
if (le.state == LightEstimate.State.VALID) {
  val colorCorr = FloatArray(4)
  le.getColorCorrection(colorCorr, 0)
  // pass to Filament material parameter if desired
}
```


## Library Choices

- AR runtime: ARCore (already integrated).
- Rendering: Filament (already in Gradle), gltfio to load GLTF/GLB.
- Foot landmarks: MediaPipe Pose Landmarker (already integrated).
- Optional convenience: `io.github.sceneview:arsceneview` could simplify Filament handling, but you already depend on Filament; staying native keeps control.

## Rollout Phases

1) Load GLB and render at world origin; verify rendering.

2) Attach to a static test anchor; verify transform updates.

3) Drive anchor from hit test under toe; verify stable placement.

4) Add orientation from ankle→toe; verify facing.

5) Add scale calibration; verify length.

6) Add lighting + occlusion; verify realism.

7) Smoothing and re-anchoring; verify robustness.

## Acceptance Criteria

- Shoe appears under the correct foot with forward-facing orientation within ±10°.
- Model sole rests on plane; minimal jitter after smoothing.
- Depth occlusion works for partial overlaps.
- 24+ FPS on a mid-range device.
- No leaks; clean shutdown via renderer `release()`.

### VIO Implementation Status

- [x] Abstraction layer (WorldPoseProvider interface)
- [x] Native VIO module (VioEngine C++, VioBridge JNI)
- [x] Kotlin VIO integration (VioEngine.kt, PlaneEstimator.kt, VioPoseProvider.kt)
- [x] Camera2 integration (Camera2Manager.kt)
- [x] Renderer updates (BackgroundRenderer, PlaneRenderer support both modes)
- [x] Activity updates (ARActivity supports both ARCore and VIO modes)
- [x] Calibration system (phone height, shoe size with persistence)
- [x] Debug overlays (optional VIO tracking status, performance metrics)
- [x] Documentation (AR-Fallback-VIO.md)

### To-dos

- [ ] Create ShoeRenderer to load and render GLB via Filament
- [ ] Compute model transform from anchor + orientation + scale with smoothing
- [ ] Derive yaw from ankle→toe projected onto plane normal
- [ ] Estimate scale from foot length or user size; add override
- [ ] Use ARCore LightEstimate and enable depth-based occlusion
- [ ] Add reticle, axes/arrow gizmo, grid, and logging
- [ ] Add smoothing and re-anchoring logic for robustness
- [ ] Integrate FootPoseFusion into SimpleRenderer for enhanced stability
- [ ] Test VIO mode on non-ARCore devices
- [ ] Performance optimization for low-end devices