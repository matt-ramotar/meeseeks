package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.internal.db.QuartzDatabaseInitializer
import dev.mattramotar.meeseeks.runtime.internal.db.QuartzProps
import kotlinx.serialization.json.Json
import org.quartz.impl.StdSchedulerFactory
import java.nio.file.Paths
import java.util.*

internal actual class BGTaskManagerFactory {
    actual fun create(
        context: AppContext,
        registry: WorkerRegistry,
        json: Json,
        config: BGTaskManagerConfig
    ): BGTaskManager {
        val database = MeeseeksDatabaseSingleton.instance
        val props = Properties().apply {
            val inStream = checkNotNull(
                javaClass.classLoader.getResourceAsStream("quartz.properties")
            ) { "quartz.properties not found on classpath" }
            inStream.use { load(it) }
        }

        val jdbcUrl = normalizeSqliteUrl(QuartzProps.jdbcUrlFromQuartzProps(props))

        QuartzProps.setJdbcUrl(props, jdbcUrl)
        QuartzProps.assertNoMismatchedSystemProperty(jdbcUrl)
        QuartzDatabaseInitializer.initialize(jdbcUrl)

        val scheduler = StdSchedulerFactory(props).scheduler
        scheduler.context["meeseeksDatabase"] = database
        scheduler.context["workerRegistry"] = registry
        scheduler.context["bgTaskManagerConfig"] = config
        scheduler.start()

        val workRequestFactory = WorkRequestFactory()
        val taskScheduler = TaskScheduler(scheduler)
        val taskRescheduler = TaskRescheduler(database, taskScheduler, workRequestFactory, config, registry)

        return RealBGTaskManager(
            database = database,
            workRequestFactory = workRequestFactory,
            taskScheduler = taskScheduler,
            taskRescheduler = taskRescheduler,
            registry = registry,
            config = config
        )
    }

    private fun normalizeSqliteUrl(url: String): String {
        val prefix = "jdbc:sqlite:"
        if (!url.startsWith(prefix)) return url
        val pathPart = url.removePrefix(prefix)
        if (pathPart == ":memory:" || Paths.get(pathPart).isAbsolute) return url
        val abs = Paths.get(pathPart).toAbsolutePath().toString()
        return prefix + abs
    }
}