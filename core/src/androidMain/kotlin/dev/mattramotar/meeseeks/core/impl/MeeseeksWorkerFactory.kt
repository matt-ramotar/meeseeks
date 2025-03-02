package dev.mattramotar.meeseeks.core.impl

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.mattramotar.meeseeks.core.MeeseeksRegistry
import dev.mattramotar.meeseeks.core.MrMeeseeksId
import dev.mattramotar.meeseeks.core.db.MeeseeksDatabase


internal class MeeseeksWorkerFactory(
    private val database: MeeseeksDatabase,
    private val registry: MeeseeksRegistry
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return if (workerClassName == MeeseeksWorker::class.qualifiedName) {
            val taskId =
                workerParameters.inputData.getLong(WorkRequestFactory.KEY_TASK_ID, -1)
            if (taskId <= 0) {
                null
            } else {
                MeeseeksWorker(
                    appContext,
                    workerParameters,
                    database,
                    MrMeeseeksId(taskId),
                    registry
                )
            }
        } else {
            null
        }
    }
}