package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.math.MatrixRepository
import com.example.myapplication.math.model.MathResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MatrixViewModel @Inject constructor(
    private val repository: MatrixRepository
) : ViewModel() {

    enum class Op { DET, INV, TRANS, TRACE, ADD, SUB, MUL }

    private val _result = MutableLiveData<MathResult<String>>()
    val result: LiveData<MathResult<String>> = _result

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun calculate(op: Op, a: String, b: String = "") {
        _isLoading.value = true
        viewModelScope.launch {
            val res = when (op) {
                Op.DET -> repository.parse(a).let { if (it.isSuccess) repository.determinant(it.data).let { d -> if (it.isSuccess) MathResult.success(d.data.toString()) else MathResult.failure(d.error!!) } else MathResult.failure(it.error!!) }
                Op.INV -> repository.parse(a).let { if (it.isSuccess) repository.inverse(it.data).let { i -> if (it.isSuccess) MathResult.success(repository.format(i.data)) else MathResult.failure(i.error!!) } else MathResult.failure(it.error!!) }
                Op.TRANS -> repository.parse(a).let { if (it.isSuccess) repository.transpose(it.data).let { t -> if (it.isSuccess) MathResult.success(repository.format(t.data)) else MathResult.failure(t.error!!) } else MathResult.failure(it.error!!) }
                Op.TRACE -> repository.parse(a).let { if (it.isSuccess) repository.trace(it.data).let { t -> if (it.isSuccess) MathResult.success(t.data.toString()) else MathResult.failure(t.error!!) } else MathResult.failure(it.error!!) }
                Op.ADD -> {
                    val ma = repository.parse(a)
                    val mb = repository.parse(b)
                    if (ma.isSuccess && mb.isSuccess) repository.add(ma.data, mb.data).let { if (it.isSuccess) MathResult.success(repository.format(it.data)) else MathResult.failure(it.error!!) }
                    else MathResult.failure(ma.error ?: mb.error!!)
                }
                Op.SUB -> {
                    val ma = repository.parse(a)
                    val mb = repository.parse(b)
                    if (ma.isSuccess && mb.isSuccess) repository.subtract(ma.data, mb.data).let { if (it.isSuccess) MathResult.success(repository.format(it.data)) else MathResult.failure(it.error!!) }
                    else MathResult.failure(ma.error ?: mb.error!!)
                }
                Op.MUL -> {
                    val ma = repository.parse(a)
                    val mb = repository.parse(b)
                    if (ma.isSuccess && mb.isSuccess) repository.multiply(ma.data, mb.data).let { if (it.isSuccess) MathResult.success(repository.format(it.data)) else MathResult.failure(it.error!!) }
                    else MathResult.failure(ma.error ?: mb.error!!)
                }
            }
            _result.value = res
            _isLoading.value = false
        }
    }
}
