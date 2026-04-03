package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.PlotEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GraphViewModel : ViewModel() {

    private val _curves = MutableLiveData<List<CurveResult>>()
    val curves: LiveData<List<CurveResult>> = _curves

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Generate points for cartesian functions: [expr, min, max, step] */
    fun generateCartesian(functions: List<CartesianInput>, xMin: Double, xMax: Double, step: Double) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val results = withContext(Dispatchers.Default) {
                functions.mapNotNull { input ->
                    val pts = PlotEngine.evalCartesian(input.expr, xMin, xMax, step)
                    if (pts.isNotEmpty()) CurveResult(pts, input.color, input.label)
                    else null
                }
            }
            if (results.isEmpty()) _error.value = "无法绘制，请检查表达式"
            _curves.value = results
            _isLoading.value = false
        }
    }

    /** Generate points for parametric functions: [xExpr, yExpr, min, max, step] */
    fun generateParametric(inputs: List<ParametricInput>, tMin: Double, tMax: Double, tStep: Double) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val results = withContext(Dispatchers.Default) {
                inputs.mapNotNull { input ->
                    val pts = PlotEngine.evalParametric(input.xExpr, input.yExpr, tMin, tMax, tStep)
                    if (pts.isNotEmpty()) CurveResult(pts, input.color, input.label)
                    else null
                }
            }
            if (results.isEmpty()) _error.value = "无法绘制，请检查参数方程"
            _curves.value = results
            _isLoading.value = false
        }
    }

    /** Generate points for polar functions: [rExpr, min, max, step] */
    fun generatePolar(functions: List<PolarInput>, tMin: Double, tMax: Double, tStep: Double) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            val results = withContext(Dispatchers.Default) {
                functions.mapNotNull { input ->
                    val pts = PlotEngine.evalPolar(input.rExpr, tMin, tMax, tStep)
                    if (pts.isNotEmpty()) CurveResult(pts, input.color, input.label)
                    else null
                }
            }
            if (results.isEmpty()) _error.value = "无法绘制，请检查表达式"
            _curves.value = results
            _isLoading.value = false
        }
    }

    fun clear() {
        _curves.value = emptyList()
        _error.value = null
    }

    // ── Data classes ──

    data class CurveResult(val points: List<FloatArray>, val color: Int, val label: String)
    data class CartesianInput(val expr: String, val color: Int, val label: String)
    data class ParametricInput(val xExpr: String, val yExpr: String, val color: Int, val label: String)
    data class PolarInput(val rExpr: String, val color: Int, val label: String)
}
