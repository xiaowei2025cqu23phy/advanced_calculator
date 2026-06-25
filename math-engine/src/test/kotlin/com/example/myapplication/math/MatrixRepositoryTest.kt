package com.example.myapplication.math

import com.example.myapplication.math.model.ErrorType
import com.example.myapplication.math.MatrixRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MatrixRepository.
 * Covers: parse, determinant, inverse, transpose, trace, add/sub/mul, dimension errors.
 */
class MatrixRepositoryTest {

    private lateinit var repo: MatrixRepository

    private val TOL = 1e-10

    @Before
    fun setup() {
        repo = MatrixRepository()
    }

    // ── Parse ──

    @Test
    fun `parse 2x2 matrix`() = runBlocking {
        val result = repo.parse("1 2, 3 4")
        assertTrue(result.isSuccess)
        val m = result.data!!
        assertEquals(2, m.numRows())
        assertEquals(2, m.numCols())
        assertEquals(1.0, m[0, 0], TOL)
        assertEquals(4.0, m[1, 1], TOL)
    }

    @Test
    fun `parse 3x3 matrix`() = runBlocking {
        val result = repo.parse("1 0 0, 0 1 0, 0 0 1")
        assertTrue(result.isSuccess)
        val m = result.data!!
        assertEquals(3, m.numRows())
        assertEquals(3, m.numCols())
        assertEquals(1.0, m[1, 1], TOL)
    }

    @Test
    fun `parse invalid input returns error`() = runBlocking {
        val result = repo.parse("abc, def")
        assertFalse(result.isSuccess)
    }

    // ── Determinant ──

    @Test
    fun `determinant of 2x2 identity equals 1`() = runBlocking {
        val m = repo.parse("1 0, 0 1").data!!
        val result = repo.determinant(m)
        assertTrue(result.isSuccess)
        assertEquals(1.0, result.data, TOL)
    }

    @Test
    fun `determinant of 2x2 matrix`() = runBlocking {
        val m = repo.parse("3 8, 4 6").data!!
        val result = repo.determinant(m)
        assertTrue(result.isSuccess)
        assertEquals(-14.0, result.data, TOL)
    }

    @Test
    fun `determinant of singular 2x2 equals 0`() = runBlocking {
        val m = repo.parse("1 2, 2 4").data!!
        val result = repo.determinant(m)
        assertTrue(result.isSuccess)
        assertEquals(0.0, result.data, TOL)
    }

    @Test
    fun `determinant of non-square returns error`() = runBlocking {
        val m = repo.parse("1 2 3, 4 5 6").data!!
        val result = repo.determinant(m)
        assertFalse(result.isSuccess)
        assertEquals(ErrorType.NOT_SQUARE_MATRIX, result.error?.type)
    }

    // ── Inverse ──

    @Test
    fun `inverse of 2x2 identity equals identity`() = runBlocking {
        val m = repo.parse("1 0, 0 1").data!!
        val result = repo.inverse(m)
        assertTrue(result.isSuccess)
        assertEquals(1.0, result.data!![0, 0], TOL)
    }

    @Test
    fun `inverse of singular matrix returns error`() = runBlocking {
        val m = repo.parse("1 2, 2 4").data!!
        val result = repo.inverse(m)
        assertFalse(result.isSuccess)
        assertEquals(ErrorType.SINGULAR_MATRIX, result.error?.type)
    }

    @Test
    fun `inverse of non-square returns error`() = runBlocking {
        val m = repo.parse("1 2 3, 4 5 6").data!!
        val result = repo.inverse(m)
        assertFalse(result.isSuccess)
        assertEquals(ErrorType.NOT_SQUARE_MATRIX, result.error?.type)
    }

    @Test
    fun `inverse times original equals identity`() = runBlocking {
        val m = repo.parse("4 7, 2 6").data!!
        val inv = repo.inverse(m).data!!
        val product = m.mult(inv)
        assertEquals(1.0, product[0, 0], TOL)
        assertEquals(0.0, product[0, 1], TOL)
        assertEquals(0.0, product[1, 0], TOL)
        assertEquals(1.0, product[1, 1], TOL)
    }

    // ── Transpose ──

