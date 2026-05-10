package com.example.myapplication

import org.junit.Assert.*
import org.junit.Test

class PlotEngineTest {

    private val TOL = 1e-4f

    @Test fun cartesian_linear() {
        val pts = PlotEngine.evalCartesian("2*x+1", -1.0, 1.0, 1.0)
        assertEquals(3, pts.size)
        assertEquals(-1f, pts[0][0], TOL); assertEquals(-1f, pts[0][1], TOL)
        assertEquals(0f,  pts[1][0], TOL); assertEquals(1f,  pts[1][1], TOL)
        assertEquals(1f,  pts[2][0], TOL); assertEquals(3f,  pts[2][1], TOL)
    }

    @Test fun cartesian_quadratic() {
        val pts = PlotEngine.evalCartesian("x^2", -2.0, 2.0, 1.0)
        assertEquals(5, pts.size)
        assertEquals(0f, pts[2][1], TOL) // x=0 → y=0
        assertEquals(4f, pts[0][1], TOL) // x=-2 → y=4
        assertEquals(4f, pts[4][1], TOL) // x=2 → y=4
    }

    @Test fun cartesian_invalid_expr() {
        val pts = PlotEngine.evalCartesian("invalid^^", -1.0, 1.0, 0.1)
        assertTrue(pts.isEmpty())
    }

    @Test fun parametric_circle() {
        // x = cos(t), y = sin(t) for t in [0, 2π] should approximate a circle
        val pts = PlotEngine.evalParametric("cos(t)", "sin(t)", 0.0, 6.283, 0.1)
        assertTrue(pts.size > 60)
        // First point at (1, 0)
        assertEquals(1f, pts[0][0], 0.05f)
        assertEquals(0f, pts[0][1], 0.05f)
        // Point at t ≈ π/2 → (0, 1)
        val mid = pts.size / 4
        assertEquals(0f, pts[mid][0], 0.1f)
        assertEquals(1f, pts[mid][1], 0.1f)
    }

    @Test fun polar_cardioid() {
        // r = 1 + cos(θ) — cardioid
        val pts = PlotEngine.evalPolar("1+cos(θ)", 0.0, 6.283, 0.05)
        assertTrue(pts.size > 100)
        // At θ=0: r=2 → (2, 0)
        assertEquals(2f, pts[0][0], 0.05f)
        assertEquals(0f, pts[0][1], 0.05f)
        // At θ=π: r=0 → (0, 0)
        assertFalse(pts.isEmpty())
    }

    @Test fun parametric_elliptical() {
        val pts = PlotEngine.evalParametric("2*cos(t)", "3*sin(t)", 0.0, 6.283, 0.1)
        assertTrue(pts.size > 60)
        // x ranges from -2 to 2
        var maxX = 0f; var maxY = 0f
        for (p in pts) {
            if (Math.abs(p[0]) > maxX) maxX = Math.abs(p[0])
            if (Math.abs(p[1]) > maxY) maxY = Math.abs(p[1])
        }
        assertEquals(2f, maxX, 0.1f)
        assertEquals(3f, maxY, 0.1f)
    }
}
