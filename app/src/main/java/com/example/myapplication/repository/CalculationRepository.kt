package com.example.myapplication.repository

import com.example.myapplication.model.MathError
import com.example.myapplication.model.MathResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mariuszgromada.math.mxparser.Expression

class CalculationRepository {

    /** Evaluate expression with optional degree mode on IO dispatcher. */
    suspend fun evaluate(expression: String, degreeMode: Boolean): MathResult<Double> =
        withContext(Dispatchers.Default) {
            if (expression.isBlank()) return@withContext MathResult.failure(
                MathError.invalidExpression("空表达式"))

            try {
                val expr = normalize(expression, degreeMode)
                val e = Expression(expr)
                val result = e.calculate()

                when {
                    result.isNaN() -> MathResult.failure(
                        MathError.invalidExpression(e.errorMessage ?: "表达式无意义"))
                    result.isInfinite() -> MathResult.failure(MathError.infinity())
                    else -> MathResult.success(result)
                }
            } catch (ex: Exception) {
                MathResult.failure(MathError.generic(ex))
            }
        }

    private fun normalize(expr: String, degreeMode: Boolean): String {
        var s = expr.replace("×", "*").replace("÷", "/").replace("−", "-")
        if (degreeMode) {
            s = s.replace(Regex("""\bsin\("""), "sin(pi/180*")
                .replace(Regex("""\bcos\("""), "cos(pi/180*")
                .replace(Regex("""\btan\("""), "tan(pi/180*")
                .replace(Regex("""\basin\("""), "180/pi*asin(")
                .replace(Regex("""\bacos\("""), "180/pi*acos(")
                .replace(Regex("""\batan\("""), "180/pi*atan(")
        }
        return s
    }
}
