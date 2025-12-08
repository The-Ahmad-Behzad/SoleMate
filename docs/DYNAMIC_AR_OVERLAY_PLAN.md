# Dynamic 3D Overlay with Real-Time Foot Tracking - Implementation Plan

**Created:** 2025-01-18  
**Status:** Planning Phase  
**Target:** Transform static 3D overlay into dynamic real-time tracking system

---

## Executive Summary

This plan outlines the transformation of the current static 3D shoe overlay into a dynamic, real-time tracking system that follows foot movements in real-time. The implementation will support user-selectable frame rates (30/60 FPS), handle various movement scenarios, and optimize for performance while maintaining accuracy.

---

## Current System Analysis

### Existing Implementation

**Current Behavior:**
- Static placement: Shoe model is placed once and remains fixed
- Placement cooldown: 500ms between placement updates (`placementCooldownMs`)
- Throttled detection: Foot detection runs every 3rd frame (`frameSkip = 3`)
- Anchor recreation: New anchors created on each placement update
- No real-time tracking: Model position doesn't update with foot movement

**Key Files:**
- `SimpleRenderer.kt`: Main rendering logic with placement cooldown
- `FootTracker.kt`: MediaPipe pose detection (IMAGE mode, not LIVE_STREAM)
- `ShoeRenderer.kt`: Filament-based 3D model rendering
- `ARActivity.kt`: AR session management

**Current Limitations:**
1. **Cooldown prevents real-time updates**: 500ms cooldown blocks continuous tracking
2. **Throttled detection**: Every 3rd frame detection limits responsiveness
3. **Anchor strategy**: Recreating anchors causes jitter and performance overhead
4. **No movement detection**: System doesn't adapt to movement speed/intensity
5. **No occlusion handling**: Foot lifting scenarios not handled
6. **Fixed update rate**: No user control over frame rate

---

## Research Findings

### ARCore Best Practices

1. **Anchor Updates vs Transform Updates:**
   - ARCore anchors maintain world-space stability automatically
   - Updating model transform relative to anchor is more efficient than recreating anchors
   - Anchor `trackingState` should be checked before using pose
   - Best practice: Create anchor once, update transform every frame

2. **Performance Optimization:**
   - ARCore recommends checking `trackingState` before expensive operations
   - Depth API can be used for occlusion but adds computational cost
   - Frame rate should be adaptive based on device capabilities
   - CPU-intensive features should be disabled when not needed

3. **Real-Time Tracking:**
   - ARCore motion tracking provides 60Hz pose updates
   - MediaPipe Pose Landmarker supports LIVE_STREAM mode for real-time processing
   - Transform updates should happen every frame for smooth tracking
   - Smoothing algorithms reduce jitter without sacrificing responsiveness

### MediaPipe Best Practices

1. **Running Modes:**
   - `IMAGE` mode: Processes single images (current implementation)
   - `LIVE_STREAM` mode: Real-time processing with callbacks (needed for dynamic tracking)
   - `VIDEO` mode: Batch processing (not suitable for real-time)

2. **Update Frequency:**
   - MediaPipe can process at 30-60 FPS on modern devices
   - Adaptive throttling based on movement speed improves performance
   - Lower update rates acceptable for slow/stationary movements
   - Higher update rates needed for fast movements

3. **Performance Considerations:**
   - Model selection: `pose_landmarker_full.task` (current) vs `pose_landmarker_lite.task` (faster)
   - Image resolution affects processing time
   - GPU acceleration available on supported devices

---

## Implementation Plan

### Phase 1: Foundation - Real-Time Detection Pipeline

**Goal:** Enable continuous foot detection without cooldown limitations

#### 1.1 Switch MediaPipe to LIVE_STREAM Mode

**File:** `FootTracker.kt`

**Changes:**
- Change `RunningMode.IMAGE` to `RunningMode.LIVE_STREAM`
- Implement `PoseLandmarkerResultListener` callback interface
- Process results asynchronously via callback instead of synchronous detection
- Maintain thread-safe result queue for renderer consumption

**Implementation Details:**
```kotlin
// Change from:
.setRunningMode(RunningMode.IMAGE)

// To:
.setRunningMode(RunningMode.LIVE_STREAM)
.setResultListener { result, image ->
    // Process result asynchronously
    processPoseResult(result, image)
}
```

