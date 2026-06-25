package com.example.myapplication.repository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnitRepository @Inject constructor() {

    enum class Category { LENGTH, MASS, TEMPERATURE, TIME }

    data class Unit(val name: String, val factor: Double, val offset: Double = 0.0)

    private val units = mapOf(
        Category.LENGTH to listOf(
            Unit("米 (m)", 1.0),
            Unit("千米 (km)", 1000.0),
            Unit("厘米 (cm)", 0.01),
            Unit("毫米 (mm)", 0.001),
            Unit("英寸 (in)", 0.0254),
            Unit("英尺 (ft)", 0.3048)
        ),
        Category.MASS to listOf(
            Unit("千克 (kg)", 1.0),
            Unit("克 (g)", 0.001),
            Unit("毫克 (mg)", 0.000001),
            Unit("磅 (lb)", 0.453592)
        ),
        Category.TEMPERATURE to listOf(
            Unit("摄氏度 (°C)", 1.0),
            Unit("华氏度 (°F)", 0.5555555555555556, -32.0),
            Unit("开尔文 (K)", 1.0, -273.15)
        )
    )

    fun getCategories() = units.keys.toList()

    fun getUnits(category: Category) = units[category] ?: emptyList()

    fun convert(value: Double, from: Unit, to: Unit): Double {
        val baseValue = (value + from.offset) * from.factor
        return (baseValue / to.factor) - to.offset
    }
}
