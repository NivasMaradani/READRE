package com.example.util

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages secure, salted cryptographic hashing of the Hidden Manga access PIN.
 * PINs are never stored in plaintext. Uses PBKDF2WithHmacSHA256 (with fallback to salted SHA-256)
 * and a cryptographically secure random 16-byte salt.
 */
object SecurityManager {

    private const val PREFS_NAME = "reader_security_prefs"
    private const val KEY_SALT = "pin_salt_b64"
    private const val KEY_HASH = "pin_hash_b64"
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH_BITS = 256

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Checks if a security PIN has already been set up by the user.
     */
    fun isPinConfigured(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.contains(KEY_SALT) && prefs.contains(KEY_HASH)
    }

    /**
     * Configures a new PIN. Generates a fresh random salt and stores the hashed PIN.
     */
    fun setupPin(context: Context, pin: String): Boolean {
        if (pin.length < 4) return false

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val hash = hashPinWithSalt(pin, salt)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        getPrefs(context).edit()
            .putString(KEY_SALT, saltB64)
            .putString(KEY_HASH, hashB64)
            .apply()

        return true
    }

    /**
     * Verifies an entered PIN against the stored cryptographic hash.
     */
    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = getPrefs(context)
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val storedHashB64 = prefs.getString(KEY_HASH, null) ?: return false

        return try {
            val salt = Base64.decode(saltB64, Base64.NO_WRAP)
            val computedHash = hashPinWithSalt(pin, salt)
            val computedHashB64 = Base64.encodeToString(computedHash, Base64.NO_WRAP)

            // Constant-time equals check to prevent timing attacks
            MessageDigest.isEqual(
                computedHashB64.toByteArray(Charsets.UTF_8),
                storedHashB64.toByteArray(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Changes the existing PIN after verifying the old PIN.
     */
    fun changePin(context: Context, oldPin: String, newPin: String): Boolean {
        if (!verifyPin(context, oldPin)) {
            return false
        }
        return setupPin(context, newPin)
    }

    /**
     * Cryptographically hashes the PIN with the provided salt using PBKDF2WithHmacSHA256
     * with a fallback to multi-round salted SHA-256.
     */
    private fun hashPinWithSalt(pin: String, salt: ByteArray): ByteArray {
        return try {
            val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
            val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            skf.generateSecret(spec).encoded
        } catch (e: Exception) {
            // Fallback: 10,000 rounds of salted SHA-256
            val md = MessageDigest.getInstance("SHA-256")
            md.update(salt)
            var current = md.digest(pin.toByteArray(Charsets.UTF_8))
            for (i in 1 until ITERATIONS) {
                md.reset()
                md.update(current)
                md.update(salt)
                current = md.digest()
            }
            current
        }
    }
}
