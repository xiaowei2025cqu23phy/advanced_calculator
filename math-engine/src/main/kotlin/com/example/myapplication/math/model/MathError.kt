package com.example.myapplication.math.model

/**
 * Typed mathematical error with user-facing message.
 */
class MathError(
    val type: ErrorType,
    val message: String,
    val userMessage: String,
    val isRecoverable: Boolean
) {
    companion object {
        fun divideByZero(): MathError {
            return MathError(ErrorType.DIVISION_BY_ZERO, "Division by zero", "除数不能为零", false)
        }

        fun singularMatrix(): MathError {
            return MathError(ErrorType.SINGULAR_MATRIX, "Singular matrix", "矩阵奇异，无法求逆（行列式为0）", false)
        }

        fun invalidExpression(detail: String): MathError {
            return MathError(
                ErrorType.INVALID_EXPRESSION,
                "Invalid expression: $detail",
                "表达式无效: $detail",
                true
            )
        }

        fun domainError(): MathError {
            return MathError(ErrorType.DOMAIN_ERROR, "Domain error", "超出定义域（如负数开平方）", true)
        }

        fun dimensionMismatch(rowsA: Int, colsA: Int, rowsB: Int, colsB: Int): MathError {
            return MathError(
                ErrorType.DIMENSION_MISMATCH,
                "Dimension mismatch: ${rowsA}x${colsA} vs ${rowsB}x${colsB}",
                "矩阵维度不匹配: ${rowsA}x${colsA} 与 ${rowsB}x${colsB} 无法相乘",
                false
            )
        }

        fun notSquareMatrix(): MathError {
            return MathError(ErrorType.NOT_SQUARE_MATRIX, "Not a square matrix", "矩阵不是方阵，无法进行此运算", false)
        }

        fun infinity(): MathError {
            return MathError(ErrorType.INFINITY, "Result is infinity", "结果为无穷大", true)
        }

        fun generic(t: Throwable): MathError {
            return MathError(
                ErrorType.UNKNOWN,
                t.message ?: "Unknown error",
                "计算错误: ${t.message ?: "未知错误"}",
                false
            )
        }
    }
}
