package dev.mattramotar.meeseeks.sample.demo

import dev.mattramotar.meeseeks.runtime.PayloadCipher

internal class DemoPayloadCipher : PayloadCipher {
    override fun encrypt(plaintext: String): String {
        return PREFIX + plaintext.reversed()
    }

    override fun decrypt(ciphertext: String): String {
        require(ciphertext.startsWith(PREFIX)) {
            "Invalid demo ciphertext prefix."
        }
        return ciphertext.removePrefix(PREFIX).reversed()
    }

    private companion object {
        private const val PREFIX = "demo:v1:"
    }
}
