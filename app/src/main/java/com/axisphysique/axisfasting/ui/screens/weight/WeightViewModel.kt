package com.axisphysique.axisfasting.ui.screens.weight

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class WeightViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("weight_prefs", Context.MODE_PRIVATE)
    
    private val _weightEntries = MutableStateFlow(loadEntries())
    val weightEntries = _weightEntries.asStateFlow()

    private fun loadEntries(): List<WeightEntry> {
        val serialized = prefs.getString("entries_simple", null) ?: return emptyList()
        // Simple serialization: weight|isMetric|date;weight|isMetric|date
        return serialized.split(";").filter { it.isNotEmpty() }.map {
            val parts = it.split("|")
            WeightEntry(
                weight = parts[0].toFloat(),
                isMetric = parts[1].toBoolean(),
                date = LocalDate.parse(parts[2])
            )
        }
    }

    fun addEntry(weight: Float, isMetric: Boolean) {
        val newEntry = WeightEntry(weight, isMetric, LocalDate.now())
        val updatedList = listOf(newEntry) + _weightEntries.value
        _weightEntries.value = updatedList
        save(updatedList)
    }

    private fun save(list: List<WeightEntry>) {
        val serialized = list.joinToString(";") { 
            "${it.weight}|${it.isMetric}|${it.date}"
        }
        prefs.edit().putString("entries_simple", serialized).apply()
    }
}
