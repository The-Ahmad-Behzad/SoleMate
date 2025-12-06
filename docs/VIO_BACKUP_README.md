# VIO Code Backup

## Overview

This directory contains backups of the original VIO (Visual-Inertial Odometry) implementation that was replaced with a simpler SimpleCamera approach.

## Why the Backup?

The original VIO implementation was showing a black screen on non-ARCore devices. After investigation, it was determined that the complex native VIO library integration and SurfaceTexture/OpenGL texture binding issues were causing the problem.

A simpler approach was implemented that focuses on:
1. Getting camera preview working first (SimpleCameraManager)
2. Basic IMU-based orientation tracking (SimpleCameraPoseProvider)
3. No complex native VIO library dependencies

## Backed Up Files

### Kotlin Files
- `VioEngine.kt.backup` - Kotlin wrapper for native VIO engine
- `VioPoseProvider.kt.backup` - VIO implementation of WorldPoseProvider
- `Camera2Manager.kt.backup` - Camera2 API manager with VIO integration
- `PlaneEstimator.kt.backup` - RANSAC-based plane estimation

### Native C++ Files
- `android/app/src/main/cpp/vio_backup/` - Contains all native VIO implementation files:
  - `VioEngine.h` - VIO engine header
  - `VioEngine.cpp` - VIO engine implementation
  - `VioBridge.cpp` - JNI bridge
  - `vio_config.h` - Configuration header

## Original Location

All files were originally located in:
- Kotlin: `android/app/src/main/kotlin/com/solemate/app/solemate_app/`
- Native: `android/app/src/main/cpp/vio/`

## Replacement Implementation

The VIO code was replaced with:
- `SimpleCameraPoseProvider.kt` - Simple camera preview with IMU tracking
- `SimpleCameraManager.kt` - Simplified Camera2 manager (no VIO integration)

## Restoration

To restore the original VIO implementation:
1. Copy files from `vio_backup/` back to their original locations
2. Update `ARActivity.kt` to use `VioPoseProvider` and `Camera2Manager` instead of `SimpleCameraPoseProvider` and `SimpleCameraManager`
3. Ensure native library `libvio-engine.so` is built and linked correctly
4. Update `CMakeLists.txt` to include VIO native sources

## Date Backed Up

Backup created: 2025-01-18




