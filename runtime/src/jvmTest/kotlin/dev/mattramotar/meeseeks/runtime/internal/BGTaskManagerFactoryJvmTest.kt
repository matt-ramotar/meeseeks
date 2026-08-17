package dev.mattramotar.meeseeks.runtime.internal

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.RuntimeContext
import dev.mattramotar.meeseeks.runtime.TaskPayload
import dev.mattramotar.meeseeks.runtime.TaskResult
import dev.mattramotar.meeseeks.runtime.Worker
import dev.mattramotar.meeseeks.runtime.WorkerFactory
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.db.adapters.taskLogEntityAdapter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.quartz.impl.SchedulerRepository
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression test for https://github.com/matt-ramotar/meeseeks/issues/80.
 *
 * `BGTaskManagerFactory.create` resolved `javaClass.classLoader` against the
 * `Properties().apply { }` receiver. `java.util.Properties` is loaded by the
 * bootstrap classloader, whose `classLoader` is null, so every JVM
 * `Meeseeks.initialize` call threw a NullPointerException before any
 * scheduling work ran. The published jar also never contained the default
 * `quartz.properties` (it lived in `src/main/resources`, which is not a
 * Kotlin Multiplatform source set), so a stock consumer classpath has no
 * host-level Quartz config either. This suite runs the factory exactly as
 * such a consumer would: no `quartz.properties` on the test classpath root.
 */
@OptIn(ExperimentalSerializationApi::class)
class BGTaskManagerFactoryJvmTest {

    @Serializable
    private data class TestPayload(val value: String) : TaskPayload

    private object TestAppContext : AppContext()

    private val json = Json

    @AfterTest
    fun tearDown() {
        // The factory starts the scheduler named by the bundled config; shut it
        // down so its non-daemon threads do not outlive the test.
        SchedulerRepository.getInstance().lookup(BUNDLED_SCHEDULER_NAME)?.let { scheduler ->
            if (!scheduler.isShutdown) scheduler.shutdown(true)
        }
        // The bundled config keeps the Quartz job store in the working directory.
        listOf(
            "quartz-scheduler.db",
            "quartz-scheduler.db-wal",
            "quartz-scheduler.db-shm",
        ).forEach { Files.deleteIfExists(Paths.get(it)) }
    }

    @Test
    fun createCompletesWithoutHostQuartzProperties() {
        val manager = BGTaskManagerFactory().create(
            context = TestAppContext,
            database = createDatabase(),
            registry = registry(),
            json = json,
            config = BGTaskManagerConfig(),
        )

        assertNotNull(manager)
        val scheduler = SchedulerRepository.getInstance().lookup(BUNDLED_SCHEDULER_NAME)
        assertNotNull(
            scheduler,
            "Expected the factory to start the scheduler defined by the bundled Quartz config",
        )
        assertTrue(scheduler.isStarted)
    }

    private fun createDatabase(): MeeseeksDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, schema = MeeseeksDatabase.Schema)
        return MeeseeksDatabase(driver, taskLogEntityAdapter(json))
    }

    private fun registry(): WorkerRegistry {
        val serializer = TestPayload.serializer()
        val registration = WorkerRegistration(
            type = TestPayload::class,
            typeId = serializer.descriptor.serialName,
            serializer = serializer,
            factory = WorkerFactory<TestPayload> { appContext ->
                object : Worker<TestPayload>(appContext) {
                    override suspend fun run(payload: TestPayload, context: RuntimeContext): TaskResult {
                        return TaskResult.Success
                    }
                }
            },
        )
        return WorkerRegistry(
            registrations = mapOf(TestPayload::class to registration),
            json = json,
        )
    }

    private companion object {
        const val BUNDLED_SCHEDULER_NAME = "MeeseeksScheduler"
    }
}
