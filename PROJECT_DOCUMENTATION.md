# SoleMate - Project Documentation

## 1. Project Description

### 1.1 Functionality

SoleMate is an Augmented Reality (AR)-based mobile application designed to revolutionize the online footwear shopping experience. The application enables users to virtually try on shoes in real-time using their smartphone camera, providing an immersive and interactive shopping experience that bridges the gap between online and in-store shopping.

**Core Features:**

1. **AR Try-On**: Real-time overlay of 3D shoe models on the user's feet using ARCore and MediaPipe pose detection. The system tracks foot position and orientation to accurately place and scale shoe models.

2. **Outfit Matching**: Intelligent recommendation system that analyzes outfit images to suggest complementary footwear based on color harmony and style compatibility.

3. **Personalized Skins**: Customization feature allowing users to create personalized shoe designs by selecting colors, textures, and surface finishes (matte to glossy), with real-time AR preview.

4. **Virtual Try-On Closet**: Persistent storage of try-on sessions, enabling users to save, compare, and revisit previously tried shoes for easy decision-making.

### 1.2 Context

The application addresses significant limitations in traditional online footwear shopping:

- **Lack of Visual Context**: Online shoppers cannot visualize how shoes will look on their feet or match with their outfits.
- **Size and Fit Uncertainty**: Without physical try-on, users struggle to assess proper fit and appearance.
- **Limited Personalization**: Standard e-commerce platforms offer minimal customization options.
- **Decision Fatigue**: Users find it difficult to compare multiple options without a unified interface.

SoleMate leverages modern AR technology, computer vision, and machine learning to provide a solution that enhances user confidence in online footwear purchases while offering an engaging, interactive experience.

### 1.3 Design

**Architecture Overview:**

SoleMate follows a hybrid mobile architecture with a clear separation between frontend, backend, and AR processing layers:

**Frontend (Mobile Application):**
- **Framework**: Flutter (Dart)
- **Platform**: Android (primary), with iOS support planned
- **AR Engine**: ARCore (Android) with MediaPipe for pose detection
- **3D Rendering**: Filament rendering engine for GLB/GLTF models
- **State Management**: Flutter's built-in state management
- **UI/UX**: Custom design system with olive green and warm brown color palette

**Backend Services:**
- **Runtime**: Node.js with Express.js framework
- **Language**: TypeScript
- **Database**: MongoDB for product data, user profiles, try-on history, and outfit matches
- **Authentication**: Firebase Authentication for user management
- **Storage**: Firebase Storage for 3D models, images, and user-generated content
- **Caching**: Redis for session management and performance optimization

**AR Processing Layer:**
- **Pose Detection**: MediaPipe Pose Landmarker (v0.10.14) for real-time foot tracking
- **AR Tracking**: ARCore for SLAM (Simultaneous Localization and Mapping) and plane detection
- **Fallback System**: Visual-Inertial Odometry (VIO) for devices without ARCore support
- **Coordinate Systems**: Multi-stage coordinate transformation (normalized → image pixels → view → world)

**Data Flow:**
1. User initiates AR session → Camera feed captured
2. MediaPipe processes frames → Foot landmarks extracted
3. ARCore/VIO provides tracking → 3D world coordinates computed
4. Pose fusion algorithm stabilizes → Shoe model placed and rendered
5. User interactions → Calibration, recalibration, and customization
6. Try-on sessions → Saved to backend for persistence

### 1.4 Background Information

**Technology Stack Rationale:**

- **ARCore**: Google's AR framework provides robust SLAM capabilities, plane detection, and anchor management essential for stable AR experiences. It handles device motion tracking, environmental understanding, and light estimation.

- **MediaPipe**: Google's open-source framework offers pre-trained pose estimation models optimized for mobile devices. The Pose Landmarker provides 33 body landmarks including specific foot landmarks (ankles, heels, toe indices) with real-time performance.

