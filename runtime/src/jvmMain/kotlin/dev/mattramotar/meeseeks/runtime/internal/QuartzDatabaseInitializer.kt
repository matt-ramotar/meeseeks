package dev.mattramotar.meeseeks.runtime.internal

import java.sql.DriverManager
import java.sql.SQLException

@Suppress("SqlNoDataSourceInspection")
internal object QuartzDatabaseInitializer {

    private const val QUARTZ_DB_URL = "jdbc:sqlite:quartz-scheduler.db"

    fun initialize() {
        createDatabaseIfNotExists()
        initializeQuartzSchema()
    }

    private fun createDatabaseIfNotExists() {
        try {
            Class.forName("org.sqlite.JDBC")
            DriverManager.getConnection(QUARTZ_DB_URL).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("PRAGMA foreign_keys = ON")
                }

                connection.createStatement().use { statement ->
                    statement.execute("SELECT 1")
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to create Quartz database", e)
        }
    }

    private fun initializeQuartzSchema() {
        val resourcePath = "/dev/mattramotar/meeseeks/runtime/internal/quartz/tables_sqlite.sql"
        val stream = this::class.java.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Could not find Quartz schema at $resourcePath.")

        val script = stream.bufferedReader().use { it.readText() }
        val statements = script.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() } // Exclude empty statements
            .filter { !it.startsWith("--") } // Exclude SQL comments

        DriverManager.getConnection(QUARTZ_DB_URL).use { connection ->
            connection.autoCommit = false
            try {
                val tablesExist = checkTablesExist(connection)
                if (tablesExist) {
                    println("Quartz tables already exist. Skipping initialization.")
                    return
                }

                for ((index, stringSqlStatement) in statements.withIndex()) {
                    val review = reviewStringSqlStatement(stringSqlStatement)
                    val isSafeToExecute = with(review) {
                        !isEmpty && startsWithAllowedPrefix && dangerousKeywords.isEmpty() && injectionPatterns.isEmpty()
                    }

                    if (isSafeToExecute) {
                        try {
                            connection.createStatement().use { statement ->
                                statement.execute(stringSqlStatement)
                            }
                        } catch (e: SQLException) {
                            if (e.message?.contains("already exists", ignoreCase = true) == true) {
                                println("Already exists. Skipping statement at index $index. Continuing.")
                            } else {
                                throw e
                            }
                        }
                    } else {

                        throw IllegalArgumentException(
                            "SQL statement is not safe to execute: $review",
                        )
                    }
                }

                connection.commit()
                println("Successfully initialized Quartz schema with ${statements.size} statements.")
            } catch (e: Exception) {
                connection.rollback()
                throw IllegalStateException("Failed to initialize Quartz schema", e)
            }
        }
    }

    private fun checkTablesExist(connection: java.sql.Connection): Boolean {
        return try {
            connection.createStatement().use { statement ->
                val resultSet = statement.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='QRTZ_JOB_DETAILS'"
                )
                resultSet.use { it.next() }
            }
        } catch (_: SQLException) {
            false
        }
    }


    private fun reviewStringSqlStatement(stringSqlStatement: String): StringSqlStatementReview {
        val trimmed = stringSqlStatement.trim().uppercase()
        if (trimmed.isEmpty()) return StringSqlStatementReview.EMPTY
        val startsWithAllowedPrefix = ALLOWED.StatementPrefixes.any { trimmed.startsWith(it) }
        val dangerousKeywords = BLOCKED.StatementKeywords.filter { trimmed.contains(it) }
        val injectionPatterns = BLOCKED.InjectionPatterns.filter { trimmed.contains(it) }
        return StringSqlStatementReview(
            isEmpty = false,
            startsWithAllowedPrefix,
            dangerousKeywords,
            injectionPatterns
        )
    }

    private data class StringSqlStatementReview(
        val isEmpty: Boolean,
        val startsWithAllowedPrefix: Boolean,
        val dangerousKeywords: List<String>,
        val injectionPatterns: List<String>
    ) {
        companion object Companion {
            val EMPTY = StringSqlStatementReview(
                isEmpty = true,
                startsWithAllowedPrefix = false,
                dangerousKeywords = emptyList(),
                injectionPatterns = emptyList()
            )
        }
    }

    private object ALLOWED {
        val StatementPrefixes = listOf(
            "CREATE TABLE",
            "CREATE INDEX",
            "CREATE UNIQUE INDEX",
            "ALTER TABLE",
            "DROP TABLE IF EXISTS",
            "DROP INDEX IF EXISTS"
        )
    }

    private object BLOCKED {

        val StatementKeywords = listOf(
            "DELETE FROM",
            "DELETE WHERE",
            "UPDATE ",
            "INSERT INTO",
            "INSERT VALUES",
            "EXEC",
            "EXECUTE",
            "DROP DATABASE",
            "DROP SCHEMA",
            "TRUNCATE",
            "GRANT",
            "REVOKE",
            "ATTACH",
            "DETACH"
        )

        val InjectionPatterns = listOf(
            "--",
            "/*",
            "*/",
            "XP_",
            "SP_",
            "\u0000",
            ";",
            "UNION SELECT",
            "OR 1=1",
            "OR '1'='1'"
        )
    }
}