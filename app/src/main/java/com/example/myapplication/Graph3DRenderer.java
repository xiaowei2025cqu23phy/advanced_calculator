package com.example.myapplication;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.example.myapplication.renderer.AxesGridGenerator;
import com.example.myapplication.renderer.ShaderHelper;
import com.example.myapplication.renderer.SurfaceMeshGenerator;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Orchestrator with async mesh/curve generation.
 * Mesh computation (mXparser) runs on background executor;
 * only buffer upload + draw happens on the GL thread.
 */
public class Graph3DRenderer implements GLSurfaceView.Renderer {

    private final SurfaceMeshGenerator meshGen = new SurfaceMeshGenerator();
    private final AxesGridGenerator axesGen = new AxesGridGenerator();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mesh-gen");
        t.setDaemon(true);
        return t;
    });

    private int program, muMVPMatrixHandle, maPositionHandle, maColorHandle;
    private final float[] projMatrix = new float[16];

    // Surface data (read by GL thread, written by executor)
    private volatile FloatBuffer surfVerts, surfColors;
    private volatile int surfCount;
    private volatile SurfaceMeshGenerator.MeshResult pendingMesh;

    // Curve data
    private volatile FloatBuffer curveVerts, curveColors;
    private volatile int curveCount;
    private volatile FloatBuffer pendingCurveVerts, pendingCurveColors;
    private volatile int pendingCurveCount;

    // Axes data (always generated on GL thread — fast)
    private FloatBuffer axesVerts, axesColors;
    private int axesCount;

    private volatile String functionExpr = "sin(x)*cos(y)";
    private volatile boolean curveMode = false;
    private volatile String cx, cy, cz;
    private volatile float tMin, tMax, tStep;

    /** Incremented on every setFunction/setParametricFunction call.
     *  Background tasks capture this at start and only apply result if still current. */
    private volatile int generation = 0;
    private final Object computeLock = new Object();

    private float rotX = -25f, rotY = 0f;

    public void setFunction(String expr) {
        functionExpr = expr;
        curveMode = false;
        generation++;
        submitMesh(expr, generation);
    }

    public void setParametricFunction(String x, String y, String z,
                                       float mn, float mx, float st) {
        cx = x; cy = y; cz = z; tMin = mn; tMax = mx; tStep = st;
        curveMode = true;
        int gen = ++generation;
        submitCurve(x, y, z, mn, mx, st, gen);
    }

    public void setRotation(float rx, float ry) { rotX = rx; rotY = ry; }
    public float getRotX() { return rotX; }
    public float getRotY() { return rotY; }

    // ── Background computation ──

    private void submitMesh(String expr, int gen) {
        executor.submit(() -> {
            SurfaceMeshGenerator.MeshResult r = meshGen.generate(expr);
            synchronized (computeLock) {
                if (gen == generation) pendingMesh = r;
            }
        });
    }

    private void submitCurve(String x, String y, String z,
                             float mn, float mx, float st, int gen) {
        executor.submit(() -> {
            Argument t = new Argument("t");
            Expression ex = new Expression(x, t);
            Expression ey = new Expression(y, t);
            Expression ez = new Expression(z, t);
            if (!ex.checkSyntax() || !ey.checkSyntax() || !ez.checkSyntax()) return;

            ArrayList<Float> pts = new ArrayList<>();
            float maxAbs = 0;
            for (double v = mn; v <= mx + st * 0.5; v += st) {
                t.setArgumentValue(v);
                double dx = ex.calculate(); if (Double.isNaN(dx) || Double.isInfinite(dx)) continue;
                double dy = ey.calculate(); if (Double.isNaN(dy) || Double.isInfinite(dy)) continue;
                double dz = ez.calculate(); if (Double.isNaN(dz) || Double.isInfinite(dz)) continue;
                pts.add((float)dx); pts.add((float)dy); pts.add((float)dz);
                maxAbs = Math.max(maxAbs, Math.max(Math.abs((float)dx),
                         Math.max(Math.abs((float)dy), Math.abs((float)dz))));
            }
            if (pts.size() < 6) return;
            if (maxAbs < 0.01f) maxAbs = 1f;

            float s = SurfaceMeshGenerator.RANGE * 0.95f / maxAbs;
            int n = pts.size() / 3;
            float[] verts = new float[n * 3];
            float[] cols  = new float[n * 4];
            for (int i = 0; i < n; i++) {
                verts[i*3]   = pts.get(i*3)     * s;
                verts[i*3+1] = pts.get(i*3+1)   * s;
                verts[i*3+2] = pts.get(i*3+2)   * s;
                rainbowColor((float)i / n, cols, i*4);
            }
            synchronized (computeLock) {
                if (gen == generation) {
                    pendingCurveVerts = buf(verts);
                    pendingCurveColors = buf(cols);
                    pendingCurveCount = n;
                }
            }
        });
    }

    // ── GL lifecycle ──

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.12f, 0.12f, 0.14f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glLineWidth(3f);

        program = ShaderHelper.createProgram();
        muMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        maPositionHandle  = GLES20.glGetAttribLocation(program, "aPosition");
        maColorHandle     = GLES20.glGetAttribLocation(program, "aColor");

        // Initial submission
        generation++;
        submitMesh(functionExpr, generation);
        updateAxes();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        GLES20.glViewport(0, 0, w, h);
        Matrix.perspectiveM(projMatrix, 0, 45f, (float) w / h, 1f, 30f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Swap in any pending results (lock-free read for volatile refs)
        SurfaceMeshGenerator.MeshResult pm;
        synchronized (computeLock) {
            pm = pendingMesh;
            if (pendingMesh != null) {
                surfVerts = pendingMesh.vertexBuffer;
                surfColors = pendingMesh.colorBuffer;
                surfCount = pendingMesh.vertexCount;
                pendingMesh = null;
            }
            if (pendingCurveCount > 0) {
                curveVerts = pendingCurveVerts;
                curveColors = pendingCurveColors;
                curveCount = pendingCurveCount;
                pendingCurveCount = 0;
            }
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        float[] mv = new float[16];
        float[] mvp = new float[16];
        Matrix.setLookAtM(mv, 0, 0, 0, 8f, 0f, 0f, 0f, 0f, 1f, 0f);
        Matrix.rotateM(mv, 0, rotX, 1f, 0f, 0f);
        Matrix.rotateM(mv, 0, rotY, 0f, 1f, 0f);
        Matrix.multiplyMM(mvp, 0, projMatrix, 0, mv, 0);

        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mvp, 0);

        draw(axesVerts, axesColors, axesCount, GLES20.GL_LINES, false);
        if (curveMode && curveCount > 0)
            draw(curveVerts, curveColors, curveCount, GLES20.GL_LINE_STRIP, false);
        else
            draw(surfVerts, surfColors, surfCount, GLES20.GL_TRIANGLE_STRIP, true);
    }

    // ── Helpers ──

    private void updateAxes() {
        AxesGridGenerator.AxesResult ar = axesGen.generate();
        axesVerts = ar.vertexBuffer; axesColors = ar.colorBuffer; axesCount = ar.vertexCount;
    }

    private void draw(FloatBuffer vb, FloatBuffer cb, int count, int mode, boolean strip) {
        if (vb == null || cb == null || count == 0) return;
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        vb.position(0); GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vb);
        GLES20.glEnableVertexAttribArray(maColorHandle);
        cb.position(0); GLES20.glVertexAttribPointer(maColorHandle, 4, GLES20.GL_FLOAT, false, 0, cb);

        if (strip) {
            int ss = SurfaceMeshGenerator.GRID_SIZE * 2;
            for (int i = 0; i < SurfaceMeshGenerator.GRID_SIZE - 1; i++)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, i * ss, ss);
        } else {
            GLES20.glDrawArrays(mode, 0, count);
        }
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maColorHandle);
    }

    private static void rainbowColor(float t, float[] out, int off) {
        out[off]   = clamp((float)(Math.sin(t * 12.566) * 0.5 + 0.5));
        out[off+1] = clamp((float)(Math.sin(t * 12.566 + 2.094) * 0.5 + 0.5));
        out[off+2] = clamp((float)(Math.sin(t * 12.566 + 4.188) * 0.5 + 0.5));
        out[off+3] = 1f;
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static FloatBuffer buf(float[] d) {
        FloatBuffer fb = ByteBuffer.allocateDirect(d.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(d); fb.position(0); return fb;
    }
}
