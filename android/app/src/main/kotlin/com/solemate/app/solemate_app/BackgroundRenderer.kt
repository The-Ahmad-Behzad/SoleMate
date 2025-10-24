//package com.solemate.app.solemate_app
//
//import android.opengl.GLES11Ext
//import android.opengl.GLES20
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.nio.FloatBuffer
//
///**
// * Minimal background renderer that draws the ARCore camera texture to the screen.
// */
//class BackgroundRenderer {
//    private val quadCoords = floatArrayOf(
//        -1f, -1f,
//        1f, -1f,
//        -1f, 1f,
//        1f, 1f
//    )
//
//    private val texCoords = floatArrayOf(
//        0f, 1f,
//        1f, 1f,
//        0f, 0f,
//        1f, 0f
//    )
//
//    private val vertexBuffer: FloatBuffer =
//        ByteBuffer.allocateDirect(quadCoords.size * 4)
//            .order(ByteOrder.nativeOrder())
//            .asFloatBuffer()
//            .put(quadCoords)
//            .apply { position(0) }
//
//    private val texBuffer: FloatBuffer =
//        ByteBuffer.allocateDirect(texCoords.size * 4)
//            .order(ByteOrder.nativeOrder())
//            .asFloatBuffer()
//            .put(texCoords)
//            .apply { position(0) }
//
//    private var program = 0
//    private var textureId = -1
//
//    fun createOnGlThread() {
//        val vertexShaderCode = """
//            attribute vec4 a_Position;
//            attribute vec2 a_TexCoord;
//            varying vec2 v_TexCoord;
//            void main() {
//                gl_Position = a_Position;
//                v_TexCoord = a_TexCoord;
//            }
//        """.trimIndent()
//
//        val fragmentShaderCode = """
//            #extension GL_OES_EGL_image_external : require
//            precision mediump float;
//            varying vec2 v_TexCoord;
//            uniform samplerExternalOES sTexture;
//            void main() {
//                gl_FragColor = texture2D(sTexture, v_TexCoord);
//            }
//        """.trimIndent()
//
//        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
//        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
//
//        program = GLES20.glCreateProgram().also {
//            GLES20.glAttachShader(it, vertexShader)
//            GLES20.glAttachShader(it, fragmentShader)
//            GLES20.glLinkProgram(it)
//        }
//
//        val textures = IntArray(1)
//        GLES20.glGenTextures(1, textures, 0)
//        textureId = textures[0]
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
//        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
//    }
//
//    fun draw() {
//        GLES20.glUseProgram(program)
//        val posHandle = GLES20.glGetAttribLocation(program, "a_Position")
//        val texHandle = GLES20.glGetAttribLocation(program, "a_TexCoord")
//        val texUniform = GLES20.glGetUniformLocation(program, "sTexture")
//
//        GLES20.glEnableVertexAttribArray(posHandle)
//        GLES20.glEnableVertexAttribArray(texHandle)
//
//        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
//        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer)
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
//        GLES20.glUniform1i(texUniform, 0)
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
//
//        GLES20.glDisableVertexAttribArray(posHandle)
//        GLES20.glDisableVertexAttribArray(texHandle)
//    }
//
//    fun getTextureId(): Int = textureId
//
//    private fun loadShader(type: Int, shaderCode: String): Int {
//        return GLES20.glCreateShader(type).also { shader ->
//            GLES20.glShaderSource(shader, shaderCode)
//            GLES20.glCompileShader(shader)
//        }
//    }
//}









package com.solemate.app.solemate_app

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Background renderer that properly uses ARCore's texture transform matrix
 * to display the camera feed with correct orientation and aspect ratio.
 */
class BackgroundRenderer {
    private val quadCoords = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f, 1f,
        1f, 1f
    )

    private val texCoords = floatArrayOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(quadCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadCoords)
            .apply { position(0) }

    // Static UVs buffer
    private val texCoordsBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
            .apply { position(0) }

    // Buffer to store the transformed UVs (must be a direct buffer)
    private val transformedTexCoordsBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

    private var textureId = -1
    private var program = 0

    fun createOnGlThread() {
        val vertexShaderCode = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, v_TexCoord);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    /**
     * Draws the camera image for the given ARCore frame, applying the correct transform.
     */
    fun draw(frame: Frame) {
        // Reset buffer positions
        texCoordsBuffer.position(0)
        transformedTexCoordsBuffer.position(0)

        // ✅ Use direct buffers for ARCore transform
        frame.transformDisplayUvCoords(texCoordsBuffer, transformedTexCoordsBuffer)

        GLES20.glUseProgram(program)

        val posHandle = GLES20.glGetAttribLocation(program, "a_Position")
        val texHandle = GLES20.glGetAttribLocation(program, "a_TexCoord")
        val texUniform = GLES20.glGetUniformLocation(program, "sTexture")

        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glEnableVertexAttribArray(texHandle)

        GLES20.glVertexAttribPointer(posHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glVertexAttribPointer(texHandle, 2, GLES20.GL_FLOAT, false, 0, transformedTexCoordsBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(texUniform, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(texHandle)
    }

    fun getTextureId(): Int = textureId

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
