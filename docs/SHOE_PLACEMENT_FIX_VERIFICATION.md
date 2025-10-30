# Shoe Placement Fix - Verification Guide

## Changes Made

### Problem Summary
- **Unpredictable placement**: Shoe appearing at random locations, far from foot, or at camera origin
- **Root causes**:
  1. No validation of hit-test distances (could hit planes meters away)
  2. No visibility threshold checks (accepting low-confidence landmarks)
  3. "Place once" behavior (never updates even if foot moves)
  4. No validation that ankle/toe hits are reasonably close
  5. No coordinate bounds checking

### Solution Implemented

**File: `SimpleRenderer.kt`**

#### 1. Added Validation Parameters
```kotlin
private val maxHitDistance = 3.0f      // Max 3 meters for valid hit-test
private val minVisibility = 0.3f        // Min 30% landmark visibility
private val placementCooldownMs = 500L  // Update every 500ms
```

#### 2. Visibility Gating
```kotlin
if (fp.visibility < minVisibility) {
    Log.d("FootAnchor", "⚠️ Low visibility ${fp.visibility} < $minVisibility, skipping")
    return@let
}
```

#### 3. Distance Validation in Hit-Testing
```kotlin
if (hit.distance > maxHitDistance) {
    Log.d("FootAnchor", "⚠️ $label hit too far: ${hit.distance}m")
    continue  // Skip this hit
}
```

#### 4. Ankle/Toe Consistency Check
```kotlin
val distanceBetween = abs(ankleHit.distance - toeHit.distance)
if (distanceBetween > 0.5f) {
    Log.w("FootAnchor", "❌ Ankle/toe distance mismatch: ${distanceBetween}m")
    return@let
}
```

#### 5. Continuous Updates (with Cooldown)
```kotlin
val canUpdate = !shoePlaced || (currentTime - lastPlacementTime) > placementCooldownMs

if (shoePlaced) {
    anchors.forEach { it.detach() }  // Clean up old anchors
    anchors.clear()
}
```

#### 6. Coordinate Bounds Checking
```kotlin
if (viewX < 0 || viewY < 0) {
    Log.w("FootAnchor", "❌ Invalid view coords: ($viewX, $viewY)")
    return null
}
```

## Testing Steps

### Step 1: Install Updated APK

Connect your device and run:
```powershell
cd C:\Projects\solemate_app\android
./gradlew installDebug
```

### Step 2: Set Up Logging

Open PowerShell and run:
```powershell
adb logcat -c
adb logcat -v time -s "FootAnchor","FootTracker" | Tee-Object -FilePath "logs\placement_test_$(Get-Date -f 'yyyyMMdd_HHmmss').log"
```

### Step 3: Prepare Test Environment

1. **Good lighting** - Ensure the room is well-lit
2. **Clear floor** - Remove clutter for better plane detection
3. **Textured surface** - Patterned floor/rug works better than plain surfaces
4. **Stand still initially** - Let ARCore detect planes first

### Step 4: Run Test Scenarios

#### Test A: Initial Placement
1. Open the AR view
2. **Wait 3-5 seconds** for plane detection (look for white dots/grid)
3. Point camera at your foot on the floor
4. **Keep foot still** for 1-2 seconds

**Expected Behavior:**
- Shoe should appear **on or near your foot**
- Distance should be **< 2 meters** from camera
- Logs should show: `✅ Anchor(Plane, d=0.XX)`

**Check Logs For:**
```
Processing foot: side=RIGHT vis=0.XX ankle=(...) toe=(...)
Converted to view coords: ankle=(...) toe=(...)
Hit test results: ankleHit=true (d=1.XX) toeHit=true (d=1.XX)
✅ Anchor(Plane, d=1.20) side=RIGHT footLen=0.XXXm yaw=X.X°
```

#### Test B: Movement Updates
1. After shoe appears, **slowly rotate your foot** left/right
2. Wait 500ms between movements
3. Shoe should **update orientation** to follow foot

**Expected Behavior:**
- Shoe rotates with foot (yaw changes in logs)
- Updates every ~500ms
- No jumping or glitching

