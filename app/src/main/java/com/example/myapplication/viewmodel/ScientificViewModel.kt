package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.MathResult
import com.example.myapplication.repository.CalculationRepository
import com.example.myapplication.repository.HistoryRepository
import com.example.myapplication.repository.SettingsManager
import kotlinx.coroutines.launch

class ScientificViewModel(application: Application) : AndroidViewModel(application) {

    private val calcRepo = CalculationRepository()
    private val historyRepo = HistoryRepository(application)
    private val settings = SettingsManager(application)

    private val _result = MutableLiveData<MathResult<Double>>()
    val result: LiveData<MathResult<Double>> = _result

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _degreeMode = MutableLiveData(settings.degreeMode)
    val degreeMode: LiveData<Boolean> = _degreeMode

    // Observe Room history as LiveData via Flow
    val history = historyRepo.getAllFlow().asLiveData()

    fun calculate(expression: String, degreeMode: Boolean) {
        if (expression.isBlank()) return
        _isLoading.value = true
        _errorMessage.value = null

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
        val v = result.data ?: return ""
        return if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString()
        else "%.8g".format(v).replace(Regex("""\.?0+$"""), "") // Concise scientific format
    }
}
