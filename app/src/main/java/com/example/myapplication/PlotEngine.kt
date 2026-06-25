package com.example.myapplication

import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure computation — generates (x,y) point lists for all 2D graph modes.
 * No Android dependencies, testable in JUnit.
 */
object PlotEngine {

    fun evalCartesian(expr: String, xMin: Double, xMax: Double, step: Double): List<FloatArray> {
        val pts = mutableListOf<FloatArray>()
        try {
            val x = Argument("x")
            val e = Expression(expr, x)
            if (!e.checkSyntax()) return pts
            var v = xMin
            while (v <= xMax + step * 0.5) {
                x.setArgumentValue(v)
                val y = e.calculate()
                if (!y.isNaN() && !y.isInfinite()) {
                    pts.add(floatArrayOf(v.toFloat(), y.toFloat()))
                }
                v += step
            }
        } catch (ignored: Exception) {
        }
        return pts
    }

    fun evalParametric(
        xExpr: String, yExpr: String,
        tMin: Double, tMax: Double, tStep: Double
    ): List<FloatArray> {
        val pts = mutableListOf<FloatArray>()
        try {
            val t = Argument("t")
            val ex = Expression(xExpr, t)
            val ey = Expression(yExpr, t)
            if (!ex.checkSyntax() || !ey.checkSyntax()) return pts
            var v = tMin
            while (v <= tMax + tStep * 0.5) {
                t.setArgumentValue(v)
                val x = ex.calculate()
                val y = ey.calculate()
                if (!x.isNaN() && !x.isInfinite() && !y.isNaN() && !y.isInfinite()) {
                    pts.add(floatArrayOf(x.toFloat(), y.toFloat()))
                }
                v += tStep
            }
        } catch (ignored: Exception) {
        }
        return pts
    }

    fun evalPolar(rExpr: String, tMin: Double, tMax: Double, tStep: Double): List<FloatArray> {
        val pts = mutableListOf<FloatArray>()
        try {
            val theta = Argument("θ")
            val e = Expression(rExpr, theta)
            if (!e.checkSyntax()) return pts
            var v = tMin
            while (v <= tMax + tStep * 0.5) {
                theta.setArgumentValue(v)
                val r = e.calculate()
                if (!r.isNaN() && !r.isInfinite()) {
                    pts.add(floatArrayOf((r * cos(v)).toFloat(), (r * sin(v)).toFloat()))
                }
                v += tStep
            }
        } catch (ignored: Exception) {
        }
        return pts
    }
}