    @Test
    fun `transpose of 2x3 matrix`() = runBlocking {
        val m = repo.parse("1 2 3, 4 5 6").data!!
        val result = repo.transpose(m)
        assertTrue(result.isSuccess)
        val t = result.data!!
        assertEquals(3, t.numRows())
        assertEquals(2, t.numCols())
        assertEquals(3.0, t[2, 0], TOL)  // original (0,2) → transposed (2,0)
        assertEquals(6.0, t[2, 1], TOL)  // original (1,2) → transposed (2,1)
    }

    @Test
    fun `double transpose returns original`() = runBlocking {
        val m = repo.parse("1 2, 3 4").data!!
        val t1 = repo.transpose(m).data!!
        val t2 = repo.transpose(t1).data!!
        assertEquals(m[0, 0], t2[0, 0], TOL)
        assertEquals(m[0, 1], t2[0, 1], TOL)
        assertEquals(m[1, 0], t2[1, 0], TOL)
    }

    // ── Trace ──

    @Test
    fun `trace of identity equals dimension`() = runBlocking {
        val m = repo.parse("1 0 0, 0 1 0, 0 0 1").data!!
        val result = repo.trace(m)
        assertTrue(result.isSuccess)
        assertEquals(3.0, result.data, TOL)
    }

    @Test
    fun `trace of non-square returns error`() = runBlocking {
        val m = repo.parse("1 2 3, 4 5 6").data!!
        val result = repo.trace(m)
        assertFalse(result.isSuccess)
        assertEquals(ErrorType.NOT_SQUARE_MATRIX, result.error?.type)
    }

    @Test
    fun `trace of 2x2 matrix`() = runBlocking {
        val m = repo.parse("5 6, 7 8").data!!
        val result = repo.trace(m)
        assertTrue(result.isSuccess)
        assertEquals(13.0, result.data, TOL)
    }

    // ── Add / Subtract / Multiply ──

    @Test
    fun `add two 2x2 matrices`() = runBlocking {
        val a = repo.parse("1 2, 3 4").data!!
        val b = repo.parse("5 6, 7 8").data!!
        val result = repo.add(a, b)
        assertTrue(result.isSuccess)
        assertEquals(6.0, result.data!![0, 0], TOL)
        assertEquals(12.0, result.data!![1, 1], TOL)
    }

    @Test
    fun `subtract two 2x2 matrices`() = runBlocking {
        val a = repo.parse("5 6, 7 8").data!!
        val b = repo.parse("1 2, 3 4").data!!
        val result = repo.subtract(a, b)
        assertTrue(result.isSuccess)
        assertEquals(4.0, result.data!![0, 0], TOL)
        assertEquals(4.0, result.data!![1, 1], TOL)
    }

    @Test
    fun `add dimension mismatch returns error`() = runBlocking {
        val a = repo.parse("1 2 3, 4 5 6").data!!
        val b = repo.parse("1 0, 0 1").data!!
        val result = repo.add(a, b)
        assertFalse(result.isSuccess)
        assertEquals(ErrorType.DIMENSION_MISMATCH, result.error?.type)
    }

    @Test
    fun `multiply 2x3 by 3x2`() = runBlocking {
        val a = repo.parse("1 2 3, 4 5 6").data!!    // 2x3
        val b = repo.parse("7 8, 9 10, 11 12").data!! // 3x2
        val result = repo.multiply(a, b)
        assertTrue(result.isSuccess)
        val m = result.data!!
        assertEquals(2, m.numRows())
        assertEquals(2, m.numCols())
        // [0,0] = 1*7 + 2*9 + 3*11 = 58
        assertEquals(58.0, m[0, 0], TOL)
        // [1,0] = 4*7 + 5*9 + 6*11 = 139
        assertEquals(139.0, m[1, 0], TOL)
    }

    @Test
    fun `multiply dimension mismatch returns error`() = runBlocking {
        val a = repo.parse("1 2 3, 4 5 6").data!!  // 2x3
        val b = repo.parse("1 0 0, 0 1 0, 0 0 1, 0 0 0").data!! // 4x3
        val result = repo.multiply(a, b)
        assertFalse(result.isSuccess)
        assertEquals(ErrorType.DIMENSION_MISMATCH, result.error?.type)
    }

    // ── Format ──

    @Test
    fun `format 2x2 matrix`() = runBlocking {
        val m = repo.parse("1 2, 3 4").data!!
        val s = repo.format(m)
        assertTrue(s.contains("1"))
        assertTrue(s.contains("4"))
        assertTrue(s.contains("\n"))
    }
}
