package io.github.kellyson71.supaco.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.kellyson71.supaco.data.repository.AcademicRepository
import io.github.kellyson71.supaco.data.session.SessionManager
import io.github.kellyson71.supaco.data.session.SessionState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Sincroniza o boletim do período corrente em segundo plano. O próprio
 * [AcademicRepository.fetchBoletim] atualiza cache, widgets e dispara os alertas.
 */
class FaltasWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val academicRepository: AcademicRepository by inject()
    private val sessionManager: SessionManager by inject()

    override suspend fun doWork(): Result {
        if (sessionManager.state.value != SessionState.LOGGED_IN) return Result.success()

        val periodo = academicRepository.getPeriodosLetivos().getOrNull()?.firstOrNull()
            ?: return Result.retry()

        return academicRepository.fetchBoletim(periodo, isCurrent = true).fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                when {
                    error is IOException -> Result.retry()
                    error is HttpException && (error.code() == 429 || error.code() >= 500) -> Result.retry()
                    else -> Result.failure()
                }
            },
        )
    }

    companion object {
        private const val WORK_NAME = "faltas_check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FaltasWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