**Benefits:**
- Real-time processing without blocking render thread
- Better performance through async processing
- Supports continuous frame processing

**Dependencies:** None

**Estimated Effort:** 2-3 hours

---

#### 1.2 Remove Placement Cooldown

**File:** `SimpleRenderer.kt`

**Changes:**
- Remove or significantly reduce `placementCooldownMs` (500ms → 0ms or 16ms for 60fps)
- Update `canUpdate` logic to allow continuous updates
- Add frame-based update throttling instead of time-based

**Implementation Details:**
```kotlin
// Remove or reduce:
private val placementCooldownMs = 500L  // Remove this

// Replace with frame-based throttling:
private val updateEveryNFrames = 1  // Update every frame (configurable)
```

**Benefits:**
- Enables real-time tracking
- Removes artificial delay
- More responsive user experience

**Dependencies:** Phase 1.1 (LIVE_STREAM mode)

**Estimated Effort:** 1 hour

---

#### 1.3 Implement Adaptive Detection Throttling

**File:** `SimpleRenderer.kt`

**Changes:**
- Replace fixed `frameSkip = 3` with adaptive throttling
- Calculate movement velocity from foot position changes
- Adjust detection frequency based on movement speed:
  - Fast movement: Every frame (1x)
  - Medium movement: Every 2nd frame (2x)
  - Slow/stationary: Every 3rd-4th frame (3-4x)

**Implementation Details:**
```kotlin
private var lastFootPosition: FloatArray? = null
private var movementVelocity: Float = 0f
private val FAST_MOVEMENT_THRESHOLD = 0.05f  // 5cm/frame
private val MEDIUM_MOVEMENT_THRESHOLD = 0.02f  // 2cm/frame

fun calculateAdaptiveSkip(): Int {
    return when {
        movementVelocity > FAST_MOVEMENT_THRESHOLD -> 1  // Every frame
        movementVelocity > MEDIUM_MOVEMENT_THRESHOLD -> 2  // Every 2nd frame
        else -> 3  // Every 3rd frame
    }
}
```

**Benefits:**
- Optimizes performance for stationary scenarios
- Maintains responsiveness for fast movements
- Reduces CPU/GPU load when not needed

**Dependencies:** Phase 1.1, 1.2

**Estimated Effort:** 2-3 hours

---

### Phase 2: Hybrid Anchor Strategy

**Goal:** Implement optimal anchor management for stability and performance

#### 2.1 Implement Anchor Tracking State Management

**File:** `SimpleRenderer.kt`

**Changes:**
- Track anchor `trackingState` every frame
- Only update model transform when anchor is `TRACKING`
- Handle `PAUSED` and `STOPPED` states gracefully
- Maintain anchor reference instead of recreating

**Implementation Details:**
```kotlin
private var currentAnchor: Anchor? = null

fun updateAnchorTracking(frame: Frame?) {
    currentAnchor?.let { anchor ->
        when (anchor.trackingState) {
            TrackingState.TRACKING -> {
                // Update model transform relative to anchor
                updateModelTransform(anchor.pose)
            }
            TrackingState.PAUSED -> {
                // Hold last known pose, reduce update frequency
                // Optionally fade out model
            }
            TrackingState.STOPPED -> {
                // Re-anchor on next valid detection
                currentAnchor = null
            }
        }
    }
}
```

**Benefits:**
- Stable tracking when anchor is valid
- Graceful degradation when tracking lost
- No unnecessary anchor recreation

**Dependencies:** Phase 1.2

**Estimated Effort:** 2-3 hours

---

#### 2.2 Implement Transform-Based Updates

**File:** `SimpleRenderer.kt`

**Changes:**
- Update model transform every frame relative to anchor
- Calculate transform from current foot position to anchor
- Apply smoothing to transform updates
- Only recreate anchor when drift exceeds threshold

**Implementation Details:**
```kotlin
private fun updateModelTransform(anchorPose: Pose) {
    // Get current foot position from hit test
    val currentFootHit = hitTestFootPosition()
    
    // Calculate transform from anchor to current foot position
    val transform = calculateTransform(anchorPose, currentFootHit)
    
    // Apply smoothing
    val smoothedTransform = smoothTransform(transform)
    
    // Update model matrix
    shoeModelMatrix = smoothedTransform
}
```