- **Filament**: Google's physically-based rendering engine enables high-quality 3D model rendering with realistic lighting, shadows, and material properties. It supports industry-standard GLB/GLTF formats.

- **Flutter**: Cross-platform framework allows code reuse while providing native performance. Its widget-based architecture facilitates rapid UI development and state management.

- **MongoDB**: NoSQL database provides flexible schema for product catalogs, user data, and try-on history. Its document model suits the varied data structures required by the application.

**Performance Considerations:**

- **Real-time Processing**: AR try-on requires processing at 30-60 FPS. MediaPipe and ARCore are optimized for mobile GPUs.
- **Battery Optimization**: Efficient frame processing, background task management, and adaptive quality settings ensure acceptable battery life.
- **Network Efficiency**: Caching strategies, image compression, and lazy loading minimize data usage.
- **Device Compatibility**: Fallback mechanisms ensure functionality on devices without ARCore support.

**Security and Privacy:**

- User authentication via Firebase ensures secure access
- Try-on history and personal data stored with user-specific access controls
- Image processing performed on-device where possible to protect privacy
- Secure API endpoints with authentication middleware

---

## 2. Algorithm Design

This section describes the core algorithms that power SoleMate's major features. Each algorithm is presented with pseudocode following a structured format.

### 2.1 Algorithm 1: Foot Detection and Landmark Extraction

**Purpose**: Detect feet in camera frames and extract key anatomical landmarks (ankle, toe, heel) for AR shoe placement.

**Technology**: MediaPipe Pose Landmarker (v0.10.14)

**Input**: Camera frame (bitmap or MPImage), image dimensions (width, height)

**Output**: FootDetectionResult containing detected foot landmarks, side (LEFT/RIGHT), visibility scores, and normalized coordinates

**Algorithm Description**: The algorithm processes camera frames through MediaPipe's pose estimation model to detect human pose landmarks. It identifies foot-specific landmarks, validates visibility, selects the best visible foot, and extracts normalized coordinates for downstream processing.

**Pseudocode**:

```
Algorithm 1 Foot Detection and Landmark Extraction

Input: Camera frame F, image width W, image height H
Output: FootDetectionResult R with landmarks and metadata

1. Initialize poseLandmarker ← MediaPipe PoseLandmarker with pose_landmarker_full.task model
2. Set runningMode ← LIVE_STREAM (or IMAGE for fallback)
3. If runningMode == LIVE_STREAM then
4.     detectFootAsync(F)
5.     R ← getLatestResult()
6. Else
7.     mpImage ← convertBitmapToMPImage(F)
8.     poseResult ← poseLandmarker.detect(mpImage)
9.     R ← extractFootFromResult(poseResult, W, H)
10. End if
11. Return R

Function extractFootFromResult(poseResult, W, H):
12.    poses ← poseResult.landmarks()
13.    If poses.isEmpty() then
14.        Return FootDetectionResult(detected=false, W, H)
15.    End if
16.    lm ← poses[0]  // Use first pose (single-person use case)
17.    
18.    // MediaPipe Pose landmark indices
19.    LEFT_ANKLE ← 27
20.    RIGHT_ANKLE ← 28
21.    LEFT_HEEL ← 29
22.    RIGHT_HEEL ← 30
23.    LEFT_FOOT_INDEX ← 31
24.    RIGHT_FOOT_INDEX ← 32
25.    
26.    la ← lm[LEFT_ANKLE]
27.    ra ← lm[RIGHT_ANKLE]
28.    lh ← lm[LEFT_HEEL]
29.    rh ← lm[RIGHT_HEEL]
30.    lfi ← lm[LEFT_FOOT_INDEX]
31.    rfi ← lm[RIGHT_FOOT_INDEX]
32.    
33.    If la == null AND ra == null then
34.        Return FootDetectionResult(detected=false, W, H)
35.    End if
36.    
37.    // Score candidates: prioritize visibility, then lower Y (closer to bottom)
38.    candidates ← []
39.    If la != null then
40.        candidates.add(Candidate("LEFT", la, lh, lfi))
41.    End if
42.    If ra != null then
43.        candidates.add(Candidate("RIGHT", ra, rh, rfi))
44.    End if
45.    
46.    chosen ← candidates.maxBy(visibility(ankle)).thenByDescending(ankle.y)
47.    
48.    // Extract normalized coordinates [0, 1]
49.    ankleX ← clamp(chosen.ankle.x(), 0, 1)
50.    ankleY ← clamp(chosen.ankle.y(), 0, 1)
51.    toeX ← chosen.toe?.x() != null ? clamp(chosen.toe.x(), 0, 1) : null
52.    toeY ← chosen.toe?.y() != null ? clamp(chosen.toe.y(), 0, 1) : null
53.    visibility ← chosen.ankle.visibility()
54.    
55.    Return FootDetectionResult(
56.        detected=true,
57.        imgWidth=W,
58.        imgHeight=H,
59.        side=chosen.side,
60.        ankleX=ankleX,
61.        ankleY=ankleY,
62.        toeX=toeX,
63.        toeY=toeY,
64.        visibility=visibility,
65.        timestampNs=currentTime()
66.    )
End Function
```

