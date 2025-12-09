package com.solemate.app.solemate_app

import android.media.Image
import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.Texture
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Bridges ARCore depth images to Filament textures for occlusion rendering.
 * 
 * ARCore provides 16-bit depth images where each pixel value represents
 * the distance in millimeters from the camera plane to that point in the scene.
 * 
 * This handler:
 * 1. Acquires depth images from ARCore frames
 * 2. Creates/updates a Filament texture with the depth data
 * 3. Computes UV transform matrix for proper coordinate alignment
 */
class DepthTextureHandler(private val engine: Engine) {
    
    companion object {
        private const val TAG = "DepthTextureHandler"
        
        // Depth texture update throttling
        private const val MIN_UPDATE_INTERVAL_MS = 33L  // ~30 FPS for depth updates
    }
    
    // Filament depth texture
    private var depthTexture: Texture? = null
    private var textureWidth = 0
    private var textureHeight = 0
    
    // Depth UV transform matrix (3x3 stored as 9 floats in column-major order)
    private val depthUvTransform = FloatArray(9) { if (it == 0 || it == 4 || it == 8) 1f else 0f }
    
    // Buffers for coordinate transformation
    private val ndcQuadCoords = floatArrayOf(
        -1f, -1f,  // bottom-left
        +1f, -1f,  // bottom-right
        -1f, +1f,  // top-left
        +1f, +1f   // top-right
    )
    private val ndcBuffer: FloatBuffer = ByteBuffer.allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply { put(ndcQuadCoords); rewind() }
    
    private val textureUvBuffer: FloatBuffer = ByteBuffer.allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    
    // State tracking
    private var lastUpdateTimeMs = 0L
    private var isDepthAvailable = false
    
    // Reusable byte buffer for texture upload
    private var uploadBuffer: ByteBuffer? = null
    
    /**
     * Update the depth texture from the current ARCore frame.
     * Should be called once per frame from the render thread.
     * 
     * @param frame The current ARCore frame
     * @return true if depth texture was updated successfully
     */
    fun updateDepthTexture(frame: Frame): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Throttle updates for performance
        if (currentTime - lastUpdateTimeMs < MIN_UPDATE_INTERVAL_MS) {
            return isDepthAvailable
        }
        
