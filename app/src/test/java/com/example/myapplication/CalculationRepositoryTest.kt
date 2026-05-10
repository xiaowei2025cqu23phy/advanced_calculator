package com.example.myapplication

import com.example.myapplication.model.ErrorType
import com.example.myapplication.repository.CalculationRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CalculationRepositoryTest {

    private lateinit var repo: CalculationRepository
    private val TOL = 1e-10

    @Before fun setup() { repo = CalculationRepository() }

    @Test fun add_2_plus_2() = runBlocking {
        val r = repo.evaluate("2+2", false)
        assertTrue(r.isSuccess); assertEquals(4.0, r.data, TOL)
    }

    @Test fun sub_10_minus_3() = runBlocking {
        val r = repo.evaluate("10-3", false)
        assertTrue(r.isSuccess); assertEquals(7.0, r.data, TOL)
    }

    @Test fun mul_6_times_7() = runBlocking {
        val r = repo.evaluate("6×7", false)
        assertTrue(r.isSuccess); assertEquals(42.0, r.data, TOL)
    }

    @Test fun div_10_over_2() = runBlocking {
        val r = repo.evaluate("10÷2", false)
        assertTrue(r.isSuccess); assertEquals(5.0, r.data, TOL)
    }

    @Test fun power_2_to_10() = runBlocking {
        val r = repo.evaluate("2^10", false)
        assertTrue(r.isSuccess); assertEquals(1024.0, r.data, TOL)
    }

    @Test fun parentheses_2x7() = runBlocking {
        val r = repo.evaluate("2*(3+4)", false)
        assertTrue(r.isSuccess); assertEquals(14.0, r.data, TOL)
    }

    @Test fun constant_pi() = runBlocking {
        val r = repo.evaluate("pi", false)
        assertTrue(r.isSuccess); assertEquals(Math.PI, r.data, TOL)
    }

    @Test fun constant_e() = runBlocking {
        val r = repo.evaluate("e", false)
        assertTrue(r.isSuccess); assertEquals(Math.E, r.data, 1e-10)
    }

    @Test fun trig_sin_pi_over_2() = runBlocking {
        val r = repo.evaluate("sin(pi/2)", false)
        assertTrue(r.isSuccess); assertEquals(1.0, r.data, TOL)
    }

    @Test fun trig_cos_pi() = runBlocking {
        val r = repo.evaluate("cos(pi)", false)
        assertTrue(r.isSuccess); assertEquals(-1.0, r.data, TOL)
    }

    @Test fun trig_asin_1() = runBlocking {
        val r = repo.evaluate("asin(1)", false)
        assertTrue(r.isSuccess); assertEquals(Math.PI / 2, r.data, TOL)
    }

    @Test fun deg_sin_90() = runBlocking {
        val r = repo.evaluate("sin(90)", true)
        assertTrue(r.isSuccess); assertEquals(1.0, r.data, TOL)
    }

    @Test fun deg_sin_30() = runBlocking {
        val r = repo.evaluate("sin(30)", true)
        assertTrue(r.isSuccess); assertEquals(0.5, r.data, TOL)
    }

    @Test fun deg_cos_60() = runBlocking {
        val r = repo.evaluate("cos(60)", true)
        assertTrue(r.isSuccess); assertEquals(0.5, r.data, TOL)
    }

    @Test fun deg_tan_45() = runBlocking {
        val r = repo.evaluate("tan(45)", true)
        assertTrue(r.isSuccess); assertEquals(1.0, r.data, TOL)
    }

    @Test fun deg_asin_0_5() = runBlocking {
        val r = repo.evaluate("asin(0.5)", true)
        assertTrue(r.isSuccess); assertEquals(30.0, r.data, TOL)
    }

    @Test fun deg_acos_0_5() = runBlocking {
        val r = repo.evaluate("acos(0.5)", true)
        assertTrue(r.isSuccess); assertEquals(60.0, r.data, TOL)
    }

    @Test fun deg_atan_1() = runBlocking {
        val r = repo.evaluate("atan(1)", true)
        assertTrue(r.isSuccess); assertEquals(45.0, r.data, TOL)
    }

    @Test fun deg_vs_rad_asin() = runBlocking {
        val deg = repo.evaluate("asin(0.5)", true)
        val rad = repo.evaluate("asin(0.5)", false)
        assertTrue(deg.isSuccess); assertTrue(rad.isSuccess)
        assertEquals(30.0, deg.data, TOL)
        assertEquals(Math.PI / 6, rad.data, TOL)
    }

    @Test fun log10_100() = runBlocking {
        val r = repo.evaluate("log10(100)", false)
        assertTrue(r.isSuccess); assertEquals(2.0, r.data, TOL)
    }

    @Test fun ln_e() = runBlocking {
        val r = repo.evaluate("ln(e)", false)
        assertTrue(r.isSuccess); assertEquals(1.0, r.data, TOL)
    }

    @Test fun sqrt_9() = runBlocking {
        val r = repo.evaluate("sqrt(9)", false)
        assertTrue(r.isSuccess); assertEquals(3.0, r.data, TOL)
    }

    @Test fun abs_minus_5() = runBlocking {
        val r = repo.evaluate("abs(-5)", false)
        assertTrue(r.isSuccess); assertEquals(5.0, r.data, TOL)
    }

    @Test fun factorial_5() = runBlocking {
        val r = repo.evaluate("5!", false)
        assertTrue(r.isSuccess); assertEquals(120.0, r.data, TOL)
    }

    @Test fun empty_expression() = runBlocking {
        val r = repo.evaluate("", false)
        assertFalse(r.isSuccess); assertEquals(ErrorType.INVALID_EXPRESSION, r.error?.type)
    }

    @Test fun division_by_zero() = runBlocking {
        val r = repo.evaluate("1/0", false)
        // mXparser returns NaN for 1/0
        assertFalse(r.isSuccess); assertEquals(ErrorType.INVALID_EXPRESSION, r.error?.type)
    }

    @Test fun zero_over_zero() = runBlocking {
        val r = repo.evaluate("0/0", false)
        assertFalse(r.isSuccess); assertEquals(ErrorType.INVALID_EXPRESSION, r.error?.type)
    }

    @Test fun garbage_expression() = runBlocking {
        val r = repo.evaluate("abc++xyz", false)
        assertFalse(r.isSuccess); assertEquals(ErrorType.INVALID_EXPRESSION, r.error?.type)
    }

    @Test fun unmatched_parens() = runBlocking {
        val r = repo.evaluate("(2+3", false)
        assertFalse(r.isSuccess); assertEquals(ErrorType.INVALID_EXPRESSION, r.error?.type)
    }

    @Test fun times_symbol() = runBlocking {
        val r = repo.evaluate("3×3", false)
        assertTrue(r.isSuccess); assertEquals(9.0, r.data, TOL)
    }

    @Test fun divide_symbol() = runBlocking {
        val r = repo.evaluate("6÷3", false)
        assertTrue(r.isSuccess); assertEquals(2.0, r.data, TOL)
    }

    @Test fun deg_replacement_in_expression() = runBlocking {
        val r = repo.evaluate("sin(90)", true)
        assertTrue(r.isSuccess); assertEquals(1.0, r.data, TOL)
    }
}
