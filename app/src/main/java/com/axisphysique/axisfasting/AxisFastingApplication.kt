package com.axisphysique.axisfasting

import android.app.Application
import com.axisphysique.axisfasting.data.AppDatabase
import com.axisphysique.axisfasting.data.FastRepository
import com.axisphysique.axisfasting.util.NotificationHelper

class AxisFastingApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { FastRepository(database.fastDao()) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