**Benefits:**
- Smooth continuous updates
- Better performance (no anchor recreation overhead)
- Maintains world-space stability

**Dependencies:** Phase 2.1

**Estimated Effort:** 3-4 hours

---

#### 2.3 Implement Smart Re-anchoring

**File:** `SimpleRenderer.kt`

**Changes:**
- Monitor anchor drift continuously
- Re-anchor only when:
  - Drift exceeds threshold (10cm)
  - Anchor tracking state becomes STOPPED
  - Foot position changes significantly (>20cm)
- Smooth transition between old and new anchor

**Implementation Details:**
```kotlin
private val REANCHOR_DRIFT_THRESHOLD = 0.10f  // 10cm
private val REANCHOR_POSITION_THRESHOLD = 0.20f  // 20cm

fun shouldReanchor(currentPosition: FloatArray): Boolean {
    return when {
        currentAnchor == null -> true
        currentAnchor!!.trackingState == TrackingState.STOPPED -> true
        calculateDrift(currentPosition) > REANCHOR_DRIFT_THRESHOLD -> true
        calculatePositionChange(currentPosition) > REANCHOR_POSITION_THRESHOLD -> true
        else -> false
    }
}
```

**Benefits:**
- Maintains accuracy during large movements
- Prevents drift accumulation
- Smooth transitions

**Dependencies:** Phase 2.2

**Estimated Effort:** 2-3 hours

---

### Phase 3: Frame Rate Control & Performance

**Goal:** User-selectable frame rates with performance optimization

#### 3.1 Add FPS Selection UI

**File:** `ARActivity.kt`

**Changes:**
- Add toggle button/segmented control for 30/60 FPS
- Store preference in SharedPreferences
- Pass preference to renderer
- Update UI to show current FPS setting

**Implementation Details:**
```kotlin
// Add to ARActivity UI
private var targetFPS = 30  // Default
private val fpsToggleButton: Button

fpsToggleButton.setOnClickListener {
    targetFPS = if (targetFPS == 30) 60 else 30
    updateFPSDisplay()
    renderer?.setTargetFPS(targetFPS)
    // Save to preferences
    saveFPSPreference(targetFPS)
}
```

**Benefits:**
- User control over performance/quality tradeoff
- Better battery life on 30 FPS mode
- Smoother experience on capable devices (60 FPS)

**Dependencies:** None

**Estimated Effort:** 2-3 hours

---

#### 3.2 Implement Frame Rate Throttling

**File:** `SimpleRenderer.kt`

**Changes:**
- Add `targetFPS` parameter (30 or 60)
- Calculate frame skip based on target FPS
- Throttle detection and updates accordingly
- Maintain smooth rendering at target rate

**Implementation Details:**
```kotlin
private var targetFPS = 30
private var frameSkipForFPS = 2  // 60fps / 30fps = 2

fun setTargetFPS(fps: Int) {
    targetFPS = fps
    frameSkipForFPS = if (fps == 60) 1 else 2
}

override fun onDrawFrame(gl: GL10?) {
    frameCounter++
    
    // Skip frames based on target FPS
    if (frameCounter % frameSkipForFPS != 0 && targetFPS == 30) {
        // Still render camera, but skip detection/updates
        renderCameraOnly()
        return
    }
    
    // Full frame processing
    processFullFrame()
}
```

**Benefits:**
- Consistent frame rate
- Reduced CPU/GPU load at 30 FPS
- Better battery life

**Dependencies:** Phase 3.1

**Estimated Effort:** 2-3 hours

---

#### 3.3 Performance Monitoring & Adaptive Quality

**File:** `SimpleRenderer.kt`

**Changes:**
- Monitor frame time and FPS
- Detect performance degradation
- Automatically reduce quality/update rate if FPS drops
- Log performance metrics

