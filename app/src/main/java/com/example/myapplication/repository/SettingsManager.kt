package com.example.myapplication.repository

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var degreeMode: Boolean
        get() = prefs.getBoolean("degree_mode", false)
        set(value) = prefs.edit().putBoolean("degree_mode", value).apply()

    var lastExpression: String
        get() = prefs.getString("last_expression", "") ?: ""
        set(value) = prefs.edit().putString("last_expression", value).apply()
}
