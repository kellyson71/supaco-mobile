package com.example.supacomobile.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.supacomobile.data.local.AppDatabase
import com.example.supacomobile.data.local.SettingsManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class FaltasWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val database: AppDatabase by inject()
    private val settings: SettingsManager by inject()

    override suspend fun doWork(): Result {
        FaltasNotifier.checkAndNotify(applicationContext, settings, database.boletimDao().getBoletim())
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "faltas_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FaltasWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