**Implementation Details:**
```kotlin
private var frameTimeHistory = mutableListOf<Long>()
private val TARGET_FRAME_TIME_MS = if (targetFPS == 60) 16L else 33L
private val PERFORMANCE_DEGRADATION_THRESHOLD = 1.5f  // 50% slower

fun monitorPerformance() {
    val currentFrameTime = System.currentTimeMillis() - lastFrameTime
    frameTimeHistory.add(currentFrameTime)
    if (frameTimeHistory.size > 60) frameTimeHistory.removeAt(0)
    
    val avgFrameTime = frameTimeHistory.average().toLong()
    if (avgFrameTime > TARGET_FRAME_TIME_MS * PERFORMANCE_DEGRADATION_THRESHOLD) {
        // Reduce quality/update rate
        adaptivelyReduceQuality()
    }
}
```

**Benefits:**
- Maintains target FPS
- Prevents overheating
- Better user experience

**Dependencies:** Phase 3.2

**Estimated Effort:** 3-4 hours

---

### Phase 4: Movement Scenarios - Foot Movement & Rotation

**Goal:** Handle foot movement, rotation, and pivoting

#### 4.1 Continuous Position Tracking

**File:** `SimpleRenderer.kt`

**Changes:**
- Update foot position every frame (no cooldown)
- Track position history for velocity calculation
- Smooth position updates
- Handle rapid position changes

**Implementation Details:**
```kotlin
private val positionHistory = mutableListOf<FloatArray>()
private val MAX_HISTORY_SIZE = 10

fun updateFootPosition(newPosition: FloatArray) {
    positionHistory.add(newPosition.copyOf())
    if (positionHistory.size > MAX_HISTORY_SIZE) {
        positionHistory.removeAt(0)
    }
    
    // Calculate velocity
    if (positionHistory.size >= 2) {
        val velocity = calculateVelocity(positionHistory)
        movementVelocity = velocity
    }
    
    // Update model position
    updateModelPosition(newPosition)
}
```

**Benefits:**
- Real-time position tracking
- Velocity-based adaptive throttling
- Smooth movement

**Dependencies:** Phase 1.2, 1.3

**Estimated Effort:** 2-3 hours

---

#### 4.2 Foot Rotation & Pivoting Detection

**File:** `SimpleRenderer.kt`

**Changes:**
- Track ankle-to-toe vector direction
- Detect rotation changes
- Update model orientation smoothly
- Handle rapid rotation (pivoting)

**Implementation Details:**
```kotlin
private var lastFootDirection: FloatArray? = null

fun updateFootOrientation(anklePos: FloatArray, toePos: FloatArray) {
    val currentDirection = FloatArray(3).apply {
        this[0] = toePos[0] - anklePos[0]
        this[1] = toePos[1] - anklePos[1]
        this[2] = toePos[2] - anklePos[2]
    }
    
    lastFootDirection?.let { lastDir ->
        val rotationAngle = calculateRotationAngle(lastDir, currentDirection)
        if (rotationAngle > ROTATION_THRESHOLD) {
            // Significant rotation detected
            updateModelOrientation(currentDirection, rotationAngle)
        }
    }
    
    lastFootDirection = currentDirection
}
```

**Benefits:**
- Accurate orientation tracking
- Handles foot pivoting
- Smooth rotation updates

**Dependencies:** Phase 4.1

**Estimated Effort:** 3-4 hours

---

#### 4.3 Movement Velocity-Based Smoothing

**File:** `SimpleRenderer.kt`

**Changes:**
- Adjust smoothing alpha based on movement velocity
- Less smoothing for fast movements (more responsive)
- More smoothing for slow movements (less jitter)
- Separate smoothing for position and rotation

**Implementation Details:**
```kotlin
fun getAdaptiveSmoothingAlpha(velocity: Float): Float {
    return when {
        velocity > FAST_MOVEMENT_THRESHOLD -> 0.7f  // Less smoothing, more responsive
        velocity > MEDIUM_MOVEMENT_THRESHOLD -> 0.5f
        else -> 0.3f  // More smoothing, less jitter
    }
}
```

**Benefits:**
- Responsive for fast movements
- Stable for slow movements
- Adaptive to user behavior

**Dependencies:** Phase 4.1, 4.2

**Estimated Effort:** 2 hours

---

### Phase 5: Occlusion Handling

**Goal:** Handle foot lifting and temporary occlusion

#### 5.1 Visibility Tracking

**File:** `SimpleRenderer.kt`, `FootTracker.kt`

**Changes:**
- Track landmark visibility scores
- Detect when foot becomes occluded (visibility drops)
- Maintain last known position during occlusion
- Fade out model when occluded for extended period

