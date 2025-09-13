package dev.mattramotar.meeseeks.runtime.impl

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.mattramotar.meeseeks.runtime.TaskWorkerRegistry
import dev.mattramotar.meeseeks.runtime.TaskId
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase


internal class MeeseeksWorkerFactory(
    private val database: MeeseeksDatabase,
    private val registry: TaskWorkerRegistry
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
                    TaskId(taskId),
                    registry
                )
            }
        } else {
            null
        }
    }
}