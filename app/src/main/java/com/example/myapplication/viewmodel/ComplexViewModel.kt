package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.MathResult
import com.example.myapplication.repository.ComplexRepository
import kotlinx.coroutines.launch

class ComplexViewModel : ViewModel() {

    private val repository = ComplexRepository()

    private val _result = MutableLiveData<MathResult<String>>()
    val result: LiveData<MathResult<String>> = _result

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun calculate(a: String, b: String, op: String) {
        if (a.isBlank() || b.isBlank()) {
            _result.value = MathResult.failure(
                com.example.myapplication.model.MathError.invalidExpression("请输入两个复数"))
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            _result.value = repository.evaluate(a, b, op)
            _isLoading.value = false
        }
    }

    fun rectToPolar(z: String) {
        if (z.isBlank()) {
            _result.value = MathResult.failure(
                com.example.myapplication.model.MathError.invalidExpression("请输入复数"))
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            _result.value = repository.rectToPolar(z)
            _isLoading.value = false
        }
    }

    fun polarToRect(input: String) {
        if (input.isBlank()) {
            _result.value = MathResult.failure(
                com.example.myapplication.model.MathError.invalidExpression("请输入模与辐角"))
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            _result.value = repository.polarToRect(input)
            _isLoading.value = false
        }
    }

    fun conjugate(z: String) {
        if (z.isBlank()) {
            _result.value = MathResult.failure(
                com.example.myapplication.model.MathError.invalidExpression("请输入复数"))
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            _result.value = repository.conjugate(z)
            _isLoading.value = false
        }
    }

    fun argument(z: String) {
        if (z.isBlank()) {
            _result.value = MathResult.failure(
                com.example.myapplication.model.MathError.invalidExpression("请输入复数"))
            return
        }
        _isLoading.value = true
        viewModelScope.launch {
            _result.value = repository.argument(z)
            _isLoading.value = false
        }
    }
}
