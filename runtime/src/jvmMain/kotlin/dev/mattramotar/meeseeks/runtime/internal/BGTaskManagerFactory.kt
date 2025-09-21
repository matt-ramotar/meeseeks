package dev.mattramotar.meeseeks.runtime.internal

import dev.mattramotar.meeseeks.runtime.AppContext
import dev.mattramotar.meeseeks.runtime.BGTaskManager
import dev.mattramotar.meeseeks.runtime.BGTaskManagerConfig
import dev.mattramotar.meeseeks.runtime.db.MeeseeksDatabase
import dev.mattramotar.meeseeks.runtime.internal.db.QuartzDatabaseInitializer
import dev.mattramotar.meeseeks.runtime.internal.db.QuartzProps
import kotlinx.serialization.json.Json
import org.quartz.impl.StdSchedulerFactory
import java.nio.file.Paths
import java.util.*

internal actual class BGTaskManagerFactory {
    actual fun create(
        context: AppContext,
        database: MeeseeksDatabase,
        registry: WorkerRegistry,
        json: Json,
        config: BGTaskManagerConfig
    ): BGTaskManager {
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

        val dependencies = MeeseeksDependencies(database, registry, config, context)
        scheduler.context[BGTaskQuartzJob.CTX_MEESEEKS_DEPS] = dependencies
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
            config = config,
            appContext = context,
            telemetry = config.telemetry,
        )
    }

    private fun normalizeSqliteUrl(url: String): String {
        val prefix = "jdbc:sqlite:"
        if (!url.startsWith(prefix)) return url
        val pathPart = url.removePrefix(prefix)
        if (pathPart == ":memory:") return url
        val questionMarkIndex = pathPart.indexOf('?')
        val (path, queryParams) = if (questionMarkIndex != -1) {
            pathPart.take(questionMarkIndex) to pathPart.substring(questionMarkIndex)
        } else {
            pathPart to ""
        }
        if (Paths.get(path).isAbsolute) return url
        val absolutePath = Paths.get(path).toAbsolutePath().toString()
        return prefix + absolutePath + queryParams
    }
}