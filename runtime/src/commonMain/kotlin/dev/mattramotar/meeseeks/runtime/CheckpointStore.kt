package dev.mattramotar.meeseeks.runtime

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Durable app-level checkpoint storage for a running task.
 *
 * Checkpoints are serializable progress markers owned by worker code. They are
 * retained across retry and process restart, then cleared when the task reaches
 * terminal success, permanent failure, or cancellation.
 */
public interface CheckpointStore {
    public suspend fun <T : Any> read(
        key: String = DEFAULT_KEY,
        serializer: KSerializer<T>,
    ): T?

    public suspend fun <T : Any> write(
        key: String = DEFAULT_KEY,
        value: T,
        serializer: KSerializer<T>,
        schemaVersion: Int = 1,
    )

    public suspend fun clear(key: String = DEFAULT_KEY)

    public suspend fun clearAll()

    public companion object {
        public const val DEFAULT_KEY: String = "default"
    }
}

@OptIn(ExperimentalSerializationApi::class)
public suspend inline fun <reified T : Any> CheckpointStore.read(
    key: String = CheckpointStore.DEFAULT_KEY,
): T? = read(key, serializer<T>())

@OptIn(ExperimentalSerializationApi::class)
public suspend inline fun <reified T : Any> CheckpointStore.write(
    value: T,
    key: String = CheckpointStore.DEFAULT_KEY,
    schemaVersion: Int = 1,
) {
    write(key, value, serializer<T>(), schemaVersion)
}

public class CheckpointDecodeException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

public class CheckpointIncompatibleException(
    message: String,
) : IllegalStateException(message)
