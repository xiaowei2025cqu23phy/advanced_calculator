package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import com.example.myapplication.repository.UnitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class UnitViewModel @Inject constructor(
    private val repository: UnitRepository
) : ViewModel() {

    private val _categories = MutableStateFlow(repository.getCategories())
    val categories: StateFlow<List<UnitRepository.Category>> = _categories

    private val _selectedCategory = MutableStateFlow(UnitRepository.Category.LENGTH)
    val selectedCategory: StateFlow<UnitRepository.Category> = _selectedCategory

    private val _units = MutableStateFlow(repository.getUnits(UnitRepository.Category.LENGTH))
    val units: StateFlow<List<UnitRepository.Unit>> = _units

    private val _result = MutableStateFlow(0.0)
    val result: StateFlow<Double> = _result

    fun selectCategory(category: UnitRepository.Category) {
        _selectedCategory.value = category
        _units.value = repository.getUnits(category)
    }

    fun convert(value: Double, from: UnitRepository.Unit, to: UnitRepository.Unit) {
        _result.value = repository.convert(value, from, to)
    }
}
