package com.axisphysique.axisfasting.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axisphysique.axisfasting.AxisFastingApplication
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as AxisFastingApplication).repository
    
    fun resetProgress() {
        viewModelScope.launch {
            repository.deleteAllFasts()
            
            // Also reset water and weight prefs
            getApplication<Application>().getSharedPreferences("water_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            getApplication<Application>().getSharedPreferences("weight_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        }
    }
}
