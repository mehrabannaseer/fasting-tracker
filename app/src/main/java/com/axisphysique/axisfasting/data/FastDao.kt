package com.axisphysique.axisfasting.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FastDao {
    @Query("SELECT * FROM fasts ORDER BY endTime DESC")
    fun getAllFasts(): Flow<List<FastEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFast(fast: FastEntity)

    @Query("SELECT COUNT(*) FROM fasts")
    fun getFastCount(): Flow<Int>

    @Query("DELETE FROM fasts WHERE id = :id")
    suspend fun deleteFast(id: Int)

    @Query("DELETE FROM fasts")
    suspend fun deleteAllFasts()
}
