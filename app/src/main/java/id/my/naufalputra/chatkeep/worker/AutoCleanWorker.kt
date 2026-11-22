package id.my.naufalputra.chatkeep.worker

import android.content.Context
import androidx.work.*
import id.my.naufalputra.chatkeep.data.ChatDatabase
import java.util.concurrent.TimeUnit

class AutoCleanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val daysToKeep = inputData.getInt("DAYS_TO_KEEP", 30)
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysToKeep.toLong())
        val dao = ChatDatabase.getInstance(applicationContext).chatDao()
        dao.deleteOlderThan(cutoff)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context, daysToKeep: Int = 30) {
            val data = workDataOf("DAYS_TO_KEEP" to daysToKeep)
            val request = PeriodicWorkRequestBuilder<AutoCleanWorker>(24, TimeUnit.HOURS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "ChatKeepAutoCleanWork",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