**Key Implementation Details**:

- **LIVE_STREAM Mode**: For real-time processing, frames are processed asynchronously with results queued for retrieval.
- **Visibility Gating**: Landmarks with visibility below threshold (typically 0.3) are rejected to ensure accuracy.
- **Side Selection**: The algorithm prioritizes the foot with higher ankle visibility and lower Y-coordinate (closer to camera).
- **Normalized Coordinates**: All coordinates are normalized to [0, 1] range for device-independent processing.

---

### 2.2 Algorithm 2: 2D to 3D Coordinate Conversion

**Purpose**: Convert normalized 2D foot landmarks to 3D world coordinates using ARCore hit testing or VIO-based ray casting.

**Technology**: ARCore hit testing, plane detection, coordinate transformation

**Input**: Normalized foot landmarks (ankleX, ankleY, toeX, toeY), ARCore Frame (or VIO pose provider), view dimensions

**Output**: 3D world coordinates for ankle and toe positions, hit distance, trackable type

**Algorithm Description**: The algorithm transforms normalized image coordinates through multiple coordinate systems (normalized → image pixels → view coordinates → world coordinates) using ARCore's hit testing or VIO ray casting. It performs multi-sample hit testing for robustness and validates results based on distance and plane quality.

**Pseudocode**:

```
Algorithm 2 2D to 3D Coordinate Conversion

Input: Normalized landmarks L (ankleX, ankleY, toeX, toeY), 
       ARCore Frame F (or VIO poseProvider P), 
       image dimensions (imgWidth, imgHeight),
       view dimensions (viewWidth, viewHeight)
Output: 3D world coordinates for ankle and toe, hit distance D, trackable type T

1. // Convert normalized coordinates to image pixels
2. srcX ← L.ankleX * imgWidth
3. srcY ← L.ankleY * imgHeight
4. Clamp srcX to [0, imgWidth-1]
5. Clamp srcY to [0, imgHeight-1]
6. 
7. If F != null then  // ARCore mode
8.     // Transform image pixels to view coordinates
9.     inBuf ← ByteBuffer([srcX, srcY])
10.    outBuf ← ByteBuffer(2)
11.    F.transformCoordinates2d(IMAGE_PIXELS, inBuf, VIEW, outBuf)
12.    viewX ← outBuf[0]
13.    viewY ← outBuf[1]
14.    
15.    // Multi-sample hit testing for robustness
16.    sampleHits ← []
17.    sampleOffsets ← [(0,0), (-r,0), (r,0), (0,-r), (0,r)]  // r = sample radius
18.    
19.    For i ← 0 to SAMPLE_COUNT-1 do
20.        offset ← sampleOffsets[i % sampleOffsets.size]
21.        sampleX ← clamp(viewX + offset.x, 0, viewWidth-1)
22.        sampleY ← clamp(viewY + offset.y, 0, viewHeight-1)
23.        
24.        hits ← F.hitTest(sampleX, sampleY)
25.        bestHit ← null
26.        bestPref ← -1
27.        
28.        For each hit in hits do
29.            // Validate distance
30.            If hit.distance < MIN_DISTANCE OR hit.distance > MAX_DISTANCE then
31.                Continue
32.            End if
33.            
34.            // Validate plane quality
35.            If hit.trackable is Plane then
36.                If NOT (plane.isTracking AND plane.isPoseInPolygon(hit.hitPose)) then
37.                    Continue
38.                End if
39.            End if
40.            
41.            // Preference: Plane > Point > DepthPoint
42.            pref ← 0
43.            If hit.trackable is Plane then pref ← 3
44.            Else if hit.trackable is Point then pref ← 2
45.            Else if hit.trackable is DepthPoint then pref ← 1
46.            
47.            If pref > bestPref OR (pref == bestPref AND hit.distance < bestHit.distance) then
48.                bestHit ← hit
49.                bestPref ← pref
50.            End if
51.        End for
52.        
53.        If bestHit != null then
54.            sampleHits.add(bestHit)
55.        End if
56.    End for
57.    
58.    // Filter outliers (remove hits far from median distance)
59.    distances ← sort(sampleHits.map(hit → hit.distance))
60.    medianDistance ← distances[distances.size / 2]
61.    filteredHits ← sampleHits.filter(hit → |hit.distance - medianDistance| < 0.2)
62.    
63.    If filteredHits.isEmpty() then
64.        Return null
65.    End if
66.    
67.    // Weight hits: type preference × inverse distance
68.    weightedHits ← []
69.    For each hit in filteredHits do
70.        typeWeight ← (hit.type == "Plane" ? 3.0 : hit.type == "Point" ? 2.0 : 1.0)
71.        distanceWeight ← 1.0 / (hit.distance + 0.1)
72.        weight ← typeWeight * distanceWeight
73.        weightedHits.add((hit, weight))
74.    End for
75.    
76.    // Average translation weighted by preference and distance
77.    totalWeight ← sum(weightedHits.map(w → w.weight))
78.    avgX ← 0, avgY ← 0, avgZ ← 0
79.    For each (hit, weight) in weightedHits do
80.        w ← weight / totalWeight
81.        avgX += hit.hitPose[12] * w  // Translation X
82.        avgY += hit.hitPose[13] * w  // Translation Y
83.        avgZ += hit.hitPose[14] * w  // Translation Z
84.    End for
85.    
86.    // Use rotation from best weighted hit
87.    bestHit ← weightedHits.maxBy(w → w.weight).hit
88.    averagedMatrix ← copy(bestHit.hitPose)
89.    averagedMatrix[12] ← avgX
90.    averagedMatrix[13] ← avgY
91.    averagedMatrix[14] ← avgZ
92.    
93.    Return UnifiedHit(
94.        hitPoseMatrix=averagedMatrix,
95.        distance=avgDistance,
96.        trackableType=bestHit.type
97.    )
98.    
99. Else if P != null then  // VIO mode
100.    hits ← P.hitTest(viewX, viewY)
101.    bestHit ← hits.minBy(hit → hit.distance)
102.    Return bestHit
103. End if
```

