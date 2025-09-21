package dev.mattramotar.meeseeks.runtime.internal.db

import java.util.Locale

internal object SqlScriptParser {

    fun parse(sql: String): List<String> {
        val result = mutableListOf<String>()
        val stringBuilder = StringBuilder()
        var index = 0
        val n = sql.length

        var inSingle = false
        var inDouble = false
        var inLineComment = false
        var inBlockComment = false

        fun peek(): Char = if (index + 1 < n) sql[index + 1] else '\u0000'

        while (index < n) {
            val char = sql[index]
            val next = peek()

            if (inLineComment) {
                if (char == '\n') inLineComment = false
                index++
                continue
            }
            if (inBlockComment) {
                if (char == '*' && next == '/') {
                    inBlockComment = false
                    index += 2
                } else {
                    index++
                }
                continue
            }

            if (!inSingle && !inDouble) {

                // Start of a comment?
                if (char == '-' && next == '-') {
                    inLineComment = true
                    index += 2
                    continue
                }
                if (char == '/' && next == '*') {
                    inBlockComment = true
                    index += 2
                    continue
                }

                // Statement boundary?
                if (char == ';') {
                    val stmt = stringBuilder.toString().trim()
                    if (stmt.isNotEmpty() && !isVendorTransactionControl(stmt)) {
                        result.add(stmt)
                    }
                    stringBuilder.setLength(0)
                    index++
                    continue
                }

                // Quote starts?
                if (char == '\'') {
                    inSingle = true
                    stringBuilder.append(char); index++
                    continue
                }
                if (char == '"') {
                    inDouble = true
                    stringBuilder.append(char); index++
                    continue
                }
            } else {
                // Inside a quoted region
                if (inSingle && char == '\'') {
                    if (next == '\'') {
                        stringBuilder.append("''"); index += 2; continue // Doubled single-quote
                    } else {
                        inSingle = false
                        stringBuilder.append(char); index++; continue
                    }
                }
                if (inDouble && char == '"') {
                    if (next == '"') {
                        stringBuilder.append("\"\""); index += 2; continue // Doubled double-quote
                    } else {
                        inDouble = false
                        stringBuilder.append(char); index++; continue
                    }
                }
            }

            stringBuilder.append(char)
            index++
        }

        val tail = stringBuilder.toString().trim()
        if (tail.isNotEmpty() && !isVendorTransactionControl(tail)) {
            result.add(tail)
        }
        return result
    }

    // Dropping vendor transaction control so we manage the JDBC transactions ourselves.
    private fun isVendorTransactionControl(statement: String): Boolean {
        val trimmed = statement.trimStart().uppercase(Locale.ROOT)
        return trimmed.startsWith("BEGIN") ||
            trimmed.startsWith("COMMIT") ||
            trimmed.startsWith("END") ||
            trimmed.startsWith("ROLLBACK")
    }
}