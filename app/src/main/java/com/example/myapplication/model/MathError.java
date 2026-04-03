package com.example.myapplication.model;

/**
 * Typed mathematical error with user-facing message.
 */
public class MathError {

    private final ErrorType type;
    private final String message;
    private final String userMessage;
    private final boolean recoverable;

    public MathError(ErrorType type, String message, String userMessage, boolean recoverable) {
        this.type = type;
        this.message = message;
        this.userMessage = userMessage;
        this.recoverable = recoverable;
    }

    public ErrorType getType() { return type; }
    public String getMessage() { return message; }
    public String getUserMessage() { return userMessage; }
    public boolean isRecoverable() { return recoverable; }

    public static MathError divideByZero() {
        return new MathError(ErrorType.DIVISION_BY_ZERO, "Division by zero",
                "除数不能为零", false);
    }

    public static MathError singularMatrix() {
        return new MathError(ErrorType.SINGULAR_MATRIX, "Singular matrix",
                "矩阵奇异，无法求逆（行列式为0）", false);
    }

    public static MathError invalidExpression(String detail) {
        return new MathError(ErrorType.INVALID_EXPRESSION, "Invalid expression: " + detail,
                "表达式无效: " + detail, true);
    }

    public static MathError domainError() {
        return new MathError(ErrorType.DOMAIN_ERROR, "Domain error",
                "超出定义域（如负数开平方）", true);
    }

    public static MathError dimensionMismatch(int rowsA, int colsA, int rowsB, int colsB) {
        return new MathError(ErrorType.DIMENSION_MISMATCH,
                "Dimension mismatch: " + rowsA + "x" + colsA + " vs " + rowsB + "x" + colsB,
                "矩阵维度不匹配: " + rowsA + "x" + colsA + " 与 " + rowsB + "x" + colsB + " 无法相乘", false);
    }

    public static MathError notSquareMatrix() {
        return new MathError(ErrorType.NOT_SQUARE_MATRIX, "Not a square matrix",
                "矩阵不是方阵，无法进行此运算", false);
    }

    public static MathError infinity() {
        return new MathError(ErrorType.INFINITY, "Result is infinity",
                "结果为无穷大", true);
    }

    public static MathError generic(Throwable t) {
        return new MathError(ErrorType.UNKNOWN, t.getMessage() != null ? t.getMessage() : "Unknown error",
                "计算错误: " + (t.getMessage() != null ? t.getMessage() : "未知错误"), false);
    }
}
