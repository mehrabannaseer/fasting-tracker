package com.axisphysique.axisfasting.ui.screens.timer

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.axisphysique.axisfasting.AxisFastingApplication
import com.axisphysique.axisfasting.data.FastEntity
import com.axisphysique.axisfasting.model.FastingPlan
import com.axisphysique.axisfasting.worker.FastWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class TimerUiState(
    val isRunning: Boolean = false,
    val startTime: Instant? = null,
    val elapsedDuration: Duration = Duration.ZERO,
    val formattedTime: String = "00:00:00",
    val selectedPlan: FastingPlan = FastingPlan.PLAN_16_8,
    val progress: Float = 0f,
    val metabolicStage: String = "Feeding State",
    val stageDescription: String = "Your body is currently processing the nutrients from your last meal."
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as AxisFastingApplication).repository
    private val prefs = application.getSharedPreferences("fasting_prefs", Context.MODE_PRIVATE)
    private val workManager = WorkManager.getInstance(application)
    
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadSelectedPlan()
        loadTimerState()
    }

    private fun loadSelectedPlan() {
        val planName = prefs.getString("selected_plan", FastingPlan.PLAN_16_8.name)
        val plan = try {
            FastingPlan.valueOf(planName ?: FastingPlan.PLAN_16_8.name)
        } catch (e: Exception) {
            FastingPlan.PLAN_16_8
        }
        _uiState.update { it.copy(selectedPlan = plan) }
    }

    fun selectPlan(plan: FastingPlan) {
        if (!_uiState.value.isRunning) {
            prefs.edit().putString("selected_plan", plan.name).apply()
            _uiState.update { it.copy(selectedPlan = plan) }
        }
    }

    private fun loadTimerState() {
        val startTimeMillis = prefs.getLong("start_time_millis", -1L)
        if (startTimeMillis != -1L) {
            val startTime = Instant.ofEpochMilli(startTimeMillis)
            _uiState.update { 
                it.copy(
                    isRunning = true,
                    startTime = startTime
                )
            }
            startTicking()
        }
    }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            stopTimer()
        } else {
            startTimer(Instant.now())
        }
    }

    fun startTimerWithCustomTime(localDateTime: LocalDateTime) {
        val instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant()
        if (instant.isBefore(Instant.now())) {
            startTimer(instant)
        }
    }

    private fun startTimer(startTime: Instant) {
        prefs.edit().putLong("start_time_millis", startTime.toEpochMilli()).apply()
        
        _uiState.update { 
            it.copy(
                isRunning = true,
                startTime = startTime
            )
        }
        
        scheduleNotification(startTime)
        startTicking()
    }

    private fun scheduleNotification(startTime: Instant) {
        val targetDuration = _uiState.value.selectedPlan.targetDuration
        val delay = targetDuration.toMillis() - (Instant.now().toEpochMilli() - startTime.toEpochMilli())
        
        if (delay > 0) {
            val workRequest = OneTimeWorkRequestBuilder<FastWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("fast_worker")
                .build()
            
            workManager.enqueue(workRequest)
        }
    }

    private fun stopTimer() {
        val startTime = _uiState.value.startTime
        val endTime = Instant.now()
        
        timerJob?.cancel()
        workManager.cancelAllWorkByTag("fast_worker")
        prefs.edit().remove("start_time_millis").apply()

        if (startTime != null) {
            viewModelScope.launch {
                repository.insertFast(
                    FastEntity(
                        startTime = startTime.toEpochMilli(),
                        endTime = endTime.toEpochMilli(),
                        type = _uiState.value.selectedPlan.displayName
                    )
                )
            }
        }
        
        _uiState.update { 
            it.copy(
                isRunning = false,
                startTime = null,
                elapsedDuration = Duration.ZERO,
                formattedTime = "00:00:00",
                progress = 0f
            )
        }
    }

    private fun startTicking() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val start = _uiState.value.startTime
                if (start != null) {
                    val elapsed = Duration.between(start, Instant.now())
                    val target = _uiState.value.selectedPlan.targetDuration
                    val progress = (elapsed.toMillis().toFloat() / target.toMillis().toFloat()).coerceIn(0f, 1f)
                    
                    val (stage, desc) = getMetabolicStage(elapsed.toHours())
                    
                    _uiState.update { 
                        it.copy(
                            elapsedDuration = elapsed,
                            formattedTime = formatDuration(elapsed),
                            progress = progress,
                            metabolicStage = stage,
                            stageDescription = desc
                        )
                    }
                }
                delay(1000)
            }
        }
    }

    private fun getMetabolicStage(hours: Long): Pair<String, String> {
        return when {
            hours < 4 -> "Feeding State" to "Your body is processing your last meal and blood sugar levels are elevated."
            hours < 12 -> "Post-absorptive" to "Blood sugar levels begin to drop as insulin levels normalize."
            hours < 18 -> "Fat Burning" to "Glycogen stores are depleting and your body begins burning stored fat for energy."
            hours < 24 -> "Ketosis" to "Your liver produces ketones to provide energy for your brain and body."
            hours < 48 -> "Autophagy" to "Cells begin a cleanup process, removing damaged components and recycling proteins."
            hours < 72 -> "Growth Hormone Peak" to "Human Growth Hormone (HGH) levels increase significantly, preserving muscle mass."
            else -> "Immune Regeneration" to "Old immune cells are cleared out and new ones are produced, regenerating the system."
        }
    }

    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
