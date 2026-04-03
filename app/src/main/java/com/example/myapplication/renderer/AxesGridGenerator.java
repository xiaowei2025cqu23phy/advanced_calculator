package com.example.myapplication.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * Generates a coordinate system: ground grid, X/Y/Z axes with arrows,
 * tick marks, and letter labels.
 */
public class AxesGridGenerator {

    private static final float EXT = SurfaceMeshGenerator.RANGE + 0.8f;
    private static final float GRID_EXT = SurfaceMeshGenerator.RANGE + 0.2f;
    private static final float GRID_Y = -SurfaceMeshGenerator.RANGE - 0.3f;

    public static class AxesResult {
        public final FloatBuffer vertexBuffer;
        public final FloatBuffer colorBuffer;
        public final int vertexCount;

        AxesResult(FloatBuffer vb, FloatBuffer cb, int vc) {
            vertexBuffer = vb; colorBuffer = cb; vertexCount = vc;
        }
    }

    public AxesResult generate() {
        ArrayList<Float> v = new ArrayList<>();
        ArrayList<Float> c = new ArrayList<>();

        generateGrid(v, c);
        generateAxes(v, c);
        generateArrows(v, c);
        generateTicks(v, c);
        generateLabels(v, c);

        int n = v.size() / 3;
        float[] verts = new float[v.size()];
        float[] cols  = new float[c.size()];
        for (int i = 0; i < v.size(); i++) verts[i] = v.get(i);
        for (int i = 0; i < c.size(); i++) cols[i] = c.get(i);

        return new AxesResult(makeBuffer(verts), makeBuffer(cols), n);
    }

    private void generateGrid(ArrayList<Float> v, ArrayList<Float> c) {
        for (int i = -3; i <= 3; i++) {
            float p = i * 1.0f;
            if (p < -GRID_EXT || p > GRID_EXT) continue;
            addLine(v, -GRID_EXT, GRID_Y, p, GRID_EXT, GRID_Y, p);
            addColor(c, 0.35f, 0.35f, 0.35f, 0.5f, 2);
            addLine(v, p, GRID_Y, -GRID_EXT, p, GRID_Y, GRID_EXT);
            addColor(c, 0.35f, 0.35f, 0.35f, 0.5f, 2);
        }
    }

    private void generateAxes(ArrayList<Float> v, ArrayList<Float> c) {
        addLine(v, -EXT, 0, 0, EXT, 0, 0);  addColor(c, 1f, 0.2f, 0.2f, 1f, 2);  // X
        addLine(v, 0, -EXT, 0, 0, EXT, 0);  addColor(c, 0.2f, 1f, 0.2f, 1f, 2);  // Y
        addLine(v, 0, 0, -EXT, 0, 0, EXT);  addColor(c, 0.2f, 0.4f, 1f, 1f, 2);  // Z
    }

    private void generateArrows(ArrayList<Float> v, ArrayList<Float> c) {
        float s = 0.15f;
        // X arrow
        addLine(v, EXT, 0, 0, EXT - s*2, s, 0);
        addLine(v, EXT, 0, 0, EXT - s*2, -s, 0);
        addColor(c, 1f, 0.2f, 0.2f, 1f, 4);
        // Y arrow
        addLine(v, 0, EXT, 0, s, EXT - s*2, 0);
        addLine(v, 0, EXT, 0, -s, EXT - s*2, 0);
        addColor(c, 0.2f, 1f, 0.2f, 1f, 4);
        // Z arrow
        addLine(v, 0, 0, EXT, s, 0, EXT - s*2);
        addLine(v, 0, 0, EXT, -s, 0, EXT - s*2);
        addColor(c, 0.2f, 0.4f, 1f, 1f, 4);
    }

    private void generateTicks(ArrayList<Float> v, ArrayList<Float> c) {
        float tick = 0.12f;
        for (int i = -3; i <= 3; i++) {
            if (i == 0) continue;
            float p = i * 1.0f;
            addLine(v, p, -tick, 0, p, tick, 0);       addColor(c, 1f, 0.5f, 0.5f, 1f, 2);
            addLine(v, -tick, p, 0, tick, p, 0);       addColor(c, 0.5f, 1f, 0.5f, 1f, 2);
            addLine(v, 0, -tick, p, 0, tick, p);       addColor(c, 0.5f, 0.5f, 1f, 1f, 2);
        }
    }

    private void generateLabels(ArrayList<Float> v, ArrayList<Float> c) {
        float ls = 0.22f;
        // X
        addLine(v, EXT-ls, ls, 0, EXT+ls, -ls, 0);
        addLine(v, EXT-ls, -ls, 0, EXT+ls, ls, 0);
        addColor(c, 1f, 0.5f, 0.5f, 1f, 4);
        // Y
        addLine(v, -ls, EXT+ls, 0, 0, EXT-ls*0.7f, 0);
        addLine(v, ls, EXT+ls, 0, 0, EXT-ls*0.7f, 0);
        addLine(v, 0, EXT-ls*0.7f, 0, 0, EXT-ls, 0);
        addColor(c, 0.5f, 1f, 0.5f, 1f, 6);
        // Z
        addLine(v, -ls, ls, EXT+ls, ls, ls, EXT+ls);
        addLine(v, ls, ls, EXT+ls, -ls, -ls, EXT+ls);
        addLine(v, -ls, -ls, EXT+ls, ls, -ls, EXT+ls);
        addColor(c, 0.5f, 0.6f, 1f, 1f, 6);
    }

    private static void addLine(ArrayList<Float> v, float x1, float y1, float z1,
                                                      float x2, float y2, float z2) {
        v.add(x1); v.add(y1); v.add(z1);
        v.add(x2); v.add(y2); v.add(z2);
    }

    private static void addColor(ArrayList<Float> c, float r, float g, float b, float a, int count) {
        for (int i = 0; i < count; i++) { c.add(r); c.add(g); c.add(b); c.add(a); }
    }

    private static FloatBuffer makeBuffer(float[] data) {
        FloatBuffer buf = ByteBuffer.allocateDirect(data.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        buf.put(data).position(0);
        return buf;
    }
}
