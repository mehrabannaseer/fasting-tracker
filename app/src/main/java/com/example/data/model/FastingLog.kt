package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fasting_logs")
data class FastingLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val targetHours: Int = 16,
    val notes: String = ""
) {
    val durationMillis: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime

    val isCompleted: Boolean
        get() = durationMillis >= (targetHours * 3600_000L)
}