**Key Implementation Details**:

- **Multi-Sample Hit Testing**: Performs 5 hit tests in a small radius around the target point to reduce noise and improve accuracy.
- **Outlier Filtering**: Removes hits that deviate significantly from the median distance (threshold: 20cm).
- **Weighted Averaging**: Combines multiple hits with weights based on trackable type preference and inverse distance.
- **Distance Validation**: Rejects hits outside realistic range (typically 0.3m to 3.0m).
- **Plane Quality Checks**: Validates that plane hits are tracking and the pose is within the plane polygon.

---

### 2.3 Algorithm 3: Pose Fusion and Stabilization

**Purpose**: Fuse Visual-Inertial Odometry (VIO) pose, plane detection, and foot landmarks into a stable, accurate shoe transformation matrix.

**Technology**: Exponential smoothing, Mahalanobis gating, drift detection

**Input**: VIO pose matrix (4×4), VIO covariance matrix (6×6, optional), plane information, foot landmarks (3D), timestamp

**Output**: Fused pose matrix (4×4), confidence score [0, 1]

**Algorithm Description**: The algorithm combines multiple sources of pose information (VIO tracking, detected planes, foot landmarks) to produce a stable shoe transform. It applies statistical filtering (Mahalanobis gating) to reject outliers, exponential smoothing for temporal stability, and drift detection for re-anchoring when necessary.

