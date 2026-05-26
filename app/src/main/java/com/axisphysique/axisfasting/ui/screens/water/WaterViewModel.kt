package com.axisphysique.axisfasting.ui.screens.water

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class WaterViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("water_prefs", Context.MODE_PRIVATE)
    
    private val _waterCount = MutableStateFlow(getStoredWaterCount())
    val waterCount = _waterCount.asStateFlow()

    private fun getStoredWaterCount(): Int {
        val lastDate = prefs.getString("last_date", "")
        val today = LocalDate.now().toString()
        return if (lastDate == today) {
            prefs.getInt("count", 0)
        } else {
            0
        }
    }

    fun increment() {
        _waterCount.value++
        save()
    }

    fun decrement() {
        if (_waterCount.value > 0) {
            _waterCount.value--
            save()
        }
    }

    private fun save() {
        prefs.edit().apply {
            putString("last_date", LocalDate.now().toString())
            putInt("count", _waterCount.value)
            apply()
        }
    }
}
