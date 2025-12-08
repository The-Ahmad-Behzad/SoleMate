package com.solemate.app.solemate_app

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLES11Ext
import android.opengl.GLSurfaceView
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.Coordinates2d
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SimpleRenderer(
    private val session: Session? = null,  // ARCore session (nullable for VIO mode)
    private val rotationHelper: DisplayRotationHelper,
    private val activity: ARActivity,
    private var shoeRenderer: ShoeRenderer? = null,
    private val poseProvider: WorldPoseProvider? = null  // VIO pose provider (nullable for ARCore mode)
) : GLSurfaceView.Renderer {

    private val backgroundRenderer = BackgroundRenderer()
    private val planeRenderer = PlaneRenderer()
    private val anchors = mutableListOf<Anchor>()
    private val vioAnchors = mutableListOf<FloatArray>() // Model matrices for VIO anchors
    
    // VIO mode state
    private var cameraReady = false
    private var cameraInitStartTime = 0L
    private val cameraInitTimeoutMs = 5000L
    
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    // ==== Detection infrastructure ====
    private val detectionExecutor = Executors.newSingleThreadExecutor()
    private val detector = FootTracker(activity)
    private val isProcessing = AtomicBoolean(false)
    private var frameCounter = 0
    private val frameSkip = 3 // process every 3rd frame (tune as needed)
    private data class FootPoint(
        val imgWidth: Int,
        val imgHeight: Int,
        val side: String,
        val ankleX: Float,      // normalized 0-1
        val ankleY: Float,
        val toeX: Float?,
        val toeY: Float?,
        val heelX: Float?,
        val heelY: Float?,
        val visibility: Float
    )
    private val pendingFootPoint = AtomicReference<FootPoint?>(null)

    // ==== Filament shoe rendering ====
    // shoeRenderer is now passed via constructor from ARActivity
    private var shoeModelMatrix: FloatArray? = null
    private var shoePlaced = false
    private var lastPlacementTime = 0L
    private val placementCooldownMs = 500L  // Update placement every 500ms max
    private val soleOffsetMeters = 0.02f
    private val baseModelScale = 0.05f  // Base scale for the shoe model (5% of original size)
    private val minFootLength = 0.15f   // Minimum realistic foot length (15cm)
    private val maxFootLength = 0.35f   // Maximum realistic foot length (35cm)
    private val referenceFootLength = 0.26f  // Average adult foot length (26cm)
    private val maxHitDistance = 3.0f   // Maximum distance for valid hit-test (3 meters)
    private val minVisibility = 0.15f    // Minimum landmark visibility threshold (lowered to allow more detections)
    
    // Recalibration request flag
    @Volatile
    private var recalibrationRequested = false

    fun release() {
        try {
            detector.close()
        } catch (_: Exception) { }
        try {
            detectionExecutor.shutdownNow()
        } catch (_: Exception) { }
        try {
            shoeRenderer?.release()
        } catch (_: Exception) { }
    }
    
    /**
     * Request immediate recalibration of shoe placement.
     * Called from UI button. Resets placement state and clears cooldown.
     */
    fun requestRecalibration() {
        recalibrationRequested = true
        Log.d("SimpleRenderer", "🔄 Recalibration requested")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("SimpleRenderer", "✅ onSurfaceCreated() called, session=${session != null}, poseProvider=${poseProvider != null}")
        // Use light gray so we can see if renderer is working (will be overridden by camera feed)
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1f)
        backgroundRenderer.createOnGlThread()
        
        if (session != null) {
            // ARCore mode
            Log.d("SimpleRenderer", "Setting up ARCore texture")
            session.setCameraTextureName(backgroundRenderer.getTextureId())
        } else if (poseProvider is SimpleCameraPoseProvider || poseProvider is VioPoseProvider) {
            // SimpleCamera or VIO mode: create external OES texture for Camera2 SurfaceTexture
            val modeName = if (poseProvider is SimpleCameraPoseProvider) "SimpleCamera" else "VIO"
            Log.d("SimpleRenderer", "Setting up $modeName texture")
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            val textureId = textures[0]
            Log.d("SimpleRenderer", "Created texture ID: $textureId")
            
            // Bind and configure texture as external OES (required for SurfaceTexture)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            
            // Set texture ID in pose provider
            poseProvider.setCameraTextureName(textureId)
            Log.d("SimpleRenderer", "Set texture ID in $modeName pose provider")
            
            // Check if SurfaceTexture already exists (from previous session)
            val existingSurfaceTexture = activity.getCameraSurfaceTexture()
            if (existingSurfaceTexture != null) {
                Log.d("SimpleRenderer", "Existing SurfaceTexture found, detaching before creating new one")
                try {
                    existingSurfaceTexture.detachFromGLContext()
                    Log.d("SimpleRenderer", "Existing SurfaceTexture detached successfully")
                } catch (e: Exception) {
                    Log.d("SimpleRenderer", "Existing SurfaceTexture not attached (expected): ${e.message}")
                }
                try {
                    existingSurfaceTexture.release()
                    Log.d("SimpleRenderer", "Existing SurfaceTexture released")
                } catch (e: Exception) {
                    Log.w("SimpleRenderer", "Error releasing existing SurfaceTexture: ${e.message}")
                }
            }
            
            // Create new SurfaceTexture with the texture ID using activity's helper method
            val surfaceTexture = activity.createCameraSurfaceTexture(textureId)
            if (surfaceTexture != null) {
                Log.d("SimpleRenderer", "SurfaceTexture created successfully, textureId=$textureId")
                
                // SurfaceTexture is already attached to the texture ID in createCameraSurfaceTexture()
                // Now we can open the camera
                try {
                    cameraInitStartTime = System.currentTimeMillis()
                    activity.openVioCamera()
                    Log.d("SimpleRenderer", "Camera open requested after SurfaceTexture creation")
                    // Camera will be ready when first frame arrives
                    cameraReady = false  // Will be set to true when first frame is received
                } catch (e: Exception) {
                    Log.e("SimpleRenderer", "Error opening camera: ${e.message}", e)
                    Log.e("SimpleRenderer", "Stack trace:", e)
                    cameraReady = false
                    cameraInitStartTime = 0L
                }
            } else {
                Log.e("SimpleRenderer", "Failed to create SurfaceTexture! Cannot open camera")
                cameraReady = false
            }
        }
        
        planeRenderer.createOnGlThread()
        detector.init()
        // ShoeRenderer will be fully initialized when we know the surface size (onSurfaceChanged)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        surfaceWidth = width
        surfaceHeight = height
        rotationHelper.onSurfaceChanged(width, height)
        // ShoeRenderer is now managed by ARActivity via TextureView
        // No initialization needed here
    }

    override fun onDrawFrame(gl: GL10?) {
        frameCounter++
        
        // Clear with a visible color so we can see if renderer is working
        // Use dark gray instead of black so we can distinguish from "not rendering"
        GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        // Log first few frames to verify renderer is being called
        if (frameCounter <= 5) {
            Log.d("SimpleRenderer", "onDrawFrame() called, frame=$frameCounter, poseProvider=${poseProvider != null}, session=${session != null}")
        }

        // Update pose provider (for VIO mode) or session (for ARCore mode)
        if (poseProvider != null) {
            if (!poseProvider.update()) {
                if (frameCounter < 5) {
                    Log.d("SimpleRenderer", "Pose provider update failed")
                }
                return // Skip rendering if update failed
            }
        } else if (session != null) {
            rotationHelper.updateSessionIfNeeded(session)
        } else {
            if (frameCounter < 10) {
                Log.e("SimpleRenderer", "Neither session nor poseProvider available!")
            }
            return
        }

        // Matrices for shoe rendering (used in both ARCore and VIO modes)
        var projMatrix: FloatArray? = null
        var viewMatrix: FloatArray? = null
        
        try {
            // Draw background - different paths for ARCore vs VIO
            val frame: Frame? = if (session != null) {
                session.update()
            } else {
                null
            }
            
            if (session != null && frame != null) {
                // ARCore path: use external texture
                backgroundRenderer.draw(frame)
                
                // Matrices for drawing (ARCore)
                projMatrix = FloatArray(16)
                viewMatrix = FloatArray(16)
                frame.camera.getProjectionMatrix(projMatrix, 0, 0.1f, 100f)
                frame.camera.getViewMatrix(viewMatrix, 0)
                val viewProjMatrix = FloatArray(16)
                android.opengl.Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

                // Handle queued taps (existing anchor creation logic)
                val tap = activity.getQueuedSingleTap()
                if (tap != null && frame.camera.trackingState == TrackingState.TRACKING) {
                    for (hit in frame.hitTest(tap)) {
                        val trackable = hit.trackable
                        if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                            anchors.add(hit.createAnchor())
                            Log.d("ARAnchor", "✅ Anchor created at ${hit.hitPose.translation.contentToString()}")
                            break
                        }
                    }
                }

                // Draw all tracked planes
                val allPlanes = session.getAllTrackables(Plane::class.java)
                for (plane in allPlanes) {
                    if (plane.trackingState == TrackingState.TRACKING && plane.subsumedBy == null) {
                        planeRenderer.drawPlane(plane, viewProjMatrix)
                    }
                }

                // Draw anchor markers
                for (anchor in anchors) {
                    if (anchor.trackingState == TrackingState.TRACKING) {
                        val pose = anchor.pose
                        val modelMatrix = FloatArray(16)
                        pose.toMatrix(modelMatrix, 0)
                        planeRenderer.drawAnchorMarker(modelMatrix, viewProjMatrix)
                    }
                }
            } else if (poseProvider is SimpleCameraPoseProvider || poseProvider is VioPoseProvider) {
                // SimpleCamera or VIO path: use Camera2 texture
                // Check for camera initialization timeout
                if (cameraInitStartTime > 0 && !cameraReady) {
                    val elapsed = System.currentTimeMillis() - cameraInitStartTime
                    if (elapsed > cameraInitTimeoutMs) {
                        Log.e("SimpleRenderer", "Camera initialization timeout after ${elapsed}ms")
                        Log.e("SimpleRenderer", "Camera may not be responding. Check camera permissions and device compatibility.")
                        cameraInitStartTime = 0L  // Stop checking
                    } else if (elapsed % 1000 == 0L) {  // Log every second
                        Log.d("SimpleRenderer", "Waiting for camera... (${elapsed}ms elapsed)")
                    }
                }
                
                // Try to update and draw camera even if not ready yet (to detect first frame)
                val textureId = poseProvider.getCameraTextureId()
                if (textureId > 0) {
                    // Get SurfaceTexture and update texture on GL thread (required for SurfaceTexture)
                    val surfaceTexture = activity.getCameraSurfaceTexture()
                    if (surfaceTexture != null) {
                        try {
                            // Update texture with latest camera frame (must be on GL thread)
                            // This will throw if no frame is available yet, which is fine
                            surfaceTexture.updateTexImage()
                            
                            // Mark camera as ready on first successful frame update
                            if (!cameraReady && cameraInitStartTime > 0) {
                                val elapsed = System.currentTimeMillis() - cameraInitStartTime
                                Log.d("SimpleRenderer", "✅ Camera ready! First frame received after ${elapsed}ms")
                                cameraReady = true
                                cameraInitStartTime = 0L
                            }
                            
                            // Get transform matrix from SurfaceTexture
                            val matrix = FloatArray(16)
                            surfaceTexture.getTransformMatrix(matrix)
                            
                            // Get device rotation and sensor orientation
                            val deviceRotation = rotationHelper.getRotationDegrees()
                            val sensorOrientation = try {
                                activity.getSimpleCameraManager()?.getSensorOrientation() ?: 0
                            } catch (e: Exception) { 0 }
                            
                            // Log rotation info for debugging (only first few frames)
                            if (frameCounter <= 5) {
                                Log.d("SimpleRenderer", "Rotation debug: deviceRotation=$deviceRotation°, sensorOrientation=$sensorOrientation°")
                                Log.d("SimpleRenderer", "SurfaceTexture matrix: [${matrix[0]}, ${matrix[1]}, ${matrix[4]}, ${matrix[5]}]")
                            }
                            
                            // Try different rotation corrections based on device orientation
                            // If camera appears horizontal in portrait, we need to rotate it
                            // Convert device rotation to degrees
                            val deviceRotationDegrees = when (deviceRotation) {
                                1 -> 90
                                2 -> 180
                                3 -> 270
                                else -> 0
                            }
                            
                            // Calculate rotation dynamically
                            // We assume SurfaceTexture might already handle sensor orientation, 
                            // so we only compensate for device rotation.
                            val rotation = (360 - deviceRotationDegrees) % 360
                            var correctedMatrix = applyTextureRotation(matrix, rotation)
                            
                            // Apply horizontal flip to correct mirroring
                            correctedMatrix = applyHorizontalFlip(correctedMatrix)
                            
                            if (frameCounter % 60 == 0) {
                                Log.d("SimpleRenderer", "Rotation: device=$deviceRotationDegrees -> final=$rotation (with horizontal flip)")
                            }
                            
                            // Debug: Log the matrix values to understand what's happening
                            if (frameCounter <= 3) {
                                Log.d("SimpleRenderer", "Original matrix: [${matrix[0]}, ${matrix[1]}, ${matrix[4]}, ${matrix[5]}]")
                                Log.d("SimpleRenderer", "Corrected matrix: [${correctedMatrix[0]}, ${correctedMatrix[1]}, ${correctedMatrix[4]}, ${correctedMatrix[5]}]")
                            }
                            
                            // Draw camera feed
                            backgroundRenderer.drawCamera2Texture(textureId, correctedMatrix)
                            
                            // === SimpleCamera Foot Detection ===
                            // Use camera image from pose provider instead of glReadPixels
                            if (frameCounter % frameSkip == 0 && isProcessing.compareAndSet(false, true)) {
                                if (frameCounter % 60 == 0) {
                                    Log.d("SimpleRenderer", "SimpleCamera: Attempting to acquire camera image, frame=$frameCounter")
                                }
                                
                                val cameraImage = try {
                                    poseProvider.acquireCameraImage()
                                } catch (e: Exception) {
                                    if (frameCounter % 60 == 0) {
                                        Log.e("SimpleRenderer", "SimpleCamera: Failed to acquire camera image: ${e.message}")
                                    }
                                    null
                                }
                                
                                if (cameraImage != null) {
                                    if (frameCounter % 60 == 0) {
                                        Log.d("SimpleRenderer", "SimpleCamera: Camera image acquired: ${cameraImage.width}x${cameraImage.height}, type=${cameraImage.javaClass.simpleName}")
                                    }
                                    
                                    // Submit to background thread for detection; MUST close image
                                    detectionExecutor.submit {
                                        try {
                                            // Get device rotation for bitmap rotation
                                            val deviceRotation = rotationHelper.getRotationDegrees()
                                            val sensorOrientation = try {
                                                activity.getSimpleCameraManager()?.getSensorOrientation() ?: 90
                                            } catch (e: Exception) { 90 }
                                            
                                            // Convert CameraImage to Bitmap
                                            val bitmap = when (cameraImage) {
                                                is Camera2Image -> {
                                                    try {
                                                        val converted = YuvConverter.imageToBitmap(cameraImage.getCamera2Image())
                                                        if (frameCounter % 60 == 0) {
                                                            Log.d("SimpleRenderer", "SimpleCamera: Bitmap converted: ${converted.width}x${converted.height}")
                                                        }
                                                        
                                                        // Rotate bitmap to portrait orientation for MediaPipe
                                                        // Camera sensor is 90° (landscape), device is 0° (portrait)
                                                        // Need to rotate 90° clockwise to make it portrait
                                                        val rotationDegrees = when {
                                                            deviceRotation == 0 && sensorOrientation == 90 -> 90  // Portrait device, landscape sensor -> rotate 90° CW
                                                            deviceRotation == 90 -> 0  // Landscape left -> no rotation needed
                                                            deviceRotation == 180 -> 270  // Upside down -> rotate 270° CW
                                                            deviceRotation == 270 -> 180  // Landscape right -> rotate 180°
                                                            else -> 90  // Default: rotate 90° CW
                                                        }
                                                        
                                                        val rotatedBitmap = if (rotationDegrees != 0) {
                                                            val matrix = android.graphics.Matrix()
                                                            matrix.postRotate(rotationDegrees.toFloat())
                                                            android.graphics.Bitmap.createBitmap(
                                                                converted, 0, 0, converted.width, converted.height, matrix, true
                                                            ).also {
                                                                if (converted != it) converted.recycle()  // Recycle original if different
                                                                // Log rotation more frequently for debugging
                                                                if (frameCounter % 10 == 0) {
                                                                    Log.d("SimpleRenderer", "SimpleCamera: Bitmap rotated ${rotationDegrees}°: ${converted.width}x${converted.height} -> ${it.width}x${it.height}")
                                                                }
                                                            }
                                                        } else {
                                                            if (frameCounter % 10 == 0) {
                                                                Log.d("SimpleRenderer", "SimpleCamera: Bitmap not rotated (rotationDegrees=0): ${converted.width}x${converted.height}")
                                                            }
                                                            converted
                                                        }
                                                        
                                                        rotatedBitmap
                                                    } catch (e: Exception) {
                                                        Log.e("SimpleRenderer", "SimpleCamera: Bitmap conversion failed: ${e.message}", e)
                                                        null
                                                    }
                                                }
                                                else -> {
                                                    // Only Camera2Image is supported for SimpleCamera mode
                                                    Log.w("SimpleRenderer", "Unsupported camera image type: ${cameraImage.javaClass.simpleName}")
                                                    null
                                                }
                                            }
                                            
                                            if (bitmap != null) {
                                                if (frameCounter % 60 == 0) {
                                                    Log.d("SimpleRenderer", "SimpleCamera: Processing frame ${bitmap.width}x${bitmap.height} for detection")
                                                }
                                                
                                                val footResult = detector.detectFoot(bitmap)
                                                if (footResult.detected) {
                                                    // Log detection details more frequently
                                                    Log.d("SimpleRenderer", "✅ Foot detected: side=${footResult.side}, ankle=(${"%.3f".format(footResult.ankleX)},${"%.3f".format(footResult.ankleY)}), toe=(${footResult.toeX?.let{"%.3f".format(it)}},${footResult.toeY?.let{"%.3f".format(it)}}), vis=${"%.3f".format(footResult.visibility)}, imgSize=${footResult.imgWidth}x${footResult.imgHeight}")
                                                    
                                                    pendingFootPoint.set(
                                                        FootPoint(
                                                            imgWidth = footResult.imgWidth,
                                                            imgHeight = footResult.imgHeight,
                                                            side = footResult.side,
                                                            ankleX = footResult.ankleX,
                                                            ankleY = footResult.ankleY,
                                                            toeX = footResult.toeX,
                                                            toeY = footResult.toeY,
                                                            heelX = footResult.heelX,
                                                            heelY = footResult.heelY,
                                                            visibility = footResult.visibility
                                                        )
                                                    )
                                                } else {
                                                    if (frameCounter % 60 == 0) {
                                                        Log.d("SimpleRenderer", "SimpleCamera: No foot detected in frame ${bitmap.width}x${bitmap.height}")
                                                    }
                                                }
                                            } else {
                                                if (frameCounter % 60 == 0) {
                                                    Log.w("SimpleRenderer", "SimpleCamera: Bitmap is null, cannot process")
                                                }
                                            }
                                        } catch (t: Throwable) {
                                            Log.e("SimpleRenderer", "SimpleCamera detection error: ${t.message}", t)
                                        } finally {
                                            try {
                                                cameraImage.close()
                                            } catch (e: Exception) {
                                                // safe-guard
                                            }
                                            isProcessing.set(false)
                                        }
                                    }
                                } else {
                                    // Camera image not available for this frame - clear processing flag
                                    if (frameCounter % 60 == 0) {
                                        Log.w("SimpleRenderer", "SimpleCamera: Camera image is null, skipping detection")
                                    }
                                    isProcessing.set(false)
                                }
                            }
                            
                            // Log success occasionally
                            if (frameCounter % 60 == 0) {
                                Log.v("SimpleRenderer", "Camera frame drawn successfully, frame=$frameCounter")
                            }
                        } catch (e: Exception) {
                            // updateTexImage() throws if no frame is available yet - this is expected
                            if (cameraInitStartTime > 0) {
                                val elapsed = System.currentTimeMillis() - cameraInitStartTime
                                // Log more frequently at the start
                                if (elapsed < 3000 || elapsed % 500 == 0L) {
                                    Log.d("SimpleRenderer", "Waiting for camera frame... (${elapsed}ms elapsed, exception: ${e.javaClass.simpleName})")
                                }
                            } else if (frameCounter < 10) {
                                Log.d("SimpleRenderer", "updateTexImage() exception (expected if no frame yet): ${e.javaClass.simpleName}: ${e.message}")
                            }
                            // Don't draw this frame, but continue to next frame
                            return
                        }
                    } else {
                        if (frameCounter < 10 || frameCounter % 60 == 0) {
                            Log.w("SimpleRenderer", "SurfaceTexture not available, skipping draw (frame=$frameCounter)")
                        }
                        return
                    }
                } else {
                    if (frameCounter < 10 || frameCounter % 60 == 0) {
                        Log.w("SimpleRenderer", "Texture ID not set yet (textureId=$textureId), skipping draw (frame=$frameCounter)")
                    }
                    return
                }
                
                // VIO mode: get matrices from pose provider
                projMatrix = poseProvider.getProjectionMatrix(0.1f, 100f)
                viewMatrix = poseProvider.getViewMatrix()
                val viewProjMatrix = FloatArray(16)
                android.opengl.Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)
                
                // Draw VIO planes (if any)
                val planes = poseProvider.getPlanes()
                if (planes.isNotEmpty() && frameCounter % 60 == 0) {
                    Log.d("SimpleRenderer", "Drawing ${planes.size} VIO plane(s)")
                }
                for (planeInfo in planes) {
                    planeRenderer.drawPlane(planeInfo, viewProjMatrix)
                }
                
                // Draw VIO anchors
                if (vioAnchors.isNotEmpty() && frameCounter % 60 == 0) {
                    Log.d("SimpleRenderer", "Drawing ${vioAnchors.size} VIO anchor(s)")
                }
                for (anchorMatrix in vioAnchors) {
                    planeRenderer.drawAnchorMarker(anchorMatrix, viewProjMatrix)
                }
                
                // Continue to foot placement processing (don't return early)
            }

            // === Acquire camera image for ML (throttled, safe) ===
            // Only for ARCore mode (VIO mode handled separately above)
            if (session != null && frame != null) {
                frameCounter++
                if (frameCounter % frameSkip == 0 && isProcessing.compareAndSet(false, true)) {
                    val cameraImage = try {
                        frame.acquireCameraImage() // MAY throw NotYetAvailableException
                    } catch (nye: NotYetAvailableException) {
                        null
                    }
                
                    if (cameraImage != null) {
                        // Submit to background thread for detection; MUST close image
                        detectionExecutor.submit {
                            try {
                                // Convert CameraImage to Bitmap
                                val bitmap = when (cameraImage) {
                                    is ArcoreCameraImage -> {
                                        YuvConverter.imageToBitmap(cameraImage.getArImage())
                                    }
                                    is Camera2Image -> {
                                        YuvConverter.imageToBitmap(cameraImage.getCamera2Image())
                                    }
                                    else -> {
                                        // Try direct conversion for ARCore Image
                                        try {
                                            YuvConverter.imageToBitmap(cameraImage)
                                        } catch (e: Exception) {
                                            Log.e("SimpleRenderer", "Failed to convert camera image: ${e.message}")
                                            null
                                        }
                                    }
                                }
                                
                                if (bitmap != null) {
                                    // Run detection (replace with MediaPipe / TFLite inference)
                                    val footResult = detector.detectFoot(bitmap)

                                    if (footResult.detected) {
                                        pendingFootPoint.set(
                                            FootPoint(
                                                imgWidth = footResult.imgWidth,
                                                imgHeight = footResult.imgHeight,
                                                side = footResult.side,
                                                ankleX = footResult.ankleX,
                                                ankleY = footResult.ankleY,
                                                toeX = footResult.toeX,
                                                toeY = footResult.toeY,
                                                heelX = footResult.heelX,
                                                heelY = footResult.heelY,
                                                visibility = footResult.visibility
                                            )
                                        )
                                    }
                                }
                            } catch (t: Throwable) {
                                Log.e("SimpleRenderer", "Detection error: ${t.message}")
                            } finally {
                                try {
                                    cameraImage.close()
                                } catch (e: Exception) {
                                    // safe-guard
                                }
                                isProcessing.set(false)
                            }
                        }
                    } else {
                        // Camera image not available for this frame - clear processing flag
                        isProcessing.set(false)
                    }
                }
            }

            // === Handle recalibration request ===
            if (recalibrationRequested) {
                recalibrationRequested = false
                shoePlaced = false
                lastPlacementTime = 0L
                // Clear old anchors
                anchors.forEach { it.detach() }
                anchors.clear()
                vioAnchors.clear()
                Log.d("SimpleRenderer", "✅ Recalibration: Reset placement state")
            }
            
            // === If we have a pending foot point (normalized coords), transform to VIEW coords and hitTest ===
            val pendingFoot = pendingFootPoint.getAndSet(null)
            if (pendingFoot != null) {
                Log.d("FootAnchor", "📥 Processing pending foot detection: side=${pendingFoot.side}, vis=${pendingFoot.visibility}, mode=${if (session != null) "ARCore" else "VIO"}")
                // Check if we can process (ARCore or VIO)
                if ((session != null && frame != null) || poseProvider != null) {
                    try {
                        // Check tracking state
                        val isTracking = if (session != null && frame != null) {
                            frame.camera.trackingState == TrackingState.TRACKING
                        } else {
                            poseProvider?.isTracking() == true
                        }
                        
                        if (isTracking) {
                            // Check if enough time has passed since last placement (avoid spam)
                            val currentTime = System.currentTimeMillis()
                            val canUpdate = !shoePlaced || (currentTime - lastPlacementTime) > placementCooldownMs
                            
                            // Validate visibility threshold
                            if (pendingFoot.visibility >= minVisibility && canUpdate) {
                                Log.d("FootAnchor", "✅ Processing foot placement: vis=${pendingFoot.visibility} >= $minVisibility, canUpdate=$canUpdate")
                                processFootPlacement(pendingFoot, frame)
                            } else {
                                if (pendingFoot.visibility < minVisibility) {
                                    Log.d("FootAnchor", "⚠️ Low visibility ${pendingFoot.visibility} < $minVisibility, skipping")
                                } else if (!canUpdate) {
                                    Log.d("FootAnchor", "⚠️ Placement cooldown active, skipping")
                                }
                            }
                        } else {
                            Log.d("FootAnchor", "⚠️ Not tracking, skipping foot placement")
                        }
                    } catch (t: Throwable) {
                        Log.e("SimpleRenderer", "Foot placement failed: ${t.message}", t)
                    }
                } else {
                    Log.w("FootAnchor", "⚠️ No session or poseProvider available for foot placement")
                }
            }

            // ==== Update shoe camera/model matrices (Filament renders on its own thread) ====
            // Only update if we have valid matrices (ARCore or VIO mode)
            if (viewMatrix != null && projMatrix != null) {
                try {
                    shoeRenderer?.let { sr ->
                        // Only update matrices - TextureView handles rendering independently
                        sr.setCamera(viewMatrix!!, projMatrix!!)
                        shoeModelMatrix?.let { mm -> 
                            sr.setModelMatrix(mm)
                            if (frameCounter % 60 == 0) { // Log every 60 frames (~2 seconds)
                                Log.d("ShoeUpdate", "Sending matrix to renderer: pos=[${mm[12]}, ${mm[13]}, ${mm[14]}]")
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Log.e("ShoeUpdate", "Failed to update matrices: ${t.message}")
                }
            }

        } catch (e: CameraNotAvailableException) {
            e.printStackTrace()
            // Consider notifying user
        }
    }
    
    /**
     * Apply rotation to texture coordinates.
     * @param transformMatrix The SurfaceTexture transform matrix
     * @param degrees Rotation in degrees (90, 180, or 270)
     */
    private fun applyTextureRotation(transformMatrix: FloatArray, degrees: Int): FloatArray {
        val rotationMatrix = FloatArray(16)
        android.opengl.Matrix.setIdentityM(rotationMatrix, 0)
        
        when (degrees) {
            90 -> {
                // 90° clockwise: (x, y) -> (1-y, x)
                rotationMatrix[0] = 0f
                rotationMatrix[1] = 1f
                rotationMatrix[4] = -1f
                rotationMatrix[5] = 0f
                rotationMatrix[12] = 1f
                rotationMatrix[13] = 0f
            }
            180 -> {
                // 180°: (x, y) -> (1-x, 1-y)
                rotationMatrix[0] = -1f
                rotationMatrix[5] = -1f
                rotationMatrix[12] = 1f
                rotationMatrix[13] = 1f
            }
            270 -> {
                // 270° clockwise (90° counter-clockwise): (x, y) -> (y, 1-x)
                rotationMatrix[0] = 0f
                rotationMatrix[1] = -1f
                rotationMatrix[4] = 1f
                rotationMatrix[5] = 0f
                rotationMatrix[12] = 0f
                rotationMatrix[13] = 1f
            }
            else -> {
                // No rotation
                return transformMatrix
            }
        }
        
        // Multiply: result = rotationMatrix * transformMatrix
        val result = FloatArray(16)
        android.opengl.Matrix.multiplyMM(result, 0, rotationMatrix, 0, transformMatrix, 0)
        
        return result
    }
    
    /**
     * Apply horizontal flip to texture coordinates.
     * @param transformMatrix The texture transform matrix
     */
    private fun applyHorizontalFlip(transformMatrix: FloatArray): FloatArray {
        val flipMatrix = FloatArray(16)
        android.opengl.Matrix.setIdentityM(flipMatrix, 0)
        
        // Horizontal flip: (x, y) -> (1-x, y)
        flipMatrix[0] = -1f
        flipMatrix[12] = 1f
        
        // Multiply: result = flipMatrix * transformMatrix
        val result = FloatArray(16)
        android.opengl.Matrix.multiplyMM(result, 0, flipMatrix, 0, transformMatrix, 0)
        
        return result
    }
    // Unified Hit Result to handle both ARCore and VIO
    private data class UnifiedHit(
        val hitPoseMatrix: FloatArray,
        val distance: Float,
        val trackableType: String,
        val arCoreHit: com.google.ar.core.HitResult? = null
    )

    private fun processFootPlacement(fp: FootPoint, frame: Frame?) {
        // Helper function to convert normalized coords to pixels then to view coords
        fun normalizedToView(normX: Float, normY: Float, bias: Float = 0f): Pair<Float, Float>? {
            val srcX = (normX * fp.imgWidth).coerceIn(0f, fp.imgWidth - 1f)
            val srcY = ((normY + bias) * fp.imgHeight).coerceIn(0f, fp.imgHeight - 1f)
            
            if (frame != null) {
                // ARCore: Convert camera image coords to view coords
                val inBuf = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                inBuf.put(srcX)
                inBuf.put(srcY)
                inBuf.position(0)
                
                val outBuf = ByteBuffer.allocateDirect(2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
                frame.transformCoordinates2d(Coordinates2d.IMAGE_PIXELS, inBuf, Coordinates2d.VIEW, outBuf)
                
                outBuf.position(0)
                return Pair(outBuf.get(0), outBuf.get(1))
            } else {
                // VIO: Coords are already in screen space (captured via glReadPixels)
                return Pair(srcX, srcY)
            }
        }
        
        // Helper to perform hit-test and return best hit with distance validation
        fun hitTestPoint(viewX: Float, viewY: Float, label: String): UnifiedHit? {
            // Validate view coordinates are within screen bounds
            if (viewX < 0 || viewY < 0) {
                Log.w("FootAnchor", "❌ Invalid view coords for $label: ($viewX, $viewY)")
                return null
            }
            
            if (frame != null) {
                // ARCore Hit Test
                var bestHit: com.google.ar.core.HitResult? = null
                var bestPref = -1
                
                for (hit in frame.hitTest(viewX, viewY)) {
                    val tr = hit.trackable
                    
                    // Validate distance - reject hits too far away
                    if (hit.distance > maxHitDistance) {
                        Log.d("FootAnchor", "⚠️ $label hit too far: ${hit.distance}m > ${maxHitDistance}m")
                        continue
                    }
                    
                    val pref = when (tr) {
                        is com.google.ar.core.Plane -> 3
                        is com.google.ar.core.Point -> 2
                        is com.google.ar.core.DepthPoint -> 1
                        else -> 0
                    }
                    val ok = when (tr) {
                        is com.google.ar.core.Plane -> tr.isPoseInPolygon(hit.hitPose) && tr.trackingState == TrackingState.TRACKING
                        else -> true
                    }
                    if (ok && (pref > bestPref || (pref == bestPref && bestHit != null && hit.distance < bestHit!!.distance))) {
                        bestHit = hit
                        bestPref = pref
                    }
                }
                
                return bestHit?.let { hit ->
                    val poseMatrix = FloatArray(16)
                    hit.hitPose.toMatrix(poseMatrix, 0)
                    UnifiedHit(
                        hitPoseMatrix = poseMatrix,
                        distance = hit.distance,
                        trackableType = when (hit.trackable) {
                            is com.google.ar.core.Plane -> "Plane"
                            is com.google.ar.core.Point -> "Point"
                            is com.google.ar.core.DepthPoint -> "DepthPoint"
                            else -> "Other"
                        },
                        arCoreHit = hit
                    )
                }
            } else if (poseProvider != null) {
                // VIO Hit Test
                val hits = poseProvider.hitTest(viewX, viewY)
                // Prefer closest hit
                val bestHit = hits.minByOrNull { it.distance }
                
                return bestHit?.let { hit ->
                    UnifiedHit(
                        hitPoseMatrix = hit.hitPose.toMatrix(),
                        distance = hit.distance,
                        trackableType = hit.trackableType.name,
                        arCoreHit = null
                    )
                }
            }
            return null
        }
        
        // Hit-test ankle and toe positions to get 3D world coordinates
        Log.d("FootAnchor", "Processing foot: side=${fp.side} vis=${fp.visibility} ankle=(${fp.ankleX},${fp.ankleY}) toe=(${fp.toeX},${fp.toeY})")
        
        val ankleView = normalizedToView(fp.ankleX, fp.ankleY)
        val toeView = if (fp.toeX != null && fp.toeY != null) {
            normalizedToView(fp.toeX, fp.toeY, 0.005f) // small downward bias
        } else null
        
        Log.d("FootAnchor", "Converted to view coords: ankle=$ankleView toe=$toeView")
        
        val ankleHit = ankleView?.let { (vx, vy) -> hitTestPoint(vx, vy, "ankle") }
        val toeHit = toeView?.let { (vx, vy) -> hitTestPoint(vx, vy, "toe") }
        
        Log.d("FootAnchor", "Hit test results: ankleHit=${ankleHit != null} (d=${ankleHit?.distance?.let{"%.2f".format(it)}}) toeHit=${toeHit != null} (d=${toeHit?.distance?.let{"%.2f".format(it)}})")
        
        // Prefer toe for anchor, fall back to ankle
        val primaryHit = toeHit ?: ankleHit
        
        primaryHit?.let { chosen ->
            // Validate that ankle and toe are reasonably close to each other
            if (ankleHit != null && toeHit != null) {
                val distanceBetween = kotlin.math.abs(ankleHit.distance - toeHit.distance)
                if (distanceBetween > 0.5f) {
                    Log.w("FootAnchor", "❌ Ankle/toe distance mismatch: ${distanceBetween}m, skipping")
                    return@let
                }
            }
            
            // Clear old anchors to avoid memory leak
            if (shoePlaced) {
                anchors.forEach { it.detach() }
                anchors.clear()
                vioAnchors.clear()
            }
            
            if (chosen.arCoreHit != null) {
                anchors.add(chosen.arCoreHit.createAnchor())
            } else {
                vioAnchors.add(chosen.hitPoseMatrix)
            }
            
            // Calculate foot dimensions and orientation if we have both ankle and toe
            if (ankleHit != null && toeHit != null) {
                val anklePos = FloatArray(3)
                val toePos = FloatArray(3)
                
                // Extract translation from matrices
                anklePos[0] = ankleHit.hitPoseMatrix[12]
                anklePos[1] = ankleHit.hitPoseMatrix[13]
                anklePos[2] = ankleHit.hitPoseMatrix[14]
                
                toePos[0] = toeHit.hitPoseMatrix[12]
                toePos[1] = toeHit.hitPoseMatrix[13]
                toePos[2] = toeHit.hitPoseMatrix[14]
                
                // Calculate foot length in 3D (XZ plane)
                val dx = toePos[0] - anklePos[0]
                val dz = toePos[2] - anklePos[2]
                val dy = toePos[1] - anklePos[1]
                val footLengthMeters = kotlin.math.sqrt(dx * dx + dz * dz)
                
                // Check if foot length is realistic
                val isRealisticLength = footLengthMeters >= minFootLength && footLengthMeters <= maxFootLength
                
                // Calculate foot yaw (direction from ankle to toe)
                val footYaw = kotlin.math.atan2(dx, dz) // radians
                
                // Calculate scale based on foot length
                // If unrealistic, use 1.0x (assume reference size)
                val footScale = if (isRealisticLength) {
                    (footLengthMeters / referenceFootLength).coerceIn(0.7f, 1.3f)
                } else {
                    Log.w("FootAnchor", "Unrealistic foot length ${footLengthMeters}m, using 1.0x scale")
                    1.0f
                }
                
                // Build model matrix with correct transformation order:
                // 1. Start at toe anchor position
                // 2. Rotate to match foot orientation
                // 3. Translate in the rotated coordinate system (up + back toward heel)
                // 4. Scale uniformly
                
                val shoeMatrix = FloatArray(16)
                System.arraycopy(chosen.hitPoseMatrix, 0, shoeMatrix, 0, 16)
                
                // First, rotate to match foot orientation (around toe position)
                android.opengl.Matrix.rotateM(shoeMatrix, 0, Math.toDegrees(footYaw.toDouble()).toFloat(), 0f, 1f, 0f)
                
                // Now translate in the ROTATED coordinate system:
                // - Y-up to lift shoe above floor
                // - Z-back to move shoe origin from toe toward heel (40% of foot length)
                val backOffset = footLengthMeters * 0.4f
                android.opengl.Matrix.translateM(shoeMatrix, 0, 0f, soleOffsetMeters, -backOffset)
                
                // Finally, apply scale: base model scale * foot-size scale
                val finalScale = baseModelScale * footScale
                android.opengl.Matrix.scaleM(shoeMatrix, 0, finalScale, finalScale, finalScale)
                
                shoeModelMatrix = shoeMatrix
                shoePlaced = true
                lastPlacementTime = System.currentTimeMillis()
                
                Log.d(
                    "FootAnchor",
                    "✅ Anchor(${chosen.trackableType}, d=${"%.2f".format(chosen.distance)}) side=${fp.side} " +
                    "footLen=${"%.3f".format(footLengthMeters)}m (${if(isRealisticLength) "OK" else "BAD"}) " +
                    "footScale=${"%.2f".format(footScale)} baseScale=$baseModelScale finalScale=${"%.4f".format(finalScale)} " +
                    "yaw=${"%.1f".format(Math.toDegrees(footYaw.toDouble()))}° " +
                    "ankle=${anklePos.contentToString()} toe=${toePos.contentToString()}"
                )
            } else {
                // Fallback: simple placement if only one point available
                val anchorM = FloatArray(16)
                System.arraycopy(chosen.hitPoseMatrix, 0, anchorM, 0, 16)
                android.opengl.Matrix.translateM(anchorM, 0, 0f, soleOffsetMeters, 0f)
                // Apply base scale
                android.opengl.Matrix.scaleM(anchorM, 0, baseModelScale, baseModelScale, baseModelScale)
                shoeModelMatrix = anchorM
                shoePlaced = true
                lastPlacementTime = System.currentTimeMillis()
                
                Log.d(
                    "FootAnchor",
                    "✅ Anchor(${chosen.trackableType}, d=${"%.2f".format(chosen.distance)}) (simple placement, single point)"
                )
            }
        }
        
        if (primaryHit == null) {
            Log.d("FootAnchor", "ℹ️ No hit for ${fp.side} foot")
        }
    }
}
