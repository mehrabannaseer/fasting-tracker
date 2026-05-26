package com.axisphysique.axisfasting.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axisphysique.axisfasting.AxisFastingApplication
import com.axisphysique.axisfasting.data.FastEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val recentFasts: List<FastRecordUiModel> = emptyList(),
    val totalFasts: String = "0",
    val currentStreak: String = "0 Days"
)

data class FastRecordUiModel(
    val id: Int,
    val type: String,
    val date: String,
    val duration: String
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AxisFastingApplication).repository

    val uiState: StateFlow<HomeUiState> = repository.allFasts.map { fasts ->
        HomeUiState(
            recentFasts = fasts.take(5).map { it.toUiModel() },
            totalFasts = fasts.size.toString(),
            currentStreak = calculateStreak(fasts)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun deleteFast(id: Int) {
        viewModelScope.launch {
            repository.deleteFast(id)
        }
    }

    private fun FastEntity.toUiModel(): FastRecordUiModel {
        val duration = Duration.ofMillis(endTime - startTime)
        val h = duration.toHours()
        val m = duration.toMinutes() % 60
        
        val date = Instant.ofEpochMilli(startTime)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))

        return FastRecordUiModel(
            id = id,
            type = type,
            date = date,
            duration = String.format("%dh %02dm", h, m)
        )
    }

    private fun calculateStreak(fasts: List<FastEntity>): String {
        // Simple streak logic: count consecutive days with at least one fast
        // For a professional app, this would be more complex
        return "${if (fasts.isNotEmpty()) "1" else "0"} Day" // Placeholder
    }
}