**Pseudocode**:

```
Algorithm 3 Pose Fusion and Stabilization

Input: VIO pose matrix V (4×4), VIO covariance C (6×6, optional),
       plane information P, foot landmarks FL (ankle3D, toe3D),
       timestamp T, previous fused pose F_prev (optional)
Output: Fused pose matrix F (4×4), confidence score conf [0, 1]

1. // Constants
2. SMOOTHING_ALPHA ← 0.7  // Exponential smoothing factor
3. DRIFT_THRESHOLD ← 0.15  // 15cm drift threshold
4. MAHALANOBIS_THRESHOLD ← 3.0  // 3-sigma threshold
5. MIN_CONFIDENCE ← 0.5
6. 
7. // Calculate initial shoe transform from inputs
8. shoeTransform ← calculateShoeTransform(V, P, FL)
9. If shoeTransform == null then
10.    Return F_prev  // Return previous pose if calculation fails
11. End if
12. 
13. // Mahalanobis gating: reject poses too far from expected distribution
14. If C != null AND F_prev != null then
15.    dx ← shoeTransform[12] - F_prev[12]  // Translation X difference
16.    dy ← shoeTransform[13] - F_prev[13]  // Translation Y difference
17.    dz ← shoeTransform[14] - F_prev[14]  // Translation Z difference
18.    
19.    // Extract diagonal covariance (position only)
20.    covX ← max(C[0], 0.01)
21.    covY ← max(C[7], 0.01)
22.    covZ ← max(C[14], 0.01)
23.    
24.    // Mahalanobis distance
25.    mahalDist ← sqrt((dx²/covX) + (dy²/covY) + (dz²/covZ))
26.    
27.    If mahalDist >= MAHALANOBIS_THRESHOLD then
28.        Return F_prev  // Reject pose, return previous
29.    End if
30. End if
31. 
32. // Apply exponential smoothing
33. If F_prev != null then
34.    smoothedTransform ← smoothPose(F_prev, shoeTransform)
35. Else
36.    smoothedTransform ← shoeTransform
37. End if
38. 
39. // Check for drift and re-anchor if needed
40. anchorPose ← getAnchorPose()
41. If anchorPose == null then
42.    setAnchorPose(smoothedTransform, T)
43. Else
44.    dx ← smoothedTransform[12] - anchorPose.translation[0]
45.    dy ← smoothedTransform[13] - anchorPose.translation[1]
46.    dz ← smoothedTransform[14] - anchorPose.translation[2]
47.    drift ← sqrt(dx² + dy² + dz²)
48.    
49.    If drift > DRIFT_THRESHOLD then
50.        setAnchorPose(smoothedTransform, T)  // Re-anchor
51.    End if
52. End if
53. 
54. // Calculate confidence
55. conf ← calculateConfidence(P, FL, C)
56. 
57. // Update fused pose
58. F ← FusedPose(
59.     matrix=smoothedTransform,
60.     confidence=conf,
61.     timestampNs=T
62. )
63. 
64. Return F

Function calculateShoeTransform(V, P, FL):
65.    If P == null OR FL == null then
66.        Return null
67.    End if
68.    
69.    // Get foot contact point on plane
70.    footContactPoint ← FL.getContactPoint(P)
71.    If footContactPoint == null then
72.        Return null
73.    End if
74.    
75.    // Calculate forward direction from ankle to toe
76.    forwardDir ← FL.getForwardDirection()  // [dx, dy, dz]
77.    If forwardDir == null then
78.        forwardDir ← [0, 0, 1]  // Default forward
79.    End if
80.    
81.    // Project forward direction onto plane
82.    planeNormal ← [0, 1, 0]  // Y-up floor plane
83.    projectedForward ← projectVectorOntoPlane(forwardDir, planeNormal)
84.    
85.    // Normalize forward direction
86.    forwardLen ← sqrt(projectedForward[0]² + projectedForward[2]²)
87.    If forwardLen < 0.01 then
88.        Return null  // Invalid direction
89. End if
90.    projectedForward[0] /= forwardLen
91.    projectedForward[2] /= forwardLen
92.    
93.    // Calculate yaw angle
94.    yaw ← atan2(projectedForward[0], projectedForward[2])
95.    
96.    // Build transformation matrix
97.    transform ← identityMatrix(4×4)
98.    transform[12] ← footContactPoint[0]  // Translation X
99.    transform[13] ← footContactPoint[1]  // Translation Y
100.    transform[14] ← footContactPoint[2]  // Translation Z
101.    rotateY(transform, yaw)  // Y-axis rotation
102.    scale(transform, scaleCalibration)  // Apply user calibration
103.    
104.    Return transform

Function smoothPose(previous, current):
105.    smoothed ← new FloatArray(16)
106.    For i ← 0 to 15 do
107.        smoothed[i] ← SMOOTHING_ALPHA * current[i] + (1 - SMOOTHING_ALPHA) * previous[i]
108.    End for
109.    Return smoothed

Function calculateConfidence(P, FL, C):
110.    conf ← 1.0
111.    If P == null then
112.        conf *= 0.5  // Reduce confidence without plane
113.    End if
114.    If FL == null then
115.        conf *= 0.5  // Reduce confidence without landmarks
116.    End if
117.    If C != null then
118.        avgCov ← (C[0] + C[7] + C[14]) / 3.0
119.        If avgCov > 1.0 then
120.            conf *= min(1.0 / avgCov, 1.0)  // Reduce confidence with high uncertainty
121.        End if
122.    End if
123.    Return clamp(conf, 0, 1)
End Function
```

