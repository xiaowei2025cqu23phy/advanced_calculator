package com.example.myapplication.renderer

import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Generates a 3D surface mesh for z = f(x, y) over a grid.
 * Output: vertex buffer (interleaved with colors via jet colormap).
 */
class SurfaceMeshGenerator {

    companion object {
        const val GRID_SIZE = 64
        const val RANGE = 3.0f
    }

    class MeshResult(
        val vertexBuffer: FloatBuffer,
        val colorBuffer: FloatBuffer,
        val vertexCount: Int
    )

    fun generate(functionExpr: String): MeshResult {
        val xArg = Argument("x")
        val yArg = Argument("y")
        val expr = Expression(functionExpr, xArg, yArg)

        val n = GRID_SIZE
        val step = 2 * RANGE / (n - 1)

        val zValues = Array(n) { FloatArray(n) }
        var zMin = Float.MAX_VALUE
        var zMax = -Float.MAX_VALUE

        for (i in 0 until n) {
            yArg.setArgumentValue((-RANGE + i * step).toDouble())
            for (j in 0 until n) {
                xArg.setArgumentValue((-RANGE + j * step).toDouble())
                val z = expr.calculate()
                val zVal = if (z.isNaN() || z.isInfinite()) 0f else z.toFloat()
                zValues[i][j] = zVal
                zMin = min(zMin, zVal)
                zMax = max(zMax, zVal)
            }
        }

        var zRange = zMax - zMin
        if (zRange < 1e-6f) zRange = 1f

        val vc = (n - 1) * n * 2
        val verts = FloatArray(vc * 3)
        val cols = FloatArray(vc * 4)
        var idx = 0

        for (i in 0 until n - 1) {
            for (j in 0 until n) {
                for (ro in 0..1) {
                    val x = -RANGE + j * step
                    val y = -RANGE + (i + ro) * step
                    val zNorm = (zValues[i + ro][j] - zMin) / zRange * 2f * RANGE - RANGE
                    verts[idx * 3] = x
                    verts[idx * 3 + 1] = zNorm
                    verts[idx * 3 + 2] = y
                    jetColor((zValues[i + ro][j] - zMin) / zRange, cols, idx * 4)
                    idx++
                }
            }
        }

        return MeshResult(makeBuffer(verts), makeBuffer(cols), vc)
    }

    private fun makeBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(data).position(0)
            }
    }

    private fun jetColor(t: Float, out: FloatArray, off: Int) {
        var r: Float
        var g: Float
        var b: Float
        when {
            t < 0.125f -> {
                r = 0f; g = 0f; b = 0.5f + t * 4f
            }
            t < 0.375f -> {
                r = 0f; g = (t - 0.125f) * 4f; b = 1f
            }
            t < 0.5f -> {
                r = 0f; g = 1f; b = 1 - (t - 0.375f) * 4f
            }
            t < 0.625f -> {
                r = (t - 0.5f) * 4f; g = 1f; b = 0f
            }
            t < 0.875f -> {
                r = 1f; g = 1 - (t - 0.625f) * 4f; b = 0f
            }
            else -> {
                r = 1 - (t - 0.875f) * 4f; g = 0f; b = 0f
            }
        }
        out[off] = clamp(r)
        out[off + 1] = clamp(g)
        out[off + 2] = clamp(b)
        out[off + 3] = 1f
    }

    private fun clamp(v: Float): Float = max(0f, min(1f, v))
}
