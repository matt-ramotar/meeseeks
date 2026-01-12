package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskSpec
import dev.mattramotar.meeseeks.runtime.internal.db.TaskMapper
import kotlinx.datetime.Clock


internal interface TaskRescheduler {
    fun rescheduleTasks()
    fun rescheduleTask(taskSpec: TaskSpec)

    companion object {
        operator fun invoke(
            database: MeeseeksDatabase,
            taskScheduler: TaskScheduler,
            workRequestFactory: WorkRequestFactory,
            config: BGTaskManagerConfig,
            registry: WorkerRegistry
        ): TaskRescheduler {
            return RealTaskRescheduler(database, taskScheduler, workRequestFactory, config, registry)
        }
    }
}

private class RealTaskRescheduler(
    private val database: MeeseeksDatabase,
    private val taskScheduler: TaskScheduler,
    private val workRequestFactory: WorkRequestFactory,
    private val config: BGTaskManagerConfig,
    private val registry: WorkerRegistry
) : TaskRescheduler {

    override fun rescheduleTasks() {
        val taskSpecs = database.taskSpecQueries.selectAllPending().executeAsList()
        for (taskSpec in taskSpecs) {
            val taskRequest = TaskMapper.mapToTaskRequest(taskSpec, registry)
            if (!taskScheduler.isScheduled(taskSpec.id, taskRequest.schedule)) {
                rescheduleTask(taskSpec)
            }
        }
    }

    override fun rescheduleTask(taskSpec: TaskSpec) {
        val taskRequest = TaskMapper.mapToTaskRequest(taskSpec, registry)
        val workRequest = workRequestFactory.createWorkRequest(taskSpec.id, taskRequest, config)

        taskScheduler.scheduleTask(taskSpec.id, taskRequest, workRequest, ExistingWorkPolicy.REPLACE)

        val now = Clock.System.now().toEpochMilliseconds()
        database.taskSpecQueries.updatePlatformId(
            platform_id = workRequest.id,
            updated_at_ms = now,
            id = taskSpec.id
        )

        database.taskLogQueries.insertLog(
            taskId = taskSpec.id,
            created = now,
            result = TaskResult.Retry.type,
            attempt = taskSpec.run_attempt_count,
            message = "Task resurrected by TaskRescheduler."
        )
    }
}