**Key Implementation Details**:

- **Exponential Smoothing**: Alpha value of 0.7 provides balance between responsiveness and stability (higher = less smoothing, more responsive).
- **Mahalanobis Gating**: Uses 3-sigma threshold to reject statistically unlikely poses, preventing sudden jumps.
- **Drift Detection**: Monitors distance from anchor point; re-anchors when drift exceeds 15cm to prevent accumulation of tracking errors.
- **Confidence Calculation**: Multi-factor confidence based on plane availability, landmark quality, and VIO uncertainty.
- **Coordinate System**: Assumes Y-up world coordinate system with floor plane normal [0, 1, 0].

---

### 2.4 Algorithm 4: Outfit Matching and Recommendation

**Purpose**: Analyze outfit images to extract dominant colors and recommend complementary shoes from the product catalog.

**Technology**: Color extraction (client-side or server-side), MongoDB query matching

**Input**: Outfit image URL (or image data), user ID, product catalog

**Output**: List of recommended shoe products matching outfit colors

**Algorithm Description**: The algorithm extracts dominant colors from the outfit image (typically using K-means clustering or color histogram analysis), queries the product database for shoes with matching colors, and returns ranked recommendations. The system stores outfit analysis results for history tracking.

**Pseudocode**:

```
Algorithm 4 Outfit Matching and Recommendation

Input: Outfit image URL I (or image data), user ID U, product catalog DB
Output: List of recommended shoe products R (max 5 products)

1. // Extract dominant colors from outfit image
2. dominantColors ← extractDominantColors(I)
3. // Typically: K-means clustering with k=3, or color histogram analysis
4. // Returns: Array of color strings (e.g., ["#FF5733", "#33FF57", "#3357FF"])
5. 
6. // Store outfit analysis for history
7. outfitMatch ← createOutfitMatch(
8.     userId=U,
9.     outfitImageUrl=I,
10.    dominantColors=dominantColors,
11.    recommendedShoeIds=[],
12.    createdAt=currentTime()
13. )
14. saveToDatabase(outfitMatch)
15. 
16. // Query products matching dominant colors
17. recommendedShoes ← queryProducts(dominantColors, limit=5)
18. 
19. // Update outfit match with recommendations
20. outfitMatch.recommendedShoeIds ← recommendedShoes.map(s → s.id)
21. updateDatabase(outfitMatch)
22. 
23. Return recommendedShoes

Function extractDominantColors(image):
24.    // Method 1: K-means clustering (if implemented)
25.    // pixels ← extractPixels(image)
26.    // clusters ← kmeans(pixels, k=3)
27.    // dominantColors ← clusters.centroids.map(c → rgbToHex(c))
28.    
29.    // Method 2: Color histogram analysis (simpler)
30.    // histogram ← computeColorHistogram(image)
31.    // dominantColors ← topK(histogram, k=3)
32.    
33.    // Current implementation: Client-side extraction, passed to backend
34.    // Returns: Array of color hex strings
35.    Return clientExtractedColors  // Passed from Flutter client

Function queryProducts(colors, limit):
36.    // MongoDB query: Find products where colors array contains any of dominant colors
37.    query ← {
38.        colors: { $in: colors }  // Match any color in the array
39.    }
40.    
41.    products ← DB.products.find(query).limit(limit)
42.    
43.    // Optional: Rank by relevance (number of matching colors)
44.    rankedProducts ← products.sort((a, b) → {
45.        matchesA ← countMatchingColors(a.colors, colors)
46.        matchesB ← countMatchingColors(b.colors, colors)
47.        Return matchesB - matchesA  // Descending order
48.    })
49.    
50.    Return rankedProducts

Function countMatchingColors(productColors, dominantColors):
51.    count ← 0
52.    For each color in dominantColors do
53.        If productColors.contains(color) then
54.            count++
55.        End if
56.    End for
57.    Return count
End Function
```