**Implementation Details:**
```kotlin
private var occlusionStartTime: Long = 0
private val OCCLUSION_TIMEOUT_MS = 2000L  // 2 seconds

fun handleOcclusion(visibility: Float) {
    if (visibility < OCCLUSION_VISIBILITY_THRESHOLD) {
        if (occlusionStartTime == 0L) {
            occlusionStartTime = System.currentTimeMillis()
        }
        
        val occlusionDuration = System.currentTimeMillis() - occlusionStartTime
        if (occlusionDuration > OCCLUSION_TIMEOUT_MS) {
            // Fade out model
            fadeOutModel()
        } else {
            // Hold last known position
            maintainLastKnownPosition()
        }
    } else {
        // Foot visible again
        occlusionStartTime = 0L
        fadeInModel()
    }
}
```

**Benefits:**
- Handles temporary occlusion gracefully
- Maintains tracking during brief occlusions
- Prevents model from disappearing incorrectly

**Dependencies:** Phase 4.1

**Estimated Effort:** 3-4 hours

---

#### 5.2 Predictive Tracking During Occlusion

**File:** `SimpleRenderer.kt`

**Changes:**
- Use velocity history to predict position during occlusion
- Extrapolate foot position based on last known velocity
- Limit prediction duration
- Snap back to actual position when foot reappears

**Implementation Details:**
```kotlin
fun predictPositionDuringOcclusion(): FloatArray? {
    if (positionHistory.size < 2) return null
    
    val lastPosition = positionHistory.last()
    val velocity = calculateVelocity(positionHistory)
    
    val occlusionDuration = System.currentTimeMillis() - occlusionStartTime
    val predictedOffset = velocity * (occlusionDuration / 1000f)
    
    return FloatArray(3).apply {
        this[0] = lastPosition[0] + predictedOffset[0]
        this[1] = lastPosition[1] + predictedOffset[1]
        this[2] = lastPosition[2] + predictedOffset[2]
    }
}
```

**Benefits:**
- Smooth experience during brief occlusions
- Better user experience
- Maintains illusion of continuous tracking

**Dependencies:** Phase 5.1

**Estimated Effort:** 2-3 hours

---

#### 5.3 Depth-Based Occlusion (Optional Enhancement)

**File:** `SimpleRenderer.kt`

**Changes:**
- Use ARCore Depth API to detect when foot is occluded by other objects
- Sample depth at foot landmark positions
- Compare depth values to detect occlusion
- Adjust model rendering accordingly

**Implementation Details:**
```kotlin
fun checkDepthOcclusion(frame: Frame, footPosition: FloatArray): Boolean {
    val depthImage = frame.acquireDepthImage() ?: return false
    
    // Sample depth at foot position
    val depth = sampleDepth(depthImage, footPosition)
    val expectedDepth = calculateExpectedDepth(footPosition)
    
    // If sampled depth is significantly closer, foot is occluded
    return depth < expectedDepth - OCCLUSION_DEPTH_THRESHOLD
}
```

**Benefits:**
- More accurate occlusion detection
- Handles complex occlusion scenarios
- Better realism

**Dependencies:** Phase 5.1, 5.2
**Note:** Requires ARCore Depth API support

**Estimated Effort:** 4-5 hours

---

### Phase 6: Phone Movement Around Stationary Foot

**Goal:** Maintain accurate tracking when phone moves around stationary foot

#### 6.1 Camera Pose Compensation

**File:** `SimpleRenderer.kt`

**Changes:**
- Track camera pose changes
- Compensate model position for camera movement
- Maintain foot position in world space
- Update hit test coordinates based on camera movement

**Implementation Details:**
```kotlin
private var lastCameraPose: Pose? = null

fun compensateForCameraMovement(currentCameraPose: Pose) {
    lastCameraPose?.let { lastPose ->
        val cameraMovement = calculatePoseDifference(lastPose, currentCameraPose)
        
        // Adjust model position to compensate
        compensateModelPosition(cameraMovement)
    }
    
    lastCameraPose = currentCameraPose
}
```

**Benefits:**
- Accurate tracking during phone movement
- Maintains world-space stability
- Better user experience

**Dependencies:** Phase 2.2

