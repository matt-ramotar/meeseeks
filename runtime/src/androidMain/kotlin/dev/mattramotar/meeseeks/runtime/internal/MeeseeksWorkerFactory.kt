package dev.mattramotar.meeseeks.runtime.internal

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

internal class MeeseeksWorkerFactory(
    private val dependencies: MeeseeksDependencies
) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        if (workerClassName == BGTaskCoroutineWorker::class.java.name) {
            return BGTaskCoroutineWorker(appContext, workerParameters, dependencies)
        }
        return null
    }
}