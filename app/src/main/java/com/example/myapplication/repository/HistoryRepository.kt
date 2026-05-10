package com.example.myapplication.repository

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple JSON-based history persistence.
 */
class HistoryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("calc_history", Context.MODE_PRIVATE)

    data class Entry(
        val expression: String,
        val result: String,
        val degreeMode: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun getAll(): List<Entry> {
        val json = prefs.getString("entries", "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = ArrayList<Entry>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Entry(
                expression = obj.optString("expr", ""),
                result = obj.optString("result", ""),
                degreeMode = obj.optBoolean("deg", false),
                timestamp = obj.optLong("ts", 0)
            ))
        }
        return list
    }

    fun save(expression: String, result: String, degreeMode: Boolean) {
        if (expression.isBlank()) return
        val list = getAll().toMutableList()
        list.add(0, Entry(expression, result, degreeMode)) // newest first
        if (list.size > 100) list.removeAt(list.size - 1) // cap at 100
        saveList(list)
    }

    fun clearAll() {
        prefs.edit().remove("entries").apply()
    }

    private fun saveList(list: List<Entry>) {
        val arr = JSONArray()
        for (e in list) {
            arr.put(JSONObject().apply {
                put("expr", e.expression)
                put("result", e.result)
                put("deg", e.degreeMode)
                put("ts", e.timestamp)
            })
        }
        prefs.edit().putString("entries", arr.toString()).apply()
    }
}
