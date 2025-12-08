# Data Flow Diagrams (DFD) Index - SoleMate System

## Overview

This document provides an index to all Data Flow Diagrams (DFD) created for the SoleMate AR-based footwear try-on application. The DFDs follow Gane and Sarson notation and are organized hierarchically from Context Diagram to detailed Level 2 diagrams.

## DFD Hierarchy

```
Context Diagram (Level 0)
    └── Diagram 0 (Core Functionality)
            └── Level 1: AR Overlay (1.0)
                    ├── Level 2: Foot Detection (1.1)
                    ├── Level 2: Camera Rendering (1.2)
                    └── Level 2: 3D Model Placement (1.3)
```

## Diagram List

### 1. Context Diagram (Level 0)
**File**: `DFD_Context_Diagram.md`

**Description**: Top-level view showing SoleMate system boundaries and all external entities.

**External Entities**:
- USER
- FIREBASE_AUTH
- MONGODB
- CLOUD_STORAGE
- AR_CORE

**Key Features**:
- Shows system as single black box (Process 0)
- Identifies all external interfaces
- Establishes system scope

---

### 2. Diagram 0 DFD (Core Functionality)
**File**: `DFD_Diagram_0.md`

**Description**: Expands Context Diagram to show major internal processes and data stores.

**Processes**:
- 1.0 AR OVERLAY
- 2.0 MANAGE CATALOG
- 3.0 MATCH OUTFIT
- 4.0 MANAGE TRY-ON CLOSET
- 5.0 MANAGE USER AUTHENTICATION
- 6.0 CUSTOMIZE SHOE SKINS

**Data Stores**:
- D1 PRODUCTS
- D2 TRY-ON HISTORY
- D3 USER PROFILES
- D4 OUTFIT MATCHES
- D5 CUSTOM SKINS
- D6 CALIBRATION DATA

**Key Features**:
- Shows all major system functions
- Reveals internal data stores
- Maintains all external entity connections

---

### 3. Level 1 DFD - AR Overlay Process (1.0)
**File**: `DFD_Level1_AR_Overlay.md`

**Description**: Expands AR Overlay process into detailed sub-processes.

**Sub-Processes**:
- 1.1 DETECT FOOT
- 1.2 RENDER CAMERA
- 1.3 PLACE 3D MODEL
- 1.4 MANAGE AR SESSION
- 1.5 PROCESS USER INPUT

**Data Stores**:
- D1.1 FOOT LANDMARKS
- D1.2 CAMERA FRAMES
- D1.3 ANCHOR POSITIONS
- D1.4 AR SESSION STATE

**Key Features**:
- Details AR try-on pipeline
- Shows real-time processing flow
- Integrates foot detection, rendering, and placement

---

### 4. Level 2 DFD - Foot Detection (1.1)
**File**: `DFD_Level2_Foot_Detection.md`

**Description**: Details MediaPipe-based foot detection pipeline.

**Sub-Processes**:
- 1.1.1 CAPTURE CAMERA FRAME
- 1.1.2 PROCESS WITH MEDIAPIPE
- 1.1.3 EXTRACT FOOT LANDMARKS
- 1.1.4 VALIDATE DETECTION

**Data Stores**:
- D1.1.1 RAW FRAMES
- D1.1.2 POSE RESULTS
- D1.1.3 LANDMARK COORDINATES

**Key Features**:
- MediaPipe Pose Landmarker integration
- Real-time frame processing
- Landmark extraction and validation

---

### 5. Level 2 DFD - Camera Rendering (1.2)
**File**: `DFD_Level2_Camera_Rendering.md`

**Description**: Details camera feed rendering and display management.

**Sub-Processes**:
- 1.2.1 INITIALIZE CAMERA
- 1.2.2 UPDATE CAMERA POSE
- 1.2.3 RENDER BACKGROUND
- 1.2.4 HANDLE DISPLAY ROTATION

**Data Stores**:
- D1.2.1 CAMERA TEXTURE
- D1.2.2 CAMERA POSE
- D1.2.3 DISPLAY STATE

**Key Features**:
- ARCore and VIO mode support
- Real-time camera pose tracking
- Display rotation handling

---

### 6. Level 2 DFD - 3D Model Placement (1.3)
**File**: `DFD_Level2_3D_Model_Placement.md`

**Description**: Details 3D shoe model placement and tracking pipeline.

**Sub-Processes**:
- 1.3.1 CONVERT 2D TO 3D
- 1.3.2 PERFORM HIT TEST
- 1.3.3 CALCULATE MODEL POSE
- 1.3.4 RENDER SHOE MODEL
- 1.3.5 UPDATE MODEL TRACKING

**Data Stores**:
- D1.3.1 3D FOOT POSITION
- D1.3.2 HIT TEST RESULTS
- D1.3.3 MODEL MATRIX
- D1.3.4 SHOE MODEL ASSETS

**Key Features**:
- 2D to 3D coordinate conversion
- Hit testing and plane detection
- Filament rendering integration
- Real-time tracking and smoothing

---

## Supporting Documents

### 7. DFD Validation Summary
**File**: `DFD_Validation_Summary.md`

**Description**: Comprehensive validation of all DFDs for proper balancing and leveling.

**Contents**:
- Balancing validation (parent-child consistency)
- Leveling validation (process decomposition)
- Data store numbering validation
- External entity consistency check
- Summary and recommendations

---

## Notation Guide

### Process Symbols
- **Rounded Rectangles**: Represent processes (functions that transform data)
- **Numbering**: Hierarchical (1.0 → 1.1 → 1.1.1)

### Data Store Symbols
- **Open Rectangles**: Represent data stores (repositories of data)
- **Numbering**: Hierarchical (D1 → D1.1 → D1.1.1)

### External Entity Symbols
- **3D Boxes**: Represent external entities (outside system boundary)

### Data Flow Symbols
- **Arrows**: Represent data flows (movement of data)
- **Labels**: Describe the data being moved

## Usage Instructions

1. **Viewing Diagrams**: Open individual markdown files to view descriptions and mermaid.js code
2. **Rendering Mermaid**: Use mermaid-compatible viewers:
   - GitHub (renders automatically)
   - Mermaid Live Editor: https://mermaid.live
   - VS Code with Mermaid extension
   - Erasor.io (for visual editing)
3. **Physical DFDs**: Convert mermaid.js to physical diagrams using:
   - draw.io
   - Microsoft Visio
   - Lucidchart
   - Any DFD-compatible tool

## Key System Components Documented

### AR Try-On Module
- Foot detection using MediaPipe
- Camera rendering (ARCore/VIO)
- 3D model placement and tracking
- AR session management

### Backend Integration
- Product catalog management
- Try-on history storage
- User authentication
- Outfit matching
- Custom skin management

## Related Documentation

- `SoleMate_Architecture_Plan.md` - System architecture overview
- `BACKEND_ARCHITECTURE.md` - Backend system design
- `AR-Feature-Plan.md` - AR implementation details
- `IMPLEMENTATION_STATUS.md` - Feature implementation status

## Maintenance

**Last Updated**: 2025-01-18
**Version**: 1.0
**Status**: Complete and Validated

All DFDs have been validated for proper balancing and leveling. The diagrams accurately represent the SoleMate system's data flows and processes.

