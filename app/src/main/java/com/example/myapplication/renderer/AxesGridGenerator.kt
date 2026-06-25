package com.example.myapplication.renderer

import com.example.myapplication.math.SurfaceMeshGenerator
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Generates a coordinate system: ground grid, X/Y/Z axes with arrows,
 * tick marks, and letter labels.
 */
class AxesGridGenerator {

    companion object {
        private const val EXT = SurfaceMeshGenerator.RANGE + 0.8f
        private const val GRID_EXT = SurfaceMeshGenerator.RANGE + 0.2f
        private const val GRID_Y = -SurfaceMeshGenerator.RANGE - 0.3f
    }

    class AxesResult(
        val vertexBuffer: FloatBuffer,
        val colorBuffer: FloatBuffer,
        val vertexCount: Int
    )

    fun generate(): AxesResult {
        val v = mutableListOf<Float>()
        val c = mutableListOf<Float>()

        generateGrid(v, c)
        generateAxes(v, c)
        generateArrows(v, c)
        generateTicks(v, c)
        generateLabels(v, c)

        val n = v.size / 3
        val verts = v.toFloatArray()
        val cols = c.toFloatArray()

        return AxesResult(makeBuffer(verts), makeBuffer(cols), n)
    }

    private fun generateGrid(v: MutableList<Float>, c: MutableList<Float>) {
        for (i in -3..3) {
            val p = i * 1.0f
            if (p < -GRID_EXT || p > GRID_EXT) continue
            addLine(v, -GRID_EXT, GRID_Y, p, GRID_EXT, GRID_Y, p)
            addColor(c, 0.35f, 0.35f, 0.35f, 0.5f, 2)
            addLine(v, p, GRID_Y, -GRID_EXT, p, GRID_Y, GRID_EXT)
            addColor(c, 0.35f, 0.35f, 0.35f, 0.5f, 2)
        }
    }

    private fun generateAxes(v: MutableList<Float>, c: MutableList<Float>) {
        addLine(v, -EXT, 0f, 0f, EXT, 0f, 0f); addColor(c, 1f, 0.2f, 0.2f, 1f, 2) // X
        addLine(v, 0f, -EXT, 0f, 0f, EXT, 0f); addColor(c, 0.2f, 1f, 0.2f, 1f, 2) // Y
        addLine(v, 0f, 0f, -EXT, 0f, 0f, EXT); addColor(c, 0.2f, 0.4f, 1f, 1f, 2) // Z
    }

    private fun generateArrows(v: MutableList<Float>, c: MutableList<Float>) {
        val s = 0.15f
        // X arrow
        addLine(v, EXT, 0f, 0f, EXT - s * 2, s, 0f)
        addLine(v, EXT, 0f, 0f, EXT - s * 2, -s, 0f)
        addColor(c, 1f, 0.2f, 0.2f, 1f, 4)
        // Y arrow
        addLine(v, 0f, EXT, 0f, s, EXT - s * 2, 0f)
        addLine(v, 0f, EXT, 0f, -s, EXT - s * 2, 0f)
        addColor(c, 0.2f, 1f, 0.2f, 1f, 4)
        // Z arrow
        addLine(v, 0f, 0f, EXT, s, 0f, EXT - s * 2)
        addLine(v, 0f, 0f, EXT, -s, 0f, EXT - s * 2)
        addColor(c, 0.2f, 0.4f, 1f, 1f, 4)
    }

    private fun generateTicks(v: MutableList<Float>, c: MutableList<Float>) {
        val tick = 0.12f
        for (i in -3..3) {
            if (i == 0) continue
            val p = i * 1.0f
            addLine(v, p, -tick, 0f, p, tick, 0f); addColor(c, 1f, 0.5f, 0.5f, 1f, 2)
            addLine(v, -tick, p, 0f, tick, p, 0f); addColor(c, 0.5f, 1f, 0.5f, 1f, 2)
            addLine(v, 0f, -tick, p, 0f, tick, p); addColor(c, 0.5f, 0.5f, 1f, 1f, 2)
        }
    }

    private fun generateLabels(v: MutableList<Float>, c: MutableList<Float>) {
        val ls = 0.22f
        // X
        addLine(v, EXT - ls, ls, 0f, EXT + ls, -ls, 0f)
        addLine(v, EXT - ls, -ls, 0f, EXT + ls, ls, 0f)
        addColor(c, 1f, 0.5f, 0.5f, 1f, 4)
        // Y
        addLine(v, -ls, EXT + ls, 0f, 0f, EXT - ls * 0.7f, 0f)
        addLine(v, ls, EXT + ls, 0f, 0f, EXT - ls * 0.7f, 0f)
        addLine(v, 0f, EXT - ls * 0.7f, 0f, 0f, EXT - ls, 0f)
        addColor(c, 0.5f, 1f, 0.5f, 1f, 6)
        // Z
        addLine(v, -ls, ls, EXT + ls, ls, ls, EXT + ls)
        addLine(v, ls, ls, EXT + ls, -ls, -ls, EXT + ls)
        addLine(v, -ls, -ls, EXT + ls, ls, -ls, EXT + ls)
        addColor(c, 0.5f, 0.6f, 1f, 1f, 6)
    }

    private fun addLine(
        v: MutableList<Float>,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float
    ) {
        v.add(x1); v.add(y1); v.add(z1)
        v.add(x2); v.add(y2); v.add(z2)
    }

    private fun addColor(c: MutableList<Float>, r: Float, g: Float, b: Float, a: Float, count: Int) {
        for (i in 0 until count) {
            c.add(r); c.add(g); c.add(b); c.add(a)
        }
    }

    private fun makeBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(data).position(0)
            }
    }
}
