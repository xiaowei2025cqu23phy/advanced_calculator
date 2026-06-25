package com.example.myapplication.model

/**
 * Unified result type for all mathematical operations.
 * Eliminates scattered try-catch(Exception) throughout fragments.
 */
class MathResult<T> private constructor(
    private val dataInternal: T?,
    val error: MathError?
) {
    val isSuccess: Boolean
        get() = error == null

    val data: T
        get() = dataInternal ?: throw IllegalStateException("Cannot access data of a failed MathResult")

    companion object {
        fun <T> success(data: T): MathResult<T> {
            return MathResult(data, null)
        }

        fun <T> failure(error: MathError): MathResult<T> {
            return MathResult(null, error)
        }
    }

    fun orElse(fallback: T): T {
        return if (isSuccess) data else fallback
    }
}
