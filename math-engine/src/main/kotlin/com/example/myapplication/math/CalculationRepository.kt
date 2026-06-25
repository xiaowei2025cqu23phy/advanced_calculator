package com.example.myapplication.math

import com.example.myapplication.math.model.MathError
import com.example.myapplication.math.model.MathResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mariuszgromada.math.mxparser.Expression
import org.matheclipse.core.eval.ExprEvaluator

/**
 * Optimized calculation repository with Symbolic Computation support.
 */
class CalculationRepository {

    private val symja = ExprEvaluator()

    suspend fun evaluate(expression: String, degreeMode: Boolean): MathResult<Double> =
        withContext(Dispatchers.Default) {
            if (expression.isBlank()) return@withContext MathResult.failure(
                MathError.invalidExpression("输入为空"))

            try {
                val normalizedExpr = normalize(expression, degreeMode)
                val e = Expression(normalizedExpr)
                val result = e.calculate()

                if (e.checkSyntax()) {
                    when {
                        result.isNaN() -> MathResult.failure(
                            MathError.invalidExpression(e.errorMessage.takeIf { it.isNotBlank() } ?: "计算无意义"))
                        result.isInfinite() -> MathResult.failure(MathError.infinity())
                        else -> MathResult.success(result)
                    }
                } else {
                    MathResult.failure(MathError.invalidExpression(e.errorMessage))
                }
            } catch (ex: Exception) {
                MathResult.failure(MathError.generic(ex))
            }
        }

    fun simplify(expression: String): String {
        return try {
            symja.evaluate("Simplify($expression)").toString()
        } catch (e: Exception) {
            "无法简化"
        }
    }

    fun differentiate(expression: String, variable: String = "x"): String {
        return try {
            symja.evaluate("D($expression, $variable)").toString()
        } catch (e: Exception) {
            "无法求导"
        }
    }

    private fun normalize(expr: String, degreeMode: Boolean): String {
        var s = expr.replace("×", "*")
                    .replace("÷", "/")
                    .replace("−", "-")
                    .replace("π", "pi")
                    .replace("√", "sqrt")

        if (degreeMode) {
            val functions = listOf("sin", "cos", "tan", "sec", "csc", "cot")
            functions.forEach { func ->
                s = s.replace(Regex("""\b$func\s*\("""), "$func(pi/180*")
            }
            val invFunctions = listOf("asin", "acos", "atan", "asec", "acsc", "acot")
            invFunctions.forEach { func ->
                s = s.replace(Regex("""\b$func\s*\("""), "180/pi*$func(")
            }
        }
        return s
    }
}