        var depthImage: Image? = null
        try {
            // Acquire 16-bit depth image from ARCore
            depthImage = frame.acquireDepthImage16Bits()
            
            val width = depthImage.width
            val height = depthImage.height
            
            // Create or recreate texture if dimensions changed
            if (depthTexture == null || textureWidth != width || textureHeight != height) {
                createDepthTexture(width, height)
            }
            
            // Upload depth data to Filament texture
            uploadDepthData(depthImage)
            
            // Update UV transform matrix
            updateDepthUvTransform(frame)
            
            isDepthAvailable = true
            lastUpdateTimeMs = currentTime
            
            return true
            
        } catch (e: NotYetAvailableException) {
            // Depth data not available yet - normal during initialization
            isDepthAvailable = false
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update depth texture: ${e.message}")
            isDepthAvailable = false
            return false
        } finally {
            depthImage?.close()
        }
    }
    
    /**
     * Create a new Filament texture for depth data.
     * 
     * Depth is stored as 16-bit values packed into RG8 format:
     * - R channel: low 8 bits of depth (mm)
     * - G channel: high 8 bits of depth (mm)
     */
    private fun createDepthTexture(width: Int, height: Int) {
        // Destroy old texture if exists
        depthTexture?.let { 
            engine.destroyTexture(it)
        }
        
        // Create new texture with RG8 format for 16-bit depth
        depthTexture = Texture.Builder()
            .width(width)
            .height(height)
            .levels(1)
            .format(Texture.InternalFormat.RG8)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .build(engine)
        
        textureWidth = width
        textureHeight = height
        
        // Allocate upload buffer
        uploadBuffer = ByteBuffer.allocateDirect(width * height * 2)
            .order(ByteOrder.nativeOrder())
        
        Log.i(TAG, "Created depth texture: ${width}x${height}")
    }
    
    /**
     * Upload depth image data to the Filament texture.
     */
    private fun uploadDepthData(depthImage: Image) {
        val texture = depthTexture ?: return
        val buffer = uploadBuffer ?: return
        
        // Get the depth plane - ARCore provides a single plane with 16-bit depth
        val plane = depthImage.planes[0]
        val depthBuffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        
        buffer.rewind()
        
        // Copy depth data to upload buffer
        // ARCore depth is already in the format we need (16-bit per pixel)
        if (rowStride == textureWidth * 2 && pixelStride == 2) {
            // Fast path: direct copy when no padding
            depthBuffer.rewind()
            buffer.put(depthBuffer)
        } else {
            // Slow path: handle row padding
            for (y in 0 until textureHeight) {
                depthBuffer.position(y * rowStride)
                for (x in 0 until textureWidth) {
                    buffer.put(depthBuffer.get())
                    buffer.put(depthBuffer.get())
                }
            }
        }
        buffer.rewind()
        
        // Create pixel buffer descriptor for Filament
        val pbd = Texture.PixelBufferDescriptor(
            buffer,
            Texture.Format.RG,
            Texture.Type.UBYTE
        )
        
        // Upload to texture
        texture.setImage(engine, 0, pbd)
    }
    
    /**
     * Compute the UV transform matrix to convert from NDC to depth texture coordinates.
     * 
     * This accounts for any rotation/scaling needed to align the depth image
     * with the rendered scene.
     */
    private fun updateDepthUvTransform(frame: Frame) {
        try {
            // Transform NDC coordinates to texture UV coordinates
            ndcBuffer.rewind()
            textureUvBuffer.rewind()
            
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                ndcBuffer,
                Coordinates2d.TEXTURE_NORMALIZED,
                textureUvBuffer
            )
            
            textureUvBuffer.rewind()
            
            // Extract the 4 transformed UV coordinates
            val u0 = textureUvBuffer.get()  // bottom-left u
            val v0 = textureUvBuffer.get()  // bottom-left v
            val u1 = textureUvBuffer.get()  // bottom-right u
            val v1 = textureUvBuffer.get()  // bottom-right v
            val u2 = textureUvBuffer.get()  // top-left u
            val v2 = textureUvBuffer.get()  // top-left v
            // val u3 = textureUvBuffer.get()  // top-right u (not needed)
            // val v3 = textureUvBuffer.get()  // top-right v (not needed)
            
            // Build 3x3 transform matrix from NDC to UV
            // Using the relationship: UV = M * NDC
            // where NDC is [-1,1] range and UV is [0,1] range
            
            // For a proper affine transform, we compute the matrix coefficients
            // from the corner mappings
            
            // UV = a * NDC_x + b * NDC_y + c
            // For u: u0 = a*(-1) + b*(-1) + c  => center is at (0,0)
            // Compute differences to get scale/rotation
            val du_dx = (u1 - u0) / 2f  // du/d(ndc_x)
            val du_dy = (u2 - u0) / 2f  // du/d(ndc_y)
            val dv_dx = (v1 - v0) / 2f  // dv/d(ndc_x)
            val dv_dy = (v2 - v0) / 2f  // dv/d(ndc_y)
            
            // Center UV (at NDC origin)
            val uc = (u0 + u1 + u2) / 3f + du_dx + du_dy
            val vc = (v0 + v1 + v2) / 3f + dv_dx + dv_dy
            
            // Build column-major 3x3 matrix:
            // | du_dx  du_dy  tx |
            // | dv_dx  dv_dy  ty |
            // |   0      0     1 |
            depthUvTransform[0] = du_dx
            depthUvTransform[1] = dv_dx
            depthUvTransform[2] = 0f
            depthUvTransform[3] = du_dy
            depthUvTransform[4] = dv_dy
            depthUvTransform[5] = 0f
            depthUvTransform[6] = uc
            depthUvTransform[7] = vc
            depthUvTransform[8] = 1f
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to compute depth UV transform: ${e.message}")
            // Reset to identity on error
            for (i in 0..8) {
                depthUvTransform[i] = if (i == 0 || i == 4 || i == 8) 1f else 0f
            }
        }
    }
    
    /**
     * Get the depth texture for use in materials.
     * May return null if depth is not yet available.
     */
    fun getDepthTexture(): Texture? = depthTexture
    
    /**
     * Get the UV transform matrix (3x3, column-major) for converting
     * screen coordinates to depth texture coordinates.
     */
    fun getDepthUvTransform(): FloatArray = depthUvTransform.copyOf()
    
    /**
     * Check if depth data is available.
     */
    fun isDepthAvailable(): Boolean = isDepthAvailable
    
    /**
     * Get the dimensions of the depth texture.
     */
    fun getDepthDimensions(): Pair<Int, Int> = Pair(textureWidth, textureHeight)
    
    /**
     * Release all resources.
     */
    fun release() {
        depthTexture?.let {
            try {
                engine.destroyTexture(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying depth texture: ${e.message}")
            }
        }
        depthTexture = null
        uploadBuffer = null
        isDepthAvailable = false
        Log.i(TAG, "DepthTextureHandler released")
    }
}
