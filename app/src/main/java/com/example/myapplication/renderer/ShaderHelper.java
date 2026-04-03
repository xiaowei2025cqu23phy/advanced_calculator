package com.example.myapplication.renderer;

import android.opengl.GLES20;

/**
 * OpenGL ES 2.0 shader compilation utilities.
 */
public class ShaderHelper {

    public static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aColor;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "    vColor = aColor;\n" +
            "}";

    public static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "    gl_FragColor = vColor;\n" +
            "}";

    public static int createProgram() {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);
        GLES20.glLinkProgram(program);
        return program;
    }

    private static int loadShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
