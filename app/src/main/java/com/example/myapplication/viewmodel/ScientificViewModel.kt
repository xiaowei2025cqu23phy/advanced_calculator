package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.MathResult
import com.example.myapplication.repository.CalculationRepository
import com.example.myapplication.repository.HistoryRepository
import kotlinx.coroutines.launch

class ScientificViewModel(application: Application) : AndroidViewModel(application) {

    private val calcRepo = CalculationRepository()
    private val historyRepo = HistoryRepository(application)

    private val _result = MutableLiveData<MathResult<Double>>()
    val result: LiveData<MathResult<Double>> = _result

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _history = MutableLiveData<List<HistoryRepository.Entry>>()
    val history: LiveData<List<HistoryRepository.Entry>> = _history

    init {
        refreshHistory()
    }

    private fun refreshHistory() {
        _history.value = historyRepo.getAll()
    }

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
                refreshHistory()
            } else {
                _errorMessage.value = res.error?.userMessage
            }
            _isLoading.value = false
        }
    }

    fun clearHistory() {
        historyRepo.clearAll()
        refreshHistory()
    }

    fun formatResult(result: MathResult<Double>): String {
        if (!result.isSuccess) return ""
        val v = result.data
        return if (v == Math.floor(v) && !v.isInfinite()) v.toLong().toString()
        else v.toString()
    }
}
