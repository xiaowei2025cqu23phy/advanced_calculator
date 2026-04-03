package com.example.myapplication.repository

import com.example.myapplication.model.MathError
import com.example.myapplication.model.MathResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mariuszgromada.math.mxparser.Expression

/**
 * Optimized calculation repository.
 */
class CalculationRepository {

    suspend fun evaluate(expression: String, degreeMode: Boolean): MathResult<Double> =
        withContext(Dispatchers.Default) {
            if (expression.isBlank()) return@withContext MathResult.failure(
                MathError.invalidExpression("输入为空"))

            try {
                val normalizedExpr = normalize(expression, degreeMode)
                val e = Expression(normalizedExpr)
                
                // Add common constants if needed
                // e.addArguments(...) 
                
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

    private fun normalize(expr: String, degreeMode: Boolean): String {
        // Basic symbol mapping
        var s = expr.replace("×", "*")
                    .replace("÷", "/")
                    .replace("−", "-")
                    .replace("π", "pi")
                    .replace("√", "sqrt")

        if (degreeMode) {
            // More robust degree conversion: only replace function calls with (
            // Using a word boundary to avoid replacing things like "asin" when we want "sin"
            val functions = listOf("sin", "cos", "tan", "sec", "csc", "cot")
            functions.forEach { func ->
                s = s.replace(Regex("""\b$func\s*\("""), "$func(pi/180*")
            }
            
            // Inverse functions: result is in radians, so convert back to degrees
            val invFunctions = listOf("asin", "acos", "atan", "asec", "acsc", "acot")
            invFunctions.forEach { func ->
                s = s.replace(Regex("""\b$func\s*\("""), "180/pi*$func(")
            }
        }
        return s
    }
}
