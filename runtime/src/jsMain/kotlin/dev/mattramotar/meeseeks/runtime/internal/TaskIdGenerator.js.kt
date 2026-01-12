package dev.mattramotar.meeseeks.runtime.internal

import kotlin.random.Random

internal actual fun generateTaskIdValue(): String {
    val cryptoUuid = js("typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : null") as String?
    return cryptoUuid ?: fallbackUuid()
}

private fun fallbackUuid(): String {
    val bytes = IntArray(16) { Random.nextInt(0, 256) }
    bytes[6] = (bytes[6] and 0x0F) or 0x40
    bytes[8] = (bytes[8] and 0x3F) or 0x80

    return buildString(36) {
        appendHex(bytes, 0, 4)
        append('-')
        appendHex(bytes, 4, 6)
        append('-')
        appendHex(bytes, 6, 8)
        append('-')
        appendHex(bytes, 8, 10)
        append('-')
        appendHex(bytes, 10, 16)
    }
}

private fun StringBuilder.appendHex(bytes: IntArray, start: Int, end: Int) {
    for (index in start until end) {
        append(bytes[index].toString(16).padStart(2, '0'))
    }
}
