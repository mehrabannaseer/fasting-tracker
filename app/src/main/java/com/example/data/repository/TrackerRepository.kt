package com.example.data.repository

import com.example.data.dao.TrackerDao
import com.example.data.model.FastingLog
import com.example.data.model.WaterLog
import kotlinx.coroutines.flow.Flow

class TrackerRepository(private val trackerDao: TrackerDao) {

    val allFastingLogs: Flow<List<FastingLog>> = trackerDao.getAllFastingLogs()
    val activeFastingLog: Flow<FastingLog?> = trackerDao.getActiveFastingLog()
    val allWaterLogs: Flow<List<WaterLog>> = trackerDao.getAllWaterLogs()

    fun getWaterLogsBetween(startTimeMillis: Long, endTimeMillis: Long): Flow<List<WaterLog>> {
        return trackerDao.getWaterLogsBetween(startTimeMillis, endTimeMillis)
    }

    suspend fun insertFastingLog(fastingLog: FastingLog): Long {
        return trackerDao.insertFastingLog(fastingLog)
    }

    suspend fun updateFastingLog(fastingLog: FastingLog) {
        trackerDao.updateFastingLog(fastingLog)
    }

    suspend fun deleteFastingLog(fastingLog: FastingLog) {
        trackerDao.deleteFastingLog(fastingLog)
    }

    suspend fun deleteFastingLogById(id: Int) {
        trackerDao.deleteFastingLogById(id)
    }

    suspend fun insertWaterLog(waterLog: WaterLog): Long {
        return trackerDao.insertWaterLog(waterLog)
    }

    suspend fun deleteWaterLog(waterLog: WaterLog) {
        trackerDao.deleteWaterLog(waterLog)
    }

    suspend fun deleteWaterLogById(id: Int) {
        trackerDao.deleteWaterLogById(id)
    }

    suspend fun clearAllLogs() {
        trackerDao.clearAllFastingLogs()
        trackerDao.clearAllWaterLogs()
    }
}
