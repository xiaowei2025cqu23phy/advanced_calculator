package com.example.myapplication.model;

/**
 * Unified result type for all mathematical operations.
 * Eliminates scattered try-catch(Exception) throughout fragments.
 */
public class MathResult<T> {

    private final T data;
    private final MathError error;

    private MathResult(T data, MathError error) {
        this.data = data;
        this.error = error;
    }

    public boolean isSuccess() { return error == null; }
    public T getData() { return data; }
    public MathError getError() { return error; }

    public static <T> MathResult<T> success(T data) {
        return new MathResult<>(data, null);
    }

    public static <T> MathResult<T> failure(MathError error) {
        return new MathResult<>(null, error);
    }

    public T orElse(T fallback) {
        return isSuccess() ? data : fallback;
    }
}
