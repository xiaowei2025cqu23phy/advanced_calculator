package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import kotlin.math.*

/**
 * 2D chart with fling-to-pan inertia.
 */
class SimpleChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val gridPaint = Paint().apply {
        color = 0x33FFFFFF
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val axisPaint = Paint().apply {
        color = 0xAAFFFFFF.toInt()
        strokeWidth = 2f
    }
    private val labelPaint = Paint().apply {
        color = 0x88FFFFFF.toInt()
        textSize = 24f
        isAntiAlias = true
    }
    private val curves = mutableListOf<CurveData>()
    private val curvePaints = mutableListOf<Paint>()
    private val scroller = OverScroller(context)
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val curvePaths = mutableListOf<Path>() // reusable path objects

    private var xMin = -10f
    private var xMax = 10f
    private var yMin = -10f
    private var yMax = 10f
    private var baseXMin = -10f
    private var baseXMax = 10f
    private var baseYMin = -10f
    private var baseYMax = 10f

    // Touch
    private var velocityTracker: VelocityTracker? = null
    private var lastX = 0f
    private var lastY = 0f
    private var touching = false
    private var scaleFactor = 1f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = detector.scaleFactor
            val fx = detector.focusX
            val fy = detector.focusY

            // Convert focus point to data coordinates before zoom
            val w = width.toFloat()
            val h = height.toFloat()
            val pad = 50f
            val plotW = w - pad * 2
            val plotH = h - pad * 2
            if (plotW <= 0 || plotH <= 0) return true

            val cx = xMin + (fx - pad) / plotW * (xMax - xMin)
            val cy = yMax - (fy - pad) / plotH * (yMax - yMin)

            val newRangeX = (xMax - xMin) / scaleFactor
            val newRangeY = (yMax - yMin) / scaleFactor

            xMin = cx - newRangeX * (fx - pad) / plotW
            xMax = cx + newRangeX * (1 - (fx - pad) / plotW)
            yMin = cy - newRangeY * (1 - (fy - pad) / plotH)
            yMax = cy + newRangeY * (fy - pad) / plotH

