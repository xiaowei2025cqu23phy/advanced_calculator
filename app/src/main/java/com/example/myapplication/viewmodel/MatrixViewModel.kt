package com.example.myapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.MathResult
import com.example.myapplication.repository.MatrixRepository
import kotlinx.coroutines.launch

class MatrixViewModel : ViewModel() {

    private val repository = MatrixRepository()

    private val _result = MutableLiveData<MathResult<String>>()
    val result: LiveData<MathResult<String>> = _result

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    enum class Op { DET, INV, TRANS, TRACE, ADD, SUB, MUL }

    fun calculate(op: Op, inputA: String, inputB: String = "") {
        if (inputA.isBlank()) {
            _result.value = MathResult.failure(
                com.example.myapplication.model.MathError.invalidExpression("请输入矩阵A"))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val parseResult = repository.parse(inputA)
            if (!parseResult.isSuccess) {
                _result.value = MathResult.failure(parseResult.error!!)
                _isLoading.value = false
                return@launch
            }
            val mA = parseResult.data!!

            val res = when (op) {
                Op.DET -> repository.determinant(mA).let { r ->
                    if (r.isSuccess) MathResult.success("Det = ${r.data}")
                    else MathResult.failure(r.error!!)
                }
                Op.INV -> repository.inverse(mA).let { r ->
                    if (r.isSuccess) MathResult.success("Inverse:\n${repository.format(r.data)}")
                    else MathResult.failure(r.error!!)
                }
                Op.TRANS -> repository.transpose(mA).let { r ->
                    MathResult.success("Transpose:\n${repository.format(r.data)}")
                }
                Op.TRACE -> repository.trace(mA).let { r ->
                    if (r.isSuccess) MathResult.success("Trace = ${r.data}")
                    else MathResult.failure(r.error!!)
                }
                Op.ADD, Op.SUB, Op.MUL -> {
                    if (inputB.isBlank()) {
                        MathResult.failure(
                            com.example.myapplication.model.MathError.invalidExpression("请输入矩阵B"))
                    } else {
                        val parseB = repository.parse(inputB)
                        if (!parseB.isSuccess) {
                            MathResult.failure(parseB.error!!)
                        } else {
                            val mB = parseB.data!!
                            val binRes = when (op) {
                                Op.ADD -> repository.add(mA, mB)
                                Op.SUB -> repository.subtract(mA, mB)
                                Op.MUL -> repository.multiply(mA, mB)
                                else -> return@launch
                            }
                            if (binRes.isSuccess) {
                                val label = when (op) {
                                    Op.ADD -> "A+B"; Op.SUB -> "A-B"; else -> "A×B"
                                }
                                MathResult.success("$label:\n${repository.format(binRes.data)}")
                            } else MathResult.failure(binRes.error!!)
                        }
                    }
                }
            }
            _result.value = res
            _isLoading.value = false
        }
    }
}
