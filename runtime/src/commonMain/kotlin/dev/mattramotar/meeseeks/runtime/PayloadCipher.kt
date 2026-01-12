package dev.mattramotar.meeseeks.runtime

/**
 * Encrypts and decrypts task payload data before it is stored in the database.
 *
 * Implementations must be deterministic and reversible across app launches.
 * Returned ciphertext must be safe to store as TEXT (e.g., Base64-encode binary output).
 */
interface PayloadCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String
}