**Key Implementation Details**:

- **Color Extraction**: Currently performed client-side in Flutter (using image processing libraries) and passed to backend. Future implementation may use server-side K-means clustering.
- **Database Query**: Uses MongoDB's `$in` operator to match products containing any of the dominant colors.
- **Ranking**: Products are ranked by the number of matching colors (more matches = higher relevance).
- **History Tracking**: All outfit analyses are stored with timestamps for user history retrieval.
- **Limitations**: Current implementation uses basic color matching. Future enhancements may include style analysis, pattern recognition, and machine learning-based recommendations.

---

## 3. Summary

SoleMate integrates multiple sophisticated algorithms to deliver a seamless AR shoe try-on experience:

1. **Foot Detection** enables real-time tracking of user's feet using MediaPipe pose estimation.
2. **2D to 3D Conversion** accurately maps screen coordinates to world space using ARCore hit testing with multi-sample validation.
3. **Pose Fusion** stabilizes shoe placement through statistical filtering and temporal smoothing.
4. **Outfit Matching** provides intelligent recommendations based on color harmony analysis.

These algorithms work together to create an immersive, accurate, and user-friendly AR shopping experience that addresses the limitations of traditional online footwear retail.

---

**Document Version**: 1.0  
**Last Updated**: 2025-01-XX  
**Authors**: SoleMate Development Team

