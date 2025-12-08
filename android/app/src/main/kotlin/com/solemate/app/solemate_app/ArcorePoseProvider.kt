package com.solemate.app.solemate_app

import android.util.Log
import android.media.Image
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.NotYetAvailableException

/**
 * ARCore implementation of WorldPoseProvider
 * Wraps ARCore Session and Frame to provide pose tracking via the abstraction interface
 */
class ArcorePoseProvider(
    private val session: Session,
    private val rotationHelper: DisplayRotationHelper
) : WorldPoseProvider {

    private var currentFrame: Frame? = null
    private var isTracking = false

    override fun update(): Boolean {
        return try {
            rotationHelper.updateSessionIfNeeded(session)
            val frame = session.update()
            currentFrame = frame
            
            isTracking = frame.camera.trackingState == com.google.ar.core.TrackingState.TRACKING
            true
        } catch (e: CameraNotAvailableException) {
            Log.e("ArcorePoseProvider", "Camera not available: ${e.message}")
            isTracking = false
            false
        } catch (e: Exception) {
            Log.e("ArcorePoseProvider", "Update failed: ${e.message}")
            isTracking = false
            false
        }
    }

    override fun getViewMatrix(): FloatArray {
        val frame = currentFrame ?: return FloatArray(16).apply {
            android.opengl.Matrix.setIdentityM(this, 0)
        }
        
        val viewMatrix = FloatArray(16)
        frame.camera.getViewMatrix(viewMatrix, 0)
        return viewMatrix
    }

    override fun getProjectionMatrix(near: Float, far: Float): FloatArray {
        val frame = currentFrame ?: return FloatArray(16).apply {
            android.opengl.Matrix.setIdentityM(this, 0)
        }
        
        val projMatrix = FloatArray(16)
        frame.camera.getProjectionMatrix(projMatrix, 0, near, far)
        return projMatrix
    }

    override fun getCameraIntrinsics(): CameraIntrinsics? {
        val frame = currentFrame ?: return null
        
        return try {
            val intrinsics = frame.camera.imageIntrinsics
            val focalLength = intrinsics.getFocalLength()
            val principalPoint = intrinsics.getPrincipalPoint()
            val imageSize = intrinsics.imageDimensions
            
            CameraIntrinsics(
                fx = focalLength[0],
                fy = focalLength[1],
                cx = principalPoint[0],
                cy = principalPoint[1],
                width = imageSize[0],
                height = imageSize[1]
            )
        } catch (e: Exception) {
            Log.w("ArcorePoseProvider", "Failed to get camera intrinsics: ${e.message}")
            null
        }
    }

    override fun hitTest(viewX: Float, viewY: Float): List<HitResult> {
        val frame = currentFrame ?: return emptyList()
        
        val results = mutableListOf<HitResult>()
        
        try {
            val hits = frame.hitTest(viewX, viewY)
            
            // Sort hits by preference: Plane > Point > DepthPoint
            val sortedHits = hits.sortedWith(compareBy<com.google.ar.core.HitResult> { hit ->
                when (hit.trackable) {
                    is Plane -> 0
                    is Point -> 1
                    is DepthPoint -> 2
                    else -> 3
                }
            }.thenBy { it.distance })
            
            for (hit in sortedHits) {
                val trackableType = when (hit.trackable) {
                    is Plane -> TrackableType.PLANE
                    is Point -> TrackableType.POINT
                    is DepthPoint -> TrackableType.DEPTH_POINT
                    else -> continue
                }
                
                val arPose = hit.hitPose
                val translation = floatArrayOf(
                    arPose.tx(),
                    arPose.ty(),
                    arPose.tz()
                )
                
                // Extract quaternion from ARCore pose
                val quaternion = FloatArray(4)
                arPose.getRotationQuaternion(quaternion, 0)
                
                results.add(
                    HitResult(
                        hitPose = Pose(translation, quaternion),
                        distance = hit.distance,
                        trackableType = trackableType,
                        trackableId = hit.trackable?.hashCode()?.toLong()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("ArcorePoseProvider", "Hit test failed: ${e.message}")
        }
        
        return results
    }

    override fun getPlanes(): List<PlaneInfo> {
        val frame = currentFrame ?: return emptyList()
        
        val planes = mutableListOf<PlaneInfo>()
        
        try {
            val allPlanes = session.getAllTrackables(Plane::class.java)
            for (plane in allPlanes) {
                if (plane.trackingState == com.google.ar.core.TrackingState.TRACKING && plane.subsumedBy == null) {
                    val arPose = plane.centerPose
                    val translation = floatArrayOf(
                        arPose.tx(),
                        arPose.ty(),
                        arPose.tz()
                    )
                    val quaternion = FloatArray(4)
                    arPose.getRotationQuaternion(quaternion, 0)
                    
                    // Extract polygon if available
                    val polygon = try {
                        val polygonBuffer = plane.polygon
                        if (polygonBuffer != null && polygonBuffer.capacity() >= 6) {
                            val polygonList = mutableListOf<FloatArray>()
                            polygonBuffer.rewind()
                            while (polygonBuffer.hasRemaining()) {
                                if (polygonBuffer.remaining() >= 2) {
                                    val x = polygonBuffer.get()
                                    val z = polygonBuffer.get()
                                    polygonList.add(floatArrayOf(x, 0f, z))
                                } else {
                                    break
                                }
                            }
                            polygonList.ifEmpty { null }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                    
                    planes.add(
                        PlaneInfo(
                            centerPose = Pose(translation, quaternion),
                            extentX = plane.extentX,
                            extentZ = plane.extentZ,
                            polygon = polygon,
                            trackingState = when (plane.trackingState) {
                                com.google.ar.core.TrackingState.TRACKING -> TrackingState.TRACKING
                                com.google.ar.core.TrackingState.PAUSED -> TrackingState.PAUSED
                                else -> TrackingState.STOPPED
                            }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ArcorePoseProvider", "Failed to get planes: ${e.message}")
        }
        
        return planes
    }

    override fun isTracking(): Boolean {
        return isTracking && currentFrame?.camera?.trackingState == com.google.ar.core.TrackingState.TRACKING
    }

    override fun getLightEstimate(): LightEstimate? {
        val frame = currentFrame ?: return null
        
        return try {
            val lightEstimate = frame.lightEstimate
            if (lightEstimate.state == com.google.ar.core.LightEstimate.State.VALID) {
                val colorCorrection = FloatArray(4)
                lightEstimate.getColorCorrection(colorCorrection, 0)
                
                LightEstimate(
                    pixelIntensity = lightEstimate.pixelIntensity,
                    colorCorrection = colorCorrection
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("ArcorePoseProvider", "Failed to get light estimate: ${e.message}")
            null
        }
    }

    override fun transformCoordinates2d(
        srcType: CoordinateSpace,
        srcCoords: FloatArray,
        dstType: CoordinateSpace
    ): FloatArray? {
        val frame = currentFrame ?: return null
        
        return try {
            val srcArCore = when (srcType) {
                CoordinateSpace.IMAGE_PIXELS -> Coordinates2d.IMAGE_PIXELS
                CoordinateSpace.IMAGE_NORMALIZED -> Coordinates2d.IMAGE_NORMALIZED
                CoordinateSpace.VIEW -> Coordinates2d.VIEW
                CoordinateSpace.WORLD -> return null // Not supported
            }
            
            val dstArCore = when (dstType) {
                CoordinateSpace.IMAGE_PIXELS -> Coordinates2d.IMAGE_PIXELS
                CoordinateSpace.IMAGE_NORMALIZED -> Coordinates2d.IMAGE_NORMALIZED
                CoordinateSpace.VIEW -> Coordinates2d.VIEW
                CoordinateSpace.WORLD -> return null // Not supported
            }
            
            val inBuf = java.nio.ByteBuffer.allocateDirect(srcCoords.size * 4)
                .order(java.nio.ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(srcCoords)
                    position(0)
                }
            
            val outBuf = java.nio.ByteBuffer.allocateDirect(srcCoords.size * 4)
                .order(java.nio.ByteOrder.nativeOrder())
                .asFloatBuffer()
            
            frame.transformCoordinates2d(srcArCore, inBuf, dstArCore, outBuf)
            
            outBuf.position(0)
            val result = FloatArray(srcCoords.size)
            outBuf.get(result)
            result
        } catch (e: Exception) {
            Log.e("ArcorePoseProvider", "Transform coordinates failed: ${e.message}")
            null
        }
    }

    override fun acquireCameraImage(): CameraImage? {
        val frame = currentFrame ?: return null
        
        return try {
            val arImage = frame.acquireCameraImage()
            ArcoreCameraImage(arImage)
        } catch (e: NotYetAvailableException) {
            null
        } catch (e: Exception) {
            Log.e("ArcorePoseProvider", "Failed to acquire camera image: ${e.message}")
            null
        }
    }

    override fun getCameraTextureId(): Int {
        // ARCore manages texture internally, return -1 to indicate external management needed
        return -1
    }

    override fun setCameraTextureName(textureId: Int) {
        try {
            session.setCameraTextureName(textureId)
        } catch (e: Exception) {
            Log.e("ArcorePoseProvider", "Failed to set camera texture: ${e.message}")
        }
    }

    /**
     * Get the current ARCore Frame (for compatibility with BackgroundRenderer, etc.)
     * Returns null if no frame is available
     */
    fun getCurrentFrame(): Frame? = currentFrame

    /**
     * Get the ARCore Session (for compatibility with existing code)
     */
    fun getSession(): Session = session

    override fun release() {
        // ARCore session is managed by ARActivity, so we don't close it here
        currentFrame = null
        isTracking = false
    }
}

/**
 * Wrapper for ARCore Image to implement CameraImage interface
 */
internal class ArcoreCameraImage(private val arImage: Image) : CameraImage {
    override val width: Int get() = arImage.width
    override val height: Int get() = arImage.height
    override val format: Int get() = arImage.format
    
    override fun close() {
        arImage.close()
    }
    
    override fun getPlanes(): Array<ImagePlane> {
        return arImage.planes.map { plane ->
            ArcoreImagePlane(plane)
        }.toTypedArray()
    }
    
    /**
     * Get the underlying android.media.Image for compatibility with existing code
     */
    fun getArImage(): Image = arImage
}

/**
 * Wrapper for ARCore Image.Plane to implement ImagePlane interface
 */
private class ArcoreImagePlane(private val arPlane: Image.Plane) : ImagePlane {
    override val buffer: java.nio.ByteBuffer get() = arPlane.buffer
    override val pixelStride: Int get() = arPlane.pixelStride
    override val rowStride: Int get() = arPlane.rowStride
}

