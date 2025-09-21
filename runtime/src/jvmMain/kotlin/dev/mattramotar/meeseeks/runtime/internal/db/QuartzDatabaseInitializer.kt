package dev.mattramotar.meeseeks.runtime.internal.db

import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

@Suppress("SqlNoDataSourceInspection")
internal object QuartzDatabaseInitializer {

    private val logger = LoggerFactory.getLogger(QuartzDatabaseInitializer::class.java)
    private const val DEFAULT_SCHEMA_RESOURCE =
        "/dev/mattramotar/meeseeks/runtime/internal/quartz/tables_sqlite.sql"

    private val EXPECTED_TABLES: Set<String> = setOf(
        "QRTZ_JOB_DETAILS",
        "QRTZ_TRIGGERS",
        "QRTZ_SIMPLE_TRIGGERS",
        "QRTZ_CRON_TRIGGERS",
        "QRTZ_SIMPROP_TRIGGERS",
        "QRTZ_BLOB_TRIGGERS",
        "QRTZ_CALENDARS",
        "QRTZ_PAUSED_TRIGGER_GRPS",
        "QRTZ_FIRED_TRIGGERS",
        "QRTZ_SCHEDULER_STATE",
        "QRTZ_LOCKS"
    )

    @JvmStatic
    fun initialize(
        jdbcUrl: String,
        schemaResourcePath: String = DEFAULT_SCHEMA_RESOURCE,
    ) {
        require(jdbcUrl.startsWith("jdbc:")) {
            "Invalid JDBC URL for Quartz: '$jdbcUrl'"
        }
        createDatabaseIfNotExists(jdbcUrl)
        initializeQuartzSchema(jdbcUrl, schemaResourcePath)
    }

    private fun createDatabaseIfNotExists(jdbcUrl: String) {
        try {
            Class.forName("org.sqlite.JDBC")
            DriverManager.getConnection(jdbcUrl).use { connection ->
                applyRecommendedPragmas(connection)
                connection.createStatement().use { it.execute("SELECT 1") }
            }
            logger.info("Quartz database is reachable at $jdbcUrl")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create or open Quartz database at $jdbcUrl", e)
        }
    }

    private fun applyRecommendedPragmas(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA foreign_keys = ON")
            statement.execute("PRAGMA journal_mode = WAL")
            statement.execute("PRAGMA synchronous = NORMAL")
            statement.execute("PRAGMA busy_timeout = 5000")
        }
    }

    private fun initializeQuartzSchema(
        jdbcUrl: String,
        schemaResourcePath: String
    ) {
        val startedNanos = System.nanoTime()
        val stream = this::class.java.getResourceAsStream(schemaResourcePath)
            ?: throw IllegalArgumentException("Could not find Quartz schema at $schemaResourcePath.")

        val script = InputStreamReader(stream, StandardCharsets.UTF_8).use { it.readText() }
        val statements = SqlScriptParser.parse(script)
        DriverManager.getConnection(jdbcUrl).use { connection ->
            applyRecommendedPragmas(connection)

            if (allExpectedQuartzTablesExist(connection)) {
                logger.info("Quartz tables already exist. Skipping schema initialization.")
                return
            }

            connection.autoCommit = false

            try {
                logger.info(
                    "Initializing Quartz schema (${statements.size} statements parsed).",
                )

                for ((index, sql) in statements.withIndex()) {
                    try {
                        connection.createStatement().use { statement -> statement.execute(sql) }
                    } catch (e: SQLException) {
                        if (isBenignIdempotencyError(e)) {
                            logger.debug("Idempotent skip at statement $index: ${firstLine(sql)}")
                            continue
                        }

                        val context = "Failure at statement $index: ${firstLine(sql)}"
                        throw SQLException(context, e)
                    }
                }
                connection.commit()
                val finishedNanos = System.nanoTime()
                val elapsedNanos = finishedNanos - startedNanos
                val nanosInMs = 1_000_000
                val elapsedMs = elapsedNanos / nanosInMs
                logger.info("Quartz schema initialization complete in $elapsedMs ms.")
            } catch (e: Exception) {
                connection.rollback()
                throw IllegalStateException("Failed to initialize Quartz schema at $schemaResourcePath", e)
            }
        }
    }

    private fun isBenignIdempotencyError(e: SQLException): Boolean {
        val msg = (e.message ?: "").lowercase(Locale.ROOT)
        return msg.contains("already exists") ||
            msg.contains("duplicate column name") ||
            msg.contains("index") && msg.contains("exists") ||
            msg.contains("table") && msg.contains("exists")
    }

    private fun firstLine(sql: String): String {
        val line = sql.lineSequence().firstOrNull()?.trim() ?: ""
        return if (line.length <= 180) line else line.take(180) + "..."
    }

    private fun allExpectedQuartzTablesExist(connection: Connection): Boolean {
        val existing = buildSet {
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'QRTZ_%'"
                ).use { resultSet ->
                    while (resultSet.next()) add(resultSet.getString(1).uppercase(Locale.ROOT))
                }
            }
        }
        return EXPECTED_TABLES.isNotEmpty() && EXPECTED_TABLES.all { it in existing }
    }
}