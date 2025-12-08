package com.solemate.app.solemate_app

import android.media.Image

/**
 * Wrapper for Camera2 Image to implement CameraImage interface
 */
internal class Camera2Image(private val camera2Image: Image) : CameraImage {
    override val width: Int get() = camera2Image.width
    override val height: Int get() = camera2Image.height
    override val format: Int get() = camera2Image.format
    
    override fun close() {
        camera2Image.close()
    }
    
    override fun getPlanes(): Array<ImagePlane> {
        return camera2Image.planes.map { plane ->
            Camera2ImagePlane(plane)
        }.toTypedArray()
    }
    
    /**
     * Get the underlying android.media.Image for compatibility with existing code
     */
    fun getCamera2Image(): Image = camera2Image
}

/**
 * Wrapper for Camera2 Image.Plane to implement ImagePlane interface
 */
private class Camera2ImagePlane(private val camera2Plane: Image.Plane) : ImagePlane {
    override val buffer: java.nio.ByteBuffer get() = camera2Plane.buffer
    override val pixelStride: Int get() = camera2Plane.pixelStride
    override val rowStride: Int get() = camera2Plane.rowStride
}






