package com.example.myapplication

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.myapplication.renderer.AxesGridGenerator
import com.example.myapplication.renderer.ShaderHelper
import com.example.myapplication.renderer.SurfaceMeshGenerator
import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.Executors
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Orchestrator with async mesh/curve generation.
 * Mesh computation (mXparser) runs on background executor;
 * only buffer upload + draw happens on the GL thread.
 */
class Graph3DRenderer : GLSurfaceView.Renderer {

    private val meshGen = SurfaceMeshGenerator()
    private val axesGen = AxesGridGenerator()
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "mesh-gen").apply {
            isDaemon = true
        }
    }

    private var program = 0
    private var muMVPMatrixHandle = 0
    private var maPositionHandle = 0
    private var maColorHandle = 0
    private val projMatrix = FloatArray(16)

    // Surface data (read by GL thread, written by executor)
    @Volatile private var surfVerts: FloatBuffer? = null
    @Volatile private var surfColors: FloatBuffer? = null
    @Volatile private var surfCount = 0
    @Volatile private var pendingMesh: SurfaceMeshGenerator.MeshResult? = null

    // Curve data
    @Volatile private var curveVerts: FloatBuffer? = null
    @Volatile private var curveColors: FloatBuffer? = null
    @Volatile private var curveCount = 0
    @Volatile private var pendingCurveVerts: FloatBuffer? = null
    @Volatile private var pendingCurveColors: FloatBuffer? = null
    @Volatile private var pendingCurveCount = 0

    // Axes data (always generated on GL thread — fast)
    private var axesVerts: FloatBuffer? = null
    private var axesColors: FloatBuffer? = null
    private var axesCount = 0

    @Volatile private var functionExpr = "sin(x)*cos(y)"
    @Volatile private var curveMode = false
    @Volatile private var cx: String? = null
    @Volatile private var cy: String? = null
    @Volatile private var cz: String? = null
    @Volatile private var tMin = 0f
    @Volatile private var tMax = 0f
    @Volatile private var tStep = 0f

    /** Incremented on every setFunction/setParametricFunction call.
     *  Background tasks capture this at start and only apply result if still current. */
    @Volatile private var generation = 0
    private val computeLock = Any()

    var rotX = -25f
        private set
    var rotY = 0f
        private set

    fun shutdown() {
        executor.shutdownNow()
    }

    fun setFunction(expr: String) {
        functionExpr = expr
        curveMode = false
        generation++
        submitMesh(expr, generation)
    }

    fun setParametricFunction(x: String, y: String, z: String, mn: Float, mx: Float, st: Float) {
        cx = x; cy = y; cz = z; tMin = mn; tMax = mx; tStep = st
        curveMode = true
        val gen = ++generation
        submitCurve(x, y, z, mn, mx, st, gen)
    }

    fun setRotation(rx: Float, ry: Float) {
        rotX = rx
        rotY = ry
    }

    // ── Background computation ──

    private fun submitMesh(expr: String, gen: Int) {
        executor.submit {
            val r = meshGen.generate(expr)
            synchronized(computeLock) {
                if (gen == generation) pendingMesh = r
            }
        }
    }

    private fun submitCurve(x: String, y: String, z: String, mn: Float, mx: Float, st: Float, gen: Int) {
        executor.submit {
            val t = Argument("t")
            val ex = Expression(x, t)
            val ey = Expression(y, t)
            val ez = Expression(z, t)
            if (!ex.checkSyntax() || !ey.checkSyntax() || !ez.checkSyntax()) return@submit

            val pts = mutableListOf<Float>()
            var maxAbs = 0f
            var v = mn.toDouble()
            while (v <= mx + st * 0.5) {
                t.setArgumentValue(v)
                val dx = ex.calculate()
                if (dx.isNaN() || dx.isInfinite()) { v += st; continue }
                val dy = ey.calculate()
                if (dy.isNaN() || dy.isInfinite()) { v += st; continue }
                val dz = ez.calculate()
                if (dz.isNaN() || dz.isInfinite()) { v += st; continue }
                
                pts.add(dx.toFloat())
                pts.add(dy.toFloat())
                pts.add(dz.toFloat())
                
                maxAbs = max(maxAbs, max(abs(dx.toFloat()), max(abs(dy.toFloat()), abs(dz.toFloat()))))
                v += st
            }
            if (pts.size < 6) return@submit
            if (maxAbs < 0.01f) maxAbs = 1f

            val s = SurfaceMeshGenerator.RANGE * 0.95f / maxAbs
            val n = pts.size / 3
            val verts = FloatArray(n * 3)
            val cols = FloatArray(n * 4)
            for (i in 0 until n) {
                verts[i * 3] = pts[i * 3] * s
                verts[i * 3 + 1] = pts[i * 3 + 1] * s
                verts[i * 3 + 2] = pts[i * 3 + 2] * s
                rainbowColor(i.toFloat() / n, cols, i * 4)
            }
            synchronized(computeLock) {
                if (gen == generation) {
                    pendingCurveVerts = buf(verts)
                    pendingCurveColors = buf(cols)
                    pendingCurveCount = n
                }
            }
        }
    }

    // ── GL lifecycle ──

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.12f, 0.12f, 0.14f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glLineWidth(3f)

        program = ShaderHelper.createProgram()
        muMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        maPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        maColorHandle = GLES20.glGetAttribLocation(program, "aColor")

        // Initial submission
        generation++
        submitMesh(functionExpr, generation)
        updateAxes()
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
        Matrix.perspectiveM(projMatrix, 0, 45f, w.toFloat() / h, 1f, 30f)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Swap in any pending results
        synchronized(computeLock) {
            pendingMesh?.let { pm ->
                surfVerts = pm.vertexBuffer
                surfColors = pm.colorBuffer
                surfCount = pm.vertexCount
                pendingMesh = null
            }
            if (pendingCurveCount > 0) {
                curveVerts = pendingCurveVerts
                curveColors = pendingCurveColors
                curveCount = pendingCurveCount
                pendingCurveCount = 0
            }
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val mv = FloatArray(16)
        val mvp = FloatArray(16)
        Matrix.setLookAtM(mv, 0, 0f, 0f, 8f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.rotateM(mv, 0, rotX, 1f, 0f, 0f)
        Matrix.rotateM(mv, 0, rotY, 0f, 1f, 0f)
        Matrix.multiplyMM(mvp, 0, projMatrix, 0, mv, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mvp, 0)

        draw(axesVerts, axesColors, axesCount, GLES20.GL_LINES, false)
        if (curveMode && curveCount > 0)
            draw(curveVerts, curveColors, curveCount, GLES20.GL_LINE_STRIP, false)
        else
            draw(surfVerts, surfColors, surfCount, GLES20.GL_TRIANGLE_STRIP, true)
    }

    // ── Helpers ──

    private fun updateAxes() {
        val ar = axesGen.generate()
        axesVerts = ar.vertexBuffer
        axesColors = ar.colorBuffer
        axesCount = ar.vertexCount
    }

    private fun draw(vb: FloatBuffer?, cb: FloatBuffer?, count: Int, mode: Int, strip: Boolean) {
        if (vb == null || cb == null || count == 0) return
        GLES20.glEnableVertexAttribArray(maPositionHandle)
        vb.position(0)
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 0, vb)
        GLES20.glEnableVertexAttribArray(maColorHandle)
        cb.position(0)
        GLES20.glVertexAttribPointer(maColorHandle, 4, GLES20.GL_FLOAT, false, 0, cb)

        if (strip) {
            val ss = SurfaceMeshGenerator.GRID_SIZE * 2
            for (i in 0 until SurfaceMeshGenerator.GRID_SIZE - 1)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, i * ss, ss)
        } else {
            GLES20.glDrawArrays(mode, 0, count)
        }
        GLES20.glDisableVertexAttribArray(maPositionHandle)
        GLES20.glDisableVertexAttribArray(maColorHandle)
    }

    private fun rainbowColor(t: Float, out: FloatArray, off: Int) {
        out[off] = clamp((sin(t * 12.566f) * 0.5 + 0.5).toFloat())
        out[off + 1] = clamp((sin(t * 12.566f + 2.094f) * 0.5 + 0.5).toFloat())
        out[off + 2] = clamp((sin(t * 12.566f + 4.188f) * 0.5 + 0.5).toFloat())
        out[off + 3] = 1f
    }

    private fun clamp(v: Float): Float = max(0f, min(1f, v))

    private fun buf(d: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(d.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(d)
            position(0)
        }
    }
}
