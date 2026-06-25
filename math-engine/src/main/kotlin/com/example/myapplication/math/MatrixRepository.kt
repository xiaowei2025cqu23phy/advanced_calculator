package com.example.myapplication.math

import com.example.myapplication.math.model.MathError
import com.example.myapplication.math.model.MathResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ejml.simple.SimpleMatrix

class MatrixRepository {

    suspend fun parse(input: String): MathResult<SimpleMatrix> = withContext(Dispatchers.Default) {
        try {
            MathResult.success(doParse(input))
        } catch (e: NumberFormatException) {
            MathResult.failure(MathError.invalidExpression("矩阵元素格式错误: ${e.message}"))
        } catch (e: Exception) {
            MathResult.failure(MathError.generic(e))
        }
    }

    suspend fun determinant(m: SimpleMatrix): MathResult<Double> = withContext(Dispatchers.Default) {
        if (m.numRows() != m.numCols()) MathResult.failure(MathError.notSquareMatrix())
        else MathResult.success(m.determinant())
    }

    suspend fun inverse(m: SimpleMatrix): MathResult<SimpleMatrix> = withContext(Dispatchers.Default) {
        if (m.numRows() != m.numCols()) MathResult.failure(MathError.notSquareMatrix())
        else if (m.determinant() == 0.0) MathResult.failure(MathError.singularMatrix())
        else MathResult.success(m.invert())
    }

    suspend fun transpose(m: SimpleMatrix): MathResult<SimpleMatrix> = withContext(Dispatchers.Default) {
        MathResult.success(m.transpose())
    }

    suspend fun trace(m: SimpleMatrix): MathResult<Double> = withContext(Dispatchers.Default) {
        if (m.numRows() != m.numCols()) MathResult.failure(MathError.notSquareMatrix())
        else MathResult.success(m.trace())
    }

    suspend fun add(a: SimpleMatrix, b: SimpleMatrix): MathResult<SimpleMatrix> = withContext(Dispatchers.Default) {
        if (a.numRows() != b.numRows() || a.numCols() != b.numCols()) {
            MathResult.failure(MathError.dimensionMismatch(a.numRows(), a.numCols(), b.numRows(), b.numCols()))
        } else MathResult.success(a.plus(b))
    }

    suspend fun subtract(a: SimpleMatrix, b: SimpleMatrix): MathResult<SimpleMatrix> = withContext(Dispatchers.Default) {
        if (a.numRows() != b.numRows() || a.numCols() != b.numCols()) {
            MathResult.failure(MathError.dimensionMismatch(a.numRows(), a.numCols(), b.numRows(), b.numCols()))
        } else MathResult.success(a.minus(b))
    }

    suspend fun multiply(a: SimpleMatrix, b: SimpleMatrix): MathResult<SimpleMatrix> = withContext(Dispatchers.Default) {
        if (a.numCols() != b.numRows()) {
            MathResult.failure(MathError.dimensionMismatch(a.numRows(), a.numCols(), b.numRows(), b.numCols()))
        } else MathResult.success(a.mult(b))
    }

    fun format(m: SimpleMatrix): String {
        val sb = StringBuilder()
        for (i in 0 until m.numRows()) {
            for (j in 0 until m.numCols()) {
                sb.append(fmt(m[i, j]))
                if (j < m.numCols() - 1) sb.append("  ")
            }
            if (i < m.numRows() - 1) sb.append("\n")
        }
        return sb.toString()
    }

    private fun fmt(d: Double) =
        if (d == Math.floor(d) && !d.isInfinite()) d.toLong().toString()
        else String.format("%.4f", d)

    private fun doParse(input: String): SimpleMatrix {
        val rows = input.split(",")
        var data: Array<DoubleArray>? = null
        for (i in rows.indices) {
            val cols = rows[i].trim().split("\\s+".toRegex())
            if (data == null) data = Array(rows.size) { DoubleArray(cols.size) }
            for (j in cols.indices) data!![i][j] = cols[j].toDouble()
        }
        return SimpleMatrix(data)
    }
}
