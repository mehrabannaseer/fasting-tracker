package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.FastingLog
import com.example.data.model.WaterLog
import com.example.data.repository.TrackerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class TrackerViewModel(private val repository: TrackerRepository) : ViewModel() {

    // Target fasting hours selection (e.g., 16 hours default)
    private val _selectedTargetHours = MutableStateFlow(16)
    val selectedTargetHours: StateFlow<Int> = _selectedTargetHours.asStateFlow()

    // Water goal selection (e.g., 2000 ml default)
    private val _waterGoalMl = MutableStateFlow(2000)
    val waterGoalMl: StateFlow<Int> = _waterGoalMl.asStateFlow()

    // Active fasting log flow
    val activeFastingLog: StateFlow<FastingLog?> = repository.activeFastingLog
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Completed fasting logs (endTime is not null)
    val fastingHistory: StateFlow<List<FastingLog>> = repository.allFastingLogs
        .map { logs -> logs.filter { it.endTime != null } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All water logs history
    val waterHistory: StateFlow<List<WaterLog>> = repository.allWaterLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current fasting elapsed/completed duration ticker
    private val _fastingDurationMillis = MutableStateFlow(0L)
    val fastingDurationMillis: StateFlow<Long> = _fastingDurationMillis.asStateFlow()

    private var tickerJob: Job? = null

    // Water logs tracking for today
    private val _todayStartEndEpochs = MutableStateFlow(getTodayTimeRange())
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayWaterLogs: StateFlow<List<WaterLog>> = _todayStartEndEpochs
        .flatMapLatest { range ->
            repository.getWaterLogsBetween(range.first, range.second)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayWaterTotal: StateFlow<Int> = todayWaterLogs
        .map { logs -> logs.sumOf { it.amountMl } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    init {
        // Start watching the active fasting log to run the ticker
        viewModelScope.launch {
            activeFastingLog.collect { log ->
                if (log != null) {
                    startFastingTicker(log.startTime)
                } else {
                    stopFastingTicker()
                }
            }
        }
    }

    // Refresh today's date range (useful if the app remains open past midnight)
    fun refreshTodayTimeRange() {
        _todayStartEndEpochs.value = getTodayTimeRange()
    }

    private fun startFastingTicker(startTime: Long) {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                _fastingDurationMillis.value = System.currentTimeMillis() - startTime
                delay(1000)
            }
        }
    }

    private fun stopFastingTicker() {
        tickerJob?.cancel()
        tickerJob = null
        _fastingDurationMillis.value = 0L
    }

    // --- Fasting Actions ---

    fun startFasting(targetHours: Int = _selectedTargetHours.value, customStartTime: Long? = null) {
        viewModelScope.launch {
            // First stop any active fast (if somehow already running)
            val currentActive = activeFastingLog.value
            if (currentActive != null) {
                repository.updateFastingLog(currentActive.copy(endTime = customStartTime ?: System.currentTimeMillis()))
            }
            // Start a new fasting log
            val startTimeToUse = customStartTime ?: System.currentTimeMillis()
            val newLog = FastingLog(
                startTime = startTimeToUse,
                targetHours = targetHours
            )
            repository.insertFastingLog(newLog)
            _selectedTargetHours.value = targetHours
        }
    }

    fun endFasting(notes: String = "") {
        viewModelScope.launch {
            activeFastingLog.value?.let { activeLog ->
                val updatedLog = activeLog.copy(
                    endTime = System.currentTimeMillis(),
                    notes = notes
                )
                repository.updateFastingLog(updatedLog)
            }
        }
    }

    fun cancelFasting() {
        viewModelScope.launch {
            activeFastingLog.value?.let { activeLog ->
                repository.deleteFastingLog(activeLog)
            }
        }
    }

    fun deleteFastingLog(log: FastingLog) {
        viewModelScope.launch {
            repository.deleteFastingLog(log)
        }
    }

    fun updateSelectedTargetHours(hours: Int) {
        _selectedTargetHours.value = hours
    }

    // --- Water Actions ---

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            val log = WaterLog(amountMl = amountMl)
            repository.insertWaterLog(log)
            // Refresh range just to be safe
            refreshTodayTimeRange()
        }
    }

    fun deleteWaterLog(log: WaterLog) {
        viewModelScope.launch {
            repository.deleteWaterLog(log)
        }
    }

    fun updateWaterGoal(goalMl: Int) {
        if (goalMl > 0) {
            _waterGoalMl.value = goalMl
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllLogs()
            refreshTodayTimeRange()
        }
    }

    // --- Helper for Day Boundaries ---

    private fun getTodayTimeRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        
        // Start of today (00:00:00.000)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        // End of today (23:59:59.999)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfToday = calendar.timeInMillis

        return Pair(startOfToday, endOfToday)
    }
}

class TrackerViewModelFactory(private val repository: TrackerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
