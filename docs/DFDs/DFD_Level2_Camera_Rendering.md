# Level 2 DFD - Camera Rendering Process (1.2)

## Description

This Level 2 DFD expands Process 1.2 (RENDER CAMERA) from the Level 1 AR Overlay diagram. It details the camera feed rendering pipeline that handles camera initialization, pose tracking, background texture rendering, and display rotation management for both ARCore and VIO modes.

### Sub-Processes

1. **1.2.1 INITIALIZE CAMERA**: Sets up camera system for AR rendering. Detects ARCore availability, initializes ARCore session (or VIO fallback), configures camera texture (external OES), creates SurfaceTexture for camera feed, and sets up camera permissions.

2. **1.2.2 UPDATE CAMERA POSE**: Tracks and updates camera position and orientation. Receives camera pose from AR session, updates camera transformation matrix, handles pose smoothing, and manages camera tracking state.

3. **1.2.3 RENDER BACKGROUND**: Renders camera feed as background texture. Binds camera texture to OpenGL, draws camera frame to screen, handles texture updates, and manages rendering pipeline.

4. **1.2.4 HANDLE DISPLAY ROTATION**: Manages device orientation changes. Detects display rotation events, updates viewport dimensions, adjusts camera projection matrix, and handles orientation transitions.

### Data Stores

- **D1.2.1 CAMERA TEXTURE**: Stores camera texture information including OpenGL texture ID, texture dimensions, texture format (external OES), and texture binding state.

- **D1.2.2 CAMERA POSE**: Stores camera pose data including camera position (X, Y, Z), camera orientation (quaternion/matrix), pose timestamp, and tracking confidence.

- **D1.2.3 DISPLAY STATE**: Stores display configuration including viewport width/height, display rotation angle, screen orientation, and projection matrix parameters.

### External Entities (from parent levels)

- **AR_CORE**: Provides AR session data and camera tracking
- **D1.4 AR SESSION STATE**: Parent-level data store for AR session information

### Key Data Flows

**Initialize Camera (1.2.1) Flows:**
- Receives AR session configuration from parent-level D1.4
- Detects ARCore availability from AR_CORE
- Initializes camera system (ARCore or VIO)
- Creates camera texture and stores in D1.2.1
- Sets up SurfaceTexture for camera feed
- Sends camera ready status to Update Camera Pose (1.2.2)

**Update Camera Pose (1.2.2) Flows:**
- Receives camera pose updates from AR_CORE (via D1.4)
- Updates camera transformation matrix
- Stores camera pose in D1.2.2
- Sends pose data to Render Background (1.2.3)
- Provides pose to Handle Display Rotation (1.2.4)

**Render Background (1.2.3) Flows:**
- Receives camera texture from D1.2.1
- Receives camera pose from D1.2.2
- Receives display state from D1.2.3
- Binds texture and renders camera frame
- Outputs rendered frame to User (via parent level)

**Handle Display Rotation (1.2.4) Flows:**
- Detects rotation events from system
- Updates display state in D1.2.3
- Adjusts viewport and projection matrix
- Notifies Render Background (1.2.3) of changes
- Receives camera pose from D1.2.2 for coordinate adjustment

## Mermaid.js Diagram Code

```mermaid
flowchart TB
    %% External Entities
    ARCORE[AR_CORE]
    USER[USER]
    
    %% Parent-level Data Stores
    D1_4_PARENT[("D1.4<br/>AR SESSION STATE")]
    
    %% Sub-Processes
    INIT_CAMERA["1.2.1<br/>INITIALIZE CAMERA"]
    UPDATE_POSE["1.2.2<br/>UPDATE CAMERA POSE"]
    RENDER_BG["1.2.3<br/>RENDER BACKGROUND"]
    HANDLE_ROTATION["1.2.4<br/>HANDLE DISPLAY ROTATION"]
    
    %% Data Stores
    D1_2_1[("D1.2.1<br/>CAMERA TEXTURE")]
    D1_2_2[("D1.2.2<br/>CAMERA POSE")]
    D1_2_3[("D1.2.3<br/>DISPLAY STATE")]
    
    %% AR Session Input
    ARCORE <-->|AR Session Data| D1_4_PARENT
    D1_4_PARENT -->|Session Configuration| INIT_CAMERA
    D1_4_PARENT -->|Camera Pose Updates| UPDATE_POSE
    D1_4_PARENT -->|Tracking State| UPDATE_POSE
    
    %% Initialize Camera Flows
    ARCORE -->|ARCore Availability| INIT_CAMERA
    INIT_CAMERA -->|Camera Texture| D1_2_1
    INIT_CAMERA -->|Camera Ready| UPDATE_POSE
    
    %% Update Camera Pose Flows
    UPDATE_POSE -->|Camera Pose| D1_2_2
    D1_2_2 -->|Pose Data| RENDER_BG
    D1_2_2 -->|Pose Data| HANDLE_ROTATION
    
    %% Render Background Flows
    D1_2_1 -->|Texture ID| RENDER_BG
    D1_2_3 -->|Viewport Info| RENDER_BG
    RENDER_BG -->|Camera Feed| USER
    
    %% Handle Display Rotation Flows
    HANDLE_ROTATION -->|Display State| D1_2_3
    HANDLE_ROTATION -->|Rotation Events| RENDER_BG
    
    %% Internal Process Flows
    INIT_CAMERA -->|SurfaceTexture| UPDATE_POSE
    UPDATE_POSE -->|Pose Updates| RENDER_BG
    
    %% Styling
    classDef process fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    classDef dataStore fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef externalEntity fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    classDef parentDataStore fill:#fce4ec,stroke:#880e4f,stroke-width:2px,stroke-dasharray: 5 5
    
    class INIT_CAMERA,UPDATE_POSE,RENDER_BG,HANDLE_ROTATION process
    class D1_2_1,D1_2_2,D1_2_3 dataStore
    class ARCORE,USER externalEntity
    class D1_4_PARENT parentDataStore
```

## Notes

- This diagram expands Process 1.2 from Level 1 DFD
- The process supports both ARCore and VIO (fallback) camera modes
- Camera initialization happens once at session start
- Camera pose updates occur every frame for real-time tracking
- Display rotation handling ensures correct rendering regardless of device orientation
- The camera texture uses OpenGL external OES format for direct camera feed rendering
- All processes work together to provide smooth, real-time camera background rendering
- The rendered camera feed serves as the background for AR overlay visualization

