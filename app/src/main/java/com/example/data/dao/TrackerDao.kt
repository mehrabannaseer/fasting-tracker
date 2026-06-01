package com.example.data.dao

import androidx.room.*
import com.example.data.model.FastingLog
import com.example.data.model.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {

    // --- Fasting Logs ---

    @Query("SELECT * FROM fasting_logs ORDER BY startTime DESC")
    fun getAllFastingLogs(): Flow<List<FastingLog>>

    @Query("SELECT * FROM fasting_logs WHERE endTime IS NULL LIMIT 1")
    fun getActiveFastingLog(): Flow<FastingLog?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFastingLog(fastingLog: FastingLog): Long

    @Update
    suspend fun updateFastingLog(fastingLog: FastingLog)

    @Delete
    suspend fun deleteFastingLog(fastingLog: FastingLog)

    @Query("DELETE FROM fasting_logs WHERE id = :id")
    suspend fun deleteFastingLogById(id: Int)


    // --- Water Logs ---

    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllWaterLogs(): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE timestamp >= :startTimeMillis AND timestamp <= :endTimeMillis ORDER BY timestamp DESC")
    fun getWaterLogsBetween(startTimeMillis: Long, endTimeMillis: Long): Flow<List<WaterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLog): Long

    @Delete
    suspend fun deleteWaterLog(waterLog: WaterLog)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteWaterLogById(id: Int)

    @Query("DELETE FROM water_logs")
    suspend fun clearAllWaterLogs()

    @Query("DELETE FROM fasting_logs")
    suspend fun clearAllFastingLogs()
}
