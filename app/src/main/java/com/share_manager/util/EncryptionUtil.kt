package com.share_manager.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption backed by the Android Keystore.
 *
 * The key never leaves the secure hardware enclave (on supported devices).
 * Each [encrypt] call generates a fresh random IV; the IV is prepended to
 * the ciphertext so [decrypt] can extract it without any extra storage.
 *
 * Format of stored value: Base64( IV[12 bytes] || Ciphertext )
 */
object EncryptionUtil {

    private const val KEY_ALIAS       = "meroshare_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION  = "AES/GCM/NoPadding"
    private const val IV_SIZE         = 12   // GCM standard IV length
    private const val TAG_SIZE        = 128  // GCM auth tag size in bits

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).also { it.load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        return KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        ).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    /** Returns Base64-encoded [ IV || ciphertext ], or "" for blank input. */
    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv        = cipher.iv                               // fresh random IV
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    /**
     * Decrypts a value produced by [encrypt].
     * Returns "" on any failure (corrupt data, wrong key, etc.) so callers
     * never crash — they just see an empty PIN/CRN.
     */
    fun decrypt(ciphertext: String): String {
        if (ciphertext.isBlank()) return ""
        return try {
            val combined  = Base64.decode(ciphertext, Base64.NO_WRAP)
            val iv        = combined.copyOfRange(0, IV_SIZE)
            val encrypted = combined.copyOfRange(IV_SIZE, combined.size)
            val cipher    = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_SIZE, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
