package com.example.myapplication;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Graph3DRenderer implements GLSurfaceView.Renderer {

    private static final int GRID_SIZE = 64;
    private static final float RANGE = 3.0f;
    private static final float AXIS_EXTRA = 0.8f;

    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;
    private int vertexCount;

    // Axis & grid
    private FloatBuffer axisVertexBuffer;
    private FloatBuffer axisColorBuffer;
    private int axisVertexCount;

    private int program;
    private int muMVPMatrixHandle;
    private int maPositionHandle;
    private int maColorHandle;

    private final float[] projMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];

    private volatile String functionExpr = "sin(x)*cos(y)";
    private volatile boolean curveMode = false;
    private volatile String curveX, curveY, curveZ;
    private volatile float tMin = 0, tMax = 6.283f, tStep = 0.02f;
    private volatile boolean needsUpdate = true;

    // Curve buffers
    private FloatBuffer curveVertexBuffer;
    private FloatBuffer curveColorBuffer;
    private int curveVertexCount;

    // Rotation
    private float rotX = -25f;
    private float rotY = 0f;

    public void setFunction(String expr) {
        functionExpr = expr;
        curveMode = false;
        needsUpdate = true;
    }

    public void setParametricFunction(String xExpr, String yExpr, String zExpr,
                                       float minT, float maxT, float stepT) {
        curveX = xExpr; curveY = yExpr; curveZ = zExpr;
        tMin = minT; tMax = maxT; tStep = stepT;
        curveMode = true;
        needsUpdate = true;
    }

    public void setRotation(float rx, float ry) {
        rotX = rx;
        rotY = ry;
    }

    public float getRotX() { return rotX; }
    public float getRotY() { return rotY; }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.12f, 0.12f, 0.14f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glLineWidth(3f);

        program = createProgram(vertexShaderSource, fragmentShaderSource);
        muMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        maPositionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        maColorHandle = GLES20.glGetAttribLocation(program, "aColor");

        generateMesh();
        generateCurve();
        generateAxesAndGrid();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.perspectiveM(projMatrix, 0, 45f, ratio, 1f, 30f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (needsUpdate) {
            generateMesh();
            generateCurve();
            generateAxesAndGrid();
            needsUpdate = false;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 8f, 0f, 0f, 0f, 0f, 1f, 0f);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.rotateM(modelMatrix, 0, rotX, 1f, 0f, 0f);
        Matrix.rotateM(modelMatrix, 0, rotY, 0f, 1f, 0f);

        float[] mvMatrix = new float[16];
        Matrix.multiplyMM(mvMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, mvMatrix, 0);

        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mvpMatrix, 0);

        // Draw axes & grid first
        drawBuffer(axisVertexBuffer, axisColorBuffer, axisVertexCount, GLES20.GL_LINES);

        // Draw surface or curve
        if (curveMode && curveVertexCount > 0) {
            drawBuffer(curveVertexBuffer, curveColorBuffer, curveVertexCount, GLES20.GL_LINE_STRIP);
        } else {
            drawBuffer(vertexBuffer, colorBuffer, vertexCount, GLES20.GL_TRIANGLE_STRIP);
        }
    }

    private void drawBuffer(FloatBuffer vBuf, FloatBuffer cBuf, int count, int mode) {
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        vBuf.position(0);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vBuf);

        GLES20.glEnableVertexAttribArray(maColorHandle);
        cBuf.position(0);
        GLES20.glVertexAttribPointer(maColorHandle, 4, GLES20.GL_FLOAT, false, 0, cBuf);

        if (mode == GLES20.GL_TRIANGLE_STRIP) {
            int stripSize = GRID_SIZE * 2;
            int numStrips = GRID_SIZE - 1;
            for (int i = 0; i < numStrips; i++) {
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, i * stripSize, stripSize);
            }
        } else {
            GLES20.glDrawArrays(mode, 0, count);
        }

        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maColorHandle);
    }

    // ─── Surface Mesh ──────────────────────────────────────────

    private void generateMesh() {
        Argument xArg = new Argument("x");
        Argument yArg = new Argument("y");
        Expression expr = new Expression(functionExpr, xArg, yArg);

        int N = GRID_SIZE;
        float step = 2 * RANGE / (N - 1);

        float[][] zValues = new float[N][N];
        float zMin = Float.MAX_VALUE, zMax = -Float.MAX_VALUE;

        for (int i = 0; i < N; i++) {
            double yv = -RANGE + i * step;
            yArg.setArgumentValue(yv);
            for (int j = 0; j < N; j++) {
                double xv = -RANGE + j * step;
                xArg.setArgumentValue(xv);
                double z = expr.calculate();
                if (Double.isNaN(z) || Double.isInfinite(z)) z = 0;
                zValues[i][j] = (float) z;
                zMin = Math.min(zMin, (float) z);
                zMax = Math.max(zMax, (float) z);
            }
        }

        float zRange = zMax - zMin;
        if (zRange < 1e-6f) zRange = 1f;

        vertexCount = (N - 1) * N * 2;
        float[] verts = new float[vertexCount * 3];
        float[] cols = new float[vertexCount * 4];

        int idx = 0;
        for (int i = 0; i < N - 1; i++) {
            for (int j = 0; j < N; j++) {
                for (int ro = 0; ro <= 1; ro++) {
                    float x = -RANGE + j * step;
                    float y = -RANGE + (i + ro) * step;
                    float zNorm = (zValues[i + ro][j] - zMin) / zRange * 2f * RANGE - RANGE;
                    verts[idx * 3] = x;
                    verts[idx * 3 + 1] = zNorm;
                    verts[idx * 3 + 2] = y;
                    float t = (zValues[i + ro][j] - zMin) / zRange;
                    jetColor(t, cols, idx * 4);
                    idx++;
                }
            }
        }

        vertexBuffer = makeBuffer(verts);
        colorBuffer = makeBuffer(cols);
    }

    // ─── Parametric Curve ──────────────────────────────────────

    private void generateCurve() {
        if (!curveMode || curveX == null) return;

        Argument t = new Argument("t");
        Expression ex = new Expression(curveX, t);
        Expression ey = new Expression(curveY, t);
        Expression ez = new Expression(curveZ, t);

        if (!ex.checkSyntax() || !ey.checkSyntax() || !ez.checkSyntax()) {
            curveVertexCount = 0;
            return;
        }

        // First pass: compute bounds for auto-scaling
        java.util.ArrayList<Float> pts = new java.util.ArrayList<>();
        float maxAbs = 0;

        for (double v = tMin; v <= tMax + tStep * 0.5; v += tStep) {
            t.setArgumentValue(v);
            double x = ex.calculate(); if (Double.isNaN(x) || Double.isInfinite(x)) continue;
            double y = ey.calculate(); if (Double.isNaN(y) || Double.isInfinite(y)) continue;
            double z = ez.calculate(); if (Double.isNaN(z) || Double.isInfinite(z)) continue;
            pts.add((float)x); pts.add((float)y); pts.add((float)z);
            maxAbs = Math.max(maxAbs, Math.max(Math.abs((float)x),
                     Math.max(Math.abs((float)y), Math.abs((float)z))));
        }

        if (pts.size() < 6) { curveVertexCount = 0; return; }
        if (maxAbs < 0.01f) maxAbs = 1f;

        float scale = RANGE * 0.95f / maxAbs;
        int n = pts.size() / 3;
        float[] verts = new float[n * 3];
        float[] cols = new float[n * 4];

        for (int i = 0; i < n; i++) {
            verts[i * 3] = pts.get(i * 3) * scale;
            verts[i * 3 + 1] = pts.get(i * 3 + 1) * scale;
            verts[i * 3 + 2] = pts.get(i * 3 + 2) * scale;
            float c = (float) i / n;
            rainbowColor(c, cols, i * 4);
        }

        curveVertexCount = n;
        curveVertexBuffer = makeBuffer(verts);
        curveColorBuffer = makeBuffer(cols);
    }

    private void rainbowColor(float t, float[] out, int off) {
        float r = (float)(Math.sin(t * 6.283 * 2) * 0.5 + 0.5);
        float g = (float)(Math.sin(t * 6.283 * 2 + 2.094) * 0.5 + 0.5);
        float b = (float)(Math.sin(t * 6.283 * 2 + 4.188) * 0.5 + 0.5);
        out[off] = clamp(r); out[off+1] = clamp(g);
        out[off+2] = clamp(b); out[off+3] = 1f;
    }

    // ─── Axes & Grid ───────────────────────────────────────────

    private void generateAxesAndGrid() {
        float ext = RANGE + AXIS_EXTRA;   // axis endpoint
        float gridExt = RANGE + 0.2f;     // grid extent

        // Build vertex list: each line = 2 vertices = 6 floats
        java.util.ArrayList<Float> vList = new java.util.ArrayList<>();
        java.util.ArrayList<Float> cList = new java.util.ArrayList<>();

        // -- Grid on the Y = -RANGE - 0.3 plane --
        float gridY = -RANGE - 0.3f;
        int gridLines = 7; // -3 to 3

        for (int i = -gridLines / 2; i <= gridLines / 2; i++) {
            float pos = i * 1.0f;
            if (pos < -gridExt || pos > gridExt) continue;
            // Parallel to X
            addLine(vList, -gridExt, gridY, pos, gridExt, gridY, pos);
            addColor(cList, 0.35f, 0.35f, 0.35f, 0.5f, 2);
            // Parallel to Z
            addLine(vList, pos, gridY, -gridExt, pos, gridY, gridExt);
            addColor(cList, 0.35f, 0.35f, 0.35f, 0.5f, 2);
        }

        // -- Axes (thicker, bright colors) --
        // X axis: red
        addLine(vList, -ext, 0, 0, ext, 0, 0);
        addColor(cList, 1f, 0.2f, 0.2f, 1f, 2);
        // Y axis: green (height axis)
        addLine(vList, 0, -ext, 0, 0, ext, 0);
        addColor(cList, 0.2f, 1f, 0.2f, 1f, 2);
        // Z axis: blue
        addLine(vList, 0, 0, -ext, 0, 0, ext);
        addColor(cList, 0.2f, 0.4f, 1f, 1f, 2);

        // -- Arrow tips (small triangles at positive ends) --
        // X arrow
        addArrow(vList, cList, ext, 0, 0, 1f, 0.2f, 0.2f);
        // Y arrow
        addArrow(vList, cList, 0, ext, 0, 0.2f, 1f, 0.2f);
        // Z arrow
        addArrow(vList, cList, 0, 0, ext, 0.2f, 0.4f, 1f);

        // -- Tick marks on each axis --
        for (int i = -3; i <= 3; i++) {
            if (i == 0) continue;
            float p = i * 1.0f;
            float tick = 0.12f;

            // X tick
            addLine(vList, p, -tick, 0, p, tick, 0);
            addColor(cList, 1f, 0.5f, 0.5f, 1f, 2);
            // Y tick
            addLine(vList, -tick, p, 0, tick, p, 0);
            addColor(cList, 0.5f, 1f, 0.5f, 1f, 2);
            // Z tick
            addLine(vList, 0, -tick, p, 0, tick, p);
            addColor(cList, 0.5f, 0.5f, 1f, 1f, 2);
        }

        // -- Axis letter labels (X, Y, Z drawn as lines) --
        float ls = 0.22f; // letter size
        // X label at X-axis end (red)
        addLine(vList, ext-ls, ls, 0, ext+ls, -ls, 0);
        addLine(vList, ext-ls, -ls, 0, ext+ls, ls, 0);
        addColor(cList, 1f, 0.5f, 0.5f, 1f, 4);
        // Y label at Y-axis end (green)
        addLine(vList, -ls, ext+ls, 0, 0, ext-ls*0.7f, 0);
        addLine(vList, ls, ext+ls, 0, 0, ext-ls*0.7f, 0);
        addLine(vList, 0, ext-ls*0.7f, 0, 0, ext-ls, 0);
        addColor(cList, 0.5f, 1f, 0.5f, 1f, 6);
        // Z label at Z-axis end (blue)
        addLine(vList, -ls, ls, ext+ls, ls, ls, ext+ls);
        addLine(vList, ls, ls, ext+ls, -ls, -ls, ext+ls);
        addLine(vList, -ls, -ls, ext+ls, ls, -ls, ext+ls);
        addColor(cList, 0.5f, 0.6f, 1f, 1f, 6);

        axisVertexCount = vList.size() / 3;
        float[] verts = new float[vList.size()];
        float[] cols = new float[cList.size()];
        for (int i = 0; i < vList.size(); i++) verts[i] = vList.get(i);
        for (int i = 0; i < cList.size(); i++) cols[i] = cList.get(i);

        axisVertexBuffer = makeBuffer(verts);
        axisColorBuffer = makeBuffer(cols);
    }

    private void addLine(java.util.ArrayList<Float> v, float x1, float y1, float z1,
                                                      float x2, float y2, float z2) {
        v.add(x1); v.add(y1); v.add(z1);
        v.add(x2); v.add(y2); v.add(z2);
    }

    private void addColor(java.util.ArrayList<Float> c, float r, float g, float b, float a, int count) {
        for (int i = 0; i < count; i++) {
            c.add(r); c.add(g); c.add(b); c.add(a);
        }
    }

    private void addArrow(java.util.ArrayList<Float> v, java.util.ArrayList<Float> c,
                          float x, float y, float z, float r, float g, float bl) {
        float s = 0.15f; // arrow half-size
        // Two lines forming a V
        addLine(v, x, y, z, x - s * 2, y + s, z);
        addLine(v, x, y, z, x - s * 2, y - s, z);
        addColor(c, r, g, bl, 1f, 4);
    }

    // ─── Helpers ───────────────────────────────────────────────

    private FloatBuffer makeBuffer(float[] data) {
        FloatBuffer buf = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(data).position(0);
        return buf;
    }

    private void jetColor(float t, float[] out, int offset) {
        float r, g, b;
        if (t < 0.125f) {
            r = 0; g = 0; b = 0.5f + t * 4f;
        } else if (t < 0.375f) {
            r = 0; g = (t - 0.125f) * 4f; b = 1;
        } else if (t < 0.5f) {
            r = 0; g = 1; b = 1 - (t - 0.375f) * 4f;
        } else if (t < 0.625f) {
            r = (t - 0.5f) * 4f; g = 1; b = 0;
        } else if (t < 0.875f) {
            r = 1; g = 1 - (t - 0.625f) * 4f; b = 0;
        } else {
            r = 1 - (t - 0.875f) * 4f; g = 0; b = 0;
        }
        out[offset] = clamp(r);
        out[offset + 1] = clamp(g);
        out[offset + 2] = clamp(b);
        out[offset + 3] = 1f;
    }

    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // ─── Shaders ───────────────────────────────────────────────

    private static final String vertexShaderSource =
            "uniform mat4 uMVPMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aColor;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "    gl_Position = uMVPMatrix * aPosition;\n" +
            "    vColor = aColor;\n" +
            "}";

    private static final String fragmentShaderSource =
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "void main() {\n" +
            "    gl_FragColor = vColor;\n" +
            "}";

    private int loadShader(int type, String source) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private int createProgram(String vertexSrc, String fragmentSrc) {
        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexSrc);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc);
        int prog = GLES20.glCreateProgram();
        GLES20.glAttachShader(prog, vs);
        GLES20.glAttachShader(prog, fs);
        GLES20.glLinkProgram(prog);
        return prog;
    }
}