**Estimated Effort:** 3-4 hours

---

#### 6.2 View Matrix Updates

**File:** `SimpleRenderer.kt`, `ShoeRenderer.kt`

**Changes:**
- Update view matrix every frame
- Ensure model renders correctly from new camera angles
- Maintain proper perspective
- Handle rapid camera movements

**Implementation Details:**
```kotlin
// Already implemented in onDrawFrame, but ensure it's called every frame
override fun onDrawFrame(gl: GL10?) {
    // ... existing code ...
    
    // Update view matrix every frame (not just on placement)
    val viewMatrix = frame.camera.getViewMatrix(...)
    val projMatrix = frame.camera.getProjectionMatrix(...)
    
    shoeRenderer?.setCamera(viewMatrix, projMatrix)
    
    // Update model matrix every frame
    shoeModelMatrix?.let { mm ->
        shoeRenderer?.setModelMatrix(mm)
    }
}
```

**Benefits:**
- Correct rendering from all angles
- Smooth camera movement
- No visual glitches

**Dependencies:** Phase 6.1

**Estimated Effort:** 1-2 hours

---

### Phase 7: Testing & Validation

**Goal:** Comprehensive testing of all features

#### 7.1 Unit Tests

**Files:** New test files

**Test Cases:**
- Adaptive throttling logic
- Anchor tracking state management
- Transform calculations
- Smoothing algorithms
- Occlusion detection
- Movement velocity calculations

**Estimated Effort:** 4-5 hours

---

#### 7.2 Integration Tests

**Test Scenarios:**
- Stationary foot, moving phone
- Moving foot, stationary phone
- Foot rotation/pivoting
- Foot lifting (occlusion)
- Rapid movements
- Slow movements
- Frame rate switching (30/60 FPS)

**Estimated Effort:** 6-8 hours

---

#### 7.3 Performance Testing

**Metrics:**
- Frame rate consistency
- CPU/GPU usage
- Battery consumption
- Memory usage
- Latency measurements

**Test Devices:**
- High-end (Pixel 7, Samsung S23)
- Mid-range (Pixel 5, Samsung A52)
- Low-end (if available)

**Estimated Effort:** 4-5 hours

---

### Phase 8: Future Enhancement - Both Foot and Phone Moving (Walking)

**Goal:** Support tracking while user is walking (both foot and phone moving)

**Status:** Future implementation - to be done after all other phases are complete and tested

#### 8.1 Simultaneous Movement Detection

**Changes:**
- Detect when both foot and phone are moving simultaneously
- Calculate relative movement between foot and phone
- Compensate for phone movement in foot tracking
- Use predictive algorithms for smoother tracking

**Estimated Effort:** 6-8 hours

---

#### 8.2 Advanced Motion Compensation

**Changes:**
- Implement Kalman filtering for motion prediction
- Use IMU data for better motion estimation
- Compensate for phone rotation during walking
- Handle step detection and prediction

**Estimated Effort:** 8-10 hours

---

#### 8.3 Multiple Feet Tracking (Future)

**Changes:**
- Detect and track both left and right feet simultaneously
- Maintain separate anchors for each foot
- Handle occlusion for each foot independently
- Render multiple shoe models

**Estimated Effort:** 10-12 hours

---

## Implementation Timeline

### Phase 1: Foundation (Week 1)
- Days 1-2: LIVE_STREAM mode implementation
- Days 3-4: Remove cooldown, adaptive throttling
- Day 5: Testing and bug fixes

### Phase 2: Anchor Strategy (Week 2)
- Days 1-2: Anchor tracking state management
- Days 3-4: Transform-based updates
- Day 5: Smart re-anchoring

### Phase 3: Frame Rate Control (Week 2-3)
- Days 1-2: FPS selection UI
- Days 3-4: Frame rate throttling
- Day 5: Performance monitoring

### Phase 4: Movement Scenarios (Week 3)
- Days 1-2: Continuous position tracking
- Days 3-4: Rotation detection
- Day 5: Velocity-based smoothing

### Phase 5: Occlusion Handling (Week 4)
- Days 1-2: Visibility tracking
- Days 3-4: Predictive tracking
- Day 5: Optional depth-based occlusion

