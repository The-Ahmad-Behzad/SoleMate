package com.solemate.app.solemate_app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Convert YUV_420_888 (from camera Image) to NV21 byte[] and then to JPEG -> Bitmap.
 * Works reliably and does not require RenderScript.
 */
object YuvConverter {

    fun imageToBitmap(image: Image): Bitmap {
        val nv21 = yuv420ThreePlanesToNV21(image.planes, image.width, image.height)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        // quality 80-95 is fine; smaller means faster but lower quality for ML
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
        val bytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun yuv420ThreePlanesToNV21(planes: Array<Image.Plane>, width: Int, height: Int): ByteArray {
        val ySize = width * height
        val uvSize = width * height / 4

        val nv21 = ByteArray(ySize + 2 * uvSize)

        // Copy Y
        val yBuffer = planes[0].buffer
        var offset = 0
        if (planes[0].rowStride == width) {
            // plane has no padding, direct copy
            yBuffer.get(nv21, 0, ySize)
            offset += ySize
        } else {
            // copy row by row
            var row = 0
            while (row < height) {
                yBuffer.position(row * planes[0].rowStride)
                yBuffer.get(nv21, offset, width)
                offset += width
                row++
            }
        }

        // U/V plane processing (interleaving VU for NV21)
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val uRowStride = planes[1].rowStride
        val vRowStride = planes[2].rowStride
        val uPixelStride = planes[1].pixelStride
        val vPixelStride = planes[2].pixelStride

        // iterate over UV rows
        var row = 0
        val chromaHeight = height / 2
        var nv21Index = ySize
        while (row < chromaHeight) {
            var col = 0
            while (col < width / 2) {
                val uIndex = row * uRowStride + col * uPixelStride
                val vIndex = row * vRowStride + col * vPixelStride

                val u = uBuffer.get(uIndex)
                val v = vBuffer.get(vIndex)

                // NV21 expects V then U
                nv21[nv21Index++] = v
                nv21[nv21Index++] = u

                col++
            }
            row++
        }

        return nv21
    }
}
