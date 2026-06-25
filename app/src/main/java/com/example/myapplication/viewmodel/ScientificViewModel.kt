package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.math.CalculationRepository
import com.example.myapplication.math.model.MathResult
import com.example.myapplication.repository.HistoryRepository
import com.example.myapplication.repository.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScientificViewModel @Inject constructor(
    private val calcRepo: CalculationRepository,
    private val historyRepo: HistoryRepository,
    private val settings: SettingsManager
) : ViewModel() {

    private val _result = MutableLiveData<MathResult<Double>>()
    val result: LiveData<MathResult<Double>> = _result

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _degreeMode = MutableLiveData(settings.degreeMode)
    val degreeMode: LiveData<Boolean> = _degreeMode

    private val _symbolicResult = MutableLiveData<String?>(null)
    val symbolicResult: LiveData<String?> = _symbolicResult

    // Observe Room history as LiveData via Flow
    val history = historyRepo.getAllFlow().asLiveData()

    fun calculate(expression: String, degreeMode: Boolean) {
        if (expression.isBlank()) return
        _isLoading.value = true
        _errorMessage.value = null
        _symbolicResult.value = null

        viewModelScope.launch {
            val res = calcRepo.evaluate(expression, degreeMode)
            _result.value = res
            if (res.isSuccess) {
                val formatted = formatResult(res)
                historyRepo.save(expression, formatted, degreeMode)
            } else {
                _errorMessage.value = res.error?.userMessage
            }
            _isLoading.value = false
        }
    }

    fun simplify(expression: String) {
        if (expression.isBlank()) return
        _symbolicResult.value = calcRepo.simplify(expression)
    }

    fun differentiate(expression: String) {
        if (expression.isBlank()) return
        _symbolicResult.value = calcRepo.differentiate(expression)
    }

    fun toggleDegreeMode() {
        val newVal = !(_degreeMode.value ?: false)
        _degreeMode.value = newVal
        settings.degreeMode = newVal
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepo.clearAll()
        }
    }

    fun formatResult(result: MathResult<Double>): String {
        val v = result.data
        return if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString()
        else "%.8g".format(v).replace(Regex("""\.?0+$"""), "") // Concise scientific format
    }
}