            ViewCompat.postInvalidateOnAnimation(this@SimpleChartView)
            return true
        }
    })

    init {
        setBackgroundColor(0xFF121214.toInt())
    }

    fun setBounds(xMn: Float, xMx: Float, yMn: Float, yMx: Float) {
        xMin = xMn; xMax = xMx; yMin = yMn; yMax = yMx
        baseXMin = xMn; baseXMax = xMx; baseYMin = yMn; baseYMax = yMx
        ViewCompat.postInvalidateOnAnimation(this)
    }

    fun resetBounds() {
        xMin = baseXMin; xMax = baseXMax
        yMin = baseYMin; yMax = baseYMax
        ViewCompat.postInvalidateOnAnimation(this)
    }

    fun addCurve(points: List<FloatArray>, color: Int, label: String) {
        curves.add(CurveData(points, color, label))
        curvePaths.add(Path()) // placeholder, rebuilt on next draw
        val p = Paint().apply {
            this.color = color
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        curvePaints.add(p)
        if (points.isNotEmpty()) {
            var mn = Float.MAX_VALUE
            var mx = -Float.MAX_VALUE
            for (pt2 in points) {
                mn = min(mn, pt2[1])
                mx = max(mx, pt2[1])
            }
            var pad = (mx - mn) * 0.1f
            if (pad < 0.01f) pad = 1f
            yMin = mn - pad
            yMax = mx + pad
            baseYMin = yMin
            baseYMax = yMax
        }
        ViewCompat.postInvalidateOnAnimation(this)
    }

    fun clearCurves() {
        curves.clear()
        curvePaints.clear()
        curvePaths.clear()
        ViewCompat.postInvalidateOnAnimation(this)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val xRange = xMax - xMin
            val yRange = yMax - yMin
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            val newXMin = baseXMin - scroller.currX / (w * 0.5f) * xRange
            val newXMax = newXMin + xRange
            val newYMin = baseYMin + scroller.currY / (h * 0.5f) * yRange
            val newYMax = newYMin + yRange

            xMin = newXMin
            xMax = newXMax
            yMin = newYMin
            yMax = newYMax
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = 50f
        val plotW = w - pad * 2
        val plotH = h - pad * 2
        if (plotW <= 0 || plotH <= 0) return

        val scaleX = plotW / (xMax - xMin)
        val scaleY = plotH / (yMax - yMin)

        // Grid
        var gStep = niceStep(xMax - xMin)
        var v = ceil(xMin / gStep) * gStep
        while (v <= xMax) {
            val px = pad + (v - xMin) * scaleX
            c.drawLine(px, pad, px, pad + plotH, gridPaint)
            v += gStep
        }
        gStep = niceStep(yMax - yMin)
        v = ceil(yMin / gStep) * gStep
        while (v <= yMax) {
            val py = pad + (yMax - v) * scaleY
            c.drawLine(pad, py, pad + plotW, py, gridPaint)
            v += gStep
        }

        // Axes
        val ox = pad + (0 - xMin) * scaleX
        val oy = pad + (yMax - 0) * scaleY
        if (xMin <= 0 && xMax >= 0) c.drawLine(ox, pad, ox, pad + plotH, axisPaint)
        if (yMin <= 0 && yMax >= 0) c.drawLine(pad, oy, pad + plotW, oy, axisPaint)

        // Labels
        gStep = niceStep(xMax - xMin)
        v = ceil(xMin / gStep) * gStep
        while (v <= xMax) {
            val px = pad + (v - xMin) * scaleX
            c.drawText(formatNum(v), px - 10, pad + plotH + 18, labelPaint)
            v += gStep
        }
        gStep = niceStep(yMax - yMin)
        v = ceil(yMin / gStep) * gStep
        while (v <= yMax) {
            val py = pad + (yMax - v) * scaleY
            c.drawText(formatNum(v), 4f, py + 4, labelPaint)
            v += gStep
        }

        // Curves
        for (ci in curves.indices) {
            val cd = curves[ci]
            val path = if (ci < curvePaths.size) curvePaths[ci] else Path()
            path.rewind()
            var first = true
            for (pt in cd.points) {
                val px = pad + (pt[0] - xMin) * scaleX
                val py2 = pad + (yMax - pt[1]) * scaleY
                if (first) {
                    path.moveTo(px, py2)
                    first = false
                } else path.lineTo(px, py2)
            }
            if (ci >= curvePaths.size) curvePaths.add(path)
            val lp = if (ci < curvePaints.size) curvePaints[ci] else curvePaints[0]
            c.drawPath(path, lp)
            if (cd.points.isNotEmpty()) {
                val last = cd.points.last()
                val lx = pad + (last[0] - xMin) * scaleX
                val ly = pad + (yMax - last[1]) * scaleY
                c.drawText(cd.label, min(lx + 8, w - 60), ly - 4, lp)
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(e)

        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(e)

        if (scaleDetector.isInProgress) {
            if (e.action == MotionEvent.ACTION_UP) {
                velocityTracker?.recycle()
                velocityTracker = null
            }
            return true
        }

        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                scroller.forceFinished(true)
                lastX = e.x
                lastY = e.y
                touching = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (lastX - e.x) / width * (xMax - xMin)
                val dy = (lastY - e.y) / height * (yMax - yMin)
                xMin += dx; xMax += dx
                yMin += dy; yMax += dy
                lastX = e.x
                lastY = e.y
                ViewCompat.postInvalidateOnAnimation(this)
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.computeCurrentVelocity(1000)
                val vx = velocityTracker?.xVelocity ?: 0f
                val vy = velocityTracker?.yVelocity ?: 0f

                if (abs(vx) > minFlingVelocity || abs(vy) > minFlingVelocity) {
                    baseXMin = xMin; baseXMax = xMax
                    baseYMin = yMin; baseYMax = yMax
                    scroller.forceFinished(true)
                    scroller.fling(
                        (width * 0.5f * (xMin / (xMax - xMin))).roundToInt(),
                        (height * 0.5f * (yMin / (yMax - yMin))).roundToInt(),
                        (-vx * 2).toInt(), (-vy * 2).toInt(),
                        Int.MIN_VALUE, Int.MAX_VALUE,
                        Int.MIN_VALUE, Int.MAX_VALUE
                    )
                    postInvalidateOnAnimation()
                } else if (touching && abs(e.x - lastX) < 5 && abs(e.y - lastY) < 5) {
                    resetBounds()
                }
                touching = false
                velocityTracker?.recycle()
                velocityTracker = null
                performClick()
                return true
            }
        }
        return super.onTouchEvent(e)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun niceStep(range: Float): Float {
        val rough = range / 5
        val mag = 10f.pow(floor(log10(rough.toDouble())).toFloat())
        val norm = rough / mag
        return when {
            norm < 1.5f -> mag
            norm < 3.5f -> mag * 2
            norm < 7.5f -> mag * 5
            else -> mag * 10
        }
    }

    private fun formatNum(v: Float): String {
        if (abs(v) < 0.0001f) return "0"
        if (abs(v) >= 100) return "%.0f".format(v)
        if (abs(v) >= 1) return "%.1f".format(v)
        return "%.2f".format(v)
    }

    data class CurveData(val points: List<FloatArray>, val color: Int, val label: String)
}
