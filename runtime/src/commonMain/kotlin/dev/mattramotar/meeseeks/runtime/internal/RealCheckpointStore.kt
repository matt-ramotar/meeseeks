package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.CheckpointDecodeException
import dev.mattramotar.meeseeks.runtime.CheckpointIncompatibleException
import dev.mattramotar.meeseeks.runtime.CheckpointStore
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.db.TaskCheckpointEntity
import dev.mattramotar.meeseeks.runtime.internal.coroutines.MeeseeksDispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

internal class RealCheckpointStore(
    private val database: MeeseeksDatabase,
    private val registry: WorkerRegistry,
    private val taskId: String,
    private val payloadTypeId: String,
    private val workerTypeId: String,
) : CheckpointStore {

    override suspend fun <T : Any> read(
        serializer: KSerializer<T>,
        key: String,
    ): T? = withContext(MeeseeksDispatchers.IO) {
        val row = database.taskCheckpointQueries
            .selectCheckpoint(taskId, key)
            .executeAsOneOrNull() ?: return@withContext null

        val expectedCheckpointTypeId = registry.checkpointTypeId(serializer)
        validateCompatibility(row, key, expectedCheckpointTypeId)

        try {
            registry.deserializeCheckpoint(serializer, row.data_)
        } catch (error: Throwable) {
            throw CheckpointDecodeException(
                message = "Could not decode checkpoint '$key' for task '$taskId' as '$expectedCheckpointTypeId'.",
                cause = error,
            )
        }
    }

    override suspend fun <T : Any> write(
        value: T,
        serializer: KSerializer<T>,
        key: String,
    ) {
        val checkpoint = registry.serializeCheckpoint(serializer, value)
        val now = Timestamp.now()
        withContext(MeeseeksDispatchers.IO) {
            database.transaction {
                val existing = database.taskCheckpointQueries
                    .selectCheckpoint(taskId, key)
                    .executeAsOneOrNull()

                if (existing == null) {
                    database.taskCheckpointQueries.insertCheckpoint(
                        task_id = taskId,
                        checkpoint_key = key,
                        payload_type_id = payloadTypeId,
                        worker_type_id = workerTypeId,
                        checkpoint_type_id = checkpoint.typeId,
                        data_ = checkpoint.data,
                        created_at_ms = now,
                        updated_at_ms = now,
                    )
                } else {
                    database.taskCheckpointQueries.updateCheckpoint(
                        payload_type_id = payloadTypeId,
                        worker_type_id = workerTypeId,
                        checkpoint_type_id = checkpoint.typeId,
                        data_ = checkpoint.data,
                        updated_at_ms = now,
                        task_id = taskId,
                        checkpoint_key = key,
                    )
                }
            }
        }
    }

    override suspend fun clear(key: String) {
        withContext(MeeseeksDispatchers.IO) {
            database.taskCheckpointQueries.deleteCheckpoint(taskId, key)
        }
    }

    override suspend fun clearAll() {
        withContext(MeeseeksDispatchers.IO) {
            database.taskCheckpointQueries.deleteAllCheckpointsForTask(taskId)
        }
    }

    private fun validateCompatibility(
        row: TaskCheckpointEntity,
        key: String,
        expectedCheckpointTypeId: String,
    ) {
        if (row.payload_type_id != payloadTypeId) {
            throw incompatible(
                key = key,
                expected = payloadTypeId,
                actual = row.payload_type_id,
                field = "payload type",
            )
        }
        if (row.worker_type_id != workerTypeId) {
            throw incompatible(
                key = key,
                expected = workerTypeId,
                actual = row.worker_type_id,
                field = "worker type",
            )
        }
        if (row.checkpoint_type_id != expectedCheckpointTypeId) {
            throw incompatible(
                key = key,
                expected = expectedCheckpointTypeId,
                actual = row.checkpoint_type_id,
                field = "checkpoint type",
            )
        }
    }

    private fun incompatible(
        key: String,
        field: String,
        expected: String,
        actual: String,
    ): CheckpointIncompatibleException {
        return CheckpointIncompatibleException(
            "Checkpoint '$key' for task '$taskId' has incompatible $field: expected '$expected', found '$actual'."
        )
    }
}
