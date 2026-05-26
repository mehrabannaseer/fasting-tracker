package com.axisphysique.axisfasting.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.axisphysique.axisfasting.util.NotificationHelper

class FastWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        NotificationHelper.sendFastCompleteNotification(applicationContext)
        return Result.success()
    }
}
