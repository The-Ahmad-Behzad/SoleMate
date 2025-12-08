package com.solemate.app.solemate_app

import android.opengl.GLES20
import com.google.ar.core.Plane
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renders detected planes as semi-transparent colored surfaces.
 * Uses proper alpha blending and avoids z-fighting.
 */
class PlaneRenderer {

    private val vertexShaderCode = """
        attribute vec4 a_Position;
        uniform mat4 u_ModelViewProjection;
        void main() {
            gl_Position = u_ModelViewProjection * a_Position;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        uniform vec4 u_Color;
        void main() {
            gl_FragColor = vec4(u_Color.rgb, u_Color.a);
        }
    """.trimIndent()

    private var program = 0
    private val planeColor = floatArrayOf(0f, 1f, 0f, 0.25f) // ✅ Soft translucent green

    fun createOnGlThread() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    fun drawPlane(plane: Plane, viewProjMatrix: FloatArray) {
        val polygon = plane.polygon ?: return
        if (polygon.capacity() < 6) return // skip invalid plane polygons

        val polygonBuffer: FloatBuffer = ByteBuffer.allocateDirect(polygon.capacity() * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(polygon)
            .apply { position(0) }

        val modelMatrix = FloatArray(16)
        plane.centerPose.toMatrix(modelMatrix, 0)

        val mvpMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)

        drawPlaneInternal(polygonBuffer, mvpMatrix, polygon.capacity() / 2)
    }

    /**
     * Draws a plane from PlaneInfo (abstraction layer compatible).
     * Extracts polygon from PlaneInfo if available, otherwise creates a simple rectangle.
     */
    fun drawPlane(planeInfo: PlaneInfo, viewProjMatrix: FloatArray) {
        val modelMatrix = planeInfo.centerPose.toMatrix()
        val mvpMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)

        val polygonBuffer: FloatBuffer = if (planeInfo.polygon != null && planeInfo.polygon.isNotEmpty()) {
            // Use provided polygon
            val vertices = planeInfo.polygon.flatMap { it.toList() }.toFloatArray()
            ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertices)
                .apply { position(0) }
        } else {
            // Create a simple rectangle from extent
            val halfX = planeInfo.extentX / 2f
            val halfZ = planeInfo.extentZ / 2f
            val rectCoords = floatArrayOf(
                -halfX, 0f, -halfZ,
                halfX, 0f, -halfZ,
                halfX, 0f, halfZ,
                -halfX, 0f, halfZ
            )
            ByteBuffer.allocateDirect(rectCoords.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(rectCoords)
                .apply { position(0) }
        }

        val vertexCount = if (planeInfo.polygon != null && planeInfo.polygon.isNotEmpty()) {
            planeInfo.polygon.size
        } else {
            4 // Rectangle
        }

        drawPlaneInternal(polygonBuffer, mvpMatrix, vertexCount)
    }

    /**
     * Internal method to draw plane geometry with common rendering logic.
     */
    private fun drawPlaneInternal(polygonBuffer: FloatBuffer, mvpMatrix: FloatArray, vertexCount: Int) {
        if (vertexCount < 3) return

        GLES20.glUseProgram(program)

        val posHandle = GLES20.glGetAttribLocation(program, "a_Position")
        val colorHandle = GLES20.glGetUniformLocation(program, "u_Color")
        val mvpHandle = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")

        // ✅ Enable alpha blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // ✅ Disable depth writing so the plane doesn't overwrite background
        GLES20.glDepthMask(false)

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 0, polygonBuffer)

        GLES20.glUniform4fv(colorHandle, 1, planeColor, 0)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)

        // ✅ Restore depth mask and blending
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDisableVertexAttribArray(posHandle)
    }

    private fun loadShader(type: Int, code: String): Int =
        GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, code)
            GLES20.glCompileShader(it)
        }
    fun drawAnchorMarker(modelMatrix: FloatArray, viewProjMatrix: FloatArray) {
        val mvpMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)

        GLES20.glUseProgram(program)
        val posHandle = GLES20.glGetAttribLocation(program, "a_Position")
        val colorHandle = GLES20.glGetUniformLocation(program, "u_Color")
        val mvpHandle = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")

        val markerCoords = floatArrayOf(
            0f, 0f,
            0.05f, 0f,
            0f, 0.05f
        )
        val markerBuffer = ByteBuffer.allocateDirect(markerCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(markerCoords)
            .apply { position(0) }

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, markerBuffer)
        GLES20.glUniform4f(colorHandle, 1f, 0f, 0f, 0.8f) // red marker
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        GLES20.glDisableVertexAttribArray(posHandle)
    }
}
