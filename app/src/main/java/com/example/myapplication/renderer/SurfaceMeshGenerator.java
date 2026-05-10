package com.example.myapplication.renderer;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Generates a 3D surface mesh for z = f(x, y) over a grid.
 * Output: vertex buffer (interleaved with colors via jet colormap).
 */
public class SurfaceMeshGenerator {

    public static final int GRID_SIZE = 64;
    public static final float RANGE = 3.0f;

    public static class MeshResult {
        public final FloatBuffer vertexBuffer;
        public final FloatBuffer colorBuffer;
        public final int vertexCount;

        MeshResult(FloatBuffer vb, FloatBuffer cb, int vc) {
            vertexBuffer = vb; colorBuffer = cb; vertexCount = vc;
        }
    }

    public MeshResult generate(String functionExpr) {
        Argument xArg = new Argument("x");
        Argument yArg = new Argument("y");
        Expression expr = new Expression(functionExpr, xArg, yArg);

        int N = GRID_SIZE;
        float step = 2 * RANGE / (N - 1);

        float[][] zValues = new float[N][N];
        float zMin = Float.MAX_VALUE, zMax = -Float.MAX_VALUE;

        for (int i = 0; i < N; i++) {
            yArg.setArgumentValue(-RANGE + i * step);
            for (int j = 0; j < N; j++) {
                xArg.setArgumentValue(-RANGE + j * step);
                double z = expr.calculate();
                if (Double.isNaN(z) || Double.isInfinite(z)) z = 0;
                zValues[i][j] = (float) z;
                zMin = Math.min(zMin, (float) z);
                zMax = Math.max(zMax, (float) z);
            }
        }

        float zRange = zMax - zMin;
        if (zRange < 1e-6f) zRange = 1f;

        int vc = (N - 1) * N * 2;
        float[] verts = new float[vc * 3];
        float[] cols = new float[vc * 4];
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
                    jetColor((zValues[i + ro][j] - zMin) / zRange, cols, idx * 4);
                    idx++;
                }
            }
        }

        return new MeshResult(makeBuffer(verts), makeBuffer(cols), vc);
    }

    private static FloatBuffer makeBuffer(float[] data) {
        FloatBuffer buf = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(data).position(0);
        return buf;
    }

    private static void jetColor(float t, float[] out, int off) {
        float r, g, b;
        if (t < 0.125f) { r = 0; g = 0; b = 0.5f + t * 4f; }
        else if (t < 0.375f) { r = 0; g = (t - 0.125f) * 4f; b = 1; }
        else if (t < 0.5f) { r = 0; g = 1; b = 1 - (t - 0.375f) * 4f; }
        else if (t < 0.625f) { r = (t - 0.5f) * 4f; g = 1; b = 0; }
        else if (t < 0.875f) { r = 1; g = 1 - (t - 0.625f) * 4f; b = 0; }
        else { r = 1 - (t - 0.875f) * 4f; g = 0; b = 0; }
        out[off]   = clamp(r);
        out[off+1] = clamp(g);
        out[off+2] = clamp(b);
        out[off+3] = 1f;
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
