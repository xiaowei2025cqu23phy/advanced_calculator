package com.example.myapplication.repository

import com.example.myapplication.model.MathError
import com.example.myapplication.model.MathResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mariuszgromada.math.mxparser.Expression

class ComplexRepository {

    /** Evaluate binary operation on two complex numbers: (a) op (b) */
    suspend fun evaluate(a: String, b: String, op: String): MathResult<String> =
        withContext(Dispatchers.Default) {
            if (a.isBlank() || b.isBlank())
                return@withContext MathResult.failure(MathError.invalidExpression("请输入两个复数"))
            try {
                val expr = "($a) $op ($b)"
                val e = Expression(expr)
                val r = e.calculate()
                if (r.isNaN() || r.isInfinite())
                    MathResult.failure(MathError.invalidExpression("结果无效"))
                else
                    MathResult.success("($a) $op ($b) = ${fmt(r)}")
            } catch (ex: Exception) {
                MathResult.failure(MathError.generic(ex))
            }
        }

    /** Convert rectangular a+bi to polar form: |z| ∠ θ */
    suspend fun rectToPolar(z: String): MathResult<String> = withContext(Dispatchers.Default) {
        if (z.isBlank()) return@withContext MathResult.failure(MathError.invalidExpression("请输入复数"))
        try {
            val magExpr = Expression("abs($z)")
            val angExpr = Expression("arg($z)")
            val mag = magExpr.calculate()
            val ang = angExpr.calculate()
            if (mag.isNaN() || ang.isNaN())
                MathResult.failure(MathError.invalidExpression("无法转换"))
            else
                MathResult.success("$z\n= ${fmt(mag)} ∠ ${fmt(ang)} rad\n= ${fmt(mag)} ∠ ${fmt(ang * 180 / Math.PI)}°")
        } catch (ex: Exception) {
            MathResult.failure(MathError.generic(ex))
        }
    }

    /** Convert polar form r∠θ to rectangular a+bi */
    suspend fun polarToRect(input: String): MathResult<String> = withContext(Dispatchers.Default) {
        if (input.isBlank()) return@withContext MathResult.failure(MathError.invalidExpression("请输入模与辐角"))
        try {
            val parts = input.split("∠", ",")
            if (parts.size < 2)
                return@withContext MathResult.failure(MathError.invalidExpression("格式: 模∠角度 或 模,角度"))
            val mag = parts[0].trim().toDouble()
            val ang = parts[1].trim().toDouble()
            val a = mag * Math.cos(ang)
            val b = mag * Math.sin(ang)
            MathResult.success("${fmt(mag)}∠${fmt(ang)} = ${fmt(a)}${if (b >= 0) "+" else ""}${fmt(b)}i")
        } catch (e: NumberFormatException) {
            MathResult.failure(MathError.invalidExpression("数字格式错误"))
        } catch (ex: Exception) {
            MathResult.failure(MathError.generic(ex))
        }
    }

    /** Compute complex conjugate: conj(a+bi) = a-bi */
    suspend fun conjugate(z: String): MathResult<String> = withContext(Dispatchers.Default) {
        if (z.isBlank()) return@withContext MathResult.failure(MathError.invalidExpression("请输入复数"))
        try {
            val re = Expression("Re($z)").calculate()
            val im = Expression("Im($z)").calculate()
            if (re.isNaN() || im.isNaN())
                MathResult.failure(MathError.invalidExpression("无法计算共轭"))
            else
                MathResult.success("conj($z) = ${fmt(re)}${if (-im >= 0) "+" else ""}${fmt(-im)}i")
        } catch (ex: Exception) { MathResult.failure(MathError.generic(ex)) }
    }

    /** Compute complex argument: arg(a+bi) = atan2(b, a) in radians */
    suspend fun argument(z: String): MathResult<String> = withContext(Dispatchers.Default) {
        if (z.isBlank()) return@withContext MathResult.failure(MathError.invalidExpression("请输入复数"))
        try {
            val arg = Expression("arg($z)").calculate()
            if (arg.isNaN())
                MathResult.failure(MathError.invalidExpression("无法计算幅角"))
            else
                MathResult.success("arg($z) = ${fmt(arg)} rad (${fmt(arg * 180 / Math.PI)}°)")
        } catch (ex: Exception) { MathResult.failure(MathError.generic(ex)) }
    }

    private fun fmt(d: Double): String {
        if (d.isNaN() || d.isInfinite()) return d.toString()
        if (d == Math.floor(d) && !d.isInfinite()) return d.toLong().toString()
        return String.format("%.6f", d).replace("0*$".toRegex(), "").replace("\\.$".toRegex(), "")
            .ifEmpty { "0" }
    }
}