**Check Logs For:**
```
yaw=10.5° (changes as you rotate)
✅ Anchor(...) (new entries every 500ms when foot moves)
```

#### Test C: Distance Validation
1. Point camera at **distant floor** (>3 meters away)
2. Extend foot toward camera

**Expected Behavior:**
- Shoe should **reject distant hits**
- Only place when foot is within 3 meters

**Check Logs For:**
```
⚠️ ankle hit too far: 3.50m > 3.00m
⚠️ toe hit too far: 3.80m > 3.00m
ℹ️ No hit for RIGHT foot; trackingPlanes=1
```

#### Test D: Low Visibility Rejection
1. Cover foot partially with hand/object
2. Move foot quickly (causes motion blur)

**Expected Behavior:**
- Shoe should **disappear** or **not place** when visibility is low
- Reappears when foot is clearly visible again

**Check Logs For:**
```
⚠️ Low visibility 0.15 < 0.30, skipping
```

#### Test E: Inconsistent Hit Rejection
1. Place foot half on floor, half on elevated surface (like a step)
2. Camera sees ankle and toe at different depths

**Expected Behavior:**
- Shoe should **not place** if ankle/toe distances differ by >50cm
- Prevents weird stretching or floating

**Check Logs For:**
```
❌ Ankle/toe distance mismatch: 0.65m, skipping
```

## Verification Checklist

### ✅ Placement Quality

- [ ] Shoe appears **on the foot**, not floating or far away
- [ ] Distance is **< 2 meters** from camera in normal use
- [ ] Shoe **never appears at camera origin** (0, 0, 0)
- [ ] Shoe **never jumps to distant planes** (walls, furniture)

### ✅ Orientation

- [ ] Shoe **points same direction as foot** (ankle→toe)
- [ ] Yaw angle in logs changes when you rotate foot
- [ ] Shoe rotation is **smooth**, not jittery

### ✅ Scale

- [ ] Shoe size is **appropriate** for foot
- [ ] `finalScale` in logs is around `0.05` (5% of model)
- [ ] Shoe doesn't appear **gigantic** or **microscopic**

### ✅ Updates

- [ ] Shoe **updates position** when foot moves (after 500ms cooldown)
- [ ] Old anchors are **cleaned up** (no memory leak)
- [ ] Placement rate is **throttled** (not spamming every frame)

### ✅ Rejection Cases

- [ ] **Low visibility** (<30%): Shoe doesn't place
- [ ] **Too far away** (>3m): Shoe doesn't place
- [ ] **Inconsistent depths**: Ankle/toe >50cm apart rejected
- [ ] **No planes detected**: Graceful failure message in logs

## Common Issues & Fixes

### Issue 1: Shoe Still Appears Far Away

**Possible Causes:**
- Hit-test returning distant plane/wall
- Coordinate transformation error

**Debug:**
```
Check log: "Hit test results: ... (d=X.XX)"
```
- If `d > 2.0`, hit is too far
- If `viewX < 0` or `viewY < 0`, coordinate conversion failed

**Fix:**
- Lower `maxHitDistance` from `3.0f` to `2.0f`
- Add more textured surfaces for better depth tracking

### Issue 2: Shoe Never Appears

**Possible Causes:**
- Visibility always too low
- No planes detected
- Hit-test always failing

**Debug:**
```
Check logs for:
- "⚠️ Low visibility X.XX < 0.30"
- "ℹ️ No hit for X foot; trackingPlanes=0"
- "❌ Invalid view coords"
```

**Fix:**
- Improve lighting
- Move camera around to detect planes
- Lower `minVisibility` from `0.3f` to `0.2f`

### Issue 3: Shoe Jumps Around

**Possible Causes:**
- Cooldown too short
- Anchor not being reused

**Debug:**
```
Count "✅ Anchor" messages in logs
```
- If > 2 per second, cooldown is too short

**Fix:**
- Increase `placementCooldownMs` from `500L` to `1000L`

### Issue 4: Shoe at Camera Origin (0,0,0)

**Possible Causes:**
- Hit-test returning invalid pose
- Model matrix not being set correctly

