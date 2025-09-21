package dev.mattramotar.meeseeks.runtime

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.mattramotar.meeseeks.runtime.internal.BGTaskCoroutineWorker
import dev.mattramotar.meeseeks.runtime.internal.RealBGTaskManager

class MeeseeksWorkerFactory(
    private val bgTaskManager: BGTaskManager,
) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
        if (workerClassName == BGTaskCoroutineWorker::class.java.name) {
            require(bgTaskManager is RealBGTaskManager) { "bgTaskManager must be an instance of RealBGTaskManager!" }
            return BGTaskCoroutineWorker(appContext, workerParameters, bgTaskManager.dependencies)
        }
        return null
    }
}