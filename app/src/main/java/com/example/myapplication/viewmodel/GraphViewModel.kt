package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.math.PlotEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GraphViewModel @Inject constructor() : ViewModel() {

    private val _curves = MutableLiveData<List<CurveResult>>(emptyList())
    val curves: LiveData<List<CurveResult>> = _curves

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun generateCartesian(inputs: List<CartesianInput>, xMin: Double, xMax: Double, step: Double) {
        _isLoading.value = true
        viewModelScope.launch {
            val results = withContext(Dispatchers.Default) {
                inputs.map { input ->
                    val pts = PlotEngine.evalCartesian(input.expr, xMin, xMax, step)
                    CurveResult(pts, input.color, input.label)
                }
            }
            _curves.value = results
            _isLoading.value = false
        }
    }

    fun generateParametric(inputs: List<ParametricInput>, tMin: Double, tMax: Double, tStep: Double) {
        _isLoading.value = true
        viewModelScope.launch {
            val results = withContext(Dispatchers.Default) {
                inputs.map { input ->
                    val pts = PlotEngine.evalParametric(input.xExpr, input.yExpr, tMin, tMax, tStep)
                    CurveResult(pts, input.color, input.label)
                }
            }
            _curves.value = results
            _isLoading.value = false
        }
    }

    fun generatePolar(inputs: List<PolarInput>, tMin: Double, tMax: Double, tStep: Double) {
        _isLoading.value = true
        viewModelScope.launch {
            val results = withContext(Dispatchers.Default) {
                inputs.map { input ->
                    val pts = PlotEngine.evalPolar(input.expr, tMin, tMax, tStep)
                    CurveResult(pts, input.color, input.label)
                }
            }
            _curves.value = results
            _isLoading.value = false
        }
    }

    fun clear() {
        _curves.value = emptyList()
    }

    data class CartesianInput(val expr: String, val color: Int, val label: String)
    data class ParametricInput(val xExpr: String, val yExpr: String, val color: Int, val label: String)
    data class PolarInput(val expr: String, val color: Int, val label: String)
    data class CurveResult(val points: List<FloatArray>, val color: Int, val label: String)
}