**Debug:**
```
Check: "Model matrix set: [X, Y, Z] (translation)"
```
- If `[0.0, 0.0, 0.0]`, hit-test failed silently

**Fix:**
- Add validation after `chosen.hitPose.toMatrix()`
- Ensure `chosen.distance > 0.1f`

## Performance Metrics

### Expected Log Output (Normal Operation)

```
10-30 22:45:10.123 D/FootAnchor: Processing foot: side=RIGHT vis=0.89 ankle=(0.42,0.51) toe=(0.38,0.49)
10-30 22:45:10.125 D/FootAnchor: Converted to view coords: ankle=(612.3, 1089.5) toe=(658.1, 1012.8)
10-30 22:45:10.127 D/FootAnchor: Hit test results: ankleHit=true (d=1.23) toeHit=true (d=1.18)
10-30 22:45:10.129 D/FootAnchor: ✅ Anchor(Plane, d=1.18) side=RIGHT footLen=0.095m (BAD) footScale=1.00 baseScale=0.05 finalScale=0.0500 yaw=-15.3° ankle=[-0.08, -1.12, -0.06] toe=[-0.10, -1.21, -0.02]
10-30 22:45:10.130 D/FootAnchor: Model matrix set: [-0.095, -1.184, -0.001] (translation)
```

### Rejection Example (Too Far)

```
10-30 22:45:15.456 D/FootAnchor: Processing foot: side=LEFT vis=0.65 ankle=(0.52,0.48) toe=(0.48,0.46)
10-30 22:45:15.458 D/FootAnchor: Converted to view coords: ankle=(523.1, 1156.2) toe=(582.9, 1098.4)
10-30 22:45:15.461 D/FootAnchor: ⚠️ ankle hit too far: 3.45m > 3.00m
10-30 22:45:15.462 D/FootAnchor: ⚠️ toe hit too far: 3.58m > 3.00m
10-30 22:45:15.463 D/FootAnchor: Hit test results: ankleHit=false (d=null) toeHit=false (d=null)
10-30 22:45:15.464 D/FootAnchor: ℹ️ No hit for LEFT foot; trackingPlanes=1
```

## Summary of Key Validations

| Validation | Threshold | Purpose |
|------------|-----------|---------|
| **Visibility** | ≥ 0.3 (30%) | Reject occluded/blurry landmarks |
| **Hit Distance** | ≤ 3.0 meters | Prevent distant plane hits |
| **Ankle/Toe Consistency** | < 0.5m difference | Ensure both on same surface |
| **View Coordinates** | ≥ 0 (on-screen) | Prevent invalid coordinates |
| **Plane Tracking** | `TrackingState.TRACKING` | Only use actively tracked planes |
| **Update Rate** | 500ms cooldown | Prevent spam, smooth updates |

## Next Steps

1. **Test on device** with all scenarios above
2. **Collect logs** for each test case
3. **Verify metrics** match expected values
4. **Report results**:
   - Which tests passed ✅
   - Which tests failed ❌
   - Log excerpts showing issues

## Fine-Tuning Parameters

If needed, adjust these in `SimpleRenderer.kt`:

```kotlin
// Make more strict (fewer placements, higher quality)
private val maxHitDistance = 2.0f       // Was 3.0f
private val minVisibility = 0.5f         // Was 0.3f
private val placementCooldownMs = 1000L  // Was 500L

// Make more lenient (more placements, lower quality)
private val maxHitDistance = 4.0f        // Was 3.0f
private val minVisibility = 0.2f         // Was 0.3f
private val placementCooldownMs = 250L   // Was 500L
```

## Success Criteria

The fix is **successful** if:

1. ✅ **95%+ placements** are within 2m of camera
2. ✅ **No placements** at camera origin (0,0,0)
3. ✅ **Shoe follows foot** when moving (with 500ms delay)
4. ✅ **Distant planes rejected** in logs
5. ✅ **Low visibility rejected** in logs
6. ✅ **Orientation matches** foot direction (yaw ±10°)

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-30  
**Author**: AI Assistant  
**Related Files**: `SimpleRenderer.kt`, `FootTracker.kt`, `ShoeRenderer.kt`