### Phase 6: Phone Movement (Week 4)
- Days 1-2: Camera pose compensation
- Day 3: View matrix updates
- Days 4-5: Testing

### Phase 7: Testing (Week 5)
- Days 1-2: Unit tests
- Days 3-4: Integration tests
- Day 5: Performance testing

**Total Estimated Time:** 5 weeks

---

## Success Criteria

### Functional Requirements
- ✅ Model updates in real-time with foot movement
- ✅ User can select 30 or 60 FPS
- ✅ Handles foot movement, rotation, and pivoting
- ✅ Handles temporary occlusion (foot lifting)
- ✅ Maintains accuracy when phone moves around stationary foot
- ✅ Smooth transitions without jitter
- ✅ Performance target: ~30 FPS minimum on mid-range devices

### Performance Requirements
- ✅ Frame rate: 30 FPS minimum, 60 FPS on capable devices
- ✅ Latency: <50ms from foot movement to model update
- ✅ CPU usage: <40% on mid-range devices
- ✅ Battery: <5% per hour of AR usage
- ✅ Memory: <200MB additional memory usage

### Quality Requirements
- ✅ Tracking accuracy: <5cm error from actual foot position
- ✅ Orientation accuracy: <10° error from actual foot orientation
- ✅ Smoothness: No visible jitter or stuttering
- ✅ Stability: Model stays locked to foot during tracking

---

## Risk Assessment & Mitigation

### Risk 1: Performance Degradation
**Probability:** Medium  
**Impact:** High  
**Mitigation:**
- Implement performance monitoring early
- Add adaptive quality reduction
- Test on multiple device tiers
- Provide 30 FPS option for lower-end devices

### Risk 2: Tracking Accuracy Loss
**Probability:** Medium  
**Impact:** High  
**Mitigation:**
- Maintain anchor-based tracking
- Implement robust re-anchoring
- Use multi-sample hit testing (already implemented)
- Add calibration system (already implemented)

### Risk 3: Battery Drain
**Probability:** High  
**Impact:** Medium  
**Mitigation:**
- Implement adaptive throttling
- Provide 30 FPS option
- Optimize detection frequency
- Monitor and log battery usage

### Risk 4: Occlusion Handling Complexity
**Probability:** Medium  
**Impact:** Medium  
**Mitigation:**
- Start with simple visibility-based approach
- Add predictive tracking incrementally
- Make depth-based occlusion optional
- Test extensively with real-world scenarios

---

## Dependencies & Prerequisites

### External Dependencies
- ARCore SDK 1.46.0+ (already integrated)
- MediaPipe Tasks Vision 0.10.14+ (already integrated)
- Filament rendering engine (already integrated)

### Internal Dependencies
- Existing anchor system
- Existing smoothing algorithms
- Existing calibration system
- Existing hit testing implementation

### Prerequisites
- Complete Phase 1 before Phase 2
- Complete Phase 2 before Phase 3
- Complete Phase 3 before Phase 4
- Complete Phase 4 before Phase 5
- Complete Phase 5 before Phase 6
- Complete all phases before Phase 7 (testing)
- Complete Phase 7 before Phase 8 (future)

---

## Questions & Clarifications Needed

1. **MediaPipe Model:** Should we switch to `pose_landmarker_lite.task` for better performance, or keep `pose_landmarker_full.task` for accuracy?

2. **Occlusion Timeout:** What is the acceptable occlusion duration before fading out? (Currently planned: 2 seconds)

3. **Performance Targets:** Are the performance targets (30 FPS minimum, <40% CPU) acceptable for your target devices?

4. **UI Placement:** Where should the FPS toggle be placed in the UI? (Top bar, settings menu, floating button?)

5. **Future Phases:** Should Phase 8 (walking scenario) be planned in detail now, or deferred until current phases are complete?

---

## References

1. ARCore Fundamentals: https://developers.google.com/ar/develop/fundamentals
2. ARCore Performance Guide: https://developers.google.com/ar/develop/performance
3. MediaPipe Pose Landmarker: https://developers.google.com/mediapipe/solutions/vision/pose_landmarker
4. ARCore Depth API: https://developers.google.com/ar/develop/depth
5. Filament Documentation: https://google.github.io/filament/

---

## Document History

- **2025-01-18**: Initial plan created based on requirements and research

