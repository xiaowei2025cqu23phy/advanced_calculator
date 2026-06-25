package com.example.myapplication.renderer

import android.opengl.GLES20

/**
 * OpenGL ES 2.0 shader compilation utilities.
 */
object ShaderHelper {

    const val VERTEX_SHADER = """
        uniform mat4 uMVPMatrix;
        attribute vec4 aPosition;
        attribute vec4 aColor;
        varying vec4 vColor;
        void main() {
            gl_Position = uMVPMatrix * aPosition;
            vColor = aColor;
        }
    """

    const val FRAGMENT_SHADER = """
        precision mediump float;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vColor;
        }
    """

    fun createProgram(): Int {
        val vs = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vs)
        GLES20.glAttachShader(program, fs)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }
}
